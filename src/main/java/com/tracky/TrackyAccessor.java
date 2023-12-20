package com.tracky;

import com.tracky.access.ClientMapHolder;
import com.tracky.access.ServerMapHolder;
import com.tracky.api.RenderSource;
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
	 * Retrieves all force loaded chunks from the level.
	 *
	 * @param level the world to get the force loaded chunks of
	 * @return the map of force loaded chunks
	 */
	public static Map<UUID, Function<Player, Collection<ChunkPos>>> getForcedChunks(Level level) {
		return ((ServerMapHolder) level).trackyHeldMapS();
	}

	public static Map<UUID, List<Player>> getPlayersLoadingChunks(Level level) {
		return ((ServerMapHolder) level).trackyPlayerMap();
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

//	/**
//	 * <p>Call this whenever you modify your list of tracked chunks.</p>
//	 * <p>this tells tracky that the list has changed, and that new chunks likely have to be sent to the client, or that the client needs to know to drop chunks.</p>
//	 */
//	public static void markForRerender(ClientLevel lvl) {
//		((ClientMapHolder) lvl).trackySetRenderChunksC(lvl);
//	}
}
