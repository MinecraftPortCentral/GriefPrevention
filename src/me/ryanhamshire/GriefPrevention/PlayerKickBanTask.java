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


import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.BanList;
import net.minecraft.server.management.UserListBansEntry;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Texts;

import java.util.Date;

//kicks or bans a player
//need a task for this because async threads (like the chat event handlers) can't kick or ban.
//but they CAN schedule a task to run in the main thread to do that job
class PlayerKickBanTask implements Runnable {

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

            UserListBansEntry userlistbansentry = new UserListBansEntry(gameprofile, null, ((EntityPlayer) player).getCommandSenderName(),
                                                                        null, this.reason);
            minecraftserver.getConfigurationManager().getBannedPlayers().addEntry(userlistbansentry);
            EntityPlayerMP entityplayermp = minecraftserver.getConfigurationManager().getPlayerByUsername(this.player.getName());

            if (entityplayermp != null) {
                entityplayermp.playerNetServerHandler.kickPlayerFromServer("You are banned from this server.");
            }
        }
    }
}
