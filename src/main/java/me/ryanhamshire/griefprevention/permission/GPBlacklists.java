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
package me.ryanhamshire.griefprevention.permission;

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.configuration.category.BlacklistCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPBlacklists {

    public static Map<String, List<String>> blacklistMap = new HashMap<>();

    public static boolean GLOBAL_SOURCE;
    public static boolean GLOBAL_TARGET;

    public static boolean BLOCK_BREAK;
    public static boolean BLOCK_NOTIFY;
    public static boolean BLOCK_PLACE;
    public static boolean BLOCK_PRE;
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

    public static void populateBlacklistStatus() {
        final BlacklistCategory blacklistCategory = GriefPreventionPlugin.getGlobalConfig().getConfig().blacklist;
        GLOBAL_SOURCE = !blacklistCategory.getGlobalSourceBlacklist().isEmpty();
        GLOBAL_TARGET = !blacklistCategory.getGlobalTargetBlacklist().isEmpty();
        BLOCK_BREAK = !blacklistCategory.getFlagBlacklist(ClaimFlag.BLOCK_BREAK.toString()).isEmpty();
        BLOCK_NOTIFY = !blacklistCategory.getFlagBlacklist("block-notify").isEmpty();
        BLOCK_PLACE = !blacklistCategory.getFlagBlacklist(ClaimFlag.BLOCK_PLACE.toString()).isEmpty();
        BLOCK_PRE = !blacklistCategory.getFlagBlacklist("block-pre").isEmpty();
        COMMAND_EXECUTE = !blacklistCategory.getFlagBlacklist(ClaimFlag.COMMAND_EXECUTE.toString()).isEmpty();
        COMMAND_EXECUTE_PVP = !blacklistCategory.getFlagBlacklist(ClaimFlag.COMMAND_EXECUTE_PVP.toString()).isEmpty();
        ENTER_CLAIM = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTER_CLAIM.toString()).isEmpty();
        ENTITY_CHUNK_SPAWN = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_CHUNK_SPAWN.toString()).isEmpty();
        ENTITY_COLLIDE_BLOCK = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString()).isEmpty();
        ENTITY_COLLIDE_ENTITY = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString()).isEmpty();
        ENTITY_DAMAGE = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_DAMAGE.toString()).isEmpty();
        ENTITY_RIDING = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_RIDING.toString()).isEmpty();
        ENTITY_SPAWN = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_SPAWN.toString()).isEmpty();
        ENTITY_TELEPORT_FROM = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_TELEPORT_FROM.toString()).isEmpty();
        ENTITY_TELEPORT_TO = !blacklistCategory.getFlagBlacklist(ClaimFlag.ENTITY_TELEPORT_TO.toString()).isEmpty();
        EXIT_CLAIM = !blacklistCategory.getFlagBlacklist(ClaimFlag.EXIT_CLAIM.toString()).isEmpty();
        EXPLOSION = !blacklistCategory.getFlagBlacklist(ClaimFlag.EXPLOSION.toString()).isEmpty();
        EXPLOSION_SURFACE = !blacklistCategory.getFlagBlacklist(ClaimFlag.EXPLOSION_SURFACE.toString()).isEmpty();
        FIRE_SPREAD = !blacklistCategory.getFlagBlacklist(ClaimFlag.FIRE_SPREAD.toString()).isEmpty();
        INTERACT_BLOCK_PRIMARY = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString()).isEmpty();
        INTERACT_BLOCK_SECONDARY = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString()).isEmpty();
        INTERACT_ENTITY_PRIMARY = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString()).isEmpty();
        INTERACT_ENTITY_SECONDARY = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString()).isEmpty();
        INTERACT_INVENTORY = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_INVENTORY.toString()).isEmpty();
        INTERACT_ITEM_PRIMARY = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_ITEM_PRIMARY.toString()).isEmpty();
        INTERACT_ITEM_SECONDARY = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_ITEM_SECONDARY.toString()).isEmpty();
        INTERACT_INVENTORY_CLICK = !blacklistCategory.getFlagBlacklist(ClaimFlag.INTERACT_INVENTORY_CLICK.toString()).isEmpty();
        ITEM_DROP = !blacklistCategory.getFlagBlacklist(ClaimFlag.ITEM_DROP.toString()).isEmpty();
        ITEM_PICKUP = !blacklistCategory.getFlagBlacklist(ClaimFlag.ITEM_PICKUP.toString()).isEmpty();
        ITEM_SPAWN = !blacklistCategory.getFlagBlacklist(ClaimFlag.ITEM_SPAWN.toString()).isEmpty();
        ITEM_USE = !blacklistCategory.getFlagBlacklist(ClaimFlag.ITEM_USE.toString()).isEmpty();
        LEAF_DECAY = !blacklistCategory.getFlagBlacklist(ClaimFlag.LEAF_DECAY.toString()).isEmpty();
        LIQUID_FLOW = !blacklistCategory.getFlagBlacklist(ClaimFlag.LIQUID_FLOW.toString()).isEmpty();
        PORTAL_USE = !blacklistCategory.getFlagBlacklist(ClaimFlag.PORTAL_USE.toString()).isEmpty();
        PROJECTILE_IMPACT_BLOCK = !blacklistCategory.getFlagBlacklist(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString()).isEmpty();
        PROJECTILE_IMPACT_ENTITY = !blacklistCategory.getFlagBlacklist(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString()).isEmpty();

        blacklistMap.put(ClaimFlag.BLOCK_BREAK.toString(), blacklistCategory.blacklistBlockBreak);
        blacklistMap.put("block-notify", blacklistCategory.blacklistBlockNotify);
        blacklistMap.put(ClaimFlag.BLOCK_PLACE.toString(), blacklistCategory.blacklistBlockPlace);
        blacklistMap.put("block-pre", blacklistCategory.blacklistBlockPre);
        blacklistMap.put(ClaimFlag.COMMAND_EXECUTE.toString(), blacklistCategory.blacklistCommandExecute);
        blacklistMap.put(ClaimFlag.COMMAND_EXECUTE_PVP.toString(), blacklistCategory.blacklistCommandExecutePvp);
        blacklistMap.put(ClaimFlag.ENTER_CLAIM.toString(), blacklistCategory.blacklistEnterClaim);
        blacklistMap.put(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), blacklistCategory.blacklistEntityChunkSpawn);
        blacklistMap.put(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString(), blacklistCategory.blacklistEntityCollideBlock);
        blacklistMap.put(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), blacklistCategory.blacklistEntityCollideEntity);
        blacklistMap.put(ClaimFlag.ENTITY_DAMAGE.toString(), blacklistCategory.blacklistEntityDamage);
        blacklistMap.put(ClaimFlag.ENTITY_RIDING.toString(), blacklistCategory.blacklistEntityRiding);
        blacklistMap.put(ClaimFlag.ENTITY_SPAWN.toString(), blacklistCategory.blacklistEntitySpawn);
        blacklistMap.put(ClaimFlag.ENTITY_TELEPORT_FROM.toString(), blacklistCategory.blacklistEntityTeleportFrom);
        blacklistMap.put(ClaimFlag.ENTITY_TELEPORT_TO.toString(), blacklistCategory.blacklistEntityTeleportTo);
        blacklistMap.put(ClaimFlag.EXIT_CLAIM.toString(), blacklistCategory.blacklistExitClaim);
        blacklistMap.put(ClaimFlag.EXPLOSION.toString(), blacklistCategory.blacklistExplosion);
        blacklistMap.put(ClaimFlag.EXPLOSION_SURFACE.toString(), blacklistCategory.blacklistExplosionSurface);
        blacklistMap.put(ClaimFlag.FIRE_SPREAD.toString(), blacklistCategory.blacklistFireSpread);
        blacklistMap.put(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString(), blacklistCategory.blacklistInteractBlockPrimary);
        blacklistMap.put(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString(), blacklistCategory.blacklistInteractBlockSecondary);
        blacklistMap.put(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString(), blacklistCategory.blacklistInteractEntityPrimary);
        blacklistMap.put(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString(), blacklistCategory.blacklistInteractEntitySecondary);
        blacklistMap.put(ClaimFlag.INTERACT_INVENTORY.toString(), blacklistCategory.blacklistInteractInventory);
        blacklistMap.put(ClaimFlag.INTERACT_ITEM_PRIMARY.toString(), blacklistCategory.blacklistInteractItemPrimary);
        blacklistMap.put(ClaimFlag.INTERACT_ITEM_SECONDARY.toString(), blacklistCategory.blacklistInteractItemSecondary);
        blacklistMap.put(ClaimFlag.INTERACT_INVENTORY_CLICK.toString(), blacklistCategory.blacklistInteractInventoryClick);
        blacklistMap.put(ClaimFlag.ITEM_DROP.toString(), blacklistCategory.blacklistItemDrop);
        blacklistMap.put(ClaimFlag.ITEM_PICKUP.toString(), blacklistCategory.blacklistItemPickup);
        blacklistMap.put(ClaimFlag.ITEM_SPAWN.toString(), blacklistCategory.blacklistItemSpawn);
        blacklistMap.put(ClaimFlag.ITEM_USE.toString(), blacklistCategory.blacklistItemUse);
        blacklistMap.put(ClaimFlag.LEAF_DECAY.toString(), blacklistCategory.blacklistLeafDecay);
        blacklistMap.put(ClaimFlag.LIQUID_FLOW.toString(), blacklistCategory.blacklistLiquidFlow);
        blacklistMap.put(ClaimFlag.PORTAL_USE.toString(), blacklistCategory.blacklistPortalUse);
        blacklistMap.put(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString(), blacklistCategory.blacklistProjectileImpactBlock);
        blacklistMap.put(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), blacklistCategory.blacklistProjectileImpactEntity);
    }
}
