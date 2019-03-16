/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandClaimBuy implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // if economy is disabled, don't do anything
        if (!GriefPreventionPlugin.instance.economyService.isPresent()) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.economyNotInstalled.toText());
            return CommandResult.success();
        }

        Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
        if (playerAccount == null) {
            final Text message = GriefPreventionPlugin.instance.messageData.economyUserNotFound
                    .apply(ImmutableMap.of(
                    "user", player.getName())).build();
            GriefPreventionPlugin.sendMessage(player, message);
            return CommandResult.success();
        }

        Set<Claim> claimsForSale = new HashSet<>();
        GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(player.getWorld().getProperties());
        for (Claim worldClaim : claimManager.getWorldClaims()) {
            if (worldClaim.isWilderness()) {
                continue;
            }
            if (!worldClaim.isAdminClaim() && worldClaim.getEconomyData().isForSale() && worldClaim.getEconomyData().getSalePrice() > -1) {
                claimsForSale.add(worldClaim);
            }
            for (Claim child : worldClaim.getChildren(true)) {
                if (child.isAdminClaim()) {
                    continue;
                }
                if (child.getEconomyData().isForSale() && child.getEconomyData().getSalePrice() > -1) {
                    claimsForSale.add(child);
                }
            }
        }

        List<Text> claimsTextList = CommandHelper.generateClaimTextList(new ArrayList<Text>(), claimsForSale, player.getWorld().getName(), null, src, CommandHelper.createCommandConsumer(src, "claimbuy", ""), true, false);
        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.AQUA,"Claims for sale")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(claimsTextList);
        paginationBuilder.sendTo(src);
        return CommandResult.success();
    }
}
