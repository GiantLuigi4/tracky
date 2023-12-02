package com.tracky.access;

import com.tracky.api.RenderSource;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface RenderChunkExtensions {

	@Nullable RenderSource tracky$getRenderSource();

	void setRenderSource(@Nullable RenderSource renderSource);
}
