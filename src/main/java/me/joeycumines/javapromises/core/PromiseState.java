package me.joeycumines.javapromises.core;

/**
 * A promise can have one of three states, pending, fulfilled, or rejected.
 * A promise will always start in the PENDING state.
 * A promise must ONLY ever resolve (finish / change state) ONCE, though it is still usable in it's resolved state.
 */
public enum PromiseState {
    PENDING,
    FULFILLED,
    REJECTED
}
