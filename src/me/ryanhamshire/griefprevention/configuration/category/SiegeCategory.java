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

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.item.ItemTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigSerializable
public class SiegeCategory extends ConfigCategory {

    @Setting(value = "enable-sieges", comment = "Whether sieges are allowed or not.")
    public boolean siegeEnabled = true;
    @Setting(value = "breakable-blocks", comment = "which blocks will be breakable in siege mode.")
    public List<String> breakableSiegeBlocks = new ArrayList<String>(
            Arrays.asList(ItemTypes.SAND.getId(), ItemTypes.GRAVEL.getId(), ItemTypes.GRASS.getId(),
                    ItemTypes.TALLGRASS.getId(), ItemTypes.GLASS.getId(), ItemTypes.DYE.getId(),
                    ItemTypes.SNOW.getId(), ItemTypes.STAINED_GLASS.getId(), ItemTypes.COBBLESTONE.getId()));
    @Setting(value = "winner-accessible-blocks", comment = "which blocks the siege winner can access in the loser's claim.")
    public List<String> winnerAccessibleBlocks = new ArrayList<String>();
           /* Arrays.asList(ItemTypes.ACACIA_DOOR.getId(), ItemTypes.ACACIA_FENCE.getId(), ItemTypes.ACACIA_FENCE_GATE.getId(),
                    ItemTypes.BIRCH_DOOR.getId(), ItemTypes.BIRCH_FENCE.getId(), ItemTypes.BIRCH_FENCE_GATE.getId(),
                    ItemTypes.DARK_OAK_DOOR.getId(), ItemTypes.DARK_OAK_FENCE.getId(), ItemTypes.DARK_OAK_FENCE_GATE.getId(),
                    ItemTypes.FENCE.getId(), ItemTypes.FENCE_GATE.getId(), ItemTypes.IRON_DOOR.getId(),
                    ItemTypes.IRON_TRAPDOOR.getId(), ItemTypes.WOODEN_DOOR.getId(), ItemTypes.STONE_BUTTON.getId(),
                    ItemTypes.WOODEN_BUTTON.getId(), ItemTypes.HEAVY_WEIGHTED_PRESSURE_PLATE.getId(),
                    ItemTypes.LIGHT_WEIGHTED_PRESSURE_PLATE.getId(),
                    ItemTypes.LEVER.getId()));*/
}