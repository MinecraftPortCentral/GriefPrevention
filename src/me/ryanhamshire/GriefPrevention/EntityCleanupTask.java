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

import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;

//FEATURE: creative mode worlds get a regular entity cleanup

//this main thread task revisits the location of a partially chopped tree from several minutes ago
//if any part of the tree is still there and nothing else has been built in its place, remove the remaining parts
class EntityCleanupTask implements Runnable {

    // where to start cleaning in the list of entities
    private double percentageStart;

    public EntityCleanupTask(double percentageStart) {
        this.percentageStart = percentageStart;
    }

    @Override
    public void run() {
        ArrayList<World> worlds = new ArrayList<World>();
        for (World world : GriefPrevention.instance.game.getServer().getWorlds()) {
            if (GriefPrevention.instance.config_claims_worldModes.get(world) == ClaimsMode.Creative) {
                worlds.add(world);
            }
        }

        for (int i = 0; i < worlds.size(); i++) {
            World world = worlds.get(i);

            List<Entity> entities = (List<Entity>) world.getEntities();

            // starting and stopping point. each execution of the task scans 10%
            // of the server's (loaded) entities
            int j = (int) (entities.size() * this.percentageStart);
            int k = (int) (entities.size() * (this.percentageStart + .1));
            Claim cachedClaim = null;
            for (; j < entities.size() && j < k; j++) {
                Entity entity = entities.get(j);

                boolean remove = false;
                if (entity instanceof Boat) // boats must be occupied
                {
                    Boat boat = (Boat) entity;
                    if (boat.get(Keys.PASSENGER).isPresent())
                        remove = true;
                }

                else if (entity.get(Keys.VEHICLE).isPresent()) {
                    Entity vehicle = entity.get(Keys.VEHICLE).get();

                    // minecarts in motion must be occupied by a player
                    if (vehicle.get(Keys.VELOCITY).get().lengthSquared() != 0) {
                        if (!vehicle.get(Keys.PASSENGER).isPresent() || !(vehicle.get(Keys.PASSENGER).get() instanceof Player)) {
                            remove = true;
                        }
                    }

                    // stationary carts must be on rails
                    else {
                        BlockType material = world.getBlockType(vehicle.getLocation().getBlockPosition());
                        if (material != BlockTypes.RAIL && material != BlockTypes.GOLDEN_RAIL && material != BlockTypes.DETECTOR_RAIL) {
                            remove = true;
                        }
                    }
                }

                // all non-player entities must be in claims
                else if (!(entity instanceof Player)) {
                    Claim claim = GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, cachedClaim);
                    if (claim != null) {
                        cachedClaim = claim;
                    } else {
                        remove = true;
                    }
                }

                if (remove) {
                    entity.remove();
                }
            }
        }

        // starting and stopping point. each execution of the task scans 5% of
        // the server's claims
        List<Claim> claims = GriefPrevention.instance.dataStore.claims;
        int j = (int) (claims.size() * this.percentageStart);
        int k = (int) (claims.size() * (this.percentageStart + .05));
        for (; j < claims.size() && j < k; j++) {
            Claim claim = claims.get(j);

            // if it's a creative mode claim
            if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                // check its entity count and remove any extras
                claim.allowMoreEntities();
            }
        }

        // schedule the next run of this task, in 3 minutes (20L is
        // approximately 1 second)
        double nextRunPercentageStart = this.percentageStart + .05;
        if (nextRunPercentageStart > .99) {
            nextRunPercentageStart = 0;
        }

        EntityCleanupTask task = new EntityCleanupTask(nextRunPercentageStart);
        GriefPrevention.instance.game.getScheduler().createTaskBuilder().delay(20L * 60 * 1).execute(task).submit(GriefPrevention.instance);
    }
}
