package com.tracky.access.sodium;

import com.tracky.api.RenderSource;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.jetbrains.annotations.Nullable;

public interface ExtendedSodiumWorldRenderer {

	RenderSectionManager tracky$getRenderSectionManager(@Nullable RenderSource source);
}
