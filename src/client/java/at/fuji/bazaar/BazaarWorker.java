package at.fuji.bazaar;

import at.fuji.utils.ScoreboardUtils;

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
        OPEN_BAZAAR, // send /bazaar command
        CLICK_SLOT, // click the pending slot exactly once
        TYPE_AMOUNT, // type calculated amount into screen
        CLICK_DONE, // click the Done button (slot 2) exactly once
        CONFIRM_TRADE, // close screen to confirm
        DONE,

        // --- Waiting states: poll until condition is met, no side effects ---
        AWAIT_SCREEN_OPEN, // wait for any screen to appear
        AWAIT_SCREEN_CHANGE, // wait for screen title to change (menu nav)
        AWAIT_SIGN_CLOSE, // wait for sign input screen to close after Enter
        AWAIT_ENTER_FLUSH, // one tick after Enter before checking screen
        AWAIT_ITEM_THEN_ESCAPE, // wait for submenu after item click, then escape
        AWAIT_ITEM_SELECT, // wait for ItemSelector async result
        // --- Sell flow (after buy order fills) ---
        OPEN_YOUR_ORDERS, // /bazaar → Your Orders menu
        SCAN_YOUR_ORDERS, // find the filled item slot
        AWAIT_FLIP_MENU, // wait for flip submenu to open
        CLICK_FLIP, // click the Flip button
        AWAIT_CUSTOM_MENU, // wait for custom amount screen
        CLICK_CUSTOM_SELL, // click Custom
        FETCH_SELL_PRICE, // fetch lowest sell price from API
        AWAIT_SELL_PRICE, // wait for async price fetch
        TYPE_SELL_PRICE, // type the sell price into sign
        AWAIT_SELL_SIGN_CLOSE, // wait for sign to close after Enter
        // --- Claim sell flow (after sell offer fills) ---
        OPEN_CLAIM_ORDERS, // /bazaar → Your Orders to claim coins
        SCAN_CLAIM_ORDERS, // find the filled sell slot
        AWAIT_CLAIM_CLOSE, // wait for menu to close after click
        START_FRESH, // close menu and kick off fresh item selection
        AWAIT_ITEM_CONFIRM, // wait to find item on confirmation screen before escaping
        AWAIT_TYPING_FLUSH, // wait one tick after typing before clicking Done
        AWAIT_PRICE, // wait for async price fetch to complete
        SCAN_MENU, // scan inventory for target slot
    }

    private static StateMachine state = StateMachine.IDLE;

    static MinecraftClient client = MinecraftClient.getInstance();
    // Set by ItemSelector before the bot starts — null means not yet selected
    static String itemName = null;
    static List<String> menuBuyItems = List.of("Custom Amount", "Create Buy Order", "Top Order +0.1");
    static List<String> menuItems = menuBuyItems;
    static ScreenHandler handler;

    private static int pendingSlot = -1;
    private static double calculatedSellPrice = 0;
    private static String pendingMenu = "";
    private static String screenTitleBefore = null;
    private static int calculatedAmount = 1;

    // ── Persistence ──────────────────────────────────────────────────────────

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
            System.out.println("[BazaarWorker] calculatedAmount=" + calculatedAmount + " sellPrice=" + best.askPrice
                    + " purse=" + purse);
            MinecraftClient.getInstance().execute(() -> state = StateMachine.OPEN_BAZAAR);
        });
    }

    /**
     * Called when a buy order fills — skip re-selection and go straight to
     * placing a sell offer for the same item at the current best sell price.
     */
    public static void onBuyFilled(String filledItemName, int amount) {
        itemName = filledItemName;
        calculatedAmount = amount;
        System.out.println("[BazaarWorker] Buy filled — placing sell offer for " + amount + "x " + itemName);
        state = StateMachine.OPEN_YOUR_ORDERS;
    }

    /**
     * Called when a sell offer fills — open Your Orders, click the item to claim
     * coins,
     * close the menu, then restart with fresh item selection.
     */
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

        switch (state) {

            // ════════════════════════════════════════════════════════════════
            // ACTION STATES — each runs exactly once then transitions out
            // ════════════════════════════════════════════════════════════════

            case OPEN_BAZAAR: {
                client.player.networkHandler.sendChatCommand("bazaar " + itemName);
                state = StateMachine.AWAIT_SCREEN_OPEN;
                break;
            }

            case CLICK_SLOT: {
                // Snapshot the current screen title so AWAIT_SCREEN_CHANGE knows what changed
                screenTitleBefore = getScreenTitle();
                doClickSlot(pendingSlot);
                pendingSlot = -1;

                switch (pendingMenu) {
                    case "__ITEM__":
                        // Opened item submenu — wait for the new menu to load
                        state = StateMachine.AWAIT_SCREEN_CHANGE;
                        break;
                    case "Create Buy Order":
                        // Clicking Buy Order opens a new menu containing "Custom Amount" — go scan for
                        // it
                        state = StateMachine.AWAIT_SCREEN_CHANGE;
                        break;
                    case "Custom Amount":
                        // Clicking Custom Amount opens the SignEditScreen — wait for it
                        state = StateMachine.AWAIT_SCREEN_CHANGE;
                        break;
                    case "Top Order +0.1":
                        // Wait for the confirmation screen, scan for the item, then escape
                        state = StateMachine.AWAIT_SCREEN_CHANGE;
                        break;
                    default:
                        state = StateMachine.SCAN_MENU;
                        break;
                }
                break;
            }

            case TYPE_AMOUNT: {
                // Type the amount into the open screen
                typeIntoScreen(String.valueOf(calculatedAmount));
                // Give the game one tick to flush the characters into the screen's
                // text field before we click Done — otherwise Done fires mid-type
                state = StateMachine.AWAIT_TYPING_FLUSH;
                break;
            }

            case CLICK_DONE: {
                // Press Enter to confirm, then idle one tick before checking screen state
                pressEnter();
                state = StateMachine.AWAIT_ENTER_FLUSH;
                break;
            }

            case AWAIT_ITEM_CONFIRM: {
                // Scan for the item on the confirmation screen, then escape
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
                        System.out.println("[BazaarWorker] Found item on confirmation screen, pressing escape.");
                        state = StateMachine.CONFIRM_TRADE;
                        return;
                    }
                }
                break;
            }

            case CONFIRM_TRADE: {
                // Top Order +0.1 was clicked — scan the current menu for the item icon and
                // click it
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
                break; // item not found yet, retry next tick
            }

            case DONE: {
                System.out.println("[BazaarWorker] Trade complete.");
                state = StateMachine.IDLE;
                break;
            }

            // ════════════════════════════════════════════════════════════════
            // WAITING STATES — pure polls, zero side effects
            // ════════════════════════════════════════════════════════════════

            case AWAIT_SCREEN_OPEN: {
                if (client.currentScreen == null)
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
                    // Clicking Custom Amount opens the SignEditScreen — wait for it specifically
                    if (isInputScreen()) {
                        state = StateMachine.TYPE_AMOUNT;
                    }
                } else if ("Top Order +0.1".equals(pendingMenu)) {
                    // After clicking Top Order, wait for confirmation screen then scan for item
                    if (client.currentScreen != null && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                        state = StateMachine.AWAIT_ITEM_CONFIRM;
                    }
                } else {
                    // All other clicks (item icon, Buy Order) just open a new inventory menu
                    if (client.currentScreen != null && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                        state = StateMachine.SCAN_MENU;
                    }
                }
                break;
            }

            case AWAIT_TYPING_FLUSH: {
                // One tick has passed since typeIntoScreen — safe to click Done now
                state = StateMachine.CLICK_DONE;
                break;
            }

            case AWAIT_ITEM_SELECT: {
                // Spinning — ItemSelector callback will set state to OPEN_BAZAAR when ready
                break;
            }

            case AWAIT_ITEM_THEN_ESCAPE: {
                // Wait for screen to change after clicking item, then press escape
                if (client.currentScreen != null && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
                    pressEscape();
                    state = StateMachine.DONE;
                }
                break;
            }

            case AWAIT_ENTER_FLUSH: {
                // One tick has passed since Enter — now wait for sign to fully close
                state = StateMachine.AWAIT_SIGN_CLOSE;
                break;
            }

            case AWAIT_SIGN_CLOSE: {
                // Wait until the sign screen is gone and a new inventory menu has opened
                if (!isInputScreen() && client.currentScreen != null) {
                    state = StateMachine.SCAN_MENU;
                }
                break;
            }

            case AWAIT_PRICE: {
                // Async fetch is in flight; the thenAccept callback will set state when ready
                break;
            }

            case SCAN_MENU: {
                if (client.currentScreen == null)
                    break;

                // Pass 1: action buttons take priority — scan all slots before falling back
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;

                    String displayName = stack.getName().getString();

                    for (String menuItem : menuItems) {
                        if (displayName.contains(menuItem)) {
                            System.out.println("[BazaarWorker] Found action: " + menuItem + " at slot " + i);
                            pendingSlot = i;
                            pendingMenu = menuItem;

                            if ("Create Buy Order".equals(menuItem)) {
                                // Fetch price now; after clicking Buy Order we scan for Custom Amount
                                state = StateMachine.AWAIT_PRICE;
                                fetchPrice();
                            } else {
                                // Custom Amount and Top Order +0.1 — click straight away
                                state = StateMachine.CLICK_SLOT;
                            }
                            return;
                        }
                    }
                }

                // Pass 2: no action buttons found — click the item icon to open its submenu
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;

                    String displayName = stack.getName().getString();

                    if (displayName.equalsIgnoreCase(itemName)) {
                        System.out.println("[BazaarWorker] Found item slot: " + displayName + " at slot " + i);
                        pendingSlot = i;
                        pendingMenu = "__ITEM__";
                        state = StateMachine.CLICK_SLOT;
                        return;
                    }
                }
                break;
            }

            case OPEN_YOUR_ORDERS: {
                // Open bazaar — this shows "Your Orders" when you have active orders
                client.player.networkHandler.sendChatCommand("bazaar");
                state = StateMachine.AWAIT_SCREEN_OPEN;
                // Override: after screen opens go to SCAN_YOUR_ORDERS not SCAN_MENU
                pendingMenu = "__SELL_FLOW__";
                break;
            }

            case SCAN_YOUR_ORDERS: {
                if (client.currentScreen == null)
                    break;
                // Find the slot containing the item we just bought
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty())
                        continue;
                    if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                        continue;
                    if (stack.getName().getString().contains(itemName)) {
                        System.out.println(
                                "[BazaarWorker] Found order slot: " + stack.getName().getString() + " at " + i);
                        screenTitleBefore = getScreenTitle();
                        doClickSlot(i);
                        state = StateMachine.AWAIT_FLIP_MENU;
                        return;
                    }
                }
                break;
            }

            case AWAIT_FLIP_MENU: {
                // Wait for the flip submenu to open (screen title changes)
                if (client.currentScreen != null && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
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
                if (client.currentScreen != null && !Objects.equals(getScreenTitle(), screenTitleBefore)) {
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
                    // getBuyOrderPrice returns buyPrice (lowest ask = lowest sell offer)
                    calculatedSellPrice = lowestSell;
                    System.out.println("[BazaarWorker] Sell price: " + calculatedSellPrice);
                    MinecraftClient.getInstance().execute(() -> state = StateMachine.TYPE_SELL_PRICE);
                });
                break;
            }

            case AWAIT_SELL_PRICE: {
                // Spinning — async callback drives transition
                break;
            }

            case TYPE_SELL_PRICE: {
                if (!isInputScreen())
                    break; // wait for sign to appear
                typeIntoScreen(String.valueOf((long) calculatedSellPrice));
                // AWAIT_TYPING_FLUSH gives one tick before Enter
                state = StateMachine.AWAIT_SELL_SIGN_CLOSE;
                break;
            }

            case AWAIT_SELL_SIGN_CLOSE: {
                // Wait one tick for typing to flush, then Enter
                if (!isInputScreen())
                    break;
                pressEnter();
                state = StateMachine.DONE;
                break;
            }

            case OPEN_CLAIM_ORDERS: {
                client.player.networkHandler.sendChatCommand("bazaar");
                pendingMenu = "__CLAIM_FLOW__";
                state = StateMachine.AWAIT_SCREEN_OPEN;
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
                    if (stack.getName().getString().contains(itemName)) {
                        System.out.println("[BazaarWorker] Claiming sell order at slot " + i);
                        doClickSlot(i);
                        state = StateMachine.AWAIT_CLAIM_CLOSE;
                        return;
                    }
                }
                break;
            }

            case AWAIT_CLAIM_CLOSE: {
                // Wait for the screen to close after clicking the order
                if (client.currentScreen == null) {
                    state = StateMachine.START_FRESH;
                }
                break;
            }

            case START_FRESH: {
                // Coins are now in purse — restart with fresh item selection
                new BazaarWorker().start();
                break;
            }

            case IDLE:
            default:
                break;
        }
    }

    // ── Price fetch (async) ───────────────────────────────────────────────────

    private static void fetchPrice() {
        // calculatedAmount is already correctly set by ItemSelector.
        // Just transition to CLICK_SLOT — no need to re-fetch price.
        System.out.println("[BazaarWorker] Amount to order: " + calculatedAmount);
        MinecraftClient.getInstance().execute(() -> state = StateMachine.CLICK_SLOT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns true if the current screen is an anvil or sign input screen. */
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
        if (slotIndex == -1 || handler == null || client.interactionManager == null || client.player == null)
            return;
        System.out.println("[BazaarWorker] Clicking slot " + slotIndex);
        client.interactionManager.clickSlot(
                handler.syncId,
                slotIndex,
                0,
                SlotActionType.PICKUP,
                client.player);
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