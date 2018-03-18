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

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;

public class GPTimings {

    public static final Timing BLOCK_BREAK_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onBlockBreak");
    public static final Timing BLOCK_COLLIDE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onBlockCollide");
    public static final Timing BLOCK_NOTIFY_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onBlockNotify");
    public static final Timing BLOCK_PLACE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onBlockPlace");
    public static final Timing BLOCK_POST_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onBlockPost");
    public static final Timing BLOCK_PRE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onBlockPre");
    public static final Timing ENTITY_EXPLOSION_PRE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityExplosionPre");
    public static final Timing ENTITY_EXPLOSION_DETONATE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityExplosionDetonate");
    public static final Timing ENTITY_ATTACK_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityAttack");
    public static final Timing ENTITY_COLLIDE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityCollide");
    public static final Timing ENTITY_DAMAGE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityDamage");
    public static final Timing ENTITY_DAMAGE_MONITOR_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityDamageMonitor");
    public static final Timing ENTITY_DEATH_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityDeath");
    public static final Timing ENTITY_DROP_ITEM_DEATH_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityDropDeathItem");
    public static final Timing ENTITY_MOVE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityMove");
    public static final Timing ENTITY_SPAWN_PRE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntitySpawnPre");
    public static final Timing ENTITY_SPAWN_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntitySpawn");
    public static final Timing ENTITY_TELEPORT_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onEntityTeleport");
    public static final Timing PLAYER_CHANGE_HELD_ITEM_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerChangeHeldItem");
    public static final Timing PLAYER_CHAT_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerChat");
    public static final Timing PLAYER_COMMAND_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerCommand");
    public static final Timing PLAYER_DEATH_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerDeath");
    public static final Timing PLAYER_DISPENSE_ITEM_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerDispenseItem");
    public static final Timing PLAYER_LOGIN_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerLogin");
    public static final Timing PLAYER_HANDLE_SHOVEL_ACTION = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerHandleShovelAction");
    public static final Timing PLAYER_INTERACT_BLOCK_PRIMARY_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInteractBlockPrimary");
    public static final Timing PLAYER_INTERACT_BLOCK_SECONDARY_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInteractBlockSecondary");
    public static final Timing PLAYER_INTERACT_ENTITY_PRIMARY_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInteractEntityPrimary");
    public static final Timing PLAYER_INTERACT_ENTITY_SECONDARY_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInteractEntitySecondary");
    public static final Timing PLAYER_INTERACT_INVENTORY_CLICK_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInteractInventoryClick");
    public static final Timing PLAYER_INTERACT_INVENTORY_CLOSE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInteractInventoryClose");
    public static final Timing PLAYER_INTERACT_INVENTORY_OPEN_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInteractInventoryOpen");
    public static final Timing PLAYER_INVESTIGATE_CLAIM = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerInvestigateClaim");
    public static final Timing PLAYER_JOIN_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerJoin");
    public static final Timing PLAYER_KICK_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerKick");
    public static final Timing PLAYER_PICKUP_ITEM_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerPickupItem");
    public static final Timing PLAYER_QUIT_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerQuit");
    public static final Timing PLAYER_RESPAWN_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerRespawn");
    public static final Timing PLAYER_USE_ITEM_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onPlayerUseItem");
    public static final Timing SIGN_CHANGE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onSignChange");
    public static final Timing PROJECTILE_IMPACT_BLOCK_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onProjectileImpactBlock");
    public static final Timing PROJECTILE_IMPACT_ENTITY_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onProjectileImpactEntity");
    public static final Timing EXPLOSION_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onExplosion");
    public static final Timing CLAIM_GETCLAIM = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "getClaimAt");
    public static final Timing WORLD_LOAD_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onWorldSave");
    public static final Timing WORLD_SAVE_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onWorldSave");
    public static final Timing WORLD_UNLOAD_EVENT = Timings.of(GriefPreventionPlugin.instance.pluginContainer, "onWorldSave");
}
