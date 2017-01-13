package me.ryanhamshire.griefprevention.claim;

import com.google.common.collect.ImmutableList;
import me.ryanhamshire.griefprevention.api.claim.TrustManager;
import org.spongepowered.api.event.cause.Cause;

import java.util.List;
import java.util.UUID;

public class GPTrustManager implements TrustManager {

    private GPClaim claim;

    public GPTrustManager(GPClaim claim) {
        this.claim = claim;
    }

    @Override
    public List<UUID> getContainers() {
        return ImmutableList.copyOf(this.claim.getInternalClaimData().getContainers());
    }

    @Override
    public List<UUID> getAccessors() {
        return ImmutableList.copyOf(this.claim.getInternalClaimData().getAccessors());
    }

    @Override
    public List<UUID> getBuilders() {
        return ImmutableList.copyOf(this.claim.getInternalClaimData().getBuilders());
    }

    @Override
    public List<UUID> getManagers() {
        return ImmutableList.copyOf(this.claim.getInternalClaimData().getManagers());
    }

    @Override
    public void addAccessor(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getAccessors().add(uuid);
    }

    @Override
    public void addContainer(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getContainers().add(uuid);
    }

    @Override
    public void addBuilder(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getBuilders().add(uuid);
    }

    @Override
    public void addManager(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getManagers().add(uuid);
    }

    @Override
    public void removeAccessor(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getAccessors().remove(uuid);
    }

    @Override
    public void removeContainer(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getContainers().remove(uuid);
    }

    @Override
    public void removeBuilder(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getBuilders().remove(uuid);
    }

    @Override
    public void removeManager(UUID uuid, Cause cause) {
        this.claim.getInternalClaimData().getManagers().remove(uuid);
    }

    @Override
    public void save() {
        this.claim.getClaimStorage().save();
    }
}
