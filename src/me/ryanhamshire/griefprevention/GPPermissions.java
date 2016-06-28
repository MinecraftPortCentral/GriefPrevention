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
package me.ryanhamshire.griefprevention;

public class GPPermissions {

    // Claims
    public static final String COMMAND_ABANDON_CLAIM = "griefprevention.claim.command.abandon";
    public static final String COMMAND_ABANDON_ALL_CLAIMS = "griefprevention.claim.command.abandon-all";
    public static final String COMMAND_ABANDON_TOP_LEVEL_CLAIM = "griefprevention.claim.command.abandon-top-level";
    public static final String COMMAND_LIST_CLAIMS = "griefprevention.claim.command.list";
    public static final String COMMAND_BASIC_CLAIMS = "griefprevention.claim.command.basic-mode";
    public static final String COMMAND_GIVE_BOOK = "griefprevention.claim.command.give-book";
    public static final String COMMAND_GIVE_PET = "griefprevention.claim.command.give-pet";
    public static final String COMMAND_CLAIM_INFO = "griefprevention.claim.command.info";
    public static final String COMMAND_SET_CLAIM_NAME = "griefprevention.claim.command.set-name";
    public static final String COMMAND_SET_CLAIM_FAREWELL = "griefprevention.claim.command.set-farewell";
    public static final String COMMAND_SET_CLAIM_GREETING = "griefprevention.claim.command.set-greeting";
    public static final String COMMAND_SUBDIVIDE_CLAIMS = "griefprevention.claim.command.subdivide";
    public static final String COMMAND_TRANSFER_CLAIM = "griefprevention.claim.command.transfer";
    public static final String COMMAND_BUY_CLAIM_BLOCKS = "griefprevention.claim.command.buy-blocks";
    public static final String COMMAND_SELL_CLAIM_BLOCKS = "griefprevention.claim.command.sell-blocks";
    public static final String COMMAND_LIST_CLAIM_FLAGS = "griefprevention.claim.command.list-flags";
    public static final String COMMAND_CLAIM_BAN_ITEM = "griefprevention.claim.command.ban-item";
    public static final String COMMAND_CLAIM_UNBAN_ITEM = "griefprevention.claim.command.unban-item";
    public static final String COMMAND_SIEGE = "griefprevention.claim.command.siege";
    public static final String CLAIM_CREATE = "griefprevention.claim.create";
    public static final String CLAIM_LIST_OTHERS = "griefprevention.claim.list-other";
    public static final String CLAIM_RESET_FLAGS_BASIC = "griefprevention.claim.reset-flags-basic";
    public static final String CLAIM_RESET_FLAGS_SUBDIVISION = "griefprevention.claim.reset-flags-subdivision";
    public static final String SIEGE_IMMUNE = "griefprevention.claim.siege.immune";
    public static final String VISUALIZE_CLAIMS = "griefprevention.claim.visualize-claims";

    // flags
    public static final String MANAGE_FLAGS = "griefprevention.claim.manage.flags";
    public static final String COMMAND_FLAGS_CLAIM = "griefprevention.claim.command.flags.base";
    public static final String COMMAND_FLAGS_DEBUG = "griefprevention.claim.command.flags.debug";
    public static final String COMMAND_FLAGS_PLAYER = "griefprevention.claim.command.flags.player";
    public static final String COMMAND_FLAGS_GROUP = "griefprevention.claim.command.flags.group";
    public static final String COMMAND_FLAGS_RESET = "griefprevention.claim.command.flags.reset";

    // Admin
    public static final String COMMAND_ADJUST_CLAIM_BLOCKS = "griefprevention.admin.command.adjust-claim-blocks";
    public static final String COMMAND_ADMIN_CLAIMS = "griefprevention.admin.command.admin-claims";
    public static final String COMMAND_DEBUG = "griefprevention.admin.command.debug";
    public static final String COMMAND_DELETE_CLAIM = "griefprevention.admin.command.delete-claim";
    public static final String COMMAND_DELETE_CLAIMS = "griefprevention.admin.command.delete-claims";
    public static final String COMMAND_DELETE_ADMIN_CLAIMS = "griefprevention.admin.command.delete-admin-claims";
    public static final String COMMAND_LIST_ADMIN_CLAIMS = "griefprevention.admin.command.list-admin-claims";
    public static final String COMMAND_IGNORE_CLAIMS = "griefprevention.admin.command.ignore-claims";
    public static final String COMMAND_SET_ACCRUED_CLAIM_BLOCKS = "griefprevention.admin.command.set-accrued-claim-blocks";
    public static final String COMMAND_RESTORE_NATURE = "griefprevention.admin.command.restore-nature";
    public static final String COMAND_RESTORE_NATURE_AGGRESSIVE = "griefprevention.admin.command.restore-nature-aggressive";
    public static final String COMMAND_RESTORE_NATURE_FILL = "griefprevention.admin.command.restore-nature-fill";
    public static final String COMMAND_RELOAD = "griefprevention.admin.command.reload";
    public static final String SET_ADMIN_FLAGS = "griefprevention.admin.claim.set-admin-flags";
    public static final String MANAGE_FLAG_DEFAULTS = "griefprevention.admin.claim.manage-flag-defaults";
    public static final String MANAGE_FLAG_OVERRIDES = "griefprevention.admin.claim.manage-flag-overrides";
    public static final String OVERRIDE_CLAIM_COUNT_LIMIT = "griefprevention.admin.claim.override-limit";
    public static final String EAVES_DROP = "griefprevention.admin.eavesdrop";
    public static final String EAVES_DROP_SIGNS = "griefprevention.admin.eavesdrop.signs";
    public static final String CLAIM_RESET_FLAGS_ADMIN = "griefprevention.admin.claim.reset-admin-flags";
    public static final String CLAIM_RESET_FLAGS_WILDERNESS = "griefprevention.admin.claim.reset-wilderness-flags";

