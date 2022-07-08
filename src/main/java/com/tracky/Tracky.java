package com.tracky;

import net.minecraftforge.fml.common.Mod;

import java.util.Random;
import java.util.UUID;

@Mod("tracky")
public class Tracky {
	private static UUID defaultUUID;
	
	public Tracky() {
//		FMLJavaModLoadingContext.get().getModEventBus();
//		MinecraftForge.EVENT_BUS;
		
		defaultUUID = new UUID(
				Tracky.class.toString().hashCode() * 9383064L,
				new Random(TrackyAccessor.class.toString().hashCode() + 32874).nextLong() * 10623261L
		);
		
		System.out.println("Tracky@" + Tracky.class + " is " + (MixinPlugin.isMainTracky ? "" : "not ") + " the main tracky instance.");
	}
	
	// a default UUID dependent upon the name which the class is shaded under
	public static UUID getDefaultUUID() {
		return defaultUUID;
	}
}
