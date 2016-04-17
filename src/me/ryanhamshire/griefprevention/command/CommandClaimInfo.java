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
import org.spongepowered.api.text.format.TextStyles;

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
            Text claimName = Text.of();
            if (claim.isSubDivision) {
                claimName = Text.of(TextColors.YELLOW, "Name", TextColors.WHITE, " : ", TextColors.GRAY, claim.subDivisionData.claimName);
            } else {
                claimName = Text.of(TextColors.YELLOW, "Name", TextColors.WHITE, " : ", TextColors.GRAY, claim.getClaimData().getConfig().claimName);
            }
            Text claimId = Text.of(TextColors.YELLOW, "UUID", TextColors.WHITE, " : ", TextColors.GRAY, claim.id);
            Text ownerLine = Text.of(TextColors.YELLOW, "Owner", TextColors.WHITE, " : ", TextColors.GRAY, owner.getName());
            Text claimFarewell = Text.of("");
            if (!claim.isSubDivision) {
                claimFarewell = Text.of(TextColors.YELLOW, "Farewell", TextColors.WHITE, " : ", TextColors.GRAY, claim.getClaimData().getConfig().claimFarewellMessage);
            }
            Text claimGreeting = Text.of("");
            if (!claim.isSubDivision) {
                claimGreeting = Text.of(TextColors.YELLOW, "Greeting", TextColors.WHITE, " : ", TextColors.GRAY, claim.getClaimData().getConfig().claimGreetingMessage);
            }
            Text lesserCorner = Text.of(TextColors.YELLOW, "LesserCorner", TextColors.WHITE, " : ", TextColors.GRAY, claim.getLesserBoundaryCorner().getBlockPosition());
            Text greaterCorner = Text.of(TextColors.YELLOW, "GreaterCorner", TextColors.WHITE, " : ", TextColors.GRAY, claim.getGreaterBoundaryCorner().getBlockPosition());
            Text claimArea = Text.of(TextColors.YELLOW, "Area", TextColors.WHITE, " : ", TextColors.GRAY, claim.getArea(), " blocks");
            Text dateCreated = Text.of(TextColors.YELLOW, "Created", TextColors.WHITE, " : ", TextColors.GRAY, claim.modifiedDate);
            Text footer = Text.of(TextColors.WHITE, TextStyles.STRIKETHROUGH, "------------------------------------------");
            GriefPrevention.sendMessage(src, 
                    Text.of("\n",
                            footer, "\n",
                            claimId, "\n",
                            claimName, "\n",
                            ownerLine, "\n",
                            claimGreeting, "\n",
                            claimFarewell, "\n",
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
