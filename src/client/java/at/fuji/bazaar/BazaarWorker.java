package at.fuji.bazaar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import java.util.List;
import java.util.Objects;

public class BazaarWorker {
    public enum StateMachine {
        IDLE,

        // --- Action states: execute once, transition immediately ---
        OPEN_BAZAAR,
        CLICK_SLOT,
        TYPE_AMOUNT,
        CLICK_DONE,
        CONFIRM_TRADE,
        DONE,

        // --- Waiting states: poll until condition is met, no side effects ---
        AWAIT_SCREEN_OPEN,
        AWAIT_SCREEN_CHANGE,
        AWAIT_SIGN_CLOSE,
        AWAIT_ENTER_FLUSH,
        AWAIT_ITEM_THEN_ESCAPE,
        AWAIT_ITEM_SELECT,

        // --- Sell flow (after buy order fills) ---
        OPEN_YOUR_ORDERS,
        CLICK_MANAGE_ORDERS,
        AWAIT_MANAGE_ORDERS,
        SCAN_YOUR_ORDERS,
        AWAIT_FLIP_MENU,
        CLICK_FLIP,
        AWAIT_CUSTOM_MENU,
        CLICK_CUSTOM_SELL,
        FETCH_SELL_PRICE,
        AWAIT_SELL_PRICE,
        TYPE_SELL_PRICE,
        AWAIT_SELL_SIGN_CLOSE,

        // --- Claim sell flow (after sell offer fills) ---
        OPEN_CLAIM_ORDERS,
        CLICK_MANAGE_ORDERS_CLAIM,
        AWAIT_MANAGE_ORDERS_CLAIM,
        SCAN_CLAIM_ORDERS,
        AWAIT_CLAIM_CLOSE,
        START_FRESH,

        AWAIT_ITEM_CONFIRM,
        AWAIT_TYPING_FLUSH,
        AWAIT_PRICE,
        SCAN_MENU,

        // --- API order polling ---
        POLL_ORDERS,
        AWAIT_POLL_RESULT,
    }

    // What the poller found that needs action
    private enum PendingAction {
        NONE,
        FLIP_BUY, // filled buy order → place sell offer
        CLAIM_SELL, // filled sell order → claim coins
    }

    private static StateMachine state = StateMachine.IDLE;
    private static PendingAction pendingAction = PendingAction.NONE;

    // How often to poll (ms). 30 s keeps us well under the 120 req/min limit.
    private static long lastOrderCheck = 0;
    private static final long ORDER_CHECK_INTERVAL = 30_000;

    static MinecraftClient client = MinecraftClient.getInstance();
    static String itemName = null;
    static List<String> menuBuyItems = List.of("Custom Amount", "Create Buy Order", "Top Order +0.1");
    static List<String> menuItems = menuBuyItems;
    static ScreenHandler handler;
    static String displayName = null;

    private static int pendingSlot = -1;
    private static double calculatedSellPrice = 0;
    private static String pendingMenu = "";
    private static String screenTitleBefore = null;
    private static int calculatedAmount = 1;

    // ── Control ───────────────────────────────────────────────────────────────

    public void start() {
        System.out.println("[BazaarWorker] Selecting best item...");
        state = StateMachine.AWAIT_ITEM_SELECT;
        double purse = at.fuji.utils.ScoreboardUtils.getPurseValue();
        ItemSelector.selectBestItem(purse).thenAccept(best -> {
            if (best == null) {
                System.err.println("[BazaarWorker] No item selected, aborting.");
                state = StateMachine.IDLE;
                return;
            }
            itemName = best.displayName;
            calculatedAmount = best.purchasableAmount;
            System.out.println("[BazaarWorker] Selected: " + best);
            System.out.println("[BazaarWorker] calculatedAmount=" + calculatedAmount
                    + " sellPrice=" + best.askPrice + " purse=" + purse);
            MinecraftClient.getInstance().execute(() -> state = StateMachine.OPEN_BAZAAR);
        });
    }

    public static void onBuyFilled(String filledItemName, int amount) {
        itemName = filledItemName;
        calculatedAmount = amount;
        System.out.println("[BazaarWorker] Buy filled — placing sell offer for " + amount + "x " + itemName);
        state = StateMachine.OPEN_YOUR_ORDERS;
    }

    public static void onSellFilled(String filledItemName) {
        itemName = filledItemName;
        System.out.println("[BazaarWorker] Sell filled — claiming coins for " + itemName);
        state = StateMachine.OPEN_CLAIM_ORDERS;
    }

