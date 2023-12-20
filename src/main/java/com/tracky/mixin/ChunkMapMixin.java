package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import com.tracky.api.TrackingSource;
import com.tracky.debug.ITrackChunks;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
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
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

	@Shadow
	@Final
	ServerLevel level;

	@Shadow
	protected abstract void updateChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad);

	@Shadow
	public static boolean isChunkInRange(int p_200879_, int p_200880_, int p_200881_, int p_200882_, int pMaxDistance){throw new RuntimeException("Whar.");}

	@Shadow
	int viewDistance;

	@Unique
	private final List<ChunkPos> tracky$Forced = new ArrayList<>();

	/**
	 * Make the chunk manager send chunk updates to clients tracking tracky-enforced chunks
	 */
	// I think this is the right target?
	@Inject(method = "getPlayers", at = @At("RETURN"), cancellable = true)
	public void getTrackingPlayers(ChunkPos chunkPos, boolean boundaryOnly, CallbackInfoReturnable<List<ServerPlayer>> cir) {
//		if (!TrackyAccessor.isMainTracky()) return;
		final Map<UUID, Supplier<Collection<TrackingSource>>> map = TrackyAccessor.getTrackingSources(level);

		final List<ServerPlayer> players = new ArrayList<>();
		boolean isTrackedByAny = false;

		// for all players in the level send the relevant chunks
		// messy iteration but no way to avoid with our structure
		loopPlayers:
		for (ServerPlayer player : this.level.getPlayers((p) -> true)) {
			for (Supplier<Collection<TrackingSource>> value : map.values()) {
				for (TrackingSource trackingSource : value.get()) {
					if (trackingSource.containsChunk(chunkPos)) {
						// send the packet if the player is tracking it
						players.add(player);
						isTrackedByAny = true;
						continue loopPlayers;
					}
				}
			}
		}

		if (isTrackedByAny) {
			// add players that are tracking it by vanilla
			cir.getReturnValue().forEach((player) -> {
				if (!players.contains(player))
					players.add(player);
			});

			cir.setReturnValue(players);
			cir.cancel();
		}
	}

	@Inject(at = @At("HEAD"), method = "tick(Ljava/util/function/BooleanSupplier;)V")
	public void preTick(BooleanSupplier pHasMoreTime, CallbackInfo ci) {
		Set<Player> playersChecked = new HashSet<>();
		final Map<UUID, Supplier<Collection<TrackingSource>>> map = TrackyAccessor.getTrackingSources(level);
		List<ChunkPos> poses = new ArrayList<>();
		for (ServerPlayer player : level.getPlayers((player) -> true)) {
			if (!playersChecked.add(player)) {
				continue;
			}

			for (Supplier<Collection<TrackingSource>> collectionSupplier : map.values()) {
				for (TrackingSource trackingSource : collectionSupplier.get()) {
					// no point in checking the player if they aren't valid for the chunk source
					if (trackingSource.check(player)) {
						// check every chunk
						for (ChunkPos chunkPos : trackingSource.getChunks()) {
							// chunk must be in loading range in order to be loaded
							if (trackingSource.checkLoadDist(player, chunkPos)) {
								if (!this.tracky$Forced.remove(chunkPos) && !poses.contains(chunkPos)) {
									this.level.setChunkForced(chunkPos.x, chunkPos.z, true);
									poses.add(chunkPos);
								}
							}
						}
					}
				}
			}
		}

		for (ChunkPos chunkPos : this.tracky$Forced) {
			this.level.setChunkForced(chunkPos.x, chunkPos.z, false);

		}

		this.tracky$Forced.addAll(poses);
	}

	/**
	 * Tracks chunks loaded by cameras to make sure they're being sent to the client
	 */
	@Inject(method = "move", at = @At(value = "TAIL"))
	private void trackCameraLoadedChunks(ServerPlayer player, CallbackInfo callback) {
		ITrackChunks chunkTracker = (ITrackChunks) player;
		if (!chunkTracker.shouldUpdate()) return;
		chunkTracker.setDoUpdate(false);

		chunkTracker.tickTracking();

		for (Supplier<Collection<TrackingSource>> value : TrackyAccessor.getTrackingSources(level).values()) {
			for (TrackingSource trackingSource : value.get()) {
				if (trackingSource.check(player)) {
					trackingSource.forEachValid(false, player, (chunkPos) -> {
						this.updateChunkTracking(player, chunkPos, new MutableObject<>(), chunkTracker.trackedChunks().contains(chunkPos), true);
						chunkTracker.trackedChunks().add(chunkPos);
					});
				}
			}
		}

		int pSectionX = SectionPos.blockToSectionCoord(player.getBlockX());
		int pSectionZ = SectionPos.blockToSectionCoord(player.getBlockZ());
		for (ChunkPos trackedChunk : chunkTracker.oldTrackedChunks()) {
			boolean inVanilla = isChunkInRange(
					trackedChunk.x, trackedChunk.z,
					pSectionX, pSectionZ,
					this.viewDistance
			);

			if (!inVanilla) {
				if (!chunkTracker.trackedChunks().contains(trackedChunk)) {
					this.updateChunkTracking(player, trackedChunk, new MutableObject<>(), true, false);
				}
			}
		}

		chunkTracker.oldTrackedChunks().clear();
	}

	@Inject(at = @At("HEAD"), method = "updatePlayerPos")
	public void preUpdatePos(ServerPlayer p_140374_, CallbackInfoReturnable<SectionPos> cir) {
		((ITrackChunks) p_140374_).setDoUpdate(true);
	}

	@Inject(method = "updateChunkTracking", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;untrackChunk(Lnet/minecraft/world/level/ChunkPos;)V", shift = At.Shift.BEFORE), cancellable = true)
	public void markChunkTracked(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
	}

	@Inject(method = "updateChunkTracking", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;untrackChunk(Lnet/minecraft/world/level/ChunkPos;)V", shift = At.Shift.BEFORE), cancellable = true)
	public void captureChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
		// Prevent vanilla from unloading a tracked chunk
		if (((ITrackChunks) pPlayer).trackedChunks().contains(pChunkPos)) {
			ci.cancel();
		}
	}
}
