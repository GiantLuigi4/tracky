package com.tracky.impl;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.access.sodium.ExtendedDefaultChunkRenderer;
import com.tracky.access.sodium.ExtendedOcclusionCuller;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyChunkRenderer;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.mixin.client.impl.sodium.RenderSectionManagerAccessor;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.util.Collection;

@ApiStatus.Internal
public class TrackyRenderSectionManager extends RenderSectionManager implements TrackyChunkRenderer {

	private final RenderSource renderSource;
	private final Vector3d chunkOffset = new Vector3d();
	private final Matrix4f projection = new Matrix4f();
	private final Matrix4f modelView = new Matrix4f();
	private final float[] fogColor = new float[4];

	private CameraTransform cameraTransform;
	private float fogStart;
	private float fogEnd;
	private FogShape fogShape;

	private final Vector3i minSection;
	private final Vector3i maxSection;
	private final Vector3f centerSection;

	public TrackyRenderSectionManager(ClientLevel level, int renderDistance, CommandList commandList, @Nullable RenderSource renderSource) {
		super(level, renderDistance, commandList);
		this.renderSource = renderSource;

		// I don't want to repeat specific sections, so these are offset by 1 to prevent overflows. This is really just an edge case before any sections are added
		this.minSection = new Vector3i(Integer.MAX_VALUE - 1);
		this.maxSection = new Vector3i(Integer.MIN_VALUE + 1);
		this.centerSection = new Vector3f();

		ExtendedOcclusionCuller culler = (ExtendedOcclusionCuller) ((RenderSectionManagerAccessor) this).getOcclusionCuller();
		culler.tracky$setRenderSource(renderSource);
		culler.tracky$setBounds(this.minSection, this.maxSection);
	}

	private void updateBounds(Vector3ic pos) {
		this.minSection.min(pos);
		this.maxSection.max(pos);
		this.centerSection.set((this.minSection.x + this.maxSection.x) << 3, (this.minSection.y + this.maxSection.y) << 3, (this.minSection.z + this.maxSection.z) << 3);
		((ExtendedOcclusionCuller) ((RenderSectionManagerAccessor) this).getOcclusionCuller()).tracky$setBounds(this.minSection, this.maxSection);
	}

	@Override
	public void onSectionAdded(int x, int y, int z) {
		super.onSectionAdded(x, y, z);
		if (this.needsUpdate()) {
			this.updateBounds(new Vector3i(x, y, z));
		}
	}

	@Override
	public void onSectionRemoved(int x, int y, int z) {
		super.onSectionRemoved(x, y, z);
		if (this.needsUpdate()) {
			this.updateBounds(new Vector3i(x, y, z));
		}
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
	public void setFogColor(float[] colors) {
		System.arraycopy(colors, 0, RenderSystem.getShaderFogColor(), 0, 4);
	}

	@Override
	public void render(Collection<TrackyRenderChunk> chunks, RenderType layer) {
		ExtendedDefaultChunkRenderer chunkRenderer = (ExtendedDefaultChunkRenderer) ((RenderSectionManagerAccessor) this).getChunkRenderer();
		chunkRenderer.tracky$setCameraTransform(this.cameraTransform);

		ChunkRenderMatrices matrices = new ChunkRenderMatrices(this.projection, this.modelView);
		if (layer == RenderType.solid()) {
			this.renderLayer(matrices, DefaultTerrainRenderPasses.SOLID, -this.chunkOffset.x, -this.chunkOffset.y, -this.chunkOffset.z);
			this.renderLayer(matrices, DefaultTerrainRenderPasses.CUTOUT, -this.chunkOffset.x, -this.chunkOffset.y, -this.chunkOffset.z);
		} else if (layer == RenderType.translucent()) {
			this.renderLayer(matrices, DefaultTerrainRenderPasses.TRANSLUCENT, -this.chunkOffset.x, -this.chunkOffset.y, -this.chunkOffset.z);
		}

		chunkRenderer.tracky$setCameraTransform(null);
	}


	public void setup(RenderSource source, PoseStack stack, double camX, double camY, double camZ) {
		this.modelView.set(stack.last().pose());
		this.projection.set(RenderSystem.getProjectionMatrix());
		this.chunkOffset.set(source.getChunkOffset());

		// Back up fog parameters
		this.fogStart = RenderSystem.getShaderFogStart();
		this.fogEnd = RenderSystem.getShaderFogEnd();
		this.fogShape = RenderSystem.getShaderFogShape();
		System.arraycopy(RenderSystem.getShaderFogColor(), 0, this.fogColor, 0, 4);

		// Update camera position
//		Vector3f pos = new Vector3f((float) camX, (float) camY, (float) camZ);
		Vector3f pos = new Vector3f((float) camX, (float) camY, (float) camZ);
		source.getTransformation(0, 0, 0).invert().transformPosition(pos);
		this.cameraTransform = new CameraTransform(pos.x - this.chunkOffset.x, pos.y - this.chunkOffset.y, pos.z - this.chunkOffset.z);
	}

	public void reset() {
		RenderSystem.getProjectionMatrix().set(this.projection);
		RenderSystem.setShaderFogStart(this.fogStart);
		RenderSystem.setShaderFogEnd(this.fogEnd);
		RenderSystem.setShaderFogShape(this.fogShape);
		System.arraycopy(this.fogColor, 0, RenderSystem.getShaderFogColor(), 0, 4);
		this.cameraTransform = null;
	}

	public boolean disableOcclusionCulling(ClientLevel level, Camera camera, boolean spectator) {
		Vec3 cameraPos = camera.getPosition();
		Vector3f pos = new Vector3f((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
		this.renderSource.getTransformation(0, 0, 0).invert().transformPosition(pos);
		pos.sub((float) this.chunkOffset.x, (float) this.chunkOffset.y, (float) this.chunkOffset.z);
		BlockPos origin = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
		return spectator && level.getBlockState(origin).isSolidRender(level, origin);
	}
}
