package com.tracky.api;

//import com.tracky.MixinPlugin;
import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public class ServerTracking {
	private static final HashMap<Level, HashMap<UUID, Supplier<Collection<ChunkPos>>>> forced = new HashMap();
	
	public static void forceChunks(Level level, Player player, Supplier<Collection<ChunkPos>> positions) {
//		if (!MixinPlugin.allowAPI)
//			throw new RuntimeException("Tracky API is not enabled; make sure you set " + MixinPlugin.class.getName() + "$allowAPI to true before initializing Tracky.");
		
		HashMap<UUID, Supplier<Collection<ChunkPos>>> forLevel = forced.get(level);
		if (forLevel == null) {
			forced.put(level, forLevel = new HashMap<>());
			HashMap<UUID, Supplier<Collection<ChunkPos>>> finalForLevel = forLevel;
			TrackyAccessor.getForcedChunks(level).put(Tracky.getDefaultUUID(), (aplayer) -> finalForLevel.getOrDefault(aplayer.getUUID(), Arrays::asList).get());
		}
		forLevel.put(player.getUUID(), positions);
	}
	
	public static void removeForceLoading(Level level, Player player) {
//		if (!MixinPlugin.allowAPI)
//			throw new RuntimeException("Tracky API is not enabled; make sure you set " + MixinPlugin.class.getName() + "$allowAPI to true before initializing Tracky.");
		
		HashMap<UUID, Supplier<Collection<ChunkPos>>> forLevel = forced.get(level);
		if (forLevel != null) {
			forLevel.remove(player.getUUID());
		}
	}
	
	public static void onUnloadLevel(LevelAccessor level) {
		if (level instanceof Level)
			forced.remove(level);
	}
	
	public static void onRemovePlayer(Player player) {
		for (HashMap<UUID, Supplier<Collection<ChunkPos>>> value : forced.values()) {
			value.remove(player.getUUID());
		}
	}
}