    // ── Main tick ─────────────────────────────────────────────────────────────

    public static void update() {
        if (client == null || client.player == null)
            return;
        handler = client.player.currentScreenHandler;

        // ── API polling — only when idle and no screen is open ───────────────
        if (state == StateMachine.IDLE && client.currentScreen == null) {
            long now = System.currentTimeMillis();
            if (now - lastOrderCheck > ORDER_CHECK_INTERVAL) {
                lastOrderCheck = now;
                state = StateMachine.POLL_ORDERS;
            }
        }

        switch (state) {

            // ════════════════════════════════════════════════════════════════
            // API POLLING
            // ════════════════════════════════════════════════════════════════

            case POLL_ORDERS: {
                state = StateMachine.AWAIT_POLL_RESULT;
                String uuid = client.player.getUuidAsString();
                HypixelOrderChecker.fetchOrders(uuid).thenAccept(orders -> {
                    for (HypixelOrderChecker.Order order : orders) {
                        if (!order.isFilled())
                            continue;

                        if ("BUY".equals(order.type)) {
                            System.out.println("[BazaarWorker] Filled BUY order found: "
                                    + order.productId + " x" + order.amount);
                            MinecraftClient.getInstance().execute(() -> {
                                itemName = HypixelBazaarApi.getItemName(order.productId);
                                calculatedAmount = order.amount;
                                pendingAction = PendingAction.FLIP_BUY;
                                state = StateMachine.OPEN_YOUR_ORDERS;
                            });
                            return; // handle one order per poll cycle
                        }

                        if ("SELL".equals(order.type)) {
                            System.out.println("[BazaarWorker] Filled SELL order found: "
                                    + order.productId + " x" + order.amount);
                            MinecraftClient.getInstance().execute(() -> {
                                itemName = HypixelBazaarApi.getItemName(order.productId);
                                pendingAction = PendingAction.CLAIM_SELL;
                                state = StateMachine.OPEN_CLAIM_ORDERS;
                            });
                            return;
                        }
                    }
                    // Nothing filled — go back to idle
                    MinecraftClient.getInstance().execute(() -> state = StateMachine.IDLE);
                });
                break;
            }

            case AWAIT_POLL_RESULT: {
                // Spinning — async callback drives the next transition
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // ACTION STATES
            // ════════════════════════════════════════════════════════════════

            case OPEN_BAZAAR: {
                displayName = HypixelBazaarApi.getItemName(itemName);
                client.player.networkHandler.sendChatCommand("bazaar " + displayName);
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }

            case CLICK_SLOT: {
                screenTitleBefore = getScreenTitle();
                doClickSlot(pendingSlot);
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
                typeIntoScreen(String.valueOf(calculatedAmount));
                state = StateMachine.AWAIT_TYPING_FLUSH;
                break;
            }

            case CLICK_DONE: {
                pressEnter();
                state = StateMachine.AWAIT_ENTER_FLUSH;
                break;
            }

            case AWAIT_ITEM_CONFIRM: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;
                    if (stack.getName().getString().contains("Buy Order")) {
                        System.out.println("[BazaarWorker] Found item on confirmation screen.");
                        state = StateMachine.CONFIRM_TRADE;
                        return;
                    }
                }
                break;
            }

            case CONFIRM_TRADE: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;
                    if (stack.getName().getString().contains("Buy Order")) {
                        System.out.println("[BazaarWorker] Clicking item to confirm at slot " + i);
                        screenTitleBefore = getScreenTitle();
                        doClickSlot(i);
                        state = StateMachine.AWAIT_ITEM_THEN_ESCAPE;
                        return;
                    }
                }
                break;
            }

            case DONE: {
                System.out.println("[BazaarWorker] Trade complete.");
                pendingAction = PendingAction.NONE;
                state = StateMachine.IDLE;
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // WAITING STATES
            // ════════════════════════════════════════════════════════════════

            case AWAIT_SCREEN_OPEN: {
                if (client.currentScreen == null)
                    break;
                if ("__SELL_FLOW__".equals(pendingMenu)) {
                    pendingMenu = "";
                    state = StateMachine.CLICK_MANAGE_ORDERS;
                } else if ("__CLAIM_FLOW__".equals(pendingMenu)) {
                    pendingMenu = "";
                    state = StateMachine.CLICK_MANAGE_ORDERS_CLAIM;
                } else {
                    state = StateMachine.SCAN_MENU;
                }
                break;
            }

            case AWAIT_SCREEN_CHANGE: {
                if ("Custom Amount".equals(pendingMenu)) {
                    if (isInputScreen())
                        state = StateMachine.TYPE_AMOUNT;
                } else if ("Top Order +0.1".equals(pendingMenu)) {
                    if (client.currentScreen != null
                            && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                        state = StateMachine.AWAIT_ITEM_CONFIRM;
                    }
                } else {
                    if (client.currentScreen != null
                            && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                        state = StateMachine.SCAN_MENU;
                    }
                }
                break;
            }

            case AWAIT_TYPING_FLUSH: {
                state = StateMachine.CLICK_DONE;
                break;
            }

            case AWAIT_ITEM_SELECT: {
                break;
            }

            case AWAIT_ITEM_THEN_ESCAPE: {
                if (client.currentScreen != null
                        && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                    pressEscape();
                    state = StateMachine.DONE;
                }
                break;
            }

            case AWAIT_ENTER_FLUSH: {
                state = StateMachine.AWAIT_SIGN_CLOSE;
                break;
            }

            case AWAIT_SIGN_CLOSE: {
                if (!isInputScreen() && client.currentScreen != null) {
                    state = StateMachine.SCAN_MENU;
                }
                break;
            }

            case AWAIT_PRICE: {
                break;
            }

            case SCAN_MENU: {
                if (client.currentScreen == null)
                    break;

                // Pass 1: action buttons
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;

                    String name = stack.getName().getString();
                    for (String menuItem : menuItems) {
                        if (name.contains(menuItem)) {
                            System.out.println("[BazaarWorker] Found action: " + menuItem + " at slot " + i);
                            pendingSlot = i;
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
                }

                // Pass 2: item icon
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;

                    String name = stack.getName().getString();
                    if (name.equalsIgnoreCase(itemName)) {
                        System.out.println("[BazaarWorker] Found item slot: " + name + " at slot " + i);
                        pendingSlot = i;
                        pendingMenu = "__ITEM__";
                        state = StateMachine.CLICK_SLOT;
                        return;
                    }
                }
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // SELL FLOW (flip filled buy order)
            // ════════════════════════════════════════════════════════════════

            case OPEN_YOUR_ORDERS: {
                client.player.networkHandler.sendChatCommand("bazaar");
                pendingMenu = "__SELL_FLOW__";
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }

            case CLICK_MANAGE_ORDERS: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getName().getString().contains("Manage Orders")) {
                        System.out.println("[BazaarWorker] Clicking Manage Orders at slot " + i);
                        screenTitleBefore = getScreenTitle();
                        doClickSlot(i);
                        state = StateMachine.AWAIT_MANAGE_ORDERS;
                        return;
                    }
                }
                break;
            }

            case AWAIT_MANAGE_ORDERS: {
                if (client.currentScreen != null
                        && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                    state = StateMachine.SCAN_YOUR_ORDERS;
                }
                break;
            }

            case SCAN_YOUR_ORDERS: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;

                    String slotName = stack.getName().getString();
                    System.out.println("[BazaarWorker] Scanning slot " + i + ": '" + slotName + "' vs itemName: '"
                            + itemName + "'");

                    if (slotName.toLowerCase().contains(itemName.toLowerCase())) {
                        System.out.println("[BazaarWorker] Found order slot: " + slotName + " at " + i);
                        screenTitleBefore = getScreenTitle();
                        doClickSlot(i);
                        state = StateMachine.AWAIT_FLIP_MENU;
                        return;
                    }
                }
                break;
            }

            case AWAIT_FLIP_MENU: {
                if (client.currentScreen != null
                        && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                    state = StateMachine.CLICK_FLIP;
                }
                break;
            }

            case CLICK_FLIP: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getName().getString().contains("Flip")) {
                        System.out.println("[BazaarWorker] Clicking Flip at slot " + i);
                        screenTitleBefore = getScreenTitle();
                        doClickSlot(i);
                        state = StateMachine.AWAIT_CUSTOM_MENU;
                        return;
                    }
                }
                break;
            }

            case AWAIT_CUSTOM_MENU: {
                if (client.currentScreen != null
                        && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                    state = StateMachine.CLICK_CUSTOM_SELL;
                }
                break;
            }

            case CLICK_CUSTOM_SELL: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getName().getString().contains("Custom")) {
                        System.out.println("[BazaarWorker] Clicking Custom at slot " + i);
                        doClickSlot(i);
                        state = StateMachine.FETCH_SELL_PRICE;
                        return;
                    }
                }
                break;
            }

            case FETCH_SELL_PRICE: {
                state = StateMachine.AWAIT_SELL_PRICE;
                String productId = HypixelBazaarApi.toProductId(itemName);
                HypixelBazaarApi.getBuyOrderPrice(productId).thenAccept(lowestSell -> {
                    calculatedSellPrice = lowestSell;
                    System.out.println("[BazaarWorker] Sell price: " + calculatedSellPrice);
                    MinecraftClient.getInstance().execute(() -> state = StateMachine.TYPE_SELL_PRICE);
                });
                break;
            }

            case AWAIT_SELL_PRICE: {
                break;
            }

            case TYPE_SELL_PRICE: {
                if (!isInputScreen())
                    break;
                typeIntoScreen(String.valueOf((long) calculatedSellPrice));
                state = StateMachine.AWAIT_SELL_SIGN_CLOSE;
                break;
            }

            case AWAIT_SELL_SIGN_CLOSE: {
                if (!isInputScreen())
                    break;
                pressEnter();
                state = StateMachine.DONE;
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // CLAIM FLOW (claim filled sell order)
            // ════════════════════════════════════════════════════════════════

            case OPEN_CLAIM_ORDERS: {
                client.player.networkHandler.sendChatCommand("bazaar");
                pendingMenu = "__CLAIM_FLOW__";
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }

            case CLICK_MANAGE_ORDERS_CLAIM: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getName().getString().contains("Manage Orders")) {
                        System.out.println("[BazaarWorker] (Claim) Clicking Manage Orders at slot " + i);
                        screenTitleBefore = getScreenTitle();
                        doClickSlot(i);
                        state = StateMachine.AWAIT_MANAGE_ORDERS_CLAIM;
                        return;
                    }
                }
                break;
            }

            case AWAIT_MANAGE_ORDERS_CLAIM: {
                if (client.currentScreen != null
                        && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                    state = StateMachine.SCAN_CLAIM_ORDERS;
                }
                break;
            }

            case SCAN_CLAIM_ORDERS: {
                if (client.currentScreen == null)
                    break;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;

                    String slotName = stack.getName().getString();
                    System.out.println("[BazaarWorker] (Claim) Scanning slot " + i + ": " + slotName);

                    if (slotName.toLowerCase().contains(itemName.toLowerCase())) {
                        System.out.println("[BazaarWorker] Claiming sell order at slot " + i);
                        doClickSlot(i);
                        state = StateMachine.AWAIT_CLAIM_CLOSE;
                        return;
                    }
                }
                break;
            }

            case AWAIT_CLAIM_CLOSE: {
                if (client.currentScreen == null) {
                    state = StateMachine.START_FRESH;
                }
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

    // ── Price fetch ───────────────────────────────────────────────────────────

    private static void fetchPrice() {
        System.out.println("[BazaarWorker] Amount to order: " + calculatedAmount);
        MinecraftClient.getInstance().execute(() -> state = StateMachine.CLICK_SLOT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isInputScreen() {
        if (client.currentScreen == null)
            return false;
        return client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.AnvilScreen
                || client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.SignEditScreen;
    }

    private static String getScreenTitle() {
        if (client.currentScreen == null)
            return null;
        return client.currentScreen.getTitle().getString();
    }

    private static void doClickSlot(int slotIndex) {
        if (slotIndex == -1 || handler == null
                || client.interactionManager == null || client.player == null)
            return;
        System.out.println("[BazaarWorker] Clicking slot " + slotIndex);
        client.interactionManager.clickSlot(
                handler.syncId, slotIndex, 0, SlotActionType.PICKUP, client.player);
    }

    private static void typeIntoScreen(String text) {
        if (client.currentScreen == null)
            return;
        for (char c : text.toCharArray()) {
            net.minecraft.client.input.CharInput input = new net.minecraft.client.input.CharInput(c, 0);
            client.currentScreen.charTyped(input);
        }
        System.out.println("[BazaarWorker] Typed: " + text);
    }

    private static void pressEnter() {
        if (client.currentScreen == null)
            return;
        client.currentScreen.keyPressed(
                new net.minecraft.client.input.KeyInput(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0));
        System.out.println("[BazaarWorker] Pressed Enter.");
    }

    private static void pressEscape() {
        if (client.currentScreen != null) {
            client.currentScreen.close();
            System.out.println("[BazaarWorker] Closed screen.");
        }
    }
}