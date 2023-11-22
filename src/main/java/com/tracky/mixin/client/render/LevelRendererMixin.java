package com.tracky.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tracky.access.ClientMapHolder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Use {@link ViewAreaMixin}'s stuff in order to ensure that tracky chunks get actually rendered
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {


    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Nullable
    private ViewArea viewArea;

    @Shadow
    @Nullable
    private ChunkRenderDispatcher chunkRenderDispatcher;

    @Shadow @Final private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;

    @Shadow @Final private AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage;

    @Shadow public abstract void renderLevel(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix);

    /**
     * Inject to where render chunks are populated
     */
    @Inject(method = "updateRenderChunks", at = @At(value = "TAIL"))
    private void updateRenderChunks(LinkedHashSet<LevelRenderer.RenderChunkInfo> pChunkInfos, LevelRenderer.RenderInfoMap pInfoMap, Vec3 pViewVector, Queue<LevelRenderer.RenderChunkInfo> pInfoQueue, boolean pShouldCull, CallbackInfo ci) {
        Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder) Minecraft.getInstance().level).trackyGetRenderChunksC();

        // for every tracky chunk the player should be rendering
        for (SectionPos chunk : trackyRenderedChunksList) {
            ChunkRenderDispatcher.RenderChunk gottenRenderChunk = ((ViewAreaAccessor) viewArea).invokeGetRenderChunkAt(new BlockPos(chunk.x() << 4, chunk.y() << 4, chunk.z() << 4));

            if (gottenRenderChunk != null) {
                LevelRenderer.RenderChunkInfo info = pInfoMap.get(gottenRenderChunk);
                if (info == null) {
                    info = RenderChunkInfoMixin.invokeInit(gottenRenderChunk, (Direction) null, 0);
                    pInfoMap.put(gottenRenderChunk, info);
                }
                pChunkInfos.add(info);
            }
        }
    }

    @Inject(method = "applyFrustum", at = @At("TAIL"))
    private void applyFrustum(Frustum pFrustrum, CallbackInfo ci) {
        Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder)Minecraft.getInstance().level).trackyGetRenderChunksC();

        HashSet<LevelRenderer.RenderChunkInfo> settedFrustum = new HashSet<>(this.renderChunksInFrustum);

        // for every tracky chunk the player should be rendering
        for (SectionPos chunk : trackyRenderedChunksList) {
            ChunkRenderDispatcher.RenderChunk renderChunk = ((ViewAreaAccessor) viewArea).invokeGetRenderChunkAt(new BlockPos(chunk.x() << 4, chunk.y() << 4, chunk.z() << 4));

            if (renderChunk != null) {
                LevelRenderer.RenderChunkInfo info = renderChunkStorage.get().renderInfoMap.get(renderChunk);

                if (info != null && !settedFrustum.contains(info))
                    this.renderChunksInFrustum.add(info);
            }
        }
    }
}