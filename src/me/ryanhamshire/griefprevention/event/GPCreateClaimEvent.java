package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.event.CreateClaimEvent;
import org.spongepowered.api.event.cause.Cause;

public class GPCreateClaimEvent extends GPClaimEvent implements CreateClaimEvent {

    public GPCreateClaimEvent(Claim claim, Cause cause) {
        super(claim, cause);
    }

}
