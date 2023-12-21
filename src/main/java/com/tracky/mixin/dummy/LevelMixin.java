package com.tracky.mixin.dummy;

import com.google.common.collect.ImmutableList;
import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.access.ServerMapHolder;
import com.tracky.api.RenderSource;
import com.tracky.api.SquareTrackingSource;
import com.tracky.api.TrackingSource;
import com.tracky.debug.TestSource;
import com.tracky.util.MapWrapper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(value = Level.class, priority = 500)
public abstract class LevelMixin implements ServerMapHolder {

	@Shadow public abstract long getDayTime();

	@Unique
	private Map<UUID, Supplier<Collection<TrackingSource>>> trackyTrackingSources;

	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(WritableLevelData pLevelData, ResourceKey pDimension, RegistryAccess pRegistryAccess,
	                     Holder pDimensionTypeRegistration, Supplier pProfiler, boolean pIsClientSide, boolean pIsDebug,
	                     long pBiomeZoomSeed, int pMaxChainedNeighborUpdates, CallbackInfo ci) {
		trackyTrackingSources = new MapWrapper<>(new HashMap<>());

		if (FMLEnvironment.production)
			return;

		SquareTrackingSource source = new SquareTrackingSource(
				new ChunkPos(new BlockPos(-656, -63, 296)),
				new ChunkPos(new BlockPos(-497, 319, 328))
		);
		boolean[] shown = new boolean[1];
		shown[0] = true;

		TrackyAccessor.getTrackingSources(((Level) (Object) this)).put(
				Tracky.getDefaultUUID("tracky", "testing"),
				() -> {
					if (Tracky.ENABLE_TEST) {
						if (getDayTime() % 200 == 0) {
							if ((Object) this instanceof ServerLevel svrlvl) {
								shown[0] = !shown[0];
								for (ServerPlayer player : svrlvl.getPlayers((p) -> true)) {
									TrackyAccessor.markForRetracking(player);
								}
							}
						}

						if (!shown[0]) return List.of();
						return List.of(source);
					}

					return List.of();
				}
		);
	}

	@Override
	public Map<UUID, Supplier<Collection<TrackingSource>>> trackyTrackingSources() {
		return trackyTrackingSources;
	}
}
