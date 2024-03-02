package com.tracky.mixin.client;

import com.google.common.collect.Streams;
import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin implements IChunkProviderAttachments {

    @Shadow
    @Final
    ClientLevel level;

    @Shadow
    volatile ClientChunkCache.Storage storage;

    @Shadow
    private static boolean isValidChunk(@Nullable LevelChunk pChunk, int pX, int pZ) {
        return false;
    }

    @Shadow
    @Final
    private LevelChunk emptyChunk;
    @Shadow
    @Final
    private static Logger LOGGER;
    @Unique
    private final Map<ChunkPos, LevelChunk> tracky$chunks = new HashMap<>();
    @Unique
    private final Map<ChunkPos, Long> tracky$lastUpdates = new ConcurrentHashMap<>();

    @Unique
    private void tracky$onRemove(LevelChunk chunk) {
        this.tracky$lastUpdates.remove(chunk.getPos());
    }

    @Unique
    private int tracky$calculateIndex(ChunkPos pos) {
        int viewRange = ((ChunkStorageAccessor) (Object) this.storage).getViewRange();
        return Math.floorMod(pos.z, viewRange) * viewRange + Math.floorMod(pos.x, viewRange);
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("TAIL"), cancellable = true)
    public void getChunk(int pChunkX, int pChunkZ, ChunkStatus pRequiredStatus, boolean pLoad, CallbackInfoReturnable<LevelChunk> cir) {
        LevelChunk ret = cir.getReturnValue();
        if (ret == null || ret == this.emptyChunk) {
            LevelChunk chunk = this.tracky$chunks.get(new ChunkPos(pChunkX, pChunkZ));
            if (chunk != null) {
                cir.setReturnValue(chunk);
            }
        }
    }

    // The cache has moved, so some chunks that used to be in vanilla's map need to be moved to tracky
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "updateViewCenter", at = @At("HEAD"))
    public void updateViewCenter(int pX, int pZ, CallbackInfo ci) {
        ChunkStorageAccessor accessor = (ChunkStorageAccessor) (Object) this.storage;
        for (int i = 0; i < accessor.getViewRange() * accessor.getViewRange(); i++) {
            LevelChunk chunk = accessor.invokeGetChunk(i);
            if (chunk == null) {
                continue;
            }

            ChunkPos pos = chunk.getPos();
            if (Math.abs(pos.x - pX) > accessor.getChunkRadius() || Math.abs(pos.z - pZ) > accessor.getChunkRadius()) {
                this.tracky$chunks.put(pos, chunk);
            }
        }
    }

    @Inject(method = "drop", at = @At("HEAD"))
    public void drop(int pX, int pZ, CallbackInfo ci) {
        ChunkPos pos = new ChunkPos(pX, pZ);
        LevelChunk dropped = this.tracky$chunks.remove(pos);
        if (dropped != null) {
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(dropped));
            this.level.unload(dropped);
            this.tracky$onRemove(dropped);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "replaceWithPacketData", at = @At(value = "HEAD"), cancellable = true)
    public void replaceWithPacketData(int pX, int pZ, FriendlyByteBuf pBuffer, CompoundTag pTag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pConsumer, CallbackInfoReturnable<LevelChunk> cir) {
        ChunkPos chunkpos = new ChunkPos(pX, pZ);
        LevelChunk chunk = this.tracky$chunks.get(chunkpos);

        if (!FMLEnvironment.production) {
            if (this.tracky$chunks.containsKey(chunkpos)) {
                LOGGER.warn("[Tracky:ClientChunkCacheMixin] A chunk has been reloaded -- this is most likely due to a bug in the server logic!");
            }
        }

        ChunkStorageAccessor accessor = (ChunkStorageAccessor) (Object) this.storage;
        if (accessor.invokeInRange(pX, pZ)) {
            // If tracky is keeping track of it, then we need to put the chunk back into the vanilla map
            if (chunk != null) {
                this.tracky$onRemove(chunk);
                accessor.invokeReplace(this.tracky$calculateIndex(chunkpos), chunk);
            }
            return;
        }

        if (!isValidChunk(chunk, pX, pZ)) {
            chunk = new LevelChunk(this.level, chunkpos);
            chunk.replaceWithPacketData(pBuffer, pTag, pConsumer);
            this.tracky$chunks.put(chunkpos, chunk);
        } else {
            chunk.replaceWithPacketData(pBuffer, pTag, pConsumer);
        }

        this.level.onChunkLoaded(chunkpos);
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));

        cir.setReturnValue(chunk);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public Iterator<LevelChunk> loadedChunks() {
        ChunkStorageAccessor accessor = (ChunkStorageAccessor) (Object) this.storage;
        int viewRange = accessor.getViewRange();
        return Streams.concat(this.tracky$chunks.values().stream(), IntStream.range(0, viewRange * viewRange).mapToObj(accessor::invokeGetChunk)).filter(Objects::nonNull).distinct().iterator();
    }

    @Override
    public void setUpdated(ChunkPos pos) {
        this.tracky$lastUpdates.put(pos, System.currentTimeMillis());
    }

    @Override
    public long getLastUpdate(ChunkPos pos) {
        return this.tracky$lastUpdates.getOrDefault(pos, 0L);
    }

    @Override
    public boolean isTrackyForced(ChunkPos pos) {
        return this.tracky$chunks.containsKey(pos);
    }
}
