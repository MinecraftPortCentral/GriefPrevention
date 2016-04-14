package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.command.CommandGriefPrevention.CommandContainer;
import me.ryanhamshire.griefprevention.command.CommandGriefPrevention.ParentCommandContainer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CommandHelp implements CommandExecutor {

    private Map<String, CommandSpec> flattenedCommands;

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        if (flattenedCommands == null) flattenedCommands = ImmutableMap.copyOf(createFlattenedCommands());

        List<Text> helpText = new LinkedList<>();
        flattenedCommands.forEach((commandUsage, commandSpec) -> {
            if (commandSpec.testPermission(src)) {
                helpText.add(Text.builder()
                    .append(Text.of(TextColors.GOLD, "Command: "), Text.of(commandUsage))
                    .build());
                helpText.add(Text.builder()
                    .append(Text.of(TextColors.GOLD, "Command Description: "), commandSpec.getShortDescription(src).get())
                    .build());
                helpText.add(Text.builder()
                    .append(Text.of(TextColors.GOLD, "Command Arguments: "), commandSpec.getUsage(src))
                    .build());
                // Dirty hack
                String permission = commandSpec.toString().substring(
                    commandSpec.toString().lastIndexOf("permission") + 11,
                    commandSpec.toString().indexOf("argumentParser") - 2);
                if (!permission.equals("null")) helpText.add(Text.builder()
                    .append(Text.of(TextColors.GOLD, "Permission Node: "),
                        Text.of(permission))
                    .build());
                helpText.add(Text.of());
            }
        });

        PaginationService paginationService = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
        PaginationList.Builder paginationBuilder =
                paginationService.builder().title(Text.of(TextColors.AQUA, "Showing GriefPrevention Help")).padding(Text.of("-")).contents(helpText);
        paginationBuilder.sendTo(src);
        return CommandResult.success();
    }

    private Map<String, CommandSpec> createFlattenedCommands() {
        // Prefix a '/' before each key
        Map<String, CommandSpec> prefixedCommands = new LinkedHashMap<>();
        getFlattenedCommands(CommandGriefPrevention.getCommand()).entrySet().stream()
            .forEach(entry -> prefixedCommands.put("/" + entry.getKey(), entry.getValue()));
        return prefixedCommands;
    }

    private Map<String, CommandSpec> getFlattenedCommands(CommandContainer commandContainer) {
        // This returns a list of subcommands, or if there are none, the command itself
        // The keys are in the format 'my sub command', where each part is a parent command
        // If a command has sub commands, get the flattened commands for each sub command and append the primary alias to each key
        Map<String, CommandSpec> flattenedCommands = new LinkedHashMap<>();
        String primaryAlias = commandContainer.getAliases().get(0); // If this throws an exception, the CommandContainer is invalid
        flattenedCommands.put(primaryAlias, commandContainer.getCommandSpec());
        if (commandContainer instanceof ParentCommandContainer) {
            ((ParentCommandContainer) commandContainer).getChildren().stream()
                .flatMap(subCommandContainer -> getFlattenedCommands(subCommandContainer).entrySet().stream())
                .forEach(entry -> flattenedCommands.put(primaryAlias + " " + entry.getKey(), entry.getValue()));
        }
        return flattenedCommands;
    }
}
