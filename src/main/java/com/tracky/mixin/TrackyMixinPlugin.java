package com.tracky.mixin;

import com.tracky.Tracky;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TrackyMixinPlugin implements IMixinConfigPlugin {

	private boolean sodium;

	@Override
	public void onLoad(String mixinPackage) {
		this.sodium = FMLLoader.getLoadingModList().getModFileById("rubidium") != null;

		if (this.sodium) {
			Tracky.LOGGER.info("Using Rubidium Renderer");
		} else {
			Tracky.LOGGER.info("Using Vanilla Renderer");
		}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith("com.tracky.mixin.client.impl")) {
			return this.sodium ? mixinClassName.startsWith("com.tracky.mixin.client.impl.sodium") : mixinClassName.startsWith("com.tracky.mixin.client.impl.vanilla");
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
