package com.tracky.impl;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.tracky.api.TrackyChunkRenderer;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.mixin.client.impl.vanilla.UniformAccessor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.nio.FloatBuffer;
import java.util.Collection;

@ApiStatus.Internal
public class VanillaChunkRenderer implements TrackyChunkRenderer {

    private static final int MODELVIEW_MAT = 0b000001;
    private static final int PROJECTION_MAT = 0b000010;
    private static final int FOG_START = 0b000100;
    private static final int FOG_END = 0b001000;
    private static final int FOG_SHAPE = 0b010000;
    private static final int FOG_COLOR = 0b10000;

    protected final Vector3d cameraPos = new Vector3d();
    protected final Vector3d chunkOffset = new Vector3d();
    protected ShaderInstance shader;

    private int modified;

    private void set(Uniform uniform, Matrix4fc value) {
        UniformAccessor accessor = (UniformAccessor) uniform;
        FloatBuffer floatValues = accessor.getFloatValues();
        floatValues.position(0);
        value.get(floatValues);
        accessor.invokeMarkDirty();
    }

    @Override
    public void setModelViewMatrix(Matrix4fc matrix) {
        if (this.shader.MODEL_VIEW_MATRIX != null) {
            this.set(this.shader.MODEL_VIEW_MATRIX, matrix);
            this.shader.MODEL_VIEW_MATRIX.upload();
            this.modified |= MODELVIEW_MAT;
        }
    }

    @Override
    public void setProjectionMatrix(Matrix4fc matrix) {
        if (this.shader.PROJECTION_MATRIX != null) {
            this.set(this.shader.PROJECTION_MATRIX, matrix);
            this.shader.PROJECTION_MATRIX.upload();
            this.modified |= PROJECTION_MAT;
        }
    }

    @Override
    public void setFogStart(float fogStart) {
        if (this.shader.FOG_START != null) {
            this.shader.FOG_START.set(fogStart);
            this.shader.FOG_START.upload();
            this.modified |= FOG_START;
        }
    }

    @Override
    public void setFogEnd(float fogEnd) {
        if (this.shader.FOG_END != null) {
            this.shader.FOG_END.set(fogEnd);
            this.shader.FOG_END.upload();
            this.modified |= FOG_END;
        }
    }

    @Override
    public void setFogShape(FogShape shape) {
        if (this.shader.FOG_SHAPE != null) {
            this.shader.FOG_SHAPE.set(shape.getIndex());
            this.shader.FOG_SHAPE.upload();
            this.modified |= FOG_SHAPE;
        }
    }

    @Override
    public void setFogColor(float red, float green, float blue, float alpha) {
        if (this.shader.FOG_COLOR != null) {
            this.shader.FOG_COLOR.set(red, green, blue, alpha);
            this.shader.FOG_COLOR.upload();
            this.modified |= FOG_COLOR;
        }
    }

    @Override
    public void setFogColor(float[] colors) {
        if (this.shader.FOG_COLOR != null) {
            this.shader.FOG_COLOR.set(colors);
            this.shader.FOG_COLOR.upload();
            this.modified |= FOG_COLOR;
        }
    }

    @Override
    public void render(Collection<TrackyRenderChunk> chunks, RenderType layer) {
        Uniform uniform = this.shader.CHUNK_OFFSET;

        for (TrackyRenderChunk chunk : chunks) {
            ChunkRenderDispatcher.RenderChunk renderChunk = (ChunkRenderDispatcher.RenderChunk) chunk;
            if (renderChunk.getCompiledChunk().isEmpty(layer)) {
                continue;
            }

            if (uniform != null) {
                BlockPos pos = renderChunk.getOrigin();
                uniform.set((float) (pos.getX() - this.chunkOffset.x()), (float) (pos.getY() - this.chunkOffset.y()), (float) (pos.getZ() - this.chunkOffset.z()));
                uniform.upload();
            }

            VertexBuffer buffer = renderChunk.getBuffer(layer);
            buffer.bind();
            buffer.draw();
        }

        if (uniform != null) {
            uniform.set(0f, 0f, 0f);
        }
    }

    public void prepare(ShaderInstance shader, Vector3dc chunkOffset) {
        this.chunkOffset.set(
                -chunkOffset.x(),
                -chunkOffset.y(),
                -chunkOffset.z()
        );
        this.shader = shader;
        this.modified = 0;
    }

    public void reset() {
        if (this.modified == 0) {
            // added this to be consistent with what happens if modified is not 0
            this.shader = null;
            return;
        }

        if ((this.modified & MODELVIEW_MAT) > 0) {
            this.shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
            this.shader.MODEL_VIEW_MATRIX.upload();
        }

        if ((this.modified & PROJECTION_MAT) > 0) {
            this.shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
            this.shader.PROJECTION_MATRIX.upload();
        }

        if ((this.modified & FOG_START) > 0) {
            this.shader.FOG_START.set(RenderSystem.getShaderFogStart());
            this.shader.FOG_START.upload();
        }

        if ((this.modified & FOG_END) > 0) {
            this.shader.FOG_END.set(RenderSystem.getShaderFogEnd());
            this.shader.FOG_END.upload();
        }

        if ((this.modified & FOG_SHAPE) > 0) {
            this.shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
            this.shader.FOG_SHAPE.upload();
        }

        if ((this.modified & FOG_COLOR) > 0) {
            this.shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
            this.shader.FOG_COLOR.upload();
        }

        this.shader = null;
        this.modified = 0;
    }

    public ShaderInstance getShader() {
        return this.shader;
    }
}
