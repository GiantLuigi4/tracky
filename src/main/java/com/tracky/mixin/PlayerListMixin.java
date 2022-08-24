package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Player list is hardcoded to just send to nearby players, causing chunk updates to not be sent
 * This affects a lot of packets, so is neccesary.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	
	@Shadow
	@Final
	private MinecraftServer server;
	
	
	// is broadcast the right target?
	@Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
	public void broadcast(@Nullable Player pExcept, double pX, double pY, double pZ, double pRadius, ResourceKey<Level> pDimension, Packet<?> pPacket, CallbackInfo ci) {
//		if (!TrackyAccessor.isMainTracky()) return;
		
		ServerLevel level = server.getLevel(pDimension);
		
		if (level != null) {
			int x = Mth.intFloorDiv((int) pX, 16);
			int y = Mth.intFloorDiv((int) pY, 16);
			int z = Mth.intFloorDiv((int) pZ, 16);
			
			final ChunkPos pos = new ChunkPos(x, z);
			
			final Map<UUID, Function<Player, Iterable<ChunkPos>>> map = TrackyAccessor.getForcedChunks(level);
			
			// for all players in the level send the relevant chunks
			// messy iteration but no way to avoid with our structure
			for (ServerPlayer player : level.getPlayers((p) -> true)) {
				if (player == pExcept) continue;
				for (Function<Player, Iterable<ChunkPos>> func : map.values()) {
					final Iterable<ChunkPos> chunks = func.apply(player);
					
					for (ChunkPos chunk : chunks) {
						if (chunk.equals(pos)) {
							// send the packet if the player is tracking it
							player.connection.send(pPacket);
							ci.cancel();
						}
					}
				}
			}
		}
	}
}