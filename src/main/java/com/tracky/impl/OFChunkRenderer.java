package com.tracky.impl;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.tracky.access.of.OFShadersAccessor;
import com.tracky.api.TrackyChunkRenderer;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.mixin.client.impl.vanilla.UniformAccessor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

import java.nio.FloatBuffer;
import java.util.Collection;

@ApiStatus.Internal
public class OFChunkRenderer extends VanillaChunkRenderer {

	boolean OFMode = false;
	
	@Override
	public void setModelViewMatrix(Matrix4fc matrix) {
		if (OFMode) {
			OFShadersAccessor.setModelViewMatrix(new Matrix4f(matrix));
		} else {
			super.setModelViewMatrix(matrix);
		}
	}

	@Override
	public void setProjectionMatrix(Matrix4fc matrix) {
		if (OFMode) {
			OFShadersAccessor.setProjection(new Matrix4f(matrix));
		} else {
			super.setProjectionMatrix(matrix);
		}
	}

	@Override
	public void setFogStart(float fogStart) {
		if (OFMode) {
			OFShadersAccessor.setFogStart(fogStart);
		} else {
			super.setFogStart(fogStart);
		}
	}

	@Override
	public void setFogEnd(float fogEnd) {
		if (OFMode) {
			OFShadersAccessor.setFogEnd(fogEnd);
		} else {
			super.setFogEnd(fogEnd);
		}
	}

	@Override
	public void setFogShape(FogShape shape) {
		if (OFMode) {
			OFShadersAccessor.setFogShape(shape.getIndex());
		} else {
			super.setFogShape(shape);
		}
	}

	@Override
	public void setFogColor(float red, float green, float blue, float alpha) {
		if (OFMode) {
			OFShadersAccessor.setFogColor(red, green, blue);
		} else {
			super.setFogColor(red, green, blue, alpha);
		}
	}

	@Override
	public void setFogColor(float[] colors) {
		if (OFMode) {
			OFShadersAccessor.setFogColor(colors[0], colors[1], colors[2]);
		} else {
			super.setFogColor(colors);
		}
	}

	@Override
	public void render(Collection<TrackyRenderChunk> chunks, RenderType layer) {
		if (OFMode) {
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

	public void prepare(ShaderInstance shader, double cameraX, double cameraY, double cameraZ) {
		OFMode = OFShadersAccessor.checkShadersActive();
		super.prepare(shader, cameraX, cameraY, cameraZ);
		this.cameraPos.set(cameraX, cameraY, cameraZ);
	}

	public void reset() {
		if (OFMode) {
			shader = null;
			
			setModelViewMatrix(RenderSystem.getModelViewMatrix());
			setProjectionMatrix(RenderSystem.getProjectionMatrix());
			setFogStart(RenderSystem.getShaderFogStart());
			setFogEnd(RenderSystem.getShaderFogEnd());
			setFogShape(RenderSystem.getShaderFogShape());
			setFogColor(RenderSystem.getShaderFogColor());
		} else {
			super.reset();
		}
	}

	public ShaderInstance getShader() {
		return this.shader;
	}
}
