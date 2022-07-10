package com.tracky;

import net.minecraftforge.fml.common.Mod;

import java.util.Map;
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
		
		SemanticVersion selfVersion = new SemanticVersion(MixinPlugin.VERSION);
		boolean isMain = false;
		int isFirstOfOwnAge = -1;
		Map<String, String> versions = TrackyAccessor.getTrackyVersions();
		// TODO: I'm only like 80% sure that this works
		for (String s : versions.keySet()) {
			String ver = versions.get(s);
			if (s.equals(MixinPlugin.class.toString()))
				if (isFirstOfOwnAge == -1)
					isFirstOfOwnAge = 1;
			SemanticVersion otherVersion = new SemanticVersion(ver);
			if (otherVersion.isSame(selfVersion)) {
				if (isFirstOfOwnAge == -1) {
					isMain = false;
					// TODO: check
//					isFirstOfOwnAge = 0;
					break; // probably not gonna be the main tracky
				} else if (isFirstOfOwnAge == 1) {
					isMain = true;
				}
			} else if (selfVersion.isNewer(otherVersion)) {
				isMain = isFirstOfOwnAge == 1 || isFirstOfOwnAge == -1;
//				isMain = true;
			} else {
				break; // clearly not gonna be the main instance, as it is not the newest
			}
		}
		MixinPlugin.isMainTracky = isMain;
		
		System.out.println("Tracky@" + (Tracky.class.toString().replace("class ", "")) + " is " + (MixinPlugin.isMainTracky ? "" : "not") + " the main tracky instance, running version " + MixinPlugin.VERSION + ".");
	}
	
	// a default UUID dependent upon the name which the class is shaded under
	public static UUID getDefaultUUID() {
		return defaultUUID;
	}
}
