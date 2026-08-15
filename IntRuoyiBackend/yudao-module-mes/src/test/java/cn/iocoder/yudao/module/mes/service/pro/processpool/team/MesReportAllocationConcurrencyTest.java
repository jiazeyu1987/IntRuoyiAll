package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReportAllocationConcurrencyTest {

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

    private MesReportAllocationCommandService service;

    @BeforeEach
    void setUp() {
        service = new MesReportAllocationCommandService(scopeService, eventMapper, activeOrderMapper, workOrderMapper,
                allocationMapper, stateMapper, auditMapper, reviewMapper, poolQuantityService, releaseStateService,
                targetService, fifoService, routeStartAuthorizationService, quantityFragmentService,
                completionService, reportManagementSummaryService);
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(3001L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(5001L).processId(6001L).build()));
    }

    @Test
    void staleClientVersionMustFailBeforeAnyAllocationSideEffect() {
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder().id(1001L).poolId(2001L)
                .routeProcessId(5001L).processId(6001L).build();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(
                MesProcessPoolReportAllocationStateDO.builder().id(7201L).eventId(1001L)
                        .currentVersion(2).build());
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.save(
                MesReportAllocationSaveCommand.builder().eventId(1001L).leaderUserId(3001L)
                        .leaderType("PRODUCTION").expectedVersion(1).idempotencyKey("stale-request")
                        .allocationMode("MANUAL").reason("stale").allocations(List.of()).build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(activeOrderMapper, never()).selectActiveListByLeaderForUpdate(3001L);
        verify(allocationMapper, never()).supersedeCurrentRows(anyCollection(), org.mockito.ArgumentMatchers.any());
        verify(auditMapper, never()).insertBatch(anyCollection());
        verify(quantityFragmentService, never()).rebuildForVersion(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyCollection());
        verify(completionService, never()).reconcileAffectedAllocations(
                org.mockito.ArgumentMatchers.any(), anyCollection());
    }
}
