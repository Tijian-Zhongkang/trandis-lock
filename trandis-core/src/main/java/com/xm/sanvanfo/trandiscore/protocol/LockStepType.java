package com.xm.sanvanfo.trandiscore.protocol;

public enum LockStepType {
    Begin(0),
    Commit(1),
    Release(2);

    private int value;
    LockStepType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LockStepType get(int value) {
        return LockStepType.values()[value];
    }
}
