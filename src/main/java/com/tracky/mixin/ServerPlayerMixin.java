package com.tracky.mixin;

import com.tracky.api.TrackingSource;
import com.tracky.impl.ITrackChunks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;
import java.util.Set;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ITrackChunks {

    @Unique
    private boolean tracky$shouldUpdate = false;
    @Unique
    private boolean tracky$update = false;
    @Unique
    private final Set<TrackingSource> tracky$trackedSources = new HashSet<>();
    @Unique
    private Set<ChunkPos> tracky$chunksBeingTracked = new HashSet<>();
    @Unique
    private Set<ChunkPos> tracky$lastChunksBeingTracked = new HashSet<>();

    @Override
    public void startTrackingTick() {
        Set<ChunkPos> c = this.tracky$lastChunksBeingTracked;
        this.tracky$lastChunksBeingTracked = this.tracky$chunksBeingTracked;
        this.tracky$chunksBeingTracked = c;
        this.tracky$update = true;
    }

    @Override
    public void endTrackingTick() {
        this.tracky$lastChunksBeingTracked.clear();
        this.tracky$update = false;
    }

    @Override
    public Set<ChunkPos> oldTrackedChunks() {
        return this.tracky$lastChunksBeingTracked;
    }

    @Override
    public Set<TrackingSource> trackedSources() {
        return this.tracky$trackedSources;
    }

    @Override
    public Set<ChunkPos> trackedChunks() {
        return this.tracky$chunksBeingTracked;
    }

    @Override
    public boolean setDoUpdate(boolean update) {
        boolean old = this.tracky$shouldUpdate;
        this.tracky$shouldUpdate = update;
        return old;
    }

    @Override
    public boolean shouldUpdate() {
        return this.tracky$shouldUpdate;
    }

    @Override
    public boolean isUpdating() {
        return this.tracky$update;
    }
}
