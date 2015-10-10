package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

//if cancelled, GriefPrevention will not cancel the PvP event it's processing.
public class PreventPvPEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    public static HandlerList getHandlerList() {
        return handlers;
    }

    Claim claim;

    public PreventPvPEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return this.claim;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
