package com.xm.sanvanfo.trandiscore.protocol.response;

import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import lombok.Data;


@Data
public abstract class AbstractTransactionResponse implements AbstractMessage {

    private static final long serialVersionUID = 4655403573491973874L;
    protected Integer code;
    protected String msg;
}
