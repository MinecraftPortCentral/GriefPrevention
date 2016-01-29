package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class CommandSiege implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // error message for when siege mode is disabled
        if (!GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.siegeEnabled) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.NonSiegeWorld));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // can't start a siege when you're already involved in one
        Player attacker = player;
        PlayerData attackerData = GriefPrevention.instance.dataStore.getPlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());
        if (attackerData.siegeData != null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.AlreadySieging));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // can't start a siege when you're protected from pvp combat
        if (attackerData.pvpImmune) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.CantFightWhileImmune));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // if a player name was specified, use that
        Optional<Player> defenderOpt = args.<Player>getOne("playerName");
        if (!defenderOpt.isPresent() && attackerData.lastPvpPlayer.length() > 0) {
            defenderOpt = Sponge.getGame().getServer().getPlayer(attackerData.lastPvpPlayer);
        }

        Player defender;
        try {
            defender = defenderOpt.orElseThrow(() -> new CommandException(Text.of("No player was matched")));
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // victim must not have the permission which makes him immune to siege
        if (defender.hasPermission("griefprevention.siegeimmune")) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.SiegeImmune));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // victim must not be under siege already
        PlayerData defenderData = GriefPrevention.instance.dataStore.getPlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
        if (defenderData.siegeData != null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.AlreadyUnderSiegePlayer));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // victim must not be pvp immune
        if (defenderData.pvpImmune) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.NoSiegeDefenseless));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        Claim defenderClaim = GriefPrevention.instance.dataStore.getClaimAt(defender.getLocation(), false, null);

        // defender must have some level of permission there to be protected
        if (defenderClaim == null || defenderClaim.allowAccess(defender.getWorld(), defender) != null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.NotSiegableThere, defender.getName()));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // attacker must be close to the claim he wants to siege
        if (!defenderClaim.isNear(attacker.getLocation(), 25)) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.SiegeTooFarAway));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // claim can't be under siege already
        if (defenderClaim.siegeData != null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.AlreadyUnderSiegeArea));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // can't siege admin claims
        if (defenderClaim.isAdminClaim()) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.NoSiegeAdminClaim));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // can't be on cooldown
        if (GriefPrevention.instance.dataStore.onCooldown(attacker, defender, defenderClaim)) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.SiegeOnCooldown));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // start the siege
        GriefPrevention.instance.dataStore.startSiege(attacker, defender, defenderClaim);

        // confirmation message for attacker, warning message for defender
        GriefPrevention.sendMessage(defender, TextMode.Warn, Messages.SiegeAlert, attacker.getName());
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.SiegeConfirmed, defender.getName());

        return CommandResult.success();
    }
}
