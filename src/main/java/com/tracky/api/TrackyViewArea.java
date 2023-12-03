package com.tracky.api;

import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

public interface TrackyViewArea {

	default @Nullable TrackyRenderChunk getRenderChunk(SectionPos pos) {
		return this.getRenderChunk(pos.getX(), pos.getY(), pos.getZ());
	}

	@Nullable TrackyRenderChunk getRenderChunk(int sectionX, int sectionY, int sectionZ);

	default @Nullable TrackyRenderChunk tryCreateRenderChunk(SectionPos pos) {
		return this.tryCreateRenderChunk(pos.getX(), pos.getY(), pos.getZ());
	}

	default @Nullable TrackyRenderChunk tryCreateRenderChunk(int sectionX, int sectionY, int sectionZ) {
		TrackyRenderChunk chunk = this.getRenderChunk(sectionX, sectionY, sectionZ);
		if (chunk != null) {
			return chunk;
		}

		this.setDirty(sectionX, sectionY, sectionZ, false);
		return this.getRenderChunk(sectionX, sectionY, sectionZ);
	}

	default void setDirty(SectionPos pos, boolean reRenderOnMainThread) {
		this.setDirty(pos.getX(), pos.getY(), pos.getZ(), reRenderOnMainThread);
	}

	void setDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread);
}
