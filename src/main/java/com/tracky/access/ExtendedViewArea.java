package com.tracky.access;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;

public interface ExtendedViewArea {
	Map<ChunkPos, ChunkRenderDispatcher.RenderChunk[]> getTracky$renderChunkCache();
}
