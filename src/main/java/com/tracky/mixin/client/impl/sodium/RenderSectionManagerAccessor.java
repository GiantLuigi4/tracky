package com.tracky.mixin.client.impl.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RenderSectionManager.class, remap = false)
public interface RenderSectionManagerAccessor {

	@Accessor
	OcclusionCuller getOcclusionCuller();

	@Accessor
	ChunkRenderer getChunkRenderer();
}
