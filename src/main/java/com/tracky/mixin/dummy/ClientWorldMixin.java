package com.tracky.mixin.dummy;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.access.ClientMapHolder;
import com.tracky.api.RenderSource;
import com.tracky.debug.TestSource;
import com.tracky.util.MapWrapper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Supplier;

@Mixin(value = ClientLevel.class, priority = 500) // This needs to apply early so other mods can add things in
public class ClientWorldMixin implements ClientMapHolder {

	@Unique
	private final Map<UUID, Supplier<Collection<RenderSource>>> tracky$trackyRenderSources = new MapWrapper<>(new HashMap<>());
	@Unique
	private final Set<SectionPos> tracky$sectionPosSet = new HashSet<>();

	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(ClientPacketListener p_205505_, ClientLevel.ClientLevelData p_205506_, ResourceKey p_205507_, Holder p_205508_, int p_205509_, int p_205510_, Supplier p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, CallbackInfo ci) {
		if (!Tracky.ENABLE_TEST)
			return;

		// new
		TestSource source = new TestSource();
		TrackyAccessor.getRenderSources(((ClientLevel) (Object) this)).put(
				Tracky.getDefaultUUID("tracky", "testing"),
				() -> List.of(source)
		);
	}

	@Override
	public Collection<SectionPos> trackyGetRenderChunksC() {
		return this.tracky$sectionPosSet;
	}

	@Override
	public Map<UUID, Supplier<Collection<RenderSource>>> trackyRenderSources() {
		return this.tracky$trackyRenderSources;
	}
}
