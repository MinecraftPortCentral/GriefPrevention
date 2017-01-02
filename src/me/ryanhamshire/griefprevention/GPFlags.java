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
import net.minecraft.entity.EnumCreatureType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.common.entity.SpongeEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GPFlags {

    public static final List<String> FLAG_LIST = new ArrayList<>();

    public static final BiMap<String, EnumCreatureType> SPAWN_TYPES = HashBiMap.create();

    // Names
    public static final String BLOCK_BREAK = "block-break";
    public static final String BLOCK_PLACE = "block-place";
    public static final String COMMAND_EXECUTE = "command-execute";
    public static final String COMMAND_EXECUTE_PVP = "command-execute-pvp";
    public static final String ENTER_CLAIM = "enter-claim";
    public static final String ENTITY_COLLIDE_BLOCK = "collide-block";
    public static final String ENTITY_COLLIDE_ENTITY = "collide-entity";
    public static final String ENTITY_DAMAGE = "entity-damage";
    public static final String ENTITY_FALL = "entity-fall";
    public static final String ENTITY_RIDING = "entity-riding";
    public static final String ENTITY_SPAWN = "entity-spawn";
    public static final String ENTITY_TELEPORT_FROM = "entity-teleport-from";
    public static final String ENTITY_TELEPORT_TO = "entity-teleport-to";
    public static final String EXIT_CLAIM = "exit-claim";
    public static final String EXPLOSION = "explosion";
    public static final String EXPLOSION_SURFACE = "explosion-surface";
    public static final String FIRE_SPREAD = "fire-spread";
    public static final String INTERACT_BLOCK_PRIMARY = "interact-block-primary";
    public static final String INTERACT_BLOCK_SECONDARY = "interact-block-secondary";
    public static final String INTERACT_ENTITY_PRIMARY = "interact-entity-primary";
    public static final String INTERACT_ENTITY_SECONDARY = "interact-entity-secondary";
    public static final String INTERACT_INVENTORY = "interact-inventory";
    public static final String INTERACT_ITEM_PRIMARY = "interact-item-primary";
    public static final String INTERACT_ITEM_SECONDARY = "interact-item-secondary";
    public static final String ITEM_DROP = "item-drop";
    public static final String ITEM_PICKUP = "item-pickup";
    public static final String ITEM_SPAWN = "item-spawn";
    public static final String ITEM_USE = "item-use";
    public static final String LIQUID_FLOW = "liquid-flow";
    public static final String PORTAL_USE = "portal-use";
    public static final String PROJECTILE_IMPACT_BLOCK = "projectile-impact-block";
    public static final String PROJECTILE_IMPACT_ENTITY = "projectile-impact-entity";

    static {
        FLAG_LIST.add(GPFlags.BLOCK_BREAK);
        FLAG_LIST.add(GPFlags.BLOCK_PLACE);
        FLAG_LIST.add(GPFlags.COMMAND_EXECUTE);
        FLAG_LIST.add(GPFlags.COMMAND_EXECUTE_PVP);
        FLAG_LIST.add(GPFlags.ENTER_CLAIM);
        FLAG_LIST.add(GPFlags.ENTITY_COLLIDE_BLOCK);
        FLAG_LIST.add(GPFlags.ENTITY_COLLIDE_ENTITY);
        FLAG_LIST.add(GPFlags.ENTITY_DAMAGE);
        FLAG_LIST.add(GPFlags.ENTITY_FALL);
        FLAG_LIST.add(GPFlags.ENTITY_RIDING);
        FLAG_LIST.add(GPFlags.ENTITY_SPAWN);
        FLAG_LIST.add(GPFlags.ENTITY_TELEPORT_FROM);
        FLAG_LIST.add(GPFlags.ENTITY_TELEPORT_TO);
        FLAG_LIST.add(GPFlags.EXIT_CLAIM);
        FLAG_LIST.add(GPFlags.EXPLOSION);
        FLAG_LIST.add(GPFlags.EXPLOSION_SURFACE);
        FLAG_LIST.add(GPFlags.FIRE_SPREAD);
        FLAG_LIST.add(GPFlags.INTERACT_BLOCK_PRIMARY);
        FLAG_LIST.add(GPFlags.INTERACT_BLOCK_SECONDARY);
        FLAG_LIST.add(GPFlags.INTERACT_ENTITY_PRIMARY);
        FLAG_LIST.add(GPFlags.INTERACT_ENTITY_SECONDARY);
        FLAG_LIST.add(GPFlags.INTERACT_INVENTORY);
        FLAG_LIST.add(GPFlags.INTERACT_ITEM_PRIMARY);
        FLAG_LIST.add(GPFlags.INTERACT_ITEM_SECONDARY);
        FLAG_LIST.add(GPFlags.ITEM_DROP);
        FLAG_LIST.add(GPFlags.ITEM_PICKUP);
        FLAG_LIST.add(GPFlags.ITEM_SPAWN);
        FLAG_LIST.add(GPFlags.ITEM_USE);
        FLAG_LIST.add(GPFlags.LIQUID_FLOW);
        FLAG_LIST.add(GPFlags.PORTAL_USE);
        FLAG_LIST.add(GPFlags.PROJECTILE_IMPACT_BLOCK);
        FLAG_LIST.add(GPFlags.PROJECTILE_IMPACT_ENTITY);

        SPAWN_TYPES.put("ambient", EnumCreatureType.AMBIENT);
        SPAWN_TYPES.put("animal", EnumCreatureType.CREATURE);
        SPAWN_TYPES.put("aquatic", EnumCreatureType.WATER_CREATURE);
        SPAWN_TYPES.put("monster", EnumCreatureType.MONSTER);
    }

    public static String getEntitySpawnFlag(String flag, String target) {
        switch (flag) {
            case GPFlags.ENTER_CLAIM:
            case GPFlags.ENTITY_COLLIDE_ENTITY:
            case GPFlags.ENTITY_DAMAGE:
            case GPFlags.ENTITY_FALL:
            case GPFlags.ENTITY_RIDING:
            case GPFlags.ENTITY_SPAWN:
            case GPFlags.ENTITY_TELEPORT_FROM:
            case GPFlags.ENTITY_TELEPORT_TO:
            case GPFlags.EXIT_CLAIM:
            case GPFlags.INTERACT_ENTITY_PRIMARY:
            case GPFlags.INTERACT_ENTITY_SECONDARY:
            case GPFlags.PROJECTILE_IMPACT_ENTITY:
                // first check for valid SpawnType
                String[] parts = target.split(":");
                EnumCreatureType type = SPAWN_TYPES.get(parts[1]);
                if (type != null) {
                    flag += "." + parts[0] + "." + SPAWN_TYPES.inverse().get(type);
                    return flag;
                } else {
                    Optional<EntityType> entityType = Sponge.getRegistry().getType(EntityType.class, target);
                    if (entityType.isPresent()) {
                        SpongeEntityType spongeEntityType = (SpongeEntityType) entityType.get();
                        if (spongeEntityType.getEnumCreatureType() != null) {
                            String creatureType = SPAWN_TYPES.inverse().get(spongeEntityType.getEnumCreatureType());
                            flag += "." + parts[0] + "." + creatureType + "." + parts[1];
                            return flag;
                        } else {
                            flag += "." + parts[0] + "." + parts[1];
                            return flag;
                        }
                    }
                    // Unfortunately this is required until Pixelmon registers their entities correctly in FML
                    if (target.contains("pixelmon")) {
                        // If target was not found in registry, assume its a pixelmon animal
                        flag += "." + parts[0] + ".animal." + parts[1];
                        return flag;
                    }
                }
            default:
                return null;
        }
    }
}
