package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
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

        if (!GriefPrevention.instance.economyService.get().getAccount(player.getUniqueId()).isPresent()) {
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
        if (GriefPrevention.instance.economyService.get().getAccount(player.getUniqueId()).isPresent()) {
            balance = GriefPrevention.instance.economyService.get().getAccount(player.getUniqueId()).get().getBalance(GriefPrevention.instance
                    .economyService.get().getDefaultCurrency()).doubleValue();
        }

        // if no parameter, just tell player cost per block and balance
        if (!blockCountOpt.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost,
                    String.valueOf(activeConfig.getConfig().economy.economyClaimBlockCost),
                    String.valueOf(balance));
            return CommandResult.success();
        } else {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

            // try to parse number of blocks
            int blockCount = blockCountOpt.get();

            if (blockCount <= 0) {
                GriefPrevention.sendMessage(player, TextMode.Err, "Invalid block count of lte 0");
                return CommandResult.success();
            }

            double totalCost = blockCount * activeConfig.getConfig().economy.economyClaimBlockCost;
            // attempt to withdraw cost
            TransactionResult transactionResult = GriefPrevention.instance.economyService.get().getAccount(player.getUniqueId()).get().withdraw
                    (GriefPrevention.instance.economyService.get().getDefaultCurrency(), BigDecimal.valueOf(totalCost),
                            Cause.of(GriefPrevention.instance));

            if (transactionResult.getResult() != ResultType.SUCCESS) {
                GriefPrevention.sendMessage(player, TextMode.Err, "Could not withdraw funds. Reason: " + transactionResult.getResult().name() +
                        ".");
                return CommandResult.success();
            }
            // add blocks
            playerData.setBonusClaimBlocks(player.getWorld(), playerData.getBonusClaimBlocks(player.getWorld()) + blockCount);
            playerData.worldStorageData.get(player.getWorld().getUniqueId()).save();

            // inform player
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost),
                    String.valueOf(playerData.getRemainingClaimBlocks(player.getWorld())));
        }
        return CommandResult.success();
    }
}
