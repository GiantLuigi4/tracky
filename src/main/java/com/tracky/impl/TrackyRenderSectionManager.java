package com.tracky.impl;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.access.ExtendedOcclusionCuller;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyChunkRenderer;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.mixin.client.impl.sodium.RenderSectionManagerAccessor;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.Collection;

@ApiStatus.Internal
public class TrackyRenderSectionManager extends RenderSectionManager implements TrackyChunkRenderer {

	private final ExtendedOcclusionCuller extendedOcclusionCuller;
	private final Matrix4f projection = new Matrix4f();
	private final Matrix4f modelView = new Matrix4f();
	private final float[] fogColor = new float[4];
	private final Vector3f cameraPos = new Vector3f();

	private Matrix4fc regionTransform;
	private float fogStart;
	private float fogEnd;
	private FogShape fogShape;

	public TrackyRenderSectionManager(ClientLevel world, CommandList commandList) {
		super(world, Integer.MAX_VALUE, commandList);
		this.extendedOcclusionCuller = (ExtendedOcclusionCuller) ((RenderSectionManagerAccessor) this).getOcclusionCuller();
	}

	@Override
	public void update(Camera camera, Viewport viewport, int frame, boolean spectator) {
		this.extendedOcclusionCuller.tracky$skipChecks();
		super.update(camera, viewport, frame, spectator);
	}

	@Override
	public void setModelViewMatrix(Matrix4fc matrix) {
		this.modelView.set(matrix);
	}

	@Override
	public void setProjectionMatrix(Matrix4fc matrix) {
		RenderSystem.getProjectionMatrix().set(matrix);
	}

	@Override
	public void setFogStart(float fogStart) {
		RenderSystem.setShaderFogStart(fogStart);
	}

	@Override
	public void setFogEnd(float fogEnd) {
		RenderSystem.setShaderFogEnd(fogEnd);
	}

	@Override
	public void setFogShape(FogShape shape) {
		RenderSystem.setShaderFogShape(shape);
	}

	@Override
	public void setFogColor(float red, float green, float blue, float alpha) {
		RenderSystem.setShaderFogColor(red, green, blue, alpha);
	}

	@Override
	public void render(Collection<TrackyRenderChunk> chunks, RenderType layer) {
		SodiumGameOptions.PerformanceSettings config = SodiumClientMod.options().performance;
		boolean useBlockFaceCulling = config.useBlockFaceCulling;
		config.useBlockFaceCulling = false; // TODO make it so this can work

		ChunkRenderMatrices matrices = new ChunkRenderMatrices(this.projection, this.modelView);
		if (layer == RenderType.solid()) {
			this.renderLayer(matrices, DefaultTerrainRenderPasses.SOLID, this.cameraPos.x, this.cameraPos.y, this.cameraPos.z);
			this.renderLayer(matrices, DefaultTerrainRenderPasses.CUTOUT, this.cameraPos.x, this.cameraPos.y, this.cameraPos.z);
		} else if (layer == RenderType.translucent()) {
			this.renderLayer(matrices, DefaultTerrainRenderPasses.TRANSLUCENT, this.cameraPos.x, this.cameraPos.y, this.cameraPos.z);
		}

		config.useBlockFaceCulling = useBlockFaceCulling;
	}

	public void setup(RenderSource source, PoseStack stack, double cameraX, double cameraY, double cameraZ) {
		this.modelView.set(stack.last().pose());
		this.projection.set(RenderSystem.getProjectionMatrix());
		this.cameraPos.set(cameraX, cameraY, cameraZ);

		// Back up fog parameters
		this.fogStart = RenderSystem.getShaderFogStart();
		this.fogEnd = RenderSystem.getShaderFogEnd();
		this.fogShape = RenderSystem.getShaderFogShape();
		System.arraycopy(RenderSystem.getShaderFogColor(), 0, this.fogColor, 0, 4);

		PoseStack poseStack = new PoseStack();
		poseStack.translate(-cameraX, -cameraY, -cameraZ);
		source.transform(poseStack, cameraX, cameraY, cameraZ);
		poseStack.translate(cameraX, cameraY, cameraZ);
		this.regionTransform = poseStack.last().pose();
	}

	public void reset() {
		RenderSystem.getProjectionMatrix().set(this.projection);
		RenderSystem.setShaderFogStart(this.fogStart);
		RenderSystem.setShaderFogEnd(this.fogEnd);
		RenderSystem.setShaderFogShape(this.fogShape);
		System.arraycopy(this.fogColor, 0, RenderSystem.getShaderFogColor(), 0, 4);
		this.regionTransform = null;
	}
}
