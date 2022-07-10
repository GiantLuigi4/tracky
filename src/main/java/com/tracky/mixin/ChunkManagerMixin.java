package com.tracky.mixin;

import com.tracky.TrackyAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PlayerGenerationTracker;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Mixin(ChunkManager.class)
public class ChunkManagerMixin {

    @Shadow @Final private ServerWorld world;

    /**
     * Make the chunk manager send chunk updates to clients tracking tracky-enforced chunks
     */
    @Inject(method = "getTrackingPlayers", at = @At("HEAD"), cancellable = true)
    public void getTrackingPlayers(ChunkPos chunkPos, boolean boundaryOnly, CallbackInfoReturnable<Stream<ServerPlayerEntity>> cir) {
        if (!TrackyAccessor.isMainTracky()) return;
        final Map<UUID, Function<PlayerEntity, Iterable<ChunkPos>>> map = TrackyAccessor.getForcedChunks(world);

        final List<ServerPlayerEntity> players = new ArrayList<>();
        boolean isTrackedByAny = false;

        // for all players in the level send the relevant chunks
        // messy iteration but no way to avoid with our structure
        for (ServerPlayerEntity player : world.getPlayers()) {
            for (Function<PlayerEntity, Iterable<ChunkPos>> func : map.values()) {
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

        if(isTrackedByAny) {
            cir.setReturnValue(players.stream());
            cir.cancel();
        }
    }
}
