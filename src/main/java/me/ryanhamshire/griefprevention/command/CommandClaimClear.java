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
package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityOwnable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.entity.SpongeEntityType;

import java.util.UUID;

public class CommandClaimClear implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String target = ctx.<String>getOne("target").get();
        String claimId = ctx.<String>getOne("claim").orElse(null);
        WorldProperties worldProperties = ctx.<WorldProperties>getOne("world").orElse(null);
        if (worldProperties == null && src instanceof Player) {
            worldProperties = ((Player) src).getWorld().getProperties();
        }
        if (worldProperties == null) {
            worldProperties = Sponge.getServer().getDefaultWorld().orElse(null);
        }

        World world = Sponge.getServer().getWorld(worldProperties.getUniqueId()).orElse(null);
        if (world == null) {
            return CommandResult.success();
        }

        if (claimId == null) {
            if (src instanceof Player) {
                GPClaim sourceClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(((Player) src).getLocation(), false, null);
                if (sourceClaim != null) {
                    claimId = sourceClaim.getUniqueId().toString();
                }
            }
        }

        UUID claimUniqueId = null;
        try {
            claimUniqueId = UUID.fromString(claimId);
        } catch (IllegalArgumentException e) {
            return CommandResult.success();
        }

        // Unfortunately this is required until Pixelmon registers their entities correctly in FML
        // If target was not found in registry, assume its a pixelmon animal
        EntityType entityType = Sponge.getRegistry().getType(EntityType.class, target).orElse(null);
        boolean isPixelmonAnimal = target.contains("pixelmon") && entityType == null;

        int count = 0;
        String[] parts = target.split(":");
        String modId = "minecraft";
        EnumCreatureType creatureType = null;
        if (parts.length > 1) {
            modId = parts[0];
            creatureType = GPFlags.SPAWN_TYPES.get(parts[1]);
        } else {
            creatureType = GPFlags.SPAWN_TYPES.get(parts[0]);
            if (creatureType == null) {
                target = "minecraft:" + target;
            }
        }

        for (Entity entity : world.getEntities()) {
            net.minecraft.entity.Entity mcEntity = (net.minecraft.entity.Entity) entity;
            if (entity instanceof Villager) {
                continue;
            }
            if (entity instanceof IEntityOwnable) {
                IEntityOwnable ownable = (IEntityOwnable) entity;
                if (ownable.getOwnerId() != null) {
                    continue;
                }
            }

            if (isPixelmonAnimal) {
                if (parts[1].equalsIgnoreCase(mcEntity.getName().toLowerCase())) {
                    GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
                    if (claim != null && claim.getUniqueId().equals(claimUniqueId)) {
                        mcEntity.setDead();
                        count++;
                    }
                }
            } else if (creatureType != null && SpongeImplHooks.isCreatureOfType(mcEntity, creatureType)) {
                GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
                if (claim != null && claim.getUniqueId().equals(claimUniqueId)) {
                    // check modId
                    String mod = ((SpongeEntityType) entity.getType()).getModId();
                    if (modId.equalsIgnoreCase(mod)) {
                        mcEntity.setDead();
                        count++;
                    }
                }
            } else {
                if (entityType == null || !entityType.equals(((SpongeEntityType) entity.getType()))) {
                    continue;
                }

                GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
                if (claim != null && claim.getUniqueId().equals(claimUniqueId)) {
                    mcEntity.setDead();
                    count++;
                }
            }
        }

        if (count == 0) {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.GRAY, "Could not locate any entities of type ", 
                    TextColors.GREEN, target, TextColors.WHITE, "."));
        } else {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.RED, "Killed ", TextColors.AQUA, count, 
                    TextColors.WHITE, " entities of type ", TextColors.GREEN, target, 
                    TextColors.WHITE, "."));
        }

        return CommandResult.success();
    }
}
