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
package me.ryanhamshire.griefprevention.configuration.category;

import me.ryanhamshire.griefprevention.GPFlags;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class FlagDefaultsCategory extends ConfigCategory {

    @Setting(value = GPFlags.BLOCK_BREAK, comment = GPFlags.COMMENT_BLOCK_BREAK)
    public boolean blockBreak = false;
    @Setting(value = GPFlags.BLOCK_COMMANDS, comment = GPFlags.COMMENT_BLOCK_COMMANDS)
    public List<String> blockCommands = new ArrayList<>();
    @Setting(value = GPFlags.BLOCK_PLACE, comment = GPFlags.COMMENT_BLOCK_PLACE)
    public boolean blockPlace = false;
    @Setting(value = GPFlags.DAMAGE_PASSIVES, comment = GPFlags.COMMENT_DAMAGE_PASSIVES)
    public boolean damagePassives = false;
    @Setting(value = GPFlags.EXPLOSIONS, comment = GPFlags.COMMENT_EXPLOSIONS)
    public boolean explosions = false;
    @Setting(value = GPFlags.FAREWELL_MESSAGE, comment = GPFlags.COMMENT_FAREWELL_MESSAGE)
    public boolean farewellMessage = true;
    @Setting(value = GPFlags.FIRE_SPREAD, comment = GPFlags.COMMENT_FIRE_SPREAD)
    public boolean fireSpread = false;
    @Setting(value = GPFlags.FORCE_DENY_ALL, comment = GPFlags.COMMENT_FORCE_DENY_ALL)
    public boolean forceDenyAll = false;
    @Setting(value = GPFlags.GREETING_MESSAGE, comment = GPFlags.COMMENT_GREETING_MESSAGE)
    public boolean greetingMessage = true;
    @Setting(value = GPFlags.INTERACT_PRIMARY, comment = GPFlags.COMMENT_INTERACT_PRIMARY)
    public boolean interactPrimary = false;
    @Setting(value = GPFlags.INTERACT_SECONDARY, comment = GPFlags.COMMENT_INTERACT_SECONDARY)
    public boolean interactSecondary = false;
    @Setting(value = GPFlags.INTERACT_INVENTORY, comment = GPFlags.COMMENT_INTERACT_INVENTORY)
    public boolean inventory = false;
    @Setting(value = GPFlags.ITEM_DROP, comment = GPFlags.COMMENT_ITEM_DROP)
    public boolean itemDrop = true;
    @Setting(value = GPFlags.ITEM_PICKUP, comment = GPFlags.COMMENT_ITEM_PICKUP)
    public boolean itemPickup = false;
    @Setting(value = GPFlags.ITEM_USE, comment = GPFlags.COMMENT_ITEM_USE)
    public boolean itemUse = false;
    @Setting(value = GPFlags.LAVA_FLOW, comment = GPFlags.COMMENT_LAVA_FLOW)
    public boolean lavaFlow = true;
    @Setting(value = GPFlags.MOB_BLOCK_DAMAGE, comment = GPFlags.COMMENT_MOB_BLOCK_DAMAGE)
    public boolean mobBlockDamage = false;
    @Setting(value = GPFlags.MOB_PLAYER_DAMAGE, comment = GPFlags.COMMENT_MOB_PLAYER_DAMAGE)
    public boolean mobPlayerDamage = true;
    @Setting(value = GPFlags.MOB_RIDING, comment = GPFlags.COMMENT_MOB_RIDING)
    public boolean mobRiding = false;
    @Setting(value = GPFlags.PORTAL_USE, comment = GPFlags.COMMENT_PORTAL_USE)
    public boolean portalUse = true;
    @Setting(value = GPFlags.PROJECTILES_ANY, comment = GPFlags.COMMENT_PROJECTILES_ANY)
    public boolean projectilesAny = true;
    @Setting(value = GPFlags.PROJECTILES_MONSTER, comment = GPFlags.COMMENT_PROJECTILES_MONSTER)
    public boolean projectilesMonster = true;
    @Setting(value = GPFlags.PROJECTILES_PLAYER, comment = GPFlags.COMMENT_PROJECTILES_PLAYER)
    public boolean projectilesPlayer = false;
    @Setting(value = GPFlags.PVP, comment = GPFlags.COMMENT_PVP)
    public boolean pvp = false;
    @Setting(value = GPFlags.SLEEP, comment = GPFlags.COMMENT_SLEEP)
    public boolean sleep = true;
    @Setting(value = GPFlags.SPAWN_AMBIENTS, comment = GPFlags.COMMENT_SPAWN_AMBIENTS)
    public boolean spawnAmbient = true;
    @Setting(value = GPFlags.SPAWN_ANIMALS, comment = GPFlags.COMMENT_SPAWN_ANIMALS)
    public boolean spawnAnimals = true;
    @Setting(value = GPFlags.SPAWN_ANY, comment = GPFlags.COMMENT_SPAWN_ANY)
    public boolean spawnAny = true;
    @Setting(value = GPFlags.SPAWN_AQUATICS, comment = GPFlags.COMMENT_SPAWN_AQUATICS)
    public boolean spawnAquatic = true;
    @Setting(value = GPFlags.SPAWN_MONSTERS, comment = GPFlags.COMMENT_SPAWN_MONSTERS)
    public boolean spawnMonsters = true;
    @Setting(value = GPFlags.VILLAGER_TRADING, comment = GPFlags.COMMENT_VILLAGER_TRADING)
    public boolean villagerTrading = false;
    @Setting(value = GPFlags.WATER_FLOW, comment = GPFlags.COMMENT_WATER_FLOW)
    public boolean waterFlow = true;

    public Object getFlagValue(String flag) {
        switch (flag) {
            case GPFlags.BLOCK_BREAK:
                return this.blockBreak;
            case GPFlags.BLOCK_COMMANDS:
                return this.blockCommands;
            case GPFlags.BLOCK_PLACE:
                return this.blockPlace;
            case GPFlags.EXPLOSIONS:
                return this.explosions;
            case GPFlags.FAREWELL_MESSAGE:
                return this.farewellMessage;
            case GPFlags.FIRE_SPREAD:
                return this.fireSpread;
            case GPFlags.FORCE_DENY_ALL:
                return this.forceDenyAll;
            case GPFlags.GREETING_MESSAGE:
                return this.greetingMessage;
            case GPFlags.INTERACT_PRIMARY:
                return this.interactPrimary;
            case GPFlags.INTERACT_SECONDARY:
                return this.interactSecondary;
            case GPFlags.INTERACT_INVENTORY:
                return this.inventory;
            case GPFlags.ITEM_DROP:
                return this.itemDrop;
            case GPFlags.ITEM_PICKUP:
                return this.itemPickup;
            case GPFlags.ITEM_USE:
                return this.itemUse;
            case GPFlags.LAVA_FLOW:
                return this.lavaFlow;
            case GPFlags.MOB_BLOCK_DAMAGE:
                return this.mobBlockDamage;
            case GPFlags.MOB_PLAYER_DAMAGE:
                return this.mobPlayerDamage;
            case GPFlags.MOB_RIDING:
                return this.mobRiding;
            case GPFlags.PORTAL_USE:
                return this.portalUse;
            case GPFlags.PROJECTILES_ANY:
                return this.projectilesAny;
            case GPFlags.PROJECTILES_MONSTER:
                return this.projectilesMonster;
            case GPFlags.PROJECTILES_PLAYER:
                return this.projectilesPlayer;
            case GPFlags.PVP:
                return this.pvp;
            case GPFlags.SLEEP:
                return this.sleep;
            case GPFlags.SPAWN_AMBIENTS:
                return this.spawnAmbient;
            case GPFlags.SPAWN_ANY:
                return this.spawnAny;
            case GPFlags.SPAWN_AQUATICS:
                return this.spawnAquatic;
            case GPFlags.SPAWN_MONSTERS:
                return this.spawnMonsters;
            case GPFlags.SPAWN_ANIMALS:
                return this.spawnAnimals;
            case GPFlags.VILLAGER_TRADING:
                return this.villagerTrading;
            case GPFlags.WATER_FLOW:
                return this.waterFlow;
            default:
                return null;
        }
    }
}