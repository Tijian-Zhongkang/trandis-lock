package com.xm.sanvanfo.trandiscore.constant;

public enum TransactionLevelEx {
    ReadUnCommitted(0),
    ReadCommitted(1),
    RepeatableRead(2),
    Serializable(3),
    Unlock(4);

    int value;
    TransactionLevelEx(int value) {
        this.value = value;
    }

    public int getValue() {return value;}

    public static TransactionLevelEx get(Integer value) {
        return TransactionLevelEx.values()[value];
    }
}
