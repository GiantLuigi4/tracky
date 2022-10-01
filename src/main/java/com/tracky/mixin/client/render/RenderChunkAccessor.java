package com.tracky.mixin.client.render;


import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public interface RenderChunkAccessor {

    @Accessor
    void setPlayerChanged(boolean playerChanged);

}
