package com.tracky.impl;

import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Set;

@ApiStatus.Internal
public interface ITrackChunks {

	void update();

	Collection<ChunkPos> trackedChunks();

	Collection<ChunkPos> oldTrackedChunks();
}
