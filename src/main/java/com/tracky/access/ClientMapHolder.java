package com.tracky.access;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public interface ClientMapHolder {
	Map<UUID, Supplier<Collection<SectionPos>>> trackyHeldMapC();
	Collection<SectionPos> trackyGetRenderChunksC();
	void trackySetRenderChunksC(Collection<SectionPos> positions);
}
