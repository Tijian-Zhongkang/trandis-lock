package com.xm.sanvanfo.scriptor;

import java.util.Arrays;
import java.util.List;

public interface IScript {

    String script();

    default List<Object> scriptKey(Object ... key) {
        return Arrays.asList(key);
    }

    default List<Object> scriptArgv(Object ... argv) {
        return Arrays.asList(argv);
    }
}
