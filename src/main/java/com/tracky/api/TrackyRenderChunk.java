package com.tracky.api;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.system.NativeResource;

public interface TrackyRenderChunk extends NativeResource {

	boolean needsSorting();

	boolean resort(RenderType layer);

	void markDirty(boolean reRenderOnMainThread);

	BlockPos getChunkOrigin();

	default SectionPos getSectionPos() {
		return SectionPos.of(this.getChunkOrigin());
	}

	@Nullable RenderSource getRenderSource();

	@ApiStatus.Internal
	void setRenderSource(@Nullable RenderSource renderSource);
}
