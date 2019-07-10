/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.griefprevention.util;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimBlockSystem;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkProviderBridge;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BlockUtils {

    public static final Direction[] CARDINAL_SET = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        };
    public static final Direction[] ORDINAL_SET = {
            Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,
        };
    private static final int NUM_XZ_BITS = 4;
    private static final int NUM_SHORT_Y_BITS = 8;
    private static final short XZ_MASK = 0xF;
    private static final short Y_SHORT_MASK = 0xFF;

    public static final Map<Integer, BlockPosCache> ENTITY_BLOCK_CACHE = new Int2ObjectArrayMap<>();
    private static final Map<BlockState, Integer> BLOCKSTATE_META_CACHE = Maps.newHashMap();
    private static final String locationStringDelimiter = ";";

    public static String positionToString(Location<World> location) {
        return positionToString(location.getBlockPosition());
    }

    public static String positionToString(Vector3i pos) {
        return positionToString(pos.getX(), pos.getY(), pos.getZ());
    }

    public static String positionToString(int x, int y, int z) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(x);
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(y);
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(z);

        return stringBuilder.toString();
    }

    // turns a location string back into a location
    public static Vector3i positionFromString(String string) throws Exception {
        // split the input string on the space
        String[] elements = string.split(locationStringDelimiter);

        // expect three elements - X, Y, and Z, respectively
        if (elements.length < 3) {
            throw new Exception("Expected four distinct parts to the location string: \"" + string + "\"");
        }

        String xString = elements[0];
        String yString = elements[1];
        String zString = elements[2];

        // convert those numerical strings to integer values
        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Vector3i(x, y, z);
    }

    // distance check for claims, distance in this case is a band around the
    // outside of the claim rather then euclidean distance
    public static boolean isLocationNearClaim(GPClaim claim, Location<World> location, int howNear) {
        Location<World> lesserBoundaryCorner = new Location<World>(claim.lesserBoundaryCorner.getExtent(), claim.lesserBoundaryCorner.getBlockX() - howNear,
                claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - howNear);
        Location<World> greaterBoundaryCorner = new Location<World>(claim.greaterBoundaryCorner.getExtent(), claim.greaterBoundaryCorner.getBlockX() + howNear,
                claim.greaterBoundaryCorner.getBlockY(), claim.greaterBoundaryCorner.getBlockZ() + howNear);
        // not in the same world implies false
        if (!location.getExtent().equals(lesserBoundaryCorner.getExtent())) {
            return false;
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        // main check
        boolean inClaim = false;
        if (claim.isCuboid()) {
            inClaim = (
                    x >= lesserBoundaryCorner.getX() &&
                    x <= greaterBoundaryCorner.getX() &&
                    y >= lesserBoundaryCorner.getY()) &&
                    y <= greaterBoundaryCorner.getY() &&
                    z >= lesserBoundaryCorner.getZ() &&
                    z <= greaterBoundaryCorner.getZ();
        } else {
            inClaim = (y >= lesserBoundaryCorner.getY()) &&
                x >= lesserBoundaryCorner.getX() &&
                x < greaterBoundaryCorner.getX() + 1 &&
                z >= lesserBoundaryCorner.getZ() &&
                z < greaterBoundaryCorner.getZ() + 1;
        }

        if (!inClaim) {
            return false;
        }

        return true;
    }

    public static boolean clickedClaimCorner(GPClaim claim, Vector3i clickedPos) {
        int clickedX = clickedPos.getX();
        int clickedY = clickedPos.getY();
        int clickedZ = clickedPos.getZ();
        int lesserX = claim.getLesserBoundaryCorner().getBlockX();
        int lesserY = claim.getLesserBoundaryCorner().getBlockY();
        int lesserZ = claim.getLesserBoundaryCorner().getBlockZ();
        int greaterX = claim.getGreaterBoundaryCorner().getBlockX();
        int greaterY = claim.getGreaterBoundaryCorner().getBlockY();
        int greaterZ = claim.getGreaterBoundaryCorner().getBlockZ();
        if ((clickedX == lesserX || clickedX == greaterX) && (clickedZ == lesserZ || clickedZ == greaterZ)
                && (!claim.isCuboid() || (clickedY == lesserY || clickedY == greaterY))) {
            return true;
        }

        return false;
    }

    public static int getBlockStateMeta(BlockState state) {
        Integer meta = BLOCKSTATE_META_CACHE.get(state);
        if (meta == null) {
            Block mcBlock = (net.minecraft.block.Block) state.getType();
            meta = mcBlock.getMetaFromState((IBlockState) state);
            BLOCKSTATE_META_CACHE.put(state, meta);
        }
        return meta;
    }

    public static int getClaimBlockCost(World world, Vector3i lesser, Vector3i greater, boolean cuboid) {
        final int claimWidth = greater.getX() - lesser.getX() + 1;
        final int claimHeight = greater.getY() - lesser.getY() + 1;
        final int claimLength = greater.getZ() - lesser.getZ() + 1;
        if (GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA) {
            return claimWidth * claimLength;
        }

        return claimLength * claimWidth * claimHeight;
    }

    /**
     * Serialize this BlockPos into a short value
     */
    public static short blockPosToShort(BlockPos pos) {
        short serialized = (short) setNibble(0, pos.getX() & XZ_MASK, 0, NUM_XZ_BITS);
        serialized = (short) setNibble(serialized, pos.getY() & Y_SHORT_MASK, 1, NUM_SHORT_Y_BITS);
        serialized = (short) setNibble(serialized, pos.getZ() & XZ_MASK, 3, NUM_XZ_BITS);
        return serialized;
    }

    private static int setNibble(int num, int data, int which, int bitsToReplace) {
        return (num & ~(bitsToReplace << (which * 4)) | (data << (which * 4)));
    }

    private static void saveChunkData(ChunkProviderServer chunkProviderServer, Chunk chunkIn)
    {
        try
        {
            chunkIn.setLastSaveTime(chunkIn.getWorld().getTotalWorldTime());
            chunkProviderServer.chunkLoader.saveChunk(chunkIn.getWorld(), chunkIn);
        }
        catch (IOException ioexception)
        {
            //LOGGER.error((String)"Couldn\'t save chunk", (Throwable)ioexception);
        }
        catch (MinecraftException minecraftexception)
        {
            //LOGGER.error((String)"Couldn\'t save chunk; already in use by another instance of Minecraft?", (Throwable)minecraftexception);
        }
        try
        {
            chunkProviderServer.chunkLoader.saveExtraChunkData(chunkIn.getWorld(), chunkIn);
        }
        catch (Exception exception)
        {
            //LOGGER.error((String)"Couldn\'t save entities", (Throwable)exception);
        }
    }

    private static boolean unloadChunk(net.minecraft.world.chunk.Chunk chunk) {
        net.minecraft.world.World mcWorld = chunk.getWorld();
        ChunkProviderServer chunkProviderServer = (ChunkProviderServer) chunk.getWorld().getChunkProvider();

        boolean saveChunk = false;
        if (chunk.needsSaving(true)) {
            saveChunk = true;
        }

        for (ClassInheritanceMultiMap<Entity> classinheritancemultimap : chunk.getEntityLists())
        {
            chunk.getWorld().unloadEntities(classinheritancemultimap);
        }

        if (saveChunk) {
            saveChunkData(chunkProviderServer, chunk);
        }

        chunkProviderServer.id2ChunkMap.remove(ChunkPos.asLong(chunk.x, chunk.z));
        ((ChunkBridge) chunk).bridge$setScheduledForUnload(-1);
        org.spongepowered.api.world.Chunk spongeChunk = (org.spongepowered.api.world.Chunk) chunk;
        for (Direction direction : CARDINAL_SET) {
            Vector3i neighborPosition = spongeChunk.getPosition().add(direction.asBlockOffset());
            ChunkProviderBridge spongeChunkProvider = (ChunkProviderBridge) mcWorld.getChunkProvider();
            net.minecraft.world.chunk.Chunk neighbor = spongeChunkProvider.bridge$getLoadedChunkWithoutMarkingActive(neighborPosition.getX(),
                neighborPosition.getZ());
            if (neighbor != null) {
                int neighborIndex = directionToIndex(direction);
                int oppositeNeighborIndex = directionToIndex(direction.getOpposite());
                ((ChunkBridge) spongeChunk).bridge$setNeighborChunk(neighborIndex, null);
                ((ChunkBridge) neighbor).bridge$setNeighborChunk(oppositeNeighborIndex, null);
            }
        }

        return true;
    }

    public static boolean createFillerChunk(GPPlayerData playerData, WorldServer world, int chunkX, int chunkZ) {
        ChunkProviderServer chunkProviderServer = world.getChunkProvider();
        Chunk chunk = ((ChunkProviderBridge) chunkProviderServer).bridge$getLoadedChunkWithoutMarkingActive(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }

        unloadChunk(chunk);
        org.spongepowered.api.world.Chunk fillerChunk = (org.spongepowered.api.world.Chunk) chunkProviderServer.chunkGenerator.generateChunk(chunk.x, chunk.z);
        int maxBuildHeight = fillerChunk.getWorld().getDimension().getBuildHeight() - 1;
        BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxBuildHeight][18];
        BlockSnapshot startBlock = fillerChunk.createSnapshot(0, 0, 0);
        Location<World> startLocation =
                new Location<World>(fillerChunk.getWorld(), startBlock.getPosition().getX() - 1, 0, startBlock.getPosition().getZ() - 1);
        for (int x = 0; x < snapshots.length; x++) {
            for (int z = 0; z < snapshots[0][0].length; z++) {
                for (int y = 0; y < snapshots[0].length; y++) {
                    snapshots[x][y][z] = fillerChunk.getWorld()
                            .createSnapshot(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
                }
            }
        }
        fillerChunk = null;
        playerData.fillerBlocks = snapshots;
        if (chunk != null) {
            world.getChunkProvider().id2ChunkMap.put(ChunkPos.asLong(chunk.x, chunk.z), chunk);

            org.spongepowered.api.world.Chunk spongeChunk = (org.spongepowered.api.world.Chunk) chunk;
            for (Direction direction : CARDINAL_SET) {
                Vector3i neighborPosition = spongeChunk.getPosition().add(direction.asBlockOffset());
                ChunkProviderBridge spongeChunkProvider = (ChunkProviderBridge) world.getChunkProvider();
                net.minecraft.world.chunk.Chunk neighbor = spongeChunkProvider.bridge$getLoadedChunkWithoutMarkingActive(neighborPosition.getX(),
                    neighborPosition.getZ());
                if (neighbor != null) {
                    int neighborIndex = directionToIndex(direction);
                    int oppositeNeighborIndex = directionToIndex(direction.getOpposite());
                    ((ChunkBridge) spongeChunk).bridge$setNeighborChunk(neighborIndex, neighbor);
                    ((ChunkBridge) neighbor).bridge$setNeighborChunk(oppositeNeighborIndex, (net.minecraft.world.chunk.Chunk)(Object) chunk);
                }
            }
        }
        return true;
    }

    public static boolean regenerateChunk(Chunk chunk) {
        boolean unload = unloadChunk(chunk);
        ChunkProviderServer chunkProviderServer = (ChunkProviderServer) chunk.getWorld().getChunkProvider();

        Chunk newChunk = chunkProviderServer.chunkGenerator.generateChunk(chunk.x, chunk.z);
        PlayerChunkMapEntry playerChunk = ((WorldServer) chunk.getWorld()).getPlayerChunkMap().getEntry(chunk.x, chunk.z);
        if (playerChunk != null) {
            playerChunk.chunk = newChunk;
        }

        chunkLoadPostProcess(newChunk);
        refreshChunk(newChunk);
        return newChunk != null;
    }

    public static boolean refreshChunk(Chunk chunk) {
        int x = chunk.x;
        int z = chunk.z;
        WorldServer world = (WorldServer) chunk.getWorld();
        ChunkProviderBridge chunkProviderServer = (ChunkProviderBridge) world.getChunkProvider();
        if (chunkProviderServer.bridge$getLoadedChunkWithoutMarkingActive(x, z) == null) {
            return false;
        }

        int px = x << 4;
        int pz = z << 4;

        int height = world.getHeight() / 16;
        for (int y = 0; y < 64; y++) {
            world.notifyBlockUpdate(new BlockPos(px + (y / height), ((y % height) * 16), pz), Blocks.AIR.getDefaultState(), Blocks.STONE.getDefaultState(), 3);
        }
        world.notifyBlockUpdate(new BlockPos(px + 15, (height * 16) - 1, pz + 15), Blocks.AIR.getDefaultState(), Blocks.STONE.getDefaultState(), 3);

        return true;
    }

    private static void chunkLoadPostProcess(Chunk chunk) {
        if (chunk != null) {
            WorldServer world = (WorldServer) chunk.getWorld();
            world.getChunkProvider().id2ChunkMap.put(ChunkPos.asLong(chunk.x, chunk.z), chunk);

            org.spongepowered.api.world.Chunk spongeChunk = (org.spongepowered.api.world.Chunk) chunk;
            for (Direction direction : CARDINAL_SET) {
                Vector3i neighborPosition = spongeChunk.getPosition().add(direction.asBlockOffset());
                ChunkProviderBridge spongeChunkProvider = (ChunkProviderBridge) world.getChunkProvider();
                net.minecraft.world.chunk.Chunk neighbor = spongeChunkProvider.bridge$getLoadedChunkWithoutMarkingActive(neighborPosition.getX(), neighborPosition.getZ());
                if (neighbor != null) {
                    int neighborIndex = directionToIndex(direction);
                    int oppositeNeighborIndex = directionToIndex(direction.getOpposite());
                    ((ChunkBridge) spongeChunk).bridge$setNeighborChunk(neighborIndex, neighbor);
                    ((ChunkBridge) neighbor).bridge$setNeighborChunk(oppositeNeighborIndex, (net.minecraft.world.chunk.Chunk)(Object) chunk);
                }
            }

            chunk.populate(world.getChunkProvider(), world.getChunkProvider().chunkGenerator);
        }
    }

    private static int directionToIndex(Direction direction) {
        switch (direction) {
            case NORTH:
            case NORTHEAST:
            case NORTHWEST:
                return 0;
            case SOUTH:
            case SOUTHEAST:
            case SOUTHWEST:
                return 1;
            case EAST:
                return 2;
            case WEST:
                return 3;
            default:
                throw new IllegalArgumentException("Unexpected direction");
        }
    }

    public static Optional<Location<World>> getTargetBlock(Player player, GPPlayerData playerData, int maxDistance, boolean ignoreAir) throws IllegalStateException {
        BlockRay<World> blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GPClaim claim = null;
        if (playerData.visualClaimId != null) {
            claim = (GPClaim) GriefPreventionPlugin.instance.dataStore.getClaim(player.getWorld().getProperties(), playerData.visualClaimId);
        }

        while (blockRay.hasNext()) {
            BlockRayHit<World> blockRayHit = blockRay.next();
            if (claim != null) {
                for (Vector3i corner : claim.getVisualizer().getVisualCorners()) {
                    if (corner.equals(blockRayHit.getBlockPosition())) {
                        return Optional.of(blockRayHit.getLocation());
                    }
                }
            }
            if (ignoreAir) {
                if (blockRayHit.getLocation().getBlockType() != BlockTypes.TALLGRASS) {
                    return Optional.of(blockRayHit.getLocation());
                }
            } else { 
                if (blockRayHit.getLocation().getBlockType() != BlockTypes.AIR &&
                        blockRayHit.getLocation().getBlockType() != BlockTypes.TALLGRASS) {
                    return Optional.of(blockRayHit.getLocation());
                }
            }
        }

        return Optional.empty();
    }

    public static boolean isLiquidSource(Object source) {
        if (source instanceof BlockSnapshot) {
            final BlockSnapshot blockSnapshot = (BlockSnapshot) source;
            MatterProperty matterProperty = blockSnapshot.getState().getProperty(MatterProperty.class).orElse(null);
            if (matterProperty != null && matterProperty.getValue() == MatterProperty.Matter.LIQUID) {
                return true;
            }
        }
        if (source instanceof LocatableBlock) {
            final LocatableBlock locatableBlock = (LocatableBlock) source;
            MatterProperty matterProperty = locatableBlock.getBlockState().getProperty(MatterProperty.class).orElse(null);
            if (matterProperty != null && matterProperty.getValue() == MatterProperty.Matter.LIQUID) {
                return true;
            }
        }
        return false;
    }

    public static Set<Claim> getNearbyClaims(Location<World> location) {
        return getNearbyClaims(location, 50);
    }

    public static Set<Claim> getNearbyClaims(Location<World> location, int blockDistance) {
        Set<Claim> claims = new HashSet<>();
        GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(location.getExtent().getProperties());
        if (claimWorldManager == null) {
            return claims;
        }

        org.spongepowered.api.world.Chunk lesserChunk = location.getExtent().getChunkAtBlock(location.sub(blockDistance, 0, blockDistance).getBlockPosition()).orElse(null);
        org.spongepowered.api.world.Chunk greaterChunk = location.getExtent().getChunkAtBlock(location.add(blockDistance, 0, blockDistance).getBlockPosition()).orElse(null);

        if (lesserChunk != null && greaterChunk != null) {
            for (int chunkX = lesserChunk.getPosition().getX(); chunkX <= greaterChunk.getPosition().getX(); chunkX++) {
                for (int chunkZ = lesserChunk.getPosition().getZ(); chunkZ <= greaterChunk.getPosition().getZ(); chunkZ++) {
                    org.spongepowered.api.world.Chunk chunk = location.getExtent().getChunk(chunkX, 0, chunkZ).orElse(null);
                    if (chunk != null) {
                        Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(ChunkPos.asLong(chunkX, chunkZ));
                        if (claimsInChunk != null) {
                            for (Claim claim : claimsInChunk) {
                                final GPClaim gpClaim = (GPClaim) claim;
                                if (gpClaim.parent == null && !claims.contains(claim)) {
                                    claims.add(claim);
                                }
                            }
                        }
                    }
                }
            }
        }

        return claims;
    }
}
