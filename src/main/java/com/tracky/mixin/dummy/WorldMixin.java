package com.tracky.mixin.dummy;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.access.ServerMapHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
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

@Mixin(Level.class)
public class WorldMixin implements ServerMapHolder {
	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(WritableLevelData p_204149_, ResourceKey p_204150_, Holder p_204151_, Supplier p_204152_, boolean p_204153_, boolean p_204154_, long p_204155_, CallbackInfo ci) {
//		trackyForcedChunks = new HashMap<>();
//		trackyForcedPlayers = new HashMap<>();
//
//		ArrayList<SectionPos> positions = new ArrayList<>();
//
//		// all temp
////		{
////			SectionPos startPos = SectionPos.of(new BlockPos(42, 0, 71));
////			SectionPos endPos = SectionPos.of(new BlockPos(-88, 0, -61));
////			for (int x = endPos.getX(); x <= startPos.getX(); x++) {
////				for (int y = startPos.getY(); y <= endPos.getY(); y++) {
////					for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
////						positions.add(SectionPos.of(x, y, z));
////					}
////				}
////			}
////		}
//		{
//			SectionPos startPos = SectionPos.of(new BlockPos(-297 - 200, -63, 296));
//			SectionPos endPos = SectionPos.of(new BlockPos(-456 - 200, 319, 328));
//			for (int x = endPos.getX(); x <= startPos.getX(); x++) {
//				for (int y = startPos.getY(); y <= endPos.getY(); y++) {
//					for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
//						positions.add(SectionPos.of(x, y, z));
//					}
//				}
//			}
//		}
//
//
//		TrackyAccessor.getForcedChunks((Level) (Object) this).put(Tracky.getDefaultUUID(), (player) -> {
//			TrackyAccessor.getPlayersLoadingChunks((Level) (Object) this).put(Tracky.getDefaultUUID(), Arrays.asList(player));
//
//			return positions;
//		});
	}
	
	@Unique
	private Map<UUID, Function<Player, Collection<SectionPos>>> trackyForcedChunks;
	
	@Override
	public Map<UUID, Function<Player, Collection<SectionPos>>> trackyHeldMapS() {
		return trackyForcedChunks;
	}
	
	@Unique
	private Map<UUID, List<Player>> trackyForcedPlayers;
	
	@Override
	public Map<UUID, List<Player>> trackyPlayerMap() {
		return trackyForcedPlayers;
	}
}
