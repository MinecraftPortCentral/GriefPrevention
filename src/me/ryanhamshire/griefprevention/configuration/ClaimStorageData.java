package me.ryanhamshire.griefprevention.configuration;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.Claim.Type;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Functional;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.IpSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class ClaimStorageData {

    public static final String HEADER = "12.1.7\n"
            + "# If you need help with the configuration or have any questions related to GriefPrevention,\n"
            + "# join us at the IRC or drop by our forums and leave a post.\n"
            + "# IRC: #spongedev @ irc.esper.net ( http://webchat.esper.net/?channel=spongedev )\n"
            + "# Forums: https://forums.spongepowered.org/\n";

    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(HEADER));
    private ObjectMapper<ClaimDataNode>.BoundInstance configMapper;
    private ClaimDataNode configBase;
    public Path filePath;

    // MAIN
    public static final String MAIN_WORLD_UUID = "world-uuid";
    public static final String MAIN_OWNER_UUID = "owner-uuid";
    public static final String MAIN_CLAIM_NAME = "claim-name";
    public static final String MAIN_CLAIM_GREETING = "claim-greeting";
    public static final String MAIN_CLAIM_FAREWELL = "claim-farewell";
    public static final String MAIN_CLAIM_TYPE = "claim-type";
    public static final String MAIN_CLAIM_LAST_ACTIVE = "date-last-active";
    public static final String MAIN_SUBDIVISION_UUID = "uuid";
    public static final String MAIN_PARENT_CLAIM_UUID = "parent-claim-uuid";
    public static final String MAIN_LESSER_BOUNDARY_CORNER = "lesser-boundary-corner";
    public static final String MAIN_GREATER_BOUNDARY_CORNER = "greater-boundary-corner";
    public static final String MAIN_ACCESSORS = "accessors";
    public static final String MAIN_BUILDERS = "builders";
    public static final String MAIN_CONTAINERS = "managers";
    public static final String MAIN_COOWNERS = "coowners";
    public static final String MAIN_PROTECTION_BLACKLIST = "bypass-protection-items";
    public static final String MAIN_SUBDIVISIONS = "sub-divisions";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path) {
        this.filePath = path;
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(ClaimDataNode.class).bindToNew();

            reload();
            save();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public ClaimDataNode getConfig() {
        return this.configBase;
    }

    public void save() {
        try {
            this.configMapper.serialize(this.root.getNode(GriefPrevention.MOD_ID));
            this.loader.save(this.root);
        } catch (IOException | ObjectMappingException e) {
            SpongeImpl.getLogger().error("Failed to save configuration", e);
        }
    }

    public void reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults()
                    .setSerializers(
                            TypeSerializers.getDefaultSerializers().newChild().registerType(TypeToken.of(IpSet.class), new IpSet.IpSetSerializer()))
                    .setHeader(HEADER));
            this.configBase = this.configMapper.populate(this.root.getNode(GriefPrevention.MOD_ID));
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to load configuration", e);
        }
    }

    public CompletableFuture<CommentedConfigurationNode> updateSetting(String key, Object value) {
        return Functional.asyncFailableFuture(() -> {
            CommentedConfigurationNode upd = getSetting(key);
            upd.setValue(value);
            this.configBase = this.configMapper.populate(this.root.getNode(GriefPrevention.MOD_ID));
            this.loader.save(this.root);
            return upd;
        }, ForkJoinPool.commonPool());
    }

    public CommentedConfigurationNode getRootNode() {
        return this.root.getNode(GriefPrevention.MOD_ID);
    }

    public CommentedConfigurationNode getSetting(String key) {
        if (!key.contains(".") || key.indexOf('.') == key.length() - 1) {
            return null;
        } else {
            String category = key.substring(0, key.indexOf('.'));
            String prop = key.substring(key.indexOf('.') + 1);
            return getRootNode().getNode(category).getNode(prop);
        }
    }

    public interface ClaimData {
        public UUID getWorldUniqueId();

        public UUID getOwnerUniqueId();

        public Claim.Type getClaimType();

        public String getLastActiveDate();

        public Text getClaimName();

        public Text getGreetingMessage();

        public Text getFarewellMessage();

        public String getLesserBoundaryCorner();

        public String getGreaterBoundaryCorner();

        public List<UUID> getAccessors();

        public List<UUID> getBuilders();

        public List<UUID> getContainers();

        public List<UUID> getCoowners();

        public List<String> getItemBlackList();

        public ClaimDataFlagsCategory getFlags();

        public void setClaimType(Claim.Type type);

        public void setLastActiveDate(String date);

        public void setClaimName(Text name);

        public void setGreetingMessage(Text message);

        public void setFarewellMessage(Text message);

        public void setLesserBoundaryCorner(String location);

        public void setGreaterBoundaryCorner(String location);
    }

    @ConfigSerializable
    public static class ClaimDataNode implements ClaimData {
        @Setting(value = MAIN_WORLD_UUID, comment = "The world uuid associated with claim.")
        public UUID worldUniqueId;
        @Setting(value = MAIN_OWNER_UUID, comment = "The owner uuid assocated with claim.")
        public UUID ownerUniqueId;
        @Setting(value = MAIN_CLAIM_TYPE, comment = "The type of claim.")
        public Claim.Type claimType = Claim.Type.BASIC;
        @Setting(value = MAIN_CLAIM_LAST_ACTIVE, comment = "The last time this claim was active.")
        public String lastActiveDate = Instant.now().toString();
        @Setting(value = MAIN_CLAIM_NAME, comment = "The name associated with claim.")
        public Text claimName = Text.of("");
        @Setting(value = MAIN_CLAIM_GREETING, comment = "The greeting message players will receive when entering claim area.")
        public Text claimGreetingMessage = Text.of("");
        @Setting(value = MAIN_CLAIM_FAREWELL, comment = "The farewell message players will receive when leaving claim area.")
        public Text claimFarewellMessage = Text.of("");
        @Setting(value = MAIN_LESSER_BOUNDARY_CORNER, comment = "The lesser boundary corner location of claim.")
        public String lesserBoundaryCornerPos;
        @Setting(value = MAIN_GREATER_BOUNDARY_CORNER, comment = "The greater boundary corner location of claim.")
        public String greaterBoundaryCornerPos;
        @Setting(value = MAIN_ACCESSORS, comment = "The accessors associated with claim. Note: Accessors can interact with all blocks except inventory containers like chests.")
        public List<UUID> accessors = new ArrayList<>();
        @Setting(value = MAIN_BUILDERS, comment = "The builders associated with claim. Note: Builders can do everything accessors and containers do with the addition of placing and breaking blocks.")
        public List<UUID> builders = new ArrayList<>();
        @Setting(value = MAIN_CONTAINERS, comment = "The containers associated with claim. Note: Containers can do everything accessors with the addition of inventory access.")
        public List<UUID> containers = new ArrayList<>();
        @Setting(value = MAIN_COOWNERS, comment = "The coowners associated with claim. Note: Use this type with caution as it provides full access in claim.")
        public List<UUID> coowners = new ArrayList<>();
        @Setting(value = MAIN_PROTECTION_BLACKLIST, comment = "Item id's that are not protected within claim.")
        public List<String> protectionBlacklist = new ArrayList<>();
        @Setting
        public ClaimDataFlagsCategory flags = new ClaimDataFlagsCategory();
        @Setting
        public Map<UUID, SubDivisionDataNode> subdivisions = Maps.newHashMap();

        @Override
        public UUID getWorldUniqueId() {
            return this.worldUniqueId;
        }
        @Override
        public UUID getOwnerUniqueId() {
            return this.ownerUniqueId;
        }
        @Override
        public Type getClaimType() {
            return this.claimType;
        }
        @Override
        public String getLastActiveDate() {
            return this.lastActiveDate;
        }
        @Override
        public Text getClaimName() {
            return this.claimName;
        }
        @Override
        public Text getGreetingMessage() {
            return this.claimGreetingMessage;
        }
        @Override
        public Text getFarewellMessage() {
            return this.claimFarewellMessage;
        }
        @Override
        public String getLesserBoundaryCorner() {
            return this.lesserBoundaryCornerPos;
        }
        @Override
        public String getGreaterBoundaryCorner() {
            return this.greaterBoundaryCornerPos;
        }
        @Override
        public List<UUID> getAccessors() {
            return this.accessors;
        }
        @Override
        public List<UUID> getBuilders() {
            return this.builders;
        }
        @Override
        public List<UUID> getContainers() {
            return this.containers;
        }
        @Override
        public List<UUID> getCoowners() {
            return this.coowners;
        }
        @Override
        public List<String> getItemBlackList() {
            return this.protectionBlacklist;
        }
        @Override
        public ClaimDataFlagsCategory getFlags() {
            return this.flags;
        }
        @Override
        public void setClaimType(Type type) {
            this.claimType = type;
        }
        @Override
        public void setLastActiveDate(String date) {
            this.lastActiveDate = date;
        }
        @Override
        public void setClaimName(Text name) {
            this.claimName = name;
        }
        @Override
        public void setGreetingMessage(Text message) {
            this.claimGreetingMessage = message;
        }
        @Override
        public void setFarewellMessage(Text message) {
            this.claimFarewellMessage = message;
        }
        @Override
        public void setLesserBoundaryCorner(String location) {
            this.lesserBoundaryCornerPos = location;
        }
        @Override
        public void setGreaterBoundaryCorner(String location) {
            this.greaterBoundaryCornerPos = location;
        }
    }

    @ConfigSerializable
    public static class SubDivisionDataNode implements ClaimData {
        @Setting(value = MAIN_WORLD_UUID, comment = "The world uuid associated with claim.")
        public UUID worldUniqueId;
        @Setting(value = MAIN_OWNER_UUID, comment = "The owner uuid assocated with claim.")
        public UUID ownerUniqueId;
        @Setting(value = MAIN_CLAIM_NAME, comment = "The name associated with subdivision.")
        public Text claimName = Text.of("");
        @Setting(value = MAIN_CLAIM_TYPE, comment = "The type of claim.")
        public Claim.Type claimType = Claim.Type.SUBDIVISION;
        @Setting(value = MAIN_CLAIM_LAST_ACTIVE, comment = "The last time this subdivision was used.")
        public String lastUsedDate = Instant.now().toString();
        @Setting(value = MAIN_CLAIM_GREETING, comment = "The greeting message players will receive when entering subdivision.")
        public Text claimGreetingMessage = Text.of("");
        @Setting(value = MAIN_CLAIM_FAREWELL, comment = "The farewell message players will receive when leaving subdivision.")
        public Text claimFarewellMessage = Text.of("");
        @Setting(value = MAIN_LESSER_BOUNDARY_CORNER, comment = "The lesser boundary corner location of subdivision.")
        public String lesserBoundaryCornerPos;
        @Setting(value = MAIN_GREATER_BOUNDARY_CORNER, comment = "The greater boundary corner location of subdivision.")
        public String greaterBoundaryCornerPos;
        @Setting(value = MAIN_ACCESSORS, comment = "The accessors associated with subdivision.")
        public ArrayList<UUID> accessors = new ArrayList<>();
        @Setting(value = MAIN_BUILDERS, comment = "The builders associated with subdivision.")
        public ArrayList<UUID> builders = new ArrayList<>();
        @Setting(value = MAIN_CONTAINERS, comment = "The containers associated with subdivision.")
        public ArrayList<UUID> containers = new ArrayList<>();
        @Setting(value = MAIN_COOWNERS, comment = "The coowners associated with subdivision.")
        public ArrayList<UUID> coowners = new ArrayList<>();
        @Setting(value = MAIN_PROTECTION_BLACKLIST, comment = "Item id's that are not protected within subdivision.")
        public ArrayList<String> protectionBlacklist = new ArrayList<>();
        @Setting
        public ClaimDataFlagsCategory flags = new ClaimDataFlagsCategory();

        @Override
        public UUID getWorldUniqueId() {
            return null;
        }
        @Override
        public UUID getOwnerUniqueId() {
            return this.ownerUniqueId;
        }
        @Override
        public Type getClaimType() {
            return this.claimType;
        }
        @Override
        public String getLastActiveDate() {
            return this.lastUsedDate;
        }
        @Override
        public Text getClaimName() {
            return this.claimName;
        }
        @Override
        public Text getGreetingMessage() {
            return this.claimGreetingMessage;
        }
        @Override
        public Text getFarewellMessage() {
            return this.claimFarewellMessage;
        }
        @Override
        public String getLesserBoundaryCorner() {
            return this.lesserBoundaryCornerPos;
        }
        @Override
        public String getGreaterBoundaryCorner() {
            return this.greaterBoundaryCornerPos;
        }
        @Override
        public List<UUID> getAccessors() {
            return this.accessors;
        }
        @Override
        public List<UUID> getBuilders() {
            return this.builders;
        }
        @Override
        public List<UUID> getContainers() {
            return this.containers;
        }
        @Override
        public List<UUID> getCoowners() {
            return this.coowners;
        }
        @Override
        public List<String> getItemBlackList() {
            return this.protectionBlacklist;
        }
        @Override
        public ClaimDataFlagsCategory getFlags() {
            return this.flags;
        }
        @Override
        public void setClaimType(Type type) {
            this.claimType = type;
        }
        @Override
        public void setLastActiveDate(String date) {
            this.lastUsedDate = date;
        }
        @Override
        public void setClaimName(Text name) {
            this.claimName = name;
        }
        @Override
        public void setGreetingMessage(Text message) {
            this.claimGreetingMessage = message;
        }
        @Override
        public void setFarewellMessage(Text message) {
            this.claimFarewellMessage = message;
        }
        @Override
        public void setLesserBoundaryCorner(String location) {
            this.lesserBoundaryCornerPos = location;
        }
        @Override
        public void setGreaterBoundaryCorner(String location) {
            this.greaterBoundaryCornerPos = location;
        }
    }

    @ConfigSerializable
    public static class ClaimDataFlagsCategory extends Category {

        @Setting(value = GPFlags.BLOCK_BREAK, comment = GPFlags.COMMENT_BLOCK_BREAK)
        public Tristate blockBreak = Tristate.UNDEFINED;
        @Setting(value = GPFlags.BLOCK_COMMANDS, comment = GPFlags.COMMENT_BLOCK_COMMANDS)
        public List<String> blockCommands = new ArrayList<>();
        @Setting(value = GPFlags.BLOCK_PLACE, comment = GPFlags.COMMENT_BLOCK_PLACE)
        public Tristate blockPlace = Tristate.UNDEFINED;
        @Setting(value = GPFlags.EXPLOSIONS, comment = GPFlags.COMMENT_EXPLOSIONS)
        public Tristate explosions = Tristate.UNDEFINED;
        @Setting(value = GPFlags.FAREWELL_MESSAGE, comment = GPFlags.COMMENT_FAREWELL_MESSAGE)
        public Tristate farewellMessage = Tristate.UNDEFINED;
        @Setting(value = GPFlags.FIRE_SPREAD, comment = GPFlags.COMMENT_FIRE_SPREAD)
        public Tristate fireSpread = Tristate.UNDEFINED;
        @Setting(value = GPFlags.FORCE_DENY_ALL, comment = GPFlags.COMMENT_FORCE_DENY_ALL)
        public Tristate forceDenyAll = Tristate.UNDEFINED;
        @Setting(value = GPFlags.GREETING_MESSAGE, comment = GPFlags.COMMENT_GREETING_MESSAGE)
        public Tristate greetingMessage = Tristate.UNDEFINED;
        @Setting(value = GPFlags.INTERACT_PRIMARY, comment = GPFlags.COMMENT_INTERACT_PRIMARY)
        public Tristate interactPrimary = Tristate.UNDEFINED;
        @Setting(value = GPFlags.INTERACT_SECONDARY, comment = GPFlags.COMMENT_INTERACT_SECONDARY)
        public Tristate interactSecondary = Tristate.UNDEFINED;
        @Setting(value = GPFlags.INTERACT_INVENTORY, comment = GPFlags.COMMENT_INTERACT_INVENTORY)
        public Tristate interactInventory = Tristate.UNDEFINED;
        @Setting(value = GPFlags.ITEM_DROP, comment = GPFlags.COMMENT_ITEM_DROP)
        public Tristate itemDrop = Tristate.UNDEFINED;
        @Setting(value = GPFlags.ITEM_PICKUP, comment = GPFlags.COMMENT_ITEM_PICKUP)
        public Tristate itemPickup = Tristate.UNDEFINED;
        @Setting(value = GPFlags.ITEM_USE, comment = GPFlags.COMMENT_ITEM_USE)
        public Tristate itemUse = Tristate.UNDEFINED;
        @Setting(value = GPFlags.LAVA_FLOW, comment = GPFlags.COMMENT_LAVA_FLOW)
        public Tristate lavaFlow = Tristate.UNDEFINED;
        @Setting(value = GPFlags.MOB_BLOCK_DAMAGE, comment = GPFlags.COMMENT_MOB_BLOCK_DAMAGE)
        public Tristate mobBlockDamage = Tristate.UNDEFINED;
        @Setting(value = GPFlags.MOB_PLAYER_DAMAGE, comment = GPFlags.COMMENT_MOB_PLAYER_DAMAGE)
        public Tristate mobPlayerDamage = Tristate.UNDEFINED;
        @Setting(value = GPFlags.MOB_RIDING, comment = GPFlags.COMMENT_MOB_RIDING)
        public Tristate mobRiding = Tristate.UNDEFINED;
        @Setting(value = GPFlags.PORTAL_USE, comment = GPFlags.COMMENT_PORTAL_USE)
        public Tristate portalUse = Tristate.UNDEFINED;
        @Setting(value = GPFlags.PROJECTILES_ANY, comment = GPFlags.COMMENT_PROJECTILES_ANY)
        public Tristate projectilesAny = Tristate.UNDEFINED;
        @Setting(value = GPFlags.PROJECTILES_MONSTER, comment = GPFlags.COMMENT_PROJECTILES_MONSTER)
        public Tristate projectilesMonster = Tristate.UNDEFINED;
        @Setting(value = GPFlags.PROJECTILES_PLAYER, comment = GPFlags.COMMENT_PROJECTILES_PLAYER)
        public Tristate projectilesPlayer = Tristate.UNDEFINED;
        @Setting(value = GPFlags.PVP, comment = GPFlags.COMMENT_PVP)
        public Tristate pvp = Tristate.UNDEFINED;
        @Setting(value = GPFlags.SLEEP, comment = GPFlags.COMMENT_SLEEP)
        public Tristate sleep = Tristate.UNDEFINED;
        @Setting(value = GPFlags.SPAWN_AMBIENTS, comment = GPFlags.COMMENT_SPAWN_AMBIENTS)
        public Tristate spawnAmbient = Tristate.UNDEFINED;
        @Setting(value = GPFlags.SPAWN_ANY, comment = GPFlags.COMMENT_SPAWN_ANY)
        public Tristate spawnAny = Tristate.UNDEFINED;
        @Setting(value = GPFlags.SPAWN_AQUATICS, comment = GPFlags.COMMENT_SPAWN_AQUATICS)
        public Tristate spawnAquatic = Tristate.UNDEFINED;
        @Setting(value = GPFlags.SPAWN_MONSTERS, comment = GPFlags.COMMENT_SPAWN_MONSTERS)
        public Tristate spawnMonsters = Tristate.UNDEFINED;
        @Setting(value = GPFlags.SPAWN_PASSIVES, comment = GPFlags.COMMENT_SPAWN_PASSIVES)
        public Tristate spawnPassives = Tristate.UNDEFINED;
        @Setting(value = GPFlags.VILLAGER_TRADING, comment = GPFlags.COMMENT_VILLAGER_TRADING)
        public Tristate villagerTrading = Tristate.UNDEFINED;
        @Setting(value = GPFlags.WATER_FLOW, comment = GPFlags.COMMENT_WATER_FLOW)
        public Tristate waterFlow = Tristate.UNDEFINED;

        public Map<String, Object> getFlagMap() {
            Map<String, Object> flagMap = Maps.newHashMap();
            flagMap.put(GPFlags.BLOCK_BREAK, this.blockBreak);
            flagMap.put(GPFlags.BLOCK_COMMANDS, this.blockCommands);
            flagMap.put(GPFlags.BLOCK_PLACE, this.blockPlace);
            flagMap.put(GPFlags.EXPLOSIONS, this.explosions);
            flagMap.put(GPFlags.FIRE_SPREAD, this.fireSpread);
            flagMap.put(GPFlags.FORCE_DENY_ALL, this.forceDenyAll);
            flagMap.put(GPFlags.INTERACT_PRIMARY, this.interactPrimary);
            flagMap.put(GPFlags.INTERACT_SECONDARY, this.interactSecondary);
            flagMap.put(GPFlags.INTERACT_INVENTORY, this.interactInventory);
            flagMap.put(GPFlags.ITEM_DROP, this.itemDrop);
            flagMap.put(GPFlags.ITEM_PICKUP, this.itemPickup);
            flagMap.put(GPFlags.ITEM_USE, this.itemUse);
            flagMap.put(GPFlags.LAVA_FLOW, this.lavaFlow);
            flagMap.put(GPFlags.MOB_BLOCK_DAMAGE, this.mobBlockDamage);
            flagMap.put(GPFlags.MOB_PLAYER_DAMAGE, this.mobPlayerDamage);
            flagMap.put(GPFlags.MOB_RIDING, this.mobRiding);
            flagMap.put(GPFlags.PORTAL_USE, this.portalUse);
            flagMap.put(GPFlags.PROJECTILES_PLAYER, this.projectilesPlayer);
            flagMap.put(GPFlags.PROJECTILES_MONSTER, this.projectilesMonster);
            flagMap.put(GPFlags.PROJECTILES_ANY, this.projectilesAny);
            flagMap.put(GPFlags.PVP, this.pvp);
            flagMap.put(GPFlags.SPAWN_MONSTERS, this.spawnMonsters);
            flagMap.put(GPFlags.SPAWN_PASSIVES, this.spawnPassives);
            flagMap.put(GPFlags.SPAWN_AMBIENTS, this.spawnAmbient);
            flagMap.put(GPFlags.SPAWN_AQUATICS, this.spawnAquatic);
            flagMap.put(GPFlags.SPAWN_ANY, this.spawnAny);
            flagMap.put(GPFlags.SLEEP, this.sleep);
            flagMap.put(GPFlags.WATER_FLOW, this.waterFlow);
            flagMap.put(GPFlags.VILLAGER_TRADING, this.villagerTrading);
            return flagMap;
        }

        @SuppressWarnings("unchecked")
        public void setFlagValue(String flag, Object value) {
            switch (flag) {
                case GPFlags.BLOCK_BREAK:
                    this.blockBreak = (Tristate) value;
                    return;
                case GPFlags.BLOCK_COMMANDS:
                    this.blockCommands = (List<String>) value;
                    return;
                case GPFlags.BLOCK_PLACE:
                    this.blockPlace = (Tristate) value;
                    return;
                case GPFlags.EXPLOSIONS:
                    this.explosions = (Tristate) value;
                    return;
                case GPFlags.FAREWELL_MESSAGE:
                    this.farewellMessage = (Tristate) value;
                    return;
                case GPFlags.FIRE_SPREAD:
                    this.fireSpread = (Tristate) value;
                    return;
                case GPFlags.FORCE_DENY_ALL:
                    this.forceDenyAll = (Tristate) value;
                    return;
                case GPFlags.GREETING_MESSAGE:
                    this.greetingMessage = (Tristate) value;
                    return;
                case GPFlags.INTERACT_PRIMARY:
                    this.interactPrimary = (Tristate) value;
                    return;
                case GPFlags.INTERACT_SECONDARY:
                    this.interactSecondary = (Tristate) value;
                    return;
                case GPFlags.INTERACT_INVENTORY:
                    this.interactInventory = (Tristate) value;
                    return;
                case GPFlags.ITEM_DROP:
                    this.itemDrop = (Tristate) value;
                    return;
                case GPFlags.ITEM_PICKUP:
                    this.itemPickup = (Tristate) value;
                    return;
                case GPFlags.ITEM_USE:
                    this.itemUse = (Tristate) value;
                    return;
                case GPFlags.LAVA_FLOW:
                    this.lavaFlow = (Tristate) value;
                    return;
                case GPFlags.MOB_BLOCK_DAMAGE:
                    this.mobBlockDamage = (Tristate) value;
                    return;
                case GPFlags.MOB_PLAYER_DAMAGE:
                    this.mobPlayerDamage = (Tristate) value;
                    return;
                case GPFlags.MOB_RIDING:
                    this.mobRiding = (Tristate) value;
                    return;
                case GPFlags.PORTAL_USE:
                    this.portalUse = (Tristate) value;
                    return;
                case GPFlags.PROJECTILES_ANY:
                    this.projectilesAny = (Tristate) value;
                    return;
                case GPFlags.PROJECTILES_MONSTER:
                    this.projectilesMonster = (Tristate) value;
                    return;
                case GPFlags.PROJECTILES_PLAYER:
                    this.projectilesPlayer = (Tristate) value;
                    return;
                case GPFlags.PVP:
                    this.pvp = (Tristate) value;
                    return;
                case GPFlags.SLEEP:
                    this.sleep = (Tristate) value;
                    return;
                case GPFlags.SPAWN_AMBIENTS:
                    this.spawnAmbient = (Tristate) value;
                    return;
                case GPFlags.SPAWN_ANY:
                    this.spawnAny = (Tristate) value;
                    return;
                case GPFlags.SPAWN_AQUATICS:
                    this.spawnAquatic = (Tristate) value;
                    return;
                case GPFlags.SPAWN_MONSTERS:
                    this.spawnMonsters = (Tristate) value;
                    return;
                case GPFlags.SPAWN_PASSIVES:
                    this.spawnPassives = (Tristate) value;
                    return;
                case GPFlags.WATER_FLOW:
                    this.waterFlow = (Tristate) value;
                    return;
                case GPFlags.VILLAGER_TRADING:
                    this.villagerTrading = (Tristate) value;
                    return;
                default:
                    return;
            }
        }

        public Object getFlagValue(String flag) {
            switch (flag) {
                case GPFlags.BLOCK_BREAK:
                    return this.blockBreak;
                case GPFlags.BLOCK_COMMANDS:
                    return this.blockCommands;
                case GPFlags.BLOCK_PLACE:
                    return this.blockPlace;
                case GPFlags.EXPLOSIONS:
                    return this.explosions;
                case GPFlags.FAREWELL_MESSAGE:
                    return this.farewellMessage;
                case GPFlags.FIRE_SPREAD:
                    return this.fireSpread;
                case GPFlags.FORCE_DENY_ALL:
                    return this.forceDenyAll;
                case GPFlags.GREETING_MESSAGE:
                    return this.greetingMessage;
                case GPFlags.INTERACT_PRIMARY:
                    return this.interactPrimary;
                case GPFlags.INTERACT_SECONDARY:
                    return this.interactSecondary;
                case GPFlags.INTERACT_INVENTORY:
                    return this.interactInventory;
                case GPFlags.ITEM_DROP:
                    return this.itemDrop;
                case GPFlags.ITEM_PICKUP:
                    return this.itemPickup;
                case GPFlags.ITEM_USE:
                    return this.itemUse;
                case GPFlags.LAVA_FLOW:
                    return this.lavaFlow;
                case GPFlags.MOB_BLOCK_DAMAGE:
                    return this.mobBlockDamage;
                case GPFlags.MOB_PLAYER_DAMAGE:
                    return this.mobPlayerDamage;
                case GPFlags.MOB_RIDING:
                    return this.mobRiding;
                case GPFlags.PORTAL_USE:
                    return this.portalUse;
                case GPFlags.PROJECTILES_ANY:
                    return this.projectilesAny;
                case GPFlags.PROJECTILES_MONSTER:
                    return this.projectilesMonster;
                case GPFlags.PROJECTILES_PLAYER:
                    return this.projectilesPlayer;
                case GPFlags.PVP:
                    return this.pvp;
                case GPFlags.SLEEP:
                    return this.sleep;
                case GPFlags.SPAWN_AMBIENTS:
                    return this.spawnAmbient;
                case GPFlags.SPAWN_ANY:
                    return this.spawnAny;
                case GPFlags.SPAWN_AQUATICS:
                    return this.spawnAquatic;
                case GPFlags.SPAWN_MONSTERS:
                    return this.spawnMonsters;
                case GPFlags.SPAWN_PASSIVES:
                    return this.spawnPassives;
                case GPFlags.WATER_FLOW:
                    return this.waterFlow;
                case GPFlags.VILLAGER_TRADING:
                    return this.villagerTrading;
                default:
                    return null;
            }
        }
    }

    @ConfigSerializable
    private static class Category {

    }
}
