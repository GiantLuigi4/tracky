package com.tracky.util;

import com.tracky.Tracky;
import com.tracky.access.ClientMapHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom, separate implementation of {@link ViewArea} to make sure no tracky chunks get sent through VisGraph.
 */
public class TrackyViewArea {

	private final ChunkRenderDispatcher chunkRenderDispatcher;
	private final ClientLevel level;
	private final Map<ChunkPos, ChunkRenderDispatcher.RenderChunk[]> renderChunkCache;
	private final Collection<ChunkRenderDispatcher.RenderChunk> dirtyChunks;

	private final int minSection;
	private final int chunkGridSizeY;

	public TrackyViewArea(ChunkRenderDispatcher chunkRenderDispatcher, ClientLevel level, Collection<ChunkRenderDispatcher.RenderChunk> dirtyChunks) {
		this.chunkRenderDispatcher = chunkRenderDispatcher;
		this.level = level;
		this.renderChunkCache = new ConcurrentHashMap<>();
		this.dirtyChunks = dirtyChunks;
		this.minSection = this.level.getMinSection();
		this.chunkGridSizeY = this.level.getSectionsCount();
	}

	/**
	 * Retrieves the render chunks in a whole column
	 *
	 * @param pos The chunk position to get chunks for
	 * @return The
	 */
	public @Nullable ChunkRenderDispatcher.RenderChunk[] getRenderChunkColumn(ChunkPos pos) {
		return this.renderChunkCache.get(pos);
	}

	/**
	 * Populate & set dirty chunks in tracky$chunkRenderDispatcher
	 */
	public void setDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
		Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder) this.level).trackyGetRenderChunksC();
		SectionPos spos = SectionPos.of(pSectionX, pSectionY, pSectionZ);
		if (Tracky.sourceContains(this.level, spos) || trackyRenderedChunksList.contains(spos)) {
			int y = Math.floorMod(pSectionY - this.minSection, this.chunkGridSizeY);

			ChunkRenderDispatcher.RenderChunk[] renderChunks = this.renderChunkCache.computeIfAbsent(new ChunkPos(pSectionX, pSectionZ), unused -> new ChunkRenderDispatcher.RenderChunk[this.chunkGridSizeY]);
			ChunkRenderDispatcher.RenderChunk renderChunk = renderChunks[y];
			if (renderChunk == null) {
				renderChunk = this.chunkRenderDispatcher.new RenderChunk(-1, pSectionX << SectionPos.SECTION_BITS, pSectionY << SectionPos.SECTION_BITS, pSectionZ << SectionPos.SECTION_BITS);
				renderChunks[y] = renderChunk;
			}

			renderChunk.setDirty(pReRenderOnMainThread);
			this.dirtyChunks.add(renderChunk);
		}
	}

	/**
	 * Retrieves a tracky-specific render chunk at the specified position.
	 *
	 * @param pos The block position to get the chunk at
	 * @return The render chunk at that position or <code>null</code> if there is none
	 */
	public @Nullable ChunkRenderDispatcher.RenderChunk getRenderChunkAt(BlockPos pos) {
		int y = Math.floorDiv(pos.getY() - this.minSection, 16);
		if (y < 0 || y >= this.chunkGridSizeY) {
			return null;
		}

		int x = Math.floorDiv(pos.getX(), 16);
		int preY = Math.floorDiv(pos.getY(), 16);
		int z = Math.floorDiv(pos.getZ(), 16);

		SectionPos spos = SectionPos.of(x, preY, z);
		Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder) this.level).trackyGetRenderChunksC();

		if (Tracky.sourceContains(this.level, spos) || trackyRenderedChunksList.contains(spos)) {
			ChunkPos cpos = new ChunkPos(x, z);
			ChunkRenderDispatcher.RenderChunk[] renderChunks = this.renderChunkCache.get(cpos);

			if (renderChunks != null) {
				return renderChunks[y];
			}
		}

		return null;
	}

	@ApiStatus.Internal
	public void releaseBuffers() {
		for (ChunkRenderDispatcher.RenderChunk[] value : this.renderChunkCache.values()) {
			for (ChunkRenderDispatcher.RenderChunk renderChunk : value) {
				if (renderChunk != null) {
					renderChunk.releaseBuffers();
				}
			}
		}
		this.renderChunkCache.clear();
	}
}
