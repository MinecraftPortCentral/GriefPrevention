package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData.ClaimDataFlagsCategory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CommandClaimFlag extends BaseCommand {

    public CommandClaimFlag(String basePerm) {
        super(basePerm);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Optional<String> flag = ctx.<String>getOne("flag");
        Optional<Tristate> value = ctx.<Tristate>getOne("value");
        Optional<String> val = ctx.<String>getOne("val");

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
            if (flag.isPresent()) {
                if (player.hasPermission(this.basePerm + "." + flag.get().toLowerCase())) {
                    if (value.isPresent()) {
                        setFlagValue(src, claim, flag.get(), value.get());
                    } else if (val.isPresent()) {
                        if (val.get().contains(",") && !ctx.hasAny("r")) {
                            ArrayList<String> input = Lists.newArrayList();
                            input.addAll(Arrays.asList(val.get().split("\\s*,\\s*")));
                            setFlagValue(src, claim, flag.get(), input);
                        } else if (val.get().contains(",") && ctx.hasAny("r")) {
                            ArrayList<String> input = Lists.newArrayList();
                            input.addAll(Arrays.asList(val.get().split("\\s*,\\s*")));
                            removeFromFlagValue(src, claim, flag.get(), input);
                        } else if (ctx.hasAny("r")) {
                            ArrayList<String> input = Lists.newArrayList();
                            input.add(val.get());
                            removeFromFlagValue(src, claim, flag.get(), input);
                        }
                    }
                } else {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No permission to use this flag."));
                }
            } else {
                List<Text> flagList = Lists.newArrayList();
                ClaimDataFlagsCategory flagsCat = claim.getClaimData().getConfig().flags;
                if (claim.isSubDivision) {
                    flagsCat = claim.subDivisionData.flags;
                }

                for (String flagName : flagsCat.getFlagMap().keySet()) {
                    String flagValue = flagsCat.getFlagValue(flagName).toString().toLowerCase();
                    if (flagValue.equalsIgnoreCase("undefined")) {
                        flagValue = "default";
                    }
                    Text flagText = Text.builder().append(Text.of(TextColors.GRAY, "Flag: ",TextColors.GREEN, flagName, "\n"))
                            .append(Text.builder()
                                    .append(Text.of(TextColors.GOLD, "Value: "),
                                            Text.of(flagValue.toString()), Text.of("\n"))
                                    .build())
                            .build();
                    flagList.add(flagText);
                }

                PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
                PaginationList.Builder paginationBuilder = paginationService.builder()
                        .title(Text.of(TextColors.AQUA, "Showing GriefPrevention Flags")).padding(Text.of("-")).contents(flagList);
                paginationBuilder.sendTo(src);
            }
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim found."));
        }
        return CommandResult.success();
    }

    public static void setFlagValue(CommandSource src, Claim claim, String flag, Object value) {
        if (claim.getClaimData().getConfig().flags.getFlagValue(flag) != null) {
            try {
                if (claim.isSubDivision) {
                    claim.subDivisionData.flags.setFlagValue(flag, value);
                } else {
                    claim.getClaimData().getConfig().flags.setFlagValue(flag, value);
                }
                src.sendMessage(Text.of(TextColors.GREEN, "Set value of ", flag, " to ", value.toString()));
            } catch (Throwable t) {
                src.sendMessage(Text.of(TextColors.RED, "Value types not compatible!"));
            }
        } else {
            src.sendMessage(Text.of(TextColors.RED, "Flag not found."));
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeFromFlagValue(CommandSource src, Claim claim, String flag, ArrayList<String> value) {
        ClaimDataFlagsCategory flagsCat = claim.getClaimData().getConfig().flags;
        if (claim.isSubDivision) {
            flagsCat = claim.subDivisionData.flags;
        }

        if (flagsCat.getFlagValue(flag) != null) {
            if (flagsCat.getFlagValue(flag).getClass().equals(value.getClass())) {
                ArrayList<String> newValue = null;
                newValue = (ArrayList<String>) flagsCat.getFlagValue(flag);
                newValue.removeAll(value);
                flagsCat.setFlagValue(flag, newValue);
                src.sendMessage(Text.of(TextColors.GREEN, "Set value of ", flag, " to ",
                        flagsCat.getFlagValue(flag).toString()));
            } else {
                src.sendMessage(Text.of(TextColors.RED, "Value types not compatible!"));
            }
        } else {
            src.sendMessage(Text.of(TextColors.RED, "Flag not found."));
        }
    }
}
