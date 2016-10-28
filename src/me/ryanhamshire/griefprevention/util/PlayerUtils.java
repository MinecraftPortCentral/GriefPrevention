/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
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

import me.ryanhamshire.griefprevention.ShovelMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.SpongeImplHooks;

public class PlayerUtils {

    public static boolean hasItemInOneHand(Player player, ItemType itemType) {
        ItemStack mainHand = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        ItemStack offHand = player.getItemInHand(HandTypes.OFF_HAND).orElse(null);
        if ((mainHand != null && mainHand.getItem().equals(itemType)) || (offHand != null && offHand.getItem().equals(itemType))) {
            return true;
        }

        return false;
    }

    public static boolean hasItemInOneHand(Player player) {
        ItemStack mainHand = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        ItemStack offHand = player.getItemInHand(HandTypes.OFF_HAND).orElse(null);
        if (mainHand != null || offHand != null) {
            return true;
        }

        return false;
    }

    public static Claim.Type getClaimTypeFromShovel(ShovelMode shovelMode) {
        if (shovelMode == ShovelMode.Admin) {
            return Claim.Type.ADMIN;
        }
        if (shovelMode == ShovelMode.Subdivide) {
            return Claim.Type.SUBDIVISION;
        }
        return Claim.Type.BASIC;
    }

    public static Tristate getTristateFromString(String value) {
        Tristate tristate = null;
        int intValue = -999;
        try {
            intValue = Integer.parseInt(value);
            if (intValue <= -1) {
                tristate = Tristate.FALSE;
            } else if (intValue == 0) {
                tristate = Tristate.UNDEFINED;
            } else {
                tristate = Tristate.TRUE;
            }
            return tristate;

        } catch (NumberFormatException e) {
            // ignore
        }

        // check if boolean
        try {
            tristate = Tristate.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        return tristate;
    }

    public static RayTraceResult rayTracePlayerEyes(EntityPlayerMP player) {
        double distance = SpongeImplHooks.getBlockReachDistance(player) + 1;
        Vec3d startPos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d endPos = startPos.add(new Vec3d(player.getLookVec().xCoord * distance, player.getLookVec().yCoord * distance, player.getLookVec().zCoord * distance));
        return player.worldObj.rayTraceBlocks(startPos, endPos);
    }

    public static Vec3d rayTracePlayerEyeHitVec(EntityPlayerMP player) {
        RayTraceResult result = rayTracePlayerEyes(player);
        return result == null ? null : result.hitVec;
    }
}
