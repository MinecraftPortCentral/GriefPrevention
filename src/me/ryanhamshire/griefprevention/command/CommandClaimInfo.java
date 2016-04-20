package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.PlayerData;
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
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            User owner = GriefPrevention.getOrCreateUser(claim.getClaimData().getOwnerUniqueId());
            Text claimName = Text.of();
            String accessors = "";
            String builders = "";
            String containers = "";
            String coowners = "";
            claimName = Text.of(TextColors.YELLOW, "Name", TextColors.WHITE, " : ", TextColors.GRAY, claim.getClaimData().getClaimName());
            for (UUID uuid : claim.getClaimData().getAccessors()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                accessors += user.getName() + " ";
            }
            for (UUID uuid : claim.getClaimData().getBuilders()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                builders += user.getName() + " ";
            }
            for (UUID uuid : claim.getClaimData().getContainers()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                containers += user.getName() + " ";
            }
            for (UUID uuid : claim.getClaimData().getCoowners()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                coowners += user.getName() + " ";
            }

            Text claimId = Text.join(Text.of(TextColors.YELLOW, "UUID", TextColors.WHITE, " : ",
                    Text.builder()
                            .append(Text.of(TextColors.GRAY, claim.id.toString()))
                            .onShiftClick(TextActions.insertText(claim.id.toString())).build()));
            Text ownerLine = Text.of(TextColors.YELLOW, "Owner", TextColors.WHITE, " : ", TextColors.GOLD, owner.getName());
            Text claimFarewell = Text.of(TextColors.YELLOW, "Farewell", TextColors.WHITE, " : ", TextColors.RESET,
                    claim.getClaimData().getFarewellMessage());
            Text claimGreeting = Text.of(TextColors.YELLOW, "Greeting", TextColors.WHITE, " : ", TextColors.RESET,
                    claim.getClaimData().getGreetingMessage());
            Text lesserCorner = Text.of(TextColors.YELLOW, "LesserCorner", TextColors.WHITE, " : ", TextColors.GRAY,
                    claim.getLesserBoundaryCorner().getBlockPosition());
            Text greaterCorner = Text.of(TextColors.YELLOW, "GreaterCorner", TextColors.WHITE, " : ", TextColors.GRAY,
                    claim.getGreaterBoundaryCorner().getBlockPosition());
            Text claimArea = Text.of(TextColors.YELLOW, "Area", TextColors.WHITE, " : ", TextColors.GRAY, claim.getArea(), " blocks");
            Text claimAccessors = Text.of(TextColors.YELLOW, "Accessors", TextColors.WHITE, " : ", TextColors.LIGHT_PURPLE, accessors);
            Text claimBuilders = Text.of(TextColors.YELLOW, "Builders", TextColors.WHITE, " : ", TextColors.GRAY, builders);
            Text claimContainers = Text.of(TextColors.YELLOW, "Containers", TextColors.WHITE, " : ", TextColors.GRAY, containers);
            Text claimCoowners = Text.of(TextColors.YELLOW, "Coowners", TextColors.WHITE, " : ", TextColors.GRAY, coowners);
            Text dateCreated = Text.of(TextColors.YELLOW, "Created", TextColors.WHITE, " : ", TextColors.GRAY, claim.modifiedDate);
            Text footer = Text.of(TextColors.WHITE, TextStyles.STRIKETHROUGH, "------------------------------------------");
            GriefPrevention.sendMessage(src,
                    Text.of("\n",
                            footer, "\n",
                            claimName, "\n",
                            ownerLine, "\n",
                            claimArea, "\n",
                            claimAccessors, "\n",
                            claimBuilders, "\n",
                            claimContainers, "\n",
                            claimCoowners, "\n",
                            claimGreeting, "\n",
                            claimFarewell, "\n",
                            dateCreated, "\n",
                            claimId, "\n",
                            footer));
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim in your current location."));
        }

        return CommandResult.success();
    }
}
