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

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

//tries to rescue a trapped player from a claim where he doesn't have permission to save himself
//related to the /trapped slash command
//this does run in the main thread, so it's okay to make non-thread-safe calls
public class PlayerRescueTask implements Runnable {

    // original location where /trapped was used
    private Location<World> location;

    // player data
    private Player player;

    public PlayerRescueTask(Player player, Location<World> location) {
        this.player = player;
        this.location = location;
    }

    @Override
    public void run() {
        // if he logged out, don't do anything
        if (!player.isOnline())
            return;

        // he no longer has a pending /trapped slash command, so he can try to
        // use it again now
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld().getProperties(), player.getUniqueId());
        playerData.pendingTrapped = false;

        // if the player moved three or more blocks from where he used /trapped,
        // admonish him and don't save him
        if (player.getLocation().getBlockPosition().distance(this.location.getBlockPosition()) > 3) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.RescueAbortedMoved);
            return;
        }

        // otherwise find a place to teleport him
        boolean result = GriefPrevention.instance.ejectPlayer(this.player);

        // log entry, in case admins want to investigate the "trap"
        GriefPrevention.AddLogEntry("Rescued trapped player " + player.getName() + " from " + GriefPrevention.getfriendlyLocationString(this.location)
                + " to " + GriefPrevention.getfriendlyLocationString(player.getLocation()) + ".");
    }
}
