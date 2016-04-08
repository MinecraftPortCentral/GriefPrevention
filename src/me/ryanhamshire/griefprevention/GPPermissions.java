/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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

    // GENERAL PERMISSIONS

    // Controls whether a player can use the '/gp claimlist' command on another player
    public static final String CLAIMS_LIST_OTHER = "griefprevention.claimslistother";

    // Controls whether a player can create a claim
    public static final String CREATE_CLAIMS = "griefprevention.createclaims";

    // Controls whether a player can delete a claim 
    public static final String DELETE_CLAIMS = "griefprevention.deleteclaims";
    public static final String EAVES_DROP = "griefprevention.eaves-drop";
    public static final String EAVES_DROP_SIGNS = "griefprevention.eaves-drop-signs";
    public static final String IGNORE_CLAIMS = "griefprevention.ignoreclaims";
    public static final String NO_PVP_IMMUNITY = "griefprevention.no-pvp-immunity";
    public static final String NOT_IGNORABLE = "griefprevention.not-ignorable";
    public static final String OVERRIDE_CLAIM_COUNT_LIMIT = "griefprevention.override-claim-count-limit";
    public static final String SIEGE_IMMUNE = "griefprevention.siege-immune";
    public static final String SPAM = "griefprevention.spam";
    public static final String VISUALIZE_NEARBY_CLAIMS = "griefprevention.visualize-nearby-claims";

    // FLAG PERMISSIONS
    public static final String BLOCK_BREAK = "griefprevention.flag.block-break";
    public static final String BLOCK_COMMANDS = "griefprevention.flag.block-commands";
    public static final String BLOCK_PLACE = "griefprevention.flag.block-place";
    public static final String EXPLOSIONS = "griefprevention.flag.explosions";
    public static final String FIRE_SPREAD = "griefprevention.flag.fire-spread";
    public static final String FLAG_BASE = "griefprevention.flag";
    public static final String FORCE_DENY_ALL = "griefprevention.flag.force-deny-all";
    public static final String LAVA_FLOW = "griefprevention.flag.lava-flow";
    public static final String MOB_BLOCK_DAMAGE = "griefprevention.flag.mob-block-damage";
    public static final String MOB_PLAYER_DAMAGE = "griefprevention.flag.mob-player-damage";
    public static final String MOB_RIDING = "griefprevention.flag.mob-riding";
    public static final String INTERACT_PRIMARY = "griefprevention.flag.interact-primary";
    public static final String INTERACT_SECONDARY = "griefprevention.flag.interact-secondary";
    public static final String ITEM_DROP = "griefprevention.flag.item-drop";
    public static final String ITEM_PICKUP = "griefprevention.flag.item-pickup";
    public static final String ITEM_USE = "griefprevention.flag.item-use";
    public static final String PORTAL_USE = "griefprevention.flag.portal-use";
    public static final String PROJECTILES_ANY = "griefprevention.flag.projectiles-any";
    public static final String PROJECTILES_MONSTER = "griefprevention.flag.projectiles-monster";
    public static final String PROJECTILES_PLAYER = "griefprevention.flag.projectiles-player";
    public static final String PVP = "griefprevention.flag.pvp";
    public static final String SLEEP = "griefprevention.flag.sleep";
    public static final String SPAWN_AMBIENTS = "griefprevention.flag.spawn-ambients";
    public static final String SPAWN_ANY = "griefprevention.flag.spawn-any";
    public static final String SPAWN_AQUATICS = "griefprevention.flag.spawn-aquatics";
    public static final String SPAWN_MONSTERS = "griefprevention.flag.spawn-monsters";
    public static final String SPAWN_PASSIVES = "griefprevention.flag.spawn-passives";
    public static final String WATER_FLOW = "griefprevention.flag.water-flow";

    // COMMAND PERMISSIONS
    public static final String COMMAND_ABANDON = "griefprevention.command.abandon";
    public static final String COMMAND_ABANDONALL = "griefprevention.command.abandonall";
    public static final String COMMAND_ABANDONTOPLEVEL = "griefprevention.command.abandontoplevel";
    public static final String COMMAND_ACCESSTRUST = "griefprevention.command.accesstrust";
    public static final String COMMAND_ADJUSTBONUSCLAIMBLOCKS = "griefprevention.command.adjustclaimblocks";
    public static final String COMMAND_ADMINCLAIMS = "griefprevention.command.adminclaims";
    public static final String COMMAND_ADMINCLAIMSLIST = "griefprevention.command.adminclaimslist";
    public static final String COMMAND_BASICCLAIMS = "griefprevention.command.basicclaims";
    public static final String COMMAND_CLAIM = "griefprevention.command.claim";
    public static final String COMMAND_CLAIMBOOK = "griefprevention.command.claimbook";
    public static final String COMMAND_CLAIMBUY = "griefprevention.command.claimbuy";
    public static final String COMMAND_CLAIMFLAG = "griefprevention.command.claimflag";
    public static final String COMMAND_CLAIMDELETE = "griefprevention.command.claimdelete";
    public static final String COMMAND_CLAIMDELETEALL = "griefprevention.command.claimdeleteall";
    public static final String COMMAND_CLAIMDELETEALLADMIN = "griefprevention.command.claimdeletealladmin";
    public static final String COMMAND_CLAIMLIST = "griefprevention.command.claimlist";
    public static final String COMMAND_CLAIMSELLBLOCKS = "griefprevention.command.claimsellblocks";
    public static final String COMMAND_CONTAINERTRUST = "griefprevention.command.containertrust";
    public static final String COMMAND_DEBUG = "griefprevention.command.debug";
    public static final String COMMAND_GIVEPET = "griefprevention.command.givepet";
    public static final String COMMAND_HELP = "griefprevention.command.help";
    public static final String COMMAND_IGNORECLAIMS = "griefprevention.command.ignoreclaims";
    public static final String COMMAND_IGNOREPLAYER = "griefprevention.command.ignoreplayer";
    public static final String COMMAND_IGNOREDPLAYERLIST = "griefprevention.command.ignoredplayerlist";
    public static final String COMMAND_PERMISSIONTRUST = "griefprevention.command.permissiontrust";
    public static final String COMMAND_RELOAD = "griefprevention.command.reload";
    public static final String COMMAND_RESTORENATURE = "griefprevention.command.restorenature";
    public static final String COMMAND_RESTORENATUREAGGRESSIVE = "griefprevention.command.restorenatureaggressive";
    public static final String COMMAND_RESTORENATUREFILL = "griefprevention.command.restorenaturefill";
    public static final String COMMAND_SEPARATE = "griefprevention.command.separate";
    public static final String COMMAND_SETACCRUEDCLAIMBLOCKS = "griefprevention.command.scb";
    public static final String COMMAND_SIEGE = "griefprevention.command.siege";
    public static final String COMMAND_SOFTMUTE = "griefprevention.command.softmute";
    public static final String COMMAND_SUBDIVIDECLAIMS = "griefprevention.command.subdivideclaims";
    public static final String COMMAND_TRANSFERCLAIM = "griefprevention.command.transferclaim";
    public static final String COMMAND_TRAPPED = "griefprevention.command.trapped";
    public static final String COMMAND_TRUST = "griefprevention.command.trust";
    public static final String COMMAND_TRUSTLIST = "griefprevention.command.trustlist";
    public static final String COMMAND_UNIGNORE = "griefprevention.command.unignore";
    public static final String COMMAND_UNLOCKDROPS = "griefprevention.command.unlockdrops";
    public static final String COMMAND_UNSEPARATE = "griefprevention.command.unseparate";
    public static final String COMMAND_UNTRUST = "griefprevention.command.untrust";
}
