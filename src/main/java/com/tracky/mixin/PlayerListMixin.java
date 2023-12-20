package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import com.tracky.api.TrackingSource;
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
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

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
			int x = Mth.floorDiv((int) pX, 16);
			int z = Mth.floorDiv((int) pZ, 16);

			ChunkPos ckPos = new ChunkPos(x, z);

			// for all players in the level send the relevant chunks
			// messy iteration but no way to avoid with our structure
			loopPlayers:
			for (ServerPlayer player : level.getPlayers((p) -> true)) {
				if (player == pExcept) continue;
				for (Supplier<Collection<TrackingSource>> value : TrackyAccessor.getTrackingSources(level).values()) {
					for (TrackingSource trackingSource : value.get()) {
						if (trackingSource.containsChunk(ckPos)) {
							if (trackingSource.checkRenderDist(player, ckPos)) {
								player.connection.send(pPacket);
								ci.cancel();
								continue loopPlayers;
							}
						}
					}
				}
			}
		}
	}
}