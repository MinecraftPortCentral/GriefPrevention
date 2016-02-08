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

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.concurrent.TimeUnit;

//checks to see whether or not a siege should end based on the locations of the players
//for example, defender escaped or attacker gave up and left
class SiegeCheckupTask implements Runnable {

    private SiegeData siegeData;

    public SiegeCheckupTask(SiegeData siegeData) {
        this.siegeData = siegeData;
    }

    @Override
    public void run() {
        DataStore dataStore = GriefPrevention.instance.dataStore;
        Player defender = this.siegeData.defender;
        Player attacker = this.siegeData.attacker;

        // where is the defender?
        Claim defenderClaim = dataStore.getClaimAt(defender.getLocation(), false, null);

        // if this is a new claim and he has some permission there, extend the siege to include it
        if (defenderClaim != null) {
            String noAccessReason = defenderClaim.allowAccess(defender.getWorld(), defender);
            if (defenderClaim.canSiege(defender) && noAccessReason == null) {
                this.siegeData.claims.add(defenderClaim);
                defenderClaim.siegeData = this.siegeData;
            }
        }

        // determine who's close enough to the siege area to be considered "still here"
        boolean attackerRemains = this.playerRemains(attacker);
        boolean defenderRemains = this.playerRemains(defender);

        // if they're both here, just plan to come check again later
        if (attackerRemains && defenderRemains) {
            this.scheduleAnotherCheck();
        }

        // otherwise attacker wins if the defender runs away
        else if (attackerRemains && !defenderRemains) {
            dataStore.endSiege(this.siegeData, attacker.getName(), defender.getName(), false);
        }

        // or defender wins if the attacker leaves
        else if (!attackerRemains && defenderRemains) {
            dataStore.endSiege(this.siegeData, defender.getName(), attacker.getName(), false);
        }

        // if they both left, but are still close together, the battle continues (check again later) 50-block radius for chasing
        else if (attacker.getWorld().equals(defender.getWorld())
                && attacker.getLocation().getPosition().distanceSquared(defender.getLocation().getPosition()) < 2500) {
            this.scheduleAnotherCheck();
        }

        // otherwise they both left and aren't close to each other, so call the
        // attacker the winner (defender escaped, possibly after a chase)
        else {
            dataStore.endSiege(this.siegeData, attacker.getName(), defender.getName(), false);
        }
    }

    // a player has to be within 25 blocks of the edge of a besieged claim to be
    // considered still in the fight
    private boolean playerRemains(Player player) {
        for (int i = 0; i < this.siegeData.claims.size(); i++) {
            Claim claim = this.siegeData.claims.get(i);
            if (claim.isNear(player.getLocation(), 25)) {
                return true;
            }
        }

        return false;
    }

    // schedules another checkup later
    private void scheduleAnotherCheck() {
        Task task =
                Sponge.getGame().getScheduler().createTaskBuilder().delay(30, TimeUnit.SECONDS).execute(this).submit(GriefPrevention.instance);
        this.siegeData.checkupTaskID = task.getUniqueId();
    }
}
