package com.tracky.mixin.client.render;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VertexBuffer.class)
public interface VertexBufferAccessor {

    @Accessor
    int getIndexCount();

    @Accessor
    VertexFormat.Mode getMode();

    @Invoker
    void invokeBindVertexArray();

    @Accessor
    VertexFormat.IndexType getIndexType();
}
