package me.ryanhamshire.GriefPrevention.command;

import com.google.common.collect.Lists;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.FlagPermissions;
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
import org.spongepowered.api.service.pagination.PaginationBuilder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CommandClaimFlag implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Optional<String> flag = ctx.<String>getOne("flag");
        Optional<String> value = ctx.<String>getOne("value");

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
            if (flag.isPresent() && value.isPresent()) {
                if (playerData.ignoreClaims || player.hasPermission(FlagPermissions.PERMISSION_FLAGS_COMMAND + "." + flag.get().toLowerCase())) {
                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                        setFlagValue(src, claim, flag.get(), Boolean.valueOf(value.get()));
                    } else if (value.get().contains(",") && !ctx.hasAny("r")) {
                        ArrayList<String> input = Lists.newArrayList();
                        input.addAll(Arrays.asList(value.get().split("\\s*,\\s*")));
                        setFlagValue(src, claim, flag.get(), input);
                    } else if (value.get().contains(",") && ctx.hasAny("r")) {
                        ArrayList<String> input = Lists.newArrayList();
                        input.addAll(Arrays.asList(value.get().split("\\s*,\\s*")));
                        removeFromFlagValue(src, claim, flag.get(), input);
                    } else if (ctx.hasAny("r")) {
                        ArrayList<String> input = Lists.newArrayList();
                        input.add(value.get());
                        removeFromFlagValue(src, claim, flag.get(), input);
                    } else {
                        setFlagValue(src, claim, flag.get(), value.get());
                    }
                } else {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No permission to use this flag."));
                }
            } else {
                List<Text> flagList = Lists.newArrayList();
                for (String flagName : claim.getClaimData().getConfig().flags.getFlagMap().keySet()) {
                    Text flagValue = Text.builder().append(Text.of(TextColors.GRAY, "Flag: ", flagName, "\n")).append(Text.builder()
                            .append(Text.of(TextColors.GOLD, "Value: "), Text.of(claim.getClaimData().getConfig().flags.getFlagValue(flagName).toString()),
                                    Text.of("\n"))
                            .build()).build();
                    flagList.add(flagValue);
                }

                PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
                PaginationBuilder paginationBuilder = paginationService.builder().title(Text.of(TextColors.AQUA, "Showing GriefPrevention Flags"))
                        .paddingString("-").contents(flagList);
                paginationBuilder.sendTo(src);
            }
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim found."));
        }
        return CommandResult.success();
    }

    public static void setFlagValue(CommandSource src, Claim claim, String flag, Object value) {
        if (claim.getClaimData().getConfig().flags.getFlagValue(flag) != null) {
            if (claim.getClaimData().getConfig().flags.getFlagValue(flag).getClass().equals(value.getClass())) {
                claim.getClaimData().getConfig().flags.setFlagValue(flag, value);
                src.sendMessage(Text.of(TextColors.GREEN, "Set value of ", flag, " to ", value.toString()));
            } else {
                src.sendMessage(Text.of(TextColors.RED, "Value types not compatible!"));
            }
        } else {
            src.sendMessage(Text.of(TextColors.RED, "Flag not found."));
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeFromFlagValue(CommandSource src, Claim claim, String flag, ArrayList<String> value) {
        if (claim.getClaimData().getConfig().flags.getFlagValue(flag) != null) {
            if (claim.getClaimData().getConfig().flags.getFlagValue(flag).getClass().equals(value.getClass())) {
                ArrayList<String> newValue = (ArrayList<String>) claim.getClaimData().getConfig().flags.getFlagValue(flag);
                newValue.removeAll(value);
                claim.getClaimData().getConfig().flags.setFlagValue(flag, newValue);
                src.sendMessage(Text.of(TextColors.GREEN, "Set value of ", flag, " to ",claim.getClaimData().getConfig().flags.getFlagValue(flag).toString
                        ()));
            } else {
                src.sendMessage(Text.of(TextColors.RED, "Value types not compatible!"));
            }
        } else {
            src.sendMessage(Text.of(TextColors.RED, "Flag not found."));
        }
    }
}
