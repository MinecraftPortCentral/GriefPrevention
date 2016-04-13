package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class CommandClaimDeleteAllAdmin implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        try {
            ctx.checkPermission(src, GPPermissions.DELETE_ADMIN_CLAIM);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // delete all admin claims
        GriefPrevention.instance.dataStore.deleteClaimsForPlayer(null, true); // null for owner
        // id indicates an administrative claim

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
        GriefPrevention.addLogEntry(player.getName() + " deleted all administrative claims.", CustomLogEntryTypes.AdminActivity);

        // revert any current visualization
        Visualization.Revert(player);

        return CommandResult.success();
    }
}
