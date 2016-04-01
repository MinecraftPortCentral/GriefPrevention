package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.task.PlayerRescueTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.DimensionTypes;

import java.util.concurrent.TimeUnit;

public class CommandTrapped implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // FEATURE: empower players who get "stuck" in an area where they
        // don't have permission to build to save themselves

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);

        // if another /trapped is pending, ignore this slash command
        if (playerData.pendingTrapped) {
            return CommandResult.empty();
        }

        // if the player isn't in a claim or has permission to build, tell him to man up
        if (claim == null || claim.allowBuild(player, player.getLocation()) == null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.NotTrappedHere));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // if the player is in the nether or end, he's screwed (there's no
        // way to programmatically find a safe place for him)
        if (player.getWorld().getDimension().getType() != DimensionTypes.OVERWORLD) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.TrappedWontWorkHere));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // if the player is in an administrative claim, he should contact an admin
        if (claim.isAdminClaim()) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.TrappedWontWorkHere));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // send instructions
        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);

        // create a task to rescue this player in a little while
        PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
        Sponge.getGame().getScheduler().createTaskBuilder()
                .delay(1, TimeUnit.SECONDS)
                .execute(task)
                .submit(GriefPrevention.instance);

        return CommandResult.success();
    }
}
