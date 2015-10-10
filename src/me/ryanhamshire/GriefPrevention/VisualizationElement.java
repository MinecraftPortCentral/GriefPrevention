/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;

//represents a "fake" block sent to a player as part of a visualization
public class VisualizationElement {

    public Location<Chunk> location;
    public BlockSnapshot visualizedBlock;
    public BlockSnapshot actualBlock;

    public VisualizationElement(Location<Chunk> location, BlockSnapshot visualBlock, BlockSnapshot actualBlock) {
        this.location = location;
        this.visualizedBlock = visualBlock;
        this.actualBlock = actualBlock;
    }
}
