package com.tracky.mixin.dummy;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.access.ClientMapHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Supplier;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the shared constants class
 */
@Mixin(ClientLevel.class)
public class ClientWorldMixin implements ClientMapHolder {
    @Unique
    private Map<UUID, Supplier<Collection<ChunkPos>>> trackyRenderedChunks;
    
    @Inject(at = @At("TAIL"), method = "<init>")
    public void postInit(ClientPacketListener p_205505_, ClientLevel.ClientLevelData p_205506_, ResourceKey p_205507_, Holder p_205508_, int p_205509_, int p_205510_, Supplier p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, CallbackInfo ci) {
        trackyRenderedChunks = new HashMap<>();
    
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

        // all temp
        TrackyAccessor.getRenderedChunks((Level) (Object) this).put(Tracky.getDefaultUUID(), () -> {
            return positions;
        });
        TrackyAccessor.getRenderedChunks((Level) (Object) this).put(UUID.randomUUID(), () -> {
            return new ArrayList<>(){{add(new ChunkPos(5, 5));}};
        });
    }
    
    @Override
    public Map<UUID, Supplier<Collection<ChunkPos>>> trackyHeldMapC() {
        return trackyRenderedChunks;
    }
}
