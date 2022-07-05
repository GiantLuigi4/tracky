package com.tracky;

import net.minecraftforge.fml.common.Mod;

@Mod("tracky")
public class Tracky {
	public Tracky() {
//		FMLJavaModLoadingContext.get().getModEventBus();
//		MinecraftForge.EVENT_BUS;
		TrackyAccessor.getForcedChunks(null);
	}
}
