package at.rewrite.mixin.client;

import at.rewrite.mining.StateMachine;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class UnfocusedTickMixin {
    @Inject(method = "onWindowFocusChanged", at = @At("HEAD"), cancellable = true)
    private void onFocusChanged(boolean focused, CallbackInfo ci) {
        if (StateMachine.enabled) {
            ci.cancel();
        }
    }
}