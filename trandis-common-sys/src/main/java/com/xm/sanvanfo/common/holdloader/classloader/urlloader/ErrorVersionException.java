package com.xm.sanvanfo.common.holdloader.classloader.urlloader;

@SuppressWarnings("WeakerAccess")
public class ErrorVersionException extends Exception {

    private static final long serialVersionUID = 8444160509534470751L;

    public ErrorVersionException() {
        super();
    }

    public ErrorVersionException(String message){
        super(message);
    }
}
