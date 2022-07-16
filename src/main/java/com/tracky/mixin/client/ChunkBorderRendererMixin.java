package com.tracky.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.util.LuigiNeedsToLearnTesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkBorderRenderer.class)
public class ChunkBorderRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	
	@Inject(at = @At("HEAD"), method = "render", cancellable = true)
	public void preRender(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ, CallbackInfo ci) {
		LuigiNeedsToLearnTesselator.preRender(minecraft, pPoseStack, pBufferSource, pCamX, pCamY, pCamZ, ci);
	}
}
