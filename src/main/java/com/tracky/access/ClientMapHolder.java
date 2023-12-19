package com.tracky.access;

import com.tracky.api.RenderSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public interface ClientMapHolder {

//	Map<UUID, Supplier<Collection<SectionPos>>> trackyHeldMapC();

	Collection<SectionPos> trackyGetRenderChunksC();

//	void trackySetRenderChunksC(ClientLevel level);

	Map<UUID, Supplier<Collection<RenderSource>>> trackyRenderSources();
}
