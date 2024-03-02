package com.tracky.access;

import com.tracky.api.RenderSource;
import net.minecraft.core.SectionPos;

import java.util.Collection;

public interface ClientMapHolder {

//	Map<UUID, Supplier<Collection<SectionPos>>> trackyHeldMapC();

    Collection<SectionPos> trackyGetRenderChunksC();

//	void trackySetRenderChunksC(ClientLevel level);

    Collection<RenderSource> trackyRenderSources();
}
