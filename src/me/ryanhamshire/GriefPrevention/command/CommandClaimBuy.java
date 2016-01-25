package me.ryanhamshire.GriefPrevention.command;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class CommandClaimBuy implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        // TODO: Implement economy
        /*final Player player = checkPlayer(src);
        // if economy is disabled, don't do anything
        if (GriefPrevention.economy == null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
            return true;
        }

        // if purchase disabled, send error message
        if (GriefPrevention.instance.config_economy_claimBlocksPurchaseCost == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
            return true;
        }

        Optional<Integer> blockCountOpt = args.getOne("numberOfBlocks");

        // if no parameter, just tell player cost per block and balance
        if (!blockCountOpt.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost,
                    String.valueOf(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost),
                    String.valueOf(GriefPrevention.economy.getBalance(player)));
            return false;
        } else {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

            // try to parse number of blocks
            int blockCount = blockCountOpt.get();

            if (blockCount <= 0) {
                throw new CommandException(Text.of("Invalid block count of lte 0"));
            }

            // if the player can't afford his purchase, send error message
            double balance = economy.getBalance(player);
            double totalCost = blockCount * GriefPrevention.instance.config_economy_claimBlocksPurchaseCost;
            if (totalCost > balance) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, String.valueOf(totalCost),
                        String.valueOf(balance));
            }

            // otherwise carry out transaction
            else {
                // withdraw cost
                economy.withdrawPlayer(player, totalCost);

                // add blocks
                playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + blockCount);
                this.dataStore.savePlayerData(player.getUniqueId(), playerData);

                // inform player
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost),
                        String.valueOf(playerData.getRemainingClaimBlocks()));
            }

        }*/
        return CommandResult.success();
    }
}
