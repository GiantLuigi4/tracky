package com.tracky.debug;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.api.BoxRenderSource;
import com.tracky.api.TrackyChunkRenderer;
import com.tracky.api.TrackyViewArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TestSource extends BoxRenderSource {

	public TestSource() {
		super(
				SectionPos.of(new BlockPos(-656, -63, 296)),
				SectionPos.of(new BlockPos(-497, 319, 328))
		);
	}

	@Override
	public void transform(PoseStack matrixStack, double camX, double camY, double camZ) {
		matrixStack.translate(-camX, -camY, -camZ);
		matrixStack.translate(0, 64, 0);
		matrixStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(Minecraft.getInstance().player.tickCount + Minecraft.getInstance().getPartialTick()), 0, 1, 0)));
		int avgX = (this.min.minBlockX() + this.max.maxBlockX()) / 2;
		int avgZ = (this.min.minBlockZ() + this.max.maxBlockZ()) / 2;
		matrixStack.translate(-avgX, -this.min.minBlockY(), -avgZ);
		matrixStack.translate(camX, camY, camZ);
	}

	@Override
	public void draw(TrackyChunkRenderer chunkRenderer, PoseStack matrixStack, TrackyViewArea area, RenderType type, double camX, double camY, double camZ) {
		chunkRenderer.setFogShape(FogShape.SPHERE); // TODO make cylindrical fog work
		super.draw(chunkRenderer, matrixStack, area, type, camX, camY, camZ);
		this.sort();
	}
}
