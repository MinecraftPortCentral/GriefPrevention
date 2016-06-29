/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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

import static org.spongepowered.api.command.args.GenericArguments.integer;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.player;
import static org.spongepowered.api.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.command.args.GenericArguments.string;

import com.google.common.collect.ImmutableList;
import me.ryanhamshire.griefprevention.GPPermissions;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandGriefPrevention {

    private static CommandContainer commandInstance;

    /**
     * Returns a static instance of the main command. <p> When called for the
     * first time, this method will construct a new CommandContainer. </p>
     *
     * @return A CommandSpec representing the main /gp command
     */
    public static CommandContainer getCommand() {
        if (commandInstance == null) {
            commandInstance = createMainCommand();
        }
        return commandInstance;
    }

    /**
     * Create a new instance of the GriefPrevention command structure.
     *
     * @return The newly created command
     */
    private static CommandContainer createMainCommand() {

        return ParentCommandContainer.builder()
                .aliases("griefprevention", "gp")
                .description(Text.of("GriefPrevention main command"))

                // Help
                .child(CommandSpec.builder()
                        .description(Text.of("What you are looking at right now"))
                        .permission(GPPermissions.COMMAND_HELP)
                        .executor(new CommandHelp())
                        .build(), "help")

                // Claims
                .child(ParentCommandContainer.builder()
                        .aliases("claims", "claim", "c")
                        .description(Text.of("Claims management"))

                        .child(CommandSpec.builder()
                                .description(Text.of("Gets information about a claim"))
                                .permission(GPPermissions.COMMAND_CLAIM_INFO)
                                .executor(new CommandClaimInfo())
                                .build(), "info")

                        .child(CommandSpec.builder()
                                .description(Text.of("Deletes a claim"))
                                .permission(GPPermissions.COMMAND_ABANDON_CLAIM)
                                .executor(new CommandClaimAbandon(false))
                                .build(), "abandon")

                        .child(CommandSpec.builder()
                                .description(Text.of("Deletes ALL your claims"))
                                .permission(GPPermissions.COMMAND_ABANDON_ALL_CLAIMS)
                                .executor(new CommandClaimAbandonAll())
                                .build(), "abandonall")

                        .child(CommandSpec.builder()
                                .description(Text.of("Deletes a claim and all its subdivisions"))
                                .permission(GPPermissions.COMMAND_ABANDON_TOP_LEVEL_CLAIM)
                                .executor(new CommandClaimAbandon(true))
                                .build(), "abandontoplevel")

                        .child(CommandSpec.builder()
                                .description(Text.of("Deletes the claim you're standing in, even if it's not your claim"))
                                .permission(GPPermissions.COMMAND_DELETE_CLAIM)
                                .executor(new CommandClaimDelete())
                                .build(), "delete")

                        .child(CommandSpec.builder()
                                .description(Text.of("Sets the farewell message of your claim"))
                                .permission(GPPermissions.COMMAND_SET_CLAIM_FAREWELL)
                                .arguments(string(Text.of("message")))
                                .executor(new CommandClaimFarewell())
                                .build(), "farewell")

                        .child(CommandSpec.builder()
                                .description(Text.of("Sets the greeting message of your claim"))
                                .permission(GPPermissions.COMMAND_SET_CLAIM_GREETING)
                                .arguments(string(Text.of("message")))
                                .executor(new CommandClaimGreeting())
                                .build(), "greeting")

                        .child(CommandSpec.builder()
                                .description(Text.of("List information about a player's claim blocks and claims"))
                                .permission(GPPermissions.COMMAND_LIST_CLAIMS)
                                .arguments(onlyOne(playerOrSource(Text.of("player"))))
                                .executor(new CommandClaimList())
                                .build(), "list")

                        .child(CommandSpec.builder()
                                .description(Text.of("Sets the name of your claim"))
                                .permission(GPPermissions.COMMAND_SET_CLAIM_NAME)
                                .arguments(string(Text.of("name")))
                                .executor(new CommandClaimName())
                                .build(), "name")

                        .child(CommandSpec.builder()
                                .description(Text.of("Switches the shovel tool to subdivision mode, used to subdivide your claims"))
                                .permission(GPPermissions.COMMAND_SUBDIVIDE_CLAIMS)
                                .executor(new CommandClaimSubdivide())
                                .build(), "subdivideclaims", "subdivide", "sc")

                        .child(CommandSpec.builder()
                                .description(Text.of("Revokes a player's access to your claim(s)"))
                                .permission(GPPermissions.COMMAND_REMOVE_TRUST)
                                .arguments(string(Text.of("type")), string(Text.of("subject")))
                                .executor(new CommandUntrust())
                                .build(), "untrust")

                        .child(CommandSpec.builder()
                                .description(Text.of("Gives a player a manual about claiming land"))
                                .permission(GPPermissions.COMMAND_GIVE_BOOK)
                                .arguments(playerOrSource(Text.of("player")))
                                .executor(new CommandClaimBook())
                                .build(), "book", "guide")

                        .child(CommandSpec.builder()
                                .description(Text.of("Purchases additional claim blocks with server money. Requires an economy plugin"))
                                .permission(GPPermissions.COMMAND_BUY_CLAIM_BLOCKS)
                                .arguments(optional(integer(Text.of("numberOfBlocks"))))
                                .executor(new CommandClaimBuy())
                                .build(), "buyblocks")

                        .child(CommandSpec.builder()
                                .description(Text.of("Sell your claim blocks for server money. Requires an economy plugin"))
                                .permission(GPPermissions.COMMAND_SELL_CLAIM_BLOCKS)
                                .arguments(optional(integer(Text.of("numberOfBlocks"))))
                                .executor(new CommandClaimSell())
                                .build(), "sellblocks")

                        .child(CommandSpec.builder()
                                .description(Text.of("Allows a player to give away a pet they tamed"))
                                .permission(GPPermissions.COMMAND_GIVE_PET).arguments(GenericArguments
                                        .firstParsing(GenericArguments.literal(Text.of("player"), "cancel"), player(Text.of("player"))))
                                .executor(new CommandGivePet())
                                .build(), "givepet")

                        .child(CommandSpec.builder()
                                .description(Text.of("Initiates a siege versus another player"))
                                .arguments(optional(onlyOne(player(Text.of("playerName")))))
                                .permission(GPPermissions.COMMAND_SIEGE)
                                .executor(new CommandSiege())
                                .build(), "siege")

                        .build())

                // Admin
                .child(ParentCommandContainer.builder()
                        .aliases("admin", "a")
                        .description(Text.of("Administrator claim tools"))

                        .child(CommandSpec.builder()
                                .description(Text.of("Switches the shovel tool to administrative claims mode"))
                                .permission(GPPermissions.COMMAND_ADMIN_CLAIMS)
                                .executor(new CommandClaimAdmin())
                                .build(), "adminclaims", "ac")

                        .child(CommandSpec.builder()
                                .description(Text.of("Adds or subtracts bonus claim blocks for a player"))
                                .permission(GPPermissions.COMMAND_ADJUST_CLAIM_BLOCKS)
                                .arguments(string(Text.of("player")), integer(Text.of("amount")))
                                .executor(new CommandAdjustBonusClaimBlocks())
                                .build(), "adjustclaimblocks", "acb")

                        .child(CommandSpec.builder()
                                .description(Text.of("Switches the shovel tool back to basic claims mode"))
                                .permission(GPPermissions.COMMAND_BASIC_CLAIMS)
                                .executor(new CommandClaimBasic())
                                .build(), "basicclaims", "bc")

                        .child(CommandSpec.builder()
                                .description(Text.of("Delete all of another player's claims"))
                                .permission(GPPermissions.COMMAND_DELETE_CLAIMS)
                                .arguments(player(Text.of("player")))
                                .executor(new CommandClaimDeleteAll())
                                .build(), "deleteallclaims", "dac")

                        .child(CommandSpec.builder()
                                .description(Text.of("Deletes all administrative claims"))
                                .permission(GPPermissions.COMMAND_DELETE_ADMIN_CLAIMS)
                                .executor(new CommandClaimDeleteAllAdmin())
                                .build(), "deletealladminclaims")

                        .child(CommandSpec.builder()
                                .description(Text.of("Toggles ignore claims mode"))
                                .permission(GPPermissions.COMMAND_IGNORE_CLAIMS)
                                .executor(new CommandClaimIgnore())
                                .build(), "ignoreclaims", "ic")

                        .child(CommandSpec.builder()
                                .description(Text.of("List all administrative claims"))
                                .permission(GPPermissions.COMMAND_LIST_ADMIN_CLAIMS)
                                .executor(new CommandClaimAdminList())
                                .build(), "listadminclaims", "lac")

                        .child(CommandSpec.builder()
                                .description(Text.of("Switches the shovel tool to restoration mode"))
                                .permission(GPPermissions.COMMAND_RESTORE_NATURE)
                                .executor(new CommandRestoreNature())
                                .build(), "restorenature", "rn")

                        .child(CommandSpec.builder()
                                .description(Text.of("Switches the shovel tool to aggressive restoration mode"))
                                .permission(GPPermissions.COMAND_RESTORE_NATURE_AGGRESSIVE)
                                .executor(new CommandRestoreNatureAggressive())
                                .build(), "restorenatureaggressive", "rna")

                        .child(CommandSpec.builder()
                                .description(Text.of("Switches the shovel tool to fill mode"))
                                .permission(GPPermissions.COMMAND_RESTORE_NATURE_FILL)
                                .arguments(optional(integer(Text.of("radius")), 2))
                                .executor(new CommandRestoreNatureFill())
                                .build(), "restorenaturefill", "rnf")

                        .child(CommandSpec.builder().description(Text.of("Updates a player's accrued claim block total"))
                                .permission(GPPermissions.COMMAND_SET_ACCRUED_CLAIM_BLOCKS).arguments(string(Text.of("player")), integer(Text.of("amount")))
                                .executor(new CommandSetAccruedClaimBlocks()).build(), "setaccruedclaimblocks", "scb")

                        .child(CommandSpec.builder()
                                .description(Text.of("Transfers a claim to a another player"))
                                .arguments(player(Text.of("player")))
                                .permission(GPPermissions.COMMAND_TRANSFER_CLAIM)
                                .executor(new CommandClaimTransfer())
                                .build(), "transferclaim")

                        .build())

                // Items
                .child(ParentCommandContainer.builder()
                        .aliases("items", "item", "i")
                        .description(Text.of("Item bans management"))

                        .child(CommandSpec.builder()
                                .description(Text.of("Unbans the specified item id or item in hand if no id is specified."))
                                .permission(GPPermissions.COMMAND_UNBAN_ITEM)
                                .arguments(optional(string(Text.of("itemid"))))
                                .executor(new CommandClaimUnbanItem())
                                .build(), "unban")

                        .build())

                .child(ParentCommandContainer.builder()
                        .aliases("players", "player", "p")
                        .description(Text.of("Player management"))

                        .child(CommandSpec.builder()
                                .description(Text.of("Ignores another player's chat messages"))
                                .permission(GPPermissions.COMMAND_IGNORE_PLAYER)
                                .arguments(onlyOne(player(Text.of("player"))))
                                .executor(new CommandIgnorePlayer())
                                .build(), "ignore")

                        .child(CommandSpec.builder()
                                .description(Text.of("Unignores another player's chat messages"))
                                .permission(GPPermissions.COMMAND_UNIGNORE_PLAYER)
                                .arguments(onlyOne(player(Text.of("player"))))
                                .executor(new CommandUnignorePlayer())
                                .build(), "unignore")

                        .child(CommandSpec.builder()
                                .description(Text.of("Lists the players you're ignoring in chat"))
                                .permission(GPPermissions.COMMAND_LIST_IGNORED_PLAYERS)
                                .executor(new CommandIgnoredPlayerList())
                                .build(), "list")

                        .child(CommandSpec.builder()
                                .description(Text.of("Forces two players to ignore each other in chat"))
                                .permission(GPPermissions.COMMAND_SEPARATE_PLAYERS)
                                .arguments(onlyOne(player(Text.of("player1"))), onlyOne(player(Text.of("player2"))))
                                .executor(new CommandSeparate())
                                .build(), "separate")

                        .child(CommandSpec.builder()
                                .description(Text.of("Reverses /separate"))
                                .permission(GPPermissions.COMMAND_UNSEPARATE_PLAYERS)
                                .arguments(onlyOne(player(Text.of("player1"))), onlyOne(player(Text.of("player2"))))
                                .executor(new CommandUnseparate())
                                .build(), "unseparate")

                        .child(CommandSpec.builder()
                                .description(Text.of("Toggles whether a player's messages will only reach other soft-muted players"))
                                .permission(GPPermissions.COMMAND_SOFT_MUTE_PLAYER)
                                .arguments(onlyOne(player(Text.of("player"))))
                                .executor(new CommandSoftMute())
                                .build(), "softmute")

                        .build())

                // Flags
                /*.child(ParentCommandContainer.builder()
                        .aliases("flags", "flag", "f")
                        .description(Text.of("Flags management"))

                    .child(CommandSpec.builder()
                            .description(Text.of("Adds flag permission to target."))
                            .permission(GPPermissions.CLAIM_MANAGE_FLAGS)
                            .arguments(GenericArguments.seq(
                                GenericArguments.choices(Text.of("target"), targetChoices, true),
                                GenericArguments.onlyOne(GenericArguments.string(Text.of("name"))),
                                GenericArguments.onlyOne(GenericArguments.string(Text.of("flag"))),
                                GenericArguments.onlyOne(GenericArguments.string(Text.of("value")))))
                            .executor(new CommandAddFlagPermission())
                            .build(), "permission", "perm")
                   .build())*/

                .child(CommandSpec.builder()
                        .description(Text.of("Allows other players to pick up the items you dropped when you died"))
                        .permission(GPPermissions.COMMAND_UNLOCK_DROPS)
                        .executor(new CommandUnlockDrops())
                        .build(), "unlockdrops")

                .child(CommandSpec.builder()
                        .description(Text.of("Turns on debug logging."))
                        .permission(GPPermissions.COMMAND_DEBUG)
                        .executor(new CommandDebug())
                        .build(), "debug")

                .child(CommandSpec.builder()
                        .description(Text.of("Reloads Grief Prevention's configuration settings"))
                        .permission(GPPermissions.COMMAND_RELOAD)
                        .executor(new CommandGpReload())
                        .build(), "reload")

                .build();

        /* All of these commands are related to the trust system or the flags system

        .child(CommandSpec.builder()
            .description(Text.of("Grants a player entry to your claim(s) and use of your bed"))
            .permission(GPPermissions.GIVE_ACCESS_TRUST)
            .arguments(string(Text.of("target")))
            .executor(new CommandAccessTrust())
            .build(), "accesstrust", "at")

        .child(CommandSpec.builder()
            .description(Text.of("Grants a player access to your claim's containers, crops, animals, bed, buttons, and levers"))
            .permission(GPPermissions.GIVE_CONTAINER_TRUST)
            .arguments(string(Text.of("target")))
            .executor(new CommandContainerTrust())
            .build(), "containertrust", "ct")

        .child(CommandSpec.builder()
            .description(Text.of("Grants a player permission to grant their level of permission to others"))
            .permission(GPPermissions.GIVE_PERMISSION_TRUST)
            .arguments(string(Text.of("target")))
            .executor(new CommandPermissionTrust())
            .build(), "permissiontrust", "pt")

        .child(CommandSpec.builder()
            .description(Text.of("Lists permissions for the claim you're standing in"))
            .permission(GPPermissions.LIST_TRUST)
            .executor(new CommandTrustList())
            .build(), "trustlist")

        .child(CommandSpec.builder()
            .description(Text.of("Grants a player full access to your claim(s)"))
            .extendedDescription(Text.of("Grants a player full access to your claim(s).\n"
                + "See also /untrust, /containertrust, /accesstrust, and /permissiontrust."))
            .permission(GPPermissions.GIVE_FULL_TRUST)
            .arguments(string(Text.of("subject")))
            .executor(new CommandTrust())
            .build(), "trust")

        .child(CommandSpec.builder()
            .description(Text.of("Revokes a player's access to your claim(s)"))
            .permission(GPPermissions.REMOVE_TRUST)
            .arguments(string(Text.of("subject")))
            .executor(new CommandUntrust())
            .build(), "untrust")

        HashMap<String, String> targetChoices = new HashMap<>();
        targetChoices.put("player", "player");
        targetChoices.put("group", "group");

        .child(CommandSpec.builder()
            .description(Text.of("Adds flag permission to target."))
            .permission(GPPermissions.CLAIM_MANAGE_FLAGS)
            .arguments(GenericArguments.seq(
                GenericArguments.choices(Text.of("target"), targetChoices, true),
                GenericArguments.onlyOne(GenericArguments.string(Text.of("name"))),
                GenericArguments.onlyOne(GenericArguments.string(Text.of("flag"))),
                GenericArguments.onlyOne(GenericArguments.string(Text.of("value")))))
            .executor(new CommandAddFlagPermission())
            .build(), "addflagpermission")

        .child(CommandSpec.builder()
            .description(Text.of("Adds flag command permission to target."))
            .permission(GPPermissions.CLAIM_MANAGE_FLAGS)
            .arguments(GenericArguments.seq(
                GenericArguments.choices(Text.of("target"), targetChoices, true),
                GenericArguments.onlyOne(GenericArguments.string(Text.of("name"))),
                GenericArguments.onlyOne(GenericArguments.string(Text.of("flag"))),
                GenericArguments.onlyOne(GenericArguments.string(Text.of("value")))))
            .executor(new CommandAddFlagCmdPermission())
            .build(), "addflagcmdpermission")

        .child(CommandSpec.builder()
            .description(Text.of("Gets/Sets various claim flags in the claim you are standing in"))
            .permission(GPPermissions.CLAIM_MANAGE_FLAGS)
            .arguments(GenericArguments.firstParsing(GenericArguments.flags().flag("-r", "r")
                .buildWith(GenericArguments.seq(optional(onlyOne(string(Text.of("flag")))),
                    optional(GenericArguments.firstParsing(onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                        .put("-1", Tristate.FALSE)
                        .put("0", Tristate.UNDEFINED)
                        .put("1", Tristate.TRUE)
                        .put("false", Tristate.FALSE)
                        .put("default", Tristate.UNDEFINED)
                        .put("true", Tristate.TRUE)
                        .build())), onlyOne(GenericArguments.remainingJoinedStrings(Text.of("val")))))))))
            .executor(new CommandClaimFlag(GPPermissions.CLAIM_MANAGE_FLAGS))
            .build(), "claimflag")

         */
    }

    // Create our own command system, because ChildCommandElementExecutor does
    // not expose the child commands
    // It has to be done this way to keep compatibility with /gp help and at the
    // same time not require a shitton of reflection/implementation dependencies

    public static class CommandContainer {

        private final List<String> aliases;
        private final CommandSpec commandSpec;

        private CommandContainer(List<String> aliases, CommandSpec commandSpec) {
            this.aliases = ImmutableList.copyOf(aliases);
            this.commandSpec = Objects.requireNonNull(commandSpec);
        }

        public List<String> getAliases() {
            return aliases;
        }

        public CommandSpec getCommandSpec() {
            return commandSpec;
        }

        public static CommandContainer of(CommandSpec commandSpec, String... aliases) {
            return new CommandContainer(Arrays.asList(aliases), commandSpec);
        }
    }

    public static class ParentCommandContainer extends CommandContainer {

        private final List<CommandContainer> children;

        private ParentCommandContainer(List<String> aliases, Text description, List<CommandContainer> children) {
            super(aliases, CommandSpec.builder()
                    .description(Objects.requireNonNull(description))
                    .children(convertChildrenList(children))
                    .build());
            this.children = ImmutableList.copyOf(children);
        }

        public List<CommandContainer> getChildren() {
            return children;
        }

        public static Builder builder() {
            return new Builder();
        }

        private static Map<List<String>, CommandSpec> convertChildrenList(List<CommandContainer> children) {
            return children.stream()
                    .collect(Collectors.toMap(CommandContainer::getAliases, CommandContainer::getCommandSpec));
        }

        public static class Builder {

            private List<String> aliases;
            private List<CommandContainer> children = new LinkedList<>();
            private Text description;

            private Builder() {
            }

            public Builder aliases(String... aliases) {
                this.aliases = Arrays.asList(aliases);
                return this;
            }

            public ParentCommandContainer build() {
                return new ParentCommandContainer(aliases, description, children);
            }

            public Builder child(CommandContainer child) {
                children.add(child);
                return this;
            }

            public Builder child(CommandSpec commandSpec, String... aliases) {
                return child(CommandContainer.of(commandSpec, aliases));
            }

            public Builder description(Text description) {
                this.description = Objects.requireNonNull(description);
                return this;
            }
        }
    }
}
