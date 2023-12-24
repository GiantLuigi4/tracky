package com.tracky.mixin.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(ClientChunkCache.Storage.class)
public interface ChunkStorageAccessor {

	@Accessor
	int getChunkRadius();

	@Accessor("viewRange")
	int getViewRange();

	@Invoker
	void invokeReplace(int pChunkIndex, @Nullable LevelChunk pChunk);

	@Invoker
	boolean invokeInRange(int pX, int pZ);

	@Invoker
	LevelChunk invokeGetChunk(int index);
}
