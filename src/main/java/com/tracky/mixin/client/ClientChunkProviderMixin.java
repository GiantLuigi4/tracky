package com.tracky.mixin.client;

import com.tracky.debug.IChunkProviderAttachments;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.lighting.WorldLightManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

// TODO: we can look into making this less invasive later on, but for now, this should do
@Mixin(ClientChunkProvider.class)
public abstract class ClientChunkProviderMixin implements IChunkProviderAttachments {
	@Shadow
	@Final
	private ClientWorld world;
	@Shadow
	@Final
	private static Logger LOGGER;
	
	@Shadow
	public abstract WorldLightManager getLightManager();
	
	@Unique
	HashMap<ChunkPos, Chunk> chunks = new HashMap<>();
	
	@Inject(at = @At("HEAD"), method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", cancellable = true)
	public void preGetChunk0(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load, CallbackInfoReturnable<Chunk> cir) {
		Chunk chunk = getChunk(new ChunkPos(chunkX, chunkZ));
		if (chunk != null) cir.setReturnValue(chunk);
	}
	
	@Inject(at = @At("HEAD"), method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/IChunk;", cancellable = true)
	public void preGetChunk1(int pChunkX, int pChunkZ, ChunkStatus pRequiredStatus, boolean pLoad, CallbackInfoReturnable<Chunk> cir) {
		Chunk chunk = getChunk(new ChunkPos(pChunkX, pChunkZ));
		if (chunk != null) cir.setReturnValue(chunk);
	}
	
	@Inject(at = @At("HEAD"), method = "unloadChunk", cancellable = true)
	public void preDropChunk(int pX, int pZ, CallbackInfo ci) {
		Chunk chunk = chunks.remove(new ChunkPos(pX, pZ));
		if (chunk != null) {
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload(chunk));
			this.world.onChunkUnloaded(chunk);
			ci.cancel();
		}
	}
	
	@Inject(at = @At("HEAD"), method = "loadChunk", cancellable = true)
	public void preReplaceWithPacket(int pX, int pZ, BiomeContainer biomeContainerIn, PacketBuffer packetIn, CompoundNBT nbtTagIn, int sizeIn, boolean fullChunk, CallbackInfoReturnable<Chunk> cir) {
		ChunkPos pos = new ChunkPos(pX, pZ);
		Chunk chunk = getChunk(pos);
		if (biomeContainerIn == null) {
			LOGGER.warn("Ignoring chunk since we don't have complete data: {}, {}", pX, pZ);
			cir.setReturnValue(null);
			return;
		}
		
		boolean wasPresent;
		if (!(wasPresent = !(chunk == null)))
			chunk = new Chunk(this.world, pos, biomeContainerIn);
		chunk.read(biomeContainerIn, packetIn, nbtTagIn, sizeIn);
		if (!wasPresent)
			this.chunks.put(pos, chunk);
		else this.chunks.replace(pos, chunk);
		
		ChunkSection[] achunksection = chunk.getSections();
		WorldLightManager worldlightmanager = this.getLightManager();
		worldlightmanager.enableLightSources(pos, true);
		
		for (int j = 0; j < achunksection.length; ++j) {
			ChunkSection chunksection = achunksection[j];
			worldlightmanager.updateSectionStatus(SectionPos.of(pX, j, pZ), ChunkSection.isEmpty(chunksection));
		}
		
		this.world.onChunkLoaded(pX, pZ);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
		cir.setReturnValue(chunk);
	}
	
	@Unique
	public Chunk getChunk(ChunkPos pos) {
		return chunks.getOrDefault(pos, null);
	}

//	@Override
//	public Chunk[] forcedChunks() {
//		return chunks.values().toArray(new Chunk[0]);
//	}

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
