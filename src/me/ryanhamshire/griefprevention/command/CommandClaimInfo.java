package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class CommandClaimInfo implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);

        if (claim != null) {
            User owner = GriefPrevention.getOrCreateUser(claim.claimData.getConfig().ownerUniqueId);
            Text ownerLine = Text.of(TextColors.YELLOW, "Owner: ", TextColors.AQUA, owner.getName());
            Text claimId = Text.of(TextColors.YELLOW, "UUID: ", TextColors.AQUA, claim.id);
            Text lesserCorner = Text.of(TextColors.YELLOW, "LesserCorner: ", TextColors.AQUA, claim.getLesserBoundaryCorner().getBlockPosition());
            Text greaterCorner = Text.of(TextColors.YELLOW, "GreaterCorner: ", TextColors.AQUA, claim.getGreaterBoundaryCorner().getBlockPosition());
            Text claimArea = Text.of(TextColors.YELLOW, "Area: ", TextColors.AQUA, claim.getArea(), " blocks");
            Text dateCreated = Text.of(TextColors.YELLOW, "Created: ", TextColors.AQUA, claim.modifiedDate);
            Text footer = Text.of(TextColors.GREEN, "==========================================");
            GriefPrevention.sendMessage(src, 
                    Text.of("\n",
                            footer, "\n",
                            ownerLine, "\n",
                            claimId, "\n",
                            lesserCorner, "\n",
                            greaterCorner, "\n",
                            claimArea, "\n",
                            dateCreated, "\n",
                            footer));
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim in your current location."));
        }

        return CommandResult.success();
    }
}
