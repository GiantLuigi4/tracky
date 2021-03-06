package com.tracky.mixin;

import com.mojang.authlib.GameProfile;
import com.tracky.debug.ITrackChunks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ITrackChunks {
	@Unique
	boolean doUpdate = false;
	@Unique
	private ArrayList<ChunkPos> chunksBeingTracked;
	@Unique
	private ArrayList<ChunkPos> lastChunksBeingTracked;
	
	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(MinecraftServer p_143384_, ServerLevel p_143385_, GameProfile p_143386_, CallbackInfo ci) {
		chunksBeingTracked = new ArrayList<>();
		lastChunksBeingTracked = new ArrayList<>();
	}
	
	@Override
	public void tickTracking() {
		lastChunksBeingTracked = chunksBeingTracked;
		chunksBeingTracked = new ArrayList<>();
	}
	
	@Override
	public ArrayList<ChunkPos> oldTrackedChunks() {
		return lastChunksBeingTracked;
	}
	
	@Override
	public ArrayList<ChunkPos> trackedChunks() {
		return chunksBeingTracked;
	}
	
	@Override
	public boolean setDoUpdate(boolean val) {
		boolean doUpdate = this.doUpdate;
		this.doUpdate = val;
		return doUpdate;
	}
}
