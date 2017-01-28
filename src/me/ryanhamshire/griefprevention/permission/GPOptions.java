package me.ryanhamshire.griefprevention.permission;

import com.google.common.collect.Maps;

import java.util.Map;

public class GPOptions {

    public static final Map<String, String> DEFAULT_OPTIONS = Maps.newHashMap();

    public static final String ABANDON_RETURN_RATIO = "griefprevention.abandon-return-ratio";
    public static final String BLOCKS_ACCRUED_PER_HOUR = "griefprevention.blocks-accrued-per-hour";
    public static final String CREATE_CLAIM_LIMIT = "griefprevention.create-claim-limit";
    public static final String INITIAL_CLAIM_BLOCKS = "griefprevention.initial-claim-blocks";
    public static final String MAX_ACCRUED_BLOCKS = "griefprevention.max-accrued-claim-blocks";
    public static final String MAX_CLAIM_SIZE_X = "griefprevention.max-claim-size-x";
    public static final String MAX_CLAIM_SIZE_Y = "griefprevention.max-claim-size-y";
    public static final String MAX_CLAIM_SIZE_Z = "griefprevention.max-claim-size-z";
    public static final String CHEST_CLAIM_EXPIRATION = "griefprevention.chest-claim-expiration";
    public static final String PLAYER_CLAIM_EXPIRATION = "griefprevention.player-claim-expiration";

    static {
        // The portion of claim blocks returned to a player when a claim is abandoned.
        DEFAULT_OPTIONS.put(ABANDON_RETURN_RATIO, "1.0");
        // "Blocks earned per hour. By default, each 'active' player should receive 10 blocks every 5 min. Note: The player must have moved at least 3 blocks since last delivery."
        DEFAULT_OPTIONS.put(BLOCKS_ACCRUED_PER_HOUR, "120");
        // Maximum number of claims per player.
        DEFAULT_OPTIONS.put(CREATE_CLAIM_LIMIT, "0");
        // The number of claim blocks a new player starts with.
        DEFAULT_OPTIONS.put(INITIAL_CLAIM_BLOCKS, "100");
        // The limit on accrued blocks (over time). doesn't limit purchased or admin-gifted blocks.
        DEFAULT_OPTIONS.put(MAX_ACCRUED_BLOCKS, "80000");
        // The max size of x, in blocks, that a claim can be, unlimited by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_X, "0");
        // The max size of y, in blocks, that a claim can be, unlimited by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_Y, "0");
        // The max size of z, in blocks, that a claim can be, unlimited by default.
        DEFAULT_OPTIONS.put(MAX_CLAIM_SIZE_Z, "0");
        // Number of days of inactivity before an automatic chest claim will be deleted.
        DEFAULT_OPTIONS.put(CHEST_CLAIM_EXPIRATION, "7");
        // Number of days of inactivity before an unused claim will be deleted.
        DEFAULT_OPTIONS.put(PLAYER_CLAIM_EXPIRATION, "14");
    }
}
