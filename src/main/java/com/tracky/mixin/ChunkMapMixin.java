package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import com.tracky.api.TrackingSource;
import com.tracky.impl.ITrackChunks;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
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

	@Inject(at = @At("HEAD"), method = "tick(Ljava/util/function/BooleanSupplier;)V")
	public void preTick(BooleanSupplier pHasMoreTime, CallbackInfo ci) {
		ProfilerFiller profiler = this.level.getProfiler();
		profiler.push("tracky_update_tracking");

		Map<UUID, Supplier<Collection<TrackingSource>>> map = TrackyAccessor.getTrackingSources(this.level);
		List<ServerPlayer> players = this.level.getPlayers(player -> true);
		Set<ChunkPos> positions = new HashSet<>();
		for (ServerPlayer player : players) {
			ITrackChunks tracker = (ITrackChunks) player;
			tracker.update();

			for (Supplier<Collection<TrackingSource>> collectionSupplier : map.values()) {
				for (TrackingSource trackingSource : collectionSupplier.get()) {
					// No point in checking the player if they aren't valid for the chunk source
					if (!trackingSource.check(player)) {
						continue;
					}

					trackingSource.forEachValid(true, player, (chunkPos) -> {
						boolean wasTracked = this.tracky$Forced.remove(chunkPos);
						if (positions.add(chunkPos)) {
							// The chunk was not previously tracked, so we need to load the chunk and sync it to the player
							if (!wasTracked) {
								this.level.setChunkForced(chunkPos.x, chunkPos.z, true);
							}
							tracker.oldTrackedChunks().remove(chunkPos);
							if (tracker.trackedChunks().add(chunkPos)) {
								this.updateChunkTracking(player, chunkPos, new MutableObject<>(), false, true);
							}
						}
					});
				}
			}

			for (ChunkPos chunkPos : tracker.oldTrackedChunks()) {
				this.updateChunkTracking(player, chunkPos, new MutableObject<>(), true, false);
				tracker.trackedChunks().remove(chunkPos);
			}
		}

		for (ChunkPos chunkPos : this.tracky$Forced) {
			this.level.setChunkForced(chunkPos.x, chunkPos.z, false);
		}

		// All remaining chunks have been unloaded, so we can update the current tracking set
		this.tracky$Forced.clear();
		this.tracky$Forced.addAll(positions);
		profiler.pop();
	}

	@Inject(method = "updateChunkTracking", at = @At(value = "HEAD"), cancellable = true)
	public void captureChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
		// If we're trying to unload the chunk, then it should be cancelled if tracky is tracking it
		if (pWasLoaded && !pLoad) {
			if (((ITrackChunks) pPlayer).trackedChunks().contains(pChunkPos)) {
				ci.cancel();
			}
		}
	}
}
