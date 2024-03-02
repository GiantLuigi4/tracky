package com.tracky;

import com.tracky.access.ClientMapHolder;
import com.tracky.access.ServerMapHolder;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackingSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

import java.util.Collection;

public class TrackyAccessor {

    /**
     * Common function for QOL.
     */
    public static Collection<TrackingSource> getTrackingSources(Level level) {
        return ((ServerMapHolder) level).trackyTrackingSources();
    }

    /**
     * Client-only function for QOL.
     */
    public static Collection<RenderSource> getRenderSources(ClientLevel level) {
        return ((ClientMapHolder) level).trackyRenderSources();
    }
}
