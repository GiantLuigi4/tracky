package com.tracky.mixin.client.render;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.tracky.access.RenderChunkExtensions;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$ResortTransparencyTask")
public class ResortTransparencyTaskMixin {

	@Shadow
	@Final
	ChunkRenderDispatcher.RenderChunk this$1;

	@ModifyArg(method = "doTask", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;setQuadSorting(Lcom/mojang/blaze3d/vertex/VertexSorting;)V"))
	public VertexSorting editSorting(VertexSorting sorting) {
		RenderChunkExtensions ext = (RenderChunkExtensions) this.this$1;
		return ext.tracky$getRenderSource() != null ? ext.tracky$getSorting() : sorting;
	}
}
