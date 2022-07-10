package com.tracky.mixin.client;

import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.AbstractChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractChunkProvider.class)
public class AbstractChunkProviderMixin {
	@Inject(at = @At("HEAD"), method = "chunkExists", cancellable = true)
	public void preCheckExists(int x, int z, CallbackInfoReturnable<Boolean> cir) {
		if (this instanceof IChunkProviderAttachments) {
			cir.setReturnValue(((IChunkProviderAttachments) this).hasChunkAt(new ChunkPos(x, z)));
		}
	}
}
