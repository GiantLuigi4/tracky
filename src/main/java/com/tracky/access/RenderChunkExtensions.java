package com.tracky.access;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.tracky.api.RenderSource;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface RenderChunkExtensions {

	@Nullable RenderSource tracky$getRenderSource();

	void tracky$setRenderSource(@Nullable RenderSource renderSource);

	VertexSorting tracky$getSorting();
}
