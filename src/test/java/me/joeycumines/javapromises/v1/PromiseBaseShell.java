package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.MutatedStateException;
import me.joeycumines.javapromises.core.PromiseInterface;
import me.joeycumines.javapromises.core.PromiseState;
import me.joeycumines.javapromises.core.SelfResolutionException;

import java.util.function.Function;

/**
 * Implementation of PromiseBase to test underlying functionality.
 */
public class PromiseBaseShell extends PromiseBase {
    @Override
    public void finalize(PromiseState state, Object value) throws IllegalArgumentException, MutatedStateException, SelfResolutionException {
        super.finalize(state, value);
    }

    @Override
    public PromiseInterface then(Function callback) {
        return this;
    }

    @Override
    public PromiseInterface except(Function callback) {
        return this;
    }

    @Override
    public PromiseInterface always(Function callback) {
        return this;
    }
}
