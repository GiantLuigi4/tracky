package com.tracky;

import com.tracky.access.ClientMapHolder;
import com.tracky.access.ServerMapHolder;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackingSource;
import com.tracky.debug.ITrackChunks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class TrackyAccessor {

	/**
	 * <p>Client-only is a function as QOL.</p>
	 * <p>Render sources have a method for determining if they can render, so a supplier is unnecessary here.</p>
	 */
	public static Map<UUID, Supplier<Collection<TrackingSource>>> getTrackingSources(Level level) {
		return ((ServerMapHolder) level).trackyTrackingSources();
	}

	/**
	 * <p>Client-only is a function as QOL.</p>
	 * <p>Render sources have a method for determining if they can render, so a supplier is unnecessary here.</p>
	 */
	public static Map<UUID, Supplier<Collection<RenderSource>>> getRenderSources(ClientLevel level) {
		return ((ClientMapHolder) level).trackyRenderSources();
	}

	/**
	 * <p>Call this whenever you modify your list of tracked chunks.</p>
	 * <p>This tells tracky that the list has changed, and that new chunks likely have to be sent to the client, or that the client needs to know to drop chunks.</p>
	 */
	public static void markForRetracking(Player player) {
		((ITrackChunks) player).setDoUpdate(true);
	}
}
