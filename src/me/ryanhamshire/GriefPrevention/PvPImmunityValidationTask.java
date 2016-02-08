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

import java.util.concurrent.TimeUnit;

//sends a message to a player
//used to send delayed messages, for example help text triggered by a player's chat
class PvPImmunityValidationTask implements Runnable {

    private Player player;

    public PvPImmunityValidationTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            return;
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (!playerData.pvpImmune) {
            return;
        }

        // check the player's inventory for anything
        if (!GriefPrevention.isInventoryEmpty(player)) {
            // if found, cancel invulnerability and notify
            playerData.pvpImmune = false;
            GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
        } else {
            // otherwise check again in one minute
            Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(this).submit(GriefPrevention.instance);
        }
    }
}
