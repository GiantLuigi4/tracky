package com.tracky.impl;

import com.tracky.api.TrackingSource;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;

public interface ITrackChunks {

    Set<TrackingSource> trackedSources();

    Set<ChunkPos> trackedChunks();

    Set<ChunkPos> oldTrackedChunks();

    void tickTracking();

    boolean setDoUpdate(boolean val);

    boolean shouldUpdate();
}
