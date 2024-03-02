package com.tracky.api.event;

import com.tracky.api.RenderSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

import java.util.Arrays;
import java.util.Collection;

public class RegisterTrackyRenderSourceEvent extends Event {

    private final Level level;
    private final Collection<RenderSource> renderSources;

    public RegisterTrackyRenderSourceEvent(Level level, Collection<RenderSource> renderSources) {
        this.level = level;
        this.renderSources = renderSources;
    }

    public Level getLevel() {
        return this.level;
    }

    public void register(RenderSource... renderSources) {
        this.renderSources.addAll(Arrays.asList(renderSources));
    }
}
