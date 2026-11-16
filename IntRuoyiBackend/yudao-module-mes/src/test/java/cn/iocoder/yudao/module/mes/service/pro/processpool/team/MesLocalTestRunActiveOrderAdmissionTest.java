package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesLocalTestRunActiveOrderAdmissionTest {
    @ParameterizedTest
    @ValueSource(strings = {"MANIFEST_REQUIRED", "TEST_MODE_DISABLED", "MANIFEST_ORDER_IDENTITY_MISMATCH", "TENANT_MISMATCH"})
    void localAdmissionFailureCannotReuseHistoricalOrderOrCreateRouteSnapshots(String reason) {
        var service = mock(MesTeamLeaderActiveOrderServiceImpl.class, CALLS_REAL_METHODS);
        var workOrders = mock(MesProWorkOrderService.class);
        var audit = mock(GxpAuditService.class);
        var guard = mock(MesLocalTestRunSourceGuard.class);
        var activeOrders = mock(MesProcessPoolActiveOrderMapper.class);
        var routes = mock(MesProRouteMapper.class);
        ReflectionTestUtils.setField(service, "workOrderService", workOrders);
        ReflectionTestUtils.setField(service, "gxpAuditService", audit);
        ReflectionTestUtils.setField(service, "localTestRunSourceGuard", guard);
        ReflectionTestUtils.setField(service, "activeOrderMapper", activeOrders);
        ReflectionTestUtils.setField(service, "routeMapper", routes);
        var workOrder = new MesProWorkOrderDO(); workOrder.setId(19L);
        workOrder.setOrderSourceCode("LOCAL_TEST_RUN:R01:A:77");
        when(workOrders.validateWorkOrderExists(19L)).thenReturn(workOrder);
        doThrow(new IllegalStateException(reason)).when(guard).requireProductionAdmission(workOrder);
        var command = MesTeamLeaderActiveOrderAddReqBO.builder().leaderUserId(2L).workOrderId(19L)
                .idempotencyKey("local-admission-19").build();
        assertEquals(reason, assertThrows(IllegalStateException.class, () -> service.addActiveOrder(command)).getMessage());
        verify(guard).requireProductionAdmission(workOrder);
        verifyNoInteractions(activeOrders, routes);
    }
}
