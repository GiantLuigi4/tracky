package com.tracky.mixin.client.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.RenderChunkInfo.class)
public interface RenderChunkInfoMixin {

    @Invoker(value = "<init>")
    static LevelRenderer.RenderChunkInfo invokeInit(ChunkRenderDispatcher.RenderChunk pChunk, @Nullable Direction pSourceDirection, int pStep) {
        throw new RuntimeException("this should not be encountered");
    }

}
