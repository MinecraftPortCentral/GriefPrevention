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
package me.ryanhamshire.griefprevention.event;

import com.google.common.collect.Sets;
import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.IpBanInfo;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.PlayerDataWorldManager;
import me.ryanhamshire.griefprevention.ShovelMode;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.VisualizationType;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.CreateClaimResult;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.task.AutoExtendClaimTask;
import me.ryanhamshire.griefprevention.task.CheckForPortalTrapTask;
import me.ryanhamshire.griefprevention.task.EquipShovelProcessingTask;
import me.ryanhamshire.griefprevention.task.PlayerKickBanTask;
import me.ryanhamshire.griefprevention.task.WelcomeTask;
import me.ryanhamshire.griefprevention.util.NbtDataHelper;
import me.ryanhamshire.griefprevention.util.WordFinder;
import net.minecraft.block.BlockDoor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.AchievementData;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.data.manipulator.mutable.entity.TameableData;
import org.spongepowered.api.data.manipulator.mutable.entity.VehicleData;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
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
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.interfaces.block.IMixinBlockState;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.util.VecHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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

    // matcher for banned words
    private WordFinder bannedWordFinder = new WordFinder(GriefPrevention.instance.dataStore.loadBannedWords());

    // typical constructor, yawn
    public PlayerEventHandler(DataStore dataStore, GriefPrevention plugin) {
        this.dataStore = dataStore;
    }

    // when a player chats, monitor for spam
    @Listener(order = Order.PRE)
    public void onPlayerChat(MessageChannelEvent.Chat event) {
        Player player = event.getCause().first(Player.class).get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        if (!player.isOnline()) {
            event.setCancelled(true);
            return;
        }

        String message = Text.of(event.getMessage()).toPlain();

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

            GriefPrevention.addLogEntry(notificationMessage, CustomLogEntryTypes.Debug, true);
        }

        // troll and excessive profanity filter
        else if (!player.hasPermission(GPPermissions.SPAM) && this.bannedWordFinder.hasMatch(message)) {
            // limit recipients to sender
            event.setChannel(player.getMessageChannel());

            // if player not new warn for the first infraction per play session.
            Optional<AchievementData> data = player.get(AchievementData.class);
            if (data.isPresent() && player.getAchievementData().achievements().contains(Achievements.MINE_WOOD)) {
                PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                if (!playerData.profanityWarned) {
                    playerData.profanityWarned = true;
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoProfanity);
                    GriefPrevention.addLogEntry("[Event: MessageChannelEvent.Chat][RootCause: " + event.getCause().root() + "][Message: " + event.getRawMessage() + "][CancelReason: " + this.dataStore.getMessage(Messages.NoProfanity) + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
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
            PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            for (MessageReceiver recipient : recipients) {
                if (recipient instanceof Player) {
                    Player reciever = (Player) recipient;

                    if (playerData.ignoredPlayers.containsKey(reciever.getUniqueId())) {
                        recipientsToRemove.add((Player) recipient);
                    } else {
                        PlayerData targetPlayerData = this.dataStore.getPlayerData(reciever.getWorld(), reciever.getUniqueId());
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
            if (GriefPrevention.instance.claimModeIsActive(player.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPrevention.sendMessage(player, TextMode.Info, Messages.CreativeBasicsVideo2, 10L);
            } else {
                GriefPrevention.sendMessage(player, TextMode.Info, Messages.SurvivalBasicsVideo2, 10L);
            }
        }

        // FEATURE: monitor for chat and command spam

        if (!GriefPrevention.getGlobalConfig().getConfig().spam.monitorEnabled)
            return false;

        // if the player has permission to spam, don't bother even examining the message
        if (player.hasPermission(GPPermissions.SPAM))
            return false;

        boolean spam = false;
        String mutedReason = null;

        // prevent bots from chatting - require movement before talking for any newish players
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.noChatLocation != null) {
            Location<World> currentLocation = player.getLocation();
            if (currentLocation.getBlockX() == playerData.noChatLocation.getBlockX() &&
                    currentLocation.getBlockZ() == playerData.noChatLocation.getBlockZ()) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoChatUntilMove, 10L);
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
            if (GriefPrevention.instance.containsBlockedIP(message)) {
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
                if (GriefPrevention.getGlobalConfig().getConfig().spam.autoBanOffenders) {
                    // log entry
                    GriefPrevention.addLogEntry("Banning " + player.getName() + " for spam.", CustomLogEntryTypes.AdminActivity);

                    // kick and ban
                    PlayerKickBanTask task =
                            new PlayerKickBanTask(player, GriefPrevention.getGlobalConfig().getConfig().spam.banMessage, "GriefPrevention Anti-Spam", true);
                    Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1).execute(task).submit(GriefPrevention.instance);
                } else {
                    // log entry
                    GriefPrevention.addLogEntry("Kicking " + player.getName() + " for spam.", CustomLogEntryTypes.AdminActivity);

                    // just kick
                    PlayerKickBanTask task = new PlayerKickBanTask(player, "", "GriefPrevention Anti-Spam", false);
                    Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1).execute(task).submit(GriefPrevention.instance);
                }

                return true;
            }

            // cancel any messages while at or above the third spam level and issue warnings anything above level 2, mute and warn
            if (playerData.spamCount >= 4) {
                if (mutedReason == null) {
                    mutedReason = "too-frequent text";
                }
                if (!playerData.spamWarned) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Warn, GriefPrevention.getGlobalConfig().getConfig().spam.banWarningMessage), 10L);
                    GriefPrevention.addLogEntry("Warned " + player.getName() + " about spam penalties.", CustomLogEntryTypes.Debug, true);
                    playerData.spamWarned = true;
                }
            }

            if (mutedReason != null) {
                // make a log entry
                GriefPrevention.addLogEntry("Muted " + mutedReason + ".");
                GriefPrevention.addLogEntry("Muted " + player.getName() + " " + mutedReason + ":" + message, CustomLogEntryTypes.Debug, true);

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
    @IsCancelled(Tristate.UNDEFINED)
    @Listener(order = Order.PRE)
    public void onPlayerCommandPreprocess(SendCommandEvent event) {
        String command = event.getCommand();
        String[] args = event.getArguments().split(" ");

        String message = "/" + event.getCommand() + " " + event.getArguments();

        CommandSource source = event.getCause().first(CommandSource.class).get();
        if (!(source instanceof Player)) {
            return;
        }

        Player player = (Player) source;
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }
        PlayerData playerData = null;

        // if a whisper
        if (GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().general.whisperCommandList.contains(command) && args.length > 1) {
            // determine target player, might be NULL
            Player targetPlayer = Sponge.getGame().getServer().getPlayer(args[1]).orElse(null);

            // if eavesdrop enabled and sender doesn't have the eavesdrop permission, eavesdrop
            if (GriefPrevention.getActiveConfig(targetPlayer.getWorld().getProperties()).getConfig().general.broadcastWhisperedMessagesToAdmins &&
                    !source.hasPermission(GPPermissions.EAVES_DROP)) {
                // except for when the recipient has eavesdrop permission
                if (targetPlayer == null || !targetPlayer.hasPermission(GPPermissions.EAVES_DROP)) {
                    StringBuilder logMessageBuilder = new StringBuilder();
                    logMessageBuilder.append("[[").append(source.getName()).append("]] ");

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
                playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                if (playerData.ignoredPlayers.containsKey(targetPlayer.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }

                PlayerData targetPlayerData = this.dataStore.getPlayerData(targetPlayer.getWorld(), targetPlayer.getUniqueId());
                if (targetPlayerData.ignoredPlayers.containsKey(player.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // if in pvp, block any pvp-banned slash commands
        if (playerData == null)
            playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        if (playerData != null && (playerData.inPvpCombat(player.getWorld()) || playerData.siegeData != null) && GriefPrevention.getActiveConfig(((Player) source).getWorld().getProperties()).getConfig().pvp.blockedCommandList.contains(command)) {
            event.setCancelled(true);
            GriefPrevention.sendMessage(event.getCause().first(Player.class).get(), TextMode.Err, Messages.CommandBannedInPvP);
            return;
        }

        // if the slash command used is in the list of monitored commands, treat
        // it like a chat message (see above)
        boolean isMonitoredCommand = false;
        for (String monitoredCommand : GriefPrevention.getGlobalConfig().getConfig().spam.monitoredCommandList) {
            if (args[0].equalsIgnoreCase(monitoredCommand)) {
                isMonitoredCommand = true;
                break;
            }
        }

        if (isMonitoredCommand && source instanceof Player) {
            // if anti spam enabled, check for spam
            if (GriefPrevention.getGlobalConfig().getConfig().spam.monitorEnabled && source instanceof Player) {
                event.setCancelled(this.handlePlayerChat((Player) source, message, event));
            }

            // unless cancelled, log in abridged logs
            if (!event.isCancelled()) {
                StringBuilder builder = new StringBuilder();
                for (String arg : args) {
                    builder.append(arg + " ");
                }

                makeSocialLogEntry((((Player) source)).getName(), builder.toString());
            }
        }

        // if requires access trust, check for permission
        Claim claim = this.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
        isMonitoredCommand = false;
        String lowerCaseMessage = message.toLowerCase();

        for (String monitoredCommand : GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.accessTrustCommands) {
            if (lowerCaseMessage.startsWith(monitoredCommand)) {
                isMonitoredCommand = true;
                break;
            }
        }

        if (claim != null) {
            for (String blockedCommand : claim.getClaimData().getFlags().blockCommands) {
                if (lowerCaseMessage.startsWith(blockedCommand)) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Err, Messages.BlockedCommand, lowerCaseMessage, claim.getOwnerName()));
                    GriefPrevention.addLogEntry("[Event: MessageChannelEvent.Chat][RootCause: " + event.getCause().root() + "][Message: " + message + "][CancelReason: Blocked command.]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (isMonitoredCommand) {
            if (claim != null) {
                playerData.lastClaim = claim;
                String reason = claim.allowAccess(player);
                if (reason != null) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Err, reason));
                    GriefPrevention.addLogEntry("[Event: MessageChannelEvent.Chat][RootCause: " + event.getCause().root() + "][Message: " + message + "][CancelReason: Monitored command.]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                }
            }
        }
    }

    static int longestNameLength = 10;

    static void makeSocialLogEntry(String name, String message) {
        StringBuilder entryBuilder = new StringBuilder(name);
        for (int i = name.length(); i < longestNameLength; i++) {
            entryBuilder.append(' ');
        }
        entryBuilder.append(": " + message);

        longestNameLength = Math.max(longestNameLength, name.length());

        GriefPrevention.addLogEntry(entryBuilder.toString(), CustomLogEntryTypes.SocialActivity, true);
    }

    private ConcurrentHashMap<UUID, Date> lastLoginThisServerSessionMap = new ConcurrentHashMap<UUID, Date>();

    // counts how many players are using each IP address connected to the server right now
    @SuppressWarnings("unused")
    private ConcurrentHashMap<String, Integer> ipCountHash = new ConcurrentHashMap<String, Integer>();

    // when a player attempts to join the server...
    @Listener(order = Order.LAST)
    public void onPlayerLogin(ClientConnectionEvent.Login event) {
        User player = event.getTargetUser();
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getToTransform().getExtent().getProperties())) {
            return;
        }

        // all this is anti-spam code
        if (GriefPrevention.getGlobalConfig().getConfig().spam.monitorEnabled) {
            // FEATURE: login cooldown to prevent login/logout spam with custom clients
            long now = Calendar.getInstance().getTimeInMillis();

            // if allowed to join and login cooldown enabled
            if (GriefPrevention.getGlobalConfig().getConfig().spam.loginCooldown > 0 && !player.hasPermission(GPPermissions.SPAM)) {
                // determine how long since last login and cooldown remaining
                Date lastLoginThisSession = lastLoginThisServerSessionMap.get(player.getUniqueId());
                if (lastLoginThisSession != null) {
                    long millisecondsSinceLastLogin = now - lastLoginThisSession.getTime();
                    long secondsSinceLastLogin = millisecondsSinceLastLogin / 1000;
                    long cooldownRemaining = GriefPrevention.getGlobalConfig().getConfig().spam.loginCooldown - secondsSinceLastLogin;

                    // if cooldown remaining
                    if (cooldownRemaining > 0) {
                        // DAS BOOT!;
                        event.setMessage(Text.of("You must wait " + cooldownRemaining + " seconds before logging-in again."));
                        GriefPrevention.addLogEntry("[Event: ClientConnectionEvent.Login][Player: " + event.getTargetUser() + "][CancelReason: login spam protection.]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);
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
        this.dataStore.createPlayerData(event.getToTransform().getExtent().getProperties(), player.getUniqueId());
        this.dataStore.getPlayerData(event.getToTransform().getExtent(), player.getUniqueId());
        PlayerData playerData = this.dataStore.getPlayerData(event.getToTransform().getExtent(), player.getUniqueId());
        playerData.ipAddress = event.getConnection().getAddress().getAddress();
    }

    // when a player successfully joins the server...
    @Listener(order = Order.LAST)
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        UUID playerID = player.getUniqueId();

        // note login time
        Date nowDate = new Date();
        long now = nowDate.getTime();
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), playerID);
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
            GriefPrevention.instance.checkPvpProtectionNeeded(player);

            // if in survival claims mode, send a message about the claim basics
            // video (except for admins - assumed experts)
            if (GriefPrevention.instance.claimModeIsActive(player.getWorld().getProperties(), ClaimsMode.Survival)
                    && !player.hasPermission(GPPermissions.CLAIMS_ADMIN) && this.dataStore.getPlayerDataWorldManager(player.getWorld().getProperties()).getWorldClaims().size() > 10) {
                WelcomeTask task = new WelcomeTask(player);
                // 10 seconds after join
                Sponge.getGame().getScheduler().createTaskBuilder().delay(10, TimeUnit.SECONDS).execute(task).submit(GriefPrevention.instance);
            }
        }

        // silence notifications when they're coming too fast
        if (!event.getMessage().equals(Text.of()) && this.shouldSilenceNotification()) {
            event.setMessage(Text.of());
        }

        // FEATURE: auto-ban accounts who use an IP address which was very
        // recently used by another banned account
        if (GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().general.smartBan && !hasJoinedBefore) {
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
                        GriefPrevention.addLogEntry("Auto-banned " + player.getName()
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
                        Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(10).execute(task).submit(GriefPrevention.instance);

                        // silence join message
                        event.setMessage(Text.of());

                        break;
                }
            }
        }

        // in case player has changed his name, on successful login, update UUID
        // > Name mapping
        //GriefPrevention.cacheUUIDNamePair(player.getUniqueId(), player.getName());

        // ensure we're not over the limit for this IP address
        // InetAddress ipAddress = playerData.ipAddress;
        // TODO - waiting on AchievementData to be implemented
        /*if (ipAddress != null) {
            String ipAddressString = ipAddress.toString();
            int ipLimit = GriefPrevention.getActiveConfig(player.getWorld()).getConfig().general.sharedIpLimit;
            if (ipLimit > 0 && (player.getOrCreate(AchievementData.class).isPresent() && !player.getAchievementData().achievements().contains(Achievements.MINE_WOOD))) {
                Integer ipCount = this.ipCountHash.get(ipAddressString);
                if (ipCount == null)
                    ipCount = 0;
                if (ipCount >= ipLimit) {
                    // kick player
                    PlayerKickBanTask task = new PlayerKickBanTask(player, "Sorry, there are too many players logged in with your IP address.",
                            "GriefPrevention IP-sharing limit.", false);
                    Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(10).execute(task).submit(GriefPrevention.instance);

                    // silence join message
                    event.setMessage(Text.of(""));
                    return;
                } else {
                    this.ipCountHash.put(ipAddressString, ipCount + 1);
                }
            }
        }

        // create a thread to load ignore information
        new IgnoreLoaderThread(playerID, playerData.ignoredPlayers).start();*/
    }


    // when a player spawns, conditionally apply temporary pvp protection
    @Listener(order = Order.POST)
    public void onPlayerRespawn(RespawnPlayerEvent event) {
        Player player = event.getTargetEntity();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
        playerData.lastPvpTimestamp = 0; // no longer in pvp combat

        // also send him any messaged from grief prevention he would have
        // received while dead
        if (playerData.messageOnRespawn != null) {
            // color is already embedded inmessage in this case
            GriefPrevention.sendMessage(player, Text.of(playerData.messageOnRespawn), 40L);
            playerData.messageOnRespawn = null;
        }

        GriefPrevention.instance.checkPvpProtectionNeeded(player);
    }

    // when a player dies...
    @Listener(order = Order.PRE)
    public void onPlayerDeath(DestructEntityEvent.Death event) {
        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }

        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            return;
        }

        // FEATURE: prevent death message spam by implementing a "cooldown
        // period" for death messages
        PlayerData playerData = this.dataStore.getPlayerData(event.getTargetEntity().getWorld(), event.getTargetEntity().getUniqueId());
        long now = Calendar.getInstance().getTimeInMillis();

        if (now - playerData.lastDeathTimeStamp < GriefPrevention.getGlobalConfig().getConfig().spam.deathMessageCooldown * 1000) {
            event.setMessage(Text.of());
        }

        playerData.lastDeathTimeStamp = now;

        // these are related to locking dropped items on death to prevent theft
        playerData.dropsAreUnlocked = false;
        playerData.receivedDropUnlockAdvertisement = false;
    }

    // when a player gets kicked...
    @Listener(order = Order.LAST)
    public void onPlayerKicked(KickPlayerEvent event) {
        Player player = event.getTargetEntity();
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        playerData.wasKicked = true;
    }

    // when a player quits...
    @Listener(order= Order.LAST)
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        Player player = event.getTargetEntity();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        UUID playerID = player.getUniqueId();
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), playerID);
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
        if (GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().pvp.punishPvpLogout && playerData.inPvpCombat(player.getWorld())) {
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
    @Listener(order = Order.PRE)
    public void onPlayerDropItem(DropItemEvent.Dispense event) {
        if (!event.getCause().containsType(Player.class)) {
            return;
        }

        Player player = event.getCause().first(Player.class).get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        // in creative worlds, dropping items is blocked
        if (GriefPrevention.instance.claimModeIsActive(player.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
            GriefPrevention.addLogEntry("[Event: DropItemEvent.Dispense][RootCause: " + event.getCause().root() + "][CancelReason: Drops not allowed in creative worlds.]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
            return;
        }

        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        // FEATURE: players under siege or in PvP combat, can't throw items on
        // the ground to hide
        // them or give them away to other players before they are defeated

        // if in combat, don't let him drop it
        if (!GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().pvp.allowCombatItemDrops && playerData.inPvpCombat(player.getWorld())) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoDrop);
            GriefPrevention.addLogEntry("[Event: DropItemEvent.Dispense][RootCause: " + event.getCause().root() + "][CancelReason: " + this.dataStore.getMessage(Messages.PvPNoDrop) + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
        }

        // if he's under siege, don't let him drop it
        else if (playerData.siegeData != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoDrop);
            GriefPrevention.addLogEntry("[Event: DropItemEvent.Dispense][RootCause: " + event.getCause().root() + "][CancelReason: " + this.dataStore.getMessage(Messages.SiegeNoDrop) + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
        }
    }

    private void handleEnterExitMessages(DisplaceEntityEvent event) {
        if (!(event.getCause().root() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getCause().root();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }
        Claim fromClaim = this.dataStore.getClaimAt(event.getFromTransform().getLocation(), false, null);
        Claim toClaim = this.dataStore.getClaimAt(event.getToTransform().getLocation(), false, null);
        // enter
        if (fromClaim != toClaim && toClaim != null) {
            if (GPFlags.getClaimFlagPermission(toClaim, GPFlags.GREETING_MESSAGE) != Tristate.FALSE) {
                Text welcomeMessage = toClaim.getClaimData().getGreetingMessage();
                if (!welcomeMessage.equals(Text.of())) {
                    player.sendMessage(welcomeMessage);
                    return;
                }
            }
        }

        // exit
        if (fromClaim != null && fromClaim != toClaim) {
            if (GPFlags.getClaimFlagPermission(fromClaim, GPFlags.FAREWELL_MESSAGE) != Tristate.FALSE) {
                Text farewellMessage = fromClaim.getClaimData().getFarewellMessage();
                if (!farewellMessage.equals(Text.of())) {
                    player.sendMessage(farewellMessage);
                }
            }
        }
    }

    @Listener
    public void onPlayerMove(DisplaceEntityEvent.Move event){
        if (!(event.getCause().root() instanceof Player)) {
            return;
        }

        handleEnterExitMessages(event);
    }

    // when a player teleports
    @Listener(order = Order.PRE)
    public void onPlayerTeleport(DisplaceEntityEvent.Teleport.TargetPlayer event) {
        Player player = event.getTargetEntity();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        handleEnterExitMessages(event);
        // these rules only apply to siege worlds only
        if (!GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.siegeEnabled) {
            return;
        }

        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        TeleportType type = event.getCause().first(TeleportCause.class).get().getTeleportType();

        // FEATURE: prevent players from using ender pearls to gain access to secured claims
        if (type.equals(TeleportTypes.ENDER_PEARL)) { // && GriefPrevention.instance.config_claims_enderPearlsRequireAccessTrust) {
            Claim toClaim = this.dataStore.getClaimAt(event.getToTransform().getLocation(), false, playerData.lastClaim);
            if (toClaim != null) {
                playerData.lastClaim = toClaim;
                String denyReason = toClaim.allowAccess(player);
                if (denyReason != null) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Err, denyReason));
                    GriefPrevention.addLogEntry("[Event: DisplaceEntityEvent.Teleport.TargetPlayer][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    ((EntityPlayer) player).inventory.addItemStackToInventory(new net.minecraft.item.ItemStack(Items.ender_pearl));
                }
            }
        }

        // FEATURE: prevent teleport abuse to win sieges

        Location<World> source = event.getFromTransform().getLocation();
        Claim sourceClaim = this.dataStore.getClaimAt(source, false, playerData.lastClaim);
        if (sourceClaim != null && sourceClaim.siegeData != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoTeleport);
            GriefPrevention.addLogEntry("[Event: DisplaceEntityEvent.Teleport.TargetPlayer][RootCause: " + event.getCause().root() + "][CancelReason: " + this.dataStore.getMessage(Messages.SiegeNoTeleport) + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
            return;
        }

        Location<World> destination = event.getToTransform().getLocation();
        Claim destinationClaim = this.dataStore.getClaimAt(destination, false, null);
        if (destinationClaim != null && destinationClaim.siegeData != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BesiegedNoTeleport);
            GriefPrevention.addLogEntry("[Event: DisplaceEntityEvent.Teleport.TargetPlayer][RootCause: " + event.getCause().root() + "][CancelReason: " + this.dataStore.getMessage(Messages.BesiegedNoTeleport) + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
            return;
        }

        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getToTransform().getExtent().getProperties())) {
            return;
        }

        if (!source.getExtent().getUniqueId().equals(destination.getExtent().getUniqueId())) {
            // new world, check if player has world storage for it
            PlayerDataWorldManager playerWorldManager = GriefPrevention.instance.dataStore.getPlayerDataWorldManager(destination.getExtent().getProperties());
            if (playerWorldManager.getPlayerData(player.getUniqueId()) == null) {
                playerWorldManager.createPlayerData(player.getUniqueId());
            }
        }

        if (event.getCause().containsType(TeleportCause.class)) {
            // FEATURE: when players get trapped in a nether portal, send them back through to the other side
            CheckForPortalTrapTask task = new CheckForPortalTrapTask(player, event.getFromTransform().getLocation());
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(200).execute(task).submit(GriefPrevention.instance);

            // FEATURE: if the player teleporting doesn't have permission to
            // build a nether portal and none already exists at the destination, cancel the teleportation
            /*if (GriefPrevention.instance.config_claims_portalsRequirePermission) {
                destination = event.getToTransform().getLocation();
                /*if (event.useTravelAgent()) {
                    if (event.getTeleporterAgent().getCanCreatePortal()) {
                        // hypothetically find where the portal would be created
                        // if it were
                        TravelAgent agent = event.getPortalTravelAgent();
                        agent.setCanCreatePortal(false);
                        destination = agent.findOrCreate(destination);
                        agent.setCanCreatePortal(true);
                    } else {
                        // if not able to create a portal, we don't have to do
                        // anything here
                        return;
                    }
                }*/

                // if creating a new portal
                /*if (destination.getBlock().getType() != BlockTypes.PORTAL) {
                    // check for a land claim and the player's permission that land claim
                    Claim claim = this.dataStore.getClaimAt(destination, false, null);
                    if (claim == null) {
                        return;
                    }

                    String denyReason = claim.allowBuild(player, destination);
                    if (denyReason != null) {
                        // cancel and inform about the reason
                        GriefPrevention.addLogEntry("[Event: DisplaceEntityEvent.Teleport.TargetPlayer][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoBuildPortalPermission, claim.getOwnerName());
                    }
                }
            }*/
        }
    }

    // when a player interacts with an entity...
    @IsCancelled(Tristate.UNDEFINED)
    @Listener(order = Order.PRE)
    public void onPlayerInteractEntity(InteractEntityEvent event) {
        if (!event.getCause().containsType(Player.class) || !GriefPrevention.isEntityProtected(event.getTargetEntity())) {
            return;
        }

        Player player = event.getCause().first(Player.class).get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        Entity entity = event.getTargetEntity();
        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(entity.getWorld().getProperties());
        Optional<ItemStack> itemInHand = player.getItemInHand();
        if (itemInHand.isPresent()) {
            net.minecraft.item.ItemStack itemstack = (net.minecraft.item.ItemStack)(Object) itemInHand.get();
            if (GriefPrevention.isItemBanned(player, itemInHand.get().getItem(), itemstack.getMetadata())) {
                GriefPrevention.sendMessage(player, TextColors.RED, Messages.ItemBanned, itemInHand.get().getItem().getId());
                GriefPrevention.addLogEntry("[Event: InteractEntityEvent][RootCause: " + event.getCause().root() + "][CancelReason: Item " + itemInHand.get().getItem().getId() + " is banned.]", CustomLogEntryTypes.Debug);
                event.setCancelled(true);
                return;
            }
        }

        Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
        // allow entity protection to be overridden to allow management from other plugins
        if (activeConfig.getConfig().claim.ignoredEntityIds.contains(entity.getType().getId())) {
            return;
        }

        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        // always allow interactions when player is in ignore claims mode
        if (playerData.ignoreClaims) {
            return;
        }

        if (claim != null) {
            String denyReason = claim.allowAccess(player, Optional.of(event.getTargetEntity().getLocation()));
            if (denyReason != null) {
                GriefPrevention.addLogEntry("[Event: InteractEntityEvent][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                event.setCancelled(true);
                return;
            }
        }

        // if entity is tameable and has an owner, apply special rules
        if (entity.supports(TameableData.class)) {
            TameableData data = entity.getOrCreate(TameableData.class).get();
            if (data.owner().exists()) {
                UUID ownerID = data.owner().get().get();

                // if the player interacting is the owner or an admin in ignore claims mode, always allow
                if (player.getUniqueId().equals(ownerID) || playerData.ignoreClaims) {
                    // if giving away pet, do that instead
                    if (playerData.petGiveawayRecipient != null) {
                        entity.offer(Keys.TAMED_OWNER, Optional.of(playerData.petGiveawayRecipient.getUniqueId()));
                        playerData.petGiveawayRecipient = null;
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.PetGiveawayConfirmation);
                        GriefPrevention.addLogEntry("[Event: InteractEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + event.getTargetEntity() + "][CancelReason: Pet giveaway.]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);
                    }

                    return;
                }
                if (!GriefPrevention.instance.pvpRulesApply(entity.getLocation().getExtent())) {
                    // otherwise disallow
                    if (event.getCause().root() instanceof Player) {
                        User owner = Sponge.getGame().getServiceManager().provideUnchecked(UserStorageService.class).get(ownerID).get();
                        String message = GriefPrevention.instance.dataStore.getMessage(Messages.NotYourPet, owner.getName());
                        if (player.hasPermission(GPPermissions.IGNORE_CLAIMS))
                            message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                        GriefPrevention.sendMessage(player, Text.of(TextMode.Err, message));
                    }
                    GriefPrevention.addLogEntry("[Event: InteractEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + event.getTargetEntity() + "][CancelReason: Entity is tamed.]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }
        } else { // search for owner manually
            IMixinEntity spongeEntity = (IMixinEntity) entity;
            Optional<User> owner = spongeEntity.getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
            if (owner.isPresent()) {
                if (player.getUniqueId().equals(owner.get().getUniqueId())) {
                    return;
                }
            }
        }

        // if the entity is a vehicle
        if (entity.supports(VehicleData.class)) {
            // if the entity is in a claim
            claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
            if (claim != null) {
                // for storage entities, apply container rules (this is a potential theft)
                if (entity instanceof Carrier) {
                    String denyReason = claim.allowAccess(player);
                    if (denyReason != null) {
                        GriefPrevention.sendMessage(player, Text.of(TextMode.Err, denyReason));
                        GriefPrevention.addLogEntry("[Event: InteractEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + event.getTargetEntity() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // if the entity is an animal, apply container rules
        if (claim != null && entity instanceof Animal
                || (entity.getType() == EntityTypes.VILLAGER && GPFlags.getClaimFlagPermission(claim, GPPermissions.VILLAGER_TRADING) == Tristate.FALSE)) {
                String denyReason = claim.allowAccess(player, Optional.of(entity.getLocation()));
            if (denyReason != null) {
                String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                if (player.hasPermission(GPPermissions.IGNORE_CLAIMS))
                    message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, message));
                GriefPrevention.addLogEntry("[Event: InteractEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + event.getTargetEntity() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                event.setCancelled(true);
                return;
            }
        }

        // if preventing theft, prevent leashing claimed creatures
        if (entity instanceof Animal && player.getItemInHand().isPresent() && player
                .getItemInHand().get().getItem().equals(ItemTypes.LEAD)) {
            claim = this.dataStore.getClaimAt(entity.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                String denyReason = claim.allowAccess(player, Optional.of(entity.getLocation()));
                if (denyReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Err, denyReason));
                    GriefPrevention.addLogEntry("[Event: InteractEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + event.getTargetEntity() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    return;
                }
            }
        }
    }

    // when a player picks up an item...
    @IsCancelled(Tristate.UNDEFINED)
    @Listener(order = Order.EARLY)
    public void onPlayerPickupItem(CollideEntityEvent event) {
        Optional<Player> optPlayer = event.getCause().first(Player.class);

        if (!optPlayer.isPresent()) {
            return;
        }

        Player player = optPlayer.get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        // FEATURE: lock dropped items to player who dropped them

        // who owns this stack?
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof Item)) {
                continue;
            }
            Item item = (Item) entity;

            Optional<User> owner = NbtDataHelper.getOwnerOfEntity((net.minecraft.entity.Entity) item);
            if (owner.isPresent()) {
                // has that player unlocked his drops?
                if (owner.isPresent() && owner.get().isOnline() && player.getUniqueId() != owner.get().getUniqueId()) {
                    PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), owner.get().getUniqueId());

                    // FEATURE: lock dropped items to player who dropped them

                    World world = entity.getWorld();

                    // decide whether or not to apply this feature to this situation
                    // (depends on the world where it happens)
                    boolean isPvPWorld = GriefPrevention.instance.pvpRulesApply(world);
                    if ((isPvPWorld && GriefPrevention.getActiveConfig(world.getProperties()).getConfig().pvp.protectItemsOnDeathPvp) ||
                            (!isPvPWorld && GriefPrevention.getActiveConfig(world.getProperties()).getConfig().pvp.protectItemsOnDeathNonPvp)) {

                        // allow the player to receive a message about how to unlock any drops
                        playerData.dropsAreUnlocked = false;
                        playerData.receivedDropUnlockAdvertisement = false;
                    }

                    // if locked, don't allow pickup
                    if (!playerData.dropsAreUnlocked && GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().general.lockItemDrops) {
                        GriefPrevention.addLogEntry("[Event: CollideEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][CancelReason: Drops are locked.]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);

                        // if hasn't been instructed how to unlock, send explanatory
                        // messages
                        if (!playerData.receivedDropUnlockAdvertisement) {
                            GriefPrevention.sendMessage((Player) owner.get(), TextMode.Instr, Messages.DropUnlockAdvertisement);
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.PickupBlockedExplanation, owner.get().getName());
                            playerData.receivedDropUnlockAdvertisement = true;
                        }

                        return;
                    }
                }
            }
        }

        // the rest of this code is specific to pvp worlds
        if (!GriefPrevention.instance.pvpRulesApply(player.getWorld()))
            return;

        // if we're preventing spawn camping and the player was previously empty handed...
        if (GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().pvp.protectFreshSpawns && !player.getItemInHand().isPresent()) {
            // if that player is currently immune to pvp
            PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            if (playerData.pvpImmune) {
                // if it's been less than 10 seconds since the last time he spawned, don't pick up the item
                long now = Calendar.getInstance().getTimeInMillis();
                long elapsedSinceLastSpawn = now - playerData.lastSpawn;
                if (elapsedSinceLastSpawn < 10000) {
                    GriefPrevention.addLogEntry("[Event: CollideEntityEvent][RootCause: " + event.getCause().root() + "][CancelReason: Player PVP Immune.]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }

                // otherwise take away his immunity. he may be armed now. at least, he's worth killing for some loot
                playerData.pvpImmune = false;
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
            }
        }
    }

    // when a player switches in-hand items
    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onItemHeldChange(ChangeInventoryEvent.Held event) {
        Optional<Player> player = event.getCause().first(Player.class);

        if (!player.isPresent()) {
            return;
        }

        if (!GriefPrevention.instance.claimsEnabledForWorld(player.get().getWorld().getProperties())) {
            return;
        }
        PlayerData playerData = this.dataStore.getPlayerData(player.get().getWorld(), player.get().getUniqueId());

        // if he's switching to the golden shovel
        for (SlotTransaction transaction : event.getTransactions()) {
            ItemStackSnapshot newItemStack = transaction.getFinal();
            if (newItemStack != null && newItemStack.getType().getId().equals(GriefPrevention.getActiveConfig(player.get().getWorld().getProperties()).getConfig().claim.modificationTool)) {
                // give the player his available claim blocks count and claiming
                // instructions, but only if he keeps the shovel equipped for a
                // minimum time, to avoid mouse wheel spam
                EquipShovelProcessingTask task = new EquipShovelProcessingTask(player.get());
                Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(15).execute(task).submit(GriefPrevention.instance);
            } else {
                // if we have an active visualization that is not claim investigation, revert and update client
                if (playerData != null && playerData.currentVisualization != null && playerData.currentVisualization.getType() != VisualizationType.ClaimInvestigation) {
                    Visualization.Revert(player.get());
                }
            }
        }
    }

    // educates a player about /adminclaims and /acb, if he can use them
    private void tryAdvertiseAdminAlternatives(Player player) {
        if (player.hasPermission(GPPermissions.CLAIMS_ADMIN) && player.hasPermission(GPPermissions.ADJUST_CLAIM_BLOCKS)) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseACandACB);
        } else if (player.hasPermission(GPPermissions.CLAIMS_ADMIN)) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseAdminClaims);
        } else if (player.hasPermission(GPPermissions.ADJUST_CLAIM_BLOCKS)) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseACB);
        }
    }

    @SuppressWarnings("unused")
    @Listener
    public void onPlayerInteractBlockPrimary(InteractBlockEvent.Primary event) {
        Optional<Player> playerOpt = event.getCause().first(Player.class);

        if (!playerOpt.isPresent() || !playerOpt.get().getItemInHand().isPresent() || !event.getTargetBlock().getLocation().isPresent()) {
            return;
        }

        Player player = playerOpt.get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        BlockSnapshot clickedBlock = event.getTargetBlock();
        if (clickedBlock.getState().getType() == BlockTypes.AIR) {
            return;
        }

        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, null);
        if (claim == null) {
            return;
        }

        String denyReason = GriefPrevention.instance.allowBreak(player, event.getTargetBlock());
        if (denyReason != null) {
            if (event.getCause().root() instanceof Player) {
                GriefPrevention.sendMessage((Player) event.getCause().root(), Text.of(TextMode.Err, denyReason));
            }

            GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Primary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
            return;
        }
        // exception for blocks on a specific watch list
        if (!this.onLeftClickWatchList(clickedBlock.getState().getType()) && clickedBlock.getState().getType().getItem().isPresent()
                && !claim.isItemBlacklisted(clickedBlock.getState().getType().getItem().get(), (((IMixinBlockState)clickedBlock.getState()).getStateMeta()))) {
            // and an exception for putting out fires
            /*if (GriefPrevention.instance.config_claims_protectFires) {
                if (clickedBlock.getState().getType() == BlockTypes.FIRE) {
                    if (playerData == null) {
                        playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                    }
                    claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
                    if (claim != null) {
                        playerData.lastClaim = claim;

                        denyReason = claim.allowBuild(player, clickedBlock.getLocation().get());
                        if (denyReason != null) {
                            GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Primary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                            event.setCancelled(true);
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuildingOutsideClaims, denyReason);
                            //player.sendBlockChange(clickedBlock.getLocation().get(), clickedBlock.getState().getType(), adjacentBlock.getData());
                            return;
                        }
                    }
                }
            }*/

            return;
        }
    }

    @Listener
    public void onPlayerInteractBlockSecondary(InteractBlockEvent.Secondary event) {
        Optional<Player> playerOpt = event.getCause().first(Player.class);
        if (!playerOpt.isPresent()) {
            return;
        }

        Player player = playerOpt.get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            return;
        }

        BlockSnapshot clickedBlock = event.getTargetBlock();
        Optional<ItemStack> itemInHand = player.getItemInHand();

        // Check if item is banned
        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(player.getWorld().getProperties());
        if (itemInHand.isPresent() && GriefPrevention.isItemBanned(player, itemInHand.get().getItem(), (
                ((net.minecraft.item.ItemStack)(Object) itemInHand.get()).getMetadata()))) {
            GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][Item: " + itemInHand.get() + "][CancelReason: Item " + itemInHand.get().getItem().getId() + " is banned.]", CustomLogEntryTypes.Debug);
            GriefPrevention.sendMessage(player, TextColors.RED, Messages.ItemBanned, itemInHand.get().getItem().getId());
            event.setCancelled(true);
            return;
        }

        investigateClaim(player, clickedBlock, itemInHand);

        if (!clickedBlock.getLocation().isPresent()) {
            return;
        }

        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Claim playerClaim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, null);
        if (playerData != null && !playerData.ignoreClaims && playerClaim != null) {
            // following a siege where the defender lost, the claim will allow everyone access for a time
            if (playerClaim.doorsOpen && activeConfig.getConfig().siege.winnerAccessibleBlocks.contains(clickedBlock.getState().getType().getId())) {
                if (clickedBlock.getState().getType() == BlockTypes.IRON_DOOR) {
                    ((BlockDoor) clickedBlock.getState().getType()).toggleDoor((net.minecraft.world.World) player.getWorld(), VecHelper.toBlockPos(event.getTargetBlock().getPosition()), true);
                }

                return; // allow
            } else {
                String denyReason = playerClaim.allowAccess(player, clickedBlock.getLocation());
                if(denyReason != null) {
                    boolean result = false;
                    if (itemInHand.isPresent() && itemInHand.get().getItem() instanceof ItemBlock) {
                        if (GPFlags.getClaimFlagPermission(player, playerClaim, GPFlags.BLOCK_PLACE) == Tristate.TRUE) {
                            result = true;
                            event.setUseItemResult(Tristate.TRUE);
                        }
                    }
                    if (GPFlags.getClaimFlagPermission(player, playerClaim, GPFlags.INTERACT_SECONDARY) == Tristate.TRUE) {
                        result = true;
                        event.setUseBlockResult(Tristate.TRUE);
                    }
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    if (!result) {
                        if (event.getCause().root() instanceof Player) {
                            GriefPrevention.sendMessage((Player) event.getCause().root(), Text.of(TextMode.Err, denyReason));
                        }
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // apply rules for containers
        Optional<TileEntity> tileEntity = clickedBlock.getLocation().get().getTileEntity();
        if (tileEntity.isPresent() && tileEntity.get() instanceof IInventory) {
            if (playerData == null) {
                playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            }

            // block container use while under siege, so players can't hide items from attackers
            if (playerData.siegeData != null) {
                GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + this.dataStore.getMessage(Messages.SiegeNoContainers) + "]", CustomLogEntryTypes.Debug);
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoContainers);
                event.setCancelled(true);
                return;
            }

            // block container use during pvp combat, same reason
            if (playerData.inPvpCombat(player.getWorld())) {
                GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + this.dataStore.getMessage(Messages.PvPNoContainers) + "]", CustomLogEntryTypes.Debug);
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
                event.setCancelled(true);
                return;
            }

            // otherwise check permissions for the claim the player is in
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
            if (claim != null) {
                String denyReason = claim.allowAccess(player, clickedBlock.getLocation());
                if (denyReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                    return;
                }
            }

            // if the event hasn't been cancelled, then the player is allowed to use the container so drop any pvp protection
            if (playerData.pvpImmune) {
                playerData.pvpImmune = false;
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
            }
        }

        // otherwise apply rules for buttons and switches
        else if ((clickedBlock.getState().getType() == BlockTypes.STONE_BUTTON || clickedBlock.getState().getType() == BlockTypes.WOODEN_BUTTON
                        || clickedBlock.getState().getType() == BlockTypes.LEVER || (playerClaim != null && clickedBlock.getState().getType()
                .getItem().isPresent() && playerClaim.isItemBlacklisted(clickedBlock.getState().getType().getItem().get(), (((IMixinBlockState)clickedBlock.getState()).getStateMeta()))))) {
            if (playerData == null)
                playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String denyReason = claim.allowAccess(player);
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                    return;
                }
            }
        }

        // otherwise apply rule for cake
        else if (clickedBlock.getState().getType() == BlockTypes.CAKE) {
            if (playerData == null)
                playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String denyReason = claim.allowAccess(player);
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                    return;
                }
            }
        }

        // apply rule for note blocks and repeaters and daylight sensors
        else if (clickedBlock.getState().getType() == BlockTypes.NOTEBLOCK ||
                 clickedBlock.getState().getType() == BlockTypes.POWERED_REPEATER ||
                 clickedBlock.getState().getType() == BlockTypes.UNPOWERED_REPEATER ||
                 clickedBlock.getState().getType() == BlockTypes.DRAGON_EGG ||
                 clickedBlock.getState().getType() == BlockTypes.DAYLIGHT_DETECTOR ||
                 clickedBlock.getState().getType() == BlockTypes.DAYLIGHT_DETECTOR_INVERTED ||
                 clickedBlock.getState().getType() == BlockTypes.POWERED_COMPARATOR ||
                 clickedBlock.getState().getType() == BlockTypes.UNPOWERED_COMPARATOR) {
            if (playerData == null)
                playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
            if (claim != null) {
                String denyReason = claim.allowBuild(player, clickedBlock);
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                    return;
                }
            }
        }

        // otherwise handle right click (shovel, string, bonemeal)
        else {

            if (!itemInHand.isPresent()) {
                return;
            }
            // what's the player holding?
            ItemType materialInHand = itemInHand.get().getItem();

            // if it's bonemeal or armor stand or spawn egg, check for build
            // permission (ink sac == bone meal, must be a Bukkit bug?)
            if ((materialInHand == ItemTypes.DYE || materialInHand == ItemTypes.ARMOR_STAND || materialInHand == ItemTypes.MONSTER_EGG)) {
                String denyReason = GriefPrevention.instance.allowBuild(player, clickedBlock);
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                    event.setCancelled(true);
                }

                return;
            }

            else if (materialInHand == ItemTypes.BOAT) {
                if (playerData == null)
                    playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
                if (claim != null) {
                    String denyReason = claim.allowAccess(player);
                    if (denyReason != null) {
                        GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                        GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                        event.setCancelled(true);
                    }
                }

                return;
            }

            // if it's a spawn egg, minecart, or boat, and this is a creative world, apply special rules
            else if ((materialInHand == ItemTypes.MINECART || materialInHand == ItemTypes.MINECART
                            || materialInHand == ItemTypes.CHEST_MINECART || materialInHand == ItemTypes.BOAT)
                    && GriefPrevention.instance.claimModeIsActive(clickedBlock.getLocation().get().getExtent().getProperties(), ClaimsMode.Creative)) {
                // player needs build permission at this location
                String denyReason = GriefPrevention.instance.allowBuild(player, clickedBlock);
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                    event.setCancelled(true);
                    return;
                }

                // enforce limit on total number of entities in this claim
                if (playerData == null)
                    playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
                if (claim == null)
                    return;

                denyReason = claim.allowMoreEntities();
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                    event.setCancelled(true);
                    return;
                }

                return;
            }

            // if holding a non-vanilla item
            else if (player.getItemInHand().isPresent() && !player.getItemInHand().get().getItem().getId().contains("minecraft")) {
                // assume it's a long range tool and project out ahead
                if (clickedBlock.getState().getType() == BlockTypes.AIR) {
                    // try to find a far away non-air block along line of sight
                    clickedBlock = getTargetBlock(player, 100);
                }

                // if target is claimed, require build trust permission
                if (playerData == null) {
                    playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                }

                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
                if (claim != null) {
                    String denyReason = GriefPrevention.instance.allowBreak(player, clickedBlock);
                    if (denyReason != null) {
                        GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                        GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                        event.setCancelled(true);
                        return;
                    }
                }

                return;
            }

                if (!materialInHand.getId().equals(activeConfig.getConfig().claim.modificationTool)) {
                    return;
                }

                // disable golden shovel while under siege
                if (playerData == null)
                    playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                if (playerData.siegeData != null) {
                    GriefPrevention.addLogEntry("[Event: InteractBlockEvent.Secondary][RootCause: " + event.getCause().root() + "][CancelReason: " + this.dataStore.getMessage(Messages.SiegeNoShovel) + "]", CustomLogEntryTypes.Debug);
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoShovel);
                    event.setCancelled(true);
                    return;
                }

                // FEATURE: shovel and stick can be used from a distance away
                if (clickedBlock.getState().getType() == BlockTypes.AIR) {
                    // try to find a far away non-air block along line of sight
                    clickedBlock = getTargetBlock(player, 100);
                }

                // if no block, stop here
                if (clickedBlock == null) {
                    return;
                }

                // can't use the shovel from too far away
                //if (clickedBlockType == Material.AIR) {
                   // GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
                    //return;
                //}

                // if the player is in restore nature mode, do only that
                UUID playerID = player.getUniqueId();
                playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                if (playerData.shovelMode == ShovelMode.RestoreNature || playerData.shovelMode == ShovelMode.RestoreNatureAggressive) {
                    // if the clicked block is in a claim, visualize that claim and deliver an error message
                    Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);
                    if (claim != null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.BlockClaimed, claim.getOwnerName());
                        Visualization visualization =
                                Visualization.FromClaim(claim, clickedBlock.getPosition().getY(), VisualizationType.ErrorClaim, player.getLocation());
                        Visualization.Apply(player, visualization);

                        return;
                    }

                    // figure out which chunk to repair
                    Chunk chunk = player.getWorld().getChunk(clickedBlock.getLocation().get().getBlockX() >> 4, 0, clickedBlock.getLocation().get().getBlockZ() >> 4).get();

                    // start the repair process

                    // set boundaries for processing
                    int miny = clickedBlock.getPosition().getY();

                    // if not in aggressive mode, extend the selection down to a little below sea level
                    if (!(playerData.shovelMode == ShovelMode.RestoreNatureAggressive)) {
                        if (miny > GriefPrevention.instance.getSeaLevel(chunk.getWorld()) - 10) {
                            miny = GriefPrevention.instance.getSeaLevel(chunk.getWorld()) - 10;
                        }
                    }

                    GriefPrevention.instance.restoreChunk(chunk, miny, playerData.shovelMode == ShovelMode.RestoreNatureAggressive, 0, player);

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

                    Claim cachedClaim = null;
                    for (int x = minx; x <= maxx; x++) {
                        for (int z = minz; z <= maxz; z++) {
                            // circular brush
                            Location<World> location = new Location<World>(clickedBlock.getLocation().get().getExtent(), x, clickedBlock.getPosition().getY(), z);
                            if (location.getPosition().distance(clickedBlock.getLocation().get().getPosition()) > playerData.fillRadius)
                                continue;

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
                                Claim claim = this.dataStore.getClaimAt(block.getLocation().get(), false, cachedClaim);
                                if (claim != null) {
                                    cachedClaim = claim;
                                    break;
                                }

                                // only replace air, spilling water, snow, long grass
                                if (block.getState().getType() == BlockTypes.AIR || block.getState().getType() == BlockTypes.SNOW
                                        || (block.getState().getType() == BlockTypes.WATER)
                                        || block.getState().getType() == BlockTypes.TALLGRASS) {
                                    // if the top level, always use the default filler picked above
                                    if (y == maxHeight) {
                                        block.withState(defaultFiller.getDefaultState()).restore(true, false);
                                    }

                                    // otherwise look to neighbors for an appropriate fill block
                                    else {
                                        Location<World> eastBlock = block.getLocation().get().getRelative(Direction.EAST);
                                        Location<World> westBlock = block.getLocation().get().getRelative(Direction.WEST);
                                        Location<World> northBlock = block.getLocation().get().getRelative(Direction.NORTH);
                                        Location<World> southBlock = block.getLocation().get().getRelative(Direction.SOUTH);

                                        // first, check lateral neighbors (ideally,
                                        // want to keep natural layers)
                                        if (allowedFillBlocks.contains(eastBlock.getBlockType())) {
                                            block.withState(eastBlock.getBlock()).restore(true, false);
                                        } else if (allowedFillBlocks.contains(westBlock.getBlockType())) {
                                            block.withState(westBlock.getBlock()).restore(true, false);
                                        } else if (allowedFillBlocks.contains(northBlock.getBlockType())) {
                                            block.withState(northBlock.getBlock()).restore(true, false);
                                        } else if (allowedFillBlocks.contains(southBlock.getBlockType())) {
                                            block.withState(southBlock.getBlock()).restore(true, false);
                                        }

                                        // if all else fails, use the default filler
                                        // selected above
                                        else {
                                            block.withState(defaultFiller.getDefaultState()).restore(true, false);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return;
                }

                // if the player doesn't have claims permission, don't do anything
                if (!player.hasPermission(GPPermissions.CREATE_CLAIM)) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreateClaimPermission);
                    return;
                }

                // if he's resizing a claim and that claim hasn't been deleted since he started resizing it
                if (playerData.claimResizing != null && playerData.claimResizing.inDataStore) {
                    if (clickedBlock.getLocation().get().equals(playerData.lastShovelLocation)) {
                        return;
                    }

                    // figure out what the coords of his new claim would be
                    int newx1, newx2, newz1, newz2, newy1, newy2;
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

                    // for top level claims, apply size rules and claim blocks requirement
                    if (playerData.claimResizing.parent == null) {
                        // measure new claim, apply size rules
                        int newWidth = (Math.abs(newx1 - newx2) + 1);
                        int newHeight = (Math.abs(newz1 - newz2) + 1);
                        boolean smaller = newWidth < playerData.claimResizing.getWidth() || newHeight < playerData.claimResizing.getHeight();

                        if (!player.hasPermission(GPPermissions.CLAIMS_ADMIN) && !playerData.claimResizing.isAdminClaim() && smaller) {
                            if (newWidth < activeConfig.getConfig().claim.claimMinimumWidth
                                    || newHeight < activeConfig.getConfig().claim.claimMinimumWidth) {
                                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeClaimTooNarrow,
                                        String.valueOf(activeConfig.getConfig().claim.claimMinimumWidth));
                                return;
                            }

                            int newArea = newWidth * newHeight;
                            if (newArea < activeConfig.getConfig().claim.claimMinimumArea) {
                                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeClaimInsufficientArea,
                                        String.valueOf(activeConfig.getConfig().claim.claimMinimumArea));
                                return;
                            }
                        }

                        // make sure player has enough blocks to make up the difference
                        if (!playerData.claimResizing.isAdminClaim() && player.getName().equals(playerData.claimResizing.getOwnerName())) {
                            int newArea = newWidth * newHeight;
                            int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + (playerData.claimResizing.getArea() - newArea);

                            if (blocksRemainingAfter < 0) {
                                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeNeedMoreBlocks,
                                        String.valueOf(Math.abs(blocksRemainingAfter)));
                                this.tryAdvertiseAdminAlternatives(player);
                                return;
                            }
                        }
                    }

                    // special rule for making a top-level claim smaller. to check this, verifying the old claim's corners are inside the new claim's boundaries.
                    // rule: in any mode, shrinking a claim removes any surface fluids
                    Claim oldClaim = playerData.claimResizing;
                    boolean smaller = false;
                    if (oldClaim.parent == null) {
                        // temporary claim instance, just for checking contains()
                        Claim newClaim = new Claim(
                                new Location<World>(oldClaim.getLesserBoundaryCorner().getExtent(), newx1, newy1, newz1),
                                new Location<World>(oldClaim.getLesserBoundaryCorner().getExtent(), newx2, newy2, newz2));

                        // if the new claim is smaller
                        if (!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false)
                                || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false)) {
                            smaller = true;

                            // remove surface fluids about to be unclaimed
                            oldClaim.removeSurfaceFluids(newClaim);
                        }
                    }

                    // ask the datastore to try and resize the claim, this checks for conflicts with other claims
                    CreateClaimResult result =
                            GriefPrevention.instance.dataStore.resizeClaim(playerData.claimResizing, newx1, newx2, newy1, newy2, newz1, newz2, player);

                    if (result.succeeded) {
                        // decide how many claim blocks are available for more resizing
                        int claimBlocksRemaining = 0;
                        if (!playerData.claimResizing.isAdminClaim()) {
                            UUID ownerID = playerData.claimResizing.ownerID;
                            if (playerData.claimResizing.parent != null) {
                                ownerID = playerData.claimResizing.parent.ownerID;
                            }

                            if (ownerID.equals(player.getUniqueId())) {
                                claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                            } else {
                                PlayerData ownerData = this.dataStore.getPlayerData(player.getWorld(), ownerID);
                                claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                                Optional<User> owner = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(ownerID);
                                if (owner.isPresent() && !owner.get().isOnline()) {
                                    this.dataStore.clearCachedPlayerData(player.getWorld().getProperties(), ownerID);
                                }
                            }
                        }

                        // inform about success, visualize, communicate remaining blocks available
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimResizeSuccess, String.valueOf(claimBlocksRemaining));
                        Visualization visualization =
                                Visualization.FromClaim(result.claim, clickedBlock.getPosition().getY(), VisualizationType.Claim, player.getLocation());
                        Visualization.Apply(player, visualization);

                        // if resizing someone else's claim, make a log entry
                        if (!playerID.equals(playerData.claimResizing.ownerID) && playerData.claimResizing.parent == null) {
                            GriefPrevention.addLogEntry(player.getName() + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at "
                                    + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
                        }

                        // if increased to a sufficiently large size and no subdivisions yet, send subdivision instructions
                        if (oldClaim.getArea() < 1000 && result.claim.getArea() >= 1000 && result.claim.children.size() == 0
                                && !player.hasPermission(GPPermissions.CLAIMS_ADMIN)) {
                            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BecomeMayor, 200L);
                            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, 201L);
                        }

                        // if in a creative mode world and shrinking an existing claim, restore any unclaimed area
                        if (smaller && GriefPrevention.instance.claimModeIsActive(oldClaim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                            GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                            GriefPrevention.instance.restoreClaim(oldClaim, 20L * 60 * 2); // 2 minutes
                            GriefPrevention.addLogEntry(player.getName() + " shrank a claim @ "
                                    + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.getLesserBoundaryCorner()));
                        }

                        // clean up
                        playerData.claimResizing = null;
                        playerData.lastShovelLocation = null;
                    } else {
                        if (result.claim != null) {
                            // inform player
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlap);

                            // show the player the conflicting claim
                            Visualization visualization =
                                    Visualization.FromClaim(result.claim, clickedBlock.getPosition().getY(), VisualizationType.ErrorClaim, player.getLocation());
                            Visualization.Apply(player, visualization);
                        } else {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapRegion);
                        }
                    }

                    return;
                }

                // otherwise, since not currently resizing a claim, must be starting
                // a resize, creating a new claim, or creating a subdivision

                // ignore height
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), true, playerData.lastClaim);
                // if within an existing claim, he's not creating a new one
                if (claim != null) {
                    // if the player has permission to edit the claim or subdivision
                    String noEditReason = claim.allowEdit(player);
                    if (noEditReason == null) {
                        // if he clicked on a corner, start resizing it
                        if ((clickedBlock.getPosition().getX() == claim.getLesserBoundaryCorner().getBlockX()
                                || clickedBlock.getPosition().getX() == claim.getGreaterBoundaryCorner().getBlockX())
                                && (clickedBlock.getPosition().getZ() == claim.getLesserBoundaryCorner().getBlockZ()
                                        || clickedBlock.getPosition().getZ() == claim.getGreaterBoundaryCorner().getBlockZ())) {
                            playerData.claimResizing = claim;
                            playerData.lastShovelLocation = clickedBlock.getLocation().get();
                            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ResizeStart);
                        }

                        // if he didn't click on a corner and is in subdivision
                        // mode, he's creating a new subdivision
                        else if (playerData.shovelMode == ShovelMode.Subdivide) {
                            // if it's the first click, he's trying to start a new
                            // subdivision
                            if (playerData.lastShovelLocation == null) {
                                // if the clicked claim was a subdivision, tell him
                                // he can't start a new subdivision here
                                if (claim.parent != null) {
                                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapSubdivision);
                                }

                                // otherwise start a new subdivision
                                else {
                                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionStart);
                                    playerData.lastShovelLocation = clickedBlock.getLocation().get();
                                    playerData.claimSubdividing = claim;
                                }
                            }

                            // otherwise, he's trying to finish creating a
                            // subdivision by setting the other boundary corner
                            else {
                                // if last shovel location was in a different world,
                                // assume the player is starting the create-claim
                                // workflow over
                                // TODO
                                /*if (!playerData.lastShovelLocation.getExtent().equals(clickedBlock.getLocation().get().getExtent())) {
                                    playerData.lastShovelLocation = null;
                                    this.onPlayerInteract(event);
                                    return;
                                }*/

                                // try to create a new claim (will return null if
                                // this subdivision overlaps another)
                                CreateClaimResult result = this.dataStore.createClaim(
                                        player.getWorld(),
                                        playerData.lastShovelLocation.getBlockX(), clickedBlock.getPosition().getX(),
                                        playerData.lastShovelLocation.getBlockY() - activeConfig.getConfig().claim.extendIntoGroundDistance,
                                        clickedBlock.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance,
                                        playerData.lastShovelLocation.getBlockZ(), clickedBlock.getPosition().getZ(),
                                        UUID.randomUUID(), playerData.claimSubdividing, player);

                                // if it didn't succeed, tell the player why
                                if (!result.succeeded) {
                                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateSubdivisionOverlap);

                                    Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getPosition().getY(), VisualizationType.ErrorClaim,
                                            player.getLocation());
                                    Visualization.Apply(player, visualization);

                                    return;
                                }

                                // otherwise, advise him on the /trust command and show him his new subdivision
                                else {
                                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.SubdivisionSuccess);
                                    Visualization visualization =
                                            Visualization.FromClaim(result.claim, clickedBlock.getPosition().getY(), VisualizationType.Claim, player.getLocation());
                                    Visualization.Apply(player, visualization);
                                    playerData.lastShovelLocation = null;
                                    playerData.claimSubdividing = null;
                                }
                            }
                        }

                        // otherwise tell him he can't create a claim here, and show him the existing claim
                        // also advise him to consider /abandonclaim or resizing the existing claim
                        else {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlap);
                            Visualization visualization =
                                    Visualization.FromClaim(claim, clickedBlock.getPosition().getY(), VisualizationType.Claim, player.getLocation());
                            Visualization.Apply(player, visualization);
                        }
                    }

                    // otherwise tell the player he can't claim here because it's
                    // someone else's claim, and show him the claim
                    else {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapOtherPlayer, claim.getOwnerName());
                        Visualization visualization =
                                Visualization.FromClaim(claim, clickedBlock.getPosition().getY(), VisualizationType.ErrorClaim, player.getLocation());
                        Visualization.Apply(player, visualization);
                    }

                    return;
                }

                // otherwise, the player isn't in an existing claim!

                // if he hasn't already start a claim with a previous shovel action
                Location<World> lastShovelLocation = playerData.lastShovelLocation;
                if (lastShovelLocation == null) {
                    // if he's at the claim count per player limit already and
                    // doesn't have permission to bypass, display an error message
                    if (activeConfig.getConfig().claim.maxClaimsPerPlayer > 0 &&
                            !player.hasPermission(GPPermissions.OVERRIDE_CLAIM_COUNT_LIMIT) &&
                            playerData.getClaims().size() >= activeConfig.getConfig().claim.maxClaimsPerPlayer) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimCreationFailedOverClaimCountLimit);
                        return;
                    }

                    // remember it, and start him on the new claim
                    playerData.lastShovelLocation = clickedBlock.getLocation().get();
                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ClaimStart);

                    // show him where he's working
                    VisualizationType visualType = VisualizationType.Claim;
                    if (playerData.shovelMode == ShovelMode.Admin) {
                        visualType = VisualizationType.AdminClaim;
                    }
                    Visualization visualization = Visualization.FromClaim(
                            new Claim(clickedBlock.getLocation().get(), clickedBlock.getLocation().get()),
                            clickedBlock.getPosition().getY(), visualType, player.getLocation());
                    Visualization.Apply(player, visualization);
                }

                // otherwise, he's trying to finish creating a claim by setting the
                // other boundary corner
                else {
                    // if last shovel location was in a different world, assume the
                    // player is starting the create-claim workflow over
                    // TODO
                    /*if (!lastShovelLocation.getExtent().equals(clickedBlock.getLocation().get().getExtent())) {
                        playerData.lastShovelLocation = null;
                        this.onPlayerInteract(event);
                        return;
                    }*/

                    // apply pvp rule
                    if (playerData.inPvpCombat(player.getWorld())) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoClaimDuringPvP);
                        return;
                    }

                    // apply minimum claim dimensions rule
                    int newClaimWidth = Math.abs(playerData.lastShovelLocation.getBlockX() - clickedBlock.getPosition().getX()) + 1;
                    int newClaimHeight = Math.abs(playerData.lastShovelLocation.getBlockZ() - clickedBlock.getPosition().getZ()) + 1;

                    if (playerData.shovelMode != ShovelMode.Admin) {
                        if (newClaimWidth < activeConfig.getConfig().claim.claimMinimumWidth
                                || newClaimHeight < activeConfig.getConfig().claim.claimMinimumWidth) {
                            // this IF block is a workaround for craftbukkit bug
                            // which fires two events for one interaction
                            if (newClaimWidth != 1 && newClaimHeight != 1) {
                                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NewClaimTooNarrow,
                                        String.valueOf(activeConfig.getConfig().claim.claimMinimumWidth));
                            }
                            return;
                        }

                        int newArea = newClaimWidth * newClaimHeight;
                        if (newArea < activeConfig.getConfig().claim.claimMinimumArea) {
                            if (newArea != 1) {
                                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeClaimInsufficientArea, String.valueOf(newArea), String.valueOf(newClaimWidth), String.valueOf(newClaimHeight),
                                        String.valueOf(activeConfig.getConfig().claim.claimMinimumArea));
                            }

                            return;
                        }
                    }

                    // if not an administrative claim, verify the player has enough
                    // claim blocks for this new claim
                    if (playerData.shovelMode != ShovelMode.Admin) {
                        int newClaimArea = newClaimWidth * newClaimHeight;
                        int remainingBlocks = playerData.getRemainingClaimBlocks();
                        if (newClaimArea > remainingBlocks) {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimInsufficientBlocks,
                                    String.valueOf(newClaimArea - remainingBlocks));
                            this.tryAdvertiseAdminAlternatives(player);
                            return;
                        }
                    } else {
                        playerID = null;
                    }

                    // try to create a new claim
                    CreateClaimResult result = this.dataStore.createClaim(
                            player.getWorld(),
                            lastShovelLocation.getBlockX(), clickedBlock.getPosition().getX(),
                            lastShovelLocation.getBlockY() - activeConfig.getConfig().claim.extendIntoGroundDistance,
                            clickedBlock.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance,
                            lastShovelLocation.getBlockZ(), clickedBlock.getPosition().getZ(),
                            UUID.randomUUID(), null, player);

                    // if it didn't succeed, tell the player why
                    if (!result.succeeded) {
                        if (result.claim != null) {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);

                            Visualization visualization =
                                    Visualization.FromClaim(result.claim, clickedBlock.getPosition().getY(), VisualizationType.ErrorClaim, player.getLocation());
                            Visualization.Apply(player, visualization);
                        } else {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapRegion);
                        }

                        return;
                    }

                    // otherwise, advise him on the /trust command and show him his new claim
                    else {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);
                        Visualization visualization =
                                Visualization.FromClaim(result.claim, clickedBlock.getPosition().getY(), VisualizationType.Claim, player.getLocation());
                        Visualization.Apply(player, visualization);
                        playerData.lastShovelLocation = null;

                        // if it's a big claim, tell the player about subdivisions
                        if (!player.hasPermission(GPPermissions.CLAIMS_ADMIN) && result.claim.getArea() >= 1000) {
                            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BecomeMayor, 200L);
                            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, 201L);
                        }

                        // auto-extend it downward to cover anything already built underground
                        Claim newClaim = result.claim;
                        Location<World> lesserCorner = newClaim.getLesserBoundaryCorner();
                        Location<World> greaterCorner = newClaim.getGreaterBoundaryCorner();
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

                        Sponge.getGame().getScheduler().createTaskBuilder().async().execute(new AutoExtendClaimTask(newClaim, snapshots, world.getDimension().getType())).submit(GriefPrevention.instance);
                    }
                }

        return;
        }
    }

    // helper methods for player events
    private void investigateClaim(Player player, BlockSnapshot clickedBlock, Optional<ItemStack> itemInHand) {
        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(player.getWorld().getProperties());

        // if he's investigating a claim
        if (!itemInHand.isPresent() || !itemInHand.get().getItem().getId().equals(activeConfig.getConfig().claim.investigationTool)) {
            return;
        }
        // if holding shift (sneaking), show all claims in area
        if (player.get(Keys.IS_SNEAKING).get() && player.hasPermission(GPPermissions.VISUALIZE_NEARBY_CLAIMS)) {
            // find nearby claims
            Set<Claim> claims = this.dataStore.getNearbyClaims(player.getLocation());
            // visualize boundaries
            Visualization visualization =
                    Visualization.fromClaims(claims, player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), VisualizationType.Claim, player.getLocation());
            Visualization.Apply(player, visualization);
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.ShowNearbyClaims, String.valueOf(claims.size()));
            return;
        }

        // FEATURE: shovel and stick can be used from a distance away
        if (clickedBlock.getState().getType() == BlockTypes.AIR) {
            // try to find a far away non-air block along line of sight
            clickedBlock = getTargetBlock(player, 100);
        }

        // if no block, stop here
        if (clickedBlock == null) {
            return;
        }

        // air indicates too far away
        if (clickedBlock.getState().getType() == BlockTypes.AIR) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
            Visualization.Revert(player);
            return;
        }

        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (playerData == null) {
            playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        }
        // ignore height
        Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get(), false, playerData.lastClaim);

        // no claim case
        if (claim == null) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockNotClaimed);
            Visualization.Revert(player);
        } else {
        // claim case
            playerData.lastClaim = claim;
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockClaimed, claim.getOwnerName());

            // visualize boundary
            Visualization visualization =
                    Visualization.FromClaim(claim, player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), VisualizationType.ClaimInvestigation, player.getLocation());
            Visualization.Apply(player, visualization);

            // if can resize this claim, tell about the boundaries
            if (claim.allowEdit(player) == null) {
                // TODO
                //GriefPrevention.sendMessage(player, TextMode.Info, "", "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
            }

            // if deleteclaims permission, show last active claim date
            if (!claim.isAdminClaim() && player.hasPermission(GPPermissions.DELETE_CLAIMS)) {
                Instant lastActive = Instant.parse(claim.getClaimData().getLastActiveDate());
                long difference = Date.from(Instant.now()).getTime() - Date.from(lastActive).getTime();
                long daysElapsed = difference / (1000 * 60 * 60 * 24);
                GriefPrevention.sendMessage(player, TextMode.Info, Messages.ClaimLastActive, Long.toString(daysElapsed));

                // drop the data we just loaded, if the player isn't online
                if (!Sponge.getGame().getServer().getPlayer(claim.ownerID).isPresent()) {
                    this.dataStore.clearCachedPlayerData(claim.world.getProperties(), claim.ownerID);
                }
            }
        }
    }

    private boolean onLeftClickWatchList(BlockType blockType) {
        if (blockType == BlockTypes.WOODEN_BUTTON || blockType == BlockTypes.STONE_BUTTON || blockType == BlockTypes.LEVER
         || blockType == BlockTypes.POWERED_REPEATER || blockType == BlockTypes.UNPOWERED_REPEATER
         || blockType == BlockTypes.CAKE || blockType == BlockTypes.DRAGON_EGG) {
            return true;
        } else {
            return false;
        }
    }

    static BlockSnapshot getTargetBlock(Player player, int maxDistance) throws IllegalStateException {
        BlockRay<World> blockRay = BlockRay.from(player).blockLimit(maxDistance).build();

        while (blockRay.hasNext()) {
            BlockRayHit<World> blockRayHit = blockRay.next();
            if (blockRayHit.getLocation().getBlockType() != BlockTypes.AIR &&
                blockRayHit.getLocation().getBlockType() != BlockTypes.WATER &&
                blockRayHit.getLocation().getBlockType() != BlockTypes.TALLGRASS) {
                    return blockRayHit.getLocation().createSnapshot();
            }
        }

        return null;
    }
}
