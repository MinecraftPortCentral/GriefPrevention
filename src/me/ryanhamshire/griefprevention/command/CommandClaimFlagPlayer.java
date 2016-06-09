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

public class CommandClaimFlagPlayer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String name = ctx.<String>getOne("player").get();
        String flag = ctx.<String>getOne("flag").get();
        String value = ctx.<String>getOne("value").get();

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        Optional<User> targetPlayer = GriefPrevention.instance.resolvePlayerByName(name);
        if (!targetPlayer.isPresent()) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "The playername " + name + " was not found."));
            return CommandResult.success();
        }
        

        return CommandHelper.addPlayerPermission(claim, player, targetPlayer.get(), flag, GPPermissions.FLAG_BASE + "." + flag, value);
    }
}
