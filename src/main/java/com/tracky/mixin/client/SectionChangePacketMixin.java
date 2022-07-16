package com.tracky.mixin.client;

import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public class SectionChangePacketMixin {
	@Shadow @Final private SectionPos sectionPos;
	
	@Inject(at = @At("HEAD"), method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V")
	public void preHandle(ClientGamePacketListener pHandler, CallbackInfo ci) {
		IChunkProviderAttachments attachments = (IChunkProviderAttachments) Minecraft.getInstance().level.getChunkSource();
		attachments.setUpdated(sectionPos.x(), sectionPos.z());
	}
}
