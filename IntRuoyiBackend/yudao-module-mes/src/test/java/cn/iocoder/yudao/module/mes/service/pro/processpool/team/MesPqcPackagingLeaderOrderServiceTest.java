package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesPqcPackagingLeaderOrderServiceTest {
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AdminUserMapper users = mock(AdminUserMapper.class);
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProWorkOrderMapper workOrders = mock(MesProWorkOrderMapper.class);
    private final MesPqcPackagingLeaderOrderService service = new MesPqcPackagingLeaderOrderService(permissions, users, orders, workOrders);
    private AdminUserDO actor;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        actor = AdminUserDO.builder().id(20L).status(0).build(); actor.setTenantId(1L);
        when(users.selectById(20L)).thenReturn(actor);
        when(permissions.hasAnyRoles(20L, MesPqcPackagingLeaderOrderService.ROLE)).thenReturn(true);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void readsDifferentProductionLeadersWithoutRequiringInspectionTasksOrChangingOwners() {
        var a = order(1L, 30L); var b = order(2L, 40L);
        when(orders.selectActiveList()).thenReturn(List.of(a, b));
        when(workOrders.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(workOrder(1L), workOrder(2L)));
        var result = service.listOrders(20L);
        assertEquals(List.of(30L, 40L), result.stream().map(MesPqcPackagingLeaderOrderService.OrderRow::productionLeaderUserId).toList());
        assertEquals(List.of("WO-1", "WO-2"), result.stream().map(MesPqcPackagingLeaderOrderService.OrderRow::workOrderCode).toList());
        verify(orders).selectActiveList();
        verify(workOrders).selectBatchIds(List.of(1L, 2L));
        verifyNoMoreInteractions(orders, workOrders);
    }
    @Test void deniesOrdinaryPqcOrProductionPackagingRoleWithoutExplicitNewRole() {
        when(permissions.hasAnyRoles(20L, MesPqcPackagingLeaderOrderService.ROLE)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.listOrders(20L)); verifyNoInteractions(orders);
    }
    @Test void deniesDisabledAccount() { actor.setStatus(1); assertDenied(); }
    @Test void deniesForeignTenantAccount() { actor.setTenantId(2L); assertDenied(); }
    @Test void deniesMissingActor() { assertThrows(RuntimeException.class, () -> service.listOrders(null)); verifyNoInteractions(orders); }
    @Test void deniesMissingTenant() { TenantContextHolder.clear(); assertDenied(); }
    @Test void deniesTenantIsolationBypass() { TenantContextHolder.setIgnore(true); assertDenied(); }
    @Test void emptyOrderListRemainsEmptyWithoutQueryingBusinessSources() {
        when(orders.selectActiveList()).thenReturn(List.of()); assertTrue(service.listOrders(20L).isEmpty()); verifyNoInteractions(workOrders);
    }
    @Test void refusesForeignSourceInsteadOfReturningCrossTenantOrder() {
        var row=order(1L,30L); row.setTenantId(2L);
        when(orders.selectActiveList()).thenReturn(List.of(row));
        when(workOrders.selectBatchIds(List.of(1L))).thenReturn(List.of(workOrder(1L)));
        assertThrows(RuntimeException.class, () -> service.listOrders(20L));
    }
    @Test void refusesMissingWorkOrderInsteadOfCreatingPlaceholder() {
        when(orders.selectActiveList()).thenReturn(List.of(order(1L,30L)));
        when(workOrders.selectBatchIds(List.of(1L))).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> service.listOrders(20L));
    }
    private void assertDenied() { assertThrows(RuntimeException.class, () -> service.listOrders(20L)); verifyNoInteractions(orders); }
    private MesProcessPoolActiveOrderDO order(Long id, Long leader) {
        var row=MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(id).leaderUserId(leader).build(); row.setTenantId(1L); return row;
    }
    private MesProWorkOrderDO workOrder(Long id) {
        var row=new MesProWorkOrderDO(); row.setId(id); row.setCode("WO-"+id); row.setTenantId(1L); return row;
    }
}
