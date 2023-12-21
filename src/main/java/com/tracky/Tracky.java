package com.tracky;

import com.mojang.logging.LogUtils;
import com.tracky.api.RenderSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

@Mod("tracky")
public class Tracky {

	public static boolean ENABLE_TEST = false;
	public static final Logger LOGGER = LogUtils.getLogger();

	public Tracky() {
		if (ENABLE_TEST) {
			System.out.println("Default UUID Tests: \n" +
					"- " + getDefaultUUID("tracky", "sampleuuid") + "\n" +
					"- " + getDefaultUUID("tracky", "sampleUUID") + "\n" +
					"- " + getDefaultUUID("landlord", "worldshell")
			);
		}
	}

	/**
	 * Checks if the specified section position is part of a render source in the specified level.
	 *
	 * @param level The level to get sources from
	 * @param pos   The section position to check
	 * @return Whether any source is loading that position
	 */
	public static boolean sourceContains(ClientLevel level, SectionPos pos) {
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values()) {
			for (RenderSource renderSource : value.get()) {
				if (renderSource.containsSection(pos)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the specified section position is part of a render source in the specified level.
	 *
	 * @param level The level to get sources from
	 * @param pos   The chunk position to check
	 * @return Whether any source is loading that position
	 */
	public static boolean sourceContains(ClientLevel level, ChunkPos pos) {
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values()) {
			for (RenderSource renderSource : value.get()) {
				if (renderSource.containsChunk(pos)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * <p>A default UUID based on the name of the calling class.</p>
	 * <p>This is expensive, and the result should be cached and each mod should only have one UUID it uses</p>
	 */
	public static UUID getDefaultUUID(String modid, String name) {
		return new UUID(
				modid.hashCode() * 9383064L,
				new Random(name.hashCode() + 32874L * modid.length()).nextLong() * 10623261L
		);
	}
}
