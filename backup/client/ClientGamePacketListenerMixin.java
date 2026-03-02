package at.fuji.mixin.client;

import at.fuji.utils.ScoreboardUtil;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientGamePacketListenerMixin {
    @Inject(method = "onScoreboardScoreUpdate(Lnet/minecraft/network/packet/s2c/play/ScoreboardScoreUpdateS2CPacket;)V", at = @At("HEAD"))
    private void onScoreboardScoreUpdate(net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket packet,
            CallbackInfo ci) {
        ScoreboardUtil.handlePacket(packet);
    }
}