    // Players
    public static final String COMMAND_IGNORE_PLAYER = "griefprevention.command.player.ignore";
    public static final String COMMAND_UNIGNORE_PLAYER = "griefprevention.command.player.unignore";
    public static final String COMMAND_LIST_IGNORED_PLAYERS = "griefprevention.command.player.list";
    public static final String COMMAND_SEPARATE_PLAYERS = "griefprevention.command.player.separate";
    public static final String COMMAND_UNSEPARATE_PLAYERS = "griefprevention.command.player.unseparate";
    public static final String COMMAND_SOFT_MUTE_PLAYER = "griefprevention.command.player.softmute";
    public static final String COMMAND_UNLOCK_DROPS = "griefprevention.command.player.unlock-drops";
    public static final String NO_PVP_IMMUNITY = "griefprevention.player.no-pvp-immunity";
    public static final String NOT_IGNORABLE = "griefprevention.player.not-ignorable";
    public static final String SPAM = "griefprevention.spam-override";

    // Misc
    public static final String COMMAND_HELP = "griefprevention.command.help";

    // Trust
    public static final String COMMAND_GIVE_ACCESS_TRUST = "griefprevention.claim.command.trust.access";
    public static final String COMMAND_GIVE_CONTAINER_TRUST = "griefprevention.claim.command.trust.container";
    public static final String COMMAND_GIVE_BUILDER_TRUST = "griefprevention.claim.command.trust.build";
    public static final String COMMAND_GIVE_PERMISSION_TRUST = "griefprevention.claim.command.trust.permission";
    public static final String COMMAND_LIST_TRUST = "griefprevention.claim.trust.command.list";
    public static final String COMMAND_REMOVE_TRUST = "griefprevention.claim.trust.command.remove";

    // Flags
    public static final String BLOCK_BREAK = "griefprevention.claim.flag.block-break";
    public static final String BLOCK_PLACE = "griefprevention.claim.flag.block-place";
    public static final String COMMAND_EXECUTE = "griefprevention.claim.flag.command-execute";
    public static final String COMMAND_EXECUTE_PVP = "griefprevention.claim.flag.command-execute-pvp";
    public static final String ENTER_CLAIM = "griefprevention.claim.flag.enter-claim";
    public static final String ENTITY_DAMAGE = "griefprevention.claim.flag.entity-damage";
    public static final String ENTITY_EXPLOSION = "griefprevention.claim.flag.explosion";
    public static final String ENTITY_RIDING = "griefprevention.claim.flag.entity-riding";
    public static final String ENTITY_SPAWN = "griefprevention.claim.flag.entity-spawn";
    public static final String ENTITY_TELEPORT_FROM = "griefprevention.claim.flag.entity-teleport-from";
    public static final String ENTITY_TELEPORT_TO = "griefprevention.claim.flag.entity-teleport-to";
    public static final String EXIT_CLAIM = "griefprevention.claim.flag.exit-claim";
    public static final String EXPLOSION = "griefprevention.claim.flag.explosion";
    public static final String EXPLOSION_SURFACE = "griefprevention.claim.flag.explosion-surface";
    public static final String FIRE_SPREAD = "griefprevention.claim.flag.fire-spread";
    public static final String FLAG_BASE = "griefprevention.claim.flag";
    public static final String LIQUID_FLOW = "griefprevention.claim.flag.liquid-flow";
    public static final String INTERACT_BLOCK_PRIMARY = "griefprevention.claim.flag.interact-block-primary";
    public static final String INTERACT_BLOCK_SECONDARY = "griefprevention.claim.flag.interact-block-secondary";
    public static final String INTERACT_ENTITY_PRIMARY = "griefprevention.claim.flag.interact-entity-primary";
    public static final String INTERACT_ENTITY_SECONDARY = "griefprevention.claim.flag.interact-entity-secondary";
    public static final String INTERACT_INVENTORY = "griefprevention.claim.flag.interact-inventory";
    public static final String ITEM_DROP = "griefprevention.claim.flag.item-drop";
    public static final String ITEM_PICKUP = "griefprevention.claim.flag.item-pickup";
    public static final String ITEM_USE = "griefprevention.claim.flag.item-use";
    public static final String PORTAL_USE = "griefprevention.claim.flag.portal-use";
    public static final String PROJECTILE_IMPACT_BLOCK = "griefprevention.claim.flag.projectile-impact-block";
    public static final String PROJECTILE_IMPACT_ENTITY = "griefprevention.claim.flag.projectile-impact-entity";
    public static final String PVP = "griefprevention.claim.flag.pvp";
}
