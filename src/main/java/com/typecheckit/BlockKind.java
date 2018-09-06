package com.typecheckit;

public enum BlockKind {
    ROOT, CLASS, METHOD, IF, FOR_LOOP, WHILE_LOOP, SWITCH, SWITCH_CASE, SYNCHRONIZED, OTHER;

    public boolean isLoop() {
        return this == FOR_LOOP || this == WHILE_LOOP;
    }
}
