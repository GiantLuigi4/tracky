package com.tracky.mixin;

import com.tracky.impl.ITrackChunks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ITrackChunks {

	@Unique
	private Set<ChunkPos> tracky$chunksBeingTracked = new HashSet<>();
	@Unique
	private Set<ChunkPos> tracky$lastChunksBeingTracked = new HashSet<>();

	@Override
	public void update() {
//		this.tracky$lastChunksBeingTracked.clear();
//		this.tracky$lastChunksBeingTracked.addAll(this.tracky$chunksBeingTracked);
		
		// large memory copies are slow, and allocation is nice to avoid
		Set<ChunkPos> s = tracky$lastChunksBeingTracked;
		s.clear();
		tracky$lastChunksBeingTracked = tracky$chunksBeingTracked;
		tracky$chunksBeingTracked = s;
	}

	@Override
	public Collection<ChunkPos> oldTrackedChunks() {
		return this.tracky$lastChunksBeingTracked;
	}

	@Override
	public Collection<ChunkPos> trackedChunks() {
		return this.tracky$chunksBeingTracked;
	}
}
