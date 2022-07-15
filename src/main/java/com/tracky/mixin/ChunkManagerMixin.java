package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(ChunkMap.class)
public class ChunkManagerMixin {
	
	@Shadow
	@Final
	ServerLevel level;
	
	/**
	 * Make the chunk manager send chunk updates to clients tracking tracky-enforced chunks
	 */
	// I think this is the right target?
	@Inject(method = "getPlayers", at = @At("HEAD"), cancellable = true)
	public void getTrackingPlayers(ChunkPos chunkPos, boolean boundaryOnly, CallbackInfoReturnable<Stream<ServerPlayer>> cir) {
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
			cir.setReturnValue(players.stream());
			cir.cancel();
		}
	}
}
