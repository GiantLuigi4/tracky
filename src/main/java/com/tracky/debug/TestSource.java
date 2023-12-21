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

public class TestSource extends BoxRenderSource {

	public TestSource() {
		super(
				SectionPos.of(new BlockPos(-656 * 1, -63, 656 * 1)),
				SectionPos.of(new BlockPos(656 * 1, 319, 656 * 2))
		);
	}

	@Override
	public void transform(PoseStack matrixStack, double camX, double camY, double camZ) {
		matrixStack.translate( - camX, 128 - camY,  - camZ);

		int avgX = (this.min.minBlockX() + this.max.maxBlockX()) / 2;
		int avgZ = (this.min.minBlockZ() + this.max.maxBlockZ()) / 2;
		matrixStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(Minecraft.getInstance().player.tickCount + Minecraft.getInstance().getPartialTick()), 0, 1, 0)));
		matrixStack.translate(-avgX, -this.min.minBlockY(), -avgZ);
	}

	@Override
	public boolean applyCameraChunkOffset() {
		return false;
	}

	@Override
	public void draw(TrackyChunkRenderer chunkRenderer, PoseStack matrixStack, TrackyViewArea area, RenderType type, double camX, double camY, double camZ) {
		// (ocelot): TODO: make cylindrical fog work
		// (laz): I've tried playing around with the fog in the past, it's gonna require custom shaders
		// 		  might be best to leave it up to the mod implementing tracky, given the fact that tracky's supposed to try to be relatively non-invasive?
		chunkRenderer.setFogShape(FogShape.SPHERE);
		super.draw(chunkRenderer, matrixStack, area, type, camX, camY, camZ);
		this.sort();
	}
}
