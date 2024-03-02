package com.tracky.mixin.client.impl.sodium;

import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import com.tracky.api.TrackyViewArea;
import com.tracky.impl.TrackyRenderSectionManager;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerMixin implements TrackyViewArea {

    @Shadow
    @Final
    private Long2ReferenceMap<RenderSection> sectionByPosition;

    @Shadow
    public abstract void scheduleRebuild(int x, int y, int z, boolean important);

    @Shadow
    public abstract void onSectionAdded(int x, int y, int z);

    @Shadow
    protected abstract RenderSection getRenderSection(int x, int y, int z);

    @Shadow
    @Final
    private ClientLevel world;

    @Inject(method = "processChunkBuildResults", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSectionManager;updateSectionInfo(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;Lme/jellysquid/mods/sodium/client/render/chunk/data/BuiltSectionInfo;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void updateCompiledChunks(ArrayList<ChunkBuildOutput> results, CallbackInfo ci, List<ChunkBuildOutput> filtered, Iterator<ChunkBuildOutput> iterator, ChunkBuildOutput result) {
        TrackyRenderChunk renderChunk = (TrackyRenderChunk) result.render;
        RenderSource renderSource = renderChunk.getRenderSource();
        if (renderSource != null) {
            renderSource.updateCompiledChunk(renderChunk);
        }
    }

    @Inject(method = "shouldUseOcclusionCulling", at = @At("RETURN"), cancellable = true)
    public void shouldUseOcclusionCulling(Camera camera, boolean spectator, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if ((Object) this instanceof TrackyRenderSectionManager extendedRenderSectionManager) {
            if (extendedRenderSectionManager.disableOcclusionCulling(this.world, camera, spectator)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Override
    public @Nullable TrackyRenderChunk getRenderChunk(int sectionX, int sectionY, int sectionZ) {
        return (TrackyRenderChunk) this.getRenderSection(sectionX, sectionY, sectionZ);
    }

    @Override
    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean priority) {
        RenderSection section = this.sectionByPosition.get(SectionPos.asLong(sectionX, sectionY, sectionZ));
        if (section != null) {
            this.scheduleRebuild(sectionX, sectionY, sectionZ, priority);
        } else {
            this.onSectionAdded(sectionX, sectionY, sectionZ);
        }
    }
}
