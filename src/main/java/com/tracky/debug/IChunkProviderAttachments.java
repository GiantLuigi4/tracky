package com.tracky.debug;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Iterator;

public interface IChunkProviderAttachments {

    Iterator<LevelChunk> loadedChunks();

    void setUpdated(ChunkPos pos);

    long getLastUpdate(ChunkPos pos);

    boolean isTrackyForced(ChunkPos pos);
}
