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
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;

//this main thread task takes the output from the RestoreNatureProcessingTask\
//and updates the world accordingly
class RestoreNatureExecutionTask implements Runnable {

    // results from processing thread
    // will be applied to the world
    private BlockSnapshot[][][] snapshots;

    // boundaries for changes
    private int miny;
    private Location<World> lesserCorner;
    private Location<World> greaterCorner;

    // player who should be notified about the result (will see a visualization
    // when the restoration is complete)
    private Player player;

    public RestoreNatureExecutionTask(BlockSnapshot[][][] snapshots, int miny, Location<World> lesserCorner, Location<World> greaterCorner,
            Player player) {
        this.snapshots = snapshots;
        this.miny = miny;
        this.lesserCorner = lesserCorner;
        this.greaterCorner = greaterCorner;
        this.player = player;
    }

    @Override
    public void run() {
        // apply changes to the world, but ONLY to unclaimed blocks
        // note that the edge of the results is not applied (the 1-block-wide
        // band around the outside of the chunk)
        // those data were sent to the processing thread for referernce
        // purposes, but aren't part of the area selected for restoration
        Claim cachedClaim = null;
        for (int x = 1; x < this.snapshots.length - 1; x++) {
            for (int z = 1; z < this.snapshots[0][0].length - 1; z++) {
                for (int y = this.miny; y < this.snapshots[0].length; y++) {
                    BlockSnapshot blockUpdate = this.snapshots[x][y][z];
                    BlockState currentBlock = blockUpdate.getLocation().get().getBlock();
                    if (blockUpdate.getState() != currentBlock) {
                        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(blockUpdate.getLocation().get(), false, cachedClaim);
                        if (claim != null) {
                            cachedClaim = claim;
                            break;
                        }

                        blockUpdate.getLocation().get().setBlock(blockUpdate.getState());
                    }
                }
            }
        }

        // clean up any entities in the chunk, ensure no players are suffocated
        Chunk chunk = this.lesserCorner.getChunk();
        Entity[] entities = chunk.getEntities();
        for (int i = 0; i < entities.length; i++) {
            Entity entity = entities[i];
            if (!(entity instanceof Player || entity instanceof Animals)) {
                // hanging entities (paintings, item frames) are protected when
                // they're in land claims
                if (!(entity instanceof Hanging) || GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, null) == null) {
                    // everything else is removed
                    entity.remove();
                }
            }

            // for players, always ensure there's air where the player is
            // standing
            else {
                BlockType feetBlock = entity.getLocation().getBlock();
                feetBlock.setType(BlockTypes.AIR);
                feetBlock.getRelative(BlockFace.UP).setType(Material.AIR);
            }
        }

        // show visualization to player who started the restoration
        if (player != null) {
            Claim claim = new Claim(lesserCorner, greaterCorner, null, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(),
                    new ArrayList<String>(), null);
            Visualization visualization =
                    Visualization.FromClaim(claim, player.getLocation().getBlockY(), VisualizationType.RestoreNature, player.getLocation());
            Visualization.Apply(player, visualization);
        }
    }
}
