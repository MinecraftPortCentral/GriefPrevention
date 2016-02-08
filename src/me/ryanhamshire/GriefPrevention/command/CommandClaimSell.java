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

public class CommandClaimSell implements CommandExecutor {

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
            GriefPrevention.instance.economyService.get().createAccount(player.getUniqueId());
        }

        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(player.getWorld().getProperties());
        if (activeConfig.getConfig().economy.economyClaimBlockCost == 0 && activeConfig.getConfig().economy.economyClaimBlockSell == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
            return CommandResult.success();
        }

        // if selling disabled, send error message
        if (activeConfig.getConfig().economy.economyClaimBlockSell == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
            return CommandResult.success();
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        int availableBlocks = playerData.getRemainingClaimBlocks(player.getWorld());
        Optional<Integer> blockCountOpt = ctx.getOne("numberOfBlocks");
        if (!blockCountOpt.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue,
                    String.valueOf(activeConfig.getConfig().economy.economyClaimBlockSell), String.valueOf(availableBlocks));
            return CommandResult.success();
        } else {
            int blockCount = blockCountOpt.get();
            // try to parse number of blocks
            if (blockCount <= 0) {
                GriefPrevention.sendMessage(player, TextMode.Err, "Invalid block count '" + blockCount + "', you must enter a value > 0.");
                return CommandResult.success();
            } else if (blockCount > availableBlocks) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
                return CommandResult.success();
            }

            // attempt to compute value and deposit it
            double totalValue = blockCount * activeConfig.getConfig().economy.economyClaimBlockSell;
            TransactionResult transactionResult = GriefPrevention.instance.economyService.get().getAccount(player.getUniqueId()).get().deposit
                    (GriefPrevention.instance.economyService.get().getDefaultCurrency(), BigDecimal.valueOf(totalValue),
                            Cause.of(GriefPrevention.instance));


            if (transactionResult.getResult() != ResultType.SUCCESS) {
                GriefPrevention.sendMessage(player, TextMode.Err, "Could not sell blocks. Reason: " + transactionResult.getResult().name() + ".");
                return CommandResult.success();
            }
            // subtract blocks
            playerData.setBonusClaimBlocks(player.getWorld(), playerData.getBonusClaimBlocks(player.getWorld()) - blockCount);
            playerData.worldStorageData.get(player.getWorld().getUniqueId()).save();

            // inform player
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue),
                    String.valueOf(playerData.getRemainingClaimBlocks(player.getWorld())));
        }
        return CommandResult.success();
    }
}
