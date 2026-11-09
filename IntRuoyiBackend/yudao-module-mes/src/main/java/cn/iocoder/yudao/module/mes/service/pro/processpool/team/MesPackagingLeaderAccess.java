package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** Cross-team order access never changes employee ownership or review authority. */
@Service
public class MesPackagingLeaderAccess {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper orders;
    public static final String ROLE = "packaging_leader_permission";
    private final PermissionApi permissions;
    private final AdminUserMapper users;
    public MesPackagingLeaderAccess(PermissionApi permissions, AdminUserMapper users) {
        this.permissions = permissions;
        this.users = users;
    }
    public boolean isPackagingLeader(Long actor) {
        Long tenant = TenantContextHolder.getTenantId();
        if (actor == null || tenant == null || Boolean.TRUE.equals(TenantContextHolder.isIgnore())) return false;
        var user = users.selectById(actor);
        return user != null && Objects.equals(user.getTenantId(), tenant)
                && Objects.equals(user.getStatus(), 0) && permissions.hasAnyRoles(actor, ROLE);
    }
    public boolean canAccess(Long actor, MesProcessPoolActiveOrderDO order) {
        return order != null && Objects.equals(order.getTenantId(), TenantContextHolder.getTenantId())
                && isPackagingLeader(actor);
    }
    public java.util.Set<Long> activeRouteIds(Long actor) {
        if (!isPackagingLeader(actor)) return java.util.Set.of();
        return orders.selectActiveListByLeader(actor, true).stream()
                .filter(order -> Objects.equals(order.getTenantId(), TenantContextHolder.getTenantId()))
                .map(MesProcessPoolActiveOrderDO::getRouteId).filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }
}
