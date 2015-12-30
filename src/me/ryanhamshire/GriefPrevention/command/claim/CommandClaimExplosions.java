package me.ryanhamshire.GriefPrevention.command.claim;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;

public class CommandClaimExplosions implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
        // determine which claim the player is standing in
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true /*
                                                                    * ignore
                                                                    * height
                                                                    */, null);

        if (claim == null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
        }
        else {
            String noBuildReason = claim.allowBuild(player, BlockTypes.STONE);
            if (noBuildReason != null) {
                try {
                    throw new CommandException(Texts.of(noBuildReason));
                } catch (CommandException e) {
                    src.sendMessage(e.getText());
                    return CommandResult.success();
                }
            }

            if (claim.areExplosivesAllowed) {
                claim.areExplosivesAllowed = false;
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
            } else {
                claim.areExplosivesAllowed = true;
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
            }
        }

        return CommandResult.success();
    }
}
