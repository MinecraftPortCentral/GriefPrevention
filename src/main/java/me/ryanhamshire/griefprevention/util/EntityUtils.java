/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package me.ryanhamshire.griefprevention.util;

import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.entity.Entity;

import java.util.UUID;

public class EntityUtils {

    public static UUID getOwnerUniqueId(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return null;
        }

        UUID ownerUniqueId = entity.getCreator().orElse(null);
        if (ownerUniqueId == null && entity instanceof IEntityOwnable) {
            IEntityOwnable ownable = (IEntityOwnable) entity;
            ownerUniqueId = ownable.getOwnerId();
        }

        return ownerUniqueId;
    }

    public static String getFriendlyName(net.minecraft.entity.Entity mcEntity) {
        String entityName = mcEntity.getName();
        final String[] parts = entityName.split(":");
        if (parts.length > 1) {
            entityName = parts[1];
        }
        if (entityName.contains(".")) {
            if ((entityName.indexOf(".") + 1) < entityName.length()) {
                entityName = entityName.substring(entityName.indexOf(".") + 1, entityName.length());
            }
        }

        entityName = entityName.replace("entity", "");
        entityName = entityName.replaceAll("[^A-Za-z0-9]", "");
        return entityName;
    }
}
