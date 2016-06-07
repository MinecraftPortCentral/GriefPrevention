package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.IClaimData;
import me.ryanhamshire.griefprevention.configuration.ClaimTemplateConfig;
import me.ryanhamshire.griefprevention.configuration.ClaimTemplateStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CommandClaimTemplate implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        if (claim != null) {
            String action = ctx.<String>getOne("action").get();
            Optional<String> templateName = ctx.<String>getOne("name");
            Optional<String> templateDescription = ctx.<String>getOne("description");
            if (action.equalsIgnoreCase("export")) {
                if (!templateName.isPresent()) {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Please enter a valid template name to export."));
                    return CommandResult.success();
                }
                if (DataStore.globalTemplates.containsKey(templateName.get())) {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Template name ", TextColors.AQUA, templateName.get(), TextColors.WHITE, " already exists. Please choose a different name."));
                    return CommandResult.success();
                }

                ClaimTemplateStorage templateStorage = new ClaimTemplateStorage(templateName.get(), templateDescription, claim.getClaimData(), player.getUniqueId());
                DataStore.globalTemplates.put(templateName.get(), templateStorage);
                GriefPrevention.sendMessage(src, Text.of(TextMode.Success, "Created template ", TextColors.AQUA, templateName.get()));
                return CommandResult.success();
            } else if (action.equalsIgnoreCase("import")){
                if (!templateName.isPresent()) {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Please enter a valid template name to import."));
                    return CommandResult.success();
                }
                // locate template
                ClaimTemplateStorage templateStorage = DataStore.globalTemplates.get(templateName.get());
                if (templateStorage == null) {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No template with name ", TextColors.AQUA, templateName.get(), " was found."));
                    return CommandResult.success();
                }

                ClaimTemplateConfig templateData = templateStorage.getConfig();
                IClaimData claimData = claim.getClaimData();
                claimData.setClaimOwnerUniqueId(templateData.getOwnerUniqueId());
                claimData.setAccessors(new ArrayList<UUID>(templateData.getAccessors()));
                claimData.setBuilders(new ArrayList<UUID>(templateData.getBuilders()));
                claimData.setContainers(new ArrayList<UUID>(templateData.getContainers()));
                claimData.setCoowners(new ArrayList<UUID>(templateData.getCoowners()));
                claimData.setFlags(templateData.getFlags().copyFlags());
                claim.getClaimStorage().save();
                GriefPrevention.sendMessage(src, Text.of(TextMode.Success, "Imported template ", TextColors.AQUA, templateName.get(), TextColors.WHITE, " successfully."));
            } else if (action.equalsIgnoreCase("list")) {
                List<Text> templates = new ArrayList<>();
                for (Map.Entry<String, ClaimTemplateStorage> mapEntry : DataStore.globalTemplates.entrySet()) {
                    templates.add(Text.of(mapEntry.getKey(), " : ", mapEntry.getValue().getConfig().getTemplateDescription()));
                }
                PaginationService paginationService = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
                PaginationList.Builder paginationBuilder =
                        paginationService.builder().title(Text.of(TextColors.AQUA, "Global Admin Claim Templates")).padding(Text.of("-")).contents(templates);
                paginationBuilder.sendTo(src);
                return CommandResult.success();
            } else {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Template action ", TextColors.AQUA, action, TextColors.WHITE, " is not valid."));
            }
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim in your current location."));
        }

        return CommandResult.success();
    }
}
