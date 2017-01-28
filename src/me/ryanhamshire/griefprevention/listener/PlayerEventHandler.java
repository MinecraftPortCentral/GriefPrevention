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
package me.ryanhamshire.griefprevention.listener;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Sets;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.IpBanInfo;
import me.ryanhamshire.griefprevention.ShovelMode;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.task.AutoExtendClaimTask;
import me.ryanhamshire.griefprevention.task.PlayerKickBanTask;
import me.ryanhamshire.griefprevention.task.WelcomeTask;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import me.ryanhamshire.griefprevention.visual.Visualization;
import me.ryanhamshire.griefprevention.visual.VisualizationType;
import net.minecraft.block.BlockDoor;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemFood;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.AchievementData;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.data.manipulator.mutable.entity.VehicleData;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.statistic.achievement.Achievements;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.type.FixedMessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.entity.SpongeEntityType;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.util.VecHelper;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PlayerEventHandler {

    private DataStore dataStore;

    // list of temporarily banned ip's
    private ArrayList<IpBanInfo> tempBannedIps = new ArrayList<IpBanInfo>();

    // number of milliseconds in a day
    private final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

    // timestamps of login and logout notifications in the last minute
    private ArrayList<Long> recentLoginLogoutNotifications = new ArrayList<Long>();

    // regex pattern for the "how do i claim land?" scanner
    private Pattern howToClaimPattern = null;

    // typical constructor, yawn
    public PlayerEventHandler(DataStore dataStore, GriefPreventionPlugin plugin) {
        this.dataStore = dataStore;
    }

    // when a player chats, monitor for spam
    @Listener(order = Order.FIRST)
    public void onPlayerChat(MessageChannelEvent.Chat event, @First Player player) {
        GPTimings.PLAYER_CHAT_EVENT.startTimingIfSync();
        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties());
        if (!activeConfig.getConfig().general.chatProtectionEnabled || !GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
            return;
        }

        if (!player.isOnline()) {
            GriefPreventionPlugin.addEventLogEntry(event, null, player.getLocation(), player, "Player is not online.");
            event.setCancelled(true);
            GPTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
            return;
        }

        String message = event.getRawMessage().toPlain();

        boolean muted = this.handlePlayerChat(player, message, event);
        Iterable<MessageReceiver> recipients = event.getChannel().get().getMembers();
        //Iterable<CommandSource> recipients = event.getSink().getRecipients();

        // muted messages go out to only the sender
        if (muted) {
            event.setChannel(player.getMessageChannel());
        }

        // soft muted messages go out to all soft muted players
        else if (this.dataStore.isSoftMuted(player.getUniqueId())) {
            String notificationMessage = "(Muted " + player.getName() + "): " + message;
            Set<CommandSource> recipientsToKeep = new HashSet<>();
            for (MessageReceiver recipient : recipients) {
                if (recipient instanceof Player && this.dataStore.isSoftMuted(((Player) recipient).getUniqueId())) {
                    recipientsToKeep.add((Player) recipient);
                } else if (recipient instanceof Player && ((Player) recipient).hasPermission(GPPermissions.EAVES_DROP)) {
                    recipient.sendMessage(Text.of(TextColors.GRAY, notificationMessage));
                }
            }
            event.setChannel(new FixedMessageChannel(recipientsToKeep));

            GriefPreventionPlugin.addLogEntry(notificationMessage, CustomLogEntryTypes.Debug, false);
        }

        // troll and excessive profanity filter
        else if (!player.hasPermission(GPPermissions.SPAM) && this.dataStore.bannedWordFinder.hasMatch(message)) {
            // limit recipients to sender
            event.setChannel(player.getMessageChannel());

            // if player not new warn for the first infraction per play session.
            Optional<AchievementData> data = player.get(AchievementData.class);
            if (data.isPresent() && player.getAchievementData().achievements().contains(Achievements.MINE_WOOD)) {
                GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                if (!playerData.profanityWarned) {
                    playerData.profanityWarned = true;
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoProfanity);
                    GriefPreventionPlugin.addEventLogEntry(event, null, player.getLocation(), player, this.dataStore.getMessage(Messages.NoProfanity));
                    event.setCancelled(true);
                    GPTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
                    return;
                }
            }

            // otherwise assume chat troll and mute all chat from this sender
            // until an admin says otherwise
            /*else {
                GriefPrevention
                        .AddLogEntry("Auto-muted new player " + player.getName() + " for profanity shortly after join.  Use /SoftMute to undo.");
                GriefPrevention.instance.dataStore.toggleSoftMute(player.getUniqueId());
            }*/
        }

        // remaining messages
        else {
            // enter in abridged chat logs
            makeSocialLogEntry(player.getName(), message);

            // based on ignore lists, remove some of the audience
            Set<CommandSource> recipientsToRemove = new HashSet<>();
            GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            for (MessageReceiver recipient : recipients) {
                if (recipient instanceof Player) {
                    Player reciever = (Player) recipient;

                    if (playerData.ignoredPlayers.containsKey(reciever.getUniqueId())) {
                        recipientsToRemove.add((Player) recipient);
                    } else {
                        GPPlayerData targetPlayerData = this.dataStore.getOrCreatePlayerData(reciever.getWorld(), reciever.getUniqueId());
                        if (targetPlayerData.ignoredPlayers.containsKey(player.getUniqueId())) {
                            recipientsToRemove.add((Player) recipient);
                        }
                    }
                }
            }

            Set<MessageReceiver> newRecipients = Sets.newHashSet(event.getChannel().get().getMembers().iterator());
            newRecipients.removeAll(recipientsToRemove);

            event.setChannel(new FixedMessageChannel(newRecipients));
        }
        GPTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
    }

    // last chat message shown, regardless of who sent it
    private String lastChatMessage = "";
    private long lastChatMessageTimestamp = 0;

    // number of identical messages in a row
    private int duplicateMessageCount = 0;

    // returns true if the message should be sent, false if it should be muted
    private boolean handlePlayerChat(Player player, String message, Event event) {
        // FEATURE: automatically educate players about claiming land
        // watching for message format how*claim*, and will send a link to the basics video
        if (this.howToClaimPattern == null) {
            this.howToClaimPattern = Pattern.compile(this.dataStore.getMessage(Messages.HowToClaimRegex), Pattern.CASE_INSENSITIVE);
        }

        if (this.howToClaimPattern.matcher(message).matches()) {
            if (GriefPreventionPlugin.instance.claimModeIsActive(player.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.CreativeBasicsVideo2, 10L);
            } else {
                GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.SurvivalBasicsVideo2, 10L);
            }
        }

        // FEATURE: monitor for chat and command spam

        if (!GriefPreventionPlugin.getGlobalConfig().getConfig().spam.monitorEnabled)
            return false;

        // if the player has permission to spam, don't bother even examining the message
        if (player.hasPermission(GPPermissions.SPAM))
            return false;

        boolean spam = false;
        String mutedReason = null;

        // prevent bots from chatting - require movement before talking for any newish players
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.noChatLocation != null) {
            Location<World> currentLocation = player.getLocation();
            if (currentLocation.getBlockX() == playerData.noChatLocation.getBlockX() &&
                    currentLocation.getBlockZ() == playerData.noChatLocation.getBlockZ()) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoChatUntilMove, 10L);
                spam = true;
                mutedReason = "pre-movement chat";
            } else {
                playerData.noChatLocation = null;
            }
        }

        // remedy any CAPS SPAM, exception for very short messages which could be emoticons like =D or XD
        if (message.length() > 4 && this.stringsAreSimilar(message.toUpperCase(), message)) {
            // exception for strings containing forward slash to avoid changing
            // a case-sensitive URL
            if (event instanceof MessageEvent) {
                ((MessageEvent) event).setMessage(Text.of(message.toLowerCase()));
            }
        }

        // always mute an exact match to the last chat message
        long now = new Date().getTime();
        if (mutedReason != null && message.equals(this.lastChatMessage) && now - this.lastChatMessageTimestamp < 750) {
            playerData.spamCount += ++this.duplicateMessageCount;
            spam = true;
            mutedReason = "repeat message";
        } else {
            this.lastChatMessage = message;
            this.lastChatMessageTimestamp = now;
            this.duplicateMessageCount = 0;
        }

        // where other types of spam are concerned, casing isn't significant
        message = message.toLowerCase();

        // check message content and timing
        long millisecondsSinceLastMessage = now - playerData.lastMessageTimestamp.getTime();

        // if the message came too close to the last one
        if (millisecondsSinceLastMessage < 1500) {
            // increment the spam counter
            playerData.spamCount++;
            spam = true;
        }

        // if it's very similar to the last message from the same player and
        // within 10 seconds of that message
        if (mutedReason == null && this.stringsAreSimilar(message, playerData.lastMessage)
                && now - playerData.lastMessageTimestamp.getTime() < 10000) {
            playerData.spamCount++;
            spam = true;
            mutedReason = "similar message";
        }

        // filter IP addresses
        if (mutedReason == null) {
            if (GriefPreventionPlugin.instance.containsBlockedIP(message)) {
                // spam notation
                playerData.spamCount += 1;
                spam = true;

                // block message
                mutedReason = "IP address";
            }
        }

        // if the message was mostly non-alpha-numerics or doesn't include much
        // whitespace, consider it a spam (probably ansi art or random text gibberish)
        if (mutedReason == null && message.length() > 5) {
            int symbolsCount = 0;
            int whitespaceCount = 0;
            for (int i = 0; i < message.length(); i++) {
                char character = message.charAt(i);
                if (!(Character.isLetterOrDigit(character))) {
                    symbolsCount++;
                }

                if (Character.isWhitespace(character)) {
                    whitespaceCount++;
                }
            }

            if (symbolsCount > message.length() / 2 || (message.length() > 15 && whitespaceCount < message.length() / 10)) {
                spam = true;
                if (playerData.spamCount > 0)
                    mutedReason = "gibberish";
                playerData.spamCount++;
            }
        }

        // very short messages close together are spam
        if (mutedReason == null && message.length() < 5 && millisecondsSinceLastMessage < 3000) {
            spam = true;
            playerData.spamCount++;
        }

        // in any case, record the timestamp of this message and also its
        // content for next time
        playerData.lastMessageTimestamp = new Date();
        playerData.lastMessage = message;

        // if the message was determined to be a spam, consider taking action
        if (spam) {
            // anything above level 8 for a player which has received a
            // warning... kick or if enabled, ban
            if (playerData.spamCount > 8 && playerData.spamWarned) {
                if (GriefPreventionPlugin.getGlobalConfig().getConfig().spam.autoBanOffenders) {
                    // log entry
                    GriefPreventionPlugin.addLogEntry("Banning " + player.getName() + " for spam.", CustomLogEntryTypes.AdminActivity);

                    // kick and ban
                    PlayerKickBanTask task =
                            new PlayerKickBanTask(player, GriefPreventionPlugin.getGlobalConfig().getConfig().spam.banMessage, "GriefPrevention Anti-Spam", true);
                    Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1).execute(task).submit(GriefPreventionPlugin.instance);
                } else {
                    // log entry
                    GriefPreventionPlugin.addLogEntry("Kicking " + player.getName() + " for spam.", CustomLogEntryTypes.AdminActivity);

                    // just kick
                    PlayerKickBanTask task = new PlayerKickBanTask(player, "", "GriefPrevention Anti-Spam", false);
                    Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1).execute(task).submit(GriefPreventionPlugin.instance);
                }

                return true;
            }

            // cancel any messages while at or above the third spam level and issue warnings anything above level 2, mute and warn
            if (playerData.spamCount >= 4) {
                if (mutedReason == null) {
                    mutedReason = "too-frequent text";
                }
                if (!playerData.spamWarned) {
                    GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Warn, GriefPreventionPlugin.getGlobalConfig().getConfig().spam.banWarningMessage), 10L);
                    GriefPreventionPlugin.addLogEntry("Warned " + player.getName() + " about spam penalties.", CustomLogEntryTypes.Debug, false);
                    playerData.spamWarned = true;
                }
            }

            if (mutedReason != null) {
                // make a log entry
                GriefPreventionPlugin.addLogEntry("Muted " + mutedReason + ".");
                GriefPreventionPlugin.addLogEntry("Muted " + player.getName() + " " + mutedReason + ":" + message, CustomLogEntryTypes.Debug, false);

                // cancelling the event guarantees other players don't receive the message
                return true;
            }
        }

        // otherwise if not a spam, reset the spam counter for this player
        else {
            playerData.spamCount = 0;
            playerData.spamWarned = false;
        }

        return false;
    }

    // if two strings are 75% identical, they're too close to follow each other in the chat
    private boolean stringsAreSimilar(String message, String lastMessage) {
        // determine which is shorter
        String shorterString, longerString;
        if (lastMessage.length() < message.length()) {
            shorterString = lastMessage;
            longerString = message;
        } else {
            shorterString = message;
            longerString = lastMessage;
        }

        if (shorterString.length() <= 5)
            return shorterString.equals(longerString);

        // set similarity tolerance
        int maxIdenticalCharacters = longerString.length() - longerString.length() / 4;

        // trivial check on length
        if (shorterString.length() < maxIdenticalCharacters)
            return false;

        // compare forward
        int identicalCount = 0;
        int i;
        for (i = 0; i < shorterString.length(); i++) {
            if (shorterString.charAt(i) == longerString.charAt(i))
                identicalCount++;
            if (identicalCount > maxIdenticalCharacters)
                return true;
        }

        // compare backward
        int j;
        for (j = 0; j < shorterString.length() - i; j++) {
            if (shorterString.charAt(shorterString.length() - j - 1) == longerString.charAt(longerString.length() - j - 1))
                identicalCount++;
            if (identicalCount > maxIdenticalCharacters)
                return true;
        }

        return false;
    }


    // when a player uses a slash command...
    @Listener(order = Order.FIRST)
    public void onPlayerCommand(SendCommandEvent event, @First Player player) {
        GPTimings.PLAYER_COMMAND_EVENT.startTimingIfSync();
        String command = event.getCommand();
        String[] args = event.getArguments().split(" ");
        String[] parts = command.split(":");
        String pluginId = null;

        if (parts.length > 1) {
            pluginId = parts[0];
            command = parts[1];
        }

        String message = "/" + event.getCommand() + " " + event.getArguments();
        if (pluginId == null || !pluginId.equals("minecraft")) {
            CommandMapping commandMapping = Sponge.getCommandManager().get(command).orElse(null);
            PluginContainer pluginContainer = null;
            if (commandMapping != null) {
                pluginContainer = Sponge.getCommandManager().getOwner(commandMapping).orElse(null);
                if (pluginContainer != null) {
                    pluginId = pluginContainer.getId();
                }
            }
            if (pluginId == null) {
                pluginId = "minecraft";
            }
        }

        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        }

        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        // if requires access trust, check for permission
        Location<World> location = player.getLocation();
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
        String commandPermission = pluginId + "." + command;

        // first check the args
        String argument = "";
        for (String arg : args) {
            argument = argument + "." + arg;
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.COMMAND_EXECUTE, null, commandPermission + argument, player) == Tristate.FALSE) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.BlockedCommand, "'" + message + "'", claim.getOwnerName());
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.COMMAND_EXECUTE, null, commandPermission + argument, player, "Blocked command.");
                event.setCancelled(true);
                GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                return;
            } else if (playerData != null && (playerData.inPvpCombat(player.getWorld()) || playerData.siegeData != null) && GPPermissionHandler.getClaimPermission(claim, GPPermissions.COMMAND_EXECUTE_PVP, null, commandPermission + argument, player) == Tristate.FALSE) {
                GriefPreventionPlugin.sendMessage(event.getCause().first(Player.class).get(), TextMode.Err, Messages.CommandBannedInPvP);
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.COMMAND_EXECUTE_PVP, null, commandPermission + argument, player, "Blocked pvp command '" + command + "'.");
                event.setCancelled(true);
                GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                return;
            }
        }
        // second check the full command
        if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.COMMAND_EXECUTE, null, commandPermission, player) == Tristate.FALSE) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.BlockedCommand, "'" + message + "'", claim.getOwnerName());
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.COMMAND_EXECUTE, null, commandPermission, player, "Blocked command '" + command + "'.");
            event.setCancelled(true);
            GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        } else if (playerData != null && (playerData.inPvpCombat(player.getWorld()) || playerData.siegeData != null) && GPPermissionHandler.getClaimPermission(claim, GPPermissions.COMMAND_EXECUTE_PVP, null, commandPermission, player) == Tristate.FALSE) {
            GriefPreventionPlugin.sendMessage(event.getCause().first(Player.class).get(), TextMode.Err, Messages.CommandBannedInPvP);
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.COMMAND_EXECUTE_PVP, null, commandPermission, player, "Blocked pvp command '" + command + "'.");
            event.setCancelled(true);
            GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        }

        // if a whisper
        if (GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().general.whisperCommandList.contains(command) && args.length > 1) {
            // determine target player, might be NULL
            Player targetPlayer = Sponge.getGame().getServer().getPlayer(args[1]).orElse(null);

            // if eavesdrop enabled and sender doesn't have the eavesdrop permission, eavesdrop
            if (GriefPreventionPlugin.getActiveConfig(targetPlayer.getWorld().getProperties()).getConfig().general.broadcastWhisperedMessagesToAdmins &&
                    !player.hasPermission(GPPermissions.EAVES_DROP)) {
                // except for when the recipient has eavesdrop permission
                if (targetPlayer == null || !targetPlayer.hasPermission(GPPermissions.EAVES_DROP)) {
                    StringBuilder logMessageBuilder = new StringBuilder();
                    logMessageBuilder.append("[[").append(player.getName()).append("]] ");

                    for (int i = 1; i < args.length; i++) {
                        logMessageBuilder.append(args[i]).append(" ");
                    }

                    String logMessage = logMessageBuilder.toString();

                    Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
                    for (Player onlinePlayer : players) {
                        if (onlinePlayer.hasPermission(GPPermissions.EAVES_DROP) && !onlinePlayer.equals(targetPlayer)) {
                            onlinePlayer.sendMessage(Text.of(TextColors.GRAY + logMessage));
                        }
                    }
                }
            }

            // ignore feature
            if (targetPlayer != null && targetPlayer.isOnline()) {
                // if either is ignoring the other, cancel this command
                playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                if (playerData.ignoredPlayers.containsKey(targetPlayer.getUniqueId())) {
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, targetPlayer, "Player is ignored.");
                    event.setCancelled(true);
                    GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                    return;
                }

                GPPlayerData targetPlayerData = this.dataStore.getOrCreatePlayerData(targetPlayer.getWorld(), targetPlayer.getUniqueId());
                if (targetPlayerData.ignoredPlayers.containsKey(player.getUniqueId())) {
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, targetPlayer, "Player is ignored.");
                    event.setCancelled(true);
                    GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        // if the slash command used is in the list of monitored commands, treat
        // it like a chat message (see above)
        boolean isMonitoredCommand = false;
        for (String monitoredCommand : GriefPreventionPlugin.getGlobalConfig().getConfig().spam.monitoredCommandList) {
            if (args[0].equalsIgnoreCase(monitoredCommand)) {
                isMonitoredCommand = true;
                break;
            }
        }

        if (isMonitoredCommand) {
            // if anti spam enabled, check for spam
            if (GriefPreventionPlugin.getGlobalConfig().getConfig().spam.monitorEnabled) {
                event.setCancelled(this.handlePlayerChat(player, message, event));
            }

            // unless cancelled, log in abridged logs
            if (!event.isCancelled()) {
                StringBuilder builder = new StringBuilder();
                for (String arg : args) {
                    builder.append(arg + " ");
                }

                makeSocialLogEntry(player.getName(), builder.toString());
            }
        }

        isMonitoredCommand = false;
        String lowerCaseMessage = message.toLowerCase();

        for (String monitoredCommand : GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.accessTrustCommands) {
            if (lowerCaseMessage.startsWith(monitoredCommand)) {
                isMonitoredCommand = true;
                break;
            }
        }

        if (isMonitoredCommand) {
            if (claim != null) {
                playerData.lastClaim = new WeakReference<>(claim);
                String reason = claim.allowAccess(player);
                if (reason != null) {
                    GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, reason));
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, player, "Monitored command.");
                    event.setCancelled(true);
                }
            }
        }
        GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
    }

    static int longestNameLength = 10;

    static void makeSocialLogEntry(String name, String message) {
        StringBuilder entryBuilder = new StringBuilder(name);
        for (int i = name.length(); i < longestNameLength; i++) {
            entryBuilder.append(' ');
        }
        entryBuilder.append(": " + message);

        longestNameLength = Math.max(longestNameLength, name.length());

        GriefPreventionPlugin.addLogEntry(entryBuilder.toString(), CustomLogEntryTypes.SocialActivity, false);
    }

    private ConcurrentHashMap<UUID, Date> lastLoginThisServerSessionMap = new ConcurrentHashMap<UUID, Date>();

    // counts how many players are using each IP address connected to the server right now
    @SuppressWarnings("unused")
    private ConcurrentHashMap<String, Integer> ipCountHash = new ConcurrentHashMap<String, Integer>();

    // when a player attempts to join the server...
    @Listener(order = Order.FIRST)
    public void onPlayerLogin(ClientConnectionEvent.Login event) {
        GPTimings.PLAYER_LOGIN_EVENT.startTimingIfSync();
        User player = event.getTargetUser();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getToTransform().getExtent().getProperties())) {
            GPTimings.PLAYER_LOGIN_EVENT.stopTimingIfSync();
            return;
        }

        // all this is anti-spam code
        if (GriefPreventionPlugin.getGlobalConfig().getConfig().spam.monitorEnabled) {
            // FEATURE: login cooldown to prevent login/logout spam with custom clients
            long now = Calendar.getInstance().getTimeInMillis();

            // if allowed to join and login cooldown enabled
            if (GriefPreventionPlugin.getGlobalConfig().getConfig().spam.loginCooldown > 0 && !player.hasPermission(GPPermissions.SPAM)) {
                // determine how long since last login and cooldown remaining
                Date lastLoginThisSession = lastLoginThisServerSessionMap.get(player.getUniqueId());
                if (lastLoginThisSession != null) {
                    long millisecondsSinceLastLogin = now - lastLoginThisSession.getTime();
                    long secondsSinceLastLogin = millisecondsSinceLastLogin / 1000;
                    long cooldownRemaining = GriefPreventionPlugin.getGlobalConfig().getConfig().spam.loginCooldown - secondsSinceLastLogin;

                    // if cooldown remaining
                    if (cooldownRemaining > 0) {
                        // DAS BOOT!;
                        event.setMessage(Text.of("You must wait " + cooldownRemaining + " seconds before logging-in again."));
                        GriefPreventionPlugin.addEventLogEntry(event, null, null, player, "Login spam protection.");
                        event.setCancelled(true);
                        GPTimings.PLAYER_LOGIN_EVENT.stopTimingIfSync();
                        return;
                    }
                }
            }

            // if logging-in account is banned, remember IP address for later
            /*if (GriefPrevention.instance.config_smartBan && event.getResult() == Result.KICK_BANNED) {
                this.tempBannedIps.add(new IpBanInfo(event.getAddress(), now + this.MILLISECONDS_IN_DAY, player.getName()));
            }*/
        }

        // remember the player's ip address
        WorldProperties worldProperties = event.getToTransform().getExtent().getProperties();
        UUID playerUniqueId = player.getUniqueId();
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(worldProperties, playerUniqueId);
        playerData.receivedDropUnlockAdvertisement = false;
        playerData.ipAddress = event.getConnection().getAddress().getAddress();
        GPClaimManager claimWorldManager = this.dataStore.getClaimWorldManager(worldProperties);
        Instant dateNow = Instant.now();
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.getType() != ClaimType.ADMIN && claim.getOwnerUniqueId().equals(playerUniqueId)) {
                // update lastActive timestamp for claim
                claim.getData().setDateLastActive(dateNow);
                // update timestamps for subdivisions
                for (Claim subdivision : ((GPClaim) claim).children) {
                    subdivision.getData().setDateLastActive(dateNow);
                }
                ((GPClaim) claim).getInternalClaimData().setRequiresSave(true);
                claimWorldManager.addClaim(claim, GriefPreventionPlugin.pluginCause);
            }
        }
        GPTimings.PLAYER_LOGIN_EVENT.stopTimingIfSync();
    }

    // when a player successfully joins the server...
    @Listener(order = Order.FIRST)
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        GPTimings.PLAYER_JOIN_EVENT.startTimingIfSync();
        Player player = event.getTargetEntity();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_JOIN_EVENT.stopTimingIfSync();
            return;
        }

        UUID playerID = player.getUniqueId();

        // note login time
        Date nowDate = new Date();
        long now = nowDate.getTime();
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), playerID);
        playerData.lastSpawn = now;

        // if newish, prevent chat until he's moved a bit to prove he's not a bot
        //if (player.getOrCreate(AchievementData.class).isPresent() && !player.getAchievementData().achievements().contains(Achievements.MINE_WOOD)) {
        //    playerData.noChatLocation = player.getLocation();
        //}

        boolean hasJoinedBefore = true;
        if (player.getOrCreate(JoinData.class).isPresent()) {
            hasJoinedBefore = !player.getJoinData().firstPlayed().get().equals(player.getJoinData().lastPlayed().get());
        }

        // if player has never played on the server before...
        if (!hasJoinedBefore) {
            // may need pvp protection
            GriefPreventionPlugin.instance.checkPvpProtectionNeeded(player);

            // if in survival claims mode, send a message about the claim basics
            // video (except for admins - assumed experts)
            if (GriefPreventionPlugin.instance.claimModeIsActive(player.getWorld().getProperties(), ClaimsMode.Survival)
                    && !player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && this.dataStore.getClaimWorldManager(player.getWorld().getProperties()).getWorldClaims().size() > 10) {
                WelcomeTask task = new WelcomeTask(player);
                // 10 seconds after join
                Sponge.getGame().getScheduler().createTaskBuilder().delay(10, TimeUnit.SECONDS).execute(task).submit(GriefPreventionPlugin.instance);
            }
        }

        // silence notifications when they're coming too fast
        if (!event.getMessage().equals(Text.of()) && this.shouldSilenceNotification()) {
            event.setMessage(Text.of());
        }

        // FEATURE: auto-ban accounts who use an IP address which was very
        // recently used by another banned account
        if (GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().general.smartBan && !hasJoinedBefore) {
            // search temporarily banned IP addresses for this one
            for (int i = 0; i < this.tempBannedIps.size(); i++) {
                IpBanInfo info = this.tempBannedIps.get(i);
                String address = info.address.toString();

                // eliminate any expired entries
                if (now > info.expirationTimestamp) {
                    this.tempBannedIps.remove(i--);
                }

                // if we find a match
                else if (address.equals(playerData.ipAddress.toString())) {/*
                    UserStorage storage = event.getGame().getServiceManager().provideUnchecked(UserStorage.class);
                    // if the account associated with the IP ban has been
                    // pardoned, remove all ip bans for that ip and we're done
                    User bannedPlayer = storage.get(info.bannedAccountName).get();
                    /*if (!bannedPlayer.isBanned()) {
                        for (int j = 0; j < this.tempBannedIps.size(); j++) {
                            IpBanInfo info2 = this.tempBannedIps.get(j);
                            if (info2.address.toString().equals(address)) {
                                User bannedAccount = storage.get(info2.bannedAccountName).get();
                                bannedAccount.setBanned(false);
                                this.tempBannedIps.remove(j--);
                            }
                        }

                        break;
                    */}

                    // otherwise if that account is still banned, ban this
                    // account, too
                    else {
                        GriefPreventionPlugin.addLogEntry("Auto-banned " + player.getName()
                                + " because that account is using an IP address very recently used by banned player " + info.bannedAccountName + " ("
                                + info.address.toString() + ").", CustomLogEntryTypes.AdminActivity);

                        // notify any online ops
                        /*Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
                        for (Player otherPlayer : players) {
                            if (otherPlayer.isOp()) {
                                GriefPrevention.sendMessage(otherPlayer, TextMode.Success, Messages.AutoBanNotify, player.getName(),
                                        info.bannedAccountName);
                            }
                        }*/

                        // ban player
                        PlayerKickBanTask task =
                                new PlayerKickBanTask(player, "", "GriefPrevention Smart Ban - Shared Login:" + info.bannedAccountName, true);
                        Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(10).execute(task).submit(GriefPreventionPlugin.instance);

                        // silence join message
                        event.setMessage(Text.of());

                        break;
                }
            }
        }

        GPTimings.PLAYER_JOIN_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        // clear active visuals
        Player player = event.getTargetEntity();
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        playerData.visualBlocks = null;
        if (playerData.visualRevertTask != null) {
            playerData.visualRevertTask.cancel();
        }
    }

    // when a player spawns, conditionally apply temporary pvp protection
    @Listener(order = Order.LAST)
    public void onPlayerRespawn(RespawnPlayerEvent event) {
        GPTimings.PLAYER_RESPAWN_EVENT.startTimingIfSync();
        Player player = event.getTargetEntity();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_RESPAWN_EVENT.stopTimingIfSync();
            return;
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
        playerData.lastPvpTimestamp = 0; // no longer in pvp combat

        // also send him any messaged from grief prevention he would have
        // received while dead
        if (playerData.messageOnRespawn != null) {
            // color is already embedded inmessage in this case
            GriefPreventionPlugin.sendMessage(player, Text.of(playerData.messageOnRespawn), 40L);
            playerData.messageOnRespawn = null;
        }

        GriefPreventionPlugin.instance.checkPvpProtectionNeeded(player);
        GPTimings.PLAYER_RESPAWN_EVENT.stopTimingIfSync();
    }

    // when a player dies...
    @Listener(order = Order.FIRST)
    public void onPlayerDeath(DestructEntityEvent.Death event, @Root DamageSource damageSource) {
        GPTimings.PLAYER_DEATH_EVENT.startTimingIfSync();
        if (!(event.getTargetEntity() instanceof Player) || !GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.PLAYER_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        // FEATURE: prevent death message spam by implementing a "cooldown period" for death messages
        Player player = (Player) event.getTargetEntity();
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), event.getTargetEntity().getUniqueId());
        long now = Calendar.getInstance().getTimeInMillis();

        if (GriefPreventionPlugin.getGlobalConfig().getConfig().spam.monitorEnabled && (now - playerData.lastDeathTimeStamp) < GriefPreventionPlugin.getGlobalConfig().getConfig().spam.deathMessageCooldown * 1000) {
            event.setMessage(Text.of());
        }

        playerData.lastDeathTimeStamp = now;

        // these are related to locking dropped items on death to prevent theft
        World world = event.getTargetEntity().getWorld();
        if (world != null) {
            GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(world.getProperties());
            GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
            if ((claim.pvpRulesApply() && activeConfig.getConfig().pvp.protectItemsOnDeathPvp) ||
                    (!claim.isPvpEnabled() && activeConfig.getConfig().general.protectItemsOnDeathNonPvp)) {
                playerData.dropsAreUnlocked = false;
                playerData.receivedDropUnlockAdvertisement = false;
            }
        }
        GPTimings.PLAYER_DEATH_EVENT.stopTimingIfSync();
    }

    // when a player gets kicked...
    @Listener(order = Order.LAST)
    public void onPlayerKicked(KickPlayerEvent event) {
        GPTimings.PLAYER_KICK_EVENT.startTimingIfSync();
        Player player = event.getTargetEntity();
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        playerData.wasKicked = true;
        GPTimings.PLAYER_KICK_EVENT.stopTimingIfSync();
    }

    // when a player quits...
    @Listener(order= Order.LAST)
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        GPTimings.PLAYER_QUIT_EVENT.startTimingIfSync();
        Player player = event.getTargetEntity();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_QUIT_EVENT.stopTimingIfSync();
            return;
        }

        UUID playerID = player.getUniqueId();
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), playerID);
        boolean isBanned = false;
