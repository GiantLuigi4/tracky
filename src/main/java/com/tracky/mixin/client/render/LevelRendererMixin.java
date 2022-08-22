package com.tracky.mixin.client.render;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tracky.TrackyAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * Use {@link ViewAreaMixin}'s stuff in order to ensure that tracky chunks get actually rendered
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {


    @Shadow @Nullable private ClientLevel level;

    @Shadow @Nullable private ViewArea viewArea;

    @Shadow @Nullable private ChunkRenderDispatcher chunkRenderDispatcher;

    /**
     * Inject to where render chunks are populated
     */
    @Inject(method = "updateRenderChunks", at = @At(value = "TAIL"))
    private void updateRenderChunks(LinkedHashSet<LevelRenderer.RenderChunkInfo> pChunkInfos, LevelRenderer.RenderInfoMap pInfoMap, Vec3 pViewVector, Queue<LevelRenderer.RenderChunkInfo> pInfoQueue, boolean pShouldCull, CallbackInfo ci) {

        Collection<Supplier<Iterable<ChunkPos>>> trackyRenderedChunks = TrackyAccessor.getRenderedChunks(level).values();
        List<ChunkPos> trackyRenderedChunksList = new ArrayList<>();

        for (Supplier<Iterable<ChunkPos>> trackyRenderedChunksSupplier : trackyRenderedChunks) {
            for (ChunkPos trackyRenderedChunk : trackyRenderedChunksSupplier.get()) {
                trackyRenderedChunksList.add(trackyRenderedChunk);
            }
        }

        // for every tracky chunk the player should be rendering
        for (ChunkPos chunk : trackyRenderedChunksList) {
            for (int y = 0; y < level.getSectionsCount(); y++) {
                ChunkRenderDispatcher.RenderChunk gottenRenderChunk = ((ViewAreaAccessor) viewArea).invokeGetRenderChunkAt(new BlockPos(chunk.x << 4, y << 4, chunk.z << 4));

                if (gottenRenderChunk != null) {
                    LevelRenderer.RenderChunkInfo info = RenderChunkInfoMixin.invokeInit(gottenRenderChunk, (Direction) null, 0);
                    pChunkInfos.add(info);
                    pInfoMap.put(gottenRenderChunk, info);
                }
            }
        }


    }


}
