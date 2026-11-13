package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProAdmissionListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProAdmissionListServiceTest {
    @Test
    void validatesEachCandidateProductOnceAndFetchesOnlySqlSelectedWorkOrders() {
        TenantContextHolder.setTenantId(1L);
        try {
            var fixture = new Fixture();
            when(fixture.admission.selectRouteCheckProductIds(eq(1L), any())).thenReturn(List.of(21L, 22L));
            when(fixture.routeProduct.selectListByItemId(any())).thenReturn(List.of());
            when(fixture.admission.selectSummary(eq(1L), any(), any(), anyList(), anyList())).thenReturn(
                    Map.of("total", 103L, "warnCount", 0, "readyCount", 0, "alreadyAdmittedCount", 0, "blockedCount", 103));
            var request = new MesProScheduleOrderAdmissionDiffPageReqVO();
            request.setPageNo(3); request.setPageSize(10); request.setMessage("未绑定");
            when(fixture.admission.selectPageIds(eq(1L), any(), any(), anyList(), anyList(), same(request), eq(20L))).thenReturn(List.of(102L, 101L));
            when(fixture.workOrder.selectListByIds(List.of(102L, 101L))).thenReturn(List.of(
                    MesProWorkOrderDO.builder().id(101L).productId(21L).status(1).build(),
                    MesProWorkOrderDO.builder().id(102L).productId(21L).status(1).build()));
            when(fixture.schedule.selectListByWorkOrderIds(any())).thenReturn(List.of());
            when(fixture.sync.selectByWorkOrderId(anyLong())).thenAnswer(invocation -> MesKingdeeProductionOrderSyncRecordDO.builder()
                    .workOrderId(invocation.getArgument(0)).sourceFid("ERP-ID").sourceBillNo("ERP-BILL").build());
            var result = fixture.service.getAdmissionDiff(request);
            assertEquals(103L, result.getTotal());
            assertEquals(List.of(102L, 101L), result.getList().stream().map(row -> row.getWorkOrderId()).toList());
            assertTrue(result.getList().stream().allMatch(row -> "BLOCKED_MISSING_ROUTE".equals(row.getReasonCode())));
            assertEquals(103, result.getSummary().getBlockedCount());
            verify(fixture.routeProduct).selectListByItemId(21L);
            verify(fixture.routeProduct).selectListByItemId(22L);
            verifyNoMoreInteractions(fixture.routeProduct);
            verify(fixture.workOrder).selectListByIds(List.of(102L, 101L));
            verifyNoMoreInteractions(fixture.workOrder);
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void malformedColumnStopsBeforeRealtimeReadsAndValidatorFailureIsNotSwallowed() {
        TenantContextHolder.setTenantId(1L);
        try {
            var fixture = new Fixture();
            var request = new MesProScheduleOrderAdmissionDiffPageReqVO();
            request.setListFilters("[{\"key\":\"untrusted\",\"operator\":\"eq\",\"value\":1}]");
            assertThrows(RuntimeException.class, () -> fixture.service.getAdmissionDiff(request));
            verifyNoInteractions(fixture.admission, fixture.workOrder, fixture.routeProduct);
            request.setListFilters(null);
            when(fixture.admission.selectRouteCheckProductIds(eq(1L), any())).thenReturn(List.of(21L));
            var failure = new IllegalStateException("authoritative configuration failure");
            when(fixture.routeProduct.selectListByItemId(21L)).thenThrow(failure);
            assertSame(failure, assertThrows(IllegalStateException.class, () -> fixture.service.getAdmissionDiff(request)));
            verify(fixture.admission, never()).selectSummary(any(), any(), any(), anyList(), anyList());
        } finally { TenantContextHolder.clear(); }
    }

    private static final class Fixture {
        final MesProScheduleOrderServiceImpl service = new MesProScheduleOrderServiceImpl();
        final MesProAdmissionListMapper admission = mock(MesProAdmissionListMapper.class);
        final MesProWorkOrderMapper workOrder = mock(MesProWorkOrderMapper.class);
        final MesProScheduleOrderMapper schedule = mock(MesProScheduleOrderMapper.class);
        final MesProRouteProductMapper routeProduct = mock(MesProRouteProductMapper.class);
        final MesKingdeeProductionOrderSyncRecordMapper sync = mock(MesKingdeeProductionOrderSyncRecordMapper.class);
        Fixture() {
            ReflectionTestUtils.setField(service, "admissionListMapper", admission);
            ReflectionTestUtils.setField(service, "workOrderMapper", workOrder);
            ReflectionTestUtils.setField(service, "scheduleOrderMapper", schedule);
            ReflectionTestUtils.setField(service, "routeProductMapper", routeProduct);
            ReflectionTestUtils.setField(service, "syncRecordMapper", sync);
        }
    }
}
