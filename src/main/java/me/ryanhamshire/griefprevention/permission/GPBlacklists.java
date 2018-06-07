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

public class GPBlacklists {

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
    public static boolean LIQUID_FLOW;
    public static boolean PORTAL_USE;
    public static boolean PROJECTILE_IMPACT_BLOCK;
    public static boolean PROJECTILE_IMPACT_ENTITY;

    public static void populateBlacklistStatus() {
        final BlacklistCategory blacklist = GriefPreventionPlugin.getGlobalConfig().getConfig().blacklist;
        GLOBAL_SOURCE = !blacklist.getGlobalSourceBlacklist().isEmpty();
        GLOBAL_TARGET = !blacklist.getGlobalTargetBlacklist().isEmpty();
        BLOCK_BREAK = !blacklist.getFlagBlacklist(ClaimFlag.BLOCK_BREAK.toString()).isEmpty();
        BLOCK_NOTIFY = !blacklist.getFlagBlacklist("block-notify").isEmpty();
        BLOCK_PLACE = !blacklist.getFlagBlacklist(ClaimFlag.BLOCK_PLACE.toString()).isEmpty();
        BLOCK_PRE = !blacklist.getFlagBlacklist("block-pre").isEmpty();
        COMMAND_EXECUTE = !blacklist.getFlagBlacklist(ClaimFlag.COMMAND_EXECUTE.toString()).isEmpty();
        COMMAND_EXECUTE_PVP = !blacklist.getFlagBlacklist(ClaimFlag.COMMAND_EXECUTE_PVP.toString()).isEmpty();
        ENTER_CLAIM = !blacklist.getFlagBlacklist(ClaimFlag.ENTER_CLAIM.toString()).isEmpty();
        ENTITY_CHUNK_SPAWN = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_CHUNK_SPAWN.toString()).isEmpty();
        ENTITY_COLLIDE_BLOCK = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString()).isEmpty();
        ENTITY_COLLIDE_ENTITY = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString()).isEmpty();
        ENTITY_DAMAGE = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_DAMAGE.toString()).isEmpty();
        ENTITY_RIDING = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_RIDING.toString()).isEmpty();
        ENTITY_SPAWN = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_SPAWN.toString()).isEmpty();
        ENTITY_TELEPORT_FROM = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_TELEPORT_FROM.toString()).isEmpty();
        ENTITY_TELEPORT_TO = !blacklist.getFlagBlacklist(ClaimFlag.ENTITY_TELEPORT_TO.toString()).isEmpty();
        EXIT_CLAIM = !blacklist.getFlagBlacklist(ClaimFlag.EXIT_CLAIM.toString()).isEmpty();
        EXPLOSION = !blacklist.getFlagBlacklist(ClaimFlag.EXPLOSION.toString()).isEmpty();
        EXPLOSION_SURFACE = !blacklist.getFlagBlacklist(ClaimFlag.EXPLOSION_SURFACE.toString()).isEmpty();
        FIRE_SPREAD = !blacklist.getFlagBlacklist(ClaimFlag.FIRE_SPREAD.toString()).isEmpty();
        INTERACT_BLOCK_PRIMARY = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString()).isEmpty();
        INTERACT_BLOCK_SECONDARY = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString()).isEmpty();
        INTERACT_ENTITY_PRIMARY = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString()).isEmpty();
        INTERACT_ENTITY_SECONDARY = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString()).isEmpty();
        INTERACT_INVENTORY = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_INVENTORY.toString()).isEmpty();
        INTERACT_ITEM_PRIMARY = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_ITEM_PRIMARY.toString()).isEmpty();
        INTERACT_ITEM_SECONDARY = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_ITEM_SECONDARY.toString()).isEmpty();
        INTERACT_INVENTORY_CLICK = !blacklist.getFlagBlacklist(ClaimFlag.INTERACT_INVENTORY_CLICK.toString()).isEmpty();
        ITEM_DROP = !blacklist.getFlagBlacklist(ClaimFlag.ITEM_DROP.toString()).isEmpty();
        ITEM_PICKUP = !blacklist.getFlagBlacklist(ClaimFlag.ITEM_PICKUP.toString()).isEmpty();
        ITEM_SPAWN = !blacklist.getFlagBlacklist(ClaimFlag.ITEM_SPAWN.toString()).isEmpty();
        ITEM_USE = !blacklist.getFlagBlacklist(ClaimFlag.ITEM_USE.toString()).isEmpty();
        LIQUID_FLOW = !blacklist.getFlagBlacklist(ClaimFlag.LIQUID_FLOW.toString()).isEmpty();
        PORTAL_USE = !blacklist.getFlagBlacklist(ClaimFlag.PORTAL_USE.toString()).isEmpty();
        PROJECTILE_IMPACT_BLOCK = !blacklist.getFlagBlacklist(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString()).isEmpty();
        PROJECTILE_IMPACT_ENTITY = !blacklist.getFlagBlacklist(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString()).isEmpty();
    }
}
