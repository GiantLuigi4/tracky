package com.tracky.access;

import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public interface ClientMapHolder {
	Map<UUID, Supplier<Iterable<ChunkPos>>> trackyHeldMapC();
}
