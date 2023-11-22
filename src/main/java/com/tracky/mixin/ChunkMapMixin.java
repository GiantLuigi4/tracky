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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
	
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
//				final Set<ChunkPos> chunks = Tracky.collapse(func.apply(player));
				
				// it seems Tracky#collapse hinders performance
				// unsure if this is true for all sizes of render source, but it's definitely true for large ones
				Collection<SectionPos> poses = func.apply(player);
				
				for (SectionPos pose : poses) {
					if (pose.x() == chunkPos.x && pose.z() == chunkPos.z) {
						// send the packet if the player is tracking it
						players.add(player);
						isTrackedByAny = true;
						break;
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
							poses.add(chunkPos);
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
	
	
	@Unique
	// these fields are the way they are for both optimization and safety incase of multi-threading moddings
	private static AtomicReference<Set<ChunkPos>> collapsedChunks = new AtomicReference<>();
	private boolean flattenedArray = false; // if this is false, then the tail injections aren't going to work on the current tick, so it should wait until the following tick
	
	@Inject(method = "move", at = @At(value = "HEAD"))
	private void preTrackCameraLoadedChunks(ServerPlayer player, CallbackInfo callback) {
		flattenedArray = false;
		ITrackChunks chunkTracker = (ITrackChunks) player;
		if (!chunkTracker.shouldUpdate()) return;
		flattenedArray = true;
		
		Set<ChunkPos> positions = new HashSet<>();
		for (Function<Player, Collection<SectionPos>> value : TrackyAccessor.getForcedChunks(level).values()) {
			positions.addAll(Tracky.collapse(value.apply(player)));
		}
		collapsedChunks.set(positions);
	}
	
	/**
	 * Tracks chunks loaded by cameras to make sure they're being sent to the client
	 */
	@Inject(method = "move", at = @At(value = "TAIL"))
	private void trackCameraLoadedChunks(ServerPlayer player, CallbackInfo callback) {
		if (!flattenedArray) return;
		
		ITrackChunks chunkTracker = (ITrackChunks) player;
		if (!chunkTracker.shouldUpdate()) return;
		chunkTracker.setDoUpdate(false);
		
		Set<ChunkPos> positions = collapsedChunks.get();
		
		for (ChunkPos position : positions) {
			Tracky$modifyTracking(player, position, new MutableObject<>(), chunkTracker.trackedChunks().contains(position), true);
		}
		
		chunkTracker.tickTracking();
		for (ChunkPos trackedChunk : chunkTracker.oldTrackedChunks()) {
			if (!positions.contains(trackedChunk)) { // TODO: this is slow
				updateChunkTracking(player, trackedChunk, new MutableObject<>(), true, false);
			} else {
				chunkTracker.trackedChunks().add(trackedChunk);
			}
		}
	}
	
	@Unique
	public boolean forAllInRange(Vec3 origin, ServerPlayer pPlayer, ITrackChunks chunkTracker, ArrayList<ChunkPos> tracked) {
		boolean anyFailed = false;
		
		ChunkPos playerChunk = pPlayer.chunkPosition();
		ChunkPos center = new ChunkPos(new BlockPos((int) Math.floor(origin.x), (int) Math.floor(origin.y),
			(int) Math.floor(origin.z)));
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
							wasLoaded = chunkTracker.oldTrackedChunks().remove(pos), // remove it so that the next loop doesn't untrack it
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
	
	// TODO: I'd like to not use a redirect, but ModifyArgs crashes the game and I think that's a forge issue
	@Redirect(
			method = "move",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ChunkMap;updateChunkTracking(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;Lorg/apache/commons/lang3/mutable/MutableObject;ZZ)V"
			)
	)
	public void modifyTracking(ChunkMap chunkMap, ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad) {
		Tracky$modifyTracking(pPlayer, pChunkPos, pPacketCache, pWasLoaded, pLoad);
	}
	
	protected void Tracky$modifyTracking(ServerPlayer pPlayer, ChunkPos pChunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, boolean pWasLoaded, boolean pLoad) {
		if (!flattenedArray) {
			updateChunkTracking(pPlayer, pChunkPos, pPacketCache, pWasLoaded, pLoad);
			return;
		}
		
		boolean isForced = false;
		if (collapsedChunks.get().contains(pChunkPos)) {
			pLoad = true;
			isForced = true;
		}
		if (isForced) {
			((ITrackChunks) pPlayer).trackedChunks().add(pChunkPos);
		}
		updateChunkTracking(pPlayer, pChunkPos, pPacketCache, pWasLoaded, pLoad);
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