package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;

public class CommandClaimList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {

        // player whose claims will be listed if any
        Optional<User> otherPlayer = ctx.<User>getOne("player");

        // otherwise if no permission to delve into another player's claims data or self
        if ((otherPlayer.isPresent() && otherPlayer.get() != src && !src.hasPermission(GPPermissions.CLAIMS_LIST_OTHER)) ||
                (!otherPlayer.isPresent() && !src.hasPermission(GPPermissions.LIST_CLAIMS))) {
            try {
                throw new CommandPermissionException();
            } catch (CommandPermissionException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        Player player = (Player) src;
        User targetUser = otherPlayer.isPresent() ? otherPlayer.get() : (User) src;
        // load the target player's data
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), targetUser.getUniqueId());
        List<Claim> claimList = playerData.playerWorldClaims.get(player.getWorld().getUniqueId());
        GriefPrevention.sendMessage(src, TextMode.Instr, Messages.StartBlockMath,
                String.valueOf(playerData.getAccruedClaimBlocks(player.getWorld())),
                String.valueOf((playerData.getBonusClaimBlocks(player.getWorld()) + GriefPrevention.instance.dataStore
                        .getGroupBonusBlocks(targetUser.getUniqueId()))),
                String.valueOf((playerData.getAccruedClaimBlocks(player.getWorld()) + playerData.getBonusClaimBlocks(player.getWorld())
                        + GriefPrevention.instance.dataStore.getGroupBonusBlocks(targetUser.getUniqueId()))));
        if (claimList.size() > 0) {
            GriefPrevention.sendMessage(src, TextMode.Instr, Messages.ClaimsListHeader);
            for (Claim claim : claimList) {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Instr, GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner())
                        + GriefPrevention.instance.dataStore.getMessage(Messages.ContinueBlockMath, String.valueOf(claim.getArea()))));
            }

            GriefPrevention
                    .sendMessage(src, TextMode.Instr, Messages.EndBlockMath, String.valueOf(playerData.getRemainingClaimBlocks(player.getWorld())));
        }

        // drop the data we just loaded, if the player isn't online
        if (!targetUser.isOnline()) {
            GriefPrevention.instance.dataStore.clearCachedPlayerData(targetUser.getUniqueId());
        }

        return CommandResult.success();
    }
}
