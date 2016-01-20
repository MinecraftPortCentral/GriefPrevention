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


import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListBansEntry;
import org.spongepowered.api.entity.living.player.Player;

//kicks or bans a player
//need a task for this because async threads (like the chat event handlers) can't kick or ban.
//but they CAN schedule a task to run in the main thread to do that job
public class PlayerKickBanTask implements Runnable {

    // player to kick or ban
    private Player player;

    // message to send player.
    private String reason;

    // source of ban
    private String source;

    // whether to ban
    private boolean ban;

    public PlayerKickBanTask(Player player, String reason, String source, boolean ban) {
        this.player = player;
        this.reason = reason;
        this.source = source;
        this.ban = ban;
    }

    @Override
    public void run() {
        if (this.ban) {
            MinecraftServer minecraftserver = MinecraftServer.getServer();
            GameProfile gameprofile = minecraftserver.getPlayerProfileCache().getGameProfileForUsername(player.getName());

            UserListBansEntry userlistbansentry = new UserListBansEntry(gameprofile, null, ((EntityPlayer) player).getName(),
                                                                        null, this.reason);
            minecraftserver.getConfigurationManager().getBannedPlayers().addEntry(userlistbansentry);
            EntityPlayerMP entityplayermp = minecraftserver.getConfigurationManager().getPlayerByUsername(this.player.getName());

            if (entityplayermp != null) {
                entityplayermp.playerNetServerHandler.kickPlayerFromServer("You are banned from this server.");
            }
        }
    }
}
