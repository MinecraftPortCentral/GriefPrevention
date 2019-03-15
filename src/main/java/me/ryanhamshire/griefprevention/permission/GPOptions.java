package me.ryanhamshire.griefprevention.permission;

import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimBlockSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GPOptions {

    public enum Type {
        ABANDON_RETURN_RATIO,
        MAX_CLAIM_SIZE_X,
        MAX_CLAIM_SIZE_Y,
        MAX_CLAIM_SIZE_Z,
        MIN_CLAIM_SIZE_X,
        MIN_CLAIM_SIZE_Y,
        MIN_CLAIM_SIZE_Z,
        CLAIM_EXPIRATION,
        CLAIM_LIMIT,
        EXPIRATION_DAYS_KEEP,
        TAX_EXPIRATION,
        TAX_RATE
    }

    public static final Map<String, Double> DEFAULT_OPTIONS = Maps.newHashMap();
    // All options in this list can only be used globally
    public static final List<String> GLOBAL_OPTIONS = new ArrayList<>();

    public static final String INVALID_OPTION = "invalid-option";
    public static final String ABANDON_RETURN_RATIO_BASIC = "griefprevention.abandon-return-ratio-basic";
    public static final String ABANDON_RETURN_RATIO_TOWN = "griefprevention.abandon-return-ratio-town";
    public static final String BLOCKS_ACCRUED_PER_HOUR = "griefprevention.blocks-accrued-per-hour";
    public static final String CREATE_CLAIM_LIMIT_BASIC = "griefprevention.create-claim-limit-basic";
    public static final String CREATE_CLAIM_LIMIT_SUBDIVISION = "griefprevention.create-claim-limit-subdivision";
    public static final String CREATE_CLAIM_LIMIT_TOWN = "griefprevention.create-claim-limit-town";
    public static final String INITIAL_CLAIM_BLOCKS = "griefprevention.initial-claim-blocks";
    public static final String RADIUS_CLAIM_LIST = "griefprevention.radius-claim-list";
    public static final String RADIUS_CLAIM_INSPECT = "griefprevention.radius-claim-inspect";
    public static final String MAX_ACCRUED_BLOCKS = "griefprevention.max-accrued-claim-blocks";
    public static final String MAX_CLAIM_LEVEL = "griefprevention.max-claim-level";
    public static final String MAX_CLAIM_SIZE_BASIC_X = "griefprevention.max-claim-size-basic-x";
    public static final String MAX_CLAIM_SIZE_BASIC_Y = "griefprevention.max-claim-size-basic-y";
    public static final String MAX_CLAIM_SIZE_BASIC_Z = "griefprevention.max-claim-size-basic-z";
    public static final String MAX_CLAIM_SIZE_SUBDIVISION_X = "griefprevention.max-claim-size-subdivision-x";
    public static final String MAX_CLAIM_SIZE_SUBDIVISION_Y = "griefprevention.max-claim-size-subdivision-y";
    public static final String MAX_CLAIM_SIZE_SUBDIVISION_Z = "griefprevention.max-claim-size-subdivision-z";
    public static final String MAX_CLAIM_SIZE_TOWN_X = "griefprevention.max-claim-size-town-x";
    public static final String MAX_CLAIM_SIZE_TOWN_Y = "griefprevention.max-claim-size-town-y";
    public static final String MAX_CLAIM_SIZE_TOWN_Z = "griefprevention.max-claim-size-town-z";
    public static final String MIN_CLAIM_LEVEL = "griefprevention.min-claim-level";
    public static final String MIN_CLAIM_SIZE_BASIC_X = "griefprevention.min-claim-size-basic-x";
    public static final String MIN_CLAIM_SIZE_BASIC_Y = "griefprevention.min-claim-size-basic-y";
    public static final String MIN_CLAIM_SIZE_BASIC_Z = "griefprevention.min-claim-size-basic-z";
    public static final String MIN_CLAIM_SIZE_TOWN_X = "griefprevention.min-claim-size-town-x";
    public static final String MIN_CLAIM_SIZE_TOWN_Y = "griefprevention.min-claim-size-town-y";
    public static final String MIN_CLAIM_SIZE_TOWN_Z = "griefprevention.min-claim-size-town-z";
    public static final String CLAIM_CREATE_MODE = "griefprevention.claim-create-mode";
    public static final String CLAIM_EXPIRATION_CHEST = "griefprevention.claim-expiration-chest";
    public static final String CLAIM_EXPIRATION_BASIC = "griefprevention.claim-expiration-basic";
    public static final String CLAIM_EXPIRATION_TOWN = "griefprevention.claim-expiration-town";
    public static final String CLAIM_EXPIRATION_SUBDIVISION = "griefprevention.claim-expiration-subdivision";
    public static final String TAX_EXPIRATION_BASIC = "griefprevention.tax-expiration-basic";
    public static final String TAX_EXPIRATION_SUBDIVISION = "griefprevention.tax-expiration-subdivision";
    public static final String TAX_EXPIRATION_TOWN = "griefprevention.tax-expiration-town";
    public static final String TAX_EXPIRATION_BASIC_DAYS_KEEP = "griefprevention.tax-expiration-basic-days-keep";
    public static final String TAX_EXPIRATION_SUBDIVISION_DAYS_KEEP = "griefprevention.tax-expiration-subdivision-days-keep";
    public static final String TAX_EXPIRATION_TOWN_DAYS_KEEP = "griefprevention.tax-expiration-town-days-keep";
    public static final String TAX_RATE_BASIC = "griefprevention.tax-rate-basic";
    public static final String TAX_RATE_SUBDIVISION = "griefprevention.tax-rate-subdivision";
    public static final String TAX_RATE_TOWN = "griefprevention.tax-rate-town";
    public static final String TAX_RATE_TOWN_BASIC = "griefprevention.tax-rate-town-basic";
    public static final String TAX_RATE_TOWN_SUBDIVISION = "griefprevention.tax-rate-town-subdivision";
    public static final String TOWN_PURCHASE_COST = "griefprevention.town-purchase-cost";
    // MCClans options
    public static final String LEADER_CLAIM_BOOST = "mcclans.leader-claim-boost";
    public static final String MEMBER_CLAIM_BOOST = "mcclans.member-claim-boost";
    public static final String MEMBER_EXPIRATION = "mcclans.member-expiration";
    public static final String MEMBER_TRUST_LEVEL = "mcclans.member-trust-level";

    public static final double DEFAULT_ABANDON_RETURN_RATIO_BASIC = 1.0;
    public static final double DEFAULT_ABANDON_RETURN_RATIO_TOWN = 1.0;
    public static final int DEFAULT_BLOCKS_ACCRUED_PER_HOUR = GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? 30720 : 120;
    public static final int DEFAULT_CREATE_CLAIM_LIMIT_BASIC = 20;
    public static final int DEFAULT_CREATE_CLAIM_LIMIT_SUBDIVISION = 10;
    public static final int DEFAULT_CREATE_CLAIM_LIMIT_TOWN = 1;
    public static final int DEFAULT_INITIAL_CLAIM_BLOCKS = GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? 25600 : 100;
    public static final int DEFAULT_RADIUS_CLAIM_INSPECT = 50;
    public static final int DEFAULT_RADIUS_CLAIM_LIST = 0;
    public static final int DEFAULT_MAX_ACCRUED_BLOCKS = GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? 20480000 : 80000;
    public static final int DEFAULT_MAX_CLAIM_LEVEL = 255;
    public static final int DEFAULT_MAX_CLAIM_SIZE_BASIC_X= 5000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_BASIC_Y = 256;
    public static final int DEFAULT_MAX_CLAIM_SIZE_BASIC_Z = 5000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_TOWN_X= 10000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_TOWN_Y = 256;
    public static final int DEFAULT_MAX_CLAIM_SIZE_TOWN_Z = 10000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_X= 1000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Y = 256;
    public static final int DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Z = 1000;
    public static final int DEFAULT_MIN_CLAIM_LEVEL = 0;
    public static final int DEFAULT_MIN_CLAIM_SIZE_BASIC_X= 5;
    public static final int DEFAULT_MIN_CLAIM_SIZE_BASIC_Y = 5;
    public static final int DEFAULT_MIN_CLAIM_SIZE_BASIC_Z = 5;
    public static final int DEFAULT_MIN_CLAIM_SIZE_TOWN_X= 32;
    public static final int DEFAULT_MIN_CLAIM_SIZE_TOWN_Y = 32;
    public static final int DEFAULT_MIN_CLAIM_SIZE_TOWN_Z = 32;
    public static final int DEFAULT_CLAIM_CREATE_MODE = 0;
    public static final int DEFAULT_CLAIM_EXPIRATION_CHEST = 7;
    public static final int DEFAULT_CLAIM_EXPIRATION_BASIC = 14;
    public static final int DEFAULT_CLAIM_EXPIRATION_SUBDIVISION = 14;
    public static final int DEFAULT_CLAIM_EXPIRATION_TOWN = 14;
    public static final int DEFAULT_TAX_EXPIRATION_BASIC = 7;
    public static final int DEFAULT_TAX_EXPIRATION_SUBDIVISION = 7;
    public static final int DEFAULT_TAX_EXPIRATION_TOWN = 7;
    public static final int DEFAULT_TAX_EXPIRATION_BASIC_DAYS_KEEP = 7;
    public static final int DEFAULT_TAX_EXPIRATION_SUBDIVISION_DAYS_KEEP = 7;
    public static final int DEFAULT_TAX_EXPIRATION_TOWN_DAYS_KEEP = 7;
    public static final double DEFAULT_TAX_RATE_BASIC = 1.0;
    public static final double DEFAULT_TAX_RATE_SUBDIVISION = 1.0;
    public static final double DEFAULT_TAX_RATE_TOWN = 0.5;
    public static final double DEFAULT_TAX_RATE_TOWN_BASIC = 0.5;
    public static final double DEFAULT_TAX_RATE_TOWN_SUBDIVISION = 0.5;
    public static final double DEFAULT_TOWN_PURCHASE_COST = 1000;

    static {
        // The portion of claim blocks returned to a player when a claim is abandoned.
        DEFAULT_OPTIONS.put(ABANDON_RETURN_RATIO_BASIC, GPOptions.DEFAULT_ABANDON_RETURN_RATIO_BASIC);
        // The portion of claim blocks returned to a player when a town is abandoned.
        DEFAULT_OPTIONS.put(ABANDON_RETURN_RATIO_TOWN, GPOptions.DEFAULT_ABANDON_RETURN_RATIO_TOWN);
        // "Blocks earned per hour. By default, each 'active' player should receive 10 blocks every 5 min. Note: The player must have moved at least 3 blocks since last delivery."
        DEFAULT_OPTIONS.put(BLOCKS_ACCRUED_PER_HOUR, (double) GPOptions.DEFAULT_BLOCKS_ACCRUED_PER_HOUR);
        // Maximum number of claims per player.
        DEFAULT_OPTIONS.put(CREATE_CLAIM_LIMIT_BASIC, (double) GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_BASIC);
        // Maximum number of subdivisions per player.
        DEFAULT_OPTIONS.put(CREATE_CLAIM_LIMIT_SUBDIVISION, (double) GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_SUBDIVISION);
        // Maximum number of towns per player.
        DEFAULT_OPTIONS.put(CREATE_CLAIM_LIMIT_TOWN, (double) GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_TOWN);
        // The number of claim blocks a new player starts with.
        DEFAULT_OPTIONS.put(INITIAL_CLAIM_BLOCKS, (double) GPOptions.DEFAULT_INITIAL_CLAIM_BLOCKS);
        // The search radius, in blocks, to search for nearby claims when using inspection tool.
        // Note: It is recommended not to set this value too high as it can affect performance.
        DEFAULT_OPTIONS.put(RADIUS_CLAIM_INSPECT, (double) GPOptions.DEFAULT_RADIUS_CLAIM_INSPECT);
        // The search radius, in blocks, to search for nearby claims when using /claimlist command.
        // Note: The default is 0 which will search ALL claims.
        // Note: It is recommended to adjust this value if your server has many claims.
        DEFAULT_OPTIONS.put(RADIUS_CLAIM_LIST, (double) GPOptions.DEFAULT_RADIUS_CLAIM_LIST);
        // The limit on accrued blocks (over time). doesn't limit purchased or admin-gifted blocks.
        DEFAULT_OPTIONS.put(MAX_ACCRUED_BLOCKS, (double) GPOptions.DEFAULT_MAX_ACCRUED_BLOCKS);
        // The max level where a claim can be created.
        DEFAULT_OPTIONS.put(MAX_CLAIM_LEVEL, (double) GPOptions.DEFAULT_MAX_CLAIM_LEVEL);
        // The max size of x, in blocks, that a basic claim can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_BASIC_X, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_X);
        // The max size of y, in blocks, that a basic claim can be, 256 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_BASIC_Y, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_Y);
        // The max size of z, in blocks, that a basic claim can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_BASIC_Z, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_Z);
        // The max size of x, in blocks, that a town can be, 200 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_TOWN_X, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_X);
        // The max size of y, in blocks, that a town can be, 256 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_TOWN_Y, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_Y);
        // The max size of z, in blocks, that a town can be, 200 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_TOWN_Z, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_Z);
        // The max size of x, in blocks, that a subdivision can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_SUBDIVISION_X, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_X);
        // The max size of y, in blocks, that a subdivision can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_SUBDIVISION_Y, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Y);
        // The max size of z, in blocks, that a subdivision can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_SUBDIVISION_Z, (double) GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Z);
        // The min level where a claim can be created.
        DEFAULT_OPTIONS.put(MIN_CLAIM_LEVEL, (double) GPOptions.DEFAULT_MIN_CLAIM_LEVEL);
        // The min size of x, in blocks, that a basic claim can be, 5 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_BASIC_X, (double) GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_X);
        // The min size of y, in blocks, that a basic claim can be, 5 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_BASIC_Y, (double) GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_Y);
        // The min size of z, in blocks, that a basic claim can be, 5 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_BASIC_Z, (double) GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_Z);
        // The min size of x, in blocks, that a town can be, 32 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_TOWN_X, (double) GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_X);
        // The min size of y, in blocks, that a town can be, 32 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_TOWN_Y, (double) GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_Y);
        // The min size of z, in blocks, that a town can be, 32 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_TOWN_Z, (double) GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_Z);
        // The default claiming mode for players on login
        DEFAULT_OPTIONS.put(CLAIM_CREATE_MODE, (double) DEFAULT_CLAIM_CREATE_MODE);
        // Number of days of inactivity before an automatic chest claim will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_CHEST, (double) GPOptions.DEFAULT_CLAIM_EXPIRATION_CHEST);
        // Number of days of inactivity before an unused claim will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_BASIC, (double) GPOptions.DEFAULT_CLAIM_EXPIRATION_BASIC);
        // Number of days of inactivity before an unused claim will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_SUBDIVISION, (double) GPOptions.DEFAULT_CLAIM_EXPIRATION_SUBDIVISION);
        // Number of days of inactivity before an unused town will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_TOWN, (double) GPOptions.DEFAULT_CLAIM_EXPIRATION_TOWN);
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_BASIC, (double) GPOptions.DEFAULT_TAX_EXPIRATION_BASIC);
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_SUBDIVISION, (double) GPOptions.DEFAULT_TAX_EXPIRATION_SUBDIVISION);
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_TOWN, (double) GPOptions.DEFAULT_TAX_EXPIRATION_TOWN);
        // The amount of days to keep claim in an expired state due to reasons such as not paying taxes.
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_BASIC_DAYS_KEEP, (double) GPOptions.DEFAULT_TAX_EXPIRATION_BASIC_DAYS_KEEP);
        DEFAULT_OPTIONS.put(TAX_RATE_BASIC, GPOptions.DEFAULT_TAX_RATE_BASIC);
        DEFAULT_OPTIONS.put(TAX_RATE_SUBDIVISION, GPOptions.DEFAULT_TAX_RATE_SUBDIVISION);
        DEFAULT_OPTIONS.put(TAX_RATE_TOWN, GPOptions.DEFAULT_TAX_RATE_TOWN);
        DEFAULT_OPTIONS.put(TAX_RATE_TOWN_BASIC, GPOptions.DEFAULT_TAX_RATE_TOWN_BASIC);
        DEFAULT_OPTIONS.put(TAX_RATE_TOWN_SUBDIVISION, GPOptions.DEFAULT_TAX_RATE_TOWN_SUBDIVISION);
        // The amount of tax per claim block owned. If a claim costs 100 claim blocks and tax is 1 then the total tax would be 100 each time it runs.")
        DEFAULT_OPTIONS.put(TOWN_PURCHASE_COST, GPOptions.DEFAULT_TOWN_PURCHASE_COST);

        if (GriefPreventionPlugin.instance.clanApiProvider != null) {
            // The ratio used to determine how many bonus blocks the leader of a clan receives. 
            // Example: If the ratio is 1.0 and there are 10 active members, the leader would gain 10 bonus claim blocks(1.0 * 10) every time the claim block task runs.
            DEFAULT_OPTIONS.put(LEADER_CLAIM_BOOST, 1.0);
            // Number of bonus claim blocks to give members of a clan
            DEFAULT_OPTIONS.put(MEMBER_CLAIM_BOOST, 10.0);
            // Number of days of inactivity before a clan member is considered inactive. Note: Inactive clan members do not count toward claim boosts.
            DEFAULT_OPTIONS.put(MEMBER_EXPIRATION, 14.0);
            // The default trust level between clan members for all member claims. 0: None, 1: Access, 2: Container, 3: Builder
            DEFAULT_OPTIONS.put(MEMBER_TRUST_LEVEL, 1.0);
        }

        GLOBAL_OPTIONS.add(ABANDON_RETURN_RATIO_BASIC);
        GLOBAL_OPTIONS.add(GPOptions.ABANDON_RETURN_RATIO_BASIC);
        GLOBAL_OPTIONS.add(GPOptions.ABANDON_RETURN_RATIO_TOWN);
        GLOBAL_OPTIONS.add(GPOptions.BLOCKS_ACCRUED_PER_HOUR);
        GLOBAL_OPTIONS.add(GPOptions.CLAIM_EXPIRATION_BASIC);
        GLOBAL_OPTIONS.add(GPOptions.CLAIM_EXPIRATION_CHEST);
        GLOBAL_OPTIONS.add(GPOptions.CLAIM_EXPIRATION_SUBDIVISION);
        GLOBAL_OPTIONS.add(GPOptions.CLAIM_EXPIRATION_TOWN);
        GLOBAL_OPTIONS.add(GPOptions.CREATE_CLAIM_LIMIT_BASIC);
        GLOBAL_OPTIONS.add(GPOptions.CREATE_CLAIM_LIMIT_SUBDIVISION);
        GLOBAL_OPTIONS.add(GPOptions.CREATE_CLAIM_LIMIT_TOWN);
    }
}
