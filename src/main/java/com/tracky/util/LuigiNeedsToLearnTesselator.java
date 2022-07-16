package com.tracky.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class LuigiNeedsToLearnTesselator {
	public static void preRender(Minecraft minecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ, CallbackInfo ci) {
		if (FMLEnvironment.production) return;
		
		long time = System.currentTimeMillis();
		
		RenderSystem.enableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Entity entity = minecraft.gameRenderer.getMainCamera().getEntity();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		RenderSystem.disableTexture();
		RenderSystem.disableBlend();
		ChunkPos chunkpos = entity.chunkPosition();
		RenderSystem.lineWidth(1.0F);
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		
		IChunkProviderAttachments attachments = (IChunkProviderAttachments) minecraft.level.getChunkSource();
		for (LevelChunk chunk : attachments.forcedChunks()) {
			long time1 = attachments.getLastUpdate(chunk);
			long diff0 = Math.abs(time - time1);
			float diff;
			if (diff0 > 1000)
				diff = 1;
			else {
				diff = diff0;
				diff /= 1000;
			}
			
			double x = chunk.getPos().getMinBlockX() - pCamX;
			double z = chunk.getPos().getMinBlockZ() - pCamZ;
			double y = chunk.getMinBuildHeight() - pCamY;
			if (pCamY > chunk.getMinBuildHeight()) y += (10 * ((1 - diff) / 100));
			else y -= (10 * ((1 - diff) / 100));
			double y1 = chunk.getMaxBuildHeight() - pCamY;
			if (pCamY < chunk.getMaxBuildHeight()) y1 -= (10 * ((1 - diff) / 100));
			else y1 += (10 * ((1 - diff) / 100));
			bufferbuilder.vertex(x, y, z).color(1 - diff, diff, 1.0F, 0.0F).endVertex();
			
			bufferbuilder.vertex(x, y, z).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(x + 16, y, z).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(x + 16, y, z + 16).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(x, y, z + 16).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			
			bufferbuilder.vertex(x, y, z).color(1 - diff, diff, 0.0F, 0.0F).endVertex();
			
			y = y1;
			bufferbuilder.vertex(x, y, z).color(1 - diff, diff, 0.0F, 0.0F).endVertex();
			
			bufferbuilder.vertex(x, y, z).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(x + 16, y, z).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(x + 16, y, z + 16).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(x, y, z + 16).color(1 - diff, diff, 0.0F, 1.0F).endVertex();
			
			bufferbuilder.vertex(x, y, z).color(1 - diff, diff, 0.0F, 0.0F).endVertex();
		}
		tesselator.end();
		
		RenderSystem.enableBlend();
		RenderSystem.enableTexture();
		
		ci.cancel();
	}
}
