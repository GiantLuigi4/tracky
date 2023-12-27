package com.tracky.impl;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.tracky.access.of.OFShadersAccessor;
import com.tracky.api.TrackyRenderChunk;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;

import java.util.Collection;

@ApiStatus.Internal
public class OFChunkRenderer extends VanillaChunkRenderer {

	private boolean shadersActive;

	@Override
	public void setModelViewMatrix(Matrix4fc matrix) {
		if (this.shadersActive) {
			OFShadersAccessor.setModelViewMatrix(new Matrix4f(matrix));
		} else {
			super.setModelViewMatrix(matrix);
		}
	}

	@Override
	public void setProjectionMatrix(Matrix4fc matrix) {
		if (this.shadersActive) {
			OFShadersAccessor.setProjection(new Matrix4f(matrix));
		} else {
			super.setProjectionMatrix(matrix);
		}
	}

	@Override
	public void setFogStart(float fogStart) {
		if (this.shadersActive) {
			OFShadersAccessor.setFogStart(fogStart);
		} else {
			super.setFogStart(fogStart);
		}
	}

	@Override
	public void setFogEnd(float fogEnd) {
		if (this.shadersActive) {
			OFShadersAccessor.setFogEnd(fogEnd);
		} else {
			super.setFogEnd(fogEnd);
		}
	}

	@Override
	public void setFogShape(FogShape shape) {
		if (this.shadersActive) {
			OFShadersAccessor.setFogShape(shape.getIndex());
		} else {
			super.setFogShape(shape);
		}
	}

	@Override
	public void setFogColor(float red, float green, float blue, float alpha) {
		if (this.shadersActive) {
			OFShadersAccessor.setFogColor(red, green, blue);
		} else {
			super.setFogColor(red, green, blue, alpha);
		}
	}

	@Override
	public void setFogColor(float[] colors) {
		if (this.shadersActive) {
			OFShadersAccessor.setFogColor(colors[0], colors[1], colors[2]);
		} else {
			super.setFogColor(colors);
		}
	}

	@Override
	public void render(Collection<TrackyRenderChunk> chunks, RenderType layer) {
		if (this.shadersActive) {
			OFShadersAccessor.enableFog(false);
			for (TrackyRenderChunk chunk : chunks) {
				ChunkRenderDispatcher.RenderChunk renderChunk = (ChunkRenderDispatcher.RenderChunk) chunk;
				if (renderChunk.getCompiledChunk().isEmpty(layer)) {
					continue;
				}

				BlockPos pos = renderChunk.getOrigin();
				OFShadersAccessor.setChunkOffset(
						(float) ((double) pos.getX() - this.cameraPos.x),
						(float) ((double) pos.getY() - this.cameraPos.y),
						(float) ((double) pos.getZ() - this.cameraPos.z)
				);

				VertexBuffer buffer = renderChunk.getBuffer(layer);
				buffer.bind();
				buffer.draw();
			}

			OFShadersAccessor.setChunkOffset(0, 0, 0);
			OFShadersAccessor.enableFog(true);
		} else {
			super.render(chunks, layer);
		}
	}

	@Override
	public void prepare(ShaderInstance shader, Vector3dc chunkOffset) {
		this.shadersActive = OFShadersAccessor.checkShadersActive();
		super.prepare(shader, chunkOffset);
	}

	@Override
	public void reset() {
		if (this.shadersActive) {
			this.shader = null;

			this.setModelViewMatrix(RenderSystem.getModelViewMatrix());
			this.setProjectionMatrix(RenderSystem.getProjectionMatrix());
			this.setFogStart(RenderSystem.getShaderFogStart());
			this.setFogEnd(RenderSystem.getShaderFogEnd());
			this.setFogShape(RenderSystem.getShaderFogShape());
			this.setFogColor(RenderSystem.getShaderFogColor());

			// must be set to false after resetting uniforms, elsewise the game crashes
			this.shadersActive = false;
		} else {
			super.reset();
		}
	}
}
