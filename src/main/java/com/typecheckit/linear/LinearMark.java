package com.typecheckit.linear;

import com.sun.source.tree.VariableTree;
import com.typecheckit.util.Mark;

import javax.lang.model.element.Name;

final class LinearMark extends Mark<LinearMark> {

    private final VariableTree node;
    private int useCount = 0;

    LinearMark( VariableTree node ) {
        this.node = node;
    }

    void markAsUsed() {
        useCount++;
    }

    boolean isUsedUp() {
        return useCount > 0;
    }

    void ignoreNextUse() {
        useCount--;
    }

    Name name() {
        return node.getName();
    }

    @Override
    protected LinearMark enterNewScope() {
        // linear types may be used up within a new scope, affecting all parent scopes
        return this;
    }

    @Override
    public LinearMark copy() {
        LinearMark linearMark = new LinearMark( node );
        linearMark.useCount = useCount;
        return linearMark;
    }

    @Override
    public LinearMark alias() {
        return this;
    }

    @Override
    public void merge( LinearMark mark ) {
        this.useCount += mark.useCount;
    }

    @Override
    public String toString() {
        return "LinearMark{" +
                "node=" + node.getName() +
                ", useCount=" + useCount +
                '}';
    }
}
