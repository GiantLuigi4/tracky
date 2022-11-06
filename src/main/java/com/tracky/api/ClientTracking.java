package com.tracky.api;

//import com.tracky.MixinPlugin;
import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public class ClientTracking {
	private static final HashMap<ClientLevel, Supplier<Collection<SectionPos>>> forced = new HashMap();
	private static final UUID uuid = Tracky.getDefaultUUID("tracky", "api:client");
	
	public static void forceChunks(ClientLevel level, Supplier<Collection<SectionPos>> positions) {
//		if (!MixinPlugin.allowAPI)
//			throw new RuntimeException("Tracky API is not enabled; make sure you set " + MixinPlugin.class.getName() + "$allowAPI to true before initializing Tracky.");
		
		TrackyAccessor.getRenderedChunks(level).put(uuid, ()->{
			return forced.get(level).get();
		});
		forced.put(level, positions);
	}
	
	public static void removeForceLoading(ClientLevel level) {
//		if (!MixinPlugin.allowAPI)
//			throw new RuntimeException("Tracky API is not enabled; make sure you set " + MixinPlugin.class.getName() + "$allowAPI to true before initializing Tracky.");
		
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
