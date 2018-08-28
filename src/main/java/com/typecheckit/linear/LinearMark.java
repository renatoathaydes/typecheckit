package com.typecheckit.linear;

import com.typecheckit.Mark;

final class LinearMark extends Mark<LinearMark> {

    boolean isUsedUp;

    @Override
    protected LinearMark enterNewScope() {
        // linear types may be used up within a new scope, affecting all parent scopes
        return this;
    }

    @Override
    protected LinearMark copy() {
        LinearMark linearMark = new LinearMark();
        linearMark.isUsedUp = this.isUsedUp;
        return linearMark;
    }
}
