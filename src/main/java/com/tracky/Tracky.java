package com.tracky;

import com.tracky.api.ClientTracking;
import com.tracky.api.ServerTracking;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;

@Mod("tracky")
public class Tracky {
	public Tracky() {
//		FMLJavaModLoadingContext.get().getModEventBus();
//		MinecraftForge.EVENT_BUS;

		MinecraftForge.EVENT_BUS.addListener(this::onUnloadWorld);
		MinecraftForge.EVENT_BUS.addListener(this::onRemovePlayer);
		System.out.println("Default UUID Tests: \n" +
				"- " + getDefaultUUID("tracky", "sampleuuid") + "\n" +
				"- " + getDefaultUUID("tracky", "sampleUUID") + "\n" +
				"- " + getDefaultUUID("landlord", "worldshell")
		);
	}
	
	public void onUnloadWorld(WorldEvent.Unload event) {
		ServerTracking.onUnloadLevel(event.getWorld());
		if (FMLEnvironment.dist.isClient())
			ClientTracking.onUnloadLevel(event.getWorld());
	}
	
	public void onRemovePlayer(PlayerEvent.PlayerLoggedOutEvent event) {
		ServerTracking.onRemovePlayer(event.getPlayer());
		if (FMLEnvironment.dist.isClient())
			ClientTracking.onRemovePlayer(event.getPlayer());
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

	public static ChunkPos sectionToChunk(SectionPos sectionPos) {
		return new ChunkPos(sectionPos.x(), sectionPos.z());
	}

	public static Set<ChunkPos> collapse(Collection<SectionPos> x) {
		Set<ChunkPos> y = new HashSet<>();

		for (SectionPos sectionPos : x) {
			ChunkPos c = sectionToChunk(sectionPos);

			y.add(c);
		}
		return y;
	}
}
