package com.tracky.mixin.client.render;

import com.tracky.Tracky;
import com.tracky.access.ClientMapHolder;
import com.tracky.access.ExtendedViewArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Make sure tracky chunks get rendered
 */
@Mixin(ViewArea.class)
public abstract class ViewAreaMixin implements ExtendedViewArea {

    @Shadow
    protected int chunkGridSizeY;
    @Shadow
    @Final
    protected Level level;

    @Shadow
    protected abstract int getChunkIndex(int pX, int pY, int pZ);

    @Shadow protected int chunkGridSizeZ;
    @Shadow protected int chunkGridSizeX;
    @Shadow public ChunkRenderDispatcher.RenderChunk[] chunks;
    @Unique
    private Map<ChunkPos, ChunkRenderDispatcher.RenderChunk[]> tracky$renderChunkCache = new ConcurrentHashMap<>();

    @Unique
    private ChunkRenderDispatcher tracky$chunkRenderDispatcher;
    
    @Override
    public Map<ChunkPos, ChunkRenderDispatcher.RenderChunk[]> getTracky$renderChunkCache() {
        return tracky$renderChunkCache;
    }
    
    /**
     * For some reason, mojang never stored the chunk render dispatcher passed to the constructor of a view area
     * Inject to fix this!
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    public void postInit(ChunkRenderDispatcher pChunkRenderDispatcher, Level pLevel, int pViewDistance, LevelRenderer pLevelRenderer, CallbackInfo ci) {
        this.tracky$chunkRenderDispatcher = pChunkRenderDispatcher;
        ViewArea area = ((LevelRendererAccessor) pLevelRenderer).getViewArea();
        if (area != null) {
            this.tracky$renderChunkCache = ((ExtendedViewArea) area).getTracky$renderChunkCache();
        }
    }


    /**
     * Populate & set dirty chunks in tracky$chunkRenderDispatcher
     */
    @Inject(method = "setDirty", at = @At("HEAD"), cancellable = true)
    public void setDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread, CallbackInfo ci) {
        int i = pSectionX << 4;
        int j = Math.floorMod(pSectionY - this.level.getMinSection(), this.chunkGridSizeY);
//        int j = pSectionY << 4;
        int k = pSectionZ << 4;

        Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder)Minecraft.getInstance().level).trackyGetRenderChunksC();
        SectionPos spos = SectionPos.of(pSectionX, pSectionY, pSectionZ);
        if (Tracky.sourceContains(level, spos) || trackyRenderedChunksList.contains(spos)) {
            ChunkPos chunk = new ChunkPos(pSectionX, pSectionZ);

            Function<ChunkPos, ChunkRenderDispatcher.RenderChunk[]> newBlankRenderChunks = idk -> new ChunkRenderDispatcher.RenderChunk[this.chunkGridSizeY];
            ChunkRenderDispatcher.RenderChunk[] renderChunks = tracky$renderChunkCache.computeIfAbsent(chunk, newBlankRenderChunks);
            ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = renderChunks[j];
            if (chunkrenderdispatcher$renderchunk == null) {
                int i1 = this.getChunkIndex(i, j, k);
                chunkrenderdispatcher$renderchunk = renderChunks[j] = tracky$chunkRenderDispatcher.new RenderChunk(i1, pSectionX << 4, pSectionY << 4, pSectionZ << 4);
            }

            chunkrenderdispatcher$renderchunk.setDirty(pReRenderOnMainThread);
//            ((RenderChunkAccessor) chunkrenderdispatcher$renderchunk).setPlayerChanged(true);
            ci.cancel();
        }
    }

    /**
     * Make tracky render chunks point to what they should be
     */
    @Inject(method = "getRenderChunkAt", at = @At("HEAD"), cancellable = true)
    public void getRenderChunkAt(BlockPos pPos, CallbackInfoReturnable<ChunkRenderDispatcher.RenderChunk> cir) {

        int x = Math.floorDiv(pPos.getX(), 16),
                y = Math.floorDiv(pPos.getY() - this.level.getMinBuildHeight(), 16),
                z = Math.floorDiv(pPos.getZ(), 16);

        int preY = Math.floorDiv(pPos.getY(), 16);

        if (y >= 0 && y < this.chunkGridSizeY) {
            SectionPos spos = SectionPos.of(x, preY, z);
            Collection<SectionPos> trackyRenderedChunksList = ((ClientMapHolder)Minecraft.getInstance().level).trackyGetRenderChunksC();
            
            if (Tracky.sourceContains(level, spos) || trackyRenderedChunksList.contains(spos)) {
                ChunkPos cpos = new ChunkPos(x, z);
                ChunkRenderDispatcher.RenderChunk[] renderChunks = tracky$renderChunkCache.get(cpos);

                if (renderChunks != null) {
                    ChunkRenderDispatcher.RenderChunk renderChunk = renderChunks[y /*- Math.abs(level.getMinSection())*/];
                    cir.setReturnValue(renderChunk);
                }
            }
        }
    }
	
    // TODO: this is probably dumb, but it's working for now
	@Inject(at = @At("HEAD"), method = "releaseAllBuffers")
	public void preReleaseBuffers(CallbackInfo ci) {
		for (ChunkRenderDispatcher.RenderChunk[] value : tracky$renderChunkCache.values()) {
            for (int i = 0; i < value.length; i++) {
                ChunkRenderDispatcher.RenderChunk renderChunk = value[i];
                if (renderChunk != null) {// sometimes a render chunk can be null
//                    renderChunk.releaseBuffers(); // free gl resources
                    value[i].setDirty(true);
                    ((RenderChunkAccessor) value[i]).setPlayerChanged(true);
                }
            }
		}
	}
}
