/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
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
package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommandPlayerInfo implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        List<User> userValues = new ArrayList<>(ctx.getAll("user"));
        WorldProperties worldProperties = ctx.<WorldProperties>getOne("world").orElse(Sponge.getServer().getDefaultWorld().orElse(null));
        User user = null;
        if (userValues.size() > 0) {
            user = userValues.get(0);
        }

        if (user == null) {
            if (!(src instanceof Player)) {
                GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "No player specified."));
                return CommandResult.success();
            }

            user = (User) src;
        }

        // otherwise if no permission to delve into another player's claims data or self
        if ((user != null && user != src && !src.hasPermission(GPPermissions.COMMAND_PLAYER_INFO_OTHERS)) ||
                !src.hasPermission(GPPermissions.COMMAND_PLAYER_INFO_BASE)) {
            try {
                throw new CommandPermissionException();
            } catch (CommandPermissionException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }


        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(worldProperties, user.getUniqueId());
        List<Claim> claimList = new ArrayList<>();
        for (Claim claim : playerData.getClaims()) {
            if (claim.getWorld().getProperties().equals(worldProperties)) {
                claimList.add(claim);
            }
        }
        List<Text> claimsTextList = Lists.newArrayList();
        claimsTextList.add(Text.of(
                TextColors.YELLOW, "UUID", TextColors.WHITE, " : ", TextColors.GRAY, user.getUniqueId(), "\n",
                TextColors.YELLOW, "World", TextColors.WHITE, " : ", TextColors.GRAY, worldProperties.getWorldName(), "\n",
                TextColors.YELLOW, "Claim Size Limits", TextColors.WHITE, " : ", TextColors.GRAY, playerData.optionMaxClaimSizeX + "," + playerData.optionMaxClaimSizeY + ", " + playerData.optionMaxClaimSizeZ, "\n",
                TextColors.YELLOW, "Initial Blocks", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionInitialClaimBlocks, "\n",
                TextColors.YELLOW, "Accrued Blocks", TextColors.WHITE, " : ", TextColors.GREEN, playerData.getAccruedClaimBlocks(), TextColors.GRAY, " (", TextColors.LIGHT_PURPLE, playerData.optionBlocksAccruedPerHour, TextColors.WHITE, " per hour", TextColors.GRAY, ")", "\n",
                TextColors.YELLOW, "Max Accrued Blocks", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionMaxAccruedBlocks, "\n",
                TextColors.YELLOW, "Bonus Blocks", TextColors.WHITE, " : ", TextColors.GREEN, playerData.getBonusClaimBlocks(), "\n",
                TextColors.YELLOW, "Remaining Blocks", TextColors.WHITE, " : ", TextColors.GREEN, playerData.getRemainingClaimBlocks(), "\n", 
                TextColors.YELLOW, "Abandoned Return Ratio", TextColors.WHITE, " : ", TextColors.GREEN, playerData.getAbandonedReturnRatio(), "\n",
                TextColors.YELLOW, "Total Blocks", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionInitialClaimBlocks + playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks(), "\n",
                TextColors.YELLOW, "Total Claims", TextColors.WHITE, " : ", TextColors.GREEN, claimList.size()));

        JoinData joinData = user.getOrCreate(JoinData.class).orElse(null);
        if (joinData != null && joinData.lastPlayed().exists()) {
            Date lastActive = null;
            try {
                lastActive = Date.from(joinData.lastPlayed().get());
            } catch(DateTimeParseException ex) {
                // ignore
            }
            if (lastActive != null) {
                claimsTextList.add(Text.of(TextColors.YELLOW, "Last Active", TextColors.WHITE, " : ", TextColors.GRAY, lastActive));
            }
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.AQUA, "Player Info")).padding(Text.of("-")).contents(claimsTextList);
        paginationBuilder.sendTo(src);

        return CommandResult.success();
    }
}
