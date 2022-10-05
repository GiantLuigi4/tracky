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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

@Mod("tracky")
public class Tracky {
	public Tracky() {
//		FMLJavaModLoadingContext.get().getModEventBus();
//		MinecraftForge.EVENT_BUS;

		MinecraftForge.EVENT_BUS.addListener(this::onUnloadWorld);
		MinecraftForge.EVENT_BUS.addListener(this::onRemovePlayer);
		System.out.println("Default UUID Test: " + getDefaultUUID());
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
	public static UUID getDefaultUUID() {
//		return defaultUUID;
		StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
		return new UUID(
				traceElements[2].toString().hashCode() * 9383064L,
				new Random(traceElements[2].toString().hashCode() + 32874).nextLong() * 10623261L
		);
	}

	public static ChunkPos sectionToChunk(SectionPos sectionPos) {
		return new ChunkPos(sectionPos.x(), sectionPos.z());
	}

	public static ArrayList<ChunkPos> collapse(Collection<SectionPos> x) {
		ArrayList<ChunkPos> y = new ArrayList<>();

		for (SectionPos sectionPos : x) {
			ChunkPos c = sectionToChunk(sectionPos);

			if(!y.contains(c))
				y.add(c);
		}
		return y;
	}
}
