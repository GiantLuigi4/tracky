package com.tracky.mixin;

import com.mojang.datafixers.util.Either;
import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.debug.ITrackChunks;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Mixin(ChunkMap.class)
public abstract class ChunkManagerMixin {

	@Shadow
	@Final
	ServerLevel level;

	/**
	 * Make the chunk manager send chunk updates to clients tracking tracky-enforced chunks
	 */
	// I think this is the right target?
	@Inject(method = "getPlayers", at = @At("RETURN"), cancellable = true)
	public void getTrackingPlayers(ChunkPos chunkPos, boolean boundaryOnly, CallbackInfoReturnable<List<ServerPlayer>> cir) {
//		if (!TrackyAccessor.isMainTracky()) return;
		final Map<UUID, Function<Player, Collection<SectionPos>>> map = TrackyAccessor.getForcedChunks(level);

		final List<ServerPlayer> players = new ArrayList<>();
		boolean isTrackedByAny = false;

		// for all players in the level send the relevant chunks
		// messy iteration but no way to avoid with our structure
		for (ServerPlayer player : level.getPlayers((p) -> true)) {
			for (Function<Player, Collection<SectionPos>> func : map.values()) {
				final Iterable<ChunkPos> chunks = Tracky.collapse(func.apply(player));

				for (ChunkPos chunk : chunks) {
					if (chunk.equals(chunkPos)) {
						// send the packet if the player is tracking it
						players.add(player);
						isTrackedByAny = true;
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

	@Shadow
	int viewDistance;
	boolean success = false;
	@Shadow
	@Final
	private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
	@Shadow
	@Final
	private PlayerMap playerMap;
	@Shadow
	@Final
	private ChunkMap.DistanceManager distanceManager;

	@Shadow
	@Nullable
	protected abstract ChunkHolder getVisibleChunkIfPresent(long p_140328_);

	@Shadow
	protected abstract void updateChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad);

	@Shadow
	protected abstract boolean skipPlayer(ServerPlayer pPlayer);

	@Shadow
	protected abstract SectionPos updatePlayerPos(ServerPlayer p_140374_);

	@Shadow
	protected abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos pChunkPos);

	@Shadow
	protected abstract void scheduleUnload(long pChunkPos, ChunkHolder pChunkHolder);

	@Unique
	ArrayList<ChunkPos> trackyForced = new ArrayList<>();

	@Inject(at = @At("HEAD"), method = "tick(Ljava/util/function/BooleanSupplier;)V")
	public void preTick(BooleanSupplier pHasMoreTime, CallbackInfo ci) throws ExecutionException, InterruptedException {
		ArrayList<Player> playersChecked = new ArrayList<>();
		Map<UUID, Function<Player, Collection<SectionPos>>> function = TrackyAccessor.getForcedChunks(level);
		ArrayList<ChunkPos> poses = new ArrayList<>();
		for (List<Player> value : TrackyAccessor.getPlayersLoadingChunks(level).values()) {
			for (Player player : value) {
				if (playersChecked.contains(player)) continue;

				playersChecked.add(player);
				for (Function<Player, Collection<SectionPos>> playerIterableFunction : function.values()) {
					for (ChunkPos chunkPos : Tracky.collapse(playerIterableFunction.apply(player))) {
						if (!trackyForced.remove(chunkPos) && !poses.contains(chunkPos)) {
							level.setChunkForced(chunkPos.x, chunkPos.z, true);
						}
					}
				}
			}
		}

		for (ChunkPos chunkPos : trackyForced) {
			level.setChunkForced(chunkPos.x, chunkPos.z, false);
		}

		trackyForced.addAll(poses);
	}


	/**
	 * Tracks chunks loaded by cameras to make sure they're being sent to the client
	 */
	@Inject(method = "move", at = @At(value = "TAIL"))
	private void trackCameraLoadedChunks(ServerPlayer player, CallbackInfo callback) {

		ITrackChunks chunkTracker = (ITrackChunks) player;
		if (!chunkTracker.setDoUpdate(false)) return;

		ArrayList<ChunkPos> tracked = new ArrayList<>();
		chunkTracker.trackedChunks().clear();
		boolean anyFailed = forAllInRange(player.position(), player, chunkTracker, tracked);
		for (Function<Player, Collection<SectionPos>> value : TrackyAccessor.getForcedChunks(level).values()) {
			for (ChunkPos chunkPos : Tracky.collapse(value.apply(player))) {
				if (tracked.contains(chunkPos)) continue;

				boolean wasLoaded;
				updateChunkTracking(
						player, chunkPos,
						new MutableObject<>(),
						wasLoaded = chunkTracker.trackedChunks().remove(chunkPos), // remove it so that the next loop doesn't untrack it
						true // start tracking
				);

				if (!wasLoaded && !success) {
					anyFailed = true;
				} else {
					tracked.add(chunkPos);
				}
			}
		}
		chunkTracker.tickTracking();
		chunkTracker.trackedChunks().addAll(tracked);
		
		if (anyFailed) {
			// if a chunk doesn't get loaded by the time the track starting finishes, mark it for another attempt at tracking
			chunkTracker.setDoUpdate(true);
		}
	}

	@Unique
	public boolean forAllInRange(Vec3 origin, ServerPlayer pPlayer, ITrackChunks chunkTracker, ArrayList<ChunkPos> tracked) {
		boolean anyFailed = false;

		ChunkPos playerChunk = pPlayer.chunkPosition();
		ChunkPos center = new ChunkPos(new BlockPos(origin.x, origin.y, origin.z));
		Vec3 pChunkPos = new Vec3(playerChunk.x, 0, playerChunk.z);

		for (int x = -viewDistance; x <= viewDistance; x++) {
			for (int z = -viewDistance; z <= viewDistance; z++) {
				ChunkPos pos = new ChunkPos(center.x + x, center.z + z);
				if (tracked.contains(pos)) continue;

				Vec3 chunkPos = new Vec3(pos.x, 0, pos.z);

				// TODO: this distance check breaks everything
				if (chunkPos.distanceToSqr(pChunkPos) < viewDistance) {
					boolean wasLoaded;
					updateChunkTracking(
							pPlayer, pos,
							new MutableObject<>(),
							wasLoaded = chunkTracker.trackedChunks().remove(pos), // remove it so that the next loop doesn't untrack it
							true // start tracking
					);

					if (!wasLoaded && !success) {
						anyFailed = true;
					} else {
						tracked.add(pos);
					}
				}
			}
		}

		return anyFailed;
	}

	@Inject(at = @At("HEAD"), method = "updatePlayerPos")
	public void preUpdatePos(ServerPlayer p_140374_, CallbackInfoReturnable<SectionPos> cir) {
		((ITrackChunks) p_140374_).setDoUpdate(true);
	}

	@Inject(at = @At("HEAD"), method = "playerLoadedChunk")
	public void preLoadChunk(ServerPlayer pPlaer, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, LevelChunk pChunk, CallbackInfo ci) {
		success = true;
	}

	@Inject(at = @At("HEAD"), method = "updateChunkTracking", cancellable = true)
	public void preUpdateTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
		success = false;
	}
}