/*        if (playerData.wasKicked) {
            isBanned = player.isBanned();
        } else {
            isBanned = false;
        }*/

        // if banned, add IP to the temporary IP ban list
        if (isBanned && playerData.ipAddress != null) {
            long now = Calendar.getInstance().getTimeInMillis();
            this.tempBannedIps.add(new IpBanInfo(playerData.ipAddress, now + this.MILLISECONDS_IN_DAY, player.getName()));
        }

        // silence notifications when they're coming too fast, or the player is banned
        if (this.shouldSilenceNotification() || isBanned) {
            event.setMessage(Text.of());
        } else {
            // make sure his data is all saved - he might have accrued some claim
            // blocks while playing that were not saved immediately
            playerData.saveAllData();
        }

        // FEATURE: players in pvp combat when they log out will die
        if (GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().pvp.punishPvpLogout && playerData.inPvpCombat(player.getWorld())) {
            player.offer(Keys.HEALTH, 0d);
        }

        // FEATURE: during a siege, any player who logs out dies and forfeits the siege
        // if player was involved in a siege, he forfeits
        if (playerData.siegeData != null) {
            if (player.getHealthData().health().get() > 0) {
                // might already be zero from above, this avoids a double death message
                player.offer(Keys.HEALTH, 0d);
            }
        }

        // drop data about this player
        this.dataStore.clearCachedPlayerData(player.getWorld().getProperties(), playerID);

        // reduce count of players with that player's IP address
        // TODO: re-enable when achievement data is implemented
        /*if (GriefPrevention.instance.config_ipLimit > 0 && !player.getAchievementData().achievements().contains(Achievements.MINE_WOOD)) {
            InetAddress ipAddress = playerData.ipAddress;
            if (ipAddress != null) {
                String ipAddressString = ipAddress.toString();
                Integer count = this.ipCountHash.get(ipAddressString);
                if (count == null)
                    count = 1;
                this.ipCountHash.put(ipAddressString, count - 1);
            }
        }*/
        GPTimings.PLAYER_QUIT_EVENT.stopTimingIfSync();
    }

    // determines whether or not a login or logout notification should be
    // silenced, depending on how many there have been in the last minute
    private boolean shouldSilenceNotification() {
        final long ONE_MINUTE = 60000;
        final int MAX_ALLOWED = 20;
        Long now = Calendar.getInstance().getTimeInMillis();

        // eliminate any expired entries (longer than a minute ago)
        for (int i = 0; i < this.recentLoginLogoutNotifications.size(); i++) {
            Long notificationTimestamp = this.recentLoginLogoutNotifications.get(i);
            if (now - notificationTimestamp > ONE_MINUTE) {
                this.recentLoginLogoutNotifications.remove(i--);
            } else {
                break;
            }
        }

        // add the new entry
        this.recentLoginLogoutNotifications.add(now);

        return this.recentLoginLogoutNotifications.size() > MAX_ALLOWED;
    }

    // when a player drops an item
    @Listener(order = Order.FIRST)
    public void onPlayerDispenseItem(DropItemEvent.Dispense event, @Root EntitySpawnCause spawncause) {
        GPTimings.PLAYER_DISPENSE_ITEM_EVENT.startTimingIfSync();
        if (event.getCause().containsNamed("InventoryClose")) {
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        Entity entity = spawncause.getEntity();
        if (!(entity instanceof User)) {
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        User user = (User) entity;
        World world = event.getTargetWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        // in creative worlds, dropping items is blocked
        if (GriefPreventionPlugin.instance.claimModeIsActive(world.getProperties(), ClaimsMode.Creative)) {
            GriefPreventionPlugin.addEventLogEntry(event, null, null, user, "Drops not allowed in creative worlds.");
            event.setCancelled(true);
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        Player player = user instanceof Player ? (Player) user : null;
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());

        // FEATURE: players under siege or in PvP combat, can't throw items on
        // the ground to hide
        // them or give them away to other players before they are defeated

        // if in combat, don't let him drop it
        if (player != null && !GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().pvp.allowCombatItemDrops && playerData.inPvpCombat(player.getWorld())) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.PvPNoDrop);
            GriefPreventionPlugin.addEventLogEntry(event, null, player.getLocation(), user, this.dataStore.getMessage(Messages.PvPNoDrop));
            event.setCancelled(true);
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        // if he's under siege, don't let him drop it
        else if (player != null && playerData.siegeData != null) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.SiegeNoDrop);
            GriefPreventionPlugin.addEventLogEntry(event, null, player.getLocation(), user, this.dataStore.getMessage(Messages.SiegeNoDrop));
            event.setCancelled(true);
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        for (Entity entityItem : event.getEntities()) {
            Location<World> location = entityItem.getLocation();
            GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
            if (claim != null) {
                Tristate override = GPPermissionHandler.getFlagOverride(claim, GPPermissions.ITEM_DROP,  user, entityItem);
                if (override != Tristate.UNDEFINED) {
                    if (override == Tristate.TRUE) {
                        GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                        return;
                    }

                    event.setCancelled(true);
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.ITEM_DROP, user, entityItem, user, this.dataStore.getMessage(Messages.NoDropsAllowed));
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                }

                // allow trusted users
                if (claim.getInternalClaimData().getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID) 
                        || claim.getInternalClaimData().getContainers().contains(GriefPreventionPlugin.PUBLIC_UUID) 
                        || claim.getInternalClaimData().getBuilders().contains(user.getUniqueId()) 
                        || claim.getInternalClaimData().getContainers().contains(user.getUniqueId())
                        || claim.getInternalClaimData().getAccessors().contains(user.getUniqueId())) {
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                }

                Tristate perm = GPPermissionHandler.getClaimPermission(claim, GPPermissions.ITEM_DROP, user, entityItem, user);
                if (perm == Tristate.TRUE) {
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                } else if (perm == Tristate.FALSE) {
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.ITEM_DROP, user, entityItem, user, this.dataStore.getMessage(Messages.NoDropsAllowed));
                    event.setCancelled(true);
                    if (entity instanceof Player) {
                        Text message = GriefPreventionPlugin.getMessage(Messages.NoDropsAllowed);
                        GriefPreventionPlugin.sendClaimDenyMessage(claim, player, Text.of(TextMode.Warn, message));
                    }
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
    }

    // when a player interacts with an entity...
    @Listener(order = Order.FIRST)
    public void onPlayerInteractEntity(InteractEntityEvent event, @First Player player) {
        GPTimings.PLAYER_INTERACT_ENTITY_EVENT.startTimingIfSync();
        Entity targetEntity = event.getTargetEntity();
        if (targetEntity instanceof Player || !GriefPreventionPlugin.isEntityProtected(targetEntity)) {
            GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
            return;
        }

        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = targetEntity.getLocation();
        GPClaim claim = this.dataStore.getClaimAt(location);
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        // if entity has an owner, apply special rules
        IMixinEntity spongeEntity = (IMixinEntity) targetEntity;
        Optional<User> owner = spongeEntity.getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
        if (owner.isPresent()) {
            UUID ownerID = owner.get().getUniqueId();

            // if the player interacting is the owner or an admin in ignore claims mode, always allow
            if (player.getUniqueId().equals(ownerID) || playerData.canIgnoreClaim(claim)) {
                // if giving away pet, do that instead
                if (playerData.petGiveawayRecipient != null) {
                    SpongeEntityType spongeEntityType = ((SpongeEntityType) spongeEntity.getType());
                    if (spongeEntityType == null || spongeEntityType.equals(EntityTypes.UNKNOWN) || !spongeEntityType.getModId().equalsIgnoreCase("minecraft")) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.PetGiveawayInvalid, spongeEntity.getType().getId());
                        playerData.petGiveawayRecipient = null;
                        GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                        return;
                    }
                    spongeEntity.setCreator(playerData.petGiveawayRecipient.getUniqueId());
                    if (targetEntity instanceof EntityTameable) {
                        EntityTameable tameable = (EntityTameable) targetEntity;
                        tameable.setOwnerId(playerData.petGiveawayRecipient.getUniqueId());
                    } else if (targetEntity instanceof EntityHorse) {
                        EntityHorse horse = (EntityHorse) targetEntity;
                        horse.setOwnerUniqueId(playerData.petGiveawayRecipient.getUniqueId());
                        horse.setHorseTamed(true);
                    }
                    playerData.petGiveawayRecipient = null;
                    GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.PetGiveawayConfirmation);
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, event.getCause().root(), targetEntity, player, "Pet giveaway.");
                    event.setCancelled(true);
                }
                GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }
        }

        if (playerData.canIgnoreClaim(claim)) {
            GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
            return;
        }

        if (event instanceof InteractEntityEvent.Secondary) {
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ENTITY_SECONDARY, player, targetEntity, player, true) == Tristate.TRUE) {
                GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }
        } else {
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ENTITY_PRIMARY, player, targetEntity, player, true) == Tristate.TRUE) {
                GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }
        }
        String denyReason = claim.allowAccess(player, location);
        if (denyReason != null) {
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, player, event.getTargetEntity(), player, denyReason);
            GriefPreventionPlugin.sendClaimDenyMessage(claim, player, Text.of(TextMode.Err, denyReason));
            event.setCancelled(true);
            GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
            return;
        }

        if (owner.isPresent()) {
            if (!claim.pvpRulesApply()) {
                String denyMessage = claim.allowAccess(player);
                if (denyMessage != null) {
                    // otherwise disallow
                    String message = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NotYourPet, owner.get().getName());
                    if (event.getCause().root() instanceof Player) {
                        GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, message));
                    }
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, player, event.getTargetEntity(), player, message);
                    event.setCancelled(true);
                    GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        // if the entity is a vehicle
        if (targetEntity.supports(VehicleData.class)) {
            // if the entity is in a claim
            claim = this.dataStore.getClaimAt(location);
            // for storage entities, apply container rules (this is a potential theft)
            if (targetEntity instanceof Carrier) {
                denyReason = claim.allowAccess(player);
                if (denyReason != null) {
                    GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, denyReason));
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, player, event.getTargetEntity(), player, denyReason);
                    event.setCancelled(true);
                    GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        if (event instanceof InteractEntityEvent.Secondary && claim != null) {
            denyReason = claim.allowAccess(player, location);
            if (denyReason != null) {
                GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, denyReason));
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, player, event.getTargetEntity(), player, denyReason);
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ENTITY_SECONDARY, player, targetEntity, player) == Tristate.FALSE) {
                String entityId = targetEntity.getType() != null ? targetEntity.getType().getId() : ((net.minecraft.entity.Entity) targetEntity).getName();
                String message = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoInteractEntityPermission, claim.getOwnerName(), entityId);
                GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, message));
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.INTERACT_ENTITY_SECONDARY, player, event.getTargetEntity(), player, denyReason);
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }
        } else {
            denyReason = claim.allowAccess(player, location);
            if (denyReason != null) {
                String message = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, message));
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, player, event.getTargetEntity(), player, denyReason);
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }
        }

        // if preventing theft, prevent leashing claimed creatures
        if (targetEntity instanceof Animal && PlayerUtils.hasItemInOneHand(player, ItemTypes.LEAD)) {
            claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
            denyReason = claim.allowAccess(player, location);
            if (denyReason != null) {
                event.setCancelled(true);
                GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, denyReason));
                GriefPreventionPlugin.addEventLogEntry(event, claim, location,player, event.getTargetEntity(), player, denyReason);
                GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }
        }
        GPTimings.PLAYER_INTERACT_ENTITY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onPlayerInteractItem(InteractItemEvent.Primary event, @Root Player player) {
        World world = player.getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            return;
        }

        BlockSnapshot blockSnapshot = event.getCause().get(NamedCause.HIT_TARGET, BlockSnapshot.class).orElse(BlockSnapshot.NONE);
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        Vector3d interactPoint = event.getInteractionPoint().orElse(null);
        Entity entity = event.getCause().get(NamedCause.HIT_TARGET, Entity.class).orElse(null);
        Location<World> location = entity != null ? entity.getLocation() : interactPoint != null ? new Location<World>(world, interactPoint) : player.getLocation();
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);

        if (event.getItemStack().getType() != ItemTypes.NONE) {
            Tristate override = GPPermissionHandler.getFlagOverride(claim, GPPermissions.INTERACT_ITEM_PRIMARY, player, event.getItemStack());
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    return;
                }
    
                event.setCancelled(true);
                GriefPreventionPlugin.sendClaimDenyMessage(claim, player, TextMode.Err, Messages.NoInteractItemPermission, claim.getOwnerName(), event.getItemStack().getType().getId());
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.INTERACT_ITEM_PRIMARY, player, event.getItemStack().getType(), player, null);
                return;
            }
        } else if (blockSnapshot == null && entity == null) {
            return;
        }

        String denyReason = claim.allowAccess(player, location);
        if (denyReason != null) {
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ITEM_PRIMARY, player, event.getItemStack(), player) == Tristate.TRUE) {
                return;
            }
            // check entity interact
            if (entity != null && GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ENTITY_PRIMARY, player, entity, player) == Tristate.TRUE) {
                return;
            }

            GriefPreventionPlugin.sendClaimDenyMessage(claim, player, TextMode.Err, Messages.NoInteractItemPermission, claim.getOwnerName(), event.getItemStack().getType().getId());
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.INTERACT_ITEM_PRIMARY, player, event.getItemStack(), player, denyReason);
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    public void onPlayerInteractItem(InteractItemEvent.Secondary event, @Root Player player) {
        World world = player.getWorld();
        if (event.getItemStack().getType() instanceof ItemFood || !GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            return;
        }

        Optional<ItemStack> itemInHand = player.getItemInHand(event.getHandType());
        BlockSnapshot blockSnapshot = event.getCause().get(NamedCause.HIT_TARGET, BlockSnapshot.class).orElse(BlockSnapshot.NONE);
        if (investigateClaim(player, blockSnapshot, itemInHand)) {
            return;
        }

        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        Vector3d interactPoint = event.getInteractionPoint().orElse(null);
        Entity entity = event.getCause().get(NamedCause.HIT_TARGET, Entity.class).orElse(null);
        Location<World> location = entity != null ? entity.getLocation() : interactPoint != null ? new Location<World>(world, interactPoint) : player.getLocation();
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);

        if (event.getItemStack().getType() != ItemTypes.NONE) {
            Tristate override = GPPermissionHandler.getFlagOverride(claim, GPPermissions.INTERACT_ITEM_SECONDARY, player, event.getItemStack().getType());
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    return;
                }
    
                event.setCancelled(true);
                GriefPreventionPlugin.sendClaimDenyMessage(claim, player, TextMode.Err, Messages.NoInteractItemPermission, claim.getOwnerName(), event.getItemStack().getType().getId());
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.INTERACT_ITEM_SECONDARY, player, event.getItemStack().getType(), player, null);
                return;
            }
        } else if (blockSnapshot == null && entity == null) {
            return;
        }

        String denyReason = claim.allowAccess(player, location);
        if (denyReason != null) {
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ITEM_SECONDARY, player, event.getItemStack().getType(), player) == Tristate.TRUE) {
                return;
            }
            // check entity interact
            if (entity != null && GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ENTITY_SECONDARY, player, entity, player) == Tristate.TRUE) {
                return;
            }
            if (blockSnapshot != BlockSnapshot.NONE) {
                TileEntity tileEntity = world.getTileEntity(location.getBlockPosition()).orElse(null);
                if (tileEntity != null) {
                    ((EntityPlayerMP) player).closeScreen();
                }
            }
            GriefPreventionPlugin.sendClaimDenyMessage(claim, player, TextMode.Err, Messages.NoInteractItemPermission, claim.getOwnerName(), event.getItemStack().getType().getId());
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.INTERACT_ENTITY_SECONDARY, player, event.getItemStack().getType(), player, denyReason);
            event.setCancelled(true);
        }
    }

    // when a player picks up an item...
    @Listener(order = Order.LAST)
    public void onPlayerPickupItem(ChangeInventoryEvent.Pickup event, @Root Player player) {
        GPTimings.PLAYER_PICKUP_ITEM_EVENT.startTimingIfSync();
        World world = player.getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.PLAYER_PICKUP_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        Location<World> location = player.getLocation();
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
        if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ITEM_PICKUP, player, event.getTargetEntity(), player, true) == Tristate.FALSE) {
            event.setCancelled(true);
            GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.ITEM_PICKUP, player, event.getTargetEntity(), player, "");
            return;
        }

        // the rest of this code is specific to pvp worlds
        if (claim.pvpRulesApply()) {
            // if we're preventing spawn camping and the player was previously empty handed...
            if (GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().pvp.protectFreshSpawns && PlayerUtils.hasItemInOneHand(player, ItemTypes.NONE)) {
                // if that player is currently immune to pvp
                if (playerData.pvpImmune) {
                    // if it's been less than 10 seconds since the last time he spawned, don't pick up the item
                    long now = Calendar.getInstance().getTimeInMillis();
                    long elapsedSinceLastSpawn = now - playerData.lastSpawn;
                    if (elapsedSinceLastSpawn < 10000) {
                        GriefPreventionPlugin.addEventLogEntry(event, claim, location, player, event.getTargetEntity(), player, "Player PVP Immune.");
                        event.setCancelled(true);
                        GPTimings.PLAYER_PICKUP_ITEM_EVENT.stopTimingIfSync();
                        return;
                    }
    
                    // otherwise take away his immunity. he may be armed now. at least, he's worth killing for some loot
                    playerData.pvpImmune = false;
                    GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
                }
            }
        }
        GPTimings.PLAYER_PICKUP_ITEM_EVENT.stopTimingIfSync();
    }

    // when a player switches in-hand items
    @Listener
    public void onPlayerChangeHeldItem(ChangeInventoryEvent.Held event, @First Player player) {
        GPTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.stopTimingIfSync();
            return;
        }
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        int count = 0;
        // if he's switching to the golden shovel
        for (SlotTransaction transaction : event.getTransactions()) {
            ItemStackSnapshot newItemStack = transaction.getFinal();
            if (count == 1 && newItemStack != null && newItemStack.getType().equals(GriefPreventionPlugin.instance.modificationTool)) {
                playerData.lastShovelLocation = null;
                playerData.endShovelLocation = null;
                playerData.claimResizing = null;
                // always reset to basic claims mode
                if (playerData.shovelMode != ShovelMode.Basic) {
                    playerData.shovelMode = ShovelMode.Basic;
                    GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.ShovelBasicClaimMode);
                }

                // tell him how many claim blocks he has available
                int remainingBlocks = playerData.getRemainingClaimBlocks();
                GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.RemainingBlocks, String.valueOf(remainingBlocks));

                // link to a video demo of land claiming, based on world type
                if (GriefPreventionPlugin.instance.claimModeIsActive(player.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.CreativeBasicsVideo2);
                } else if (GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getLocation().getExtent().getProperties())) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2);
                }
            } else {
                if (playerData.lastShovelLocation != null) {
                    playerData.revertActiveVisual(player);
                }
                playerData.lastShovelLocation = null;
                playerData.endShovelLocation = null;
                playerData.claimResizing = null;
            }
            count++;
        }
        GPTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onPlayerUseItem(UseItemStackEvent.Start event, @First Player player) {
        GPTimings.PLAYER_USE_ITEM_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = player.getLocation();
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(location.getExtent(), player.getUniqueId());
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);

        Tristate override = GPPermissionHandler.getFlagOverride(claim, GPPermissions.ITEM_USE, player, event.getItemStackInUse().getType());
        if (override != Tristate.UNDEFINED) {
            if (override == Tristate.TRUE) {
                GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
                return;
            }

            event.setCancelled(true);
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.ITEM_USE, player, event.getItemStackInUse().getType(), player, "");
            GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        String denyMessage = claim.allowAccess(player);
        if (denyMessage != null) {
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ITEM_USE, player, event.getItemStackInUse().getType(), player) == Tristate.TRUE) {
                GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
                return;
            }

            String message = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.ItemNotAuthorized, event.getItemStackInUse().getType().getId());
            GriefPreventionPlugin.sendClaimDenyMessage(claim, player, TextMode.Err, message);
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.ITEM_USE, player, event.getItemStackInUse().getType(), player, message);
            event.setCancelled(true);
        }
        GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
    }

    // educates a player about /adminclaims and /acb, if he can use them
    private void tryAdvertiseAdminAlternatives(Player player) {
        if (player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && player.hasPermission(GPPermissions.COMMAND_ADJUST_CLAIM_BLOCKS)) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.AdvertiseACandACB);
        } else if (player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.AdvertiseAdminClaims);
        } else if (player.hasPermission(GPPermissions.COMMAND_ADJUST_CLAIM_BLOCKS)) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.AdvertiseACB);
        }
    }

    @Listener
    public void onPlayerInteractBlockPrimary(InteractBlockEvent.Primary.MainHand event, @First Player player) {
        GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }

        final BlockSnapshot clickedBlock = event.getTargetBlock();
        final Location<World> location = clickedBlock.getLocation().orElse(null);
        if (location == null) {
            GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }

        final GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(location.getExtent(), player.getUniqueId());
        final GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);

        Tristate override = GPPermissionHandler.getFlagOverride(claim, GPPermissions.INTERACT_BLOCK_PRIMARY, player, clickedBlock.getState());
        if (override != Tristate.UNDEFINED) {
            if (override == Tristate.TRUE) {
                GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
                return;
            }

            event.setCancelled(true);
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.INTERACT_BLOCK_PRIMARY, player, clickedBlock.getState(), player, "");
            GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }

        String denyReason = claim.allowAccess(player, location);
        if (denyReason != null) {
            // check if player is allowed to break target
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.BLOCK_BREAK, player, clickedBlock.getState(), player) == Tristate.TRUE) {
                GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
                playerData.setLastInteractData(claim);
                return;
            }
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_BLOCK_PRIMARY, player, clickedBlock.getState(), player) == Tristate.TRUE) {
                GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
                playerData.setLastInteractData(claim);
                return;
            }

            GriefPreventionPlugin.addEventLogEntry(event, claim, location, GPPermissions.BLOCK_BREAK, player, clickedBlock.getState(), player, denyReason);
            GriefPreventionPlugin.sendClaimDenyMessage(claim, player, Text.of(TextMode.Err, denyReason));
            event.setCancelled(true);
            GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }
        playerData.setLastInteractData(claim);
        GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
    }

    @Listener
    public void onPlayerInteractBlockSecondary(InteractBlockEvent.Secondary event, @First Player player) {
        GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }

        BlockSnapshot clickedBlock = event.getTargetBlock();
        HandType handType = event.getHandType();
        ItemStack itemInHand = player.getItemInHand(handType).orElse(null);

        // Check if item is banned
        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties());
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Location<World> location = clickedBlock.getLocation().orElse(null);

        if (location == null) {
            onPlayerHandleShovelAction(event, event.getTargetBlock(), player, handType, playerData);
            GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }

        GPClaim playerClaim = this.dataStore.getClaimAtPlayer(playerData, location, false);
        if (playerData != null && !playerData.canIgnoreClaim(playerClaim)) {
            // following a siege where the defender lost, the claim will allow everyone access for a time
            if (playerClaim.doorsOpen && activeConfig.getConfig().siege.winnerAccessibleBlocks.contains(clickedBlock.getState().getType().getId())) {
                if (clickedBlock.getState().getType() == BlockTypes.IRON_DOOR) {
                    ((BlockDoor) clickedBlock.getState().getType()).toggleDoor((net.minecraft.world.World) player.getWorld(), VecHelper.toBlockPos(event.getTargetBlock().getPosition()), true);
                }
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }

            Tristate override = GPPermissionHandler.getFlagOverride(playerClaim, GPPermissions.INTERACT_BLOCK_SECONDARY, player, event.getTargetBlock());
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, playerClaim, location, GPPermissions.INTERACT_BLOCK_SECONDARY, player, event.getTargetBlock(), player, "");
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }

            String denyReason = "";
            if (clickedBlock.getLocation().get().hasTileEntity()) {
                denyReason = playerClaim.allowContainers(player, location);
            } else {
                denyReason = playerClaim.allowAccess(player, location, true);
            }

            if(denyReason != null) {
                // if player is holding an item, check if it can be placed
                if (itemInHand != null) {
                    if (GPPermissionHandler.getClaimPermission(playerClaim, GPPermissions.BLOCK_PLACE, player, itemInHand, player) == Tristate.TRUE) {
                        GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                        playerData.setLastInteractData(playerClaim);
                        return;
                    }
                }
                // Don't send a deny message if the player is holding an investigation tool
                if (!PlayerUtils.hasItemInOneHand(player, GriefPreventionPlugin.instance.investigationTool)) {
                    GriefPreventionPlugin.sendClaimDenyMessage(playerClaim, player, Text.of(TextMode.Err, denyReason));
                }

                GriefPreventionPlugin.addEventLogEntry(event, playerClaim, location, GPPermissions.INTERACT_BLOCK_SECONDARY, player, event.getTargetBlock(), player, denyReason);
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }
        }

        // apply rules for containers
        TileEntity tileEntity = clickedBlock.getLocation().get().getTileEntity().orElse(null);
        if (tileEntity != null && tileEntity instanceof IInventory) {
            if (playerData == null) {
                playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            }

            // block container use while under siege, so players can't hide items from attackers
            if (playerData.siegeData != null) {
                GriefPreventionPlugin.addEventLogEntry(event, playerClaim, location, player, tileEntity, player, this.dataStore.getMessage(Messages.SiegeNoContainers));
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.SiegeNoContainers);
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }

            // block container use during pvp combat, same reason
            if (playerData.inPvpCombat(player.getWorld())) {
                GriefPreventionPlugin.addEventLogEntry(event, playerClaim, location, player, tileEntity, player, this.dataStore.getMessage(Messages.PvPNoContainers));
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }

            // if the event hasn't been cancelled, then the player is allowed to use the container so drop any pvp protection
            if (playerData.pvpImmune) {
                playerData.pvpImmune = false;
                GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
            }
        }
        // otherwise handle right click (shovel, string, bonemeal)
        else if (itemInHand != null && itemInHand.getItem() == GriefPreventionPlugin.instance.modificationTool) {
            // disable golden shovel while under siege
            if (playerData == null)
                playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            if (playerData.siegeData != null) {
                GriefPreventionPlugin.addEventLogEntry(event, playerClaim, location, player, itemInHand.getItem(), player, this.dataStore.getMessage(Messages.SiegeNoShovel));
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.SiegeNoShovel);
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }

            onPlayerHandleShovelAction(event, event.getTargetBlock(), player, handType, playerData);
            // avoid changing blocks after using a shovel
            event.setCancelled(true);
        }
        playerData.setLastInteractData(playerClaim);
        GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
    }

    private void onPlayerHandleShovelAction(InteractEvent event, BlockSnapshot targetBlock, Player player, HandType handType, GPPlayerData playerData) {
        GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.startTimingIfSync();
        if (!player.getItemInHand(handType).isPresent()) {
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties());
        // what's the player holding?
        ItemType materialInHand = player.getItemInHand(handType).get().getItem();
        if (!materialInHand.getId().equals(activeConfig.getConfig().claim.modificationTool)) {
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        BlockSnapshot clickedBlock = targetBlock;
        // disable golden shovel while under siege
        if (playerData.siegeData != null) {
            GriefPreventionPlugin.addEventLogEntry(event, null, targetBlock.getLocation().orElse(null), player, materialInHand, player, this.dataStore.getMessage(Messages.SiegeNoShovel));
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.SiegeNoShovel);
            event.setCancelled(true);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // FEATURE: shovel and stick can be used from a distance away
        if (clickedBlock.getState().getType() == BlockTypes.AIR) {
            // try to find a far away non-air block along line of sight
            clickedBlock = getTargetBlock(player, 100);
        }

        // if no block, stop here
        if (clickedBlock == null) {
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // can't use the shovel from too far away
        if (clickedBlock.getState().getType() == BlockTypes.AIR) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.TooFarAway);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // if the player is in restore nature mode, do only that
        UUID playerID = player.getUniqueId();
        playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.shovelMode == ShovelMode.RestoreNature || playerData.shovelMode == ShovelMode.RestoreNatureAggressive) {
            // if the clicked block is in a claim, visualize that claim and deliver an error message
            GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, clickedBlock.getLocation().get());
            if (!claim.isWildernessClaim()) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.BlockClaimed, claim.getOwnerName());
                Visualization visualization = new Visualization(claim, VisualizationType.ErrorClaim);
                visualization.createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                visualization.apply(player);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // figure out which chunk to repair
            Chunk chunk = player.getWorld().getChunk(clickedBlock.getLocation().get().getBlockX() >> 4, 0, clickedBlock.getLocation().get().getBlockZ() >> 4).get();

            // start the repair process

            // set boundaries for processing
            int miny = clickedBlock.getPosition().getY();

            // if not in aggressive mode, extend the selection down to a little below sea level
            if (!(playerData.shovelMode == ShovelMode.RestoreNatureAggressive)) {
                if (miny > GriefPreventionPlugin.instance.getSeaLevel(chunk.getWorld()) - 10) {
                    miny = GriefPreventionPlugin.instance.getSeaLevel(chunk.getWorld()) - 10;
                }
            }

            GriefPreventionPlugin.instance.restoreChunk(chunk, miny, playerData.shovelMode == ShovelMode.RestoreNatureAggressive, 0, player);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // if in restore nature fill mode
        if (playerData.shovelMode == ShovelMode.RestoreNatureFill) {
            ArrayList<BlockType> allowedFillBlocks = new ArrayList<BlockType>();
            DimensionType environment = clickedBlock.getLocation().get().getExtent().getDimension().getType();
            if (environment.equals(DimensionTypes.NETHER)) {
                allowedFillBlocks.add(BlockTypes.NETHERRACK);
            } else if (environment.equals(DimensionTypes.THE_END)) {
                allowedFillBlocks.add(BlockTypes.END_STONE);
            } else {
                allowedFillBlocks.add(BlockTypes.GRASS);
                allowedFillBlocks.add(BlockTypes.DIRT);
                allowedFillBlocks.add(BlockTypes.STONE);
                allowedFillBlocks.add(BlockTypes.SAND);
                allowedFillBlocks.add(BlockTypes.SANDSTONE);
                allowedFillBlocks.add(BlockTypes.ICE);
            }

            int maxHeight = clickedBlock.getPosition().getY();
            int minx = clickedBlock.getPosition().getX() - playerData.fillRadius;
            int maxx = clickedBlock.getPosition().getX() + playerData.fillRadius;
            int minz = clickedBlock.getPosition().getZ() - playerData.fillRadius;
            int maxz = clickedBlock.getPosition().getZ() + playerData.fillRadius;
            int minHeight = maxHeight - 10;
            if (minHeight < 0)
                minHeight = 0;

            WeakReference<Claim> cachedClaim = null;
            for (int x = minx; x <= maxx; x++) {
                for (int z = minz; z <= maxz; z++) {
                    // circular brush
                    Location<World> location = new Location<World>(clickedBlock.getLocation().get().getExtent(), x, clickedBlock.getPosition().getY(), z);
                    if (location.getPosition().distance(clickedBlock.getLocation().get().getPosition()) > playerData.fillRadius) {
                        continue;
                    }

                    // default fill block is initially the first from the
                    // allowed fill blocks list above
                    BlockType defaultFiller = allowedFillBlocks.get(0);

                    // prefer to use the block the player clicked on, if
                    // it's an acceptable fill block
                    if (allowedFillBlocks.contains(clickedBlock.getState().getType())) {
                        defaultFiller = clickedBlock.getState().getType();
                    }

                    // if the player clicks on water, try to sink through
                    // the water to find something underneath that's useful
                    // for a filler
                    else if (clickedBlock.getState().getType() == BlockTypes.FLOWING_WATER || clickedBlock.getState().getType() == BlockTypes.WATER) {
                        BlockType newBlockType = clickedBlock.getState().getType();
                        while (newBlockType != BlockTypes.FLOWING_WATER && newBlockType != BlockTypes.WATER) {
                            newBlockType = clickedBlock.getLocation().get().getRelative(Direction.DOWN).getBlockType();
                        }
                        if (allowedFillBlocks.contains(newBlockType)) {
                            defaultFiller = newBlockType;
                        }
                    }

                    // fill bottom to top
                    for (int y = minHeight; y <= maxHeight; y++) {
                        BlockSnapshot block = clickedBlock.getLocation().get().getExtent().createSnapshot(x, y, z);

                        // respect claims
                        GPClaim claim = this.dataStore.getClaimAt(block.getLocation().get(), false, cachedClaim);
                        if (claim != null) {
                            cachedClaim = new WeakReference<>(claim);
                            break;
                        }

                        // only replace air, spilling water, snow, long grass
                        if (block.getState().getType() == BlockTypes.AIR || block.getState().getType() == BlockTypes.SNOW
                                || (block.getState().getType() == BlockTypes.WATER)
                                || block.getState().getType() == BlockTypes.TALLGRASS) {
                            // if the top level, always use the default filler picked above
                            if (y == maxHeight) {
                                block.withState(defaultFiller.getDefaultState()).restore(true, BlockChangeFlag.PHYSICS);
                            }

                            // otherwise look to neighbors for an appropriate fill block
                            else {
                                Location<World> eastBlock = block.getLocation().get().getRelative(Direction.EAST);
                                Location<World> westBlock = block.getLocation().get().getRelative(Direction.WEST);
                                Location<World> northBlock = block.getLocation().get().getRelative(Direction.NORTH);
                                Location<World> southBlock = block.getLocation().get().getRelative(Direction.SOUTH);

                                // first, check lateral neighbors (ideally, want to keep natural layers)
                                if (allowedFillBlocks.contains(eastBlock.getBlockType())) {
                                    block.withState(eastBlock.getBlock()).restore(true, BlockChangeFlag.PHYSICS);
                                } else if (allowedFillBlocks.contains(westBlock.getBlockType())) {
                                    block.withState(westBlock.getBlock()).restore(true, BlockChangeFlag.PHYSICS);
                                } else if (allowedFillBlocks.contains(northBlock.getBlockType())) {
                                    block.withState(northBlock.getBlock()).restore(true, BlockChangeFlag.PHYSICS);
                                } else if (allowedFillBlocks.contains(southBlock.getBlockType())) {
                                    block.withState(southBlock.getBlock()).restore(true, BlockChangeFlag.PHYSICS);
                                }

                                // if all else fails, use the default filler selected above
                                else {
                                    block.withState(defaultFiller.getDefaultState()).restore(true, BlockChangeFlag.PHYSICS);
                                }
                            }
                        }
                    }
                }
            }
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // if the player doesn't have claims permission, don't do anything
        if (!player.hasPermission(GPPermissions.CLAIM_CREATE)) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoCreateClaimPermission);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        if (playerData.shovelMode == ShovelMode.Basic && playerData.getCuboidMode() && !player.hasPermission(GPPermissions.CLAIM_CUBOID_BASIC)) {
            playerData.setCuboidMode(false);
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, "You do not have permission to create/resize basic claims in 3D mode.");
            GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.CuboidClaimDisabled);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        } else if (playerData.shovelMode == ShovelMode.Subdivide && playerData.getCuboidMode() && !player.hasPermission(GPPermissions.CLAIM_CUBOID_SUBDIVISION)) {
            playerData.setCuboidMode(false);
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, "You do not have permission to create/resize subdivisions in 3D mode.");
            GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.CuboidClaimDisabled);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // if he's resizing a claim
        if (playerData.claimResizing != null) {
            if (clickedBlock.getLocation().get().equals(playerData.lastShovelLocation)) {
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            playerData.endShovelLocation = clickedBlock.getLocation().get();
            // figure out what the coords of his new claim would be
            int newx1, newx2, newz1, newz2, newy1, newy2;
            int smallX = 0, smallY = 0, smallZ = 0, bigX = 0, bigY = 0, bigZ = 0;
            int newWidth = 0;
            int newHeight = 0;

            if (playerData.claimResizing.isCuboid()) {
                newx1 = playerData.lastShovelLocation.getBlockX();
                newx2 = clickedBlock.getPosition().getX();
                newy1 = playerData.lastShovelLocation.getBlockY();
                newy2 = clickedBlock.getPosition().getY();
                newz1 = playerData.lastShovelLocation.getBlockZ();
                newz2 = clickedBlock.getPosition().getZ();
                Location<World> lesserBoundaryCorner = playerData.claimResizing.getLesserBoundaryCorner();
                Location<World> greaterBoundaryCorner = playerData.claimResizing.getGreaterBoundaryCorner();
                smallX = lesserBoundaryCorner.getBlockX();
                smallY = lesserBoundaryCorner.getBlockY();
                smallZ = lesserBoundaryCorner.getBlockZ();
                bigX = greaterBoundaryCorner.getBlockX();
                bigY = greaterBoundaryCorner.getBlockY();
                bigZ = greaterBoundaryCorner.getBlockZ();

                if (newx1 == smallX) {
                    smallX = newx2;
                } else {
                    bigX = newx2;
                }

                if (newy1 == smallY) {
                    smallY = newy2;
                } else {
                    bigY = newy2;
                }

                if (newz1 == smallZ) {
                    smallZ = newz2;
                } else {
                    bigZ = newz2;
                }
                /*newx1 = smallX;
                newy1 = smallY;
                newz1 = smallZ;
                newx2 = bigX;
                newy2 = bigY;
                newz2 = bigZ;*/
                newWidth = Math.abs(bigX - smallX) + 1;
                newHeight = Math.abs(bigZ - smallZ) + 1;
            } else {
                if (playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getLesserBoundaryCorner().getBlockX()) {
                    newx1 = clickedBlock.getPosition().getX();
                } else {
                    newx1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockX();
                }
    
                if (playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockX()) {
                    newx2 = clickedBlock.getPosition().getX();
                } else {
                    newx2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockX();
                }
    
                if (playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getLesserBoundaryCorner().getBlockZ()) {
                    newz1 = clickedBlock.getPosition().getZ();
                } else {
                    newz1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockZ();
                }
    
                if (playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ()) {
                    newz2 = clickedBlock.getPosition().getZ();
                } else {
                    newz2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ();
                }
    
                newy1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockY();
                newy2 = clickedBlock.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance;
                if (newy2 < 0) {
                    newy2 = 0;
                }

                newWidth = (Math.abs(newx1 - newx2) + 1);
                newHeight = (Math.abs(newz1 - newz2) + 1);
            }
            int maxClaimX = playerData.getMaxClaimX(playerData.claimResizing.getType());
            int maxClaimY = playerData.getMaxClaimY(playerData.claimResizing.getType());
            int maxClaimZ = playerData.getMaxClaimZ(playerData.claimResizing.getType());
            if (maxClaimX > 0 && newWidth > maxClaimX) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, "You cannot resize passed the max claim x of " + maxClaimX + ".");
                event.setCancelled(true);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }
            if (playerData.claimResizing.isCuboid() && maxClaimY > 0 && newWidth > maxClaimY) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, "You cannot resize passed the max claim y of " + maxClaimY + ".");
                event.setCancelled(true);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }
            if (maxClaimZ > 0 && newWidth > maxClaimZ) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, "You cannot resize passed the max claim z of " + maxClaimZ + ".");
                event.setCancelled(true);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // for top level claims, apply size rules and claim blocks requirement
            if (playerData.claimResizing.parent == null) {
                // measure new claim, apply size rules
                int newArea = newWidth * newHeight;
                boolean smaller = newWidth < playerData.claimResizing.getWidth() || newHeight < playerData.claimResizing.getHeight();

                if (!player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && !playerData.claimResizing.isAdminClaim() && smaller) {
                    if (newWidth < activeConfig.getConfig().claim.claimMinimumWidth
                            || newHeight < activeConfig.getConfig().claim.claimMinimumWidth) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ResizeClaimTooNarrow, String.valueOf(newArea), String.valueOf(newWidth), String.valueOf(newHeight),
                                String.valueOf(activeConfig.getConfig().claim.claimMinimumWidth));
                        GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                        return;
                    }

                    if (newArea < activeConfig.getConfig().claim.claimMinimumArea) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ResizeClaimInsufficientArea, String.valueOf(newArea), String.valueOf(newWidth), String.valueOf(newHeight),
                                String.valueOf(activeConfig.getConfig().claim.claimMinimumArea));
                        GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                        return;
                    }
                }

                // make sure player has enough blocks to make up the difference
                if (!playerData.claimResizing.isAdminClaim() && player.getName().equals(playerData.claimResizing.getOwnerName())) {
                    int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + (playerData.claimResizing.getArea() - newArea);
                    if (blocksRemainingAfter < 0) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ResizeNeedMoreBlocks,
                                String.valueOf(Math.abs(blocksRemainingAfter)));
                        this.tryAdvertiseAdminAlternatives(player);
                        GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                        return;
                    }
                }
            }

            // special rule for making a top-level claim smaller. to check this, verifying the old claim's corners are inside the new claim's boundaries.
            // rule: in any mode, shrinking a claim removes any surface fluids
            GPClaim oldClaim = playerData.claimResizing;
            boolean smaller = false;
            if (oldClaim.parent == null) {
                GPClaim newClaim = null;
                // temporary claim instance, just for checking contains()
                if (oldClaim.isCuboid()) {
                    newClaim = new GPClaim(
                            new Location<World>(oldClaim.getLesserBoundaryCorner().getExtent(), smallX, smallY, smallZ),
                            new Location<World>(oldClaim.getLesserBoundaryCorner().getExtent(), bigX, bigY, bigZ), ClaimType.BASIC, oldClaim.isCuboid());

                } else {
                    newClaim = new GPClaim(
                        new Location<World>(oldClaim.getLesserBoundaryCorner().getExtent(), newx1, newy1, newz1),
                        new Location<World>(oldClaim.getLesserBoundaryCorner().getExtent(), newx2, newy2, newz2), ClaimType.BASIC, oldClaim.isCuboid());
                }
                // if the new claim is smaller
                if (!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false)
                        || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false)) {
                    smaller = true;

                    // remove surface fluids about to be unclaimed
                    oldClaim.removeSurfaceFluids(newClaim);
                }
            }

            // ask the datastore to try and resize the claim, this checks for conflicts with other claims
            ClaimResult claimResult = null;
            if (playerData.claimResizing.isCuboid()) {
                // 3D resize
                claimResult = playerData.claimResizing.resizeCuboid(newx1, newy1, newz1, newx2, newy2, newz2, Cause.of(NamedCause.source(player)));
            } else {
                // 2D resize
                claimResult = playerData.claimResizing.resize(newx1, newx2, newy1, newy2, newz1, newz2, Cause.of(NamedCause.source(player)));
            }
            if (claimResult.successful()) {
                Claim claim = (GPClaim) claimResult.getClaim().get();
                // decide how many claim blocks are available for more resizing
                int claimBlocksRemaining = 0;
                if (!playerData.claimResizing.isAdminClaim()) {
                    UUID ownerID = playerData.claimResizing.getOwnerUniqueId();
                    if (playerData.claimResizing.parent != null) {
                        ownerID = playerData.claimResizing.parent.getOwnerUniqueId();
                    }

                    if (ownerID.equals(player.getUniqueId())) {
                        claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                    } else {
                        GPPlayerData ownerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), ownerID);
                        claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                        Optional<User> owner = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(ownerID);
                        if (owner.isPresent() && !owner.get().isOnline()) {
                            this.dataStore.clearCachedPlayerData(player.getWorld().getProperties(), ownerID);
                        }
                    }
                }

                // clean up
                playerData.claimResizing = null;
                playerData.lastShovelLocation = null;
                playerData.endShovelLocation = null;
                // inform about success, visualize, communicate remaining blocks available
                GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.ClaimResizeSuccess, String.valueOf(claimBlocksRemaining));
                playerData.revertActiveVisual(player);
                ((GPClaim) claim).getVisualizer().resetVisuals();
                ((GPClaim) claim).getVisualizer().createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                ((GPClaim) claim).getVisualizer().apply(player);

                // if resizing someone else's claim, make a log entry
                if (!playerID.equals(claim.getOwnerUniqueId()) && !claim.getParent().isPresent()) {
                    GriefPreventionPlugin.addLogEntry(player.getName() + " resized " + claim.getOwnerName() + "'s claim at "
                            + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + ".");
                }

                // if increased to a sufficiently large size and no subdivisions yet, send subdivision instructions
                if (oldClaim.getArea() < 1000 && claim.getArea() >= 1000 && claim.getSubdivisions().isEmpty()
                        && !player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.BecomeMayor, 200L);
                    GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, 201L);
                }

                // if in a creative mode world and shrinking an existing claim, restore any unclaimed area
                if (smaller && GriefPreventionPlugin.instance.claimModeIsActive(oldClaim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                    GriefPreventionPlugin.instance.restoreClaim(oldClaim, 20L * 60 * 2); // 2 minutes
                    GriefPreventionPlugin.addLogEntry(player.getName() + " shrank a claim @ "
                            + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                }
            } else {
                if (claimResult.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                    GPClaim overlapClaim = (GPClaim) claimResult.getClaim().get();
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlap);
                    overlapClaim.getVisualizer().createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                    // show the player the conflicting claim
                    overlapClaim.getVisualizer().apply(player);
                    List<Claim> claims = new ArrayList<>();
                    claims.add(overlapClaim);
                    CommandHelper.showOverlapClaims(player, claims);
                }
            }
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // otherwise, since not currently resizing a claim, must be starting
        // a resize, creating a new claim, or creating a subdivision

        GPClaim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), true);
        // if within an existing claim, he's not creating a new one
        if (!claim.isWildernessClaim()) {
            // if the player has permission to edit the claim or subdivision
            String noEditReason = claim.allowEdit(player);
            if (noEditReason == null) {
                // if he clicked on a corner, start resizing it
                if (BlockUtils.clickedClaimCorner(claim, clickedBlock.getPosition())) {
                    boolean playerCanResize = true;
                    // players can always resize subdivisions
                    if (!claim.isSubdivision() && !player.hasPermission(GPPermissions.CLAIM_RESIZE) && claim.allowEdit(player) != null) {
                        if (claim.parent == null) {
                            if (claim.isAdminClaim()) {
                                playerCanResize = player.hasPermission(GPPermissions.CLAIM_RESIZE_ADMIN);
                            } else {
                                playerCanResize = player.hasPermission(GPPermissions.CLAIM_RESIZE_BASIC);
                            }
                        } else {
                            if (claim.isAdminClaim()) {
                                playerCanResize = player.hasPermission(GPPermissions.CLAIM_RESIZE_ADMIN_SUBDIVISION);
                            } else {
                                playerCanResize = player.hasPermission(GPPermissions.CLAIM_RESIZE_BASIC_SUBDIVISION);
                            }
                        }
                    }

                    if (claim.getInternalClaimData().isResizable() && !playerCanResize) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, "You do not have permission to resize this claim.");
                        return;
                    }
                    playerData.claimResizing = claim;
                    playerData.lastShovelLocation = clickedBlock.getLocation().get();
                    GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.ResizeStart);
                }

                // if he didn't click on a corner and is in subdivision
                // mode, he's creating a new subdivision
                else if (playerData.shovelMode == ShovelMode.Subdivide) {
                    // if it's the first click, he's trying to start a new subdivision
                    if (playerData.lastShovelLocation == null) {
                        // if the clicked claim was a subdivision, tell him
                        // he can't start a new subdivision here
                        if (claim.parent != null) {
                            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapSubdivision);
                        }

                        // otherwise start a new subdivision
                        else {
                            GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.SubdivisionStart);
                            playerData.lastShovelLocation = clickedBlock.getLocation().get();
                            playerData.claimSubdividing = claim;
                            Visualization visualization = Visualization.fromClick(clickedBlock.getLocation().get(), clickedBlock.getPosition().getY(), VisualizationType.Subdivision, playerData);
                            visualization.apply(player);
                            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                            return;
                        }
                    }

                    // otherwise, he's trying to finish creating a
                    // subdivision by setting the other boundary corner
                    else {
                        // try to create a new claim (will return null if
                        // this subdivision overlaps another)
                        Vector3i lesserBoundaryCorner = new Vector3i(playerData.lastShovelLocation.getBlockX(), 
                                playerData.lastShovelLocation.getBlockY() - (playerData.getCuboidMode() ? 0 : activeConfig.getConfig().claim.extendIntoGroundDistance),
                                playerData.lastShovelLocation.getBlockZ());
                        Vector3i greaterBoundaryCorner = new Vector3i(clickedBlock.getPosition().getX(), clickedBlock.getPosition().getY() - (playerData.getCuboidMode() ? 0 : activeConfig.getConfig().claim.extendIntoGroundDistance),
                                clickedBlock.getPosition().getZ());
                        ClaimResult result = playerData.claimSubdividing.createSubdivision(
                                lesserBoundaryCorner, greaterBoundaryCorner,
                                player.getUniqueId(), playerData.getCuboidMode(), GriefPreventionPlugin.pluginCause);

                        GPClaim gpClaim = (GPClaim) result.getClaim().orElse(null);
                        // if it didn't succeed, tell the player why
                        if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CreateSubdivisionOverlap);
                            Visualization visualization = new Visualization(gpClaim, VisualizationType.ErrorClaim);
                            visualization.createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                            visualization.apply(player);
                            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                            return;
                        }

                        // otherwise, advise him on the /trust command and show him his new subdivision
                        else {
                            playerData.lastShovelLocation = null;
                            playerData.claimSubdividing = null;
                            GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.SubdivisionSuccess);
                            gpClaim.getVisualizer().createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                            gpClaim.getVisualizer().apply(player);
                        }
                    }
                }

                // otherwise tell him he can't create a claim here, and show him the existing claim
                // also advise him to consider /abandonclaim or resizing the existing claim
                else {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlap);
                    claim.getVisualizer().createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                    claim.getVisualizer().apply(player);
                }
            }

            // otherwise tell the player he can't claim here because it's
            // someone else's claim, and show him the claim
            else {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapOtherPlayer, claim.getOwnerName());
                Visualization visualization = new Visualization(claim, VisualizationType.ErrorClaim);
                visualization.createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                visualization.apply(player);
            }
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        } else if (playerData.shovelMode == ShovelMode.Subdivide && playerData.lastShovelLocation != null) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.SubdivisionNoClaimFound);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // otherwise, the player isn't in an existing claim!

        // if he hasn't already start a claim with a previous shovel action
        Location<World> lastShovelLocation = playerData.lastShovelLocation;
        if (lastShovelLocation == null) {
            // if he's at the claim count per player limit already and
            // doesn't have permission to bypass, display an error message
            if (playerData.optionCreateClaimLimit > 0 && !player.hasPermission(GPPermissions.OVERRIDE_CLAIM_LIMIT) &&
                    playerData.getClaims().size() >= playerData.optionCreateClaimLimit) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ClaimCreationFailedOverClaimCountLimit);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // remember it, and start him on the new claim
            playerData.lastShovelLocation = clickedBlock.getLocation().get();
            GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.ClaimStart);

            // show him where he's working
            VisualizationType visualType = VisualizationType.Claim;
            if (playerData.shovelMode == ShovelMode.Admin) {
                visualType = VisualizationType.AdminClaim;
            }

            Visualization visualization = Visualization.fromClick(clickedBlock.getLocation().get(), clickedBlock.getPosition().getY(), visualType, playerData);
            visualization.apply(player);
        }

        // otherwise, he's trying to finish creating a claim by setting the other boundary corner
        else {
            // apply pvp rule
            if (playerData.inPvpCombat(player.getWorld())) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoClaimDuringPvP);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // apply minimum claim dimensions rule
            int newClaimWidth = Math.abs(playerData.lastShovelLocation.getBlockX() - clickedBlock.getPosition().getX()) + 1;
            int newClaimHeight = Math.abs(playerData.lastShovelLocation.getBlockZ() - clickedBlock.getPosition().getZ()) + 1;

            if (playerData.shovelMode != ShovelMode.Admin) {
                int newArea = newClaimWidth * newClaimHeight;
                if (newClaimWidth < activeConfig.getConfig().claim.claimMinimumWidth
                        || newClaimHeight < activeConfig.getConfig().claim.claimMinimumWidth) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ResizeClaimInsufficientArea, String.valueOf(newArea), String.valueOf(newClaimWidth), String.valueOf(newClaimHeight),
                            String.valueOf(activeConfig.getConfig().claim.claimMinimumArea));
                    GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                    return;
                }

                if (newArea < activeConfig.getConfig().claim.claimMinimumArea) {
                    if (newArea != 1) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.ResizeClaimInsufficientArea, String.valueOf(newArea), String.valueOf(newClaimWidth), String.valueOf(newClaimHeight),
                                String.valueOf(activeConfig.getConfig().claim.claimMinimumArea));
                    }

                    GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                    return;
                }
            }

            // if not an administrative claim, verify the player has enough claim blocks for this new claim
            if (playerData.shovelMode != ShovelMode.Admin) {
                int newClaimArea = newClaimWidth * newClaimHeight;
                int remainingBlocks = playerData.getRemainingClaimBlocks();
                if (newClaimArea > remainingBlocks) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CreateClaimInsufficientBlocks,
                            String.valueOf(newClaimArea - remainingBlocks));
                    this.tryAdvertiseAdminAlternatives(player);
                    GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                    return;
                }
            } else {
                playerID = null;
            }

            Vector3i lesserBoundary = new Vector3i(
                    lastShovelLocation.getBlockX(),
                    lastShovelLocation.getBlockY() - (playerData.getCuboidMode() ? 0 : activeConfig.getConfig().claim.extendIntoGroundDistance),
                    lastShovelLocation.getBlockZ());
            Vector3i greaterBoundary = new Vector3i(
                    clickedBlock.getPosition().getX(),
                    clickedBlock.getPosition().getY() - (playerData.getCuboidMode() ? 0 : activeConfig.getConfig().claim.extendIntoGroundDistance),
                    clickedBlock.getPosition().getZ());
            // try to create a new claim
            ClaimResult result = this.dataStore.createClaim(
                    player.getWorld(),
                    lesserBoundary,
                    greaterBoundary,
                    PlayerUtils.getClaimTypeFromShovel(playerData.shovelMode), player.getUniqueId(), playerData.getCuboidMode());

            GPClaim gpClaim = (GPClaim) result.getClaim().orElse(null);
            // if it didn't succeed, tell the player why
            if (!result.successful()) {
                if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);
                    gpClaim.getVisualizer().createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                    gpClaim.getVisualizer().apply(player);
                } else if (result.getResultType() == ClaimResultType.EXCEEDS_MAX_SIZE_X) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, "Claim exceeds your size X limit of " + playerData.optionMaxClaimSizeX + " blocks.");
                } else if (result.getResultType() == ClaimResultType.EXCEEDS_MAX_SIZE_Y) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, "Claim exceeds your size Y limit of " + playerData.optionMaxClaimSizeY + " blocks.");
                } else if (result.getResultType() == ClaimResultType.EXCEEDS_MAX_SIZE_Z) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, "Claim exceeds your size Z limit of " + playerData.optionMaxClaimSizeZ + " blocks.");
                }
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // otherwise, advise him on the /trust command and show him his new claim
            else {
                GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);
                gpClaim.getVisualizer().createClaimBlockVisuals(clickedBlock.getPosition().getY(), player.getLocation(), playerData);
                gpClaim.getVisualizer().apply(player);
                playerData.lastShovelLocation = null;

                // if it's a big claim, tell the player about subdivisions
                if (!player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && gpClaim.getArea() >= 1000) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.BecomeMayor, 200L);
                    GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, 201L);
                }

                // auto-extend it downward to cover anything already built underground
                Location<World> lesserCorner = gpClaim.getLesserBoundaryCorner();
                Location<World> greaterCorner = gpClaim.getGreaterBoundaryCorner();
                World world = lesserCorner.getExtent();
                ArrayList<Location<Chunk>> snapshots = new ArrayList<>();
                for (int chunkx = lesserCorner.getBlockX() >> 4; chunkx <= greaterCorner.getBlockX() >> 4; chunkx++) {
                    for (int chunkz = lesserCorner.getBlockZ() >> 4; chunkz <= greaterCorner.getBlockZ() >> 4; chunkz++) {
                        Optional<Chunk> chunk = world.getChunk(chunkx, 0, chunkz);
                        if (chunk.isPresent()) {
                            snapshots.add(new Location<Chunk>(chunk.get(), chunkx << 4, 0, chunkz << 4)); // need to use block coords for Location
                        }
                    }
                }

                Sponge.getGame().getScheduler().createTaskBuilder().async().execute(new AutoExtendClaimTask(gpClaim, snapshots, world.getDimension().getType())).submit(GriefPreventionPlugin.instance);
            }
        }
        GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
    }

    // helper methods for player events
    private boolean investigateClaim(Player player, BlockSnapshot clickedBlock, Optional<ItemStack> itemInHand) {
        GPTimings.PLAYER_INVESTIGATE_CLAIM.startTimingIfSync();

        // if he's investigating a claim
        if (!itemInHand.isPresent() || itemInHand.get().getItem() != GriefPreventionPlugin.instance.investigationTool) {
            GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
            return false;
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        // if holding shift (sneaking), show all claims in area

        GPClaim claim = null;
        if (!clickedBlock.getLocation().isPresent()) {
            claim = this.findNearbyClaim(player);
            // if holding shift (sneaking), show all claims in area
            if (player.get(Keys.IS_SNEAKING).get()) {
                if (!playerData.canIgnoreClaim(claim) && !player.hasPermission(GPPermissions.VISUALIZE_CLAIMS_NEARBY)) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, "You do not have permission to visualize nearby claims.");
                    GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                    return false;
                }

                // find nearby claims
                Location<World> nearbyLocation = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation : player.getLocation();
                Set<GPClaim> claims = this.dataStore.getNearbyClaims(nearbyLocation);
                int height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY();
                Visualization visualization = Visualization.fromClaims(claims, playerData.getCuboidMode() ? height : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), player.getLocation(), playerData);
                visualization.apply(player);

                GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.ShowNearbyClaims, String.valueOf(claims.size()));
                GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return true;
            }
            if (claim != null && claim.isWildernessClaim()) {
                playerData.lastValidInspectLocation = null;
                GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return false;
            }
        } else {
            claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get());
            if (claim.isWildernessClaim()) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.BlockNotClaimed);
                GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return false;
            }
        }

        // claim case
        GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.BlockClaimed, claim.getOwnerName());
        // visualize boundary
        if (claim.id != playerData.visualClaimId) {
            int height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : clickedBlock.getLocation().get().getBlockY();
            claim.getVisualizer().createClaimBlockVisuals(playerData.getCuboidMode() ? height : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), player.getLocation(), playerData);
            claim.getVisualizer().apply(player);
        }

        // if can resize this claim, tell about the boundaries
        if (claim.allowEdit(player) == null) {
            // TODO
            //GriefPrevention.sendMessage(player, TextMode.Info, "", "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
        }

        // if deleteclaims permission, show last active claim date
        if (!claim.isAdminClaim() && player.hasPermission(GPPermissions.COMMAND_DELETE_CLAIMS)) {
            Date lastActive = null;
            try {
                Instant instant = claim.getInternalClaimData().getDateLastActive();
                lastActive = Date.from(instant);
            } catch(DateTimeParseException ex) {
                // ignore
            }

            GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.ClaimLastActive, lastActive != null ? lastActive.toString() : "Unknown");

            // drop the data we just loaded, if the player isn't online
            if (!Sponge.getGame().getServer().getPlayer(claim.getOwnerUniqueId()).isPresent()) {
                this.dataStore.clearCachedPlayerData(claim.world.getProperties(), claim.getOwnerUniqueId());
            }
        }
        GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
        return true;
    }

    private GPClaim findNearbyClaim(Player player) {
        int maxDistance = GriefPreventionPlugin.instance.maxInspectionDistance;
        BlockRay<World> blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = null;
        int count = 0;
        while (blockRay.hasNext()) {
            BlockRayHit<World> blockRayHit = blockRay.next();
            Location<World> location = blockRayHit.getLocation();
            claim = this.dataStore.getClaimAt(location, false, null);
            if (claim != null && !claim.isWildernessClaim() && (playerData.visualBlocks == null || (claim.id != playerData.visualClaimId))) {
                playerData.lastValidInspectLocation = location;
                return claim;
            }

            BlockType blockType = location.getBlockType();
            if (blockType != BlockTypes.AIR && blockType != BlockTypes.TALLGRASS) {
                break;
            }
            count++;
        }

        if (count == maxDistance) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.TooFarAway);
        } else if (claim != null && claim.isWildernessClaim()){
            GriefPreventionPlugin.sendMessage(player, TextMode.Info, Messages.BlockNotClaimed);
        }

        return claim;
    }

    private BlockSnapshot getTargetBlock(Player player, int maxDistance) throws IllegalStateException {
        BlockRay<World> blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();

        while (blockRay.hasNext()) {
            BlockRayHit<World> blockRayHit = blockRay.next();
            if (blockRayHit.getLocation().getBlockType() != BlockTypes.AIR &&
                blockRayHit.getLocation().getBlockType() != BlockTypes.TALLGRASS) {
                    return blockRayHit.getLocation().createSnapshot();
            }
        }

        return null;
    }
}
