package com.xm.sanvanfo.trandiscore.transaction;

import com.xm.sanvanfo.trandiscore.GlobalLockKey;

public interface GlobalKeyInterface {

    default String getKeyPath(GlobalLockKey key) {

        return String.format("%s-%s-%s", key.getSpaceName(), key.getKeyId(), key.getSubKeyId());
    }
}
