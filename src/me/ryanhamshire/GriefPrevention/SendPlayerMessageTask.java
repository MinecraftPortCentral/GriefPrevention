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
import org.spongepowered.api.text.Text;

//sends a message to a player
//used to send delayed messages, for example help text triggered by a player's chat
class SendPlayerMessageTask implements Runnable {

    private Player player;
    private Text message;

    public SendPlayerMessageTask(Player player, Text message) {
        this.player = player;
        this.message = message;
    }

    @Override
    public void run() {
        if (player == null) {
            GriefPrevention.AddLogEntry(Text.of(message).toPlain());
            return;
        }

        // if the player is dead, save it for after his respawn
        if (((net.minecraft.entity.player.EntityPlayerMP) this.player).isDead) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(this.player.getWorld(), this.player.getUniqueId());
            playerData.messageOnRespawn = this.message;
        }

        // otherwise send it immediately
        else {
            GriefPrevention.sendMessage(this.player, Text.of(this.message));
        }
    }
}
