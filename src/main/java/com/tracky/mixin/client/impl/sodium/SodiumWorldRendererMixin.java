package com.tracky.mixin.client.impl.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.TrackyAccessor;
import com.tracky.access.ExtendedBlockEntityRenderDispatcher;
import com.tracky.access.sodium.ExtendedSodiumWorldRenderer;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyViewArea;
import com.tracky.impl.TrackyRenderSectionManager;
import com.tracky.mixin.client.render.LevelRendererAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Supplier;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererMixin implements ExtendedSodiumWorldRenderer {

	@Shadow
	@Final
	private Minecraft client;
	@Shadow
	private ClientLevel world;

	@Shadow
	private RenderSectionManager renderSectionManager;

	@Shadow
	private static void renderBlockEntity(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, float tickDelta, MultiBufferSource.BufferSource immediate, double x, double y, double z, BlockEntityRenderDispatcher dispatcher, BlockEntity entity) {
	}

	@Unique
	private final List<RenderSource> tracky$renderSources = new LinkedList<>();

	@Unique
	private final Map<RenderSource, RenderSectionManager> tracky$renderSectionManagers = new Object2ObjectArrayMap<>();

	@Unique
	private Frustum tracky$getFrustum() {
		LevelRendererAccessor accessor = (LevelRendererAccessor) this.client.levelRenderer;
		return Objects.requireNonNullElse(accessor.getCapturedFrustum(), accessor.getCullingFrustum());
	}

	@Unique
	private void tracky$freeRenderers() {
		this.tracky$renderSectionManagers.values().forEach(RenderSectionManager::destroy);
		this.tracky$renderSectionManagers.clear();
	}

	/* allows sources to delete any resources they have allocated */
	@Inject(at = @At("HEAD"), method = "setWorld")
	public void freeSources(ClientLevel level, CallbackInfo ci) {
		this.tracky$freeRenderers();
		if (this.world != null && this.world != level) { // The level is about to be changed to something else, so free render sources
			for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.world).values()) {
				for (RenderSource source : value.get()) {
					source.free();
				}
			}
		}
	}

	@Inject(method = "initRenderer", at = @At("HEAD"))
	public void freeRenderers(CommandList commandList, CallbackInfo ci) {
		this.tracky$freeRenderers();
	}

	@Inject(method = "getVisibleChunkCount", at = @At("TAIL"), cancellable = true)
	public void getVisibleChunkCount(CallbackInfoReturnable<Integer> cir) {
		if (this.tracky$renderSectionManagers.isEmpty()) {
			return;
		}

		int count = cir.getReturnValueI();
		for (RenderSectionManager sectionManager : this.tracky$renderSectionManagers.values()) {
			count += sectionManager.getVisibleChunkCount();
		}
		cir.setReturnValue(count);
	}

	@Inject(method = "scheduleTerrainUpdate", at = @At("TAIL"))
	public void scheduleTerrainUpdate(CallbackInfo ci) {
		this.tracky$renderSectionManagers.values().forEach(RenderSectionManager::markGraphDirty);
	}

	@Inject(method = "isTerrainRenderComplete", at = @At("HEAD"), cancellable = true)
	public void isTerrainRenderComplete(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			for (RenderSectionManager sectionManager : this.tracky$renderSectionManagers.values()) {
				if (!sectionManager.getBuilder().isBuildQueueEmpty()) {
					cir.setReturnValue(false);
					break;
				}
			}
		}
	}

	@Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSectionManager;markGraphDirty()V"))
	public void markGraphDirty(Camera camera, Viewport viewport, int frame, boolean spectator, boolean updateChunksImmediately, CallbackInfo ci) {
		Frustum frustum = this.tracky$getFrustum();
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.world).values()) {
			for (RenderSource source : value.get()) {
				source.doFrustumUpdate(camera, frustum);
			}
		}
		this.tracky$renderSectionManagers.values().forEach(RenderSectionManager::markGraphDirty);
	}

	@Inject(method = "setupTerrain", at = @At("TAIL"))
	public void setupTrackyChunks(Camera camera, Viewport viewport, int frame, boolean spectator, boolean updateChunksImmediately, CallbackInfo ci) {
		ProfilerFiller profiler = this.client.getProfiler();
		Frustum frustum = this.tracky$getFrustum();

		this.tracky$renderSources.clear();
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.world).values()) {
			for (RenderSource source : value.get()) {
				RenderSectionManager renderSectionManager = this.tracky$getRenderSectionManager(source);
				TrackyViewArea viewArea = (TrackyViewArea) renderSectionManager;

				profiler.push("tracky_update_chunks");
				source.updateChunks(viewArea, renderChunk -> {
					renderChunk.setRenderSource(source);
					viewArea.setDirty(renderChunk.getSectionPos(), updateChunksImmediately);
				});
				profiler.popPush("tracky_apply_frustum");
				if (source.needsFrustumUpdate()) {
					source.doFrustumUpdate(camera, frustum);
				}
				profiler.pop();

				if (source.canDraw(camera, frustum)) {
					this.tracky$renderSources.add(source);
				}
			}
		}

		this.tracky$renderSectionManagers.values().forEach(renderSectionManager -> {
			profiler.push("chunk_update");
			renderSectionManager.updateChunks(updateChunksImmediately);
			profiler.popPush("chunk_upload");
			renderSectionManager.uploadChunks();
			if (renderSectionManager.needsUpdate()) {
				profiler.popPush("chunk_render_lists");
				renderSectionManager.update(camera, viewport, frame, spectator);
			}

			if (updateChunksImmediately) {
				profiler.popPush("chunk_upload_immediately");
				renderSectionManager.uploadChunks();
			}

			profiler.popPush("chunk_render_tick");
			renderSectionManager.tickVisibleRenders();
			profiler.pop();
		});
	}

	@Inject(method = "drawChunkLayer", at = @At("TAIL"))
	public void drawRenderSources(RenderType renderLayer, PoseStack matrixStack, double x, double y, double z, CallbackInfo ci) {
		if (renderLayer == RenderType.solid() || renderLayer == RenderType.translucent()) {
			for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.world).values()) {
				for (RenderSource source : value.get()) {
					TrackyRenderSectionManager sectionManager = (TrackyRenderSectionManager) this.tracky$getRenderSectionManager(source);
					sectionManager.setup(source, matrixStack, x, y, z);
					source.draw(sectionManager, matrixStack, (TrackyViewArea) sectionManager, renderLayer, x, y, z);
					sectionManager.reset();
				}
			}
		}
	}

	/* allows sources to dump their chunk lists and request updates */
	@Inject(at = @At("TAIL"), method = "reload")
	public void refresh(CallbackInfo ci) {
		if (this.world != null) { // level can be null here
			for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(this.world).values()) {
				for (RenderSource source : value.get()) {
					source.refresh();
				}
			}
		}
	}

	@Inject(method = "renderBlockEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderBuffers;Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;Lnet/minecraft/client/Camera;F)V", at = @At("TAIL"))
	public void drawRenderSourceBlockEntities(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, Camera camera, float tickDelta, CallbackInfo ci) {
		MultiBufferSource.BufferSource immediate = bufferBuilders.bufferSource();
		Vec3 cameraPos = camera.getPosition();
		double x = cameraPos.x();
		double y = cameraPos.y();
		double z = cameraPos.z();
		BlockEntityRenderDispatcher blockEntityRenderer = this.client.getBlockEntityRenderDispatcher();

		for (RenderSource renderSource : this.tracky$renderSources) {
			RenderSectionManager sectionManager = this.tracky$getRenderSectionManager(renderSource);
			Iterator<ChunkRenderList> iterator = sectionManager.getRenderLists().iterator();
			Matrix4f transformation = renderSource.getTransformation(x, y, z);

			matrices.pushPose();
			matrices.mulPoseMatrix(transformation);

			Vector3f cameraPosition = new Vector3f();
			transformation.invert().transformPosition(cameraPosition);
			cameraPosition.add((float) x, (float) y, (float) z);
			((ExtendedBlockEntityRenderDispatcher) blockEntityRenderer).tracky$setCameraPosition(new Vec3(cameraPosition));

			while (iterator.hasNext()) {
				ChunkRenderList renderList = iterator.next();
				RenderRegion renderRegion = renderList.getRegion();
				ByteIterator renderSectionIterator = renderList.sectionsWithEntitiesIterator();
				if (renderSectionIterator != null) {
					while (renderSectionIterator.hasNext()) {
						int renderSectionId = renderSectionIterator.nextByteAsInt();
						RenderSection renderSection = renderRegion.getSection(renderSectionId);
						BlockEntity[] blockEntities = renderSection.getCulledBlockEntities();
						if (blockEntities != null) {
							for (BlockEntity blockEntity : blockEntities) {
								renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
							}
						}
					}
				}
			}

			for (RenderSection renderSection : sectionManager.getSectionsWithGlobalEntities()) {
				BlockEntity[] blockEntities = renderSection.getGlobalBlockEntities();
				if (blockEntities != null) {
					for (BlockEntity blockEntity : blockEntities) {
						renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
					}
				}
			}
			matrices.popPose();
		}

		((ExtendedBlockEntityRenderDispatcher) blockEntityRenderer).tracky$setCameraPosition(null);
	}

	@Inject(method = "scheduleRebuildForChunk", at = @At("TAIL"))
	public void scheduleRebuildForChunk(int x, int y, int z, boolean important, CallbackInfo ci) {
		for (RenderSectionManager sectionManager : this.tracky$renderSectionManagers.values()) {
			sectionManager.scheduleRebuild(x, y, z, important);
		}
	}

	@Override
	public RenderSectionManager tracky$getRenderSectionManager(@Nullable RenderSource source) {
		if (source == null) {
			return this.renderSectionManager;
		}

		RenderSectionManager sectionManager = this.tracky$renderSectionManagers.get(source);
		if (sectionManager == null) {
			try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
				sectionManager = new TrackyRenderSectionManager(this.world, commandList);
				this.tracky$renderSectionManagers.put(source, sectionManager);
			}
		}
		return sectionManager;
	}
}
