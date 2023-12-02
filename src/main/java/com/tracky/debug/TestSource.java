package com.tracky.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.api.BoxRenderSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.AxisAngle4d;
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
	public void calculateChunkOffset(Vector3f vec, double camX, double camY, double camZ) {
		int avgX = (min.minBlockX() + max.maxBlockX()) / 2;
		int avgZ = (min.minBlockZ() + max.maxBlockZ()) / 2;
		vec.add(
				-avgX,
				-min.minBlockY(),
				-avgZ
		);
	}
	
	@Override
	public void draw(PoseStack matrix, ViewArea area, ShaderInstance instance, RenderType type, double camX, double camY, double camZ) {
		if (instance.MODEL_VIEW_MATRIX != null) {
			matrix.pushPose();
			matrix.translate(-camX, -camY + 64, -camZ);
			matrix.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(45), 0, 1, 0)));

			instance.MODEL_VIEW_MATRIX.set(matrix.last().pose());
			instance.MODEL_VIEW_MATRIX.upload();

			matrix.popPose();
		}

		super.draw(matrix, area, instance, type, camX, camY, camZ);

		if (instance.MODEL_VIEW_MATRIX != null) {
			instance.MODEL_VIEW_MATRIX.set(matrix.last().pose());
			instance.MODEL_VIEW_MATRIX.upload();
		}
	}
}
