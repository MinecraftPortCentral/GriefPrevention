package me.ryanhamshire.GriefPrevention.command;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class CommandClaimSell implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        // TODO: Implement economy
        /*final Player player = checkPlayer(src);
        // if economy is disabled, don't do anything
        if (GriefPrevention.economy == null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
            return true;
        }

        // if disabled, error message
        if (GriefPrevention.instance.config_economy_claimBlocksSellValue == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
            return true;
        }

        // load player data
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        int availableBlocks = playerData.getRemainingClaimBlocks();

        Optional<Integer> blockCountOpt = args.getOne("numberOfBlocks");
        // if no amount provided, just tell player value per block sold, and
        // how many he can sell
        if (!blockCountOpt.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue,
                    String.valueOf(GriefPrevention.instance.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
            return false;
        }

        // parse number of blocks
        int blockCount = blockCountOpt.get();

        if (blockCount <= 0) {
            throw new CommandException(Text.of("Invalid block count of lte 0"));
        }

        // if he doesn't have enough blocks, tell him so
        if (blockCount > availableBlocks) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
        }

        // otherwise carry out the transaction
        else {
            // compute value and deposit it
            double totalValue = blockCount * GriefPrevention.instance.config_economy_claimBlocksSellValue;
            economy.depositPlayer(player, totalValue);

            // subtract blocks
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - blockCount);
            this.dataStore.savePlayerData(player.getUniqueId(), playerData);

            // inform player
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue),
                    String.valueOf(playerData.getRemainingClaimBlocks()));
        }*/
        return CommandResult.success();
    }
}
