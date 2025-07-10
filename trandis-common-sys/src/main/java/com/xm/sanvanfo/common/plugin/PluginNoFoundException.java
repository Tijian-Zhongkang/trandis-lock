package com.xm.sanvanfo.common.plugin;

@SuppressWarnings("unused")
public class PluginNoFoundException extends RuntimeException {

    public PluginNoFoundException() {
        super();
    }


    public PluginNoFoundException(String message) {
        super(message);
    }


    public PluginNoFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginNoFoundException(Throwable cause) {
        super(cause);
    }

    protected PluginNoFoundException(String message, Throwable cause,
                               boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
