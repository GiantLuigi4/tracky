package com.tracky.mixin.client.impl.sodium;

import com.tracky.access.sodium.ExtendedOcclusionCuller;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.collections.WriteQueue;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
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

	@Shadow
	@Final
	private Long2ReferenceMap<RenderSection> sections;

	@Shadow
	protected abstract void initWithinWorld(Consumer<RenderSection> visitor, WriteQueue<RenderSection> queue, Viewport viewport, boolean useOcclusionCulling, int frame);

	@Shadow
	protected abstract RenderSection getRenderSection(int x, int y, int z);

	@Shadow
	private static boolean isOutsideRenderDistance(CameraTransform camera, RenderSection section, float maxDistance) {
		return false;
	}

	@Shadow
	public static boolean isOutsideFrustum(Viewport viewport, RenderSection section) {
		return false;
	}

	@ModifyVariable(method = "findVisible", at = @At("HEAD"), argsOnly = true)
	public Viewport modifyViewport(Viewport viewport) {
		if (this.tracky$renderSource == null) {
			return viewport;
		}

		CameraTransform cameraTransform = viewport.getTransform();
		Vector3f cameraPos = new Vector3f((float) cameraTransform.x, (float) cameraTransform.y, (float) cameraTransform.z);
		this.tracky$renderSource.getTransformation(0, 0, 0).invert().transformPosition(cameraPos);
		return new Viewport(((ViewportAccessor) (Object) viewport).getFrustum(), new Vector3d(cameraPos));
	}

	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	public void init(Consumer<RenderSection> visitor, WriteQueue<RenderSection> queue, Viewport viewport, float searchDistance, boolean useOcclusionCulling, int frame, CallbackInfo ci) {
		if (this.tracky$renderSource == null) {
			return;
		}

		SectionPos origin = viewport.getChunkCoord();
		RenderSection section = this.getRenderSection(origin.getX(), origin.getY(), origin.getZ());
		if (section != null) {
			this.initWithinWorld(visitor, queue, viewport, useOcclusionCulling, frame);
		} else {
			// TODO Allow occlusion culling outside the source
			this.sections.values().forEach(renderSection -> {
				if (!isOutsideRenderDistance(viewport.getTransform(), renderSection, searchDistance) && !isOutsideFrustum(viewport, renderSection)) {
					visitor.accept(renderSection);
				}
			});
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
}
