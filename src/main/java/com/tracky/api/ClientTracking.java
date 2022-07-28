package com.tracky.api;

import com.tracky.MixinPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashMap;
import java.util.function.Supplier;

public class ClientTracking {
	private static final HashMap<ClientLevel, Supplier<Iterable<ChunkPos>>> forced = new HashMap();
	
	public static void forceChunks(ClientLevel level, Supplier<Iterable<ChunkPos>> positions) {
		if (!MixinPlugin.allowAPI)
			throw new RuntimeException("Tracky API is not enabled; make sure you set " + MixinPlugin.class.getName() + "$allowAPI to true before initializing Tracky.");
		
		forced.put(level, positions);
	}
	
	public static void removeForceLoading(ClientLevel level) {
		if (!MixinPlugin.allowAPI)
			throw new RuntimeException("Tracky API is not enabled; make sure you set " + MixinPlugin.class.getName() + "$allowAPI to true before initializing Tracky.");
		
		forced.remove(level);
	}
	
	public static void onUnloadLevel(LevelAccessor level) {
		if (level instanceof ClientLevel)
			forced.remove(level);
	}
	
	// TODO: is this right?
	public static void onRemovePlayer(Player player) {
		if (player == Minecraft.getInstance().player) {
			forced.clear();
		}
	}
}
