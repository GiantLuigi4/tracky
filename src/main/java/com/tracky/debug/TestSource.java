package com.tracky.debug;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.api.BoxRenderSource;
import com.tracky.api.TrackyChunkRenderer;
import com.tracky.api.TrackyViewArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class TestSource extends BoxRenderSource {

	public static final BlockPos MIN = new BlockPos(-656 * 1, -63, 656 * 1);
	public static final BlockPos MAX = new BlockPos(656 * 1, 319, 656 * 2);

	public TestSource() {
		super(SectionPos.of(MIN), SectionPos.of(MAX));
	}

	@Override
	public void transform(PoseStack matrixStack, double camX, double camY, double camZ) {
//		matrixStack.translate(10000 - camX, 128 - camY, -camZ);
		matrixStack.translate(-camX, 128 - camY, -camZ);

		matrixStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(Minecraft.getInstance().player.tickCount + Minecraft.getInstance().getPartialTick()), 0, 1, 0)));
	}

	@Override
	public void draw(TrackyChunkRenderer chunkRenderer, PoseStack matrixStack, TrackyViewArea area, RenderType type, double camX, double camY, double camZ) {
		// (ocelot): TODO: make cylindrical fog work
		// (laz): I've tried playing around with the fog in the past, it's gonna require custom shaders
		// 		  might be best to leave it up to the mod implementing tracky, given the fact that tracky's supposed to try to be relatively non-invasive?
		chunkRenderer.setFogShape(FogShape.SPHERE);
		super.draw(chunkRenderer, matrixStack, area, type, camX, camY, camZ);

		// This constantly tries to resort for testing, but gets unnecessarily slow after about 1k elements
		if (this.transparentChunksInSource.size() < 1000) {
			this.scheduleSort();
		}
	}

	@Override
	public Vector3dc getChunkOffset() {
		int avgX = (this.min.minBlockX() + this.max.maxBlockX()) / 2;
		int avgZ = (this.min.minBlockZ() + this.max.maxBlockZ()) / 2;
		return new Vector3d(-avgX, -this.min.minBlockY(), -avgZ);
	}
}
