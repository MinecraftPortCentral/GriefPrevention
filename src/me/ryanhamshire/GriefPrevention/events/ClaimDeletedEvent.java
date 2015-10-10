package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.util.event.callback.CallbackList;

/**
 * This event gets called whenever a claim is going to be deleted. This event is
 * not called when a claim is resized.
 * 
 * @author Tux2
 * 
 */
public class ClaimDeletedEvent implements Event {

    // Custom Event Requirements

    private Claim claim;

    public ClaimDeletedEvent(Claim claim) {
        this.claim = claim;
    }

    /**
     * Gets the claim to be deleted.
     * 
     * @return
     */
    public Claim getClaim() {
        return claim;
    }

    @Override
    public CallbackList getCallbacks() {
        return null;
    }
}
