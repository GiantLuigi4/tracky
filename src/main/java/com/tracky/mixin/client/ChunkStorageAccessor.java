package com.tracky.mixin.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientChunkCache.Storage.class)
public interface ChunkStorageAccessor {

	@Accessor("chunkRadius")
	int getChunkRadius();
}
