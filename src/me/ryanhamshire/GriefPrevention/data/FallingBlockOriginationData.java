package me.ryanhamshire.GriefPrevention.data;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class FallingBlockOriginationData implements DataManipulator<FallingBlockOriginationData, ImmutableFallingBlockOriginationData> {

    @Override
    public Optional<FallingBlockOriginationData> fill(DataHolder dataHolder) {
        return null;
    }

    @Override
    public Optional<FallingBlockOriginationData> fill(DataHolder dataHolder, MergeFunction overlap) {
        return null;
    }

    @Override
    public Optional<FallingBlockOriginationData> from(DataContainer container) {
        return null;
    }

    @Override
    public <E> FallingBlockOriginationData set(Key<? extends BaseValue<E>> key, E value) {
        return null;
    }

    @Override
    public FallingBlockOriginationData set(BaseValue<?> value) {
        return null;
    }

    @Override
    public FallingBlockOriginationData set(BaseValue<?>... values) {
        return null;
    }

    @Override
    public FallingBlockOriginationData set(Iterable<? extends BaseValue<?>> values) {
        return null;
    }

    @Override
    public <E> FallingBlockOriginationData transform(Key<? extends BaseValue<E>> key, Function<E, E> function) {
        return null;
    }

    @Override
    public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
        return null;
    }

    @Override
    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
        return null;
    }

    @Override
    public boolean supports(Key<?> key) {
        return false;
    }

    @Override
    public FallingBlockOriginationData copy() {
        return null;
    }

    @Override
    public Set<Key<?>> getKeys() {
        return null;
    }

    @Override
    public Set<ImmutableValue<?>> getValues() {
        return null;
    }

    @Override
    public ImmutableFallingBlockOriginationData asImmutable() {
        return null;
    }

    @Override
    public int compareTo(FallingBlockOriginationData o) {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        return null;
    }
}
