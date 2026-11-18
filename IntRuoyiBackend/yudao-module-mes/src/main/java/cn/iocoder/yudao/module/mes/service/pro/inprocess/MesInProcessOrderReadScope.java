package cn.iocoder.yudao.module.mes.service.pro.inprocess;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import java.util.Objects;
import java.util.Set;

/** Cross-leader read scope only; never use this rule to authorize mutations. */
public final class MesInProcessOrderReadScope {
    private MesInProcessOrderReadScope() { }

    public static boolean canRead(PermissionApi permissions, Long actor, MesProcessPoolActiveOrderDO order) {
        var login = SecurityFrameworkUtils.getLoginUser();
        Long tenant = TenantContextHolder.getTenantId();
        return actor != null && login != null && Objects.equals(actor, login.getId())
                && tenant != null && !Boolean.TRUE.equals(TenantContextHolder.isIgnore())
                && Objects.equals(tenant, login.getTenantId())
                && order != null && Objects.equals(tenant, order.getTenantId())
                && "ACTIVE".equals(order.getActiveStatus())
                && order.getBusinessStatus() != null
                && Set.of("ACTIVE", "COMPLETED", "VERSION_UPGRADE_PENDING").contains(order.getBusinessStatus())
                && permissions.hasAnyPermissions(actor, "mes:pro-in-process-order:query");
    }
}
