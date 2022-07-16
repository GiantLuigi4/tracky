package com.tracky.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tracky.debug.IChunkProviderAttachments;
import com.tracky.util.LuigiNeedsToLearnTesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkBorderRenderer.class)
public class LevelRendererMixin {
	@Shadow @Final private Minecraft minecraft;
	
	@Inject(at = @At("HEAD"), method = "render")
	public void preRender(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ, CallbackInfo ci) {
		LuigiNeedsToLearnTesselator.preRender(minecraft, pPoseStack, pBufferSource, pCamX, pCamY, pCamZ, ci);
	}
}
