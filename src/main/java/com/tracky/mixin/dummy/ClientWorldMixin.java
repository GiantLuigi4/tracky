package com.tracky.mixin.dummy;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.access.ClientMapHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public class ClientWorldMixin implements ClientMapHolder {
	@Unique
	private Map<UUID, Supplier<Collection<SectionPos>>> trackyRenderedChunks;
	
	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(ClientPacketListener p_205505_, ClientLevel.ClientLevelData p_205506_, ResourceKey p_205507_, Holder p_205508_, int p_205509_, int p_205510_, Supplier p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, CallbackInfo ci) {
		trackyRenderedChunks = new HashMap<>();
//
		ArrayList<SectionPos> positions = new ArrayList<>();

		// all temp
//		{
//			SectionPos startPos = SectionPos.of(new BlockPos(-297 - 200, -63, 296));
//			SectionPos endPos = SectionPos.of(new BlockPos(-456 - 200, 319, 328));
//			for (int x = endPos.getX(); x <= startPos.getX(); x++) {
//				for (int y = startPos.getY(); y <= endPos.getY(); y++) {
//					for (int z = startPos.getZ(); z <= endPos.getZ(); z++) {
//						positions.add(SectionPos.of(x, y, z));
//					}
//				}
//			}
//		}
////
////		// all temp
//		TrackyAccessor.getRenderedChunks((Level) (Object) this).put(Tracky.getDefaultUUID("tracky", "testing"), () -> {
//			return positions;
//		});
//		TrackyAccessor.getRenderedChunks((Level) (Object) this).put(UUID.randomUUID(), () -> {
//			return new ArrayList<>() {{
//				for (int i = -2; i < 5; i++) {
//					add(SectionPos.of(5, i, 5));
//				}
//			}};
//		});
	}
	
	@Override
	public Map<UUID, Supplier<Collection<SectionPos>>> trackyHeldMapC() {
		return trackyRenderedChunks;
	}
}
