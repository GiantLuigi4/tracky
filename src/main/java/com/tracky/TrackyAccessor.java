package com.tracky;

import com.tracky.access.ClientMapHolder;
import com.tracky.access.ServerMapHolder;
import com.tracky.api.RenderSource;
import com.tracky.debug.ITrackChunks;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class TrackyAccessor {
	/**
	 * this method gets generated at runtime
	 * it looks a little like "return level.fff5750a-0745-edaa-ffed-dd57fd3eb768 Tracky Forced;"
	 * now obviously, that cannot be compiled, and maybe less obviously, that field doesn't exist on the world class normally
	 *
	 * @param level the world to get the force loaded chunks of
	 * @return the list of force loaded chunks
	 */
	public static Map<UUID, Function<Player, Collection<ChunkPos>>> getForcedChunks(Level level) {
		return ((ServerMapHolder) level).trackyHeldMapS();
	}

	/**
	 * client only
	 * is a function as QOL
	 * <p>
	 * having it be a supplier allows it to change multiple times per frame without
	 * having the dev need to change any variables for every render
	 */
	public static Map<UUID, Supplier<Collection<SectionPos>>> getRenderedChunks(Level level) {
		return ((ClientMapHolder) level).trackyHeldMapC();
	}

	/**
	 * client only
	 * is a function as QOL
	 * <p>
	 * render sources have a method for determining if they can render, so a supplier is unnecessary here
	 */
	public static Map<UUID, Supplier<Collection<RenderSource>>> getRenderSources(Level level) {
		return ((ClientMapHolder) level).trackyRenderSources();
	}
	
	public static Map<UUID, List<Player>> getPlayersLoadingChunks(Level level) {
		return ((ServerMapHolder) level).trackyPlayerMap();
	}
	
	/**
	 * call this whenever you modify your list of tracked chunks
	 * this tells tracky that the list has changed, and that new chunks likely have to be sent to the client, or that the client needs to know to drop chunks
	 */
	public static void markForRetracking(Player player) {
		((ITrackChunks) player).setDoUpdate(true);
	}
	
	/**
	 * call this whenever you modify your list of tracked chunks
	 * this tells tracky that the list has changed, and that new chunks likely have to be sent to the client, or that the client needs to know to drop chunks
	 */
	public static void markForRerender(Level lvl) {
		// TODO: might be a good idea to offload this onto a second thread?
		// creates a new set to avoid needing synchronization
		Set<SectionPos> positions = new HashSet<>();
		for (Supplier<Collection<SectionPos>> value : ((ClientMapHolder) lvl).trackyHeldMapC().values())
			positions.addAll(value.get());
		((ClientMapHolder) lvl).trackySetRenderChunksC(positions);
	}
}
