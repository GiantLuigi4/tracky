package com.tracky.mixin.client;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkProviderMixin implements IChunkProviderAttachments {

	@Shadow
	@Final
	ClientLevel level;

	@Shadow
	volatile ClientChunkCache.Storage storage;

	@Unique
	private final Map<ChunkPos, LevelChunk> tracky$chunks = new HashMap<>();
	@Unique
	private final Map<ChunkPos, Long> tracky$lastUpdates = new ConcurrentHashMap<>();

	@Inject(at = @At("RETURN"), method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", cancellable = true)
	public void preGetChunk0(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load, CallbackInfoReturnable<LevelChunk> cir) {
		LevelChunk chunk = this.tracky$getChunk(new ChunkPos(chunkX, chunkZ));
		if (chunk != null) {
			cir.setReturnValue(chunk);
		}
	}

	@Inject(at = @At("RETURN"), method = "drop")
	public void preDropChunk(int pX, int pZ, CallbackInfo ci) {
		LevelChunk chunk = this.tracky$chunks.remove(new ChunkPos(pX, pZ));
		if (chunk != null) {
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk));
			this.level.unload(chunk);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", shift = At.Shift.BEFORE), method = "replaceWithPacketData", cancellable = true)
	public void preReplaceWithPacket(int pX, int pZ, FriendlyByteBuf pBuffer, CompoundTag pTag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pConsumer, CallbackInfoReturnable<LevelChunk> cir) {
		ChunkPos pos = new ChunkPos(pX, pZ);
		LevelChunk chunk = tracky$getChunk(pos);

		boolean wasPresent = chunk != null;
		if (!wasPresent) {
			chunk = new LevelChunk(this.level, pos);
		}

		chunk.replaceWithPacketData(pBuffer, pTag, pConsumer);
		this.tracky$chunks.put(pos, chunk);
		if (wasPresent) {
			LevelChunk chunk1 = this.tracky$chunks.get(pos);
			if (chunk1 != null && chunk1 != chunk) {
				this.level.unload(chunk1);
			}
		}

		this.level.onChunkLoaded(pos);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));
		cir.setReturnValue(chunk);
	}

	@Inject(at = @At("TAIL"), method = "replaceWithPacketData")
	public void trackChunk(int pX, int pZ, FriendlyByteBuf pBuffer, CompoundTag pTag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pConsumer, CallbackInfoReturnable<LevelChunk> cir) {
		LevelChunk chunk = cir.getReturnValue();
		this.tracky$chunks.putIfAbsent(new ChunkPos(pX, pZ), chunk);
	}

	@Unique
	public LevelChunk tracky$getChunk(ChunkPos pos) {
		return this.tracky$chunks.get(pos);
	}

	@Override
	public LevelChunk[] forcedChunks() {
		return this.tracky$chunks.values().toArray(new LevelChunk[0]);
	}

	@Override
	public void setUpdated(int x, int z) {
		this.tracky$lastUpdates.put(new ChunkPos(x, z), System.currentTimeMillis());
	}

	@Override
	public long getLastUpdate(LevelChunk chunk) {
		return this.tracky$lastUpdates.getOrDefault(chunk.getPos(), 0L);
	}

	@Inject(at = @At("HEAD"), method = "updateViewRadius")
	public void onUpdateViewDegrees /* GiantLuigi4: sometimes I name things weirdly like this */(int pViewDistance, CallbackInfo ci) {
		if (pViewDistance != ((ChunkStorageAccessor) (Object) this.storage).getChunkRadius()) {
			ArrayList<ChunkPos> toRemove = new ArrayList<>();
			loopChunks:
			for (ChunkPos chunkPos : tracky$chunks.keySet()) {
				LevelChunk levelchunk = tracky$getChunk(chunkPos);
				if (levelchunk != null) {
					LocalPlayer player = Minecraft.getInstance().player;
					if (chunkPos.getChessboardDistance(player.chunkPosition()) < pViewDistance) {
						continue;
					}

					for (Supplier<Collection<SectionPos>> value : TrackyAccessor.getRenderedChunks(this.level).values()) {
						for (SectionPos sectionPos : value.get()) {
							if (sectionPos.x() == chunkPos.x && sectionPos.z() == chunkPos.x) {
								continue loopChunks;
							}
						}
					}

					for (Function<Player, Collection<SectionPos>> value : TrackyAccessor.getForcedChunks(levelchunk.getLevel()).values()) {
						if (Tracky.collapse(value.apply(player)).contains(chunkPos)) {
							continue loopChunks;
						}
					}
				}
				toRemove.add(chunkPos);
			}
			toRemove.forEach(this.tracky$chunks::remove);
		}
	}

	@Inject(at = @At("HEAD"), method = "tick")
	public void preTick(BooleanSupplier hasTimeLeft, boolean tickChunks, CallbackInfo ci) {
		this.tracky$lastUpdates.keySet().removeIf(chunkPos -> !this.tracky$chunks.containsKey(chunkPos));
	}
}
