package me.joeycumines.javapromises.core;

import java.util.List;

/**
 * A list of exceptions which cause this one. Inspired by the AggregateError in the bluebird.js library.
 */
public class AggregateException extends RuntimeException {
    private List<Throwable> exceptionList;

    public AggregateException() {
        this(null);
    }

    public AggregateException(List<Throwable> exceptionList) {
        super("could not resolve the promise(s) due to exceptions: " + exceptionList.toString(), null, true, false);
        this.exceptionList = exceptionList;
    }

    public List<Throwable> getExceptionList() {
        return this.exceptionList;
    }
}
