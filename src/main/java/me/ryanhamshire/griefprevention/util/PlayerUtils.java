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

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.ShovelMode;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.visual.VisualizationType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.SpongeImplHooks;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

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

    public static ClaimType getClaimTypeFromShovel(ShovelMode shovelMode) {
        if (shovelMode == ShovelMode.Admin) {
            return ClaimType.ADMIN;
        }
        if (shovelMode == ShovelMode.Subdivide) {
            return ClaimType.SUBDIVISION;
        }
        if (shovelMode == ShovelMode.Town) {
            return ClaimType.TOWN;
        }
        return ClaimType.BASIC;
    }

    public static VisualizationType getVisualTypeFromShovel(ShovelMode shovelMode) {
        if (shovelMode == ShovelMode.Admin) {
            return VisualizationType.ADMINCLAIM;
        }
        if (shovelMode == ShovelMode.Subdivide) {
            return VisualizationType.SUBDIVISION;
        }
        if (shovelMode == ShovelMode.Town) {
            return VisualizationType.TOWN;
        }
        return VisualizationType.CLAIM;
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

    public static int getOptionIntValue(Subject subject, String key, int defaultValue) {
        String optionValue = subject.getOption(key).orElse(null);
        if (optionValue != null) {
            try {
                return Integer.parseInt(optionValue);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        // Set default option
        GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().setOption(new HashSet<>(), key, String.valueOf(defaultValue));

        return defaultValue;
    }

    public static double getOptionDoubleValue(Subject subject, String key, double defaultValue) {
        String optionValue = subject.getOption(key).orElse(null);
        if (optionValue != null) {
            try {
                return Double.parseDouble(optionValue);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        // Set default option
        GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().setOption(new HashSet<>(), key, String.valueOf(defaultValue));

        return defaultValue;
    }

    public static Optional<User> resolvePlayerByName(String name) {
        // try online players first
        Optional<Player> targetPlayer = Sponge.getGame().getServer().getPlayer(name);
        if (targetPlayer.isPresent()) {
            return Optional.of((User) targetPlayer.get());
        }

        Optional<User> user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(name);
        if (user.isPresent()) {
            return user;
        }

        return Optional.empty();
    }

    @Nullable
    public static UUID getUUIDByName(String name) {
        // try online players first
        Player targetPlayer = Sponge.getGame().getServer().getPlayer(name).orElse(null);
        if (targetPlayer != null) {
            return targetPlayer.getUniqueId();
        }

        User user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(name).orElse(null);
        if (user != null) {
            return user.getUniqueId();
        }

        return null;
    }

    // string overload for above helper
    public static String lookupPlayerName(String uuid) {
        if (uuid.equals(GriefPreventionPlugin.WORLD_USER_UUID.toString())) {
            return "administrator";
        }
        Optional<User> user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(UUID.fromString(uuid));
        if (!user.isPresent()) {
            GriefPreventionPlugin.addLogEntry("Error: Tried to look up a local player name for invalid UUID: " + uuid);
            return "someone";
        }

        return user.get().getName();
    }
}
