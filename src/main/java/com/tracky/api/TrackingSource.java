package com.tracky.api;

import com.tracky.util.ReadOnlySet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class TrackingSource {
    protected Collection<ChunkPos> chunks;
    // TODO: use this in the chunk provider tick method instead of iterating over every single chunk in the render source?
//    protected Set<ChunkPos> newChunks = new HashSet<>();

    protected boolean needsUpdate = true;
    
    public TrackingSource(Collection<ChunkPos> chunks) {
        this.chunks = chunks;
//        newChunks.addAll(chunks);
    }

    /**
     * Tests if the render source contains a chunk
     * This is used for ensuring that tracked chunks remain loaded when changing render distance
     * HEAVILY advised to override this, as it's slow
     *
     * @param pos the position of the chunk
     * @return if the render source has it in its list of sections
     */
    public boolean containsChunk(ChunkPos pos) {
        return this.chunks.contains(pos);
    }

    public void addChunk(ChunkPos pos) {
        if (this.chunks.add(pos)) {
//            this.newChunks.add(pos);
            needsUpdate = true;
        }
    }

    public void removeChunk(ChunkPos pos) {
        this.chunks.remove(pos);
//        this.newChunks.remove(pos);
        needsUpdate = true;
    }

    /**
     * Checks whether or not the tracking source should be considered for the provided player
     *
     * @param player the player in question
     * @return if the tracking source should be skipped
     */
    public boolean check(Player player) {
        return true;
    }

    /**
     * Checks whether or not a chunk should be synced to a player's client
     *
     * @param player the player to check the chunk for
     * @param pos    the chunk position to check; may not always be in the tracking source
     * @return whether or not the chunk should be synced to the associated client
     */
    public boolean checkRenderDist(Player player, ChunkPos pos) {
        return true;
    }

    /**
     * Checks whether or not a chunk should be loaded by a player
     *
     * @param player the player to check the chunk for
     * @param pos    the chunk position to check; may not always be in the tracking source
     * @return whether or not the chunk should be loaded due to the player
     */
    public boolean checkLoadDist(Player player, ChunkPos pos) {
        return true;
    }
    
    /**
     * For most if not all purposes, {@link TrackingSource#forEachUntil(boolean, ServerPlayer, Function)} and {@link TrackingSource#forEachValid(boolean, ServerPlayer, Consumer)} should do
     * There is no strict expectation that this will actually return the proper list
     * <br>
     * This method may be removed entirely, or it may be replaced with a forEach() method
     * Haven't decided yet what the best approach is
     *
     * @return a list of chunks that may or may not actually be accurate to what the source is tracking for the player, dependent on the mod implementing it
     */
    @Deprecated(forRemoval = true)
    public Collection<ChunkPos> getChunks(ServerPlayer player) {
        return new ReadOnlySet<>(this.chunks);
    }

    public void forEachValid(boolean load, ServerPlayer player, Consumer<ChunkPos> action) {
        if (load) {
            for (ChunkPos chunk : this.chunks) {
                if (this.checkLoadDist(player, chunk)) {
                    action.accept(chunk);
                }
            }
        } else {
            for (ChunkPos chunk : this.chunks) {
                if (this.checkRenderDist(player, chunk)) {
                    action.accept(chunk);
                }
            }
        }
    }

    public boolean forEachUntil(boolean load, ServerPlayer player, Function<ChunkPos, Boolean> action) {
        if (load) {
            for (ChunkPos chunk : this.chunks) {
                if (this.checkLoadDist(player, chunk)) {
                    if (action.apply(chunk)) {
                        return true;
                    }
                }
            }
        } else {
            for (ChunkPos chunk : this.chunks) {
                if (this.checkRenderDist(player, chunk)) {
                    if (action.apply(chunk)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public boolean needsUpdate() {
        return needsUpdate;
    }
    
    public void markUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }
}
