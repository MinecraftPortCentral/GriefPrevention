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

    public static final BiMap<String, EnumCreatureType> SPAWN_TYPES = HashBiMap.create();

    static {
        SPAWN_TYPES.put("ambient", EnumCreatureType.AMBIENT);
        SPAWN_TYPES.put("animal", EnumCreatureType.CREATURE);
        SPAWN_TYPES.put("aquatic", EnumCreatureType.WATER_CREATURE);
        SPAWN_TYPES.put("monster", EnumCreatureType.MONSTER);
    }

    public static String getEntitySpawnFlag(ClaimFlag flag, String target) {
        switch (flag) {
            case ENTER_CLAIM:
            case ENTITY_COLLIDE_ENTITY:
            case ENTITY_DAMAGE:
            case ENTITY_RIDING:
            case ENTITY_SPAWN:
            case ENTITY_TELEPORT_FROM:
            case ENTITY_TELEPORT_TO:
            case EXIT_CLAIM:
            case INTERACT_ENTITY_PRIMARY:
            case INTERACT_ENTITY_SECONDARY:
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
                            claimFlag += "." + parts[0] + "." + creatureType + "." + parts[1];
                            return claimFlag;
                        } else {
                            claimFlag += "." + parts[0] + "." + parts[1];
                            return claimFlag;
                        }
                    }
                    // Unfortunately this is required until Pixelmon registers their entities correctly in FML
                    if (target.contains("pixelmon")) {
                        // If target was not found in registry, assume its a pixelmon animal
                        claimFlag += "." + parts[0] + ".animal." + parts[1];
                        return claimFlag;
                    }
                }
            default:
                return null;
        }
    }
}
