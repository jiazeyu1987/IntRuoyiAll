package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.service.permission.bo.SystemEntitlementSyncCommand;

import java.util.Set;

public interface SystemEntitlementService {

    void syncClaims(SystemEntitlementSyncCommand command);

    void revokeEntitlementSource(Long tenantId, String sourceType, String sourceKey, String policyCode,
                                 Long operatorUserId, String operatorUsername);

    boolean hasAnyPermission(Long userId, String permissionCode);

    Set<Long> getActiveMenuIdsByUserId(Long userId);

}
