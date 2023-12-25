package com.tracky.mixin;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.api.IPlayerInfo;
import com.tracky.api.TrackingSource;
import com.tracky.impl.ITrackChunks;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

	@Shadow
	@Final
	ServerLevel level;

	@Shadow
	protected abstract void updateChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad);
	
	@Shadow @Final private static Logger LOGGER;
	
	@Shadow public static boolean isChunkInRange(int p_200879_, int p_200880_, int p_200881_, int p_200882_, int pMaxDistance) {throw new RuntimeException("Whar?");}
	
	@Shadow private int viewDistance;
	@Unique
	private final Set<ChunkPos> tracky$Forced = new HashSet<>();

	/**
	 * Make the chunk manager send chunk updates to clients tracking tracky-enforced chunks
	 */
	@Inject(method = "getPlayers", at = @At("RETURN"), cancellable = true)
	public void getTrackingPlayers(ChunkPos chunkPos, boolean boundaryOnly, CallbackInfoReturnable<List<ServerPlayer>> cir) {
		List<ServerPlayer> vanillaPlayers = cir.getReturnValue();
		List<ServerPlayer> players = this.level.getPlayers(player -> !vanillaPlayers.contains(player));
		List<ServerPlayer> ret = null;

		// Players already included don't need to be checked
		for (ServerPlayer player : players) {
			ITrackChunks tracker = (ITrackChunks) player;
			if (tracker.trackedChunks().contains(chunkPos)) {
				if (ret == null) {
					ret = new ArrayList<>(vanillaPlayers);
				}
				ret.add(player);
			}
		}

		if (ret != null) {
			cir.setReturnValue(ret);
		}
	}
	
	@Unique
	Set<UUID> tracky$recognizedUUIDs = new HashSet<>();
	
	@Unique boolean tracky$ticking = false;
	
	protected void update(IPlayerInfo info, Set<ChunkPos> positions, Set<IPlayerInfo> trackers, Map<UUID, Supplier<Collection<TrackingSource>>> map) {
		ServerPlayer player = info.getPlayer();
		
		for (Supplier<Collection<TrackingSource>> collectionSupplier : map.values()) {
			for (TrackingSource trackingSource : collectionSupplier.get()) {
				
				for (IPlayerInfo iPlayerInfo : trackingSource.infos(info)) {
					trackers.add(iPlayerInfo);
					
					iPlayerInfo.update();
					
					// No point in checking the player if they aren't valid for the chunk source
					if (!trackingSource.check(iPlayerInfo)) {
						continue;
					}
					
					boolean updateClients = trackingSource.needsUpdate();
					
					// force chunks in load distance
					trackingSource.forEachValid(true, iPlayerInfo, (chunkPos) -> {
						boolean wasTracked = this.tracky$Forced.contains(chunkPos);
						if (positions.add(chunkPos)) {
							// The chunk was not previously tracked, so we need to load the chunk
							if (!wasTracked) {
								this.level.setChunkForced(chunkPos.x, chunkPos.z, true);
							}
						}
					});
					
					// sync chunks in syncing distance
					// usually, the load range should be larger than the syncing range
					trackingSource.forEachValid(false, iPlayerInfo, (chunkPos) -> {
						// if I'm not mistaken, putting this in the if statement for updateClients will do one of two things:
						// - make stuff start un-syncing incorrectly when two render sources share chunks
						// - make stuff unsync the tick after it syncs
						// don't think there's any way around both of these without refactoring ITrackChunks
						if (iPlayerInfo.trackedChunks().add(chunkPos)) {
							if (!iPlayerInfo.oldTrackedChunks().remove(chunkPos)) {
								if (updateClients) {
									this.updateChunkTracking(player, chunkPos, new MutableObject<>(), false, true);
								}
							}
						}
					});
				}
				
			}
		}
	}
	
	@Inject(at = @At("HEAD"), method = "tick(Ljava/util/function/BooleanSupplier;)V")
	public void preTick(BooleanSupplier pHasMoreTime, CallbackInfo ci) {
		ProfilerFiller profiler = this.level.getProfiler();
		profiler.push("tracky_update_tracking");

		Map<UUID, Supplier<Collection<TrackingSource>>> map = TrackyAccessor.getTrackingSources(this.level);
		List<ServerPlayer> players = this.level.getPlayers(player -> true);
		
		if (players.isEmpty() && tracky$Forced.isEmpty())
			return;
		
		tracky$ticking = true;
		
		Set<ChunkPos> positions = new HashSet<>();
		Set<IPlayerInfo> trackers = new HashSet<>();
		for (ServerPlayer player : players)
			update(IPlayerInfo.of(player), positions, trackers, map);
		
		for (Supplier<Collection<TrackingSource>> value : map.values()) {
			for (TrackingSource trackingSource : value.get()) {
				for (IPlayerInfo info : trackingSource.infos(null)) {
					if (info == null) continue;
					
					trackingSource.setupRedir(info);
					update(info, positions, trackers, map);
					trackingSource.endRedir(info);
				}
			}
		}
		
		for (IPlayerInfo tracker : trackers) {
			for (ChunkPos chunkPos : tracker.oldTrackedChunks()) {
				this.updateChunkTracking(tracker.getPlayer(), chunkPos, new MutableObject<>(), true, false);
				
				tracker.trackedChunks().remove(chunkPos);
			}
		}
		
		tracky$ticking = false;
		
		tracky$recognizedUUIDs.clear();
		for (ServerPlayer player : players) {
			tracky$recognizedUUIDs.add(player.getUUID());
		}
		
		for (Supplier<Collection<TrackingSource>> collectionSupplier : map.values()) {
			for (TrackingSource trackingSource : collectionSupplier.get()) {
				trackingSource.markUpdate(false);
			}
		}

		for (ChunkPos chunkPos : this.tracky$Forced) {
			if (!positions.contains(chunkPos)) {
				this.level.setChunkForced(chunkPos.x, chunkPos.z, false);
			}
		}

		// All remaining chunks have been unloaded, so we can update the current tracking set
		this.tracky$Forced.clear();
		this.tracky$Forced.addAll(positions);
		profiler.pop();
	}

	@Inject(method = "updateChunkTracking", at = @At(value = "HEAD"), cancellable = true)
	public void captureChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
		// If tracky's currently ticking, we should be able to not worry about this
		// if we do, that's a bug!
		if (!tracky$ticking) {
			// If vanilla's trying to load or unload the chunk, then it should be cancelled if tracky is tracking it
			if (pWasLoaded != pLoad) {
				if (((ITrackChunks) pPlayer).trackedChunks().contains(pChunkPos)) {
					ci.cancel();
				}
			}
		} else {
			
			if (Tracky.ENABLE_TEST) {
				// this contains check fails 100% of the time for (!pWasLoad && pLoad), so that case shouldn't be included
				if (pWasLoaded && !pLoad) {
					if (((ITrackChunks) pPlayer).trackedChunks().contains(pChunkPos)) {
						LOGGER.warn("[Tracky:ChunkMapMixin] A tracky chunk was dropped incorrectly.");
						// don't cancel it, this way something happens in game, which can alert us to the problem
					}
				}
			}
			
			// If a chunk is loaded/unloaded due to tracky but should already be loaded by vanilla, then tracky shouldn't update its status
			// doing so causes problems
			int pSectionX = SectionPos.blockToSectionCoord(pPlayer.getBlockX());
			int pSectionZ = SectionPos.blockToSectionCoord(pPlayer.getBlockZ());
			
			if (isChunkInRange(
					pChunkPos.x, pChunkPos.z,
					pSectionX, pSectionZ,
					this.viewDistance
			)) {
				ci.cancel();
			}
			
		}
	}
}
