package com.tracky.mixin.dummy;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the world class
 */
@Mixin(Level.class)
public class WorldMixin {
	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(WritableLevelData p_204149_, ResourceKey p_204150_, Holder p_204151_, Supplier p_204152_, boolean p_204153_, boolean p_204154_, long p_204155_, CallbackInfo ci) {
		ArrayList<ChunkPos> positions = new ArrayList<>();

		// all temp
		{
			ChunkPos startPos = new ChunkPos(new BlockPos(42, 0, 71));
			ChunkPos endPos = new ChunkPos(new BlockPos(-88, 0, -61));
			for (int x = endPos.x; x <= startPos.x; x++) {
				for (int z = endPos.z; z <= startPos.z; z++) {
					positions.add(new ChunkPos(x, z));
				}
			}
		}
		{
			ChunkPos startPos = new ChunkPos(new BlockPos(-297 - 200, 0, 296));
			ChunkPos endPos = new ChunkPos(new BlockPos(-456 - 200, 0, 328));
			for (int x = endPos.x; x <= startPos.x; x++) {
				for (int z = startPos.z; z <= endPos.z; z++) {
					positions.add(new ChunkPos(x, z));
				}
			}
		}



		TrackyAccessor.getForcedChunks((Level) (Object) this).put(Tracky.getDefaultUUID(), (player) -> {
			TrackyAccessor.getPlayersLoadingChunks((Level) (Object) this).put(Tracky.getDefaultUUID(), Arrays.asList(player));
			
			return new ArrayList<>() {{ add(new ChunkPos(5,5)); }};
		});
	}
}
