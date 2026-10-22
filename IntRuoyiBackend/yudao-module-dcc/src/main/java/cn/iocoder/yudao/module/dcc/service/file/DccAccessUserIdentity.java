package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;

import java.util.Objects;

/** Shared account identity boundary for category, viewing matrix and directory rules. */
public final class DccAccessUserIdentity {
    private DccAccessUserIdentity() {}

    public static boolean isEnabledTenantUser(Long actorId, AdminUserRespDTO user) {
        return actorId != null && actorId > 0 && user != null
                && Objects.equals(actorId, user.getId())
                && Objects.equals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus())
                && !TenantContextHolder.isIgnore()
                && Objects.equals(TenantContextHolder.getRequiredTenantId(), user.getTenantId());
    }
}
