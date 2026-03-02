package at.fuji.mixin.client;

import at.fuji.utils.ScoreboardUtil;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientGamePacketListenerMixin {
    @Inject(method = "onSetScore(Lnet/minecraft/network/protocol/game/ClientboundSetScorePacket;)V", at = @At("HEAD"))
    private void onSetScore(ClientboundSetScorePacket packet, CallbackInfo ci) {
        ScoreboardUtil.handlePacket(packet);
    }
}