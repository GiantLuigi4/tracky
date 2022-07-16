package com.tracky.mixin.client;

import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundBlockUpdatePacket.class)
public class BlockUpdatePacketMixin {
	@Shadow
	@Final
	private BlockPos pos;
	
	@Inject(at = @At("HEAD"), method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V")
	public void preHandle(ClientGamePacketListener pHandler, CallbackInfo ci) {
		if (Minecraft.getInstance().level != null) {
			IChunkProviderAttachments attachments = (IChunkProviderAttachments) Minecraft.getInstance().level.getChunkSource();
			ChunkPos cPos = new ChunkPos(pos);
			attachments.setUpdated(cPos.x, cPos.z);
		}
	}
}
