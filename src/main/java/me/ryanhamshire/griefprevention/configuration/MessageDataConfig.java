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
package me.ryanhamshire.griefprevention.configuration;

import me.ryanhamshire.griefprevention.configuration.category.ConfigCategory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextColors;

@ConfigSerializable
public class MessageDataConfig extends ConfigCategory {

    @Setting(MessageStorage.ABANDON_CLAIM_ADVERTISEMENT)
    public TextTemplate abandonClaimAdvertisement = TextTemplate.of("To delete another claim and free up some blocks, use /AbandonClaim.");

    @Setting(MessageStorage.ABANDON_OTHER_SUCCESS)
    public TextTemplate abandonOtherSuccess = TextTemplate.of(TextColors.GREEN, TextTemplate.arg("player"), "'s claim has been abandoned.", TextTemplate.arg("player"), " now has ", TextTemplate.arg("total"), " available claim blocks.");

    @Setting("access")
    public TextTemplate access = TextTemplate.of("Access");

    @Setting(MessageStorage.ADJUST_BLOCKS_SUCCESS)
    public TextTemplate adjustBlocksSuccess = TextTemplate.of(TextColors.GREEN, "Adjusted ", TextTemplate.arg("player"), "'s bonus claim blocks by ", TextTemplate.arg("adjustment"), ".  New total bonus blocks: ", TextTemplate.arg("total"), ".");

    @Setting(MessageStorage.ADJUST_GROUP_BLOCKS_SUCCESS)
    public TextTemplate adjustGroupBlocksSuccess = TextTemplate.of(TextColors.GREEN, "Adjusted bonus claim blocks for players with the ", TextTemplate.arg("permission"), " permission by ", TextTemplate.arg("amount"), ".  New total: ", TextTemplate.arg("bonus"), ".");

    @Setting("advertise-acb")
    public TextTemplate advertiseAcb = TextTemplate.of(TextColors.GREEN, "You may use /ACB to give yourself more claim blocks.");

    @Setting("advertise-ac-and-acb")
    public TextTemplate advertiseAcAndAcb = TextTemplate.of(TextColors.GREEN, "You may use /ACB to give yourself more claim blocks, or /AdminClaims to create a free administrative claim.");

    @Setting("advertise-admin-claims")
    public TextTemplate advertiseAdminClaims = TextTemplate.of(TextColors.GREEN, "You could create an administrative land claim instead using /AdminClaims, which you'd share with other administrators.");

    @Setting("avoid-grief-claim-land")
    public TextTemplate avoidGriefClaimLand = TextTemplate.of(TextColors.GREEN, "Prevent grief!  If you claim your land, you will be grief-proof.");

    @Setting("block-change-from-wilderness")
    public TextTemplate blockChangeFromWilderness = TextTemplate.of(TextColors.RED, "Claim blocks are not allowed to be changed from wilderness.");

    @Setting(MessageStorage.BLOCK_CLAIMED)
    public TextTemplate blockClaimed = TextTemplate.of(TextColors.GREEN, "That block has been claimed by ", TextTemplate.arg("owner").color(TextColors.GOLD), ".");

    @Setting("block-not-claimed")
    public TextTemplate blockNotClaimed = TextTemplate.of(TextColors.RED, "No one has claimed this block.");

    @Setting(MessageStorage.BLOCK_SALE_VALUE)
    public TextTemplate blockSaleValue = TextTemplate.of(TextColors.GREEN, "Each claim block is worth ", TextTemplate.arg("block-value"), ".  You have ", TextTemplate.arg("available-blocks"), " available for sale.");

    @Setting("book-author")
    public TextTemplate bookAuthor = TextTemplate.of("BigScary");

    @Setting("book-disabled-chest-claims")
    public TextTemplate bookDisabledChestClaims = TextTemplate.of(TextColors.RED, "On this server, placing a chest will NOT claim land for you.");

    @Setting("book-intro")
    public TextTemplate bookIntro = TextTemplate.of(TextColors.GREEN, "Claim land to protect your stuff!  Click the link above to learn land claims in 3 minutes or less.  :)");

    @Setting("book-link")
    public TextTemplate bookLink = TextTemplate.of(TextColors.GREEN, "Click: http://bit.ly/mcgpuser");

    @Setting("book-title")
    public TextTemplate bookTitle = TextTemplate.of("How to Claim Land");

    @Setting(MessageStorage.BOOK_TOOLS)
    public TextTemplate bookTools = TextTemplate.of("Our claim tools are ", TextTemplate.arg("modification-tool"), " and ", TextTemplate.arg("information-tool"), ".");

    @Setting("book-useful-commands")
    public TextTemplate bookUsefulCommands = TextTemplate.of("Useful Commands:");

    @Setting("building-outside-claims")
    public TextTemplate buildingOutsideClaims = TextTemplate.of("Other players can build here, too.  Consider creating a land claim to protect your work!");

    @Setting("cant-transfer-admin-claim")
    public TextTemplate cantTransferAdminClaim = TextTemplate.of(TextColors.RED, "You don't have permission to transfer administrative claims.");

    @Setting("chat-how-to-claim-regex")
    public TextTemplate chatHowToClaimRegex = TextTemplate.of("(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)", "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how " + "to claim land.");

    @Setting(MessageStorage.CLAIM_ABANDON_SUCCESS)
    public TextTemplate claimAbandonSuccess = TextTemplate.of(TextColors.GREEN, "Claim abandoned. You now have ", TextTemplate.arg("remaining-blocks"), " available claim blocks.");

    @Setting("claim-automatic-notification")
    public TextTemplate claimAutomaticNotification = TextTemplate.of(TextColors.RED, "This chest and nearby blocks are protected from breakage and theft.");

    @Setting("claim-bank-tax-system-not-enabled")
    public TextTemplate claimBankTaxSystemNotEnabled = TextTemplate.of(TextColors.RED, "The bank/tax system is not enabled. If you want it enabled, set 'bank-tax-system' to true in config.");

    @Setting("claim-bank-info")
    public TextTemplate claimBankInfo = TextTemplate.of(Text.of(TextColors.GREEN, "Balance: "), TextTemplate.arg("balance").color(TextColors.GOLD), 
            Text.of(TextColors.GREEN, "\nTax: "), TextTemplate.arg("amount").color(TextColors.GOLD), Text.of(TextColors.WHITE, " due in "), TextTemplate.arg("time_remaining").color(TextColors.GRAY),
            Text.of(TextColors.GREEN, "\nTax Owed: "), TextTemplate.arg("tax_balance").color(TextColors.GOLD));

    @Setting("claim-bank-deposit")
    public TextTemplate claimBankDeposit = TextTemplate.of(TextColors.GREEN, "Successful deposit of ", TextTemplate.arg("amount").color(TextColors.GOLD), " into bank.");

    @Setting("claim-bank-deposit-no-funds")
    public TextTemplate claimBankDepositNoFunds = TextTemplate.of(TextColors.RED, "You do not have enough funds to deposit into the bank.");

