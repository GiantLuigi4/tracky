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
	private final Set<ChunkPos> tracky$chunksBeingTracked = new HashSet<>();
	@Unique
	private Set<ChunkPos> tracky$lastChunksBeingTracked = new HashSet<>();

	@Override
	public void update() {
		// interestingly, this does perform better than swapping the lists?
		// not actually sure how
		// might wanna check if this is also the case for large tracking sources?
		// personally, I'd expect that large addAll operations would get slow
		this.tracky$lastChunksBeingTracked.clear();
		this.tracky$lastChunksBeingTracked.addAll(this.tracky$chunksBeingTracked);
		this.tracky$chunksBeingTracked.clear();
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
