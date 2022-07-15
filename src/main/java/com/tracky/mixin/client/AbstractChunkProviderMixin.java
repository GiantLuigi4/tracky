package com.tracky.mixin.client;

import com.tracky.TrackyAccessor;
import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.world.level.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSource.class)
public class AbstractChunkProviderMixin {
//	@Inject(at = @At("HEAD"), method = "hasChunk", cancellable = true)
//	public void preCheckExists(int x, int z, CallbackInfoReturnable<Boolean> cir) {
//		if (!TrackyAccessor.isMainTracky()) return;
//		if (this instanceof IChunkProviderAttachments) {
//			cir.setReturnValue(((IChunkProviderAttachments) this).hasChunkAt(new ChunkPos(x, z)));
//		}
//	}
}
