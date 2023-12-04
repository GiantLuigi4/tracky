package com.tracky.debug;

import net.minecraft.world.level.chunk.LevelChunk;

public interface IChunkProviderAttachments {

	LevelChunk[] forcedChunks();

	void setUpdated(int x, int z);

	long getLastUpdate(LevelChunk chunk);

	boolean isTrackyForced(LevelChunk chunk);
}
