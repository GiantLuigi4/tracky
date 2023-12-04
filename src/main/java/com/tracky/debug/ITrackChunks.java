package com.tracky.debug;

import net.minecraft.world.level.ChunkPos;

import java.util.Set;

public interface ITrackChunks {

	Set<ChunkPos> trackedChunks();

	Set<ChunkPos> oldTrackedChunks();

	void tickTracking();

	boolean setDoUpdate(boolean val);

	boolean shouldUpdate();
}
