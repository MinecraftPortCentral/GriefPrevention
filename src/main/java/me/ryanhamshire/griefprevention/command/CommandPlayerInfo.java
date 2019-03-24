/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimBlockSystem;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.permission.GPOptionHandler;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommandPlayerInfo implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        List<User> userValues = new ArrayList<>(ctx.getAll("user"));
        WorldProperties worldProperties = ctx.<WorldProperties>getOne("world").orElse(Sponge.getServer().getDefaultWorld().orElse(null));
        User user = null;
        if (userValues.size() > 0) {
            user = userValues.get(0);
        }

        if (user == null) {
            if (!(src instanceof Player)) {
                GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.commandPlayerInvalid.toText());
                return CommandResult.success();
            }

            user = (User) src;
        }

        // otherwise if no permission to delve into another player's claims data or self
        if ((user != null && user != src && !src.hasPermission(GPPermissions.COMMAND_PLAYER_INFO_OTHERS))) {
            try {
                throw new CommandPermissionException();
            } catch (CommandPermissionException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }


        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(worldProperties, user.getUniqueId());
        boolean useGlobalData = DataStore.USE_GLOBAL_PLAYER_STORAGE;
        List<Claim> claimList = new ArrayList<>();
        for (Claim claim : playerData.getInternalClaims()) {
            if (useGlobalData) {
                claimList.add(claim);
            } else {
                if (claim.getWorld().getProperties().equals(worldProperties)) {
                    claimList.add(claim);
                }
            }
        }
        Text claimSizeLimit = Text.of(TextColors.GRAY, "none");
        if (playerData.optionMaxClaimSizeBasicX != 0 || playerData.optionMaxClaimSizeBasicY != 0 || playerData.optionMaxClaimSizeBasicZ != 0) {
            claimSizeLimit = Text.of(TextColors.GRAY, playerData.optionMaxClaimSizeBasicX + "," + playerData.optionMaxClaimSizeBasicY + "," + playerData.optionMaxClaimSizeBasicZ);
        }

        Text townTaxRate = Text.of(
                TextColors.GRAY, "TOWN", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionTaxRateTown, 
                TextColors.GRAY, " BASIC", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionTaxRateTownBasic, 
                TextColors.GRAY, " SUB", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionTaxRateTownSubdivision);
        Text claimTaxRate = Text.of(
                TextColors.GRAY, "BASIC", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionTaxRateBasic, 
                TextColors.GRAY, " SUB", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionTaxRateSubdivision);
        Text currentTaxRateText = Text.of(TextColors.YELLOW, "Current Claim Tax Rate", TextColors.WHITE, " : ", TextColors.RED, "N/A");
        Text claimCreateLimits = Text.of(
                TextColors.GRAY, "TOWN", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionCreateClaimLimitTown, 
                TextColors.GRAY, " BASIC", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionCreateClaimLimitBasic, 
                TextColors.GRAY, " SUB", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionCreateClaimLimitSubdivision);
        if (src instanceof Player) {
            Player player = (Player) src;
            if (player.getUniqueId().equals(user.getUniqueId())) {
                final GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(player.getLocation());
                if (claim != null && !claim.isWilderness()) {
                    final double playerTaxRate = GPOptionHandler.getClaimOptionDouble(user, claim, GPOptions.Type.TAX_RATE, playerData);
                    currentTaxRateText = Text.of(
                            TextColors.YELLOW, "Current Claim Tax Rate", TextColors.WHITE, " : ",
                            TextColors.GREEN, playerTaxRate);
                }
            }
        }
        final Text WHITE_SEMI_COLON = Text.of(TextColors.WHITE, " : ");
        final double claimableChunks = GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? (playerData.getRemainingClaimBlocks() / 65536.0) : (playerData.getRemainingClaimBlocks() / 256.0);
        final Text uuidText = Text.of(TextColors.YELLOW, "UUID", WHITE_SEMI_COLON, TextColors.GRAY, user.getUniqueId());
        final Text worldText = Text.of(TextColors.YELLOW, "World", WHITE_SEMI_COLON, TextColors.GRAY, worldProperties.getWorldName());
        final Text sizeLimitText = Text.of(TextColors.YELLOW, "Claim Size Limits", WHITE_SEMI_COLON, claimSizeLimit);
        final Text createLimitText = Text.of(TextColors.YELLOW, "Claim Create Limits", WHITE_SEMI_COLON, claimCreateLimits);
        final Text initialBlockText = Text.of(TextColors.YELLOW, "Initial Blocks", WHITE_SEMI_COLON, TextColors.GREEN, playerData.optionInitialClaimBlocks);
        final Text accruedBlockText = Text.of(TextColors.YELLOW, "Accrued Blocks", WHITE_SEMI_COLON, TextColors.GREEN, playerData.getAccruedClaimBlocks(), TextColors.GRAY, " (", TextColors.LIGHT_PURPLE, playerData.optionBlocksAccruedPerHour, TextColors.WHITE, " per hour", TextColors.GRAY, ")");
        final Text maxAccruedBlockText = Text.of(TextColors.YELLOW, "Max Accrued Blocks", WHITE_SEMI_COLON, TextColors.GREEN, playerData.optionMaxAccruedBlocks);
        final Text bonusBlockText = Text.of(TextColors.YELLOW, "Bonus Blocks", WHITE_SEMI_COLON, TextColors.GREEN, playerData.getBonusClaimBlocks());
        final Text remainingBlockText = Text.of(TextColors.YELLOW, "Remaining Blocks", WHITE_SEMI_COLON, TextColors.GREEN, playerData.getRemainingClaimBlocks());
        final Text minMaxLevelText = Text.of(TextColors.YELLOW, "Min/Max Claim Level", WHITE_SEMI_COLON, TextColors.GREEN, playerData.getMinClaimLevel() + "-" + playerData.getMaxClaimLevel());
        final Text abandonRatioText = Text.of(TextColors.YELLOW, "Abandoned Return Ratio", WHITE_SEMI_COLON, TextColors.GREEN, playerData.getAbandonedReturnRatio());
        final Text globalTownTaxText = Text.of(TextColors.YELLOW, "Global Town Tax Rate", WHITE_SEMI_COLON, TextColors.GREEN, townTaxRate);
        final Text globalClaimTaxText = Text.of(TextColors.YELLOW, "Global Claim Tax Rate", WHITE_SEMI_COLON, TextColors.GREEN, claimTaxRate);
        final Text totalTaxText = Text.of(TextColors.YELLOW, "Total Tax", WHITE_SEMI_COLON, TextColors.GREEN, playerData.getTotalTax());
        final Text totalBlockText = Text.of(TextColors.YELLOW, "Total Blocks", WHITE_SEMI_COLON, TextColors.GREEN, playerData.optionInitialClaimBlocks + playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks());
        final Text totalClaimableChunkText = Text.of(TextColors.YELLOW, "Total Claimable Chunks", WHITE_SEMI_COLON, TextColors.GREEN, Math.round(claimableChunks * 100.0)/100.0);
        final Text totalClaimText = Text.of(TextColors.YELLOW, "Total Claims", WHITE_SEMI_COLON, TextColors.GREEN, claimList.size());

        List<Text> claimsTextList = Lists.newArrayList();
        claimsTextList.add(uuidText);
        claimsTextList.add(worldText);
        claimsTextList.add(sizeLimitText);
        claimsTextList.add(createLimitText);
        claimsTextList.add(initialBlockText);
        claimsTextList.add(accruedBlockText);
        claimsTextList.add(maxAccruedBlockText);
        claimsTextList.add(bonusBlockText);
        claimsTextList.add(remainingBlockText);
        claimsTextList.add(minMaxLevelText);
        claimsTextList.add(abandonRatioText);
        if (GriefPreventionPlugin.getGlobalConfig().getConfig().claim.bankTaxSystem) {
            claimsTextList.add(currentTaxRateText);
            claimsTextList.add(globalTownTaxText);
            claimsTextList.add(globalClaimTaxText);
            claimsTextList.add(totalTaxText);
        }
        claimsTextList.add(totalBlockText);
        claimsTextList.add(totalClaimableChunkText);
        claimsTextList.add(totalClaimText);
        JoinData joinData = user.getOrCreate(JoinData.class).orElse(null);
        if (joinData != null && joinData.lastPlayed().exists()) {
            Date lastActive = null;
            try {
                lastActive = Date.from(joinData.lastPlayed().get());
            } catch(DateTimeParseException ex) {
                // ignore
            }
            if (lastActive != null) {
                claimsTextList.add(Text.of(TextColors.YELLOW, "Last Active", TextColors.WHITE, " : ", TextColors.GRAY, lastActive));
            }
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.AQUA, "Player Info")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(claimsTextList);
        paginationBuilder.sendTo(src);

        return CommandResult.success();
    }
}
