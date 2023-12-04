package com.tracky.mixin;

import com.tracky.debug.ITrackChunks;
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
	private final Set<ChunkPos> tracky$chunksBeingTracked = new HashSet<>();
	@Unique
	private final Set<ChunkPos> tracky$lastChunksBeingTracked = new HashSet<>();

	@Override
	public void tickTracking() {
		this.tracky$lastChunksBeingTracked.addAll(this.tracky$chunksBeingTracked);
		this.tracky$chunksBeingTracked.clear();
	}

	@Override
	public Set<ChunkPos> oldTrackedChunks() {
		return this.tracky$lastChunksBeingTracked;
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
}
