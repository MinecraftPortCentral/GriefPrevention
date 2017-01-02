package me.ryanhamshire.griefprevention.claim;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;

import java.util.Optional;

public class GPClaimResult implements ClaimResult {

    private final Claim claim;
    private final ClaimResultType resultType;

    public GPClaimResult(Claim claim, ClaimResultType type) {
        this.claim = claim;
        this.resultType = type;
    }

    @Override
    public ClaimResultType getResultType() {
        return this.resultType;
    }

    @Override
    public Optional<Claim> getClaim() {
        return Optional.ofNullable(this.claim);
    }

}
