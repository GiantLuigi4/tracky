package com.tracky.mixin.client.render;

import com.tracky.TrackyAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Supplier;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class RenderChunkMixin {

    @Shadow public abstract BlockPos getOrigin();

    @Inject(method = "getDistToPlayerSqr", at = @At("HEAD"), cancellable = true)
    protected void getDistToPlayerSqr(CallbackInfoReturnable<Double> cir) {
        BlockPos origin = this.getOrigin();

        int x = Mth.intFloorDiv(origin.getX(), 16);
        int y = Mth.intFloorDiv(origin.getY(), 16);
        int z = Mth.intFloorDiv(origin.getZ(), 16);

        Collection<Supplier<Collection<SectionPos>>> trackyRenderedChunks = TrackyAccessor.getRenderedChunks(Minecraft.getInstance().level).values();
        List<SectionPos> trackyRenderedChunksList = new ArrayList<>();

        for (Supplier<Collection<SectionPos>> trackyRenderedChunksSupplier : trackyRenderedChunks) {
//            for (ChunkPos trackyRenderedChunk : trackyRenderedChunksSupplier.get()) {
//                trackyRenderedChunksList.add(trackyRenderedChunk);
//            }
            trackyRenderedChunksList.addAll(trackyRenderedChunksSupplier.get());
        }

        if (trackyRenderedChunksList.contains(SectionPos.of(x, y, z))) {
            cir.setReturnValue(0.0D);
        }
    }
}