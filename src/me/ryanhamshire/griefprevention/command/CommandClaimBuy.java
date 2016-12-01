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

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

import java.math.BigDecimal;
import java.util.Optional;

public class CommandClaimBuy implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // if economy is disabled, don't do anything
        if (!GriefPrevention.instance.economyService.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Err, "Economy plugin not installed!");
            return CommandResult.success();
        }

        if (!GriefPrevention.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Err, "No economy account found for user " + player.getName() + "!");
            return CommandResult.success();
        }

        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(player.getWorld().getProperties());
        if (activeConfig.getConfig().economy.economyClaimBlockCost == 0 && activeConfig.getConfig().economy.economyClaimBlockSell == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
            return CommandResult.success();
        }

        // if purchase disabled, send error message
        if (activeConfig.getConfig().economy.economyClaimBlockCost == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
            return CommandResult.success();
        }

        Optional<Integer> blockCountOpt = ctx.getOne("numberOfBlocks");
        double balance = 0;
        if (GriefPrevention.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).isPresent()) {
            balance = GriefPrevention.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).get().getBalance(GriefPrevention.instance
                    .economyService.get().getDefaultCurrency()).doubleValue();
        }

        // if no parameter, just tell player cost per block and balance
        if (!blockCountOpt.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost,
                    String.valueOf(activeConfig.getConfig().economy.economyClaimBlockCost),
                    String.valueOf(balance));
            return CommandResult.success();
        } else {
            PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

            // try to parse number of blocks
            int blockCount = blockCountOpt.get();

            if (blockCount <= 0) {
                GriefPrevention.sendMessage(player, TextMode.Err, "Invalid block count of lte 0");
                return CommandResult.success();
            }

            double totalCost = blockCount * activeConfig.getConfig().economy.economyClaimBlockCost;
            // attempt to withdraw cost
            TransactionResult transactionResult = GriefPrevention.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).get().withdraw
                    (GriefPrevention.instance.economyService.get().getDefaultCurrency(), BigDecimal.valueOf(totalCost),
                            Cause.of(NamedCause.of(GriefPrevention.MOD_ID, GriefPrevention.instance)));

            if (transactionResult.getResult() != ResultType.SUCCESS) {
                GriefPrevention.sendMessage(player, TextMode.Err, "Could not withdraw funds. Reason: " + transactionResult.getResult().name() +
                        ".");
                return CommandResult.success();
            }
            // add blocks
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + blockCount);
            playerData.getStorageData().save();

            // inform player
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost),
                    String.valueOf(playerData.getRemainingClaimBlocks()));
        }
        return CommandResult.success();
    }
}
