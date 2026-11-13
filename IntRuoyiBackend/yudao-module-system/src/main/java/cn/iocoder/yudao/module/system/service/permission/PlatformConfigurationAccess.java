package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/** Platform definitions are shared infrastructure, not tenant-owned business settings. */
public final class PlatformConfigurationAccess {
    private static final Set<String> RESOURCES = Set.of(
            "system:menu", "system:tenant", "system:tenant-package",
            "infra:codegen", "infra:data-source-config");

    private PlatformConfigurationAccess() {}

    public static boolean isPlatformPermission(String permission) {
        if (permission == null) return false;
        int action = permission.lastIndexOf(':');
        return action > 0 && RESOURCES.contains(permission.substring(0, action));
    }

    public static boolean isPlatformContext(LoginUser user, Long userId, Long currentTenantId, TenantDO tenant) {
        return user != null && userId != null && currentTenantId != null && tenant != null
                && Objects.equals(user.getId(), userId)
                && Objects.equals(user.getUserType(), UserTypeEnum.ADMIN.getValue())
                && Objects.equals(user.getTenantId(), currentTenantId)
                && (user.getVisitTenantId() == null || Objects.equals(user.getVisitTenantId(), currentTenantId))
                && Objects.equals(tenant.getId(), currentTenantId)
                && Objects.equals(tenant.getPackageId(), TenantDO.PACKAGE_ID_SYSTEM)
                && Objects.equals(tenant.getStatus(), CommonStatusEnum.ENABLE.getStatus())
                && !Boolean.TRUE.equals(tenant.getDeleted())
                && tenant.getExpireTime() != null && tenant.getExpireTime().isAfter(LocalDateTime.now());
    }
}
