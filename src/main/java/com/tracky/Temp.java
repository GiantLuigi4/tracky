package com.tracky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.tracky.mixin.client.render.VertexBufferAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;

public class Temp {
    public static void process(ChunkRenderDispatcher.RenderChunk renderChunk, RenderType renderType, Uniform uniform, PoseStack matrixStack, ChunkRenderDispatcher.CompiledChunk chunk, BlockPos origin, int x, int y, int z, double camX, double camY, double camZ, ChunkRenderDispatcher.CompiledChunk instance) {

        VertexBuffer vertexbuffer = renderChunk.getBuffer(renderType);
        BlockPos blockpos = renderChunk.getOrigin();
        if (uniform != null) {
            uniform.set((float)((double)blockpos.getX() - camX), (float)((double)blockpos.getY() - camY), (float)((double)blockpos.getZ() - camZ));
            uniform.upload();
        }

        vertexbuffer.drawChunkLayer();
    }

    public static void process(LevelRenderer self, VertexBuffer buffer, ChunkRenderDispatcher.RenderChunk renderChunk, Uniform uniform, PoseStack modelView, Matrix4f projectionMatrix, ChunkRenderDispatcher.CompiledChunk chunk, BlockPos origin, int x, int y, int z, double camX, double camY, double camZ, VertexBuffer instance) {

        if(true) {
            buffer.drawChunkLayer();
            return;
        }

        ShaderInstance shader = RenderSystem.getShader();
        modelView.pushPose();
        modelView.translate(512.0, 0.0, 0.0);
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(modelView.last().pose());
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projectionMatrix);
        }
        shader.apply();
        uniform.upload();
        modelView.popPose();

        buffer.drawChunkLayer();

        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(modelView.last().pose());
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projectionMatrix);
        }
        shader.apply();
        uniform.upload();
    }


    public static void drawWithShader(VertexBuffer buffer, Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShaderInstance) {
        VertexBufferAccessor accessor = (VertexBufferAccessor) buffer;

        if (accessor.getIndexCount() != 0) {
            RenderSystem.assertOnRenderThread();
            BufferUploader.reset();

            for (int i = 0; i < 12; ++i) {
                int j = RenderSystem.getShaderTexture(i);
                pShaderInstance.setSampler("Sampler" + i, j);
            }

            if (pShaderInstance.MODEL_VIEW_MATRIX != null) {
                pShaderInstance.MODEL_VIEW_MATRIX.set(pModelViewMatrix);
            }

            if (pShaderInstance.PROJECTION_MATRIX != null) {
                pShaderInstance.PROJECTION_MATRIX.set(pProjectionMatrix);
            }

            if (pShaderInstance.INVERSE_VIEW_ROTATION_MATRIX != null) {
                pShaderInstance.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
            }

            if (pShaderInstance.COLOR_MODULATOR != null) {
                pShaderInstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
            }

            if (pShaderInstance.FOG_START != null) {
                pShaderInstance.FOG_START.set(RenderSystem.getShaderFogStart());
            }

            if (pShaderInstance.FOG_END != null) {
                pShaderInstance.FOG_END.set(RenderSystem.getShaderFogEnd());
            }

            if (pShaderInstance.FOG_COLOR != null) {
                pShaderInstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
            }

            if (pShaderInstance.FOG_SHAPE != null) {
                pShaderInstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
            }

            if (pShaderInstance.TEXTURE_MATRIX != null) {
                pShaderInstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
            }

            if (pShaderInstance.GAME_TIME != null) {
                pShaderInstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
            }

            if (pShaderInstance.SCREEN_SIZE != null) {
                Window window = Minecraft.getInstance().getWindow();
                pShaderInstance.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
            }

            if (pShaderInstance.LINE_WIDTH != null && (accessor.getMode() == VertexFormat.Mode.LINES || accessor.getMode() == VertexFormat.Mode.LINE_STRIP)) {
                pShaderInstance.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
            }

            RenderSystem.setupShaderLights(pShaderInstance);
            accessor.invokeBindVertexArray();
            buffer.bind();
            buffer.getFormat().setupBufferState();
            pShaderInstance.apply();
            RenderSystem.drawElements(accessor.getMode().asGLMode, accessor.getIndexCount(), accessor.getIndexType().asGLType);
            pShaderInstance.clear();
            buffer.getFormat().clearBufferState();
        }
    }
}