    @Setting("claim-bank-no-permission")
    public TextTemplate claimBankNoPermission = TextTemplate.of(TextColors.RED, "You don't have permission to manage", TextTemplate.arg("owner"), "'s claim bank.");

    @Setting("claim-bank-withdraw")
    public TextTemplate claimBankWithdraw = TextTemplate.of(TextColors.GREEN, "Successful withdraw of ", TextTemplate.arg("amount").color(TextColors.GOLD), " from bank.");

    @Setting("claim-bank-withdraw-no-funds")
    public TextTemplate claimBankWithdrawNoFunds = TextTemplate.of(TextColors.RED, "The claim bank has a remaining balance of ", TextTemplate.arg("balance").color(TextColors.GOLD), " and does not have enough funds to withdraw ", TextTemplate.arg("amount").color(TextColors.GOLD), ".");

    @Setting("claim-block-purchase-limit")
    public TextTemplate claimBlockPurchaseLimit = TextTemplate.of(TextColors.RED, "The new claim block total of ", TextTemplate.arg("new_total").color(TextColors.GOLD), " will exceed your claim block limit of ", TextTemplate.arg("block_limit").color(TextColors.GREEN), ". The transaction has been cancelled.");

    @Setting("claim-chest-confirmation")
    public TextTemplate claimChestConfirmation = TextTemplate.of(TextColors.RED, "This chest is protected.");

    @Setting("claim-chest-full")
    public TextTemplate claimChestFull = TextTemplate.of(TextColors.RED, "This chest is full.");

    @Setting("claim-chest-outside-level")
    public TextTemplate claimChestOutsideLevel = TextTemplate.of(TextColors.RED, "This chest can't be protected as the position is outside your claim level limits of ", TextTemplate.arg("min-claim-level").color(TextColors.GREEN), " and ", TextTemplate.arg("max-claim-level").color(TextColors.GREEN), ". (/playerinfo)");

    @Setting("claim-children-warning")
    public TextTemplate claimChildrenWarning = TextTemplate.of("This claim includes child claims.  If you're sure you want to delete it, use /DeleteClaim again.");

    @Setting("claim-create-only-subdivision")
    public TextTemplate claimCreateOnlySubdivision = TextTemplate.of(TextColors.RED, "Unable to create claim. Only subdivisions can be created at a single block location.");

    @Setting("claim-create-cuboid-disabled")
    public TextTemplate claimCreateCuboidDisabled = TextTemplate.of(TextColors.RED, "The creation of 3D cuboid claims has been disabled by an administrator.\nYou can only create 3D claims as an Admin or on a 2D claim that you own.");

