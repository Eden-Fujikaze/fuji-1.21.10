package at.fuji.bazaar;

import at.fuji.ModConfig;
import at.fuji.utils.InputUtils;
import at.fuji.utils.LoreUtils;
import at.fuji.utils.MenuUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.List;

public class BazaarWorker {

    public enum StateMachine {
        IDLE,
        OPEN_BAZAAR, CLICK_SLOT, TYPE_AMOUNT, CLICK_DONE, CONFIRM_TRADE, DONE,
        AWAIT_SCREEN_OPEN, AWAIT_SCREEN_CHANGE, AWAIT_SIGN_CLOSE, AWAIT_ENTER_FLUSH,
        AWAIT_ITEM_THEN_ESCAPE, AWAIT_ITEM_SELECT,
        OPEN_YOUR_ORDERS, SCAN_YOUR_ORDERS,
        AWAIT_FLIP_MENU, CLICK_FLIP, AWAIT_CUSTOM_MENU, CLICK_CUSTOM_SELL,
        AWAIT_SELL_PRICE, TYPE_SELL_PRICE, AWAIT_SELL_SIGN_CLOSE,
        OPEN_CLAIM_ORDERS, SCAN_CLAIM_ORDERS, AWAIT_CLAIM_CLOSE, START_FRESH,
        AWAIT_ITEM_CONFIRM, AWAIT_TYPING_FLUSH, AWAIT_PRICE, SCAN_MENU,
        NPC_OPEN_CANCEL, NPC_AWAIT_CANCEL_SCREEN, NPC_SCAN_SELL_ORDER,
        NPC_AWAIT_ORDER_DETAIL, NPC_CLICK_CANCEL_ORDER, NPC_AWAIT_CANCELLED,
    }

    private static StateMachine state = StateMachine.IDLE;
    private static boolean enabled = false;

    public static volatile double lastKnownPurse = 0;

    // ── Item tracking — keep productId and displayName separate ───────────────
    /**
     * Actual Hypixel API product key (e.g. "ENCHANTED_SUGAR"). Used for API calls.
     */
    public static String currentProductId = null;
    /**
     * Human-readable display name (e.g. "Enchanted Sugar"). Used for commands and
     * slot scanning.
     */
    static String itemName = null;

    static MinecraftClient client = MinecraftClient.getInstance();
    static List<String> menuItems = List.of("Custom Amount", "Create Buy Order", "Top Order +0.1");

    private static int pendingSlot = -1;
    private static double calculatedSellPrice = 0;
    private static String pendingMenu = "";
    private static String screenTitleBefore = null;
    private static int calculatedAmount = 1;
    private static long lastOrderCheck = 0;
    private static final long ORDER_CHECK_INTERVAL = 15_000;

    // ── Control ───────────────────────────────────────────────────────────────

    public void start() {
        enabled = true;
        state = StateMachine.AWAIT_ITEM_SELECT;
        double purse = at.fuji.utils.ScoreboardUtils.getPurseValue();
        lastKnownPurse = purse;
        ItemSelector.selectBestItem(purse).thenAccept(best -> {
            if (best == null) {
                System.err.println("[BazaarWorker] No item selected, stopping.");
                enabled = false;
                state = StateMachine.IDLE;
                return;
            }
            currentProductId = best.productId;
            itemName = best.displayName;
            calculatedAmount = ModConfig.get().debugMode ? 1 : best.purchasableAmount;
            System.out.printf("[BazaarWorker] start(): %s (id=%s) x%d%n",
                    itemName, currentProductId, calculatedAmount);
            MinecraftClient.getInstance().execute(() -> state = StateMachine.OPEN_BAZAAR);
        });
    }

    /**
     * Bypass item selection and start flipping a specific item immediately.
     * Called from the UI "START BOT" button.
     */
    public static void startWithItem(ItemScore item) {
        stop();
        currentProductId = item.productId;
        itemName = item.displayName;
        calculatedAmount = ModConfig.get().debugMode ? 1 : item.purchasableAmount;
        lastKnownPurse = at.fuji.utils.ScoreboardUtils.getPurseValue();
        enabled = true;
        state = StateMachine.OPEN_BAZAAR;
        System.out.printf("[BazaarWorker] startWithItem(): %s (id=%s) x%d%n",
                itemName, currentProductId, calculatedAmount);
    }

