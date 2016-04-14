package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GPPermissions;
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

import java.util.Optional;

public class CommandAddFlagPermission implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String target = ctx.<String>getOne("target").get();
        String name = ctx.<String>getOne("name").get();
        String flag = ctx.<String>getOne("flag").get();
        String value = ctx.<String>getOne("value").get();

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        Optional<User> targetPlayer = GriefPrevention.instance.resolvePlayerByName(name);
        if (!targetPlayer.isPresent() && target.equalsIgnoreCase("player")) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        }

        return CommandHelper.addPermission(claim, player, targetPlayer, name, flag, GPPermissions.FLAG_BASE + "." + flag, value);
    }
}
