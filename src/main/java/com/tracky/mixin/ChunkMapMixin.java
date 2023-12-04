package com.tracky.mixin;

import com.tracky.TrackyAccessor;
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

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

	@Shadow
	@Final
	ServerLevel level;

	@Unique
	boolean tracky$success = false;

	@Shadow
	protected abstract void updateChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad);

	@Unique
	private final List<ChunkPos> tracky$Forced = new ArrayList<>();

	/**
	 * Make the chunk manager send chunk updates to clients tracking tracky-enforced chunks
	 */
	// I think this is the right target?
	@Inject(method = "getPlayers", at = @At("RETURN"), cancellable = true)
	public void getTrackingPlayers(ChunkPos chunkPos, boolean boundaryOnly, CallbackInfoReturnable<List<ServerPlayer>> cir) {
//		if (!TrackyAccessor.isMainTracky()) return;
		final Map<UUID, Function<Player, Collection<ChunkPos>>> map = TrackyAccessor.getForcedChunks(level);

		final List<ServerPlayer> players = new ArrayList<>();
		boolean isTrackedByAny = false;

		// for all players in the level send the relevant chunks
		// messy iteration but no way to avoid with our structure
		for (ServerPlayer player : this.level.getPlayers((p) -> true)) {
			for (Function<Player, Collection<ChunkPos>> func : map.values()) {
//				final Set<ChunkPos> chunks = Tracky.collapse(func.apply(player));

				// it seems Tracky#collapse hinders performance
				// unsure if this is true for all sizes of render source, but it's definitely true for large ones
				Collection<ChunkPos> poses = func.apply(player);

				if (poses.contains(chunkPos)) {
					// send the packet if the player is tracking it
					players.add(player);
					isTrackedByAny = true;
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
		Map<UUID, Function<Player, Collection<ChunkPos>>> function = TrackyAccessor.getForcedChunks(this.level);
		List<ChunkPos> poses = new ArrayList<>();
		for (List<Player> value : TrackyAccessor.getPlayersLoadingChunks(this.level).values()) {
			for (Player player : value) {
				if (!playersChecked.add(player)) {
					continue;
				}

				for (Function<Player, Collection<ChunkPos>> playerIterableFunction : function.values()) {
					for (ChunkPos chunkPos : playerIterableFunction.apply(player)) {
						if (!this.tracky$Forced.remove(chunkPos) && !poses.contains(chunkPos)) {
							this.level.setChunkForced(chunkPos.x, chunkPos.z, true);
							poses.add(chunkPos);
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

		for (Function<Player, Collection<ChunkPos>> value : TrackyAccessor.getForcedChunks(this.level).values()) {
			for (ChunkPos chunkPos : value.apply(player)) {
				this.updateChunkTracking(player, chunkPos, new MutableObject<>(), chunkTracker.trackedChunks().contains(chunkPos), true);
			}
		}

		chunkTracker.tickTracking();
		for (ChunkPos trackedChunk : chunkTracker.oldTrackedChunks()) {
			if (!chunkTracker.trackedChunks().add(trackedChunk)) {
				this.updateChunkTracking(player, trackedChunk, new MutableObject<>(), true, false);
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "updatePlayerPos")
	public void preUpdatePos(ServerPlayer p_140374_, CallbackInfoReturnable<SectionPos> cir) {
		((ITrackChunks) p_140374_).setDoUpdate(true);
	}

	@Inject(method = "updateChunkTracking", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;untrackChunk(Lnet/minecraft/world/level/ChunkPos;)V", shift = At.Shift.BEFORE), cancellable = true)
	public void captureChunkTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
		// Prevent vanilla from unloading a tracked chunk
		if (((ITrackChunks) pPlayer).trackedChunks().contains(pChunkPos)) {
			ci.cancel();
		}
	}

	@Inject(at = @At("HEAD"), method = "playerLoadedChunk")
	public void preLoadChunk(ServerPlayer pPlaer, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, LevelChunk pChunk, CallbackInfo ci) {
		this.tracky$success = true;
	}

	@Inject(at = @At("HEAD"), method = "updateChunkTracking")
	public void preUpdateTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad, CallbackInfo ci) {
		this.tracky$success = false;
	}
}
