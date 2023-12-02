package com.tracky.mixin.dummy;

import com.google.common.collect.ImmutableList;
import com.tracky.Tracky;
import com.tracky.access.ServerMapHolder;
import com.tracky.util.MapWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
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

@Mixin(value = Level.class, priority = 500)
public class LevelMixin implements ServerMapHolder {

	@Unique
	private Map<UUID, Function<Player, Collection<SectionPos>>> trackyForcedChunks;

	@Unique
	private Map<UUID, List<Player>> trackyForcedPlayers;

	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(WritableLevelData pLevelData, ResourceKey pDimension, RegistryAccess pRegistryAccess,
						 Holder pDimensionTypeRegistration, Supplier pProfiler, boolean pIsClientSide, boolean pIsDebug,
						 long pBiomeZoomSeed, int pMaxChainedNeighborUpdates, CallbackInfo ci) {
		trackyForcedChunks = new MapWrapper<>(new HashMap<>());
		trackyForcedPlayers = new MapWrapper<>(new HashMap<>());

		if (!Tracky.ENABLE_TEST) {
			return;
		}

		ArrayList<SectionPos> positions = new ArrayList<>();

		// all temp
		{
			SectionPos startPos = SectionPos.of(new BlockPos(42, 0, 71));
			SectionPos endPos = SectionPos.of(new BlockPos(-88, 0, -61));
			for (int x = endPos.getX(); x <= startPos.getX(); x++) {
				for (int y = startPos.getY(); y <= endPos.getY(); y++) {
					for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
						positions.add(SectionPos.of(x, y, z));
					}
				}
			}
		}

		{
			SectionPos startPos = SectionPos.of(new BlockPos(-297 - 200, -63, 296));
			SectionPos endPos = SectionPos.of(new BlockPos(-456 - 200, 319, 328));
			for (int x = endPos.getX(); x <= startPos.getX(); x++) {
				for (int y = startPos.getY(); y <= endPos.getY(); y++) {
					for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
						positions.add(SectionPos.of(x, y, z));
					}
				}
			}
		}

		final List<SectionPos> immut = ImmutableList.copyOf(positions);

		UUID testUUID = Tracky.getDefaultUUID("tracky", "testing");
		trackyForcedChunks.put(testUUID, (player) -> {
			trackyForcedPlayers.put(testUUID, Collections.singletonList(player));
			return immut;
		});
	}
	
	@Override
	public Map<UUID, Function<Player, Collection<SectionPos>>> trackyHeldMapS() {
		return trackyForcedChunks;
	}
	
	@Override
	public Map<UUID, List<Player>> trackyPlayerMap() {
		return trackyForcedPlayers;
	}
}
