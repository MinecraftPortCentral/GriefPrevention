package me.ryanhamshire.GriefPrevention.command;

import com.google.common.collect.Lists;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.service.pagination.PaginationBuilder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.LinkedHashMap;
import java.util.List;

public class CommandHelp implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        LinkedHashMap<List<String>, CommandSpec> gpSubcommands = GriefPrevention.instance.registerSubCommands();
        List<Text> helpList = Lists.newArrayList();

        for (List<String> aliases : gpSubcommands.keySet()) {
            CommandSpec commandSpec = gpSubcommands.get(aliases);
            if (commandSpec.testPermission(src)) {
                Text commandHelp =
                        Text.builder()
                                .append(Text
                                        .builder().append(Text.of(TextColors.GOLD, "Command: ")).append(Text.of(aliases.toString(), "\n")).build())
                                .append(Text.builder()
                                        .append(Text.of(TextColors.GOLD, "Command Description: "), commandSpec.getShortDescription(src).get(),
                                                Text.of("\n"))
                                        .build())
                                .append(Text.builder()
                                        .append(Text.of(TextColors.GOLD, "Command Arguments: "), commandSpec.getUsage(src), Text.of("\n"))
                                        .build()).append(
                                Text.builder()
                                        .append(Text.of(TextColors.GOLD, "Permission Node: "),
                                                Text.of(commandSpec.toString().substring(
                                                        commandSpec.toString().lastIndexOf("permission") + 11,
                                                        commandSpec.toString().indexOf("argumentParser") - 2)),
                                                Text.of("\n"))
                                        .build())
                                .build();
                helpList.add(commandHelp);
            }
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationBuilder paginationBuilder =
                paginationService.builder().title(Text.of(TextColors.AQUA, "Showing GriefPrevention Help")).paddingString("-").contents(helpList);
        paginationBuilder.sendTo(src);
        return CommandResult.success();
    }
}
