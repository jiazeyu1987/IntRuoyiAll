package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;

/** Internal qualification lock; neither operation creates a missing tenant row. */
public interface ApprovalAuthorityApi {
    ApprovalAuthorityRevisionDTO lockTenantApprovalAuthority(Long tenantId);
    void incrementLockedAuthorityRevision(Long tenantId, Long expectedRevision);
}
