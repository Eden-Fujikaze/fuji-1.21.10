package at.fuji.bazaar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import at.fuji.utils.MenuUtils;
import at.fuji.ModConfig;
import at.fuji.utils.InputUtils;
import at.fuji.utils.LoreUtils;
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
    }

    private static StateMachine state = StateMachine.IDLE;
    private static boolean enabled = false;
    public static volatile double lastKnownPurse = 0;

    private static long lastOrderCheck = 0;
    private static final long ORDER_CHECK_INTERVAL = 15_000;

    static MinecraftClient client = MinecraftClient.getInstance();
    static String itemName = null;
    static List<String> menuBuyItems = List.of("Custom Amount", "Create Buy Order", "Top Order +0.1");
    static List<String> menuItems = menuBuyItems;
    static String displayName = null;

    private static int pendingSlot = -1;
    private static double calculatedSellPrice = 0;
    private static String pendingMenu = "";
    private static String screenTitleBefore = null;
    private static int calculatedAmount = 1;

    // ── Control ───────────────────────────────────────────────────────────────

    public void start() {
        enabled = true;
        System.out.println("[BazaarWorker] Started.");
        state = StateMachine.AWAIT_ITEM_SELECT;
        double purse = at.fuji.utils.ScoreboardUtils.getPurseValue();
        lastKnownPurse = purse;
        ItemSelector.selectBestItem(purse).thenAccept(best -> {
            if (best == null) {
                System.err.println("[BazaarWorker] No item selected, aborting.");
                state = StateMachine.IDLE;
                return;
            }
            itemName = best.displayName;
            int amount = ModConfig.get().debugMode ? 1 : best.purchasableAmount;
            calculatedAmount = amount;
            System.out.println("[BazaarWorker] Selected: " + best.displayName + " x" + calculatedAmount);
            MinecraftClient.getInstance().execute(() -> state = StateMachine.OPEN_BAZAAR);
        });
    }

    public static void stop() {
        enabled = false;
        state = StateMachine.IDLE;
        System.out.println("[BazaarWorker] Stopped.");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // ── Main tick ─────────────────────────────────────────────────────────────

    public static void update() {
        if (!enabled)
            return;
        if (client == null || client.player == null)
            return;

        // Timer-based order polling when idle — only after itemName is known
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
            // ACTION STATES
            // ════════════════════════════════════════════════════════════════

            case OPEN_BAZAAR: {
                displayName = HypixelBazaarApi.getItemName(itemName);
                InputUtils.sendCommand("bazaar " + displayName);
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }

            case CLICK_SLOT: {
                screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                MenuUtils.clickSlot(pendingSlot, false);
                pendingSlot = -1;
                switch (pendingMenu) {
                    case "__ITEM__":
                    case "Create Buy Order":
                    case "Custom Amount":
                    case "Top Order +0.1":
                        state = StateMachine.AWAIT_SCREEN_CHANGE;
                        break;
                    default:
                        state = StateMachine.SCAN_MENU;
                        break;
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
                int slot = MenuUtils.scanForSlot("Buy Order", false);
                System.out.println("[BazaarWorker] AWAIT_ITEM_CONFIRM scanForSlot='Buy Order' returned " + slot);
                if (slot != -1)
                    state = StateMachine.CONFIRM_TRADE;
                break;
            }

            case CONFIRM_TRADE: {
                if (!MenuUtils.isScreenOpen())
                    break;
                int slot = MenuUtils.scanForSlot("Buy Order", false);
                if (slot != -1) {
                    System.out.println("[BazaarWorker] Confirming at slot " + slot);
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(slot, false);
                    state = StateMachine.AWAIT_ITEM_THEN_ESCAPE;
                }
                break;
            }

            case DONE: {
                System.out.println("[BazaarWorker] Trade complete.");
                lastOrderCheck = 0; // poll immediately next tick
                state = StateMachine.IDLE;
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // WAITING STATES
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

                // Pass 1: action buttons
                for (String menuItem : menuItems) {
                    int slot = MenuUtils.scanForSlot(menuItem, false);
                    if (slot != -1) {
                        System.out.println("[BazaarWorker] Found action: " + menuItem + " at slot " + slot);
                        pendingSlot = slot;
                        pendingMenu = menuItem;
                        if ("Create Buy Order".equals(menuItem)) {
                            state = StateMachine.AWAIT_PRICE;
                            fetchPrice();
                        } else {
                            state = StateMachine.CLICK_SLOT;
                        }
                        return;
                    }
                }

                // Pass 2: item icon
                int itemSlot = MenuUtils.scanForSlot(itemName, true);
                if (itemSlot != -1) {
                    System.out.println("[BazaarWorker] Found item at slot " + itemSlot);
                    pendingSlot = itemSlot;
                    pendingMenu = "__ITEM__";
                    state = StateMachine.CLICK_SLOT;
                }
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // SELL FLOW
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
                // itemName may be null if the timer fires before selectBestItem completes
                if (itemName == null) {
                    InputUtils.pressEscape();
                    state = StateMachine.IDLE;
                    break;
                }
                ScreenHandler handler = client.player.currentScreenHandler;

                // Pass 1: look for filled BUY order
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
                    System.out.println("[BazaarWorker] Filled BUY: " + itemName + " x" + amount);
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(i, false);
                    state = StateMachine.AWAIT_FLIP_MENU;
                    return;
                }

                // Pass 2: look for filled SELL order to claim
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
                    System.out.println("[BazaarWorker] Filled SELL found, claiming: " + slotName);
                    MenuUtils.clickSlot(i, false);
                    state = StateMachine.AWAIT_CLAIM_CLOSE;
                    return;
                }

                // Nothing found — close and idle
                System.out.println("[BazaarWorker] No filled orders found.");
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
                System.out.println("[BazaarWorker] CLICK_FLIP scanForSlot='Flip' returned " + slot);
                if (slot != -1) {
                    System.out.println("[BazaarWorker] Clicking Flip at slot " + slot);
                    screenTitleBefore = MenuUtils.getCurrentScreenTitle();
                    MenuUtils.clickSlot(slot, false);
                    state = StateMachine.AWAIT_CUSTOM_MENU;
                }
                break;
            }

            case AWAIT_CUSTOM_MENU: {
                // Flip opens sign directly — detect sign and skip CLICK_CUSTOM_SELL
                if (InputUtils.isInputScreen()) {
                    System.out.println("[BazaarWorker] Sign opened directly after Flip, fetching sell price.");
                    calculatedSellPrice = 0;
                    fetchSellPrice();
                    state = StateMachine.AWAIT_SELL_PRICE;
                    break;
                }
                // Otherwise wait for submenu title change then scan for Custom button
                if (MenuUtils.isScreenOpen() && MenuUtils.screenTitleChanged(screenTitleBefore))
                    state = StateMachine.CLICK_CUSTOM_SELL;
                break;
            }

            case CLICK_CUSTOM_SELL: {
                if (!MenuUtils.isScreenOpen())
                    break;
                int slot = MenuUtils.scanForSlot("Custom", false);
                System.out.println("[BazaarWorker] CLICK_CUSTOM_SELL scanForSlot='Custom' returned " + slot);
                if (slot != -1) {
                    System.out.println("[BazaarWorker] Clicking Custom at slot " + slot);
                    MenuUtils.clickSlot(slot, false);
                    calculatedSellPrice = 0;
                    fetchSellPrice();
                    state = StateMachine.AWAIT_SELL_PRICE;
                }
                break;
            }

            case AWAIT_SELL_PRICE: {
                System.out.println("[BazaarWorker] AWAIT_SELL_PRICE isInputScreen=" + InputUtils.isInputScreen()
                        + " sellPrice=" + calculatedSellPrice);
                if (InputUtils.isInputScreen() && calculatedSellPrice > 0)
                    state = StateMachine.TYPE_SELL_PRICE;
                break;
            }

            case TYPE_SELL_PRICE: {
                InputUtils.typeIntoScreen(String.valueOf((long) calculatedSellPrice));
                System.out.println("[BazaarWorker] Typed sell price: " + (long) calculatedSellPrice);
                calculatedSellPrice = 0;
                state = StateMachine.AWAIT_SELL_SIGN_CLOSE;
                break;
            }

            case AWAIT_SELL_SIGN_CLOSE: {
                if (!InputUtils.isInputScreen())
                    break;
                InputUtils.pressEnter();
                state = StateMachine.DONE;
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
                String playerName = client.player.getName().getString();
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
                    if (!LoreUtils.loreContains(stack, playerName))
                        continue;
                    System.out.println("[BazaarWorker] Claiming SELL: " + slotName);
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
        System.out.println("[BazaarWorker] Amount to order: " + calculatedAmount);
        MinecraftClient.getInstance().execute(() -> state = StateMachine.CLICK_SLOT);
    }

    private static void fetchSellPrice() {
        String productId = HypixelBazaarApi.toProductId(itemName);
        System.out.println("[BazaarWorker] Fetching sell price for productId: " + productId);
        HypixelBazaarApi.getBuyOrderPrice(productId).thenAccept(price -> {
            System.out.println("[BazaarWorker] Sell price fetched: " + price);
            MinecraftClient.getInstance().execute(() -> calculatedSellPrice = price);
        });
    }
}