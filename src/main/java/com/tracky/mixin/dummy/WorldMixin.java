package com.tracky.mixin.dummy;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.access.ServerMapHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the world class
 */
@Mixin(Level.class)
public class WorldMixin implements ServerMapHolder {
	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(WritableLevelData p_204149_, ResourceKey p_204150_, Holder p_204151_, Supplier p_204152_, boolean p_204153_, boolean p_204154_, long p_204155_, CallbackInfo ci) {
		trackyForcedChunks = new HashMap<>();
		trackyForcedPlayers = new HashMap<>();

		ArrayList<ChunkPos> positions = new ArrayList<>();
		
		// all temp
		{
			ChunkPos startPos = new ChunkPos(new BlockPos(80, 0, 80));
			ChunkPos endPos = new ChunkPos(new BlockPos(120, 0, 120));
			for (int x = startPos.x; x <= endPos.x; x++) {
				for (int z = startPos.z; z <= endPos.z; z++) {
					positions.add(new ChunkPos(x, z));
				}
			}
		}
//
//
		TrackyAccessor.getForcedChunks((Level) (Object) this).put(Tracky.getDefaultUUID(), (player) -> {
			TrackyAccessor.getPlayersLoadingChunks((Level) (Object) this).put(Tracky.getDefaultUUID(), Arrays.asList(player));
//
			return positions;
		});
	}
	
	@Unique
	private Map<UUID, Function<Player, Collection<ChunkPos>>> trackyForcedChunks;
	
	@Override
	public Map<UUID, Function<Player, Collection<ChunkPos>>> trackyHeldMapS() {
		return trackyForcedChunks;
	}
	
	@Unique
	private Map<UUID, List<Player>> trackyForcedPlayers;
	
	@Override
	public Map<UUID, List<Player>> trackyPlayerMap() {
		return trackyForcedPlayers;
	}
}
