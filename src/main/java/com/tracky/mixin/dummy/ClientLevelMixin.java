package com.tracky.mixin.dummy;

import com.tracky.access.ClientMapHolder;
import com.tracky.api.RenderSource;
import com.tracky.api.event.RegisterTrackyRenderSourceEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public class ClientLevelMixin implements ClientMapHolder {

    @Unique
    private List<RenderSource> tracky$trackyRenderSources;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void postInit(ClientPacketListener p_205505_, ClientLevel.ClientLevelData p_205506_, ResourceKey p_205507_, Holder p_205508_, int p_205509_, int p_205510_, Supplier p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, CallbackInfo ci) {
        this.tracky$trackyRenderSources = new ArrayList<>();
        RegisterTrackyRenderSourceEvent event = new RegisterTrackyRenderSourceEvent((ClientLevel) (Object) this, this.tracky$trackyRenderSources);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public List<RenderSource> trackyRenderSources() {
        return this.tracky$trackyRenderSources;
    }
}
