package com.tracky.mixin.client.render;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Matrix4f;
import com.tracky.Temp;
import com.tracky.TrackyAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

        Collection<Supplier<Collection<ChunkPos>>> trackyRenderedChunks = TrackyAccessor.getRenderedChunks(level).values();
        List<ChunkPos> trackyRenderedChunksList = new ArrayList<>();

        for (Supplier<Collection<ChunkPos>> trackyRenderedChunksSupplier : trackyRenderedChunks) {
            trackyRenderedChunksList.addAll(trackyRenderedChunksSupplier.get());
        }

        // for every tracky chunk the player should be rendering
        for (ChunkPos chunk : trackyRenderedChunksList) {
            for (int y = level.getMinSection(); y < level.getMaxSection(); y++) {
                ChunkRenderDispatcher.RenderChunk gottenRenderChunk = ((ViewAreaAccessor) viewArea).invokeGetRenderChunkAt(new BlockPos(chunk.x << 4, y << 4, chunk.z << 4));

                if (gottenRenderChunk != null) {
                    LevelRenderer.RenderChunkInfo info = RenderChunkInfoMixin.invokeInit(gottenRenderChunk, (Direction) null, 0);
                    pChunkInfos.add(info);
                    pInfoMap.put(gottenRenderChunk, info);
                }
            }
        }


    }

    @Inject(method = "applyFrustum", at = @At("TAIL"))
    private void applyFrustum(Frustum pFrustrum, CallbackInfo ci) {
        Collection<Supplier<Collection<ChunkPos>>> trackyRenderedChunks = TrackyAccessor.getRenderedChunks(level).values();
        List<ChunkPos> trackyRenderedChunksList = new ArrayList<>();

        for (Supplier<Collection<ChunkPos>> trackyRenderedChunksSupplier : trackyRenderedChunks) {
            trackyRenderedChunksList.addAll(trackyRenderedChunksSupplier.get());
        }

        // for every tracky chunk the player should be rendering
        for (ChunkPos chunk : trackyRenderedChunksList) {
            for (int y = level.getMinSection(); y < level.getMaxSection(); y++) {
                ChunkRenderDispatcher.RenderChunk renderChunk = ((ViewAreaAccessor) viewArea).invokeGetRenderChunkAt(new BlockPos(chunk.x << 4, y << 4, chunk.z << 4));

                if (renderChunk != null) {
                    LevelRenderer.RenderChunkInfo info = renderChunkStorage.get().renderInfoMap.get(renderChunk);

                    if (!renderChunksInFrustum.contains(info) && info != null)
                        renderChunksInFrustum.add(info);
                }
            }
        }
    }


    @Unique ChunkRenderDispatcher.RenderChunk renderChunk = null;

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;getCompiledChunk()Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$CompiledChunk;"))
    public ChunkRenderDispatcher.CompiledChunk preRenderLayer(ChunkRenderDispatcher.RenderChunk instance) {
        return (renderChunk = instance).getCompiledChunk();
    }

    PoseStack savedStack = null;
    double camX, camY, camZ;

    @Inject(at = @At("HEAD"), method = "renderChunkLayer")
    public void preRender(RenderType j, PoseStack d0, double d1, double d2, double i, Matrix4f k, CallbackInfo ci) {
        camX = d1;
        camY = d2;
        camZ = i;
        savedStack = d0;
    }

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawChunkLayer()V"))
    public void preRenderLayer(VertexBuffer instance) {
        ShaderInstance shaderinstance = RenderSystem.getShader();
        Uniform uniform = shaderinstance.CHUNK_OFFSET;

        BlockPos origin = renderChunk.getOrigin();
        ChunkRenderDispatcher.CompiledChunk chunk = renderChunk.compiled.get();

        int x = Mth.intFloorDiv(origin.getX(), 16);
        int y = Mth.intFloorDiv(origin.getY(), 16);
        int z = Mth.intFloorDiv(origin.getZ(), 16);


        if (TrackyAccessor.getRenderedChunks(level).values().stream().map(Supplier::get).flatMap(Collection::stream).toList().contains(new ChunkPos(x, z))) {
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

            modelViewStack.pushPose();
            Temp.process((LevelRenderer) (Object) this, instance, renderChunk, uniform, savedStack, projectionMatrix, chunk, origin, x, y, z, camX, camY, camZ, instance);

            modelViewStack.popPose();
        } else {
            instance.drawChunkLayer();
        }
    }
}