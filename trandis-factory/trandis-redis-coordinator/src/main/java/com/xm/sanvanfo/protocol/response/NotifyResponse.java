package com.xm.sanvanfo.protocol.response;

import com.xm.sanvanfo.protocol.request.NotifyRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class NotifyResponse extends BaseResponse {

    private static final long serialVersionUID = -8680456405195891502L;
    private NotifyRequest request;

    @Override
    public boolean toServer() {
        return true;
    }
}