    @Setting("claim-create-overlap")
    public TextTemplate claimCreateOverlap = TextTemplate.of(TextColors.RED, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.");

    @Setting("claim-create-overlap-player")
    public TextTemplate claimCreateOverlapPlayer = TextTemplate.of(TextColors.RED, "You can't create a claim here because it would overlap ", TextTemplate.arg("owner"), "'s claim.");

    @Setting("claim-create-overlap-short")
    public TextTemplate claimCreateOverlapShort = TextTemplate.of(TextColors.RED, "Your selected area overlaps an existing claim.");

    @Setting("claim-create-success")
    public TextTemplate claimCreateSuccess = TextTemplate.of(TextColors.GREEN, TextTemplate.arg("type"), " created!  Use /trust to share it with friends.");

    @Setting("claim-create-error-result")
    public TextTemplate claimCreateErrorResult = TextTemplate.of(TextColors.RED, "Unable to create claim due to error result ", TextTemplate.arg("result"), ".");

    @Setting("claim-cleanup-warning")
    public TextTemplate claimCleanupWarning = TextTemplate.of("The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.");

    @Setting("claim-context-not-found")
    public TextTemplate claimContextNotFound = TextTemplate.of(TextColors.RED, "Context '", TextTemplate.arg("context"), "' was not found.");

    @Setting(MessageStorage.CLAIM_SIZE_MAX_X)
    public TextTemplate claimSizeMaxX = TextTemplate.of(TextColors.RED, "The claim x size of ", TextTemplate.arg("size").color(TextColors.GREEN), " exceeds the max size of ", TextTemplate.arg("max-size").color(TextColors.GREEN), ".\nThe area needs to be a mininum of ", TextTemplate.arg("min-area").color(TextColors.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColors.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MAX_Y)
    public TextTemplate claimSizeMaxY = TextTemplate.of(TextColors.RED, "The claim y size of ", TextTemplate.arg("size").color(TextColors.GREEN), " exceeds the max size of ", TextTemplate.arg("max-size").color(TextColors.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColors.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColors.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MAX_Z)
    public TextTemplate claimSizeMaxZ = TextTemplate.of(TextColors.RED, "The claim z size of ", TextTemplate.arg("size").color(TextColors.GREEN), " exceeds the max size of ", TextTemplate.arg("max-size").color(TextColors.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColors.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColors.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MIN_X)
    public TextTemplate claimSizeMinX = TextTemplate.of(TextColors.RED, "The claim x size of ", TextTemplate.arg("size").color(TextColors.GREEN), " is below the minimum size of ", TextTemplate.arg("min-size").color(TextColors.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColors.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColors.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MIN_Y)
    public TextTemplate claimSizeMinY = TextTemplate.of(TextColors.RED, "The claim y size of ", TextTemplate.arg("size").color(TextColors.GREEN), " is below the minimum size of ", TextTemplate.arg("min-size").color(TextColors.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColors.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColors.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MIN_Z)
    public TextTemplate claimSizeMinZ = TextTemplate.of(TextColors.RED, "The claim z size of ", TextTemplate.arg("size").color(TextColors.GREEN), " is below the minimum size of ", TextTemplate.arg("min-size").color(TextColors.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColors.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColors.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_TOO_SMALL)
    public TextTemplate claimSizeTooSmall = TextTemplate.of(TextColors.RED, "The selected claim size of ", TextTemplate.arg("width"), "x", TextTemplate.arg("length"), " would be too small. A claim must be at least ", TextTemplate.arg("minimum-width"), "x", TextTemplate.arg("minimum-length"), " in size.");

    @Setting("claim-create-failed-claim-limit")
    public TextTemplate claimCreateFailedLimit = TextTemplate.of(TextColors.RED, "You've reached your limit on land claims.  Use /AbandonClaim to remove one before creating another.");

    @Setting(MessageStorage.CLAIM_CREATE_INSUFFICIENT_BLOCKS_2D)
    public TextTemplate claimCreateInsufficientBlocks2d = TextTemplate.of(TextColors.RED, "You don't have enough blocks to claim this area.\nYou need ", TextTemplate.arg("remaining-blocks").color(TextColors.GOLD), " more blocks.");

    @Setting(MessageStorage.CLAIM_CREATE_INSUFFICIENT_BLOCKS_3D)
    public TextTemplate claimCreateInsufficientBlocks3d = TextTemplate.of(TextColors.RED, "You don't have enough blocks to claim this area.\nYou need ", TextTemplate.arg("remaining-chunks").color(TextColors.GOLD), " more chunks. (", TextTemplate.arg("remaining-blocks").color(TextColors.WHITE), " blocks)");

    @Setting("claim-create-subdivision-fail")
    public TextTemplate claimCreateSubdivisionFail = TextTemplate.of(TextColors.RED, "No claim exists at selected corner. Please click a valid block location within parent claim in order to create your subdivision.");

    @Setting("claim-delete-all-admin-success")
    public TextTemplate claimDeleteAllAdminSuccess = TextTemplate.of("Deleted all administrative claims.");

    @Setting("claim-delete-all-success")
    public TextTemplate claimDeleteAllSuccess = TextTemplate.of("Deleted all of ", TextTemplate.arg("owner"), "'s claims.");

    @Setting("claim-protected-entity")
    public TextTemplate claimProtectedEntity = TextTemplate.of(TextColors.RED, "That belongs to ", TextTemplate.arg("owner"), ".");

    @Setting("claim-explosives-advertisement")
    public TextTemplate claimExplosivesAdvertisement = TextTemplate.of("To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.");

    @Setting("claim-farewell")
    public TextTemplate claimFarewell = TextTemplate.of(TextColors.GREEN, "Set claim farewell to ", TextTemplate.arg("farewell").optional(), ".");

    @Setting("claim-greeting")
    public TextTemplate claimGreeting = TextTemplate.of(TextColors.GREEN, "Set claim greeting to ", TextTemplate.arg("greeting").optional(), ".");

    @Setting("claim-ignore")
    public TextTemplate claimIgnore = TextTemplate.of(TextColors.GREEN, "Now ignoring claims.");

    @Setting("claim-last-active")
    public TextTemplate claimLastActive = TextTemplate.of(TextColors.GREEN, "Claim last active ", TextTemplate.arg("date"), ".");

    @Setting("claim-mode-admin")
    public TextTemplate claimModeAdmin = TextTemplate.of(TextColors.GREEN, "Administrative claims mode active. Any claims created will be free and editable by other administrators.");

    @Setting("claim-mode-basic")
    public TextTemplate claimModeBasic = TextTemplate.of(TextColors.GREEN, "Basic claim creation mode enabled.");

    @Setting("claim-mode-subdivision")
    public TextTemplate claimModeSubdivision = TextTemplate.of(TextColors.GREEN, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.");

    @Setting("claim-mode-town")
    public TextTemplate claimModeTown = TextTemplate.of(TextColors.GREEN, "Town creation mode enabled.");

    @Setting("claim-type-not-found")
    public TextTemplate claimTypeNotFound = TextTemplate.of(TextColors.RED, "No ", TextTemplate.arg("type"), " claims found.");

    @Setting("claim-not-found")
    public TextTemplate claimNotFound = TextTemplate.of(TextColors.RED, "There's no claim here.");

    @Setting("claim-not-yours")
    public TextTemplate claimNotYours = TextTemplate.of(TextColors.RED, "This isn't your claim.");

    @Setting("claim-no-claims")
    public TextTemplate claimNoClaims = TextTemplate.of(TextColors.RED, "You don't have any land claims.");

    @Setting("claim-owner-already")
    public TextTemplate claimOwnerAlready = TextTemplate.of(TextColors.RED, "You are already the claim owner.");

    @Setting("claim-owner-only")
    public TextTemplate claimOwnerOnly = TextTemplate.of(TextColors.RED, "Only ", TextTemplate.arg("owner"), " can modify this claim.");

    @Setting(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_2D)
    public TextTemplate claimSizeNeedBlocks2d = TextTemplate.of(TextColors.RED, "You don't have enough blocks for this claim size.\nYou need ", TextTemplate.arg("blocks").color(TextColors.GREEN), " more blocks.");

    @Setting(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_3D)
    public TextTemplate claimSizeNeedBlocks3d = TextTemplate.of(TextColors.RED, "You don't have enough blocks for this claim size.\nYou need ", TextTemplate.arg("chunks").color(TextColors.GREEN), " more chunks. (", TextTemplate.arg("blocks").color(TextColors.WHITE), " blocks)");

    @Setting("claim-resize-same-location")
    public TextTemplate claimResizeSameLocation = TextTemplate.of(TextColors.RED, "You must select a different block location to resize claim.");

    @Setting("claim-resize-overlap-subdivision")
    public TextTemplate claimResizeOverlapSubdivision = TextTemplate.of(TextColors.RED, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use " + "your shovel at a corner to resize it.");

    @Setting("claim-resize-overlap")
    public TextTemplate claimResizeOverlap = TextTemplate.of(TextColors.RED, "Can't resize here because it would overlap another nearby claim.");

    @Setting("claim-resize-start")
    public TextTemplate claimResizeStart = TextTemplate.of(TextColors.GREEN, "Resizing claim.  Use your shovel again at the new location for this corner.");

    @Setting("claim-resize-success-2d")
    public TextTemplate claimResizeSuccess = TextTemplate.of(TextColors.GREEN, "Claim resized. You have ", TextTemplate.arg("remaining-blocks").color(TextColors.GOLD), " more blocks remaining.");

    @Setting(MessageStorage.CLAIM_RESIZE_SUCCESS_3D)
    public TextTemplate claimResizeSuccess3d = TextTemplate.of(TextColors.GREEN, "Claim resized. You have ", TextTemplate.arg("remaining-chunks").color(TextColors.GOLD), " more chunks remaining.\n(", TextTemplate.arg("remaining-blocks").color(TextColors.WHITE), " blocks)");

    @Setting("claim-respecting")
    public TextTemplate claimRespecting = TextTemplate.of(TextColors.GREEN, "Now respecting claims.");

    @Setting("claim-show-nearby")
    public TextTemplate claimShowNearby = TextTemplate.of(TextColors.GREEN, "Found ", TextTemplate.arg("claim-count"), " land claims.");

    @Setting("claim-start")
    public TextTemplate claimStart = TextTemplate.of(TextColors.GREEN, TextTemplate.arg("type"), " corner set! Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.");

    @Setting("claim-tax-info")
    public TextTemplate claimTaxInfo = TextTemplate.of(TextColors.GREEN, "Your next scheduled tax payment of ", TextColors.GOLD, TextTemplate.arg("amount"), " will be withdrawn from your account on ", TextColors.AQUA, TextTemplate.arg("withdraw_date"), ".");

    @Setting("claim-tax-past-due")
    public TextTemplate claimTaxPastDue = TextTemplate.of(TextColors.RED, "You currently have a past due tax balance of ", TextColors.GOLD, TextTemplate.arg("balance_due"), TextColors.RED, " that must be paid by ", TextColors.AQUA, TextTemplate.arg("due_date"), TextColors.RED, ". Failure to pay off your tax balance will result in losing your property.");

    @Setting(MessageStorage.CLAIM_ABOVE_LEVEL)
    public TextTemplate claimAboveLevel = TextTemplate.of(TextColors.RED, "Unable to claim block as it is above your maximum claim level limit of ", TextTemplate.arg("claim-level").color(TextColors.GREEN), ". (/playerinfo)");

    @Setting(MessageStorage.CLAIM_BELOW_LEVEL)
    public TextTemplate claimBelowLevel = TextTemplate.of(TextColors.RED, "Unable to claim block as it is below your minimum claim level limit of ", TextTemplate.arg("claim-level").color(TextColors.GREEN), ". (/playerinfo)");

    @Setting("claim-too-far")
    public TextTemplate claimTooFar = TextTemplate.of(TextColors.RED, "That's too far away.");

    @Setting("claim-too-small-for-entities")
    public TextTemplate claimTooSmallEntities = TextTemplate.of(TextColors.RED, "This claim isn't big enough for that. Try enlarging it.");

    @Setting("claim-too-many-entities")
    public TextTemplate claimTooManyEntities = TextTemplate.of(TextColors.RED, "This claim has too many entities already. Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.");

    @Setting("claim-transfer-success")
    public TextTemplate claimTransferSuccess = TextTemplate.of(TextColors.GREEN, "Claim transferred.");

    @Setting("claim-transfer-exceeds-limit")
    public TextTemplate claimTransferExceedsLimit = TextTemplate.of(TextColors.RED, "Claim could not be transferred as it would exceed the new owner's creation limit.");

    @Setting("claim-deleted")
    public TextTemplate claimDeleted = TextTemplate.of(TextColors.GREEN, "Claim deleted.");

    @Setting("claim-disabled-world")
    public TextTemplate claimDisabledWorld = TextTemplate.of(TextColors.RED, "Land claims are disabled in this world.");

    @Setting("claim-list-header")
    public TextTemplate claimListHeader = TextTemplate.of("Claims:");

    @Setting("collective-public")
    public TextTemplate collectivePublic = TextTemplate.of("the public", "as in 'granted the public permission to...'");

    @Setting("command-abandon-claim-missing")
    public TextTemplate commandAbandonClaimMissing = TextTemplate.of("Stand in the claim you want to delete, or consider /AbandonAllClaims.");

    @Setting("command-abandon-top-level")
    public TextTemplate commandAbandonTopLevel = TextTemplate.of(TextColors.RED, "This claim cannot be abandoned as it contains one or more child claims. In order to abandon a claim with child claims, you must use /AbandonTopLevelClaim instead.");

    @Setting("command-abandon-town-children")
    public TextTemplate commandAbandonTownChildren = TextTemplate.of(TextColors.RED, "You do not have permission to abandon a town with child claims you do not own. Use /ignoreclaims or have the child claim owner abandon their claim first. If you just want to abandon the town without affecting children then use /abandonclaim instead.");

    @Setting("command-ban-item")
    public TextTemplate commandBanItem = TextTemplate.of(TextColors.RED, "No item id was specified and player is not holding an item.");

    @Setting("command-banned-in-pvp")
    public TextTemplate commandBannedInPvp = TextTemplate.of(TextColors.RED, "You can't use that command while in PvP combat.");

    @Setting("command-blocked")
    public TextTemplate commandBlocked = TextTemplate.of(TextColors.RED, "The command ", TextTemplate.of("command"), " has been blocked by claim owner ", TextTemplate.arg("owner"), ".");

    @Setting("command-create-worldedit")
    public TextTemplate commandCreateWorldEdit = TextTemplate.of(TextColors.RED, "This command requires WorldEdit to be installed on server.");

    @Setting("command-farewell")
    public TextTemplate commandFarewell = TextTemplate.of(TextColors.GREEN, "Claim flag ", TextTemplate.arg("flag").optional(), " is invalid.");

    @Setting("command-greeting")
    public TextTemplate commandGreeting = TextTemplate.of(TextColors.GREEN, "Set claim greeting to ", TextTemplate.arg("greeting").optional(), ".");

    @Setting("command-group-invalid")
    public TextTemplate commandGroupInvalid = TextTemplate.of(TextColors.RED, "Group ", TextTemplate.arg("group"), " is not valid.");

    @Setting("command-inherit")
    public TextTemplate commandInherit = TextTemplate.of(TextColors.RED, "This command can only be used in child claims.");

    @Setting("command-claim-name")
    public TextTemplate commandClaimName = TextTemplate.of(TextColors.GREEN, "Set claim name to ", TextTemplate.arg("name"), ".");

    @Setting("command-option-exceeds-admin")
    public TextTemplate commandOptionExceedsAdmin = TextTemplate.of(TextColors.RED, "Option value of ", TextTemplate.arg("original_value").color(TextColors.GREEN), " exceeds admin set value of '", TextTemplate.arg("admin_value").color(TextColors.GREEN),"'. Adjusting to admin value...");

    @Setting("command-option-invalid-claim")
    public TextTemplate commandOptionInvalidClaim = TextTemplate.of(TextColors.RED, "This command cannot be used in subdivisions.");

    @Setting("command-pet-confirmation")
    public TextTemplate commandPetConfirmation = TextTemplate.of(TextColors.GREEN, "Pet transferred.");

    @Setting("command-pet-invalid")
    public TextTemplate commandPetInvalid = TextTemplate.of(TextColors.RED, "Pet type ", TextTemplate.arg("type"), " is invalid, only vanilla entities are supported.");

    @Setting("command-pet-transfer-ready")
    public TextTemplate commandPetTransferReady = TextTemplate.of(TextColors.GREEN, "Ready to transfer!  Right-click the pet you'd like to give away, or cancel with /GivePet cancel.");

    @Setting("command-pet-transfer-cancel")
    public TextTemplate commandPetTransferCancel = TextTemplate.of("Pet giveaway cancelled.");

    @Setting("command-player-invalid")
    public TextTemplate commandPlayerInvalid = TextTemplate.of(TextColors.RED, "Player ", TextTemplate.arg("player"), " is not valid.");

    @Setting("command-player-group-invalid")
    public TextTemplate commandPlayerGroupInvalid = TextTemplate.of(TextColors.RED, "Not a valid player or group.");

    @Setting("command-acb-success")
    public TextTemplate commandAcbSuccess = TextTemplate.of(TextColors.GREEN, "Updated accrued claim blocks.");
 
    @Setting("command-spawn-not-set")
    public TextTemplate commandSpawnNotSet = TextTemplate.of(TextColors.RED, "No claim spawn has been set.");

    @Setting("command-spawn-set-success")
    public TextTemplate commandSpawnSet = TextTemplate.of(TextColors.GREEN, "Successfully set claim spawn to ", TextTemplate.arg("location"), ".");

    @Setting("command-spawn-teleport")
    public TextTemplate commandSpawnTeleport = TextTemplate.of(TextColors.GREEN, "Teleported to claim spawn at ", TextTemplate.arg("location"), ".");

    @Setting("confirm-fluid-removal")
    public TextTemplate confirmFluidRemoval = TextTemplate.of("Abandoning this claim will remove lava inside the claim.  If you're sure, use /AbandonClaim again.");

    @Setting("context-invalid")
    public TextTemplate contextInvalid = TextTemplate.of(TextColors.RED, "Context '", TextTemplate.arg("context").color(TextColors.AQUA), "' is invalid.");

    @Setting("cuboid-claim-disabled")
    public TextTemplate claimCuboidDisabled = TextTemplate.of(TextColors.GREEN, "Now claiming in 2D mode.");

    @Setting("cuboid-claim-enabled")
    public TextTemplate claimCuboidEnabled = TextTemplate.of(TextColors.GREEN, "Now claiming in 3D mode.");

    @Setting("economy-blocks-purchase-cost")
    public TextTemplate economyBlockPurchaseCost = TextTemplate.of("Each claim block costs ", TextTemplate.arg("cost"), ".  Your balance is ", TextTemplate.arg("balance"), ".");

    @Setting("economy-blocks-not-available")
    public TextTemplate economyBlocksNotAvailable = TextTemplate.of(TextColors.RED, "You don't have that many claim blocks available for sale.");

    @Setting("economy-blocks-buy-sell-not-configured")
    public TextTemplate economyBuySellNotConfigured = TextTemplate.of(TextColors.RED, "Sorry, buying and selling claim blocks is disabled.");

    @Setting("economy-blocks-buy-invalid-count")
    public TextTemplate economyBuyInvalidBlockCount = TextTemplate.of(TextColors.RED, "Block count must be greater than 0.");

    @Setting("economy-not-installed")
    public TextTemplate economyNotInstalled = TextTemplate.of(TextColors.RED, "Economy plugin not installed!.");

    @Setting("economy-blocks-only-buy")
    public TextTemplate economyOnlyBuyBlocks = TextTemplate.of(TextColors.RED, "Claim blocks may only be purchased, not sold.");

    @Setting("economy-blocks-only-sell")
    public TextTemplate economyOnlySellBlocks = TextTemplate.of(TextColors.RED, "Claim blocks may only be sold, not purchased.");

    @Setting("economy-blocks-purchase-confirmation")
    public TextTemplate economyBlocksPurchaseConfirmation = TextTemplate.of(TextColors.GREEN, "Withdrew ", TextTemplate.arg("cost"), " from your account.  You now have ", TextTemplate.arg("remaining-blocks"), " available claim blocks.");

    @Setting("economy-blocks-sale-confirmation")
    public TextTemplate economyBlockSaleConfirmation = TextTemplate.of(TextColors.GREEN, "Deposited ", TextTemplate.arg("deposit"), " in your account.  You now have ", TextTemplate.arg("remaining-blocks"), " available claim blocks.");

    @Setting("economy-blocks-sell-error")
    public TextTemplate economyBlockSellError = TextTemplate.of(TextColors.RED, "Could not sell blocks. Reason: ", TextTemplate.arg("reason"), ".");

    @Setting("economy-claim-not-for-sale")
    public TextTemplate economyClaimNotForSale = TextTemplate.of(TextColors.RED, "This claim is not for sale.");

    @Setting("economy-claim-buy-not-enough-funds")
    public TextTemplate economyClaimBuyNotEnoughFunds = TextTemplate.of(TextColors.RED, "You do not have enough funds to purchase this claim for ", TextTemplate.arg("sale_price").color(TextColors.GOLD), ". You currently have a balance of ", TextTemplate.arg("balance").color(TextColors.GOLD), " and need ", TextTemplate.arg("amount_needed").color(TextColors.GOLD), " more for purchase.");

    @Setting("economy-claim-buy-confirmation")
    public TextTemplate economyClaimBuyConfirmation = TextTemplate.of(TextColors.GREEN, "Are you sure you want to buy this claim for ", TextTemplate.arg("sale_price").color(TextColors.GOLD), " ? Click confirm to proceed.");

    @Setting("economy-claim-buy-confirmed")
    public TextTemplate economyClaimBuyConfirmed = TextTemplate.of(TextColors.GREEN, "You have successfully bought the claim for ", TextTemplate.arg("sale_price").color(TextColors.GOLD), ".");

    @Setting("economy-claim-sale-cancelled")
    public TextTemplate economyClaimSaleCancelled = TextTemplate.of(TextColors.GREEN, "You have cancelled your claim sale.");

    @Setting("economy-claim-sale-confirmation")
    public TextTemplate economyClaimSaleConfirmation = TextTemplate.of(TextColors.GREEN, "Are you sure you want to sell your claim for ", TextTemplate.arg("sale_price").color(TextColors.GOLD), " ? If your claim is sold, all items and blocks will be transferred to the buyer. Click confirm if this is OK.");

    @Setting(MessageStorage.ECONOMY_CLAIM_SALE_CONFIRMED)
    public TextTemplate economyClaimSaleConfirmed = TextTemplate.of(TextColors.GREEN, "You have successfully put your claim up for sale for the amount of ", TextTemplate.arg("sale_price").color(TextColors.GOLD), ".");

    @Setting("economy-claim-sale-invalid-price")
    public TextTemplate economyClaimSaleInvalidPrice = TextTemplate.of(TextColors.RED, "The sale price of, ", TextTemplate.arg("sale_price").color(TextColors.GOLD), " must be greater than or equal to 0.");

    @Setting("economy-claim-sold")
    public TextTemplate economyClaimSold = TextTemplate.of(TextColors.GREEN, "Your claim sold! The amount of ", TextTemplate.arg("amount").color(TextColors.GOLD), " has been deposited into your account. Your total available balance is now ", TextTemplate.arg("balance").color(TextColors.GOLD));

    @Setting(MessageStorage.ECONOMY_USER_NOT_FOUND)
    public TextTemplate economyUserNotFound = TextTemplate.of(TextColors.RED, "No economy account found for user ", TextTemplate.arg("user"), ".");

    @Setting("economy-withdraw-error")
    public TextTemplate economyWithdrawError = TextTemplate.of(TextColors.RED, "Could not withdraw funds. Reason: ", TextTemplate.arg("reason"), ".");

    @Setting("economy-virtual-not-supported")
    public TextTemplate economyVirtualNotSupported = TextTemplate.of(TextColors.RED, "Economy plugin does not support virtual accounts which is required. Use another economy plugin or contact plugin dev for virtual account support.");

    @Setting("flag-invalid-context")
    public TextTemplate flagInvalidContext = TextTemplate.of(TextColors.RED, "Invalid context '", TextTemplate.arg("context") + "' entered for base flag ", TextTemplate.arg("flag"), ".");

    @Setting("flag-invalid-meta")
    public TextTemplate flagInvalidMeta = TextTemplate.of(TextColors.RED, "Invalid target meta '", TextTemplate.arg("meta") + "' entered for base flag ", TextTemplate.arg("flag"), ".");

    @Setting("flag-invalid-target")
    public TextTemplate flagInvalidTarget = TextTemplate.of(TextColors.RED, "Invalid target '", TextTemplate.arg("target"), "' entered for base flag ", TextTemplate.arg("flag"), ".");

    @Setting("flag-overridden")
    public TextTemplate flagOverridden = TextTemplate.of(TextColors.RED, "Failed to set claim flag. The flag ", TextTemplate.arg("flag"), " has been overridden by an admin.");

    @Setting("flag-overrides-not-supported")
    public TextTemplate flagOverridesNotSupported = TextTemplate.of("Claim type ", TextTemplate.arg("type"), " does not support flag overrides.");

    @Setting("flag-reset-success")
    public TextTemplate flagResetSuccess = TextTemplate.of(TextColors.GREEN, "Claim flags reset to defaults successfully.");

    @Setting("nucleus-no-sethome")
    public TextTemplate nucleusNoSetHome = TextTemplate.of(TextColors.RED, "You must be trusted in order to use /sethome here.");

    @Setting("owner-admin")
    public TextTemplate ownerAdmin = TextTemplate.of("an administrator");

    @Setting("permission-access")
    public TextTemplate permissionAccess = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("player").color(TextColors.GOLD), "'s permission to access that.");

    @Setting("permission-build")
    public TextTemplate permissionBuild = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("player").color(TextColors.GOLD), "'s permission to build.");

    @Setting("permission-build-near-claim")
    public TextTemplate permissionBuildNearClaim = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("owner").color(TextColors.GOLD), "'s permission to build near claim.");

    @Setting("permission-claim-create")
    public TextTemplate permissionClaimCreate = TextTemplate.of(TextColors.RED, "You don't have permission to claim land.");

    @Setting("permission-claim-delete")
    public TextTemplate permissionClaimDelete = TextTemplate.of(TextColors.RED, "You don't have permission to delete ", TextTemplate.arg("type"), " claims.");

    @Setting("permission-claim-enter")
    public TextTemplate permissionClaimEnter = TextTemplate.of(TextColors.RED, "You don't have permission to enter this claim.");

    @Setting("permission-claim-exit")
    public TextTemplate permissionClaimExit = TextTemplate.of(TextColors.RED, "You don't have permission to exit this claim.");

    @Setting("permission-claim-ignore")
    public TextTemplate permissionClaimIgnore = TextTemplate.of(TextColors.RED, "You do not have permission to ignore ", TextTemplate.arg("type"), " claims.");

    @Setting("permission-claim-list")
    public TextTemplate permissionClaimList = TextTemplate.of(TextColors.RED, "You don't have permission to get information about another player's land claims.");

    @Setting("permission-claim-manage")
    public TextTemplate permissionClaimManage = TextTemplate.of(TextColors.RED, "You don't have permission to manage ", TextTemplate.arg("type"), " claims.");

    @Setting("permission-claim-reset-flags")
    public TextTemplate permissionClaimResetFlags = TextTemplate.of(TextColors.RED, "You don't have permission to reset ", TextTemplate.arg("type"), " claims to flag defaults.");

    @Setting("permission-claim-reset-flags-self")
    public TextTemplate permissionClaimResetFlagsSelf = TextTemplate.of(TextColors.RED, "You don't have permission to reset your claim flags to defaults.");

    @Setting("permission-claim-resize")
    public TextTemplate permissionClaimResize = TextTemplate.of(TextColors.RED, "You don't have permission to resize this claim.");

    @Setting("permission-claim-sale")
    public TextTemplate permissionClaimSale = TextTemplate.of(TextColors.RED, "You don't have permission to sell this claim.");

    @Setting("permission-claim-transfer-admin")
    public TextTemplate permissionClaimTransferAdmin = TextTemplate.of(TextColors.RED, "You don't have permission to transfer admin claims.");

    @Setting("permission-clear-all")
    public TextTemplate permissionClearAll = TextTemplate.of(TextColors.RED, "Only the claim owner can clear all permissions.");

    @Setting("permission-clear")
    public TextTemplate permissionClear = TextTemplate.of(TextColors.RED, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.");

    @Setting("permission-cuboid")
    public TextTemplate permissionCuboid = TextTemplate.of(TextColors.RED, "You don't have permission to create/resize basic claims in 3D mode.");

    @Setting("permission-edit-claim")
    public TextTemplate permissionEditClaim = TextTemplate.of(TextColors.RED, "You don't have permission to edit this claim.");

    @Setting("permission-fire-spread")
    public TextTemplate permissionFireSpread = TextTemplate.of(TextColors.RED, "You don't have permission to spread fire in this claim.");

    @Setting("permission-flag-defaults")
    public TextTemplate permissionFlagDefaults = TextTemplate.of(TextColors.RED, "You don't have permission to manage flag defaults.");

    @Setting("permission-flag-overrides")
    public TextTemplate permissionFlagOverrides = TextTemplate.of(TextColors.RED, "You don't have permission to manage flag overrides.");

    @Setting("permission-flag-use")
    public TextTemplate permissionFlagUse = TextTemplate.of(TextColors.RED, "You don't have permission to use this flag.");

    @Setting("permission-flow-liquid")
    public TextTemplate permissionFlowLiquid = TextTemplate.of(TextColors.RED, "You don't have permission to flow liquid in this claim.");

    @Setting("permission-interact-block")
    public TextTemplate permissionInteractBlock = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("owner").color(TextColors.GOLD), "'s", TextColors.RED, " permission to interact with the block ", TextTemplate.arg("block").color(TextColors.LIGHT_PURPLE), ".");

    @Setting("permission-interact-entity")
    public TextTemplate permissionInteractEntity = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("owner").color(TextColors.GOLD), "'s", TextColors.RED, " permission to interact with the entity ", TextTemplate.arg("entity").color(TextColors.LIGHT_PURPLE), ".");

    @Setting("permission-interact-item-block")
    public TextTemplate permissionInteractItemBlock = TextTemplate.of(TextColors.RED, "You don't have permission to use ", TextTemplate.arg("item").color(TextColors.LIGHT_PURPLE), TextColors.RED, " on a ", TextTemplate.arg("block").color(TextColors.AQUA), ".");

    @Setting("permission-interact-item-entity")
    public TextTemplate permissionInteractItemEntity = TextTemplate.of(TextColors.RED, "You don't have permission to use ", TextTemplate.arg("item").color(TextColors.LIGHT_PURPLE), TextColors.RED, " on a ", TextTemplate.arg("entity").color(TextColors.AQUA), ".");

    @Setting("permission-interact-item")
    public TextTemplate permissionInteractItem = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("owner").color(TextColors.GOLD), "'s", TextColors.RED, " permission to interact with the item ", TextTemplate.arg("item").color(TextColors.LIGHT_PURPLE), ".");

    @Setting("permission-inventory-open")
    public TextTemplate permissionInventoryOpen = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("owner").color(TextColors.GOLD), "'s", TextColors.RED, " permission to open ", TextTemplate.arg("block").color(TextColors.LIGHT_PURPLE), ".");

    @Setting("permission-item-drop")
    public TextTemplate permissionItemDrop = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("owner").color(TextColors.GOLD), "'s", TextColors.RED, " permission to drop the item ", TextTemplate.arg("item").color(TextColors.LIGHT_PURPLE), " in this claim.");

    @Setting("permission-item-use")
    public TextTemplate permissionItemUse = TextTemplate.of(TextColors.RED, "You can't use the item ", TextTemplate.arg("item"), " in this claim.");

    @Setting("permission-grant")
    public TextTemplate permissionGrant = TextTemplate.of(TextColors.RED, "You can't grant a permission you don't have yourself.");

    @Setting("permission-global-option")
    public TextTemplate permissionGlobalOption = TextTemplate.of(TextColors.RED, "You don't have permission to manage global options.");

    @Setting("permission-group-option")
    public TextTemplate permissionGroupOption = TextTemplate.of(TextColors.RED, "You don't have permission to assign an option to a group.");

    @Setting("permission-assign-without-having")
    public TextTemplate permissionAssignWithoutHaving = TextTemplate.of(TextColors.RED, "You are not allowed to assign a permission that you do not have.");

    @Setting("permission-player-option")
    public TextTemplate permissionPlayerOption = TextTemplate.of(TextColors.RED, "You don't have permission to assign an option to a player.");

    @Setting("permission-player-admin-flags")
    public TextTemplate permissionSetAdminFlags = TextTemplate.of(TextColors.RED, "You don't have permission to change flags on an admin player.");

    @Setting("permission-player-not-ignorable")
    public TextTemplate permissionPlayerNotIgnorable = TextTemplate.of(TextColors.RED, "You can't ignore that player.");

    @Setting("permission-portal-enter")
    public TextTemplate permissionPortalEnter = TextTemplate.of(TextColors.RED, "You can't use this portal because you don't have ", TextTemplate.arg("owner"), "'s permission to enter the destination land claim.");

    @Setting("permission-portal-exit")
    public TextTemplate permissionPortalExit = TextTemplate.of(TextColors.RED, "You can't use this portal because you don't have ", TextTemplate.arg("owner"), "'s permission to build an exit portal in the destination land claim.");

    @Setting("permission-protected-portal")
    public TextTemplate permissionProtectedPortal = TextTemplate.of(TextColors.RED, "You don't have permission to use portals in this claim owned by ", TextTemplate.arg("owner"), ".");

    @Setting("permission-trust")
    public TextTemplate permissionTrust = TextTemplate.of(TextColors.RED, "You don't have ", TextTemplate.arg("owner"), "'s permission to manage permissions here.");

    @Setting("permission-visual-claims-nearby")
    public TextTemplate permissionVisualClaimsNearby = TextTemplate.of(TextColors.RED, "You don't have permission to visualize nearby claims.");

    @Setting("player-drop-unlock-confirmation")
    public TextTemplate playerDropUnlockConfirmation = TextTemplate.of("Unlocked your drops.  Other players may now pick them up (until you die again).");

    @Setting("player-ignore-confirmation")
    public TextTemplate playerIgnoreConfirmation = TextTemplate.of("You're now ignoring chat messages from that player.");

    @Setting("player-ignore-none")
    public TextTemplate playerIgnoreNone = TextTemplate.of(TextColors.YELLOW, "You're not ignoring anyone.");

    @Setting("player-no-chat-until-move")
    public TextTemplate playerNoChatUntilMove = TextTemplate.of("Sorry, but you have to move a little more before you can chat.  We get lots of spam bots here.  :)");

    @Setting("player-no-profanity")
    public TextTemplate playerNoProfanity = TextTemplate.of("Please moderate your language.");

    @Setting("player-not-ignoring")
    public TextTemplate playerNotIgnoring = TextTemplate.of("You're not ignoring that player.");

    @Setting("player-remaining-blocks-2d")
    public TextTemplate playerRemainingBlocks2d = TextTemplate.of(TextColors.YELLOW, "You may claim up to ", TextTemplate.arg("remaining-blocks").color(TextColors.GREEN), " more blocks.");

    @Setting("player-remaining-blocks-3d")
    public TextTemplate playerRemainingBlocks3d = TextTemplate.of(TextColors.YELLOW, "You may claim up to ", TextTemplate.arg("remaining-chunks").color(TextColors.GREEN), " more chunks. (", TextTemplate.arg("remaining-blocks").color(TextColors.WHITE), " blocks)");

    @Setting("player-separate")
    public TextTemplate playerSeparate = TextTemplate.of("Those players will now ignore each other in chat.");

    @Setting("player-soft-muted")
    public TextTemplate playerSoftMuted = TextTemplate.of(TextColors.GREEN, "Soft-muted ", TextTemplate.arg("player"), ".");

    @Setting("player-unignore-confirmation")
    public TextTemplate playerUnignoreConfirmation = TextTemplate.of("You're no longer ignoring chat messages from that player.");

    @Setting("player-unseparate")
    public TextTemplate playerUnseparate = TextTemplate.of("Those players will no longer ignore each other in chat.");

    @Setting("player-unsoft-muted")
    public TextTemplate playerUnsoftMuted = TextTemplate.of("Unsoft-muted ", TextTemplate.arg("player"), ".");

    @Setting("plugin-reload")
    public TextTemplate pluginReload = TextTemplate.of("GriefPrevention has been reloaded.");

    @Setting("pvp-command-banned")
    public TextTemplate pvpCommandBanned = TextTemplate.of(TextColors.RED, "The command ", TextTemplate.arg("command"), " is banned in PvP.");

    @Setting("pvp-fight-immune")
    public TextTemplate pvpFightImmune = TextTemplate.of(TextColors.RED, "You can't fight someone while you're protected from PvP.");

    @Setting("pvp-defenseless")
    public TextTemplate pvpDefenseless = TextTemplate.of(TextColors.RED, "You can't injure defenseless players.");

    @Setting("pvp-immunity-end")
    public TextTemplate pvpImmunityEnd = TextTemplate.of("Now you can fight with other players.");

    @Setting("pvp-immunity-start")
    public TextTemplate pvpImmunityStart = TextTemplate.of("You're protected from attack by other players as long as your inventory is empty.");

    @Setting("pvp-no-build")
    public TextTemplate pvpNoBuild = TextTemplate.of(TextColors.RED, "You can't build in claims during PvP combat.");

    @Setting("pvp-no-claim")
    public TextTemplate pvpNoClaim = TextTemplate.of(TextColors.RED, "You can't claim lands during PvP combat.");

    @Setting("pvp-no-containers")
    public TextTemplate pvpNoContainers = TextTemplate.of(TextColors.RED, "You can't access containers during PvP combat.");

    @Setting("pvp-no-item-drop")
    public TextTemplate pvpNoItemDrop = TextTemplate.of(TextColors.RED, "You can't drop items while in PvP combat.");

    @Setting("pvp-player-safe-zone")
    public TextTemplate pvpPlayerSafeZone = TextTemplate.of(TextColors.RED, "That player is in a PvP safe zone.");

    @Setting("restore-nature-activate")
    public TextTemplate restoreNatureActivate = TextTemplate.of(TextColors.GREEN, "Ready to restore some nature! Right click to restore nature, and use /BasicClaims to stop.");

    @Setting("restore-nature-aggressive-activate")
    public TextTemplate restoreNatureAggressiveActivate = TextTemplate.of(TextColors.GREEN, "Aggressive mode activated. Do NOT use this underneath anything you want to keep! Right click to aggressively restore nature, and use /BasicClaims to stop.");

    @Setting("restore-nature-fillmode-active")
    public TextTemplate restoreNatureFillModeActive = TextTemplate.of(TextColors.GREEN, "Fill mode activated with radius ", TextTemplate.arg("radius"), ". Right click an area to fill.");

    @Setting(value = "smart-ban-message")
    public TextTemplate smartBanMessage = TextTemplate.of("Attempted to bypass an existing ban.");

    @Setting(value = "spam-ban-message")
    public TextTemplate spamBanMessage = TextTemplate.of("Banned for spam.");

    @Setting(value = "spam-kick-message")
    public TextTemplate spamKickMessage = TextTemplate.of("Kicked for spam.");

    @Setting("tax-claim-expired")
    public TextTemplate taxClaimExpired = TextTemplate.of(TextColors.RED, "This claim has been frozen due to unpaid taxes. The current amount owed is '", TextTemplate.arg("tax_owed").color(TextColors.GOLD), "'." 
            + "\nThere are '", TextTemplate.arg("remaining_days").color(TextColors.GREEN), "' days left to deposit payment to claim bank in order to unfreeze this claim.\nFailure to pay this debt will result in deletion of claim.\nNote: To deposit funds to claimbank, use /claimbank deposit <amount>.");

    @Setting("tax-claim-paid-balance")
    public TextTemplate taxClaimPaidBalance = TextTemplate.of(TextColors.GREEN, "The tax debt of '", TextTemplate.arg("amount").color(TextColors.GOLD), "' has been paid. Your claim has been unfrozen and is now available for use.");

    @Setting("tax-claim-paid-partial")
    public TextTemplate taxClaimPaidPartial = TextTemplate.of(TextColors.GREEN, "The tax debt of '", TextTemplate.arg("amount").color(TextColors.GOLD), "' has been partially paid. In order to unfreeze your claim, the remaining tax owed balance of '", TextTemplate.arg("balance").color(TextColors.GOLD), "' must be paid.");

    @Setting("town-create-not-enough-funds")
    public TextTemplate townCreateNotEnoughFunds = TextTemplate.of(TextColors.RED, "You do not have enough funds to create this town for ", TextTemplate.arg("create_cost").color(TextColors.GOLD), ". You currently have a balance of ", TextTemplate.arg("balance").color(TextColors.GOLD), " and need ", TextTemplate.arg("amount_needed").color(TextColors.GOLD), " more for creation.");

    @Setting("town-chat-disabled")
    public TextTemplate townChatDisabled = TextTemplate.of("Town chat disabled.");

    @Setting("town-chat-enabled")
    public TextTemplate townChatEnabled = TextTemplate.of("Town chat enabled.");

    @Setting("town-name")
    public TextTemplate townName = TextTemplate.of("Set town name to ", TextTemplate.arg("name"), ".");

    @Setting("town-not-found")
    public TextTemplate townNotFound = TextTemplate.of(TextColors.RED, "Town not found.");

    @Setting("town-not-in")
    public TextTemplate townNotIn = TextTemplate.of(TextColors.RED, "You are not in a town.");

    @Setting("town-tag")
    public TextTemplate townTag = TextTemplate.of("Set town tag to ", TextTemplate.arg("tag"), ".");

    @Setting("town-tax-no-claims")
    public TextTemplate townTaxNoClaims = TextTemplate.of(TextColors.RED, "You must own property in this town in order to be taxed.");

    @Setting("town-owner")
    public TextTemplate townTrust = TextTemplate.of(TextColors.RED, "That belongs to the town.");

    @Setting("trust-already-has")
    public TextTemplate trustAlreadyHas = TextTemplate.of(TextColors.RED, TextTemplate.arg("target"), " already has ", TextTemplate.arg("type"), " permission.");

    @Setting("trust-current-claim")
    public TextTemplate trustGrant = TextTemplate.of(TextColors.GREEN, "Granted ", TextTemplate.arg("target").color(TextColors.AQUA), " permission to ", TextTemplate.arg("type"), " in current claim.");

    @Setting("trust-individual-all-claims")
    public TextTemplate trustIndividualAllClaims = TextTemplate.of(TextColors.GREEN, "Granted ", TextTemplate.arg("player").color(TextColors.AQUA), "'s full trust to all your claims.  To unset permissions for ALL your claims, use /untrustall.");

    @Setting("trust-list-header")
    public TextTemplate trustListHeader = TextTemplate.of("Explicit permissions here:");

    @Setting("trust-no-claims")
    public TextTemplate trustNoClaims = TextTemplate.of(TextColors.RED, "You have no claims to trust.");

    @Setting("trust-self")
    public TextTemplate trustSelf = TextTemplate.of(TextColors.RED, "You cannot trust yourself.");

    @Setting("untrust-individual-all-claims")
    public TextTemplate untrustIndividualAllClaims = TextTemplate.of("Revoked ", TextTemplate.arg("target"), "'s access to ALL your claims.  To set permissions for a single claim, stand inside it and use /untrust.");

    @Setting("untrust-individual-single-claim")
    public TextTemplate untrustIndividualSingleClaim = TextTemplate.of("Revoked ", TextTemplate.arg("target"), "'s access to this claim.  To unset permissions for ALL your claims, use /untrustall.");

    @Setting("untrust-owner")
    public TextTemplate untrustOwner = TextTemplate.of(TextTemplate.arg("owner"), " is owner of claim and cannot be untrusted.");

    @Setting("untrust-no-claims")
    public TextTemplate untrustNoClaims = TextTemplate.of(TextColors.RED, "You have no claims to untrust.");

    @Setting("untrust-self")
    public TextTemplate untrustSelf = TextTemplate.of(TextColors.RED, "You cannot untrust yourself.");

    @Setting("url-creative-basics")
    public TextTemplate urlCreativeBasics = TextTemplate.of(TextColors.YELLOW, "Click for Land Claim Help: ", TextColors.GREEN, "http://bit.ly/mcgpcrea");

    @Setting("url-subdivision-basics")
    public TextTemplate urlSubdivisionBasics = TextTemplate.of(TextColors.YELLOW, "Click for Subdivision Help: ", TextColors.GREEN, "http://bit.ly/mcgpsub");

    @Setting("url-survival-basics")
    public TextTemplate urlSurvivalBasics = TextTemplate.of(TextColors.YELLOW, "Click for Land Claim Help: ", TextColors.GREEN, "http://bit.ly/mcgpuser");

    @Setting("warning-pistons-outside-claims")
    public TextTemplate warningPistonsOutsideClaims = TextTemplate.of("Warning: Pistons won't move blocks outside land claims.");

    @Setting("warning-tnt-above-sea-level")
    public TextTemplate warningTntAboveSeaLevel = TextTemplate.of("Warning: TNT will not destroy blocks above sea level.");

    @Setting("warning-chest-unprotected")
    public TextTemplate warningChestUnprotected = TextTemplate.of("This chest is NOT protected. Consider using a golden shovel to expand an existing claim or to create a new one.");

    @Setting("warning-ban-message")
    public TextTemplate warningBanMessage = TextTemplate.of("Please reduce your noise level.  Spammers will be banned.");
}
