package com.tracky;

import com.mojang.logging.LogUtils;
import com.tracky.api.ClientTracking;
import com.tracky.api.RenderSource;
import com.tracky.api.ServerTracking;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

@Mod("tracky")
public class Tracky {

	public static final boolean ENABLE_TEST = true;
	public static final Logger LOGGER = LogUtils.getLogger();

	public Tracky() {
//		FMLJavaModLoadingContext.get().getModEventBus();
//		MinecraftForge.EVENT_BUS;

		MinecraftForge.EVENT_BUS.addListener(this::onUnloadWorld);
		MinecraftForge.EVENT_BUS.addListener(this::onRemovePlayer);

		if (ENABLE_TEST) {
			System.out.println("Default UUID Tests: \n" +
					"- " + getDefaultUUID("tracky", "sampleuuid") + "\n" +
					"- " + getDefaultUUID("tracky", "sampleUUID") + "\n" +
					"- " + getDefaultUUID("landlord", "worldshell")
			);
		}
	}
	
	public static boolean sourceContains(Level level, SectionPos pos) {
		for (Supplier<Collection<RenderSource>> value : TrackyAccessor.getRenderSources(level).values())
			for (RenderSource renderSource : value.get())
				if (renderSource.containsSection(pos))
					return true;
		
		return false;
	}
	
	public void onUnloadWorld(LevelEvent.Unload event) {
		ServerTracking.onUnloadLevel(event.getLevel());
		if (FMLEnvironment.dist.isClient())
			ClientTracking.onUnloadLevel(event.getLevel());
	}
	
	public void onRemovePlayer(PlayerEvent.PlayerLoggedOutEvent event) {
		ServerTracking.onRemovePlayer(event.getEntity());
		if (FMLEnvironment.dist.isClient())
			ClientTracking.onRemovePlayer(event.getEntity());
	}
	
	/*
	 * a default UUID based on the name of the  calling class
	 * this is expensive, and the result should be cached and each mod should only have one UUID it uses
	*/
	public static UUID getDefaultUUID(String modid, String name) {
		return new UUID(
				modid.hashCode() * 9383064L,
				new Random(name.hashCode() + 32874L * modid.length()).nextLong() * 10623261L
		);
	}
}
