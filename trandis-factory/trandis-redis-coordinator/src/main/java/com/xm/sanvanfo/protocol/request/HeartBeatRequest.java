package com.xm.sanvanfo.protocol.request;

import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class HeartBeatRequest extends CoordinatorMessageWare {
    private static final long serialVersionUID = -363974766117986170L;

    @Override
    public boolean toServer() {
        return true;
    }
}
