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

import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.GPFlags;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class ClaimDataFlagsCategory extends ConfigCategory {

    @Setting(value = GPFlags.BLOCK_BREAK, comment = GPFlags.COMMENT_BLOCK_BREAK)
    public Tristate blockBreak = Tristate.UNDEFINED;
    @Setting(value = GPFlags.BLOCK_COMMANDS, comment = GPFlags.COMMENT_BLOCK_COMMANDS)
    public List<String> blockCommands = new ArrayList<>();
    @Setting(value = GPFlags.BLOCK_PLACE, comment = GPFlags.COMMENT_BLOCK_PLACE)
    public Tristate blockPlace = Tristate.UNDEFINED;
    @Setting(value = GPFlags.DAMAGE_PASSIVES, comment = GPFlags.COMMENT_DAMAGE_PASSIVES)
    public Tristate damagePassives = Tristate.UNDEFINED;
    @Setting(value = GPFlags.EXPLOSIONS, comment = GPFlags.COMMENT_EXPLOSIONS)
    public Tristate explosions = Tristate.UNDEFINED;
    @Setting(value = GPFlags.FAREWELL_MESSAGE, comment = GPFlags.COMMENT_FAREWELL_MESSAGE)
    public Tristate farewellMessage = Tristate.UNDEFINED;
    @Setting(value = GPFlags.FIRE_SPREAD, comment = GPFlags.COMMENT_FIRE_SPREAD)
    public Tristate fireSpread = Tristate.UNDEFINED;
    @Setting(value = GPFlags.FORCE_DENY_ALL, comment = GPFlags.COMMENT_FORCE_DENY_ALL)
    public Tristate forceDenyAll = Tristate.UNDEFINED;
    @Setting(value = GPFlags.GREETING_MESSAGE, comment = GPFlags.COMMENT_GREETING_MESSAGE)
    public Tristate greetingMessage = Tristate.UNDEFINED;
    @Setting(value = GPFlags.INTERACT_PRIMARY, comment = GPFlags.COMMENT_INTERACT_PRIMARY)
    public Tristate interactPrimary = Tristate.UNDEFINED;
    @Setting(value = GPFlags.INTERACT_SECONDARY, comment = GPFlags.COMMENT_INTERACT_SECONDARY)
    public Tristate interactSecondary = Tristate.UNDEFINED;
    @Setting(value = GPFlags.INTERACT_INVENTORY, comment = GPFlags.COMMENT_INTERACT_INVENTORY)
    public Tristate interactInventory = Tristate.UNDEFINED;
    @Setting(value = GPFlags.ITEM_DROP, comment = GPFlags.COMMENT_ITEM_DROP)
    public Tristate itemDrop = Tristate.UNDEFINED;
    @Setting(value = GPFlags.ITEM_PICKUP, comment = GPFlags.COMMENT_ITEM_PICKUP)
    public Tristate itemPickup = Tristate.UNDEFINED;
    @Setting(value = GPFlags.ITEM_USE, comment = GPFlags.COMMENT_ITEM_USE)
    public Tristate itemUse = Tristate.UNDEFINED;
    @Setting(value = GPFlags.LAVA_FLOW, comment = GPFlags.COMMENT_LAVA_FLOW)
    public Tristate lavaFlow = Tristate.UNDEFINED;
    @Setting(value = GPFlags.MOB_BLOCK_DAMAGE, comment = GPFlags.COMMENT_MOB_BLOCK_DAMAGE)
    public Tristate mobBlockDamage = Tristate.UNDEFINED;
    @Setting(value = GPFlags.MOB_PLAYER_DAMAGE, comment = GPFlags.COMMENT_MOB_PLAYER_DAMAGE)
    public Tristate mobPlayerDamage = Tristate.UNDEFINED;
    @Setting(value = GPFlags.MOB_RIDING, comment = GPFlags.COMMENT_MOB_RIDING)
    public Tristate mobRiding = Tristate.UNDEFINED;
    @Setting(value = GPFlags.PORTAL_USE, comment = GPFlags.COMMENT_PORTAL_USE)
    public Tristate portalUse = Tristate.UNDEFINED;
    @Setting(value = GPFlags.PROJECTILES_ANY, comment = GPFlags.COMMENT_PROJECTILES_ANY)
    public Tristate projectilesAny = Tristate.UNDEFINED;
    @Setting(value = GPFlags.PROJECTILES_MONSTER, comment = GPFlags.COMMENT_PROJECTILES_MONSTER)
    public Tristate projectilesMonster = Tristate.UNDEFINED;
    @Setting(value = GPFlags.PROJECTILES_PLAYER, comment = GPFlags.COMMENT_PROJECTILES_PLAYER)
    public Tristate projectilesPlayer = Tristate.UNDEFINED;
    @Setting(value = GPFlags.PVP, comment = GPFlags.COMMENT_PVP)
    public Tristate pvp = Tristate.UNDEFINED;
    @Setting(value = GPFlags.SLEEP, comment = GPFlags.COMMENT_SLEEP)
    public Tristate sleep = Tristate.UNDEFINED;
    @Setting(value = GPFlags.SPAWN_AMBIENTS, comment = GPFlags.COMMENT_SPAWN_AMBIENTS)
    public Tristate spawnAmbient = Tristate.UNDEFINED;
    @Setting(value = GPFlags.SPAWN_ANIMALS, comment = GPFlags.COMMENT_SPAWN_ANIMALS)
    public Tristate spawnAnimals = Tristate.UNDEFINED;
    @Setting(value = GPFlags.SPAWN_ANY, comment = GPFlags.COMMENT_SPAWN_ANY)
    public Tristate spawnAny = Tristate.UNDEFINED;
    @Setting(value = GPFlags.SPAWN_AQUATICS, comment = GPFlags.COMMENT_SPAWN_AQUATICS)
    public Tristate spawnAquatic = Tristate.UNDEFINED;
    @Setting(value = GPFlags.SPAWN_MONSTERS, comment = GPFlags.COMMENT_SPAWN_MONSTERS)
    public Tristate spawnMonsters = Tristate.UNDEFINED;
    @Setting(value = GPFlags.VILLAGER_TRADING, comment = GPFlags.COMMENT_VILLAGER_TRADING)
    public Tristate villagerTrading = Tristate.UNDEFINED;
    @Setting(value = GPFlags.WATER_FLOW, comment = GPFlags.COMMENT_WATER_FLOW)
    public Tristate waterFlow = Tristate.UNDEFINED;

    public Map<String, Object> getFlagMap() {
        Map<String, Object> flagMap = Maps.newHashMap();
        flagMap.put(GPFlags.BLOCK_BREAK, this.blockBreak);
        flagMap.put(GPFlags.BLOCK_COMMANDS, this.blockCommands);
        flagMap.put(GPFlags.BLOCK_PLACE, this.blockPlace);
        flagMap.put(GPFlags.DAMAGE_PASSIVES, this.damagePassives);
        flagMap.put(GPFlags.EXPLOSIONS, this.explosions);
        flagMap.put(GPFlags.FIRE_SPREAD, this.fireSpread);
        flagMap.put(GPFlags.FORCE_DENY_ALL, this.forceDenyAll);
        flagMap.put(GPFlags.INTERACT_PRIMARY, this.interactPrimary);
        flagMap.put(GPFlags.INTERACT_SECONDARY, this.interactSecondary);
        flagMap.put(GPFlags.INTERACT_INVENTORY, this.interactInventory);
        flagMap.put(GPFlags.ITEM_DROP, this.itemDrop);
        flagMap.put(GPFlags.ITEM_PICKUP, this.itemPickup);
        flagMap.put(GPFlags.ITEM_USE, this.itemUse);
        flagMap.put(GPFlags.LAVA_FLOW, this.lavaFlow);
        flagMap.put(GPFlags.MOB_BLOCK_DAMAGE, this.mobBlockDamage);
        flagMap.put(GPFlags.MOB_PLAYER_DAMAGE, this.mobPlayerDamage);
        flagMap.put(GPFlags.MOB_RIDING, this.mobRiding);
        flagMap.put(GPFlags.PORTAL_USE, this.portalUse);
        flagMap.put(GPFlags.PROJECTILES_PLAYER, this.projectilesPlayer);
        flagMap.put(GPFlags.PROJECTILES_MONSTER, this.projectilesMonster);
        flagMap.put(GPFlags.PROJECTILES_ANY, this.projectilesAny);
        flagMap.put(GPFlags.PVP, this.pvp);
        flagMap.put(GPFlags.SPAWN_MONSTERS, this.spawnMonsters);
        flagMap.put(GPFlags.SPAWN_ANIMALS, this.spawnAnimals);
        flagMap.put(GPFlags.SPAWN_AMBIENTS, this.spawnAmbient);
        flagMap.put(GPFlags.SPAWN_AQUATICS, this.spawnAquatic);
        flagMap.put(GPFlags.SPAWN_ANY, this.spawnAny);
        flagMap.put(GPFlags.SLEEP, this.sleep);
        flagMap.put(GPFlags.WATER_FLOW, this.waterFlow);
        flagMap.put(GPFlags.VILLAGER_TRADING, this.villagerTrading);
        return flagMap;
    }

    @SuppressWarnings("unchecked")
    public void setFlagValue(String flag, Object value) {
        switch (flag) {
            case GPFlags.BLOCK_BREAK:
                this.blockBreak = (Tristate) value;
                return;
            case GPFlags.BLOCK_COMMANDS:
                this.blockCommands = (List<String>) value;
                return;
            case GPFlags.BLOCK_PLACE:
                this.blockPlace = (Tristate) value;
                return;
            case GPFlags.DAMAGE_PASSIVES:
                this.damagePassives = (Tristate) value;
                return;
            case GPFlags.EXPLOSIONS:
                this.explosions = (Tristate) value;
                return;
            case GPFlags.FAREWELL_MESSAGE:
                this.farewellMessage = (Tristate) value;
                return;
            case GPFlags.FIRE_SPREAD:
                this.fireSpread = (Tristate) value;
                return;
            case GPFlags.FORCE_DENY_ALL:
                this.forceDenyAll = (Tristate) value;
                return;
            case GPFlags.GREETING_MESSAGE:
                this.greetingMessage = (Tristate) value;
                return;
            case GPFlags.INTERACT_PRIMARY:
                this.interactPrimary = (Tristate) value;
                return;
            case GPFlags.INTERACT_SECONDARY:
                this.interactSecondary = (Tristate) value;
                return;
            case GPFlags.INTERACT_INVENTORY:
                this.interactInventory = (Tristate) value;
                return;
            case GPFlags.ITEM_DROP:
                this.itemDrop = (Tristate) value;
                return;
            case GPFlags.ITEM_PICKUP:
                this.itemPickup = (Tristate) value;
                return;
            case GPFlags.ITEM_USE:
                this.itemUse = (Tristate) value;
                return;
            case GPFlags.LAVA_FLOW:
                this.lavaFlow = (Tristate) value;
                return;
            case GPFlags.MOB_BLOCK_DAMAGE:
                this.mobBlockDamage = (Tristate) value;
                return;
            case GPFlags.MOB_PLAYER_DAMAGE:
                this.mobPlayerDamage = (Tristate) value;
                return;
            case GPFlags.MOB_RIDING:
                this.mobRiding = (Tristate) value;
                return;
            case GPFlags.PORTAL_USE:
                this.portalUse = (Tristate) value;
                return;
            case GPFlags.PROJECTILES_ANY:
                this.projectilesAny = (Tristate) value;
                return;
            case GPFlags.PROJECTILES_MONSTER:
                this.projectilesMonster = (Tristate) value;
                return;
            case GPFlags.PROJECTILES_PLAYER:
                this.projectilesPlayer = (Tristate) value;
                return;
            case GPFlags.PVP:
                this.pvp = (Tristate) value;
                return;
            case GPFlags.SLEEP:
                this.sleep = (Tristate) value;
                return;
            case GPFlags.SPAWN_AMBIENTS:
                this.spawnAmbient = (Tristate) value;
                return;
            case GPFlags.SPAWN_ANY:
                this.spawnAny = (Tristate) value;
                return;
            case GPFlags.SPAWN_AQUATICS:
                this.spawnAquatic = (Tristate) value;
                return;
            case GPFlags.SPAWN_MONSTERS:
                this.spawnMonsters = (Tristate) value;
                return;
            case GPFlags.SPAWN_ANIMALS:
                this.spawnAnimals = (Tristate) value;
                return;
            case GPFlags.WATER_FLOW:
                this.waterFlow = (Tristate) value;
                return;
            case GPFlags.VILLAGER_TRADING:
                this.villagerTrading = (Tristate) value;
                return;
            default:
                return;
        }
    }

    public Object getFlagValue(String flag) {
        switch (flag) {
            case GPFlags.BLOCK_BREAK:
                return this.blockBreak;
            case GPFlags.BLOCK_COMMANDS:
                return this.blockCommands;
            case GPFlags.BLOCK_PLACE:
                return this.blockPlace;
            case GPFlags.DAMAGE_PASSIVES:
                return this.damagePassives;
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
                return this.interactInventory;
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
            case GPFlags.WATER_FLOW:
                return this.waterFlow;
            case GPFlags.VILLAGER_TRADING:
                return this.villagerTrading;
            default:
                return null;
        }
    }

    public ClaimDataFlagsCategory copyFlags() {
        ClaimDataFlagsCategory flagsCopy = new ClaimDataFlagsCategory();
        for (Map.Entry<String, Object> mapEntry : this.getFlagMap().entrySet()) {
            flagsCopy.setFlagValue(mapEntry.getKey(), mapEntry.getValue());
        }
        return flagsCopy;
    }
}