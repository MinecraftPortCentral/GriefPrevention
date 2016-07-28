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

    public static final Timing BLOCK_BREAK_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onBlockBreak");
    public static final Timing BLOCK_COLLIDE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onBlockCollide");
    public static final Timing BLOCK_NOTIFY_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onBlockNotify");
    public static final Timing BLOCK_PLACE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onBlockPlace");
    public static final Timing BLOCK_POST_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onBlockPost");
    public static final Timing BLOCK_PRE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onBlockPre");
    public static final Timing ENTITY_EXPLOSION_PRE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityExplosionPre");
    public static final Timing ENTITY_EXPLOSION_DETONATE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityExplosionDetonate");
    public static final Timing ENTITY_ATTACK_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityAttack");
    public static final Timing ENTITY_COLLIDE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityCollide");
    public static final Timing ENTITY_DAMAGE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityDamage");
    public static final Timing ENTITY_DAMAGE_MONITOR_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityDamageMonitor");
    public static final Timing ENTITY_DEATH_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityDeath");
    public static final Timing ENTITY_DROP_ITEM_DEATH_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityDropDeathItem");
    public static final Timing ENTITY_MOVE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityMove");
    public static final Timing ENTITY_SPAWN_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntitySpawn");
    public static final Timing ENTITY_TELEPORT_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onEntityTeleport");
    public static final Timing PLAYER_CHANGE_HELD_ITEM_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerChangeHeldItem");
    public static final Timing PLAYER_CHAT_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerChat");
    public static final Timing PLAYER_COMMAND_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerCommand");
    public static final Timing PLAYER_DEATH_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerDeath");
    public static final Timing PLAYER_DISPENSE_ITEM_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerDispenseItem");
    public static final Timing PLAYER_LOGIN_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerLogin");
    public static final Timing PLAYER_HANDLE_SHOVEL_ACTION = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerHandleShovelAction");
    public static final Timing PLAYER_INTERACT_BLOCK_PRIMARY_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerInteractBlockPrimary");
    public static final Timing PLAYER_INTERACT_BLOCK_SECONDARY_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerInteractBlockSecondary");
    public static final Timing PLAYER_INTERACT_ENTITY_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerInteractEntity");
    public static final Timing PLAYER_INVESTIGATE_CLAIM = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerInvestigateClaim");
    public static final Timing PLAYER_JOIN_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerJoin");
    public static final Timing PLAYER_KICK_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerKick");
    public static final Timing PLAYER_PICKUP_ITEM_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerPickupItem");
    public static final Timing PLAYER_QUIT_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerQuit");
    public static final Timing PLAYER_RESPAWN_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerRespawn");
    public static final Timing PLAYER_USE_ITEM_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onPlayerUseItem");
    public static final Timing SIGN_CHANGE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onSignChange");
    public static final Timing PROJECTILE_IMPACT_BLOCK_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onProjectileImpactBlock");
    public static final Timing PROJECTILE_IMPACT_ENTITY_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onProjectileImpactEntity");
    public static final Timing EXPLOSION_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onExplosion");
    public static final Timing CLAIM_GETCLAIM = Timings.of(GriefPrevention.instance.pluginContainer, "getClaimAt");
    public static final Timing WORLD_LOAD_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onWorldSave");
    public static final Timing WORLD_SAVE_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onWorldSave");
    public static final Timing WORLD_UNLOAD_EVENT = Timings.of(GriefPrevention.instance.pluginContainer, "onWorldSave");
}
