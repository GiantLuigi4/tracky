package com.tracky.mixin.client.render;

import com.tracky.Tracky;
import com.tracky.access.ClientMapHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class RenderChunkMixin {

    @Shadow public abstract BlockPos getOrigin();

    @Inject(method = "getDistToPlayerSqr", at = @At("HEAD"), cancellable = true)
    protected void getDistToPlayerSqr(CallbackInfoReturnable<Double> cir) {
        BlockPos origin = this.getOrigin();

        int x = Mth.floorDiv(origin.getX(), 16);
        int y = Mth.floorDiv(origin.getY(), 16);
        int z = Mth.floorDiv(origin.getZ(), 16);

        Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder)Minecraft.getInstance().level).trackyGetRenderChunksC();
    
        SectionPos pos = SectionPos.of(x, y, z);
        
        if (Tracky.sourceContains(Minecraft.getInstance().level, pos) || trackyRenderedChunksList.contains(pos)) {
            cir.setReturnValue(10.0D);
        }
    }
}