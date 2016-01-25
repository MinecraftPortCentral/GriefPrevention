package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;

public class CommandClaimList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {

        // player whose claims will be listed
        User otherPlayer = ctx.<User>getOne("player").get();

        // otherwise if no permission to delve into another player's claims data
        if (otherPlayer != src && !src.hasPermission("griefprevention.claimslistother")) {
            try {
                throw new CommandPermissionException();
            } catch (CommandPermissionException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        Player player = (Player) src;
        // load the target player's data
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), otherPlayer.getUniqueId());
        List<Claim> claimList = playerData.playerWorldClaims.get(player.getWorld().getUniqueId());
        GriefPrevention.sendMessage(src, TextMode.Instr, Messages.StartBlockMath,
                String.valueOf(playerData.getAccruedClaimBlocks(player.getWorld())),
                String.valueOf((playerData.getBonusClaimBlocks(player.getWorld()) + GriefPrevention.instance.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId()))),
                String.valueOf((playerData.getAccruedClaimBlocks(player.getWorld()) + playerData.getBonusClaimBlocks(player.getWorld())
                        + GriefPrevention.instance.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId()))));
        if (claimList.size() > 0) {
            GriefPrevention.sendMessage(src, TextMode.Instr, Messages.ClaimsListHeader);
            for (Claim claim : claimList) {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Instr, GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner())
                        + GriefPrevention.instance.dataStore.getMessage(Messages.ContinueBlockMath, String.valueOf(claim.getArea()))));
            }

            GriefPrevention.sendMessage(src, TextMode.Instr, Messages.EndBlockMath, String.valueOf(playerData.getRemainingClaimBlocks(player.getWorld())));
        }

        // drop the data we just loaded, if the player isn't online
        if (!otherPlayer.isOnline()) {
            GriefPrevention.instance.dataStore.clearCachedPlayerData(otherPlayer.getUniqueId());
        }

        return CommandResult.success();
    }
}
