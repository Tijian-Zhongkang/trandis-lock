package com.xm.sanvanfo.protocol.request;

import com.xm.sanvanfo.protocol.CoordinatorThreadMessageWare;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


import java.util.ArrayList;
import java.util.List;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class ReleaseRequest extends CoordinatorThreadMessageWare {
    private static final long serialVersionUID = -8473250660259477469L;
    private List<String> readPath;
    private Integer readEntrantTimes;
    private List<String> writePath;
    private Integer writeEntrantTimes;

    @Override
    public boolean toServer() {
        return true;
    }

    @Override
    public boolean timeoutResend() {
        return true;
    }
}
