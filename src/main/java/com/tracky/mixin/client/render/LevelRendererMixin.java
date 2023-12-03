package com.tracky.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.TrackyAccessor;
import com.tracky.access.RenderChunkExtensions;
import com.tracky.api.RenderSource;
import com.tracky.util.TrackyChunkInfoMap;
import com.tracky.util.TrackyViewArea;
import com.tracky.util.list.ObjectUnionList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	@Shadow
	private Frustum cullingFrustum;

	@Shadow
	@Nullable
	private Frustum capturedFrustum;

	@Shadow
	@Final
	@Mutable
	private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;

	@Shadow
	@Nullable
	private ClientLevel level;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Nullable
	private ChunkRenderDispatcher chunkRenderDispatcher;
	@Unique
	private final TrackyChunkInfoMap tracky$chunkInfoMap = new TrackyChunkInfoMap();

	@Unique
	private final Set<ChunkRenderDispatcher.RenderChunk> tracky$chunksToRender = new ObjectArraySet<>();

	@Unique
	private TrackyViewArea tracky$ViewArea;

	// FIXME Sometimes the chunks don't seem to fully load when the level is first set. It's inconsistent, so it might be a threading issue

	@SuppressWarnings("unchecked")
	@Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunksInFrustum:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"), method = "renderLevel")
	public ObjectArrayList<LevelRenderer.RenderChunkInfo> preRenderBEs(LevelRenderer instance) {
		ObjectUnionList<LevelRenderer.RenderChunkInfo> copy = new ObjectUnionList<>(this.renderChunksInFrustum);
		Camera mainCamera = this.minecraft.gameRenderer.getMainCamera();
		Frustum frustum = this.capturedFrustum == null ? this.cullingFrustum : this.capturedFrustum;

		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
			for (RenderSource source : value.get()) {
				if (!source.canDraw(mainCamera, frustum)) {
					continue;
				}

				ArrayList<LevelRenderer.RenderChunkInfo> infos = null;
				for (ChunkRenderDispatcher.RenderChunk renderChunk : source.getChunksInFrustum()) {
					LevelRenderer.RenderChunkInfo info = this.tracky$chunkInfoMap.get(renderChunk);
					if (info != null) {
						if (infos == null) {
							infos = new ArrayList<>();
						}
						infos.add(info);
					}
				}

				if (infos != null) {
					copy.addList(infos);
				}
			}
		}

		return copy;
	}

	/* force chunk mesh rebaking on dirty chunks */
	@SuppressWarnings("unchecked")
	@Inject(at = @At("HEAD"), method = "compileChunks")
	public void preCompileChunks(Camera p_194371_, CallbackInfo ci) {
		// No point trying to compile render source chunks if nothing needs to be compiled
		if (this.tracky$chunksToRender.isEmpty()) {
			return;
		}

		// TODO only compile render chunks that are in the frustum

		// This is a fast way to inject the render source chunks into the list for future iteration
		this.renderChunksInFrustum = new ObjectUnionList<>(this.renderChunksInFrustum);
		HashSet<LevelRenderer.RenderChunkInfo> settedFrustum = new HashSet<>();
		Iterator<ChunkRenderDispatcher.RenderChunk> iterator = this.tracky$chunksToRender.iterator();

		// TODO Do we need to set a max limit on the number of render source chunks that can be compiled at once?
		while (iterator.hasNext() && settedFrustum.size() < 1000) {
			ChunkRenderDispatcher.RenderChunk renderChunk = iterator.next();

			if (renderChunk.isDirty()) {
				LevelRenderer.RenderChunkInfo info = this.tracky$chunkInfoMap.getOrCreate(renderChunk);
				if (settedFrustum.add(info)) {
					this.renderChunksInFrustum.add(info);
				}
			}

			// We successfully processed the chunk, so we don't have to check it anymore
			iterator.remove();
		}
	}

	/* remove the chunks which were forced from the in frustum list */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(at = @At("RETURN"), method = "compileChunks")
	public void postCompileChunks(Camera p_194371_, CallbackInfo ci) {
		if (this.renderChunksInFrustum instanceof ObjectUnionList list) {
			this.renderChunksInFrustum = (ObjectArrayList<LevelRenderer.RenderChunkInfo>) list.getList(0);
		}
	}

	/* allow sources to request baking of new sections and also get the new sections added to the list of existing render chunks */
	@Inject(method = "setupRender", at = @At(value = "TAIL"))
	private void updateRenderChunks(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator, CallbackInfo ci) {
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
			for (RenderSource source : value.get()) {
				source.updateChunks(this.tracky$ViewArea, renderChunk -> {
					((RenderChunkExtensions) renderChunk).tracky$setRenderSource(source);
					this.tracky$chunksToRender.add(renderChunk);
				});
			}
		}
	}

	/* updates the valid render chunks in view for a render source */
	@Inject(method = "setupRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer$RenderInfoMap;get(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;)Lnet/minecraft/client/renderer/LevelRenderer$RenderChunkInfo;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
	private void updateCompiledChunks(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator, CallbackInfo ci, Vec3 vec3, double d0, double d1, double d2, int i, int j, int k, BlockPos blockpos, double d3, double d4, double d5, boolean flag, LevelRenderer.RenderChunkStorage levelrenderer$renderchunkstorage, Queue<LevelRenderer.RenderChunkInfo> queue, ChunkRenderDispatcher.RenderChunk renderChunk) {
		RenderSource renderSource = ((RenderChunkExtensions) renderChunk).tracky$getRenderSource();
		if (renderSource != null) {
			renderSource.updateCompiledChunk(renderChunk);
		}
	}

	/* allows sources to dump their chunk lists and request updates */
	@Inject(at = @At("TAIL"), method = "allChanged")
	public void postChanged(CallbackInfo ci) {
		// These chunks are no longer valid, so the render sources have to re-submit them
		this.tracky$chunkInfoMap.clear();
		this.tracky$chunksToRender.clear();

		if (this.level != null) { // level can be null here
			if (this.tracky$ViewArea != null) {
				this.tracky$ViewArea.releaseBuffers();
			}
			this.tracky$ViewArea = new TrackyViewArea(this.chunkRenderDispatcher, this.level, this.tracky$chunksToRender);

			for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
				for (RenderSource source : value.get()) {
					source.refresh();
				}
			}
		}
	}

	/* allows sources to delete any resources they have allocated */
	@Inject(at = @At("HEAD"), method = "setLevel")
	public void freeSources(ClientLevel level, CallbackInfo ci) {
		if (level == null && this.tracky$ViewArea != null) {
			this.tracky$ViewArea.releaseBuffers();
			this.tracky$ViewArea = null;
		}
		if (this.level != null && this.level != level) { // The level is about to be changed to something else, so free render sources
			for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
				for (RenderSource source : value.get()) {
					source.free();
				}
			}
		}
	}

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("TAIL"))
	public void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread, CallbackInfo ci) {
		this.tracky$ViewArea.setDirty(sectionX, sectionY, sectionZ, reRenderOnMainThread);
	}

	/* allows render sources to perform frustum culling when vanilla does */
	@Inject(at = @At("TAIL"), method = "applyFrustum")
	public void postApplyFrustum(Frustum pFrustrum, CallbackInfo ci) {
		ProfilerFiller profiler = this.minecraft.getProfiler();

		profiler.push("tracky_apply_frustum");
		Camera camera = this.minecraft.getBlockEntityRenderDispatcher().camera;
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
			for (RenderSource source : value.get()) {
				source.doFrustumUpdate(camera, pFrustrum);
			}
		}
		profiler.pop();
	}

	/* invokes rendering of render sources */
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V"), method = "renderChunkLayer")
	public void postRenderBlocks(RenderType renderType, PoseStack stack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo ci) {
		ShaderInstance instance = Objects.requireNonNull(RenderSystem.getShader(), "shader");
		Camera mainCamera = this.minecraft.gameRenderer.getMainCamera();
		Frustum frustum = this.capturedFrustum == null ? this.cullingFrustum : this.capturedFrustum;

		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
			for (RenderSource source : value.get()) {
				if (source.canDraw(mainCamera, frustum)) {
					if (source.needsCulling()) {
						source.doFrustumUpdate(mainCamera, frustum);
					}

					source.draw(stack, this.tracky$ViewArea, instance, renderType, camX, camY, camZ);
				}
			}
		}
	}
}
