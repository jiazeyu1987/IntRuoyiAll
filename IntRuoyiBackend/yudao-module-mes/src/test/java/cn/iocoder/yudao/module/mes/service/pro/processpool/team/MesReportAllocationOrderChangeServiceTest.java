package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReportAllocationOrderChangeServiceTest {

    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProcessPoolReportAllocationStateMapper stateMapper;
    @Mock private MesProcessPoolReportAllocationAdjustmentAuditMapper auditMapper;
    @Mock private MesReportAllocationReleaseStateService releaseStateService;
    @Mock private MesTeamLeaderOrderProcessTargetService targetService;
    @Mock private MesReportAllocationQuantityFragmentService fragmentService;
    @Mock private MesTeamLeaderOrderProcessCompletionService completionService;
    @Mock private MesProductionReportManagementSummaryService reportManagementSummaryService;

    private MesReportAllocationOrderChangeService service;

    @BeforeEach
    void setUp() {
        service = new MesReportAllocationOrderChangeService(activeOrderMapper, eventMapper, allocationMapper,
                stateMapper, auditMapper, releaseStateService, targetService, fragmentService, completionService,
                reportManagementSummaryService);
    }

    @Test
    void invalidatingActiveOrderMustReturnOnlyItsUnreleasedAllocationToSourceEvent() {
        MesProcessPoolReportAllocationDO allocationA = allocation(7101L, 8101L, 9001L, "100");
        MesProcessPoolReportAllocationDO allocationB = allocation(7102L, 8102L, 9002L, "20");
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationStateDO state = state();
        when(allocationMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of(allocationA));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(Set.of(8101L))).thenReturn(Set.of());
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(allocationA, allocationB));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.supersedeCurrentRows(List.of(7101L), 2)).thenReturn(1);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);

        service.invalidateActiveOrder(8101L, 3001L, "活跃订单移除");

        verify(fragmentService).rebuildForVersion(event, 2, List.of(allocationB));
        verify(completionService).reconcileAffectedAllocations(event, List.of(allocationA));
        ArgumentCaptor<Collection<MesProcessPoolReportAllocationAdjustmentAuditDO>> audits =
                ArgumentCaptor.forClass(Collection.class);
        verify(auditMapper).insertBatch(audits.capture());
        MesProcessPoolReportAllocationAdjustmentAuditDO audit = audits.getValue().iterator().next();
        assertAmount("100", audit.getBeforeQuantity());
        assertAmount("0", audit.getAfterQuantity());
        assertAmount("-100", audit.getDeltaQuantity());
        assertEquals(MesProcessPoolReportAllocationAdjustmentAuditDO.SOURCE_ORDER_CHANGE,
                audit.getChangeSource());
        InOrder order = inOrder(allocationMapper, fragmentService, completionService, stateMapper);
        order.verify(allocationMapper).supersedeCurrentRows(List.of(7101L), 2);
        order.verify(fragmentService).rebuildForVersion(event, 2, List.of(allocationB));
        order.verify(completionService).reconcileAffectedAllocations(event, List.of(allocationA));
        order.verify(stateMapper).updateById(state);
    }

    @Test
    void formallyReleasedActiveOrderMustRemainUnchangedDuringOrderChange() {
        MesProcessPoolReportAllocationDO allocationA = allocation(7101L, 8101L, 9001L, "100");
        when(allocationMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of(allocationA));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(Set.of(8101L)))
                .thenReturn(Set.of(8101L));

        service.invalidateActiveOrder(8101L, 3001L, "工单取消");

        verify(eventMapper, never()).selectByIdForUpdate(1001L);
        verify(allocationMapper, never()).supersedeCurrentRows(anyCollection(), org.mockito.ArgumentMatchers.any());
        verify(auditMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void workOrderDecreaseMustKeepEarliestQuantityAndReturnOnlyExcess() {
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder().id(8101L)
                .workOrderId(9001L).activeStatus("ACTIVE").build();
        MesProcessPoolReportAllocationDO allocationA = allocation(7101L, 8101L, 9001L, "100");
        MesProcessPoolReportAllocationDO allocationB = allocation(7102L, 8102L, 9002L, "20");
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationStateDO state = state();
        when(activeOrderMapper.selectListByWorkOrderIdForUpdate(9001L)).thenReturn(List.of(activeOrder));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of(allocationA));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(Set.of(8101L))).thenReturn(Set.of());
        when(targetService.requireTarget(8101L, 9001L, 5101L, 6001L)).thenReturn(
                new MesTeamLeaderOrderProcessTarget(5101L, 6001L, new BigDecimal("100"), BigDecimal.ONE,
                        new BigDecimal("100")));
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(allocationA, allocationB));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.supersedeCurrentRows(List.of(7101L), 2)).thenReturn(1);
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(true);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);

        service.reduceWorkOrderAllocations(9001L, new BigDecimal("60"), 3001L, "工单数量减少");

        ArgumentCaptor<Collection<MesProcessPoolReportAllocationDO>> next = ArgumentCaptor.forClass(Collection.class);
        verify(fragmentService).rebuildForVersion(org.mockito.ArgumentMatchers.eq(event),
                org.mockito.ArgumentMatchers.eq(2), next.capture());
        MesProcessPoolReportAllocationDO reducedA = next.getValue().stream()
                .filter(row -> row.getActiveOrderId().equals(8101L)).findFirst().orElseThrow();
        assertAmount("60", reducedA.getAllocatedQuantity());
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder().id(1001L).poolId(2001L).routeProcessId(5001L)
                .processId(6001L).build();
    }

    private static MesProcessPoolReportAllocationStateDO state() {
        return MesProcessPoolReportAllocationStateDO.builder().id(7201L).eventId(1001L).currentVersion(1).build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id, Long activeOrderId, Long workOrderId,
                                                                String quantity) {
        return MesProcessPoolReportAllocationDO.builder().id(id).eventId(1001L).reviewId(7301L)
                .leaderUserId(3001L).activeOrderId(activeOrderId).workOrderId(workOrderId)
                .routeProcessId(5101L).processId(6001L).allocatedQuantity(new BigDecimal(quantity))
                .allocationMode("MANUAL").lifecycleStatus("CURRENT").createdVersion(1).build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
