package com.tracky;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class TrackyAccessor {
	/**
	 * this method gets generated at runtime
	 * it looks a little like "return level.fff5750a-0745-edaa-ffed-dd57fd3eb768 Tracky Forced;"
	 * now obviously, that cannot be compiled, and maybe less obviously, that field doesn't exist on the world class normally
	 *
	 * @param level the world to get the force loaded chunks of
	 * @return the list of force loaded chunks
	 */
	// also it's native so that it doesn't have a method body
	public static native Map<UUID, Function<Player, Iterable<ChunkPos>>> getForcedChunks(Level level);
	
	
	/**
	 * client only
	 * is a function as QOL
	 * <p>
	 * having it be a supplier allows it to change multiple times per frame without
	 * having the dev need to change any variables for every render
	 */
	public static native Map<UUID, Supplier<Iterable<ChunkPos>>> getRenderedChunks(Level level);
	
	public static boolean isMainTracky() {
		return MixinPlugin.isMainTracky;
	}
	
	public static native Map<String, String> getTrackyVersions();
}
