package com.tracky.mixin.dummy;

import com.tracky.access.ServerMapHolder;
import com.tracky.api.TrackingSource;
import com.tracky.api.event.RegisterTrackyTrackingSourceEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Mixin(Level.class)
public class LevelMixin implements ServerMapHolder {

    @Unique
    private Collection<TrackingSource> tracky$TrackingSources;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void postInit(WritableLevelData pLevelData, ResourceKey pDimension, RegistryAccess pRegistryAccess,
                         Holder pDimensionTypeRegistration, Supplier pProfiler, boolean pIsClientSide, boolean pIsDebug,
                         long pBiomeZoomSeed, int pMaxChainedNeighborUpdates, CallbackInfo ci) {
        List<TrackingSource> sources = new ArrayList<>();
        this.tracky$TrackingSources = Collections.unmodifiableCollection(sources);
        RegisterTrackyTrackingSourceEvent event = new RegisterTrackyTrackingSourceEvent((Level) (Object) this, sources);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public Collection<TrackingSource> trackyTrackingSources() {
        return this.tracky$TrackingSources;
    }
}
