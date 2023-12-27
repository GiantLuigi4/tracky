package com.tracky.mixin.client.impl.sodium;

import com.tracky.access.sodium.ExtendedOcclusionCuller;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.collections.WriteQueue;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(value = OcclusionCuller.class, remap = false)
public abstract class OcclusionCullerMixin implements ExtendedOcclusionCuller {

	@Unique
	private RenderSource tracky$renderSource;
	@Unique
	private Vector3ic tracky$minSection;
	@Unique
	private Vector3ic tracky$maxSection;

	@Shadow
	protected abstract void initWithinWorld(Consumer<RenderSection> visitor, WriteQueue<RenderSection> queue, Viewport viewport, boolean useOcclusionCulling, int frame);

	@Shadow
	protected abstract RenderSection getRenderSection(int x, int y, int z);

	@Shadow
	protected abstract void tryVisitNode(WriteQueue<RenderSection> queue, int x, int y, int z, int direction, int frame, Viewport viewport);

	@ModifyVariable(method = "findVisible", at = @At("HEAD"), argsOnly = true)
	public Viewport modifyViewport(Viewport viewport) {
		if (this.tracky$renderSource == null) {
			return viewport;
		}

		CameraTransform cameraTransform = viewport.getTransform();
		Vector3dc chunkOffset = this.tracky$renderSource.getChunkOffset();
		Vector3f cameraPos = new Vector3f((float) cameraTransform.x, (float) cameraTransform.y, (float) cameraTransform.z);
		this.tracky$renderSource.getTransformation(0, 0, 0).invert().transformPosition(cameraPos);
		return new Viewport(((ViewportAccessor) (Object) viewport).getFrustum(), new Vector3d(cameraPos).sub(chunkOffset));
	}

	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	public void init(Consumer<RenderSection> visitor, WriteQueue<RenderSection> queue, Viewport viewport, float searchDistance, boolean useOcclusionCulling, int frame, CallbackInfo ci) {
		if (this.tracky$renderSource == null) {
			return;
		}

		SectionPos origin = viewport.getChunkCoord();
		RenderSection section = this.getRenderSection(origin.x(), origin.y(), origin.z());
		if (section != null) {
			this.initWithinWorld(visitor, queue, viewport, useOcclusionCulling, frame);
		} else {
			// In this scenario we need to grab each "plane" of the area and submit it as the start since we can see the entire face
			CameraTransform transform = viewport.getTransform();

			// TODO don't include chunks outside the render distance

			// down/up
			if (transform.y < this.tracky$minSection.y() << SectionPos.SECTION_BITS) {
				for (int x = this.tracky$minSection.x(); x <= this.tracky$maxSection.x(); x++) {
					for (int z = this.tracky$minSection.z(); z <= this.tracky$maxSection.z(); z++) {
						this.tryVisitNode(queue, x, this.tracky$minSection.y(), z, 0, frame, viewport);
					}
				}
			} else if (transform.y > this.tracky$maxSection.y() << SectionPos.SECTION_BITS) {
				for (int x = this.tracky$minSection.x(); x <= this.tracky$maxSection.x(); x++) {
					for (int z = this.tracky$minSection.z(); z <= this.tracky$maxSection.z(); z++) {
						this.tryVisitNode(queue, x, this.tracky$maxSection.y(), z, 1, frame, viewport);
					}
				}
			}

			// north/south
			if (transform.z < this.tracky$minSection.z() << SectionPos.SECTION_BITS) {
				for (int x = this.tracky$minSection.x(); x <= this.tracky$maxSection.x(); x++) {
					for (int y = this.tracky$minSection.y(); y <= this.tracky$maxSection.y(); y++) {
						this.tryVisitNode(queue, x, y, this.tracky$minSection.z(), 2, frame, viewport);
					}
				}
			} else if (transform.z > this.tracky$maxSection.z() << SectionPos.SECTION_BITS) {
				for (int x = this.tracky$minSection.x(); x <= this.tracky$maxSection.x(); x++) {
					for (int y = this.tracky$minSection.y(); y <= this.tracky$maxSection.y(); y++) {
						this.tryVisitNode(queue, x, y, this.tracky$maxSection.z(), 3, frame, viewport);
					}
				}
			}

			// west/east
			if (transform.x < this.tracky$minSection.x() << SectionPos.SECTION_BITS) {
				for (int z = this.tracky$minSection.z(); z <= this.tracky$maxSection.z(); z++) {
					for (int y = this.tracky$minSection.y(); y <= this.tracky$maxSection.y(); y++) {
						this.tryVisitNode(queue, this.tracky$minSection.x(), y, z, 4, frame, viewport);
					}
				}
			} else if (transform.x > this.tracky$maxSection.x() << SectionPos.SECTION_BITS) {
				for (int z = this.tracky$minSection.z(); z <= this.tracky$maxSection.z(); z++) {
					for (int y = this.tracky$minSection.y(); y <= this.tracky$maxSection.y(); y++) {
						this.tryVisitNode(queue, this.tracky$maxSection.x(), y, z, 5, frame, viewport);
					}
				}
			}
		}

		ci.cancel();
	}

	@Inject(method = "isOutsideFrustum", at = @At("HEAD"), cancellable = true)
	private static void isOutsideFrustum(Viewport viewport, RenderSection section, CallbackInfoReturnable<Boolean> cir) {
		if (((TrackyRenderChunk) section).getRenderSource() != null) {
			cir.setReturnValue(false); // TODO
		}
	}

	@Override
	public void tracky$setRenderSource(@Nullable RenderSource source) {
		this.tracky$renderSource = source;
	}

	@Override
	public void tracky$setBounds(Vector3ic minSection, Vector3ic maxSection) {
		this.tracky$minSection = minSection;
		this.tracky$maxSection = maxSection;
	}
}
