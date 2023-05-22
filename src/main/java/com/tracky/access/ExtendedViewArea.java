package com.tracky.access;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;

public interface ExtendedViewArea {
	HashMap<ChunkPos, ChunkRenderDispatcher.RenderChunk[]> getTracky$renderChunkCache();
}
