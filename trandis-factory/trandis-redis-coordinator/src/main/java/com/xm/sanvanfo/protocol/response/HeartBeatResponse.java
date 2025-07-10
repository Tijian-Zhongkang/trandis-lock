package com.xm.sanvanfo.protocol.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class HeartBeatResponse extends BaseResponse {
    private static final long serialVersionUID = 1502516768487246293L;
    private String serverId;

    @Override
    public boolean toServer() {
        return false;
    }
}
