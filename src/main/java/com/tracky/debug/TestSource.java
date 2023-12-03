package com.tracky.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.api.BoxRenderSource;
import com.tracky.util.TrackyViewArea;
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
	public void calculateChunkOffset(Vector3f vec, double camX, double camY, double camZ) {
		int avgX = (this.min.minBlockX() + this.max.maxBlockX()) / 2;
		int avgZ = (this.min.minBlockZ() + this.max.maxBlockZ()) / 2;
		vec.add(-avgX, -this.min.minBlockY(), -avgZ);
		vec.sub((float) camX, (float) camY, (float) camZ);
	}

	@Override
	public void transform(PoseStack matrix, double camX, double camY, double camZ) {
		matrix.translate(0, 64, 0);
		matrix.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(Minecraft.getInstance().player.tickCount + Minecraft.getInstance().getPartialTick()), 0, 1, 0)));
	}

	@Override
	public void draw(PoseStack matrix, TrackyViewArea area, ShaderInstance instance, RenderType type, double camX, double camY, double camZ) {
		if (instance.FOG_COLOR != null) {
			instance.FOG_COLOR.set(0.0F, 0.0F, 0.0F, 0.0F);
			instance.FOG_COLOR.upload();
		}

		super.draw(matrix, area, instance, type, camX, camY, camZ);

		this.sort();

		if (instance.FOG_COLOR != null) {
			instance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
			instance.FOG_COLOR.upload();
		}
	}
}
