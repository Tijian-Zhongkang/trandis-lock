package com.xm.sanvanfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("unused")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BootstrapServerInfo {

    public enum Status {
        INIT(0),
        ACTIVE(1);

        private int code;
        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Status get(int code) {
            return Status.values()[code];
        }

    }

    private String ip;
    private Integer port;
    private String applicationId;
    private Long activeTime;
    private String id;
    private Status status = Status.INIT;
}
