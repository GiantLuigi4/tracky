package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import com.tracky.api.TrackingSource;
import com.tracky.impl.ITrackChunks;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    protected abstract void updateChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad);

    @Shadow
    public static boolean isChunkInRange(int p_200879_, int p_200880_, int p_200881_, int p_200882_, int pMaxDistance) {
        throw new AssertionError();
    }

    @Shadow
    int viewDistance;

    @Unique
    private final Set<ChunkPos> tracky$Forced = new HashSet<>();

    /**
     * Make the chunk manager send chunk updates to clients tracking tracky-enforced chunks
     */
    @Inject(method = "getPlayers", at = @At("RETURN"), cancellable = true)
    public void getTrackingPlayers(ChunkPos chunkPos, boolean boundaryOnly, CallbackInfoReturnable<List<ServerPlayer>> cir) {
        List<ServerPlayer> players = new ArrayList<>();
        List<ServerPlayer> vanillaPlayers = cir.getReturnValue();

        // for all players in the level send the relevant chunks
        loopPlayers:
        for (ServerPlayer player : this.level.getPlayers((p) -> !vanillaPlayers.contains(p))) {
            for (TrackingSource trackingSource : ((ITrackChunks) player).trackedSources()) {
                if (trackingSource.checkRenderDist(player, chunkPos)) {
                    if (trackingSource.containsChunk(chunkPos)) {
                        // send the packet if the player is tracking it
                        players.add(player);
                        continue loopPlayers;
                    }
                }
            }
        }

        if (!players.isEmpty()) {
            // add players that are tracking it by vanilla
            players.addAll(vanillaPlayers);

            cir.setReturnValue(players);
        }
    }

    @Inject(at = @At("HEAD"), method = "tick(Ljava/util/function/BooleanSupplier;)V")
    public void preTick(BooleanSupplier pHasMoreTime, CallbackInfo ci) {
        List<TrackingSource> sources = TrackyAccessor.getTrackingSources(this.level);

        // Remove sources that are no longer tracked
        for (ServerPlayer player : this.level.getPlayers((player) -> true)) {
            ITrackChunks tracker = (ITrackChunks) player;
            Set<TrackingSource> trackedSources = tracker.trackedSources();

            // If any sources no longer exist, then force a retrack
            if(trackedSources.removeIf(source -> !sources.contains(source))){
                tracker.setDoUpdate(true);
            }
        }

        // This loop loads the sources/chunks around players server-side
        // trackCameraLoadedChunks actually updates the client tracking
        for (TrackingSource trackingSource : sources) {
            for (ServerPlayer player : this.level.getPlayers((player) -> true)) {
                ITrackChunks tracker = (ITrackChunks) player;
                Set<TrackingSource> trackedSources = tracker.trackedSources();

                // If the source hasn't updated, and the player is already tracking the source, then there's no need to update the player sources
                if (!trackingSource.isDirty() && trackingSource.check(player) == trackedSources.contains(trackingSource)) {
                    continue;
                }

                // no point in checking the player if they aren't valid for the chunk source
                if (!trackingSource.check(player)) {
                    trackedSources.remove(trackingSource);
                    continue;
                }

                trackedSources.add(trackingSource);
                // The source updated, so each player needs to recheck what chunks they are tracking
                tracker.setDoUpdate(true);

                // check every chunk
//                trackingSource.forEachValid(true, player, (chunkPos) -> {
//                    // chunk must be in loading range in order to be loaded
//                    if (trackingSource.checkLoadDist(player, chunkPos)) {
//                        if (!this.tracky$Forced.remove(chunkPos) && poses.add(chunkPos)) {
//                            // This won't try to save the forced chunk to disc
//                            this.level.getChunkSource().updateChunkForced(chunkPos, true);
////										this.level.setChunkForced(chunkPos.x, chunkPos.z, true);
//                        }
//                    }
//                });
            }
            trackingSource.setDirty(false);
        }

//        for (ChunkPos chunkPos : this.tracky$Forced) {
//            this.level.getChunkSource().updateChunkForced(chunkPos, false);
////			this.level.setChunkForced(chunkPos.x, chunkPos.z, false);
//        }
//
//        this.tracky$Forced.addAll(poses);
    }

    /**
     * Tracks chunks loaded by cameras to make sure they're being sent to the client
     */
    @Inject(method = "move", at = @At(value = "TAIL"))
    private void trackCameraLoadedChunks(ServerPlayer player, CallbackInfo callback) {
        ITrackChunks tracker = (ITrackChunks) player;
        if (!tracker.shouldUpdate()) {
            return;
        }

        tracker.setDoUpdate(false);
        tracker.tickTracking();

        for (TrackingSource trackingSource : tracker.trackedSources()) {
            trackingSource.forEachValid(false, player, (chunkPos) -> {
                tracker.trackedChunks().add(chunkPos);

                // If the chunk was previously tracked, then do nothing
                if (tracker.oldTrackedChunks().contains(chunkPos)) {
                    return;
                }

                // The player needs the chunk, so make sure it's loaded
                if (this.tracky$Forced.add(chunkPos)) {
                    this.level.getChunkSource().updateChunkForced(chunkPos, true);
                }
                this.updateChunkTracking(player, chunkPos, new MutableObject<>(), false, true);
            });
        }

        int pSectionX = SectionPos.blockToSectionCoord(player.getBlockX());
        int pSectionZ = SectionPos.blockToSectionCoord(player.getBlockZ());
        for (ChunkPos chunkPos : tracker.oldTrackedChunks()) {
            // The chunk is still tracked, so do nothing
            if (tracker.trackedChunks().contains(chunkPos)) {
                continue;
            }

            boolean inVanilla = isChunkInRange(
                    chunkPos.x, chunkPos.z,
                    pSectionX, pSectionZ,
                    this.viewDistance
            );

            if (!inVanilla) {
                if (this.tracky$Forced.remove(chunkPos)) {
                    this.level.getChunkSource().updateChunkForced(chunkPos, false);
                }
                this.updateChunkTracking(player, chunkPos, new MutableObject<>(), true, false);
            }
        }

        tracker.oldTrackedChunks().clear();
    }

    @Inject(at = @At("HEAD"), method = "updatePlayerPos")
    public void preUpdatePos(ServerPlayer player, CallbackInfoReturnable<SectionPos> cir) {
        // This forces the server to recheck what chunks are in the player range when they switch chunks
        ((ITrackChunks) player).setDoUpdate(true);
    }

    @Inject(method = "updateChunkTracking", at = @At(value = "HEAD"), cancellable = true)
    public void captureChunkTracking(ServerPlayer player, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
        // Prevent vanilla from loading/unloading a tracked chunk
        if (pWasLoaded == pLoad) {
            return;
        }

        ITrackChunks tracker = (ITrackChunks) player;

        // for each render source, the following conditions must be true for this to be canceled:
        for (TrackingSource trackingSource : tracker.trackedSources()) {
            // the source must actually contain the chunk
            if (trackingSource.containsChunk(pChunkPos)) {
                // it must be within the sync distance
                if (trackingSource.checkRenderDist(player, pChunkPos)) {
                    // the client must already be tracking the chunk
                    if (tracker.trackedChunks().contains(pChunkPos)) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
}
