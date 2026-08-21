package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderSimulationServiceTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper reportAllocationMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper submissionReviewMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    @Mock
    private MesProcessPoolEventService processPoolEventService;
    @Mock
    private MesReportAllocationCommandService reportAllocationCommandService;
    @Mock
    private MesPqcProcessInspectionAggregationService pqcProcessInspectionAggregationService;

    private MesTeamLeaderActiveOrderSimulationService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderSimulationService(activeOrderMapper, processSnapshotMapper,
                routeVersionMapper, reportAllocationMapper, submissionReviewMapper, pqcInspectionTaskMapper,
                inspectionRegulationItemMapper, pqcPieceDetailMapper, processPoolEventService,
                reportAllocationCommandService, pqcProcessInspectionAggregationService);
    }

    @Test
    void simulateCompletionShouldUseFixedPqcTasksInsteadOfAllProductionProcesses() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder();
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.of(
                processSnapshot(5001L, 6001L),
                processSnapshot(5002L, 6002L));
        MesPqcInspectionTaskDO pendingPqcTask = pqcTask(MesPqcInspectionTaskDO.TASK_STATUS_PENDING);
        MesPqcInspectionTaskDO confirmedPqcTask = pqcTask(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                .setActualInspectionQuantity(1)
                .setSubmittedEventId(8001L);
        List<MesProcessPoolReportAllocationDO> completedAllocations = List.of(
                allocation(7001L, 5001L, 6001L),
                allocation(7002L, 5002L, 6002L));
        AtomicLong reviewId = new AtomicLong(9000L);

        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(activeOrder);
        when(routeVersionMapper.selectById(448L)).thenReturn(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .build());
        when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(snapshots);
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of(pendingPqcTask));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(List.of(), completedAllocations, completedAllocations);
        when(processPoolEventService.createEvent(any())).thenReturn(7001L, 7002L);
        when(reportAllocationMapper.selectListByEventIdForUpdate(7001L))
                .thenReturn(List.of(allocation(7001L, 5001L, 6001L)));
        when(reportAllocationMapper.selectListByEventIdForUpdate(7002L))
                .thenReturn(List.of(allocation(7002L, 5002L, 6002L)));
        when(reportAllocationMapper.updateById(any(MesProcessPoolReportAllocationDO.class))).thenReturn(1);
        when(submissionReviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(reviewId.incrementAndGet());
            return 1;
        });
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(List.of(inspectionItem()));
        when(pqcPieceDetailMapper.selectListByTaskId(8301L)).thenReturn(List.of());
        when(pqcPieceDetailMapper.insertBatch(any(List.class))).thenReturn(Boolean.TRUE);
        when(pqcInspectionTaskMapper.updateSubmittedIfPending(8301L, 1, "SIMULATED:8301:1",
                MesPqcInspectionTaskDO.TASK_STATUS_PENDING, MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED))
                .thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any())).thenReturn(8001L);
        when(pqcInspectionTaskMapper.updateSubmittedEventId(8301L, 8001L)).thenReturn(1);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(8101L))
                .thenReturn(List.of(confirmedPqcTask), List.of(confirmedPqcTask));

        MesTeamLeaderActiveOrderSimulationResult result = service.simulateActiveOrderCompletion(3001L, 8101L);

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(2, result.getProductionSubmitCount());
        assertEquals(2, result.getProductionReviewCount());
        assertEquals(1, result.getPqcSubmitCount());
        assertEquals(1, result.getPqcReviewCount());
        assertEquals(new BigDecimal("100.000000"), result.getProductionProgressPercent());
        assertEquals(new BigDecimal("100.000000"), result.getInspectionProgressPercent());
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .erpFixedQuantitySnapshot(new BigDecimal("200.000000"))
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .version(1)
                .build();
        activeOrder.setTenantId(1L);
        return activeOrder;
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(Long routeProcessId, Long processId) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .plannedQuantitySnapshot(new BigDecimal("200.000000"))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long eventId, Long routeProcessId, Long processId) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(eventId + 100L)
                .eventId(eventId)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .allocatedQuantity(new BigDecimal("200.000000"))
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(String status) {
        MesPqcInspectionTaskDO task = MesPqcInspectionTaskDO.builder()
                .id(8301L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(5001L)
                .processId(6001L)
                .qaProcessId(19902L)
                .qaItemCode("FIRST-001")
                .regulationVersionId(9902L)
                .inspectionType("FIRST")
                .inspectionRuleKey("FIRST")
                .businessDate(LocalDate.of(2026, 8, 20))
                .shiftCode("FIRST")
                .roundNo(1)
                .plannedInspectionQuantity(1)
                .actualInspectionQuantity(0)
                .taskStatus(status)
                .build();
        task.setTenantId(1L);
        return task;
    }

    private static MesQaInspectionRegulationItemDO inspectionItem() {
        return MesQaInspectionRegulationItemDO.builder()
                .id(990201L)
                .regulationVersionId(9902L)
                .qaProcessId(19902L)
                .itemSort(1)
                .inspectionType("FIRST")
                .itemCode("FIRST-001")
                .itemName("Appearance")
                .inspectionMethod("Visual")
                .standardText("Pass")
                .resultType("TEXT")
                .standardLowerLimit(BigDecimal.ZERO)
                .standardUpperLimit(BigDecimal.ONE)
                .standardPrecision(1)
                .build();
    }
}
