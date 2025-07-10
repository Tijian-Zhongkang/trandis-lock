package com.xm.sanvanfo;

import com.xm.sanvanfo.roles.IRole;

public interface ICoordinatorPlugin {

    void roleInit(IRole.RoleType roleType);

    void roleClose(IRole.RoleType roleType);
}
