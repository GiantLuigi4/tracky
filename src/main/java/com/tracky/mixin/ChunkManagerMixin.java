package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import com.tracky.debug.ITrackChunks;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

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
		if (!TrackyAccessor.isMainTracky()) return;
		final Map<UUID, Function<Player, Iterable<ChunkPos>>> map = TrackyAccessor.getForcedChunks(level);
		
		final List<ServerPlayer> players = new ArrayList<>();
		boolean isTrackedByAny = false;
		
		// for all players in the level send the relevant chunks
		// messy iteration but no way to avoid with our structure
		for (ServerPlayer player : level.getPlayers((p) -> true)) {
			for (Function<Player, Iterable<ChunkPos>> func : map.values()) {
				final Iterable<ChunkPos> chunks = func.apply(player);
				
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
	
	// TODO: is there a way to do this without replacing the entire "move" method?
	@Inject(at = @At("HEAD"), method = "move", cancellable = true)
	public void preMove(ServerPlayer pPlayer, CallbackInfo ci) {
		for (ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
			if (chunkmap$trackedentity.entity == pPlayer) chunkmap$trackedentity.updatePlayers(this.level.players());
			else chunkmap$trackedentity.updatePlayer(pPlayer);
		}
		
		SectionPos oldPos = pPlayer.getLastSectionPos();
		SectionPos newPos = SectionPos.of(pPlayer);
		long oldAsLong = oldPos.chunk().toLong();
		long newAsLong = newPos.chunk().toLong();
		boolean ignorePlayer = this.playerMap.ignored(pPlayer);
		boolean skipPlayer = this.skipPlayer(pPlayer);
		boolean wat = oldPos.asLong() != newPos.asLong();
		if (wat || ignorePlayer != skipPlayer) {
			this.updatePlayerPos(pPlayer);
			if (!ignorePlayer) this.distanceManager.removePlayer(oldPos, pPlayer);
			if (!skipPlayer) this.distanceManager.addPlayer(newPos, pPlayer);
			if (!ignorePlayer && skipPlayer) this.playerMap.ignorePlayer(pPlayer);
			if (ignorePlayer && !skipPlayer) this.playerMap.unIgnorePlayer(pPlayer);
			if (oldAsLong != newAsLong) this.playerMap.updatePlayer(oldAsLong, newAsLong, pPlayer);
		}
		
		ITrackChunks chunkTracker = (ITrackChunks) pPlayer;
		if (!chunkTracker.setDoUpdate(false)) return;
		
		ArrayList<ChunkPos> tracked = new ArrayList<>();
		boolean anyFailed = false;
		anyFailed = forAllInRange(pPlayer.position(), pPlayer, chunkTracker, tracked) || anyFailed;
		for (Function<Player, Iterable<ChunkPos>> value : TrackyAccessor.getForcedChunks(level).values()) {
			for (ChunkPos chunkPos : value.apply(pPlayer)) {
				if (tracked.contains(chunkPos)) continue;
				
				boolean wasLoaded;
				updateChunkTracking(
						pPlayer, chunkPos,
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

//		chunkTracker.trackedChunks().removeAll(tracked);
		for (ChunkPos trackedChunk : chunkTracker.trackedChunks()) {
//			if (nonEclidianInRange(trackedChunk, pPlayer)) {
//				tracked.add(trackedChunk);
//				updateChunkTracking(
//						pPlayer, trackedChunk,
//						new MutableObject<>(),
//						true, // this will always be true, no point in checking the list
//						true // player should know about the chunk
//				);
//			} else {
			updateChunkTracking(
					pPlayer, trackedChunk,
					new MutableObject<>(),
					true, // this will always be true, no point in checking the list
					false // unloading/untracking
			);
//			}
		}
		chunkTracker.trackedChunks().clear();
		chunkTracker.trackedChunks().addAll(tracked);
		
		if (anyFailed) chunkTracker.setDoUpdate(true);
		ci.cancel();
	}
	
	@Unique
	public boolean forAllInRange(Vec3 origin, ServerPlayer pPlayer, ITrackChunks chunkTracker, ArrayList<ChunkPos> tracked) {
		boolean anyFailed = false;
		
		ChunkPos playerChunk = pPlayer.chunkPosition();
		ChunkPos center = new ChunkPos(new BlockPos(origin.x, origin.y, origin.z));
		
		for (int x = -viewDistance; x <= viewDistance; x++) {
			for (int z = -viewDistance; z <= viewDistance; z++) {
				ChunkPos pos = new ChunkPos(center.x + x, center.z + z);
				if (tracked.contains(pos)) continue;

//				Vec3 chunkPos = new Vec3(pos.x, 0, pos.z);
//
//				// TODO: this distance check breaks everything
//				if (chunkPos.distanceToSqr(pChunkPos) < viewDistance) {
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
//				}
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
