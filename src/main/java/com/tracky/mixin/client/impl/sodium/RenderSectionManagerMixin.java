package com.tracky.mixin.client.impl.sodium;

import com.tracky.access.sodium.ExtendedRenderSection;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.api.TrackyViewArea;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerMixin implements TrackyViewArea {

	@Shadow
	@Final
	private Long2ReferenceMap<RenderSection> sectionByPosition;

	@Shadow
	public abstract void scheduleRebuild(int x, int y, int z, boolean important);

	@Shadow
	public abstract void onSectionAdded(int x, int y, int z);

	@Shadow
	protected abstract RenderSection getRenderSection(int x, int y, int z);

	@Inject(method = "processChunkBuildResults", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSectionManager;updateSectionInfo(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;Lme/jellysquid/mods/sodium/client/render/chunk/data/BuiltSectionInfo;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
	public void updateCompiledChunks(ArrayList<ChunkBuildOutput> results, CallbackInfo ci, List<ChunkBuildOutput> filtered, Iterator<ChunkBuildOutput> iterator, ChunkBuildOutput result) {
		TrackyRenderChunk renderChunk = (TrackyRenderChunk) result.render;
		RenderSource renderSource = renderChunk.getRenderSource();
		if (renderSource != null) {
			renderSource.updateCompiledChunk(renderChunk);
		}
	}

	@Inject(method = "onSectionAdded", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegion;addSection(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;)V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
	public void setSectionManager(int x, int y, int z, CallbackInfo ci, long key, RenderRegion region, RenderSection section) {
		((ExtendedRenderSection) section).veil$setRenderSectionManager((RenderSectionManager) (Object) this);
	}

	@Override
	public @Nullable TrackyRenderChunk getRenderChunk(int sectionX, int sectionY, int sectionZ) {
		return (TrackyRenderChunk) this.getRenderSection(sectionX, sectionY, sectionZ);
	}

	@Override
	public void setDirty(int sectionX, int sectionY, int sectionZ, boolean priority) {
		RenderSection section = this.sectionByPosition.get(SectionPos.asLong(sectionX, sectionY, sectionZ));
		if (section != null) {
			this.scheduleRebuild(sectionX, sectionY, sectionZ, priority);
		} else {
			this.onSectionAdded(sectionX, sectionY, sectionZ);
		}
	}
}
