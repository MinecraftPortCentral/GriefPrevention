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

import java.util.Collection;

//secures a claim after a siege looting window has closed
class SecureClaimTask implements Runnable {

    private SiegeData siegeData;

    public SecureClaimTask(SiegeData siegeData) {
        this.siegeData = siegeData;
    }

    @Override
    public void run() {
        // for each claim involved in this siege
        for (int i = 0; i < this.siegeData.claims.size(); i++) {
            // lock the doors
            Claim claim = this.siegeData.claims.get(i);
            claim.doorsOpen = false;

            // eject bad guys
            Collection<Player> onlinePlayers = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
            for (Player player : onlinePlayers) {
                if (claim.contains(player.getLocation(), false, false) && claim.allowAccess(player.getWorld(), player) != null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeDoorsLockedEjection);
                    GriefPrevention.instance.ejectPlayer(player);
                }
            }
        }
    }
}
