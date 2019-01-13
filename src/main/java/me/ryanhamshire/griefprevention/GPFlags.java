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
package me.ryanhamshire.griefprevention;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import net.minecraft.entity.EnumCreatureType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.common.entity.SpongeEntityType;

import java.util.Optional;

public class GPFlags {

    public static boolean BLOCK_BREAK;
    public static boolean BLOCK_PLACE;
    public static boolean COMMAND_EXECUTE;
    public static boolean COMMAND_EXECUTE_PVP;
    public static boolean ENTER_CLAIM;
    public static boolean ENTITY_CHUNK_SPAWN;
    public static boolean ENTITY_COLLIDE_BLOCK;
    public static boolean ENTITY_COLLIDE_ENTITY;
    public static boolean ENTITY_DAMAGE;
    public static boolean ENTITY_RIDING;
    public static boolean ENTITY_SPAWN;
    public static boolean ENTITY_TELEPORT_FROM;
    public static boolean ENTITY_TELEPORT_TO;
    public static boolean EXIT_CLAIM;
    public static boolean EXPLOSION;
    public static boolean EXPLOSION_SURFACE;
    public static boolean FIRE_SPREAD;
    public static boolean INTERACT_BLOCK_PRIMARY;
    public static boolean INTERACT_BLOCK_SECONDARY;
    public static boolean INTERACT_ENTITY_PRIMARY;
    public static boolean INTERACT_ENTITY_SECONDARY;
    public static boolean INTERACT_INVENTORY;
    public static boolean INTERACT_ITEM_PRIMARY;
    public static boolean INTERACT_ITEM_SECONDARY;
    public static boolean INTERACT_INVENTORY_CLICK;
    public static boolean ITEM_DROP;
    public static boolean ITEM_PICKUP;
    public static boolean ITEM_SPAWN;
    public static boolean ITEM_USE;
    public static boolean LEAF_DECAY;
    public static boolean LIQUID_FLOW;
    public static boolean PORTAL_USE;
    public static boolean PROJECTILE_IMPACT_BLOCK;
    public static boolean PROJECTILE_IMPACT_ENTITY;
    public static final BiMap<String, EnumCreatureType> SPAWN_TYPES = HashBiMap.create();

    static {
        SPAWN_TYPES.put("ambient", EnumCreatureType.AMBIENT);
        SPAWN_TYPES.put("animal", EnumCreatureType.CREATURE);
        SPAWN_TYPES.put("aquatic", EnumCreatureType.WATER_CREATURE);
        SPAWN_TYPES.put("monster", EnumCreatureType.MONSTER);
    }

    public static void populateFlagStatus() {
        BLOCK_BREAK = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.BLOCK_BREAK.toString());
        BLOCK_PLACE = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.BLOCK_PLACE.toString());
        COMMAND_EXECUTE  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.COMMAND_EXECUTE.toString());
        COMMAND_EXECUTE_PVP  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.COMMAND_EXECUTE_PVP.toString());
        ENTER_CLAIM  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTER_CLAIM.toString());
        ENTITY_CHUNK_SPAWN  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_CHUNK_SPAWN.toString());
        ENTITY_COLLIDE_BLOCK  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString());
        ENTITY_COLLIDE_ENTITY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString());
        ENTITY_DAMAGE  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_DAMAGE.toString());
        ENTITY_RIDING  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_RIDING.toString());
        ENTITY_SPAWN  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_SPAWN.toString());
        ENTITY_TELEPORT_FROM  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_TELEPORT_FROM.toString());
        ENTITY_TELEPORT_TO  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ENTITY_TELEPORT_TO.toString());
        EXIT_CLAIM  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.EXIT_CLAIM.toString());
        EXPLOSION  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.EXPLOSION.toString());
        EXPLOSION_SURFACE  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.EXPLOSION_SURFACE.toString());
        FIRE_SPREAD  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.FIRE_SPREAD.toString());
        INTERACT_BLOCK_PRIMARY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString());
        INTERACT_BLOCK_SECONDARY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString());
        INTERACT_ENTITY_PRIMARY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString());
        INTERACT_ENTITY_SECONDARY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString());
        INTERACT_INVENTORY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_INVENTORY.toString());
        INTERACT_INVENTORY_CLICK  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_INVENTORY_CLICK.toString());
        INTERACT_ITEM_PRIMARY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_ITEM_PRIMARY.toString());
        INTERACT_ITEM_SECONDARY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.INTERACT_ITEM_SECONDARY.toString());
        ITEM_DROP  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ITEM_DROP.toString());
        ITEM_PICKUP  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ITEM_PICKUP.toString());
        ITEM_SPAWN  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ITEM_SPAWN.toString());
        ITEM_USE  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.ITEM_USE.toString());
        LEAF_DECAY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.LEAF_DECAY.toString());
        LIQUID_FLOW  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.LIQUID_FLOW.toString());
        PORTAL_USE  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.PORTAL_USE.toString());
        PROJECTILE_IMPACT_BLOCK  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString());
        PROJECTILE_IMPACT_ENTITY  = GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString());
    }

    public static String getEntitySpawnFlag(ClaimFlag flag, String target) {
        switch (flag) {
            case ENTER_CLAIM:
            case ENTITY_CHUNK_SPAWN:
            case ENTITY_COLLIDE_ENTITY:
            case ENTITY_DAMAGE:
            case ENTITY_RIDING:
            case ENTITY_SPAWN:
            case ENTITY_TELEPORT_FROM:
            case ENTITY_TELEPORT_TO:
            case EXIT_CLAIM:
            case INTERACT_ENTITY_PRIMARY:
            case INTERACT_ENTITY_SECONDARY:
            case ITEM_SPAWN:
            case PROJECTILE_IMPACT_ENTITY:
                String claimFlag = flag.toString();
                // first check for valid SpawnType
                String[] parts = target.split(":");
                EnumCreatureType type = SPAWN_TYPES.get(parts[1]);
                if (type != null) {
                    claimFlag += "." + parts[0] + "." + SPAWN_TYPES.inverse().get(type);
                    return claimFlag;
                } else {
                    Optional<EntityType> entityType = Sponge.getRegistry().getType(EntityType.class, target);
                    if (entityType.isPresent()) {
                        SpongeEntityType spongeEntityType = (SpongeEntityType) entityType.get();
                        if (spongeEntityType.getEnumCreatureType() != null) {
                            String creatureType = SPAWN_TYPES.inverse().get(spongeEntityType.getEnumCreatureType());
                            if (parts[1].equalsIgnoreCase("pixelmon")) {
                                claimFlag += "." + parts[0] + ".animal";
                            } else {
                                claimFlag += "." + parts[0] + "." + creatureType + "." + parts[1];
                            }
                            return claimFlag;
                        } else {
                            claimFlag += "." + parts[0] + "." + parts[1];
                            return claimFlag;
                        }
                    }
                    // Unfortunately this is required until Pixelmon registers their entities correctly in FML
                    if (target.contains("pixelmon")) {
                        // If target was not found in registry, assume its a pixelmon animal
                        if (parts[1].equalsIgnoreCase("pixelmon")) {
                            claimFlag += "." + parts[0] + ".animal";
                        } else {
                            claimFlag += "." + parts[0] + ".animal." + parts[1];
                        }
                        return claimFlag;
                    }
                }
            default:
                return null;
        }
    }
}
