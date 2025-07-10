package com.xm.sanvanfo.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class CoordinatorMessageWare implements Serializable {
    private static final long serialVersionUID = 8334777230991834073L;
    private String id;
    private String coordinatorId;
    private String appName;
    private String destCoordinatorId;

    public abstract boolean toServer();

    public boolean timeoutResend() {
        return false;
    }
}
