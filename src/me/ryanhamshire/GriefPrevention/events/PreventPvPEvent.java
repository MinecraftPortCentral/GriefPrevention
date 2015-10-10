package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.util.event.callback.CallbackList;

//if cancelled, GriefPrevention will not cancel the PvP event it's processing.
public class PreventPvPEvent implements Event, Cancellable {

    private boolean cancelled = false;

    Claim claim;

    public PreventPvPEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return this.claim;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public CallbackList getCallbacks() {
        return null;
    }
}
