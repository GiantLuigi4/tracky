package com.tracky.mixin.client;

import com.tracky.Tracky;
import com.tracky.TrackyAccessor;
import com.tracky.api.TrackingSource;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

	@Shadow
	@Final
	private LevelChunk emptyChunk;

	@Unique
	private final Map<ChunkPos, LevelChunk> tracky$chunks = new HashMap<>();
	@Unique
	private final Map<ChunkPos, Long> tracky$lastUpdates = new ConcurrentHashMap<>();
	@Unique
	private final Set<ChunkPos> tracky$forcedChunks = new HashSet<>();
	@Unique
	private boolean tracky$suppressEvent;

	@Unique
	private void tracky$onRemove(LevelChunk chunk) {
		this.level.unload(chunk);
		this.tracky$forcedChunks.remove(chunk.getPos());
		this.tracky$lastUpdates.remove(chunk.getPos());
	}

	@Inject(at = @At("RETURN"), method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", cancellable = true)
	public void preGetChunk0(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load, CallbackInfoReturnable<LevelChunk> cir) {
		if (cir.getReturnValue() != null && cir.getReturnValue() != this.emptyChunk) {
			return;
		}

		LevelChunk chunk = this.tracky$getChunk(new ChunkPos(chunkX, chunkZ));
		if (chunk != null) {
			cir.setReturnValue(chunk);
		}
	}

	@Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;replace(ILnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/world/level/chunk/LevelChunk;"))
	public void a(int pX, int pZ, CallbackInfo ci) {
		this.tracky$suppressEvent = true;
	}

	@Inject(at = @At("TAIL"), method = "drop")
	public void preDropChunk(int pX, int pZ, CallbackInfo ci) {
		ChunkPos pos = new ChunkPos(pX, pZ);
		LevelChunk chunk = this.tracky$chunks.remove(pos);
		if (chunk != null) {
			if (!this.tracky$suppressEvent) {
				// We don't want this event to be fired twice
				MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk));
			}
			this.tracky$onRemove(chunk);
		}
		this.tracky$suppressEvent = false;
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
		MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));
		this.tracky$forcedChunks.add(pos);
		cir.setReturnValue(chunk);
	}

	@Inject(at = @At("TAIL"), method = "replaceWithPacketData")
	public void trackChunk(int pX, int pZ, FriendlyByteBuf pBuffer, CompoundTag pTag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pConsumer, CallbackInfoReturnable<LevelChunk> cir) {
		this.tracky$chunks.putIfAbsent(new ChunkPos(pX, pZ), cir.getReturnValue());
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

	@Override
	public boolean isTrackyForced(LevelChunk chunk) {
		return this.tracky$forcedChunks.contains(chunk.getPos());
	}

	// couldn't figure out how to reproduce the vanilla behavior, so I just copied this from the server
	@Unique
	private static boolean isChunkInRange(int chunkX, int chunkZ, int playerX, int playerZ, int pMaxDistance) {
		int xOff = Math.max(0, Math.abs(chunkX - playerX) - 1);
		int zOff = Math.max(0, Math.abs(chunkZ - playerZ) - 1);
		long d0 = Math.max(0, Math.max(xOff, zOff) - 1);
		long d1 = Math.min(xOff, zOff);
		return (d1 * d1 + d0 * d0) < ((long) pMaxDistance * pMaxDistance);
	}

	@Inject(at = @At("HEAD"), method = "updateViewRadius")
	public void onUpdateViewDegrees /* GiantLuigi4: sometimes I name things weirdly like this */(int pViewDistance, CallbackInfo ci) {
		if (pViewDistance != ((ChunkStorageAccessor) (Object) this.storage).getChunkRadius()) {
			Iterator<ChunkPos> iterator = this.tracky$chunks.keySet().iterator();

			LocalPlayer player = Minecraft.getInstance().player;
			int sectionX = SectionPos.blockToSectionCoord(player.blockPosition().getX());
			int sectionZ = SectionPos.blockToSectionCoord(player.blockPosition().getZ());

			loopChunks:
			while (iterator.hasNext()) {
				ChunkPos chunkPos = iterator.next();
				LevelChunk levelchunk = tracky$getChunk(chunkPos);
				if (levelchunk != null) {

					if (isChunkInRange(
							levelchunk.getPos().x, levelchunk.getPos().z,
							sectionX, sectionZ,
							pViewDistance
					)) {
						continue;
					}

					if (Tracky.sourceContains(level, chunkPos))
						continue;

					// TODO: actually might be unecessary?
					//		 need to check this with a mod like dynamic portals
					// 		 Tracky#sourceContains sorta invalidates this check, but this might have to be a bit more precise, so I'm leaving this in comment code until I can test this a bit better
//					for (Supplier<Collection<TrackingSource>> value : TrackyAccessor.getTrackingSources(level).values()) {
//						for (TrackingSource trackingSource : value.get()) {
//							// assumption: chunk render dist will be more computationally expensive than containsChunk when implemented
//							if (trackingSource.containsChunk(chunkPos)) {
//								if (trackingSource.checkRenderDist(player, chunkPos))
//									continue loopChunks;
//							}
//						}
//					}

					this.tracky$onRemove(levelchunk);
				}
				iterator.remove();
			}
		}
	}
}
