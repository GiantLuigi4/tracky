package com.tracky.api.event;

import com.tracky.api.TrackingSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

import java.util.Arrays;
import java.util.Collection;

public class RegisterTrackyTrackingSourceEvent extends Event {

    private final Level level;
    private final Collection<TrackingSource> trackingSources;

    public RegisterTrackyTrackingSourceEvent(Level level, Collection<TrackingSource> trackingSources) {
        this.level = level;
        this.trackingSources = trackingSources;
    }

    public Level getLevel() {
        return this.level;
    }

    public void register(TrackingSource... trackingSources) {
        this.trackingSources.addAll(Arrays.asList(trackingSources));
    }
}
