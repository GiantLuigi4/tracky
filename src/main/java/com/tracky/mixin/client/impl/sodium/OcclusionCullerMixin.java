package com.tracky.mixin.client.impl.sodium;

import com.tracky.access.sodium.ExtendedOcclusionCuller;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = OcclusionCuller.class, remap = false)
public abstract class OcclusionCullerMixin implements ExtendedOcclusionCuller {

	@Shadow
	@Final
	private Long2ReferenceMap<RenderSection> sections;

	@Unique
	private boolean tracky$skipChecks;

	@Inject(method = "findVisible", at = @At("HEAD"), cancellable = true)
	public void findVisible(Consumer<RenderSection> visitor, Viewport viewport, float searchDistance, boolean useOcclusionCulling, int frame, CallbackInfo ci) {
		if (this.tracky$skipChecks) {
			this.tracky$skipChecks = false;
			this.sections.values().forEach(visitor);
			ci.cancel();
		}
	}

	@Override
	public void tracky$skipChecks() {
		this.tracky$skipChecks = true;
	}
}
