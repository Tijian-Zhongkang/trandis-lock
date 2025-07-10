package com.xm.sanvanfo.protocol.response;

import com.xm.sanvanfo.roles.NodeLockInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class ReleaseResponse extends BaseResponse {
    private static final long serialVersionUID = -5246949545163653244L;
    private String coordinatorId;
    private String threadId;
    private List<NodeLockInfo.LockInfo> releasePaths;
    private List<NodeLockInfo.LockInfo> failPaths;

    @Override
    public boolean toServer() {
        return false;
    }
}
