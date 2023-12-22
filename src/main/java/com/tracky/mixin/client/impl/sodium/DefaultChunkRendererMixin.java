package com.tracky.mixin.client.impl.sodium;

import com.tracky.access.sodium.ExtendedDefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public class DefaultChunkRendererMixin implements ExtendedDefaultChunkRenderer {

	@Unique
	private CameraTransform tracky$cameraTransform;

	@Override
	public void tracky$setCameraTransform(@Nullable CameraTransform transform) {
		this.tracky$cameraTransform = transform;
	}

	@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/DefaultChunkRenderer;fillCommandBuffer(Lme/jellysquid/mods/sodium/client/gl/device/MultiDrawBatch;Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegion;Lme/jellysquid/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;Lme/jellysquid/mods/sodium/client/render/chunk/lists/ChunkRenderList;Lme/jellysquid/mods/sodium/client/render/viewport/CameraTransform;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Z)V"))
	public CameraTransform editTransform(CameraTransform transform) {
		return this.tracky$cameraTransform != null ? this.tracky$cameraTransform : transform;
	}
}
