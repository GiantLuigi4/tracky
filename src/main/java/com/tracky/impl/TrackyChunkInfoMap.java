package com.tracky.impl;

import com.tracky.mixin.client.render.RenderChunkInfoMixin;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.HashMap;

/**
 * This class is not thread safe.
 */
@ApiStatus.Internal
public class TrackyChunkInfoMap {

	private final HashMap<Vector3ic, LevelRenderer.RenderChunkInfo> tracky$map = new HashMap<>();
	private final Vector3i pos = new Vector3i();

	private Vector3ic getKey(ChunkRenderDispatcher.RenderChunk renderChunk) {
		Vec3i vec = renderChunk.getOrigin();
		int x = Mth.floorDiv(vec.getX(), 16);
		int y = Mth.floorDiv(vec.getY(), 16);
		int z = Mth.floorDiv(vec.getZ(), 16);
		return this.pos.set(x, y, z);
	}

	public void put(ChunkRenderDispatcher.RenderChunk renderChunk) {
		this.tracky$map.put(new Vector3i(this.getKey(renderChunk)), RenderChunkInfoMixin.invokeInit(renderChunk, null, 0));
	}

	public @Nullable LevelRenderer.RenderChunkInfo get(ChunkRenderDispatcher.RenderChunk renderChunk) {
		return this.tracky$map.get(this.getKey(renderChunk));
	}

	public LevelRenderer.RenderChunkInfo getOrCreate(ChunkRenderDispatcher.RenderChunk renderChunk) {
		Vector3ic key = this.getKey(renderChunk);
		LevelRenderer.RenderChunkInfo info = this.tracky$map.get(key);

		if (info == null) {
			info = RenderChunkInfoMixin.invokeInit(renderChunk, null, 0);
			this.tracky$map.put(new Vector3i(key), info);
		}

		return info;
	}

	public void clear() {
		this.tracky$map.clear();
	}
}
