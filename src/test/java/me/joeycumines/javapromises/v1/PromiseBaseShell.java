package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.MutatedStateException;
import me.joeycumines.javapromises.core.Promise;
import me.joeycumines.javapromises.core.PromiseState;
import me.joeycumines.javapromises.core.SelfResolutionException;

import java.util.function.Function;

/**
 * Implementation of PromiseBase to test underlying functionality.
 */
public class PromiseBaseShell extends PromiseBase {
    @Override
    public PromiseBaseShell finalize(PromiseState state, Object value) throws IllegalArgumentException, MutatedStateException, SelfResolutionException {
        // protected > public
        super.finalize(state, value);

        return this;
    }

    @Override
    public PromiseBaseShell reject(Exception value) {
        // protected > public
        super.reject(value);

        return this;
    }

    @Override
    public PromiseBaseShell resolve(Object value) {
        // protected > public
        super.resolve(value);

        return this;
    }

    @Override
    public Promise then(Function callback) {
        return this;
    }

    @Override
    public Promise except(Function callback) {
        return this;
    }

    @Override
    public Promise always(Function callback) {
        return this;
    }
}
