package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED;

/** Read-only order visibility is independent of inspection task evidence and review ownership. */
@Service
public class MesPqcPackagingLeaderOrderService {
    public static final String ROLE = "pqc_packaging_leader_permission";
    private final PermissionApi permissions;
    private final AdminUserMapper users;
    private final MesProcessPoolActiveOrderMapper orders;
    private final MesProWorkOrderMapper workOrders;

    public MesPqcPackagingLeaderOrderService(PermissionApi permissions, AdminUserMapper users,
            MesProcessPoolActiveOrderMapper orders, MesProWorkOrderMapper workOrders) {
        this.permissions = permissions;
        this.users = users;
        this.orders = orders;
        this.workOrders = workOrders;
    }

    public List<OrderRow> listOrders(Long actor) {
        Long tenant = TenantContextHolder.getTenantId();
        if (actor == null || tenant == null || Boolean.TRUE.equals(TenantContextHolder.isIgnore())) {
            throw exception(PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "PQC包装组长租户上下文");
        }
        var user = users.selectById(actor);
        if (user == null || !Objects.equals(tenant, user.getTenantId())
                || !Objects.equals(0, user.getStatus()) || !permissions.hasAnyRoles(actor, ROLE)) {
            throw exception(PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "PQC包装组长角色");
        }
        var activeOrders = orders.selectActiveList();
        if (activeOrders.isEmpty()) return List.of();
        var byId = workOrders.selectBatchIds(activeOrders.stream().map(order -> order.getWorkOrderId()).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(order -> order.getId(), order -> order));
        return activeOrders.stream().map(order -> {
            var workOrder = byId.get(order.getWorkOrderId());
            if (!Objects.equals(tenant, order.getTenantId()) || workOrder == null
                    || !Objects.equals(tenant, workOrder.getTenantId())) {
                throw exception(PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "活跃订单正式租户或工单来源");
            }
            return new OrderRow(order.getId(), workOrder.getId(), workOrder.getCode(), workOrder.getName(),
                    workOrder.getQuantity(), order.getLeaderUserId(), order.getRouteId(), order.getBusinessStatus());
        }).toList();
    }

    public record OrderRow(Long activeOrderId, Long workOrderId, String workOrderCode, String workOrderName,
                           BigDecimal quantity, Long productionLeaderUserId, Long routeId, String businessStatus) {}
}
