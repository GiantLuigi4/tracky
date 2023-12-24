package com.tracky.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@ApiStatus.Internal
public class LuigiKindaLearnedTesselator {
	
	public static void preRender(Minecraft minecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ, CallbackInfo ci) {
		if (FMLEnvironment.production) return;

		long time = System.currentTimeMillis();

		RenderSystem.enableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Entity entity = minecraft.gameRenderer.getMainCamera().getEntity();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		RenderSystem.disableBlend();
		RenderSystem.lineWidth(1.0F);
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

		Matrix4f pose = pPoseStack.last().pose();

		IChunkProviderAttachments attachments = (IChunkProviderAttachments) minecraft.level.getChunkSource();
		Iterator<LevelChunk> iterator = attachments.loadedChunks();
		while (iterator.hasNext()) {
			LevelChunk chunk = iterator.next();
			ChunkPos pos = chunk.getPos();
			boolean trackyForced = attachments.isTrackyForced(pos);
			float diff = Mth.clamp(time - attachments.getLastUpdate(pos), 0, 1000) / 1000.0F;
			float red = 1.0F - diff;
			float blue = trackyForced ? 1.0F : 0.0F;

			float x = pos.getMinBlockX() - (float) pCamX;
			float z = pos.getMinBlockZ() - (float) pCamZ;
			float y = chunk.getMinBuildHeight() - (float) pCamY;
			if (pCamY > chunk.getMinBuildHeight()) y += (10 * ((1 - diff) / 100));
			else y -= (10 * ((1 - diff) / 100));
			float y1 = chunk.getMaxBuildHeight() - (float) pCamY;
			if (pCamY < chunk.getMaxBuildHeight()) y1 -= (10 * ((1 - diff) / 100));
			else y1 += (10 * ((1 - diff) / 100));
			bufferbuilder.vertex(pose, x, y, z).color(red, diff, blue, 0.0F).endVertex();

			bufferbuilder.vertex(pose, x, y, z).color(red, diff, blue, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x + 16, y, z).color(red, diff, blue, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x + 16, y, z + 16).color(red, diff, blue, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x, y, z + 16).color(red, diff, blue, 1.0F).endVertex();

			bufferbuilder.vertex(pose, x, y, z).color(red, diff, blue, 0.0F).endVertex();

			y = y1;
			bufferbuilder.vertex(pose, x, y, z).color(red, diff, blue, 0.0F).endVertex();

			bufferbuilder.vertex(pose, x, y, z).color(red, diff, blue, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x + 16, y, z).color(red, diff, blue, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x + 16, y, z + 16).color(red, diff, blue, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x, y, z + 16).color(red, diff, blue, 1.0F).endVertex();

			bufferbuilder.vertex(pose, x, y, z).color(red, diff, blue, 0.0F).endVertex();
		}

		ChunkPos ckPos = entity.chunkPosition();
		float x = (float) (ckPos.x * 16 - pCamX);
		float y = (float) (minecraft.level.getMinBuildHeight() - pCamY);
		float y1 = (float) (minecraft.level.getMaxBuildHeight() - pCamY);
		float z = (float) (ckPos.z * 16 - pCamZ);

		for (int xO = 0; xO < 2; xO++) {
			for (int zO = 0; zO < 2; zO++) {
				bufferbuilder.vertex(pose, x + xO * 16, y, z + zO * 16).color(1, 1, 0.0F, 0.0F).endVertex();

				bufferbuilder.vertex(pose, x + xO * 16, y, z + zO * 16).color(1, 1, 0.0F, 1.0F).endVertex();
				bufferbuilder.vertex(pose, x + xO * 16, y1, z + zO * 16).color(1, 1, 0.0F, 1.0F).endVertex();

				bufferbuilder.vertex(pose, x + xO * 16, y1, z + zO * 16).color(1, 1, 0.0F, 0.0F).endVertex();
			}
		}

		y = minecraft.level.getMinBuildHeight();
		y = ((int) (y / 16)) * 16;
		y1 = minecraft.level.getMaxBuildHeight();

		for (int yO = (int) y; yO <= y1 + 1; yO += 16) {
			bufferbuilder.vertex(pose, x, (float) (yO - pCamY), z).color(0, 0, 1.0F, 0.0F).endVertex();

			bufferbuilder.vertex(pose, x, (float) (yO - pCamY), z).color(0, 0, 1.0F, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x + 16, (float) (yO - pCamY), z).color(0, 0, 1.0F, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x + 16, (float) (yO - pCamY), z + 16).color(0, 0, 1.0F, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x, (float) (yO - pCamY), z + 16).color(0, 0, 1.0F, 1.0F).endVertex();
			bufferbuilder.vertex(pose, x, (float) (yO - pCamY), z).color(0, 0, 1.0F, 1.0F).endVertex();

			bufferbuilder.vertex(pose, x, (float) (yO - pCamY), z).color(0, 0, 1.0F, 0.0F).endVertex();
		}

		tesselator.end();

		RenderSystem.enableBlend();

		ci.cancel();
	}
}
