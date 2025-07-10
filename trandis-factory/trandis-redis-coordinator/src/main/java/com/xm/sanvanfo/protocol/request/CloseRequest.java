package com.xm.sanvanfo.protocol.request;

import com.xm.sanvanfo.protocol.CoordinatorMessageWare;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class CloseRequest extends CoordinatorMessageWare {
    private static final long serialVersionUID = 5383486544285632557L;

    @Override
    public boolean toServer() {
        return true;
    }
}
