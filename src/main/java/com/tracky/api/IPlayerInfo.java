package com.tracky.api;

import com.tracky.impl.ITrackChunks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public interface IPlayerInfo extends ITrackChunks {
	static IPlayerInfo of(ServerPlayer player) {
		return new IPlayerInfo() {
			@Override
			public void update() {
				((ITrackChunks) player).update();
			}
			
			@Override
			public Collection<ChunkPos> trackedChunks() {
				return ((ITrackChunks) player).trackedChunks();
			}
			
			@Override
			public Collection<ChunkPos> oldTrackedChunks() {
				return ((ITrackChunks) player).oldTrackedChunks();
			}
			
			@Override
			public Vec3 getPosition() {
				return player.getPosition(1);
			}
			
			@Override
			public Vec2 getLook() {
				return player.getRotationVector();
			}
			
			@Override
			public ServerPlayer getPlayer() {
				return player;
			}
		};
	}
	
	Vec3 getPosition();
	
	Vec2 getLook();
	
	ServerPlayer getPlayer();
}
