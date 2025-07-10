package com.xm.sanvanfo.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class CoordinatorThreadMessageWare extends CoordinatorMessageWare {
    private static final long serialVersionUID = 8491165909669144949L;
    private String threadId;


    public static String getQuoteThreadId() {
        String threadId = Thread.currentThread().getId() + "-" + Thread.currentThread().getName();
        String str = threadId.replaceAll("%", "%25");
        str = str.replaceAll("-", "%2d");
        return str;
    }


}
