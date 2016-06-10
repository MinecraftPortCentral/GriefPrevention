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

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.Set;

public class GPFlags {

    // Names
    public static final String BLOCK_BREAK = "block-break";
    public static final String BLOCK_COMMANDS = "block-commands";
    public static final String BLOCK_PLACE = "block-place";
    public static final String DAMAGE_PASSIVES = "damage-passives";
    public static final String EXPLOSIONS = "explosions";
    public static final String FAREWELL_MESSAGE = "farewell-message";
    public static final String FIRE_SPREAD = "fire-spread";
    public static final String FORCE_DENY_ALL = "force-deny-all";
    public static final String GREETING_MESSAGE = "greeting-message";
    public static final String IGNITE = "ignite";
    public static final String INTERACT_PRIMARY = "interact-primary";
    public static final String INTERACT_SECONDARY = "interact-secondary";
    public static final String INTERACT_INVENTORY = "interact-inventory";
    public static final String ITEM_DROP = "item-drop";
    public static final String ITEM_PICKUP = "item-pickup";
    public static final String ITEM_USE = "item-use";
    public static final String LAVA_FLOW = "lava-flow";
    public static final String MOB_BLOCK_DAMAGE = "mob-block-damage";
    public static final String MOB_PLAYER_DAMAGE = "mob-player-damage";
    public static final String MOB_RIDING = "mob-riding";
    public static final String PORTAL_USE = "portal-use";
    public static final String PROJECTILES_ANY = "projectiles-any";
    public static final String PROJECTILES_MONSTER = "projectiles-monster";
    public static final String PROJECTILES_PLAYER = "projectiles-player";
    public static final String PVP = "pvp";
    public static final String SLEEP = "sleep";
    public static final String SPAWN_AMBIENTS = "spawn-ambient";
    public static final String SPAWN_ANY = "spawn-any";
    public static final String SPAWN_AQUATICS = "spawn-aquatic";
    public static final String SPAWN_MONSTERS = "spawn-monsters";
    public static final String SPAWN_ANIMALS = "spawn-animals";
    public static final String WATER_FLOW = "water-flow";
    public static final String VILLAGER_TRADING = "villager-trading";

    // Comments
    public static final String COMMENT_BLOCK_BREAK = "Allow/deny breaking blocks.";
    public static final String COMMENT_BLOCK_COMMANDS = "Blocked commands.";
    public static final String COMMENT_BLOCK_PLACE = "Allow/deny placing blocks.";
    public static final String COMMENT_DAMAGE_PASSIVES = "Allow/deny players damaging passive entities.";
    public static final String COMMENT_EXPLOSIONS = "Allow/deny explosions.";
    public static final String COMMENT_FAREWELL_MESSAGE = "Allow/deny farewell message.";
    public static final String COMMENT_FIRE_SPREAD = "Allow/deny fire spread.";
    public static final String COMMENT_FORCE_DENY_ALL = "Only intended if you want to explicitly ignore all checking for player permissions.";
    public static final String COMMENT_GREETING_MESSAGE = "Allow/deny greeting message.";
    public static final String COMMENT_IGNITE = "Allow/deny ignites.";
    public static final String COMMENT_INTERACT_PRIMARY = "Allow/deny left-clicking.";
    public static final String COMMENT_INTERACT_SECONDARY = "Allow/deny right-clicking.";
    public static final String COMMENT_INTERACT_INVENTORY = "Allow/deny blocks with inventories.";
    public static final String COMMENT_ITEM_DROP = "Allow/deny item drops.";
    public static final String COMMENT_ITEM_PICKUP = "Allow/deny picking up items.";
    public static final String COMMENT_ITEM_USE = "Allow/deny item use.";
    public static final String COMMENT_LAVA_FLOW = "Allow/deny lava flow.";
    public static final String COMMENT_MOB_BLOCK_DAMAGE = "Allow/deny mob block damage.";
    public static final String COMMENT_MOB_PLAYER_DAMAGE = "Allow/deny mob player damage.";
    public static final String COMMENT_MOB_RIDING = "Allow/deny mob riding.";
    public static final String COMMENT_PORTAL_USE = "Allow/deny portal use.";
    public static final String COMMENT_PROJECTILES_ANY = "Allow/deny any projectiles.";
    public static final String COMMENT_PROJECTILES_MONSTER = "Allow/deny monster projectiles.";
    public static final String COMMENT_PROJECTILES_PLAYER = "Allow/deny player projectiles.";
    public static final String COMMENT_PVP = "Allow/deny pvp.";
    public static final String COMMENT_SLEEP = "Allow/deny sleep.";
    public static final String COMMENT_SPAWN_AMBIENTS = "Allow/deny the spawning of ambients.";
    public static final String COMMENT_SPAWN_ANIMALS = "Allow/deny the spawning of animals.";
    public static final String COMMENT_SPAWN_ANY = "Allow/deny the spawning of any entities.";
    public static final String COMMENT_SPAWN_AQUATICS = "Allow/deny the spawning of aquatics.";
    public static final String COMMENT_SPAWN_MONSTERS = "Allow/deny the spawning of monsters.";
    public static final String COMMENT_WATER_FLOW = "Allow/deny water flow.";
    public static final String COMMENT_VILLAGER_TRADING = "Allow/deny villager trading.";

    public static Tristate getClaimFlagPermission(User user, Claim claim, String flag) {
        if (claim.hasFullAccess(user)) {
            return Tristate.TRUE;
        }

        if (GriefPrevention.instance.permPluginInstalled) {
            Set<Context> contextSet = ImmutableSet.of(claim.getContext());
            Tristate value = user.getPermissionValue(contextSet, flag);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        }

        return getClaimFlagPermission(claim, flag);
    }

    // No user
    public static Tristate getClaimFlagPermission(Claim claim, String flag) {
        if (claim == null) {
            return Tristate.UNDEFINED;
        }
        flag = flag.replace("griefprevention.flag.", "");
        Tristate value = (Tristate) claim.getClaimData().getFlags().getFlagValue(flag);

        if (value == Tristate.UNDEFINED) {
            Object obj = GriefPrevention.getActiveConfig(claim.world.getProperties()).getConfig().flags.getFlagValue(flag);
            if (obj != null) {
                return Tristate.fromBoolean((boolean) obj);
            }
        }

        if (value != null) {
            return value;
        }

        return Tristate.TRUE;
    }
}
