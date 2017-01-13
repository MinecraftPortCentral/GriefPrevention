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
import me.ryanhamshire.griefprevention.claim.GPClaim;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Map;

public class BlockUtils {

    private static final Map<BlockState, Integer> BLOCKSTATE_META_CACHE = Maps.newHashMap();
    private static final String locationStringDelimiter = ";";

    public static String positionToString(Location<World> location) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location.getBlockX());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockY());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockZ());

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
}
