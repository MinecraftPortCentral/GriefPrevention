package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.event.TransferClaimEvent;
import org.spongepowered.api.event.cause.Cause;

import java.util.UUID;

public class GPTransferClaimEvent extends GPClaimEvent implements TransferClaimEvent {

    private UUID originalOwner;
    private UUID newOwner;

    public GPTransferClaimEvent(Claim claim, Cause cause, UUID originalOwner, UUID newOwner) {
        super(claim, cause);
        this.originalOwner = originalOwner;
        this.newOwner = newOwner;
    }

    @Override
    public UUID getOriginalOwner() {
        return this.originalOwner;
    }

    @Override
    public UUID getNewOwner() {
        return this.newOwner;
    }

    @Override
    public void setNewOwner(UUID newOwner) {
        this.newOwner = newOwner;
    }

}
