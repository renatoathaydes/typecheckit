package com.typecheckit;

public enum BlockKind {
    IF_THEN, IF_THEN_ELSE, ELSE, FOR_LOOP, WHILE_LOOP, OTHER;

    public boolean isLoop() {
        return this == FOR_LOOP || this == WHILE_LOOP;
    }
}
