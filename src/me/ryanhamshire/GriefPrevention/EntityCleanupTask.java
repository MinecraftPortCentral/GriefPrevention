/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
package me.ryanhamshire.GriefPrevention;

//FEATURE: creative mode worlds get a regular entity cleanup

//this main thread task revisits the location of a partially chopped tree from several minutes ago
//if any part of the tree is still there and nothing else has been built in its place, remove the remaining parts
class EntityCleanupTask implements Runnable {

    @Override
    public void run() {
        // TODO - temp disabled
    }

    // where to start cleaning in the list of entities
   /* private double percentageStart;

    public EntityCleanupTask(double percentageStart) {
        this.percentageStart = percentageStart;
    }

    @Override
    public void run() {
        ArrayList<World> worlds = new ArrayList<World>();
        for (World world : Sponge.getGame().getServer().getWorlds()) {
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

        // schedule the next run of this task, in 1 minute
        double nextRunPercentageStart = this.percentageStart + .05;
        if (nextRunPercentageStart > .99) {
            nextRunPercentageStart = 0;
        }

        EntityCleanupTask task = new EntityCleanupTask(nextRunPercentageStart);
        Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(task).submit(GriefPrevention.instance);
    }*/
}
