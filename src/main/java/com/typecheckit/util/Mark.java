package com.typecheckit.util;

/**
 * A mark that may be applied on a code element.
 *
 * @param <M> self type
 */
public abstract class Mark<M> {

    protected abstract M enterNewScope();

    public abstract M copy();

    public abstract M alias();

    public abstract void merge( M mark );

}
