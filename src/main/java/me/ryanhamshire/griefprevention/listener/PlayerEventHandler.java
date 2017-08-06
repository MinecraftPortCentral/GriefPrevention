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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.api.chat.NucleusChatChannel;
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
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.MessageStorage;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.permission.GPOptionHandler;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.provider.NucleusApiProvider;
import me.ryanhamshire.griefprevention.provider.WorldEditApiProvider;
import me.ryanhamshire.griefprevention.task.PlayerKickBanTask;
import me.ryanhamshire.griefprevention.task.WelcomeTask;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import me.ryanhamshire.griefprevention.visual.Visualization;
import me.ryanhamshire.griefprevention.visual.VisualizationType;
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
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
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
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
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
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.entity.SpongeEntityType;
import org.spongepowered.common.interfaces.entity.IMixinEntity;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PlayerEventHandler {

    private final DataStore dataStore;
    private final WorldEditApiProvider worldEditProvider;

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
        this.worldEditProvider = GriefPreventionPlugin.instance.worldEditProvider;
    }

    // when a player chats, monitor for spam
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerChat(MessageChannelEvent.Chat event, @First Player player) {
        GPTimings.PLAYER_CHAT_EVENT.startTimingIfSync();
        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties());
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
            return;
        }

        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.inTown && playerData.townChat) {
            final MessageChannel channel = event.getChannel().orElse(null);
            if (GriefPreventionPlugin.instance.nucleusApiProvider != null && channel != null) {
                if (channel instanceof NucleusChatChannel) {
                    return;
                }
            }
            final GPClaim sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
            final GPClaim sourceTown = sourceClaim.getTownClaim();
            final Text townTag = sourceTown.getTownData().getTownTag().orElse(null);

            Text header = event.getFormatter().getHeader().toText();
            Text body = event.getFormatter().getBody().toText();
            Text footer = event.getFormatter().getFooter().toText();
            Text townMessage = Text.of(TextColors.GREEN, body);
            if (townTag != null) {
                townMessage = Text.of(townTag, townMessage);
            }
            event.setMessage(townMessage);
            Set<CommandSource> recipientsToRemove = new HashSet<>();
            Iterator<MessageReceiver> iterator = event.getChannel().get().getMembers().iterator();
            while (iterator.hasNext()) {
                MessageReceiver receiver = iterator.next();
                if (receiver instanceof Player) {
                    Player recipient = (Player) receiver;
                    if (GriefPreventionPlugin.instance.nucleusApiProvider != null) {
                        if (NucleusApiProvider.getPrivateMessagingService().isPresent() && NucleusApiProvider.getPrivateMessagingService().get().isSocialSpy(recipient)) {
                            // always allow social spy users
                            continue;
                        }
                    }

                    final GPPlayerData targetPlayerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(recipient.getWorld(), recipient.getUniqueId());
                    if (!targetPlayerData.inTown) {
                        recipientsToRemove.add(recipient);
                        continue;
                    }

                    final GPClaim targetClaim = this.dataStore.getClaimAtPlayer(targetPlayerData, recipient.getLocation());
                    final GPClaim targetTown = targetClaim.getTownClaim();
                    if (targetPlayerData.canIgnoreClaim(targetClaim)) {
                        continue;
                    }
                    if (sourceTown != null && (targetTown == null || !sourceTown.getUniqueId().equals(targetTown.getUniqueId()))) {
                        recipientsToRemove.add(recipient);
                    }
                }
            }

            if (!recipientsToRemove.isEmpty()) {
                Set<MessageReceiver> newRecipients = Sets.newHashSet(event.getChannel().get().getMembers().iterator());
                newRecipients.removeAll(recipientsToRemove);
                event.setChannel(new FixedMessageChannel(newRecipients));
            }
        }

        if (!activeConfig.getConfig().general.chatProtectionEnabled) {
            GPTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
            return;
        }

        if (!player.isOnline()) {
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
            /*Optional<AchievementData> data = player.get(AchievementData.class);
            if (data.isPresent() && player.getAchievementData().achievements().contains(Achievements.MINE_WOOD)) {
                if (!playerData.profanityWarned) {
                    playerData.profanityWarned = true;
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.playerNoProfanity.toText());
                    event.setCancelled(true);
                    GPTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
                    return;
                }
            }*/

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
            Set<CommandSource> recipientsToRemove = new HashSet<>();
            // based on ignore lists, remove some of the audience
            for (MessageReceiver recipient : recipients) {
                if (recipient instanceof Player) {
                    Player receiver = (Player) recipient;

                    if (playerData.ignoredPlayers.containsKey(receiver.getUniqueId())) {
                        recipientsToRemove.add(receiver);
                    } else {
                        GPPlayerData targetPlayerData = this.dataStore.getOrCreatePlayerData(receiver.getWorld(), receiver.getUniqueId());
                        if (targetPlayerData.ignoredPlayers.containsKey(player.getUniqueId())) {
                            recipientsToRemove.add(receiver);
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
            this.howToClaimPattern = Pattern.compile(GriefPreventionPlugin.instance.messageData.chatHowToClaimRegex.toText().toPlain(), Pattern.CASE_INSENSITIVE);
        }

        if (this.howToClaimPattern.matcher(message).matches()) {
            if (GriefPreventionPlugin.instance.claimModeIsActive(player.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlCreativeBasics.toText(), 10L);
            } else {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlSurvivalBasics.toText(), 10L);
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
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.playerNoChatUntilMove.toText(), 10L);
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
                            new PlayerKickBanTask(player, GriefPreventionPlugin.instance.messageData.banMessage.toText(), "GriefPrevention Anti-Spam", true);
                    Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1).execute(task).submit(GriefPreventionPlugin.instance);
                } else {
                    // log entry
                    GriefPreventionPlugin.addLogEntry("Kicking " + player.getName() + " for spam.", CustomLogEntryTypes.AdminActivity);

                    // just kick
                    PlayerKickBanTask task = new PlayerKickBanTask(player, Text.of(""), "GriefPrevention Anti-Spam", false);
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
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.warningBanMessage.toText(), 10L);
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
    @Listener(order = Order.FIRST, beforeModifications = true)
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
            if (GPPermissionHandler.getClaimPermission(event, player.getLocation(), claim, GPPermissions.COMMAND_EXECUTE, null, commandPermission + argument, player) == Tristate.FALSE) {
                final Text denyMessage = GriefPreventionPlugin.instance.messageData.commandBlocked
                        .apply(ImmutableMap.of(
                        "command", command,
                        "owner", claim.getOwnerName())).build();
                GriefPreventionPlugin.sendMessage(player, denyMessage);
                event.setCancelled(true);
                GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                return;
            } else if (playerData != null && (playerData.inPvpCombat(player.getWorld())) && GPPermissionHandler.getClaimPermission(event, player.getLocation(), claim, GPPermissions.COMMAND_EXECUTE_PVP, null, commandPermission + argument, player) == Tristate.FALSE) {
                final Text denyMessage = GriefPreventionPlugin.instance.messageData.pvpCommandBanned
                        .apply(ImmutableMap.of(
                        "command", command)).build();
                GriefPreventionPlugin.sendMessage(event.getCause().first(Player.class).get(), denyMessage);
                event.setCancelled(true);
                GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                return;
            }
        }
        // second check the full command
        if (GPPermissionHandler.getClaimPermission(event, player.getLocation(), claim, GPPermissions.COMMAND_EXECUTE, null, commandPermission, player) == Tristate.FALSE) {
            final Text denyMessage = GriefPreventionPlugin.instance.messageData.commandBlocked
                    .apply(ImmutableMap.of(
                    "command", command,
                    "owner", claim.getOwnerName())).build();
            GriefPreventionPlugin.sendMessage(player, denyMessage);
            event.setCancelled(true);
            GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        } else if (playerData != null && (playerData.inPvpCombat(player.getWorld())) && GPPermissionHandler.getClaimPermission(event, player.getLocation(), claim, GPPermissions.COMMAND_EXECUTE_PVP, null, commandPermission, player) == Tristate.FALSE) {
            final Text denyMessage = GriefPreventionPlugin.instance.messageData.pvpCommandBanned
                    .apply(ImmutableMap.of(
                    "command", command)).build();
            GriefPreventionPlugin.sendMessage(event.getCause().first(Player.class).get(), denyMessage);
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
                    event.setCancelled(true);
                    GPTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                    return;
                }

                GPPlayerData targetPlayerData = this.dataStore.getOrCreatePlayerData(targetPlayer.getWorld(), targetPlayer.getUniqueId());
                if (targetPlayerData.ignoredPlayers.containsKey(player.getUniqueId())) {
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
                if (!claim.isUserTrusted(player, TrustType.ACCESSOR)) {
                    //GriefPreventionPlugin.sendMessage(player, reason);
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
    @Listener(order = Order.FIRST, beforeModifications = true)
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
        final WorldProperties worldProperties = event.getToTransform().getExtent().getProperties();
        final UUID playerUniqueId = player.getUniqueId();
        final GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(worldProperties, playerUniqueId);
        playerData.receivedDropUnlockAdvertisement = false;
        playerData.ipAddress = event.getConnection().getAddress().getAddress();
        final GPClaimManager claimWorldManager = this.dataStore.getClaimWorldManager(worldProperties);
        final Instant dateNow = Instant.now();
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.getType() != ClaimType.ADMIN && claim.getOwnerUniqueId().equals(playerUniqueId)) {
                // update lastActive timestamp for claim
                claim.getData().setDateLastActive(dateNow);
                // update timestamps for subdivisions
                for (Claim subdivision : ((GPClaim) claim).children) {
                    subdivision.getData().setDateLastActive(dateNow);
                }
                ((GPClaim) claim).getInternalClaimData().setRequiresSave(true);
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
        final GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), playerID);
        playerData.lastSpawn = now;
        final GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (claim.isInTown()) {
            playerData.inTown = true;
        }

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
                                new PlayerKickBanTask(player, Text.of(""), "GriefPrevention Smart Ban - Shared Login:" + info.bannedAccountName, true);
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
        if (this.worldEditProvider != null) {
            this.worldEditProvider.revertVisuals(player, playerData, null);
            this.worldEditProvider.removePlayer(player);
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
    @Listener(order = Order.FIRST, beforeModifications = true)
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
        final World world = entity.getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        // in creative worlds, dropping items is blocked
        if (GriefPreventionPlugin.instance.claimModeIsActive(world.getProperties(), ClaimsMode.Creative)) {
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
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.pvpNoItemDrop.toText());
            event.setCancelled(true);
            GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        for (Entity entityItem : event.getEntities()) {
            Location<World> location = entityItem.getLocation();
            GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
            if (claim != null) {
                // check for bans first
                Tristate override = GPPermissionHandler.getFlagOverride(event, location, GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(claim.getWorld().getProperties()).getWildernessClaim(), GPPermissions.ITEM_DROP, source, entityItem);
                if (override != Tristate.UNDEFINED) {
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    if (override == Tristate.TRUE) {
                        return;
                    }
                    event.setCancelled(true);
                    return;
                }
                override = GPPermissionHandler.getFlagOverride(event, location, claim, GPPermissions.ITEM_DROP,  user, entityItem);
                if (override != Tristate.UNDEFINED) {
                    if (override == Tristate.TRUE) {
                        GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                        return;
                    }

                    event.setCancelled(true);
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                }

                // allow trusted users
                if (claim.isUserTrusted(user, TrustType.ACCESSOR)) {
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                }

                Tristate perm = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.ITEM_DROP, user, entityItem, user);
                if (perm == Tristate.TRUE) {
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                } else if (perm == Tristate.FALSE) {
                    event.setCancelled(true);
                    if (entity instanceof Player) {
                        GriefPreventionPlugin.sendClaimDenyMessage(claim, player, GriefPreventionPlugin.instance.messageData.permissionItemDrop.toText());
                    }
                    GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GPTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
    }

    // when a player interacts with an entity...
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractEntity(InteractEntityEvent.Primary event, @First Player player) {
        GPTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.startTimingIfSync();
        Entity targetEntity = event.getTargetEntity();

        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = targetEntity.getLocation();
        GPClaim claim = this.dataStore.getClaimAt(location);
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        if (playerData.canIgnoreClaim(claim) || playerData.lastInteractItemBlockResult == Tristate.TRUE || playerData.lastInteractItemEntityResult == Tristate.TRUE) {
            GPTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }

        Tristate result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_ENTITY_PRIMARY, player, targetEntity, player, TrustType.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            final Text message = GriefPreventionPlugin.instance.messageData.claimProtectedEntity
                    .apply(ImmutableMap.of(
                    "owner", claim.getOwnerName())).build();
            GriefPreventionPlugin.sendMessage(player, message);
            event.setCancelled(true);
            GPTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }
    }

    // when a player interacts with an entity...
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractEntity(InteractEntityEvent.Secondary event, @First Player player) {
        GPTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.startTimingIfSync();
        Entity targetEntity = event.getTargetEntity();

        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = targetEntity.getLocation();
        GPClaim claim = this.dataStore.getClaimAt(location);
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.canIgnoreClaim(claim) || playerData.lastInteractItemBlockResult == Tristate.TRUE || playerData.lastInteractItemEntityResult == Tristate.TRUE) {
            GPTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }

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
                        final Text message = GriefPreventionPlugin.instance.messageData.townName
                                .apply(ImmutableMap.of(
                                "type", spongeEntity.getType().getId())).build();
                        GriefPreventionPlugin.sendMessage(player, message);
                        playerData.petGiveawayRecipient = null;
                        GPTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
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
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandPetConfirmation.toText());
                    event.setCancelled(true);
                }
                GPTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }
        }

        if (playerData.canIgnoreClaim(claim)) {
            GPTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }

        final Tristate result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_ENTITY_SECONDARY, player, targetEntity, player, TrustType.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            String entityId = targetEntity.getType() != null ? targetEntity.getType().getId() : ((net.minecraft.entity.Entity) targetEntity).getName();
            Text message = null;
            if (!(targetEntity instanceof Player)) {
                message = GriefPreventionPlugin.instance.messageData.permissionInteractEntity
                        .apply(ImmutableMap.of(
                        "owner", claim.getOwnerName(),
                        "entity", entityId)).build();
                GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
            }

            event.setCancelled(true);
            GPTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractItem(InteractItemEvent.Primary event, @Root Player player) {
        final World world = player.getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            return;
        }

        final BlockSnapshot blockSnapshot = event.getCause().get(NamedCause.HIT_TARGET, BlockSnapshot.class).orElse(BlockSnapshot.NONE);
        final GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        final Vector3d interactPoint = event.getInteractionPoint().orElse(null);
        final Entity entity = event.getCause().get(NamedCause.HIT_TARGET, Entity.class).orElse(null);
        final Location<World> location = entity != null ? entity.getLocation() : interactPoint != null ? new Location<World>(world, interactPoint) : player.getLocation();
        final GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
        final ItemType playerItem = event.getItemStack().getType();
        if (playerItem == ItemTypes.NONE && blockSnapshot == null && entity == null) {
            return;
        }

        final Tristate result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_ITEM_PRIMARY, player, event.getItemStack(), player, TrustType.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (entity != null) {
                playerData.lastInteractItemEntityResult = Tristate.FALSE;
            } else if (blockSnapshot != null) {
                playerData.lastInteractItemBlockResult = Tristate.FALSE;
            }
        }

        // cache primary item checks
        if (entity != null) {
            playerData.lastInteractItemEntityResult = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_ENTITY_PRIMARY, playerItem, entity, player, TrustType.BUILDER, true);
        } else {
            playerData.lastInteractItemEntityResult = Tristate.UNDEFINED;
        }
        if (blockSnapshot != null && blockSnapshot != BlockSnapshot.NONE) {
            playerData.lastInteractItemBlockResult = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_BLOCK_PRIMARY, playerItem, blockSnapshot, player, TrustType.BUILDER, true);
        } else {
            playerData.lastInteractItemBlockResult = Tristate.UNDEFINED;
        }

        if (playerData.lastInteractItemEntityResult == Tristate.FALSE || playerData.lastInteractItemBlockResult == Tristate.FALSE) {
            if (playerData.lastInteractItemEntityResult == Tristate.FALSE) {
                if (playerItem == ItemTypes.NONE) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractEntity
                            .apply(ImmutableMap.of(
                            "owner", claim.getOwnerName(),
                            "entity", entity.getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                } else {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractItemEntity
                            .apply(ImmutableMap.of(
                            "item", event.getItemStack().getType().getId(),
                            "entity", entity.getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                }
            } else {
                if (playerItem == ItemTypes.NONE) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractBlock
                            .apply(ImmutableMap.of(
                            "owner", claim.getOwnerName(),
                            "block", blockSnapshot.getState().getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                } else {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractItemBlock
                            .apply(ImmutableMap.of(
                            "item", event.getItemStack().getType().getId(),
                            "block", blockSnapshot.getState().getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                }
            }
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractItem(InteractItemEvent.Secondary event, @Root Player player) {
        final World world = player.getWorld();
        final ItemType playerItem = event.getItemStack().getType();
        if (playerItem instanceof ItemFood || !GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            return;
        }

        final ItemStack itemInHand = player.getItemInHand(event.getHandType()).orElse(null);
        final BlockSnapshot blockSnapshot = event.getCause().get(NamedCause.HIT_TARGET, BlockSnapshot.class).orElse(BlockSnapshot.NONE);
        if (investigateClaim(player, blockSnapshot, itemInHand)) {
            return;
        }

        final GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        final Vector3d interactPoint = event.getInteractionPoint().orElse(null);
        final Entity entity = event.getCause().get(NamedCause.HIT_TARGET, Entity.class).orElse(null);
        final Location<World> location = entity != null ? entity.getLocation() : interactPoint != null ? new Location<World>(world, interactPoint) : player.getLocation();
        final GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
        if (playerItem == ItemTypes.NONE && blockSnapshot == null && entity == null) {
            return;
        }

        Tristate result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_ITEM_SECONDARY, player, playerItem, player, TrustType.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            if (entity != null) {
                playerData.lastInteractItemEntityResult = Tristate.FALSE;
            } else if (blockSnapshot != null) {
                playerData.lastInteractItemBlockResult = Tristate.FALSE;
            }
        }

        // cache secondary item checks
        if (entity != null) {
            playerData.lastInteractItemEntityResult = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_ENTITY_SECONDARY, playerItem, entity, player, TrustType.ACCESSOR, true);
        } else {
            playerData.lastInteractItemEntityResult = Tristate.UNDEFINED;
        }
        if (blockSnapshot != null && blockSnapshot != BlockSnapshot.NONE) {
            // check user trust
            if (location.hasTileEntity()) {
                if (claim.isUserTrusted(player, TrustType.CONTAINER)) {
                    playerData.lastInteractItemBlockResult = Tristate.TRUE;
                    return;
                }
            } else {
                if (claim.isUserTrusted(player, TrustType.ACCESSOR)) {
                    playerData.lastInteractItemBlockResult = Tristate.TRUE;
                    return;
                }
            }
            final TrustType trustType = location.hasTileEntity() ? TrustType.CONTAINER : TrustType.ACCESSOR;
            result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_BLOCK_SECONDARY, playerItem, blockSnapshot, player, trustType, true);
            if (result == Tristate.FALSE && playerItem.getBlock().isPresent()) {
                result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.BLOCK_PLACE, playerItem, playerItem.getBlock().get(), player, trustType, true);
            }
            playerData.lastInteractItemBlockResult = result;
        } else {
            playerData.lastInteractItemBlockResult = Tristate.UNDEFINED;
        }

        if (playerData.lastInteractItemEntityResult == Tristate.FALSE || playerData.lastInteractItemBlockResult == Tristate.FALSE) {
            if (blockSnapshot != BlockSnapshot.NONE) {
                TileEntity tileEntity = world.getTileEntity(location.getBlockPosition()).orElse(null);
                if (tileEntity != null) {
                    ((EntityPlayerMP) player).closeScreen();
                }
            }

            if (playerData.lastInteractItemEntityResult == Tristate.FALSE) {
                if (playerItem == ItemTypes.NONE) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractEntity
                            .apply(ImmutableMap.of(
                            "owner", claim.getOwnerName(),
                            "entity", entity.getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                } else {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractItemEntity
                            .apply(ImmutableMap.of(
                            "item", event.getItemStack().getType().getId(),
                            "entity", entity.getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                }
            } else {
                if (playerItem == ItemTypes.NONE) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractBlock
                            .apply(ImmutableMap.of(
                            "owner", claim.getOwnerName(),
                            "block", blockSnapshot.getState().getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                } else {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionInteractItemBlock
                            .apply(ImmutableMap.of(
                            "item", event.getItemStack().getType().getId(),
                            "block", blockSnapshot.getState().getType().getId())).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
                }
            }
            event.setCancelled(true);
            return;
        }
    }

    // when a player picks up an item...
    @Listener(order = Order.LAST, beforeModifications = true)
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
        if (GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.ITEM_PICKUP, player, event.getTargetEntity(), player, true) == Tristate.FALSE) {
            event.setCancelled(true);
            GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
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
                        event.setCancelled(true);
                        GPTimings.PLAYER_PICKUP_ITEM_EVENT.stopTimingIfSync();
                        return;
                    }
    
                    // otherwise take away his immunity. he may be armed now. at least, he's worth killing for some loot
                    playerData.pvpImmune = false;
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.pvpImmunityEnd.toText());
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
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimModeBasic.toText());
                }

                // tell him how many claim blocks he has available
                final Text message = GriefPreventionPlugin.instance.messageData.playerRemainingBlocks
                        .apply(ImmutableMap.of(
                        "remaining-chunks", playerData.getRemainingChunks(),
                        "remaining-blocks", playerData.getRemainingClaimBlocks())).build();
                GriefPreventionPlugin.sendMessage(player, message);

                // link to a video demo of land claiming, based on world type
                if (player.hasPermission(GPPermissions.CLAIM_SHOW_TUTORIAL)) {
                    if (GriefPreventionPlugin.instance.claimModeIsActive(player.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
                        GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlCreativeBasics.toText());
                    } else if (GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getLocation().getExtent().getProperties())) {
                        GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlSurvivalBasics.toText());
                    }
                }
            } else {
                if (playerData.lastShovelLocation != null) {
                    playerData.revertActiveVisual(player);
                    // check for any active WECUI visuals
                    if (GriefPreventionPlugin.instance.worldEditProvider != null) {
                        GriefPreventionPlugin.instance.worldEditProvider.revertVisuals(player, playerData, null);
                    }
                }
                playerData.lastShovelLocation = null;
                playerData.endShovelLocation = null;
                playerData.claimResizing = null;
            }
            count++;
        }
        GPTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerUseItem(UseItemStackEvent.Start event, @First Player player) {
        GPTimings.PLAYER_USE_ITEM_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = player.getLocation();
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(location.getExtent(), player.getUniqueId());
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);

        final Tristate result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.ITEM_USE, player, event.getItemStackInUse().getType(), player, TrustType.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            if (GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.ITEM_USE, player, event.getItemStackInUse().getType(), player) == Tristate.TRUE) {
                GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
                return;
            }

            final Text message = GriefPreventionPlugin.instance.messageData.townName
                    .apply(ImmutableMap.of(
                    "item", event.getItemStackInUse().getType().getId())).build();
            GriefPreventionPlugin.sendClaimDenyMessage(claim, player,  message);
            event.setCancelled(true);
        }
        GPTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
    }

    // educates a player about /adminclaims and /acb, if he can use them
    private void tryAdvertiseAdminAlternatives(Player player) {
        if (player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && player.hasPermission(GPPermissions.COMMAND_ADJUST_CLAIM_BLOCKS)) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.advertiseAcAndAcb.toText());
        } else if (player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.advertiseAdminClaims.toText());
        } else if (player.hasPermission(GPPermissions.COMMAND_ADJUST_CLAIM_BLOCKS)) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.advertiseAcb.toText());
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
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
        if (playerData.lastInteractItemBlockResult == Tristate.TRUE || playerData.lastInteractItemEntityResult == Tristate.TRUE) {
            GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }
        final GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location, false);
        final Tristate result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_BLOCK_PRIMARY, player, clickedBlock.getState(), player, true);
        if (result == Tristate.FALSE) {
            if (GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.BLOCK_BREAK, player, clickedBlock.getState(), player) == Tristate.TRUE) {
                GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
                playerData.setLastInteractData(claim);
                return;
            }

            final Text message = GriefPreventionPlugin.instance.messageData.permissionAccess
                    .apply(ImmutableMap.of(
                    "player", Text.of(claim.getOwnerName()))).build();

            GriefPreventionPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
            GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }
        playerData.setLastInteractData(claim);
        GPTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
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
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Location<World> location = clickedBlock.getLocation().orElse(null);

        if (location == null) {
            onPlayerHandleShovelAction(event, event.getTargetBlock(), player, handType, playerData);
            GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }
        if (playerData.lastInteractItemBlockResult == Tristate.TRUE || playerData.lastInteractItemEntityResult == Tristate.TRUE) {
            if (itemInHand != null && (itemInHand.getItem().equals(GriefPreventionPlugin.instance.modificationTool))) {
                onPlayerHandleShovelAction(event, event.getTargetBlock(), player, handType, playerData);
                // avoid changing blocks after using a shovel
                event.setCancelled(true);
            }
            GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }

        GPClaim playerClaim = this.dataStore.getClaimAtPlayer(playerData, location, false);
        if (playerData != null && !playerData.canIgnoreClaim(playerClaim)) {
            Tristate result = GPPermissionHandler.getClaimPermission(event, location, playerClaim, GPPermissions.INTERACT_BLOCK_SECONDARY, player, event.getTargetBlock(), player, true);
            if (result == Tristate.FALSE) {
                // if player is holding an item, check if it can be placed
                if (itemInHand != null) {
                    if (GPPermissionHandler.getClaimPermission(event, location, playerClaim, GPPermissions.BLOCK_PLACE, player, itemInHand, player) == Tristate.TRUE) {
                        GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                        playerData.setLastInteractData(playerClaim);
                        return;
                    }
                }
                // Don't send a deny message if the player is holding an investigation tool
                if (!PlayerUtils.hasItemInOneHand(player, GriefPreventionPlugin.instance.investigationTool)) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionAccess
                            .apply(ImmutableMap.of(
                            "player", Text.of(playerClaim.getOwnerName())
                    )).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(playerClaim, player, message);
                }
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }
        }

        // apply rules for containers
        TileEntity tileEntity = clickedBlock.getLocation().get().getTileEntity().orElse(null);
        if (tileEntity != null && tileEntity instanceof IInventory) {
            // block container use during pvp combat, same reason
            if (playerData.inPvpCombat(player.getWorld())) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.pvpNoContainers.toText());
                event.setCancelled(true);
                GPTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }

            // if the event hasn't been cancelled, then the player is allowed to use the container so drop any pvp protection
            if (playerData.pvpImmune) {
                playerData.pvpImmune = false;
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.pvpImmunityEnd.toText());
            }
        }
        // otherwise handle right click (shovel, string, bonemeal)
        else if (itemInHand != null && (itemInHand.getItem() == GriefPreventionPlugin.instance.modificationTool)) {
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
        Location<World> location = clickedBlock.getLocation().orElse(null);

        // FEATURE: shovel and stick can be used from a distance away
        if (clickedBlock.getState().getType() == BlockTypes.AIR) {
            // try to find a far away non-air block along line of sight
            boolean ignoreAir = false;
            if (this.worldEditProvider != null) {
                // Ignore air so players can use client-side WECUI block target which uses max reach distance
                if (this.worldEditProvider.hasCUISupport(player) && playerData.getCuboidMode() && playerData.lastShovelLocation != null) {
                    ignoreAir = true;
                }
            }
            final int distance = !ignoreAir ? 100 : (int) SpongeImplHooks.getBlockReachDistance((EntityPlayerMP) player);
            location = BlockUtils.getTargetBlock(player, playerData, distance, ignoreAir).orElse(null);
        }

        // if no block, stop here
        if (location == null) {
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // can't use the shovel from too far away
        /*if (location.getBlock().getType() == BlockTypes.AIR) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimTooFar.toText());
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }*/

        // if the player is in restore nature mode, do only that
        UUID playerID = player.getUniqueId();
        playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.shovelMode == ShovelMode.RestoreNature || playerData.shovelMode == ShovelMode.RestoreNatureAggressive) {
            if (true) {
                player.sendMessage(Text.of(TextColors.RED, "This mode is currently disabled until further notice."));
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // if the clicked block is in a claim, visualize that claim and deliver an error message
            GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);
            if (!claim.isWilderness()) {
                final Text message = GriefPreventionPlugin.instance.messageData.blockClaimed
                        .apply(ImmutableMap.of(
                        "owner", claim.getOwnerName())).build();
                GriefPreventionPlugin.sendMessage(player, message);
                Visualization visualization = new Visualization(claim, VisualizationType.ERROR);
                visualization.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                visualization.apply(player);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // figure out which chunk to repair
            Chunk chunk = player.getWorld().getChunk(location.getBlockX() >> 4, 0, location.getBlockZ() >> 4).get();

            // start the repair process

            // set boundaries for processing
            int miny = location.getBlockY();

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
            DimensionType environment = location.getExtent().getDimension().getType();
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

            int maxHeight = location.getBlockY();
            int minx = location.getBlockX() - playerData.fillRadius;
            int maxx = location.getBlockX() + playerData.fillRadius;
            int minz = location.getBlockZ() - playerData.fillRadius;
            int maxz = location.getBlockZ() + playerData.fillRadius;
            int minHeight = maxHeight - 10;
            if (minHeight < 0)
                minHeight = 0;

            for (int x = minx; x <= maxx; x++) {
                for (int z = minz; z <= maxz; z++) {
                    // circular brush
                    Location<World> location1 = new Location<World>(location.getExtent(), x, location.getBlockY(), z);
                    if (location1.getPosition().distance(location.getPosition()) > playerData.fillRadius) {
                        continue;
                    }

                    // default fill block is initially the first from the
                    // allowed fill blocks list above
                    BlockType defaultFiller = allowedFillBlocks.get(0);

                    // prefer to use the block the player clicked on, if
                    // it's an acceptable fill block
                    if (allowedFillBlocks.contains(location.getBlock().getType())) {
                        defaultFiller = location.getBlock().getType();
                    }

                    // if the player clicks on water, try to sink through
                    // the water to find something underneath that's useful
                    // for a filler
                    else if (location.getBlock().getType() == BlockTypes.FLOWING_WATER || location.getBlock().getType() == BlockTypes.WATER) {
                        BlockType newBlockType = location.getBlock().getType();
                        while (newBlockType != BlockTypes.FLOWING_WATER && newBlockType != BlockTypes.WATER) {
                            newBlockType = location.getRelative(Direction.DOWN).getBlockType();
                        }
                        if (allowedFillBlocks.contains(newBlockType)) {
                            defaultFiller = newBlockType;
                        }
                    }

                    // fill bottom to top
                    for (int y = minHeight; y <= maxHeight; y++) {
                        BlockSnapshot block = location.getExtent().createSnapshot(x, y, z);

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
        if (!playerData.canCreateClaim(player, true)) {
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // if he's resizing a claim
        if (playerData.claimResizing != null) {
            if (location.equals(playerData.lastShovelLocation)) {
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            playerData.endShovelLocation = location;
            // figure out what the coords of his new claim would be
            int newx1, newx2, newz1, newz2, newy1, newy2;
            int smallX = 0, smallY = 0, smallZ = 0, bigX = 0, bigY = 0, bigZ = 0;

            if (playerData.claimResizing.isCuboid()) {
                newx1 = playerData.lastShovelLocation.getBlockX();
                newx2 = location.getBlockX();
                newy1 = playerData.lastShovelLocation.getBlockY();
                newy2 = location.getBlockY();
                newz1 = playerData.lastShovelLocation.getBlockZ();
                newz2 = location.getBlockZ();
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
            } else {
                if (playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getLesserBoundaryCorner().getBlockX()) {
                    newx1 = location.getBlockX();
                } else {
                    newx1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockX();
                }
    
                if (playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockX()) {
                    newx2 = location.getBlockX();
                } else {
                    newx2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockX();
                }
    
                if (playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getLesserBoundaryCorner().getBlockZ()) {
                    newz1 = location.getBlockZ();
                } else {
                    newz1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockZ();
                }
    
                if (playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ()) {
                    newz2 = location.getBlockZ();
                } else {
                    newz2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ();
                }
    
                newy1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockY();
                newy2 = playerData.getCuboidMode() ? location.getBlockY() : player.getWorld().getDimension().getBuildHeight() - 1;
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
                final double claimableChunks = claimBlocksRemaining / 65536.0;
                Map<String, ?> params = ImmutableMap.of(
                        "remaining-chunks", Math.round(claimableChunks * 100.0)/100.0, 
                        "remaining-blocks", claimBlocksRemaining);
                GriefPreventionPlugin.sendMessage(player, MessageStorage.CLAIM_RESIZE_SUCCESS, GriefPreventionPlugin.instance.messageData.claimResizeSuccess, params);
                playerData.revertActiveVisual(player);
                ((GPClaim) claim).getVisualizer().resetVisuals();
                ((GPClaim) claim).getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                ((GPClaim) claim).getVisualizer().apply(player);
                if (this.worldEditProvider != null) {
                    this.worldEditProvider.visualizeClaim(claim, player, playerData, false);
                }

                // if resizing someone else's claim, make a log entry
                if (!playerID.equals(claim.getOwnerUniqueId()) && !claim.getParent().isPresent()) {
                    GriefPreventionPlugin.addLogEntry(player.getName() + " resized " + claim.getOwnerName() + "'s claim at "
                            + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + ".");
                }

                // if increased to a sufficiently large size and no children yet, send children instructions
                if (oldClaim.getArea() < 1000 && claim.getArea() >= 1000 && claim.getChildren(false).isEmpty()
                        && !player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlSubdivisionBasics.toText(), 201L);
                }

                // if in a creative mode world and shrinking an existing claim, restore any unclaimed area
                if (smaller && GriefPreventionPlugin.instance.claimModeIsActive(oldClaim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCleanupWarning.toText());
                    GriefPreventionPlugin.instance.restoreClaim(oldClaim, 20L * 60 * 2); // 2 minutes
                    GriefPreventionPlugin.addLogEntry(player.getName() + " shrank a claim @ "
                            + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                }
            } else {
                if (claimResult.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                    GPClaim overlapClaim = (GPClaim) claimResult.getClaim().get();
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimResizeOverlap.toText());
                    List<Claim> claims = new ArrayList<>();
                    claims.add(overlapClaim);
                    CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                }
                event.setCancelled(true);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // otherwise, since not currently resizing a claim, must be starting
        // a resize, creating a new claim, town, or creating a subdivision

        GPClaim claim = this.dataStore.getClaimAt(location, true);
        // if within an existing claim, he's not creating a new one
        if (!claim.isWilderness()) {
            // if the player has permission to edit the claim or subdivision
            Text noEditReason = claim.allowEdit(player);
            if (noEditReason == null) {
                // if he clicked on a corner, start resizing it
                if (BlockUtils.clickedClaimCorner(claim, location.getBlockPosition())) {
                    boolean playerCanResize = true;
                    // players can always resize subdivisions
                    if (!claim.isSubdivision() && !player.hasPermission(GPPermissions.CLAIM_RESIZE) && claim.allowEdit(player) != null) {
                        if (claim.parent == null) {
                            if (claim.isTown()) {
                                playerCanResize = player.hasPermission(GPPermissions.CLAIM_RESIZE_TOWN);
                            } else if (claim.isAdminClaim()) {
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
                        GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionClaimResize.toText());
                        return;
                    }

                    playerData.revertActiveVisual(player);
                    playerData.claimResizing = claim;
                    playerData.lastShovelLocation = location;
                    if (GriefPreventionPlugin.instance.worldEditProvider != null) {
                        // get opposite corner
                        final int x = playerData.lastShovelLocation.getBlockX() == claim.lesserBoundaryCorner.getBlockX() ? claim.greaterBoundaryCorner.getBlockX() : claim.lesserBoundaryCorner.getBlockX();
                        final int y = playerData.lastShovelLocation.getBlockY() == claim.lesserBoundaryCorner.getBlockY() ? claim.greaterBoundaryCorner.getBlockY() : claim.lesserBoundaryCorner.getBlockY();
                        final int z = playerData.lastShovelLocation.getBlockZ() == claim.lesserBoundaryCorner.getBlockZ() ? claim.greaterBoundaryCorner.getBlockZ() : claim.lesserBoundaryCorner.getBlockZ();
                        GriefPreventionPlugin.instance.worldEditProvider.visualizeClaim(claim, new Vector3i(x, y, z), playerData.lastShovelLocation.getBlockPosition(), player, playerData, false);
                    }
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimResizeStart.toText());
                }

                // if he didn't click on a corner and is in subdivision
                // mode, he's creating a new subdivision
                else if ((playerData.shovelMode == ShovelMode.Subdivide 
                        || ((claim.isTown() || claim.isAdminClaim()) && (playerData.lastShovelLocation == null || playerData.claimSubdividing != null)) && playerData.shovelMode != ShovelMode.Town)) {
                    if (claim.getTownClaim() != null && playerData.shovelMode == ShovelMode.Town) {
                        GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlapShort.toText());
                        List<Claim> claims = new ArrayList<>();
                        claims.add(claim);
                        CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                        GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                        return;
                    }
                    // if it's the first click, he's trying to start a new subdivision
                    if (playerData.lastShovelLocation == null) {
                        // if the clicked claim was a subdivision, tell him
                        // he can't start a new subdivision here
                        if (claim.isSubdivision()) {
                            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimResizeOverlapSubdivision.toText());
                        }

                        // otherwise start a new subdivision
                        else {
                            final Text message = GriefPreventionPlugin.instance.messageData.claimStart
                                    .apply(ImmutableMap.of(
                                    "type", playerData.shovelMode.name())).build();
                            GriefPreventionPlugin.sendMessage(player, message);
                            playerData.lastShovelLocation = location;
                            playerData.claimSubdividing = claim;
                            Visualization visualization = Visualization.fromClick(location, location.getBlockY(), PlayerUtils.getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
                            visualization.apply(player, false);
                            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                            return;
                        }
                    }

                    // otherwise, he's trying to finish creating a
                    // subdivision by setting the other boundary corner
                    else if (playerData.claimSubdividing != null) {
                        // Validate second clicked corner is within claim being sub divided
                        final GPClaim clickedClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(location);
                        if (clickedClaim == null || !playerData.claimSubdividing.getUniqueId().equals(clickedClaim.getUniqueId())) {
                            if (clickedClaim != null) {
                                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlapShort.toText());
                                final GPClaim overlapClaim = playerData.claimSubdividing;
                                List<Claim> claims = new ArrayList<>();
                                claims.add(overlapClaim);
                                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                            }

                            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                            return;
                        }

                        Vector3i lesserBoundaryCorner = new Vector3i(playerData.lastShovelLocation.getBlockX(), 
                                playerData.getCuboidMode() ? playerData.lastShovelLocation.getBlockY() : 0,
                                playerData.lastShovelLocation.getBlockZ());
                        Vector3i greaterBoundaryCorner = new Vector3i(location.getBlockX(), 
                                playerData.getCuboidMode() ? location.getBlockY() : player.getWorld().getDimension().getBuildHeight() - 1,
                                        location.getBlockZ());

                        ClaimResult result = this.dataStore.createClaim(player.getWorld(),
                                lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtils.getClaimTypeFromShovel(playerData.shovelMode),
                                player.getUniqueId(), playerData.getCuboidMode(), playerData.claimSubdividing, Cause.of(NamedCause.source(player)));

                        GPClaim gpClaim = (GPClaim) result.getClaim().orElse(null);
                        // if it didn't succeed, tell the player why
                        if (!result.successful()) {
                            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlapShort.toText());
                                List<Claim> claims = new ArrayList<>();
                                claims.add(gpClaim);
                                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                            } else {
                                GriefPreventionPlugin.sendMessage(player, Text.of(TextColors.RED, "Unable able to create claim due to result " + result.getResultType()));
                            }
                            event.setCancelled(true);
                            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                            return;
                        }

                        // otherwise, advise him on the /trust command and show him his new subdivision
                        else {
                            playerData.lastShovelLocation = null;
                            playerData.claimSubdividing = null;
                            final Text message = GriefPreventionPlugin.instance.messageData.claimCreateSuccess
                                    .apply(ImmutableMap.of(
                                    "type", playerData.shovelMode.name())).build();
                            GriefPreventionPlugin.sendMessage(player, message);
                            gpClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                            gpClaim.getVisualizer().apply(player, false);
                            final WorldEditApiProvider worldEditProvider = GriefPreventionPlugin.instance.worldEditProvider;
                            if (worldEditProvider != null) {
                                worldEditProvider.stopVisualDrag(player);
                                worldEditProvider.visualizeClaim(gpClaim, player, playerData, false);
                            }
                        }
                    }
                }

                // otherwise tell him he can't create a claim here, and show him the existing claim
                // also advise him to consider /abandonclaim or resizing the existing claim
                else {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlap.toText());
                    List<Claim> claims = new ArrayList<>();
                    claims.add(claim);
                    CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                }
            }

            // otherwise tell the player he can't claim here because it's
            // someone else's claim, and show him the claim
            else {
                final Text message = GriefPreventionPlugin.instance.messageData.claimCreateOverlapPlayer
                        .apply(ImmutableMap.of(
                        "owner", claim.getOwnerName())).build();
                GriefPreventionPlugin.sendMessage(player, message);
                Visualization visualization = new Visualization(claim, VisualizationType.ERROR);
                visualization.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                visualization.apply(player);
                List<Claim> claims = new ArrayList<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims);
            }
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        } else if (playerData.shovelMode == ShovelMode.Subdivide && playerData.lastShovelLocation != null) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateSubdivisionFail.toText());
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        // otherwise, the player isn't in an existing claim!

        // if he hasn't already start a claim with a previous shovel action
        Location<World> lastShovelLocation = playerData.lastShovelLocation;
        if (lastShovelLocation == null) {
            // if he's at the claim count per player limit already and
            // doesn't have permission to bypass, display an error message
            if (!player.hasPermission(GPPermissions.OVERRIDE_CLAIM_LIMIT)) {
                int createClaimLimit = -1;
                if (playerData.shovelMode == ShovelMode.Basic) {
                    createClaimLimit = playerData.optionCreateClaimLimitBasic;
                } else if (playerData.shovelMode == ShovelMode.Town) {
                    createClaimLimit = playerData.optionCreateClaimLimitTown;
                } else if (playerData.shovelMode == ShovelMode.Subdivide) {
                    createClaimLimit = playerData.optionCreateClaimLimitSubdivision;
                }
    
                GPClaim parentClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(location);
                if (!parentClaim.isWilderness()) {
                    createClaimLimit = GPOptionHandler.getClaimOptionDouble(player, parentClaim, GPOptions.CREATE_CLAIM_LIMIT_BASIC, playerData).intValue();
                }

                if (createClaimLimit > 0 &&
                        (playerData.getInternalClaims().size() + 1) >= playerData.optionCreateClaimLimitBasic) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateFailedLimit.toText());
                    GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                    return;
                }
            }

            playerData.revertActiveVisual(player);
            // remember it, and start him on the new claim
            playerData.lastShovelLocation = location;
            final Text message = GriefPreventionPlugin.instance.messageData.claimStart
                    .apply(ImmutableMap.of(
                    "type", playerData.shovelMode.name())).build();
            GriefPreventionPlugin.sendMessage(player, message);

            // show him where he's working
            Visualization visualization = Visualization.fromClick(location, location.getBlockY(), PlayerUtils.getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
            visualization.apply(player, false);
        }

        // otherwise, he's trying to finish creating a claim by setting the other boundary corner
        else {
            // apply pvp rule
            if (playerData.inPvpCombat(player.getWorld())) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.pvpNoClaim.toText());
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            final GPClaim firstClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(playerData.lastShovelLocation);
            final GPClaim clickedClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(location);
            if (!firstClaim.equals(clickedClaim)) {
                final GPClaim overlapClaim = firstClaim.isWilderness() ? clickedClaim : firstClaim;
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlapShort.toText());
                List<Claim> claims = new ArrayList<>();
                claims.add(overlapClaim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            int y = lastShovelLocation.getBlockY();
            boolean cuboid = playerData.getCuboidMode();
            if (!cuboid) {
                y = lastShovelLocation.getBlockY() - activeConfig.getConfig().claim.extendIntoGroundDistance;
                if (y > 0) {
                    cuboid = true;
                }
            }
            Vector3i lesserBoundary = new Vector3i(
                    lastShovelLocation.getBlockX(),
                    cuboid ? lastShovelLocation.getBlockY() : 0,
                    lastShovelLocation.getBlockZ());
            Vector3i greaterBoundary = new Vector3i(
                    location.getBlockX(),
                    playerData.getCuboidMode() ? location.getBlockY() : player.getWorld().getDimension().getBuildHeight() - 1,
                    location.getBlockZ());
            // try to create a new claim
            ClaimResult result = this.dataStore.createClaim(
                    player.getWorld(),
                    lesserBoundary,
                    greaterBoundary,
                    PlayerUtils.getClaimTypeFromShovel(playerData.shovelMode), player.getUniqueId(), cuboid, Cause.of(NamedCause.source(player)));

            GPClaim gpClaim = (GPClaim) result.getClaim().orElse(null);
            // if it didn't succeed, tell the player why
            if (!result.successful()) {
                if (result.getResultType() != ClaimResultType.EXCEEDS_MAX_SIZE_X && result.getResultType() != ClaimResultType.EXCEEDS_MAX_SIZE_Y && result.getResultType() != ClaimResultType.EXCEEDS_MAX_SIZE_Z
                        && result.getResultType() != ClaimResultType.EXCEEDS_MIN_SIZE_X && result.getResultType() != ClaimResultType.EXCEEDS_MIN_SIZE_Y && result.getResultType() != ClaimResultType.EXCEEDS_MIN_SIZE_Z) {
                    GPClaim overlapClaim = (GPClaim) result.getClaim().get();
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlapShort.toText());
                    List<Claim> claims = new ArrayList<>();
                    claims.add(overlapClaim);
                    CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                }
                GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                return;
            }

            // otherwise, advise him on the /trust command and show him his new claim
            else {
                playerData.lastShovelLocation = null;
                final Text message = GriefPreventionPlugin.instance.messageData.claimCreateSuccess
                        .apply(ImmutableMap.of(
                        "type", gpClaim.getType().name())).build();
                GriefPreventionPlugin.sendMessage(player, message);
                final WorldEditApiProvider worldEditProvider = GriefPreventionPlugin.instance.worldEditProvider;
                if (worldEditProvider != null) {
                    worldEditProvider.stopVisualDrag(player);
                    worldEditProvider.visualizeClaim(gpClaim, player, playerData, false);
                }
                gpClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                gpClaim.getVisualizer().apply(player, false);
                // if it's a big claim, tell the player about subdivisions
                if (!player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && gpClaim.getArea() >= 1000) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlSubdivisionBasics.toText(), 201L);
                }
            }
        }
        GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
    }

    // helper methods for player events
    private boolean investigateClaim(Player player, BlockSnapshot clickedBlock, ItemStack itemInHand) {
        GPTimings.PLAYER_INVESTIGATE_CLAIM.startTimingIfSync();

        // if he's investigating a claim
        if (itemInHand == null || itemInHand.getItem() != GriefPreventionPlugin.instance.investigationTool) {
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
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionVisualClaimsNearby.toText());
                    GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                    return false;
                }

                // find nearby claims
                Location<World> nearbyLocation = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation : player.getLocation();
                List<Claim> claims = this.dataStore.getNearbyClaims(nearbyLocation);
                int height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY();
                Visualization visualization = Visualization.fromClaims(claims, playerData.getCuboidMode() ? height : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), player.getLocation(), playerData, null);
                visualization.apply(player);
                final Text message = GriefPreventionPlugin.instance.messageData.claimShowNearby
                        .apply(ImmutableMap.of(
                        "claim-count", claims.size())).build();
                GriefPreventionPlugin.sendMessage(player, message);
                if (!claims.isEmpty()) {

                    if (this.worldEditProvider != null) {
                        worldEditProvider.revertVisuals(player, playerData, null);
                        worldEditProvider.visualizeClaims(claims, player, playerData, true);
                    }
                    CommandHelper.showClaims(player, new ArrayList<Claim>(claims));
                }
                GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return true;
            }
            if (claim != null && claim.isWilderness()) {
                playerData.lastValidInspectLocation = null;
                GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return false;
            }
        } else {
            claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get());
            if (claim.isWilderness()) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.blockNotClaimed.toText());
                GPTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return false;
            }
        }

        // visualize boundary
        if (claim.id != playerData.visualClaimId) {
            int height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : clickedBlock.getLocation().get().getBlockY();
            claim.getVisualizer().createClaimBlockVisuals(playerData.getCuboidMode() ? height : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), player.getLocation(), playerData);
            claim.getVisualizer().apply(player);
            playerData.revertActiveVisual(player);
            if (this.worldEditProvider != null) {
                worldEditProvider.visualizeClaim(claim, player, playerData, true);
            }
            List<Claim> claims = new ArrayList<>();
            claims.add(claim);
            CommandHelper.showClaims(player, claims);
        }
        Text message = GriefPreventionPlugin.instance.messageData.blockClaimed
                .apply(ImmutableMap.of(
                "owner", claim.getOwnerName())).build();
        GriefPreventionPlugin.sendMessage(player, message);

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
            if (claim != null && !claim.isWilderness() && (playerData.visualBlocks == null || (claim.id != playerData.visualClaimId))) {
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
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimTooFar.toText());
        } else if (claim != null && claim.isWilderness()){
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.blockNotClaimed.toText());
        }

        return claim;
    }
}