    public static void stop() {
        enabled = false;
        state = StateMachine.IDLE;
        System.out.println("[BazaarWorker] Stopped.");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String currentItemDisplay() {
        return itemName != null ? itemName : "—";
    }

    // ── Main tick ─────────────────────────────────────────────────────────────

    public static void update() {
        if (!enabled || client == null || client.player == null)
            return;

        if (state == StateMachine.IDLE && client.currentScreen == null && itemName != null) {
            long now = System.currentTimeMillis();
            if (now - lastOrderCheck > ORDER_CHECK_INTERVAL) {
                lastOrderCheck = now;
                pendingMenu = "__SELL_FLOW__";
                state = StateMachine.OPEN_YOUR_ORDERS;
            }
        }

        switch (state) {

            // ════════════════════════════════════════════════════════════════
            // BUY FLOW
            // ════════════════════════════════════════════════════════════════

            case OPEN_BAZAAR: {
                InputUtils.sendCommand("bazaar " + itemName);
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }
            case CLICK_SLOT: {
                screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                MenuUtils.clickSlot(pendingSlot, false);
                pendingSlot = -1;
                switch (pendingMenu) {
                    case "__ITEM__", "Create Buy Order", "Custom Amount", "Top Order +0.1" ->
                        state = StateMachine.AWAIT_SCREEN_CHANGE;
                    default -> state = StateMachine.SCAN_MENU;
                }
                break;
            }
            case TYPE_AMOUNT: {
                InputUtils.typeIntoScreen(String.valueOf(calculatedAmount));
                state = StateMachine.AWAIT_TYPING_FLUSH;
                break;
            }
            case CLICK_DONE: {
                InputUtils.pressEnter();
                state = StateMachine.AWAIT_ENTER_FLUSH;
                break;
            }
            case AWAIT_ITEM_CONFIRM: {
                if (!MenuUtils.isScreenOpen())
                    break;
                if (MenuUtils.scanForSlot("Buy Order", false) != -1)
                    state = StateMachine.CONFIRM_TRADE;
                break;
            }
            case CONFIRM_TRADE: {
                if (!MenuUtils.isScreenOpen())
                    break;
                int slot = MenuUtils.scanForSlot("Buy Order", false);
                if (slot != -1) {
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(slot, false);
                    state = StateMachine.AWAIT_ITEM_THEN_ESCAPE;
                }
                break;
            }
            case DONE: {
                lastOrderCheck = 0;
                state = StateMachine.IDLE;
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // WAIT STATES
            // ════════════════════════════════════════════════════════════════

            case AWAIT_SCREEN_OPEN: {
                if (!MenuUtils.isScreenOpen())
                    break;
                if ("__SELL_FLOW__".equals(pendingMenu)) {
                    pendingMenu = "";
                    state = StateMachine.SCAN_YOUR_ORDERS;
                } else if ("__CLAIM_FLOW__".equals(pendingMenu)) {
                    pendingMenu = "";
                    state = StateMachine.SCAN_CLAIM_ORDERS;
                } else {
                    state = StateMachine.SCAN_MENU;
                }
                break;
            }
            case AWAIT_SCREEN_CHANGE: {
                if ("Custom Amount".equals(pendingMenu)) {
                    if (InputUtils.isInputScreen())
                        state = StateMachine.TYPE_AMOUNT;
                } else if ("Top Order +0.1".equals(pendingMenu)) {
                    if (MenuUtils.isScreenOpen() && MenuUtils.screenTitleChanged(screenTitleBefore))
                        state = StateMachine.AWAIT_ITEM_CONFIRM;
                } else {
                    if (MenuUtils.isScreenOpen() && MenuUtils.screenTitleChanged(screenTitleBefore))
                        state = StateMachine.SCAN_MENU;
                }
                break;
            }
            case AWAIT_TYPING_FLUSH:
                state = StateMachine.CLICK_DONE;
                break;
            case AWAIT_ITEM_SELECT:
                break;
            case AWAIT_ITEM_THEN_ESCAPE: {
                if (MenuUtils.isScreenOpen() && MenuUtils.screenTitleChanged(screenTitleBefore)) {
                    InputUtils.pressEscape();
                    state = StateMachine.DONE;
                }
                break;
            }
            case AWAIT_ENTER_FLUSH:
                state = StateMachine.AWAIT_SIGN_CLOSE;
                break;
            case AWAIT_SIGN_CLOSE: {
                if (!InputUtils.isInputScreen() && MenuUtils.isScreenOpen())
                    state = StateMachine.SCAN_MENU;
                break;
            }
            case AWAIT_PRICE:
                break;

            case SCAN_MENU: {
                if (!MenuUtils.isScreenOpen())
                    break;
                for (String item : menuItems) {
                    int slot = MenuUtils.scanForSlot(item, false);
                    if (slot != -1) {
                        pendingSlot = slot;
                        pendingMenu = item;
                        if ("Create Buy Order".equals(item)) {
                            state = StateMachine.AWAIT_PRICE;
                            fetchPrice();
                        } else {
                            state = StateMachine.CLICK_SLOT;
                        }
                        return;
                    }
                }
                int itemSlot = MenuUtils.scanForSlot(itemName, true);
                if (itemSlot != -1) {
                    pendingSlot = itemSlot;
                    pendingMenu = "__ITEM__";
                    state = StateMachine.CLICK_SLOT;
                }
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // SELL / FLIP FLOW
            // ════════════════════════════════════════════════════════════════

            case OPEN_YOUR_ORDERS: {
                InputUtils.sendCommand("managebazaarorders");
                pendingMenu = "__SELL_FLOW__";
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }
            case SCAN_YOUR_ORDERS: {
                if (!MenuUtils.isScreenOpen())
                    break;
                if (itemName == null) {
                    InputUtils.pressEscape();
                    state = StateMachine.IDLE;
                    break;
                }
                ScreenHandler handler = client.player.currentScreenHandler;

                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    if (slot == null || !slot.hasStack())
                        continue;
                    ItemStack stack = slot.getStack();
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;
                    String slotName = LoreUtils.getSlotName(stack);
                    if (!slotName.startsWith("BUY "))
                        continue;
                    if (!slotName.toLowerCase().contains(itemName.toLowerCase()))
                        continue;
                    if (!LoreUtils.loreContains(stack, "100%"))
                        continue;
                    int amount = LoreUtils.parseAmount(stack, "Order amount:");
                    if (amount != calculatedAmount)
                        continue;
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(i, false);
                    state = StateMachine.AWAIT_FLIP_MENU;
                    return;
                }

                String playerName = client.player.getName().getString();
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    if (slot == null || !slot.hasStack())
                        continue;
                    ItemStack stack = slot.getStack();
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;
                    String slotName = LoreUtils.getSlotName(stack);
                    if (!slotName.startsWith("SELL "))
                        continue;
                    if (!LoreUtils.loreContains(stack, "100%"))
                        continue;
                    if (!LoreUtils.loreContains(stack, playerName))
                        continue;
                    MenuUtils.clickSlot(i, false);
                    state = StateMachine.AWAIT_CLAIM_CLOSE;
                    return;
                }

                InputUtils.pressEscape();
                state = StateMachine.IDLE;
                break;
            }
            case AWAIT_FLIP_MENU: {
                if (MenuUtils.isScreenOpen() && MenuUtils.screenTitleChanged(screenTitleBefore))
                    state = StateMachine.CLICK_FLIP;
                break;
            }
            case CLICK_FLIP: {
                if (!MenuUtils.isScreenOpen())
                    break;
                int slot = MenuUtils.scanForSlot("Flip", false);
                if (slot != -1) {
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(slot, false);
                    state = StateMachine.AWAIT_CUSTOM_MENU;
                }
                break;
            }
            case AWAIT_CUSTOM_MENU: {
                if (InputUtils.isInputScreen()) {
                    calculatedSellPrice = 0;
                    fetchSellPrice();
                    state = StateMachine.AWAIT_SELL_PRICE;
                    break;
                }
                if (MenuUtils.isScreenOpen() && MenuUtils.screenTitleChanged(screenTitleBefore))
                    state = StateMachine.CLICK_CUSTOM_SELL;
                break;
            }
            case CLICK_CUSTOM_SELL: {
                if (!MenuUtils.isScreenOpen())
                    break;
                int slot = MenuUtils.scanForSlot("Custom", false);
                if (slot != -1) {
                    MenuUtils.clickSlot(slot, false);
                    calculatedSellPrice = 0;
                    fetchSellPrice();
                    state = StateMachine.AWAIT_SELL_PRICE;
                }
                break;
            }
            case AWAIT_SELL_PRICE: {
                if (InputUtils.isInputScreen() && calculatedSellPrice > 0)
                    state = StateMachine.TYPE_SELL_PRICE;
                break;
            }
            case TYPE_SELL_PRICE: {
                InputUtils.typeIntoScreen(String.valueOf((long) calculatedSellPrice));
                calculatedSellPrice = 0;
                state = StateMachine.AWAIT_SELL_SIGN_CLOSE;
                break;
            }
            case AWAIT_SELL_SIGN_CLOSE: {
                if (!InputUtils.isInputScreen())
                    break;
                InputUtils.pressEnter();
                state = ModConfig.get().npcSellMode
                        ? StateMachine.NPC_OPEN_CANCEL
                        : StateMachine.DONE;
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // NPC CANCEL FLOW
            // ════════════════════════════════════════════════════════════════

            case NPC_OPEN_CANCEL: {
                if (MenuUtils.isScreenOpen() || InputUtils.isInputScreen())
                    break;
                InputUtils.sendCommand("managebazaarorders");
                state = StateMachine.NPC_AWAIT_CANCEL_SCREEN;
                break;
            }
            case NPC_AWAIT_CANCEL_SCREEN: {
                if (MenuUtils.isScreenOpen())
                    state = StateMachine.NPC_SCAN_SELL_ORDER;
                break;
            }
            case NPC_SCAN_SELL_ORDER: {
                if (!MenuUtils.isScreenOpen())
                    break;
                ScreenHandler handler = client.player.currentScreenHandler;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    if (slot == null || !slot.hasStack())
                        continue;
                    ItemStack stack = slot.getStack();
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;
                    String slotName = LoreUtils.getSlotName(stack);
                    if (!slotName.startsWith("SELL "))
                        continue;
                    if (!slotName.toLowerCase().contains(itemName.toLowerCase()))
                        continue;
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(i, false);
                    state = StateMachine.NPC_AWAIT_ORDER_DETAIL;
                    return;
                }
                InputUtils.pressEscape();
                state = StateMachine.START_FRESH;
                break;
            }
            case NPC_AWAIT_ORDER_DETAIL: {
                if (MenuUtils.isScreenOpen() && MenuUtils.screenTitleChanged(screenTitleBefore))
                    state = StateMachine.NPC_CLICK_CANCEL_ORDER;
                break;
            }
            case NPC_CLICK_CANCEL_ORDER: {
                if (!MenuUtils.isScreenOpen())
                    break;
                int slot = MenuUtils.scanForSlot("Cancel Order", false);
                if (slot == -1)
                    slot = MenuUtils.scanForSlot("Cancel", false);
                if (slot != -1) {
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(slot, false);
                    state = StateMachine.NPC_AWAIT_CANCELLED;
                } else {
                    InputUtils.pressEscape();
                    state = StateMachine.START_FRESH;
                }
                break;
            }
            case NPC_AWAIT_CANCELLED: {
                if (!MenuUtils.isScreenOpen()) {
                    state = StateMachine.START_FRESH;
                    break;
                }
                if (MenuUtils.screenTitleChanged(screenTitleBefore)) {
                    int confirm = MenuUtils.scanForSlot("Confirm", false);
                    if (confirm != -1)
                        MenuUtils.clickSlot(confirm, false);
                    else {
                        InputUtils.pressEscape();
                        state = StateMachine.START_FRESH;
                    }
                }
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // CLAIM FLOW
            // ════════════════════════════════════════════════════════════════

            case OPEN_CLAIM_ORDERS: {
                InputUtils.sendCommand("managebazaarorders");
                pendingMenu = "__CLAIM_FLOW__";
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }
            case SCAN_CLAIM_ORDERS: {
                if (!MenuUtils.isScreenOpen())
                    break;
                String pName = client.player.getName().getString();
                ScreenHandler handler = client.player.currentScreenHandler;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    if (slot == null || !slot.hasStack())
                        continue;
                    ItemStack stack = slot.getStack();
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;
                    String slotName = LoreUtils.getSlotName(stack);
                    if (!slotName.startsWith("SELL "))
                        continue;
                    if (!LoreUtils.loreContains(stack, "100%"))
                        continue;
                    if (!LoreUtils.loreContains(stack, pName))
                        continue;
                    MenuUtils.clickSlot(i, false);
                    state = StateMachine.AWAIT_CLAIM_CLOSE;
                    return;
                }
                InputUtils.pressEscape();
                state = StateMachine.IDLE;
                break;
            }
            case AWAIT_CLAIM_CLOSE: {
                if (!MenuUtils.isScreenOpen())
                    state = StateMachine.START_FRESH;
                break;
            }
            case START_FRESH: {
                new BazaarWorker().start();
                break;
            }
            case IDLE:
            default:
                break;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void fetchPrice() {
        System.out.println("[BazaarWorker] Amount: " + calculatedAmount);
        MinecraftClient.getInstance().execute(() -> state = StateMachine.CLICK_SLOT);
    }

    /**
     * Fetch sell price using currentProductId (the real API key, not a guess
     * from the display name). NPC mode uses 2× ask to guarantee the sell order
     * won't fill before we cancel it to retrieve the items.
     */
    private static void fetchSellPrice() {
        if (currentProductId == null) {
            System.err.println("[BazaarWorker] fetchSellPrice: currentProductId is null!");
            return;
        }
        System.out.println("[BazaarWorker] fetchSellPrice for productId: " + currentProductId);
        HypixelBazaarApi.getBuyOrderPrice(currentProductId).thenAccept(price -> {
            if (price <= 0) {
                System.err.println("[BazaarWorker] fetchSellPrice: bad price " + price
                        + " for " + currentProductId + " — staying in AWAIT_SELL_PRICE");
                return;
            }
            boolean npcMode = ModConfig.get().npcSellMode;
            double sell = npcMode ? price * 2.0 : price;
            System.out.printf("[BazaarWorker] fetchSellPrice result: %.0f%s%n",
                    sell, npcMode ? " (NPC 2×)" : "");
            MinecraftClient.getInstance().execute(() -> calculatedSellPrice = sell);
        });
    }
}