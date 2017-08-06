package me.ryanhamshire.griefprevention.permission;

import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;

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

    public static final Map<String, String> DEFAULT_OPTIONS = Maps.newHashMap();

    public static final String INVALID_OPTION = "invalid-option";
    public static final String ABANDON_RETURN_RATIO_BASIC = "griefprevention.abandon-return-ratio-basic";
    public static final String ABANDON_RETURN_RATIO_TOWN = "griefprevention.abandon-return-ratio-town";
    public static final String BLOCKS_ACCRUED_PER_HOUR = "griefprevention.blocks-accrued-per-hour";
    public static final String CREATE_CLAIM_LIMIT_BASIC = "griefprevention.create-claim-limit-basic";
    public static final String CREATE_CLAIM_LIMIT_SUBDIVISION = "griefprevention.create-claim-limit-subdivision";
    public static final String CREATE_CLAIM_LIMIT_TOWN = "griefprevention.create-claim-limit-town";
    public static final String INITIAL_CLAIM_BLOCKS = "griefprevention.initial-claim-blocks";
    public static final String MAX_ACCRUED_BLOCKS = "griefprevention.max-accrued-claim-blocks";
    public static final String MAX_CLAIM_SIZE_BASIC_X = "griefprevention.max-claim-size-basic-x";
    public static final String MAX_CLAIM_SIZE_BASIC_Y = "griefprevention.max-claim-size-basic-y";
    public static final String MAX_CLAIM_SIZE_BASIC_Z = "griefprevention.max-claim-size-basic-z";
    public static final String MAX_CLAIM_SIZE_SUBDIVISION_X = "griefprevention.max-claim-size-subdivision-x";
    public static final String MAX_CLAIM_SIZE_SUBDIVISION_Y = "griefprevention.max-claim-size-subdivision-y";
    public static final String MAX_CLAIM_SIZE_SUBDIVISION_Z = "griefprevention.max-claim-size-subdivision-z";
    public static final String MAX_CLAIM_SIZE_TOWN_X = "griefprevention.max-claim-size-town-x";
    public static final String MAX_CLAIM_SIZE_TOWN_Y = "griefprevention.max-claim-size-town-y";
    public static final String MAX_CLAIM_SIZE_TOWN_Z = "griefprevention.max-claim-size-town-z";
    public static final String MIN_CLAIM_SIZE_BASIC_X = "griefprevention.min-claim-size-basic-x";
    public static final String MIN_CLAIM_SIZE_BASIC_Y = "griefprevention.min-claim-size-basic-y";
    public static final String MIN_CLAIM_SIZE_BASIC_Z = "griefprevention.min-claim-size-basic-z";
    public static final String MIN_CLAIM_SIZE_TOWN_X = "griefprevention.min-claim-size-town-x";
    public static final String MIN_CLAIM_SIZE_TOWN_Y = "griefprevention.min-claim-size-town-y";
    public static final String MIN_CLAIM_SIZE_TOWN_Z = "griefprevention.min-claim-size-town-z";
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
    public static final int DEFAULT_BLOCKS_ACCRUED_PER_HOUR = 30720;
    public static final int DEFAULT_CREATE_CLAIM_LIMIT_BASIC = 20;
    public static final int DEFAULT_CREATE_CLAIM_LIMIT_SUBDIVISION = 10;
    public static final int DEFAULT_CREATE_CLAIM_LIMIT_TOWN = 1;
    public static final int DEFAULT_INITIAL_CLAIM_BLOCKS = 25600;
    public static final int DEFAULT_MAX_ACCRUED_BLOCKS = 20480000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_BASIC_X= 500;
    public static final int DEFAULT_MAX_CLAIM_SIZE_BASIC_Y = 256;
    public static final int DEFAULT_MAX_CLAIM_SIZE_BASIC_Z = 500;
    public static final int DEFAULT_MAX_CLAIM_SIZE_TOWN_X= 1000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_TOWN_Y = 256;
    public static final int DEFAULT_MAX_CLAIM_SIZE_TOWN_Z = 1000;
    public static final int DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_X= 250;
    public static final int DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Y = 256;
    public static final int DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Z = 250;
    public static final int DEFAULT_MIN_CLAIM_SIZE_BASIC_X= 10;
    public static final int DEFAULT_MIN_CLAIM_SIZE_BASIC_Y = 10;
    public static final int DEFAULT_MIN_CLAIM_SIZE_BASIC_Z = 10;
    public static final int DEFAULT_MIN_CLAIM_SIZE_TOWN_X= 32;
    public static final int DEFAULT_MIN_CLAIM_SIZE_TOWN_Y = 32;
    public static final int DEFAULT_MIN_CLAIM_SIZE_TOWN_Z = 32;
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
        DEFAULT_OPTIONS.put(ABANDON_RETURN_RATIO_BASIC, Double.toString(GPOptions.DEFAULT_ABANDON_RETURN_RATIO_BASIC));
        // The portion of claim blocks returned to a player when a town is abandoned.
        DEFAULT_OPTIONS.put(ABANDON_RETURN_RATIO_TOWN, Double.toString(GPOptions.DEFAULT_ABANDON_RETURN_RATIO_TOWN));
        // "Blocks earned per hour. By default, each 'active' player should receive 10 blocks every 5 min. Note: The player must have moved at least 3 blocks since last delivery."
        DEFAULT_OPTIONS.put(BLOCKS_ACCRUED_PER_HOUR, Integer.toString(GPOptions.DEFAULT_BLOCKS_ACCRUED_PER_HOUR));
        // Maximum number of claims per player.
        DEFAULT_OPTIONS.put(CREATE_CLAIM_LIMIT_BASIC, Integer.toString(GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_BASIC));
        // Maximum number of subdivisions per player.
        DEFAULT_OPTIONS.put(CREATE_CLAIM_LIMIT_SUBDIVISION, Integer.toString(GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_SUBDIVISION));
        // Maximum number of towns per player.
        DEFAULT_OPTIONS.put(CREATE_CLAIM_LIMIT_TOWN, Integer.toString(GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_TOWN));
        // The number of claim blocks a new player starts with.
        DEFAULT_OPTIONS.put(INITIAL_CLAIM_BLOCKS, Integer.toString(GPOptions.DEFAULT_INITIAL_CLAIM_BLOCKS));
        // The limit on accrued blocks (over time). doesn't limit purchased or admin-gifted blocks.
        DEFAULT_OPTIONS.put(MAX_ACCRUED_BLOCKS, Integer.toString(GPOptions.DEFAULT_MAX_ACCRUED_BLOCKS));
        // The max size of x, in blocks, that a basic claim can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_BASIC_X, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_X));
        // The max size of y, in blocks, that a basic claim can be, 256 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_BASIC_Y, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_Y));
        // The max size of z, in blocks, that a basic claim can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_BASIC_Z, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_Z));
        // The max size of x, in blocks, that a town can be, 200 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_TOWN_X, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_X));
        // The max size of y, in blocks, that a town can be, 256 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_TOWN_Y, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_Y));
        // The max size of z, in blocks, that a town can be, 200 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_TOWN_Z, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_Z));
        // The max size of x, in blocks, that a subdivision can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_SUBDIVISION_X, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_X));
        // The max size of y, in blocks, that a subdivision can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_SUBDIVISION_Y, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Y));
        // The max size of z, in blocks, that a subdivision can be, 100 by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_SUBDIVISION_Z, Integer.toString(GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Z));
        // The min size of x, in blocks, that a basic claim can be, 10 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_BASIC_X, Integer.toString(GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_X));
        // The min size of y, in blocks, that a basic claim can be, 10 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_BASIC_Y, Integer.toString(GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_Y));
        // The min size of z, in blocks, that a basic claim can be, 10 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_BASIC_Z, Integer.toString(GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_Z));
        // The min size of x, in blocks, that a town can be, 32 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_TOWN_X, Integer.toString(GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_X));
        // The min size of y, in blocks, that a town can be, 32 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_TOWN_Y, Integer.toString(GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_Y));
        // The min size of z, in blocks, that a town can be, 32 by default.
        DEFAULT_OPTIONS.put(MIN_CLAIM_SIZE_TOWN_Z, Integer.toString(GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_Z));
        // Number of days of inactivity before an automatic chest claim will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_CHEST, Integer.toString(GPOptions.DEFAULT_CLAIM_EXPIRATION_CHEST));
        // Number of days of inactivity before an unused claim will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_BASIC, Integer.toString(GPOptions.DEFAULT_CLAIM_EXPIRATION_BASIC));
        // Number of days of inactivity before an unused claim will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_SUBDIVISION, Integer.toString(GPOptions.DEFAULT_CLAIM_EXPIRATION_SUBDIVISION));
        // Number of days of inactivity before an unused town will be deleted.
        DEFAULT_OPTIONS.put(CLAIM_EXPIRATION_TOWN, Integer.toString(GPOptions.DEFAULT_CLAIM_EXPIRATION_TOWN));
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_BASIC, Integer.toString(GPOptions.DEFAULT_TAX_EXPIRATION_BASIC));
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_SUBDIVISION, Integer.toString(GPOptions.DEFAULT_TAX_EXPIRATION_SUBDIVISION));
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_TOWN, Integer.toString(GPOptions.DEFAULT_TAX_EXPIRATION_TOWN));
        // The amount of days to keep claim in an expired state due to reasons such as not paying taxes.
        DEFAULT_OPTIONS.put(TAX_EXPIRATION_BASIC_DAYS_KEEP, Integer.toString(GPOptions.DEFAULT_TAX_EXPIRATION_BASIC_DAYS_KEEP));
        DEFAULT_OPTIONS.put(TAX_RATE_BASIC, Double.toString(GPOptions.DEFAULT_TAX_RATE_BASIC));
        DEFAULT_OPTIONS.put(TAX_RATE_SUBDIVISION, Double.toString(GPOptions.DEFAULT_TAX_RATE_SUBDIVISION));
        DEFAULT_OPTIONS.put(TAX_RATE_TOWN, Double.toString(GPOptions.DEFAULT_TAX_RATE_TOWN));
        DEFAULT_OPTIONS.put(TAX_RATE_TOWN_BASIC, Double.toString(GPOptions.DEFAULT_TAX_RATE_TOWN_BASIC));
        DEFAULT_OPTIONS.put(TAX_RATE_TOWN_SUBDIVISION, Double.toString(GPOptions.DEFAULT_TAX_RATE_TOWN_SUBDIVISION));
        // The amount of tax per claim block owned. If a claim costs 100 claim blocks and tax is 1 then the total tax would be 100 each time it runs.")
        DEFAULT_OPTIONS.put(TOWN_PURCHASE_COST, Double.toString(GPOptions.DEFAULT_TOWN_PURCHASE_COST));

        if (GriefPreventionPlugin.instance.clanApiProvider != null) {
            // The ratio used to determine how many bonus blocks the leader of a clan receives. 
            // Example: If the ratio is 1.0 and there are 10 active members, the leader would gain 10 bonus claim blocks(1.0 * 10) every time the claim block task runs.
            DEFAULT_OPTIONS.put(LEADER_CLAIM_BOOST, "1.0");
            // Number of bonus claim blocks to give members of a clan
            DEFAULT_OPTIONS.put(MEMBER_CLAIM_BOOST, "10");
            // Number of days of inactivity before a clan member is considered inactive. Note: Inactive clan members do not count toward claim boosts.
            DEFAULT_OPTIONS.put(MEMBER_EXPIRATION, "14");
            // The default trust level between clan members for all member claims. 0: None, 1: Access, 2: Container, 3: Builder
            DEFAULT_OPTIONS.put(MEMBER_TRUST_LEVEL, "1");
        }
    }
}
