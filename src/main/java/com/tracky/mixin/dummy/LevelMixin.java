package com.tracky.mixin.dummy;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.access.ServerMapHolder;
import com.tracky.api.SquareTrackingSource;
import com.tracky.api.TrackingSource;
import com.tracky.debug.TestSource;
import com.tracky.util.MapWrapper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
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
import java.util.function.Supplier;

@Mixin(value = Level.class, priority = 500)
public abstract class LevelMixin implements ServerMapHolder {
	
	@Shadow public abstract long getDayTime();
	
	@Shadow public abstract long getGameTime();
	
	@Unique
	private Map<UUID, Supplier<Collection<TrackingSource>>> tracky$TrackingSources;

	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(WritableLevelData pLevelData, ResourceKey pDimension, RegistryAccess pRegistryAccess,
	                     Holder pDimensionTypeRegistration, Supplier pProfiler, boolean pIsClientSide, boolean pIsDebug,
	                     long pBiomeZoomSeed, int pMaxChainedNeighborUpdates, CallbackInfo ci) {
		this.tracky$TrackingSources = new MapWrapper<>(new HashMap<>());
		
		if (!Tracky.ENABLE_TEST)
			return;
		
		boolean[] on = new boolean[]{true};
		long[] lastTime = new long[1];
		
		SquareTrackingSource source = new SquareTrackingSource(new ChunkPos(TestSource.MIN), new ChunkPos(TestSource.MAX));
		TrackyAccessor.getTrackingSources(((Level) (Object) this)).put(
				Tracky.getDefaultUUID("tracky", "testing"),
				() -> {
					if (Tracky.CYCLE_SOURCE)
						if (this.getGameTime() % 200 == 0) {
							if (lastTime[0] != this.getGameTime()) {
								on[0] = !on[0];
								source.markUpdate(true);
							}
							lastTime[0] = this.getGameTime();
						}
					
					if (on[0]) {
						return List.of(source);
					}
					return List.of();
				}
		);
	}

	@Override
	public Map<UUID, Supplier<Collection<TrackingSource>>> trackyTrackingSources() {
		return this.tracky$TrackingSources;
	}
}
