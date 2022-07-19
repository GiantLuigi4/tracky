package com.tracky.mixin.client;

import com.tracky.TrackyAccessor;
import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

// TODO: we can look into making this less invasive later on, but for now, this should do
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkProviderMixin implements IChunkProviderAttachments {
	@Shadow
	@Final
	ClientLevel level;
	
	@Shadow
	public abstract LevelLightEngine getLightEngine();
	
	@Unique
	HashMap<ChunkPos, LevelChunk> chunks = new HashMap<>();
	
	@Inject(at = @At("HEAD"), method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", cancellable = true)
	public void preGetChunk0(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load, CallbackInfoReturnable<LevelChunk> cir) {
		if (!TrackyAccessor.isMainTracky()) return;
		LevelChunk chunk = getChunk(new ChunkPos(chunkX, chunkZ));
		if (chunk != null) cir.setReturnValue(chunk);
	}
	
	@Inject(at = @At("HEAD"), method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", cancellable = true)
	public void preGetChunk1(int pChunkX, int pChunkZ, ChunkStatus pRequiredStatus, boolean pLoad, CallbackInfoReturnable<LevelChunk> cir) {
		if (!TrackyAccessor.isMainTracky()) return;
		LevelChunk chunk = getChunk(new ChunkPos(pChunkX, pChunkZ));
		if (chunk != null) cir.setReturnValue(chunk);
	}
	
	@Inject(at = @At("HEAD"), method = "drop", cancellable = true)
	public void preDropChunk(int pX, int pZ, CallbackInfo ci) {
		if (!TrackyAccessor.isMainTracky()) return;
		LevelChunk chunk = chunks.remove(new ChunkPos(pX, pZ));
		if (chunk != null) {
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload(chunk));
			this.level.unload(chunk);
			ci.cancel();
		}
	}
	
	@Inject(at = @At("HEAD"), method = "replaceWithPacketData", cancellable = true)
	public void preReplaceWithPacket(int pX, int pZ, FriendlyByteBuf pBuffer, CompoundTag pTag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pConsumer, CallbackInfoReturnable<LevelChunk> cir) {
		if (!TrackyAccessor.isMainTracky()) return;
		ChunkPos pos = new ChunkPos(pX, pZ);
		LevelChunk chunk = getChunk(pos);
		
		boolean wasPresent;
		if (!(wasPresent = !(chunk == null)))
			chunk = new LevelChunk(this.level, pos);
		chunk.replaceWithPacketData(pBuffer, pTag, pConsumer);
		if (!wasPresent)
			this.chunks.put(pos, chunk);
		else {
			LevelChunk chunk1 = this.chunks.get(pos);
			if (chunk1 != null)
				if (chunk1 != chunk)
					level.unload(chunk1);
			this.chunks.replace(pos, chunk);
		}
		
		this.level.onChunkLoaded(pos);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
		cir.setReturnValue(chunk);
	}
	
	@Unique
	public LevelChunk getChunk(ChunkPos pos) {
		return chunks.getOrDefault(pos, null);
	}
	
	@Override
	public LevelChunk[] forcedChunks() {
		return chunks.values().toArray(new LevelChunk[0]);
	}
	
	HashMap<ChunkPos, Long> lastUpdates = new HashMap<>();
	
	@Override
	public void setUpdated(int x, int z) {
		ChunkPos pos = new ChunkPos(x, z);

		synchronized (lastUpdates) {
			if (lastUpdates.containsKey(pos))
				lastUpdates.replace(pos, System.currentTimeMillis());
			else lastUpdates.put(pos, System.currentTimeMillis());
		}
	}
	
	@Override
	public long getLastUpdate(LevelChunk chunk) {
		return lastUpdates.getOrDefault(chunk.getPos(), 0L);
	}
	
	@Inject(at = @At("HEAD"), method = "tick")
	public void preTick(BooleanSupplier p_202421_, boolean p_202422_, CallbackInfo ci) {
		synchronized (lastUpdates) {
			ArrayList<ChunkPos> toRemove = new ArrayList<>();

			for (ChunkPos chunkPos : lastUpdates.keySet())
				if (!chunks.containsKey(chunkPos))
					toRemove.add(chunkPos);

			for (ChunkPos chunkPos : toRemove)
				lastUpdates.remove(chunkPos);
		}
	}
	
	//	@Override
//	public Chunk[] regularChunks() {
//		// AT did weird
//		AtomicReferenceArray<Chunk> chunksArray = ((StorageAccessor) (Object) storage).chunks();
//		Chunk[] chunks = new Chunk[chunksArray.length()];
//		for (int i = 0; i < chunks.length; i++)
//			chunks[i] = chunksArray.get(i);
//		return chunks;
//	}
}
