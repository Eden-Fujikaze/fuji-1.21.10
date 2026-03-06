package at.rewrite;

import at.rewrite.utils.ScoreboardUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MainClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            String first = ScoreboardUtils.findValue("Purse", false);
            String second = ScoreboardUtils.findValue("Purse", true);
            System.out.println(first + " " + second);
        });
    }
}
