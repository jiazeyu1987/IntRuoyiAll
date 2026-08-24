package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReportAllocationFrontlineSnapshotGuardTest {

    @Mock private MesTeamLeaderScopeService scopeService;
    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProcessPoolReportAllocationStateMapper stateMapper;
    @Mock private MesProcessPoolReportAllocationAdjustmentAuditMapper auditMapper;
    @Mock private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock private MesReportAllocationPoolQuantityService poolQuantityService;
    @Mock private MesReportAllocationReleaseStateService releaseStateService;
    @Mock private MesTeamLeaderOrderProcessTargetService targetService;
    @Mock private MesTeamLeaderFifoAllocationService fifoService;
    @Mock private MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService;
    @Mock private MesReportAllocationQuantityFragmentService quantityFragmentService;
    @Mock private MesTeamLeaderOrderProcessCompletionService completionService;
    @Mock private MesProductionReportManagementSummaryService reportManagementSummaryService;
    @Mock private MesTeamLeaderOverageLimitService overageLimitService;

    private MesReportAllocationCommandService service;

    @BeforeEach
    void setUp() {
        service = new MesReportAllocationCommandService(scopeService, eventMapper, activeOrderMapper, workOrderMapper,
                allocationMapper, stateMapper, auditMapper, reviewMapper, poolQuantityService, releaseStateService,
                targetService, fifoService, routeStartAuthorizationService, quantityFragmentService,
                completionService, reportManagementSummaryService, overageLimitService);
    }

    @Test
    void frontlineInitialAllocationMustNotRequireOrderProcessTargetSnapshot() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L);
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(0).build();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(activeOrder);
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(true);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);

        service.createInitialAllocation(1001L, 8101L, new BigDecimal("300"));

        verify(targetService, never()).requireTarget(activeOrder, 5001L, 6001L);
        verify(targetService, never()).requireUniqueTargetForProcess(activeOrder, 6001L);
        ArgumentCaptor<Collection<MesProcessPoolReportAllocationDO>> allocationCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(allocationMapper).insertBatch(allocationCaptor.capture());
        MesProcessPoolReportAllocationDO allocation = allocationCaptor.getValue().iterator().next();
        assertEquals(5001L, allocation.getRouteProcessId());
        assertEquals(6001L, allocation.getProcessId());
        assertEquals(0, new BigDecimal("300").compareTo(allocation.getAllocatedQuantity()));
        verify(quantityFragmentService).rebuildForVersion(event, 1, List.of(allocation));
        verify(completionService, never()).reconcileAffectedAllocations(any(), anyCollection());
        verify(reportManagementSummaryService).refreshProductionEvent(event);
    }

    @Test
    void frontlineInitialAllocationMustStillRejectMismatchedActiveOrder() {
        MesProProcessPoolEventDO event = event();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(activeOrder(8101L, 9002L));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.createInitialAllocation(1001L, 8101L, new BigDecimal("300")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED.getCode(),
                error.getCode());
        verify(allocationMapper, never()).insertBatch(anyCollection());
        verify(targetService, never()).requireTarget(any(), any(), any());
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder().id(1001L).eventType("PRODUCTION_SUBMIT")
                .eventIdempotencyKey("P0-SUBMIT-F2-20260817-001").workOrderId(9001L)
                .deviceAccountId(9001L).reportOutputQuantity(new BigDecimal("300"))
                .actualEmployeeId(4001L).routeProcessId(5001L).processId(6001L).build();
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long workOrderId) {
        return MesProcessPoolActiveOrderDO.builder().id(id).leaderUserId(3001L).workOrderId(workOrderId)
                .activeStatus("ACTIVE").build();
    }
}
