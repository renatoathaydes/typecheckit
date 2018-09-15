package com.typecheckit.linear;

import com.sun.source.tree.VariableTree;
import com.typecheckit.util.Mark;

import javax.lang.model.element.Name;

final class LinearMark extends Mark<LinearMark> {

    private final VariableTree node;
    private boolean usedUp = false;

    LinearMark( VariableTree node ) {
        this.node = node;
    }

    void markAsUsed() {
        this.usedUp = true;
    }

    boolean isUsedUp() {
        return usedUp;
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
    protected LinearMark copy() {
        LinearMark linearMark = new LinearMark( node );
        linearMark.usedUp = usedUp;
        return linearMark;
    }

    @Override
    public void merge( LinearMark mark ) {
        this.usedUp |= mark.usedUp;
    }

}
