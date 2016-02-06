package me.ryanhamshire.GriefPrevention.command;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.configuration.ClaimStorageData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;

public class CommandAddFlagPermission implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String target = ctx.<String>getOne("target").get();
        String name = ctx.<String>getOne("name").get();
        String flag = ctx.<String>getOne("flag").get();
        String value = ctx.<String>getOne("value").get();

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
            if (ClaimStorageData.flags.containsKey(flag)) {
                if (target.equalsIgnoreCase("player")) {
                    Optional<Player> targetPlayer = Sponge.getServer().getPlayer(name);
                    if (targetPlayer.isPresent()) {
                        Subject subj = targetPlayer.get().getContainingCollection().get(targetPlayer.get().getIdentifier());
                        // if (value.equalsIgnoreCase("true") ||
                        // value.equalsIgnoreCase("false")) {
                        subj.getSubjectData().setPermission(ImmutableSet.of(claim.getContext()), "griefprevention.command.claim.flag." + flag,
                                Tristate.fromBoolean(Boolean.valueOf(value)));
                        // } else if (value.contains(",")) {
                        // ArrayList<String> input = Lists.newArrayList();
                        // if (subj instanceof OptionSubject) {
                        // OptionSubject optionSubj = (OptionSubject) subj;
                        // input.addAll(Arrays.asList(value.split("\\s*,\\s*")));
                        // optionSubj.getSubjectData().setOption(ImmutableSet.of(claim.getContext()),
                        // "griefprevention.command.claim.flag." + flag, input);
                        // }
                        // } else {
                        // ArrayList<String> input = Lists.newArrayList();
                        // if (subj instanceof OptionSubject) {
                        // OptionSubject optionSubj = (OptionSubject) subj;
                        // input.add(value);
                        // optionSubj.getSubjectData().setPermission(ImmutableSet.of(claim.getContext()),
                        // "griefprevention.command.claim.flag." + flag, input);
                        // }
                        // }
                        src.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", flag, " to ", value, " for ", target, " ", name, "."));
                    } else {
                        GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Not a valid player."));
                    }
                } else if (target.equalsIgnoreCase("group")) {
                    PermissionService service = Sponge.getServiceManager().provide(PermissionService.class).get();
                    Subject subj = service.getGroupSubjects().get(name);
                    if (subj != null) {
                        subj.getSubjectData().setPermission(ImmutableSet.of(claim.getContext()), "griefprevention.command.claim.flag." + flag,
                                Tristate.fromBoolean(Boolean.valueOf(value)));
                        src.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", flag, " to ", value, " for ", target, " ", name, "."));
                    } else {
                        GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Not a valid group."));
                    }
                }
            } else {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Not a valid flag."));
            }
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim found."));
        }

        return CommandResult.success();
    }
}
