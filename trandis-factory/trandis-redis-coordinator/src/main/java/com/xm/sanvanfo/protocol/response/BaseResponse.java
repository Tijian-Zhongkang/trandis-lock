package com.xm.sanvanfo.protocol.response;

import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseResponse extends CoordinatorMessageWare {
    private static final long serialVersionUID = 4657597333195307062L;
    private String msg;
    private Integer code;

    @SuppressWarnings("unused")
    public enum  ResponseCode {
        SUCCESS(200),
        INNER_ERROR(500),
        NOT_FOUND(404),
        REDIRECT(302),
        WAITSIGNAL(900),
        REPAIRING(901),
        NOINIT(801),
        TIMEOUT(802),
        NETWORK_ERROR(803);

        private int code;
        ResponseCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }
}
