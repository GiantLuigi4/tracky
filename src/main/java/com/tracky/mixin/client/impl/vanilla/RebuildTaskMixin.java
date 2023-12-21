package com.tracky.mixin.client.impl.vanilla;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.tracky.access.ExtendedRenderChunk;
import com.tracky.api.TrackyRenderChunk;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class RebuildTaskMixin {

	@Shadow(aliases = {"this$1"})
	@Final
	ChunkRenderDispatcher.RenderChunk this$1;

	@ModifyArg(method = "compile", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;setQuadSorting(Lcom/mojang/blaze3d/vertex/VertexSorting;)V"))
	public VertexSorting editSorting(VertexSorting sorting) {
		TrackyRenderChunk ext = (TrackyRenderChunk) this.this$1;
		return ext.getRenderSource() != null ? ((ExtendedRenderChunk) ext).createVertexSorting() : sorting;
	}
}
