package at.fuji.bazaar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.item.ItemStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.client.Keyboard;
import net.minecraft.client.Keyboard.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class BazaarWorker {
    private static final Path FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve("bazaar_data.dat");

    public enum StateMachine {
        IDLE,
        OPEN_BAZAAR,
        SCAN_MENU,
        OPEN_SEARCH,
        ENTER_ITEM,
        READ_RESULT,
        SELECT_ITEM,
        CALC_AMOUNT,
        ENTER_AMOUNT,
        CONFIRM_TRADE,
        DONE
    }

    private static StateMachine state = StateMachine.IDLE;
    static MinecraftClient client = MinecraftClient.getInstance();
    static java.util.Optional<String> itemName = java.util.Optional.of("Enchanted Diamond");
    static List<String> menuItems = List.of("Custom Amount", "Create Buy Order", "Top Order +0.1");
    static ScreenHandler handler;
    private static int lastFoundSlotIndex = -1;
    private static final CommandDispatcher<ClientCommandSource> commandDispatcher = new CommandDispatcher<>();

    public static void load() {
        if (!FILE_PATH.toFile().exists())
            return;

        try {
            NbtCompound nbt = NbtIo.read(FILE_PATH);
            if (nbt != null) {
                itemName = nbt.getString("itemName");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("itemName", itemName.orElse(""));

        try {
            NbtIo.write(nbt, FILE_PATH);
            System.out.println("Bazaar data saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Starting bot");
        state = StateMachine.OPEN_BAZAAR;
    }

    public static void update() {
        if (client == null || client.player == null)
            return;
        handler = client.player.currentScreenHandler;
        switch (state) {
            case IDLE:
                state = StateMachine.SCAN_MENU;
                break;
            case OPEN_BAZAAR:
                String actualItemName = itemName.orElse("Enchanted Diamond");
                String cmd = String.format("bazaar %s", actualItemName);
                if (client.player != null) {
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand(cmd);
                }

                state = StateMachine.SCAN_MENU;
                break;
            case SCAN_MENU:
                for (String menuItem : menuItems) {
                    if (client.currentScreen != null) {
                        for (int i = 0; i < handler.slots.size(); i++) {
                            Slot slot = handler.slots.get(i);
                            ItemStack stack = slot.getStack();

                            if (!stack.isEmpty() && stack.getName().getString().contains(menuItem)
                                    || stack.getName().getString().contains(itemName.get())) {
                                System.out.println("Found menu item: " + menuItem + " at slot " + i);
                                lastFoundSlotIndex = i;
                                state = StateMachine.OPEN_SEARCH;
                                return;
                            }
                        }
                    }
                }
                break;
            case OPEN_SEARCH:
                if (lastFoundSlotIndex != -1 && client.interactionManager != null) {
                    System.out.println("Clicking slot: " + lastFoundSlotIndex);
                    client.interactionManager.clickSlot(
                            handler.syncId,
                            lastFoundSlotIndex,
                            0,
                            net.minecraft.screen.slot.SlotActionType.PICKUP,
                            client.player);
                    lastFoundSlotIndex = -1;
                    state = StateMachine.ENTER_ITEM;
                }
                break;
            default:
                break;
        }
    }

    private static double delay(long min, long max) {
        return (Math.random() * (max - min + 1) + min);
    }
}