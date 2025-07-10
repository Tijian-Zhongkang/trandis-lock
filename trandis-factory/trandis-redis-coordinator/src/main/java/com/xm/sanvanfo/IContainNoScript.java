package com.xm.sanvanfo;

public interface IContainNoScript {

    default boolean exceptionNotContainsNoScriptError(Throwable e) {

        Throwable current = e;
        while (current != null) {

            String exMessage = current.getMessage();
            if (exMessage != null && exMessage.contains("NOSCRIPT")) {
                return false;
            }

            current = current.getCause();
        }

        return true;
    }
}
