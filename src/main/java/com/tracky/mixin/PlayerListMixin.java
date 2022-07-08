package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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

    @Shadow @Final private MinecraftServer server;


    @Inject(method = "sendToAllNearExcept", at = @At("HEAD"), cancellable = true)
    public void broadcast(@Nullable PlayerEntity pExcept, double pX, double pY, double pZ, double pRadius, RegistryKey<World> pDimension, IPacket<?> pPacket, CallbackInfo ci) {

        ServerWorld level = server.getWorld(pDimension);

        if(level != null) {
            int x = MathHelper.intFloorDiv((int) pX, 16);
            int y = MathHelper.intFloorDiv((int) pY, 16);
            int z = MathHelper.intFloorDiv((int) pZ, 16);

            final ChunkPos pos = new ChunkPos(x, z);

            final Map<UUID, Function<PlayerEntity, Iterable<ChunkPos>>> map = TrackyAccessor.getForcedChunks(level);

            // for all players in the level send the relevant chunks
            // messy iteration but no way to avoid with our structure
            for (ServerPlayerEntity player : level.getPlayers()) {
                for (Function<PlayerEntity, Iterable<ChunkPos>> func : map.values()) {
                    final Iterable<ChunkPos> chunks = func.apply(player);

                    for (ChunkPos chunk : chunks) {
                        if (chunk.equals(pos)) {
                            // send the packet if the player is tracking it
                            player.connection.sendPacket(pPacket);
                            ci.cancel();
                        }
                    }
                }
            }
        }
    }
}