package com.tracky.impl;

import com.tracky.Tracky;
import com.tracky.access.ClientMapHolder;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.api.TrackyViewArea;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Custom, separate implementation of {@link ViewArea} to make sure no tracky chunks get sent through VisGraph.
 */
@ApiStatus.Internal
public class TrackyVanillaViewArea implements TrackyViewArea, NativeResource {

	private final ChunkRenderDispatcher chunkRenderDispatcher;
	private final ClientLevel level;
	private final Map<ChunkPos, TrackyRenderChunk[]> renderChunkCache;
	private final Consumer<ChunkRenderDispatcher.RenderChunk> dirtyChunks;

	private final int minSection;
	private final int sectionCount;

	public TrackyVanillaViewArea(ChunkRenderDispatcher chunkRenderDispatcher, ClientLevel level, Consumer<ChunkRenderDispatcher.RenderChunk> dirtyChunks) {
		this.chunkRenderDispatcher = chunkRenderDispatcher;
		this.level = level;
		this.renderChunkCache = new ConcurrentHashMap<>();
		this.dirtyChunks = dirtyChunks;
		this.minSection = this.level.getMinSection();
		this.sectionCount = this.level.getSectionsCount();
	}

	@Override
	public @Nullable TrackyRenderChunk getRenderChunk(int sectionX, int sectionY, int sectionZ) {
		int y = Math.floorMod(sectionY - this.minSection, this.sectionCount);
		if (y < 0 || y >= this.sectionCount) {
			return null;
		}

		TrackyRenderChunk[] column = this.renderChunkCache.get(new ChunkPos(sectionX, sectionZ));
		return column != null ? column[y] : null;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void setDirty(int pSectionX, int pSectionY, int pSectionZ, boolean priority) {
//		Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder) this.level).trackyGetRenderChunksC();
		SectionPos spos = SectionPos.of(pSectionX, pSectionY, pSectionZ);
//		if (Tracky.sourceContains(this.level, spos) || trackyRenderedChunksList.contains(spos)) {
		if (Tracky.sourceContains(this.level, spos)) {
			int y = Math.floorMod(pSectionY - this.minSection, this.sectionCount);

			TrackyRenderChunk[] renderChunks = this.renderChunkCache.computeIfAbsent(new ChunkPos(pSectionX, pSectionZ), unused -> new TrackyRenderChunk[this.sectionCount]);
			ChunkRenderDispatcher.RenderChunk renderChunk = (ChunkRenderDispatcher.RenderChunk) renderChunks[y];
			if (renderChunk == null) {
				renderChunk = this.chunkRenderDispatcher.new RenderChunk(-1, pSectionX << SectionPos.SECTION_BITS, pSectionY << SectionPos.SECTION_BITS, pSectionZ << SectionPos.SECTION_BITS);
				renderChunks[y] = (TrackyRenderChunk) renderChunk;
			}

			renderChunk.setDirty(priority);
			this.dirtyChunks.accept(renderChunk);
		}
	}

	@Override
	public void free() {
		for (TrackyRenderChunk[] value : this.renderChunkCache.values()) {
			for (TrackyRenderChunk renderChunk : value) {
				if (renderChunk != null) {
					renderChunk.free();
				}
			}
		}
		this.renderChunkCache.clear();
	}
}
