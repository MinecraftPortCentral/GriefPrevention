package me.ryanhamshire.GriefPrevention.command.claim;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;

public class CommandClaimFlag implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String flag = ctx.<String>getOne("flag").get();
        Boolean value = ctx.<Boolean>getOne("value").get();

        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
        if (claim != null) {
            if (player.hasPermission("griefprevention.command.claim.flag." + flag.toLowerCase())) {
                setFlagValue(src, claim, flag, value);
            } else {
                GriefPrevention.sendMessage(src, Texts.of(TextMode.Err, "No permission to use this flag."));
            }
        } else {
            GriefPrevention.sendMessage(src, Texts.of(TextMode.Err, "No claim found."));
        }
        return CommandResult.success();
    }

    public static void setFlagValue(CommandSource src, Claim claim, String flag, boolean value) {
        switch (flag.toLowerCase()) {
            case "explosions":
                claim.getClaimData().getConfig().flags.explosions = value;
                break;
            case "itemdrops":
                claim.getClaimData().getConfig().flags.itemDrops = value;
                break;
            case "lavaflow":
                claim.getClaimData().getConfig().flags.lavaFlow = value;
                break;
            case "mobdamage":
                claim.getClaimData().getConfig().flags.mobDamage = value;
                break;
            case "mobspawning":
                claim.getClaimData().getConfig().flags.mobSpawning = value;
                break;
            case "sleepinbeds":
                claim.getClaimData().getConfig().flags.sleepInBeds = value;
                break;
            case "waterflow":
                claim.getClaimData().getConfig().flags.waterFlow = value;
                break;
            default:
                GriefPrevention.sendMessage(src, Texts.of(TextMode.Err, "Flag invalid."));
                return;
        }
        GriefPrevention.sendMessage(src, Texts.of(TextMode.Success, "Set flag value."));
    }
}
