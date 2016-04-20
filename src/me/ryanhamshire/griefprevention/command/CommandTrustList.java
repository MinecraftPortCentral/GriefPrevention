package me.ryanhamshire.griefprevention.command;

import static org.spongepowered.api.command.CommandMessageFormatting.SPACE_TEXT;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
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
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;

import java.util.UUID;

public class CommandTrustList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);

        // if no claim here, error message
        if (claim == null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.TrustListNoClaim));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        GriefPrevention.sendMessage(player, TextMode.Info, Messages.TrustListHeader);
        Builder permissions = Text.builder(">").color(TextColors.GOLD);

        if (claim.isSubdivision()) {
            for (UUID uuid : claim.getClaimData().getCoowners()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                permissions.append(SPACE_TEXT, Text.of(user.getName()));
            }

            player.sendMessage(permissions.build());
            permissions = Text.builder(">").color(TextColors.YELLOW);

            for (UUID uuid : claim.getClaimData().getBuilders()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                permissions.append(SPACE_TEXT, Text.of(user.getName()));
            }

            player.sendMessage(permissions.build());
            permissions = Text.builder(">").color(TextColors.GREEN);

            for (UUID uuid : claim.getClaimData().getContainers()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                permissions.append(SPACE_TEXT, Text.of(user.getName()));
            }

            player.sendMessage(permissions.build());
            permissions = Text.builder(">").color(TextColors.BLUE);

            for (UUID uuid : claim.getClaimData().getAccessors()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                permissions.append(SPACE_TEXT, Text.of(user.getName()));
            }
        }

        player.sendMessage(permissions.build());
        player.sendMessage(Text.of(
                Text.of(TextColors.BLUE, GriefPrevention.instance.dataStore.getMessage(Messages.Access)), SPACE_TEXT,
                Text.of(TextColors.YELLOW, GriefPrevention.instance.dataStore.getMessage(Messages.Build)), SPACE_TEXT,
                Text.of(TextColors.GREEN, GriefPrevention.instance.dataStore.getMessage(Messages.Containers)), SPACE_TEXT,
                Text.of(TextColors.GOLD, GriefPrevention.instance.dataStore.getMessage(Messages.Coowner))));
        return CommandResult.success();

    }
}
