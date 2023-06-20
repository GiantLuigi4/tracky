package com.tracky.mixin.client.render.rendersource;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.tracky.TrackyAccessor;
import com.tracky.api.RenderSource;
import com.tracky.util.list.ObjectUnionList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Shadow
	@Nullable
	private ViewArea viewArea;
	
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
	@Final
	private AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage;
	
	@Shadow
	@Nullable
	private ClientLevel level;
	
	@Unique
	List<LevelRenderer.RenderChunkInfo> chunksToRender = new ObjectArrayList<>();
	
	@Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunksInFrustum:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"), method = "renderLevel")
	public ObjectArrayList<LevelRenderer.RenderChunkInfo> preRenderBEs(LevelRenderer instance) {
		//noinspection unchecked
		ObjectUnionList<LevelRenderer.RenderChunkInfo> copy = new ObjectUnionList<>(renderChunksInFrustum);
		
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values()) {
			for (RenderSource source : value.get()) {
				if (!source.canDraw(
						Minecraft.getInstance().gameRenderer.getMainCamera(),
						capturedFrustum == null ? cullingFrustum : capturedFrustum
				)) continue;
				
				ArrayList<LevelRenderer.RenderChunkInfo> infos = new ArrayList<>();
				for (ChunkRenderDispatcher.RenderChunk renderChunk : source.getChunksInFrustum()) {
					LevelRenderer.RenderChunkInfo info = renderChunkStorage.get().renderInfoMap.get(renderChunk);
					if (info != null)
						infos.add(info);
				}
				
				if (!infos.isEmpty())
					copy.addList(infos);
			}
		}
		
		return copy;
	}
	
	/* force chunk mesh rebaking on dirty chunks */
	@Inject(at = @At("HEAD"), method = "compileChunks")
	public void preCompileChunks(Camera p_194371_, CallbackInfo ci) {
		HashSet<LevelRenderer.RenderChunkInfo> settedFrustum = new HashSet<>(this.renderChunksInFrustum);
		
		int initialSize = settedFrustum.size();
		
		//noinspection unchecked
		renderChunksInFrustum = new ObjectUnionList<>(renderChunksInFrustum);
		
		out:
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values()) {
			for (RenderSource source : value.get()) {
				for (ChunkRenderDispatcher.RenderChunk renderChunk : source.getChunksInFrustum()) {
					if (settedFrustum.size() > (initialSize + 1000)) break out;
					
					if (renderChunk != null && renderChunk.isDirty()) {
						LevelRenderer.RenderChunkInfo info = renderChunkStorage.get().renderInfoMap.get(renderChunk);
						
						if (info != null && !settedFrustum.contains(info)) {
							this.renderChunksInFrustum.add(info);
							chunksToRender.add(info);
							settedFrustum.add(info);
						}
					}
				}
			}
		}
	}
	
	/* remove the chunks which were forced from the in frustum list */
	@Inject(at = @At("TAIL"), method = "compileChunks")
	public void postCompileChunks(Camera p_194371_, CallbackInfo ci) {
		//noinspection rawtypes
		if (renderChunksInFrustum instanceof ObjectUnionList list)
			//noinspection unchecked
			renderChunksInFrustum = (ObjectArrayList<LevelRenderer.RenderChunkInfo>) list.getList(0);
		chunksToRender.clear();
	}
	
	/* allow sources to request baking of new sections and also get the new sections added to the list of existing render chunks */
	@Inject(method = "updateRenderChunks", at = @At(value = "TAIL"))
	private void updateRenderChunks(LinkedHashSet<LevelRenderer.RenderChunkInfo> pChunkInfos, LevelRenderer.RenderInfoMap pInfoMap, Vec3 pViewVector, Queue<LevelRenderer.RenderChunkInfo> pInfoQueue, boolean pShouldCull, CallbackInfo ci) {
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values())
			for (RenderSource source : value.get())
				source.updateChunks(viewArea, pChunkInfos, pInfoMap);
	}
	
	/* allows sources to dump their chunk lists and request updates */
	@Inject(at = @At("TAIL"), method = "allChanged")
	public void postChanged(CallbackInfo ci) {
		if (level != null) // level can be null here
			for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values())
				for (RenderSource source : value.get())
					source.refresh();
	}
	
	/* allows sources to dump their chunk lists and request updates */
	@Inject(at = @At("TAIL"), method = "setLevel")
	public void postSetLevel(ClientLevel pLevel, CallbackInfo ci) {
		if (pLevel != null) // ???
			for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(pLevel).values())
				for (RenderSource source : value.get())
					source.refresh();
	}
	
	/* allows render sources to perform frustum culling when vanilla does */
	@Inject(at = @At("TAIL"), method = "applyFrustum")
	public void postApplyFrustum(Frustum pFrustrum, CallbackInfo ci) {
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values())
			for (RenderSource source : value.get())
				source.doFrustumUpdate(Minecraft.getInstance().getBlockEntityRenderDispatcher().camera, pFrustrum);
	}
	
	/* invokes rendering of render sources */
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V"), method = "renderChunkLayer")
	public void postRenderBlocks(RenderType pRenderType, PoseStack pPoseStack, double pCamX, double pCamY, double pCamZ, Matrix4f pProjectionMatrix, CallbackInfo ci) {
		ShaderInstance instance = RenderSystem.getShader();
		
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values()) {
			for (RenderSource source : value.get()) {
				if (source.canDraw(
						Minecraft.getInstance().gameRenderer.getMainCamera(),
						capturedFrustum == null ? cullingFrustum : capturedFrustum
				)) {
					if (source.needsCulling()) {
						source.doFrustumUpdate(
								Minecraft.getInstance().gameRenderer.getMainCamera(),
								capturedFrustum == null ? cullingFrustum : capturedFrustum
						);
					}
					
					//noinspection ConstantConditions
					source.draw(pPoseStack, viewArea, instance, pRenderType, pCamX, pCamY, pCamZ);
				}
			}
		}
	}
}
