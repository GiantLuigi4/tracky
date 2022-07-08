package com.tracky;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class TrackyAccessor {
	/**
	 * this method gets generated at runtime
	 * it looks a little like "return world.fff5750a-0745-edaa-ffed-dd57fd3eb768 Tracky Forced;"
	 * now obviously, that cannot be compiled, and maybe less obviously, that field doesn't exist on the world class normally
	 *
	 * @param world the world to get the force loaded chunks of
	 * @return the list of force loaded chunks
	 */
	// also it's native so that it doesn't have a method body
	public static native Map<UUID, Function<PlayerEntity, Iterable<ChunkPos>>> getForcedChunks(World world);
}
