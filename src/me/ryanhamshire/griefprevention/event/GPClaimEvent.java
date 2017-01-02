package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.event.ClaimEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public class GPClaimEvent extends AbstractEvent implements ClaimEvent {

    private Claim claim;
    private Cause cause;
    private boolean isCancelled = false;

    public GPClaimEvent(Claim claim, Cause cause) {
        this.claim = claim;
        this.cause = cause;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public Cause getCause() {
        if (this.cause == null) {
            return GriefPreventionPlugin.pluginCause;
        }

        return this.cause;
    }

    @Override
    public Claim getClaim() {
        return this.claim;
    }

}
