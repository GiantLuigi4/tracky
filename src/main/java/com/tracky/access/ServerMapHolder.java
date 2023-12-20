package com.tracky.access;

import com.tracky.api.RenderSource;
import com.tracky.api.TrackingSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ServerMapHolder {
    Map<UUID, Supplier<Collection<TrackingSource>>> trackyTrackingSources();
}
