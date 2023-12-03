package com.tracky.mixin.client.impl.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.TrackyAccessor;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.impl.TrackyChunkInfoMap;
import com.tracky.impl.TrackyVanillaViewArea;
import com.tracky.impl.VanillaChunkRenderer;
import com.tracky.util.list.ObjectUnionList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
	private TrackyVanillaViewArea tracky$ViewArea;

	@Unique
	private final VanillaChunkRenderer chunkRenderer = new VanillaChunkRenderer();

	// FIXME Sometimes the chunks don't seem to fully load when the level is first set. It's inconsistent, so it might be a threading issue

	@SuppressWarnings("unchecked")
	@Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunksInFrustum:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", shift = At.Shift.BEFORE))
	public void preRenderBEs(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera camera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
		ObjectUnionList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum = new ObjectUnionList<>(this.renderChunksInFrustum);
		Frustum frustum = this.capturedFrustum == null ? this.cullingFrustum : this.capturedFrustum;

		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
			for (RenderSource source : value.get()) {
				if (!source.canDraw(camera, frustum)) {
					continue;
				}

				List<LevelRenderer.RenderChunkInfo> infos = null;
				for (TrackyRenderChunk renderChunk : source.getChunksInFrustum()) {
					LevelRenderer.RenderChunkInfo info = this.tracky$chunkInfoMap.get((ChunkRenderDispatcher.RenderChunk) renderChunk);
					if (info != null) {
						if (infos == null) {
							infos = new ArrayList<>();
						}
						infos.add(info);
					}
				}

				if (infos != null) {
					renderChunksInFrustum.addList(infos);
				}
			}
		}

		if (renderChunksInFrustum.listSize() > 1) {
			this.renderChunksInFrustum = renderChunksInFrustum;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;", shift = At.Shift.BEFORE, ordinal = 0))
	public void postRenderBEs(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera camera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
		if (this.renderChunksInFrustum instanceof ObjectUnionList list) {
			this.renderChunksInFrustum = (ObjectArrayList<LevelRenderer.RenderChunkInfo>) list.getList(0);
		}
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
					renderChunk.setRenderSource(source);
					this.tracky$chunksToRender.add((ChunkRenderDispatcher.RenderChunk) renderChunk);
				});
			}
		}
	}

	/* updates the valid render chunks in view for a render source */
	@Inject(method = "setupRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer$RenderInfoMap;get(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;)Lnet/minecraft/client/renderer/LevelRenderer$RenderChunkInfo;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
	private void updateCompiledChunks(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator, CallbackInfo ci, Vec3 vec3, double d0, double d1, double d2, int i, int j, int k, BlockPos blockpos, double d3, double d4, double d5, boolean flag, LevelRenderer.RenderChunkStorage levelrenderer$renderchunkstorage, Queue<LevelRenderer.RenderChunkInfo> queue, ChunkRenderDispatcher.RenderChunk renderChunk) {
		RenderSource renderSource = ((TrackyRenderChunk) renderChunk).getRenderSource();
		// Don't bother trying to render the chunk at all if it can't be rendered
		if (renderSource != null && !renderChunk.getCompiledChunk().hasNoRenderableLayers()) {
			renderSource.updateCompiledChunk((TrackyRenderChunk) renderChunk);
		}
	}

	/* allows sources to dump their chunk lists and request updates */
	@Inject(at = @At("TAIL"), method = "allChanged")
	public void refresh(CallbackInfo ci) {
		// These chunks are no longer valid, so the render sources have to re-submit them
		this.tracky$chunkInfoMap.clear();
		this.tracky$chunksToRender.clear();

		if (this.level != null) { // level can be null here
			if (this.tracky$ViewArea != null) {
				this.tracky$ViewArea.free();
			}
			this.tracky$ViewArea = new TrackyVanillaViewArea(this.chunkRenderDispatcher, this.level, this.tracky$chunksToRender::add);

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
			this.tracky$ViewArea.free();
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

	/* Updates the tracky view area */
	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("TAIL"))
	public void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread, CallbackInfo ci) {
		this.tracky$ViewArea.setDirty(sectionX, sectionY, sectionZ, reRenderOnMainThread);
	}

	/* allows render sources to perform frustum culling when vanilla does */
	@Inject(at = @At("TAIL"), method = "applyFrustum")
	public void applyFrustumUpdate(Frustum pFrustrum, CallbackInfo ci) {
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
	public void renderSources(RenderType renderType, PoseStack stack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo ci) {
		ShaderInstance instance = Objects.requireNonNull(RenderSystem.getShader(), "shader");
		Camera mainCamera = this.minecraft.gameRenderer.getMainCamera();
		Frustum frustum = this.capturedFrustum == null ? this.cullingFrustum : this.capturedFrustum;

		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.level).values()) {
			for (RenderSource source : value.get()) {
				if (source.canDraw(mainCamera, frustum)) {
					if (source.needsFrustumUpdate()) {
						source.doFrustumUpdate(mainCamera, frustum);
					}

					this.chunkRenderer.prepare(instance, camX, camY, camZ);
					source.draw(this.chunkRenderer, stack, this.tracky$ViewArea, renderType, camX, camY, camZ);
					this.chunkRenderer.reset();
				}
			}
		}
	}
}
