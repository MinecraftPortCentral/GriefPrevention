package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandClaimAdminList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        // find admin claims
        List<Claim> claims = new ArrayList<>();
        for (Map.Entry<UUID, List<Claim>> mapEntry : GriefPrevention.instance.dataStore.worldClaims.entrySet()) {
            List<Claim> claimList = mapEntry.getValue();
            for (Claim claim : claimList) {
                if (claim.ownerID == null) { // admin claim
                    claims.add(claim);
                }
            }
        }

        if (claims.size() > 0) {
            GriefPrevention.sendMessage(src, TextMode.Instr, Messages.ClaimsListHeader);
            for (int i = 0; i < claims.size(); i++) {
                Claim claim = claims.get(i);
                GriefPrevention.sendMessage(src, Text.of(TextMode.Instr, GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner())));
            }
        }

        return CommandResult.success();
    }
}
