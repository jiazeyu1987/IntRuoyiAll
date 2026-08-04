package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderReportConfirmationServiceTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    @Mock
    private MesProcessPoolFifoAllocationService processPoolFifoAllocationService;
    @Mock
    private MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    @Mock
    private MesTeamLeaderOrderProcessCompletionService orderProcessCompletionService;

    private MesTeamLeaderReportConfirmationService service;

    @BeforeEach
    void setUp() {
        MesTeamLeaderFifoAllocationService fifoAllocationService =
                new MesTeamLeaderFifoAllocationService(activeOrderMapper, workOrderMapper, allocationMapper,
                        orderProcessTargetService);
        service = new MesTeamLeaderReportConfirmationServiceImpl(scopeService, eventMapper, activeOrderMapper,
                workOrderMapper, reviewMapper, allocationMapper, quantityFragmentMapper, pqcRecordMapper,
                fifoAllocationService, processPoolFifoAllocationService, pqcTaskMapper, pqcPieceDetailMapper,
                orderProcessTargetService, orderProcessCompletionService);
    }

    @Test
    void shouldConfirmSubmissionWithManualAllocationsToActiveOrders() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event("{\"outputQuantity\":80,\"pressure\":15}"));
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        givenSuccessPqcBinding(80);
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                activeOrder(8102L, 9002L, "2026-07-31T09:00:00")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L, 9002L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001", "200"),
                workOrder(9002L, "WO-9002", "200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L, 9002L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "160"), allocation(9002L, "160")));
        when(orderProcessTargetService.requireTarget(activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                5001L, 6001L)).thenReturn(target("600"));
        when(orderProcessTargetService.requireTarget(activeOrder(8102L, 9002L, "2026-07-31T09:00:00"),
                5001L, 6001L)).thenReturn(target("600"));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
            return 1;
        });
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        Long reviewId = service.confirmSubmission(MesTeamLeaderReportConfirmationReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                .reviewRemark("现场调整 O1/O2 各 40")
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                .allocations(List.of(line(8101L, "40"), line(8102L, "40")))
                .build());

        assertEquals(7001L, reviewId);
        verify(scopeService).assertCanAccessEmployee(3001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 2001L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolReportAllocationDO>> allocationCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(allocationMapper).insertBatch(allocationCaptor.capture());
        List<MesProcessPoolReportAllocationDO> savedLines = List.copyOf(allocationCaptor.getValue());
        assertEquals(2, savedLines.size());
        assertSavedLine(savedLines.get(0), 7001L, 8101L, 9001L, "40", MesProcessPoolReportAllocationDO.MODE_MANUAL);
        assertSavedLine(savedLines.get(1), 7001L, 8102L, 9002L, "40", MesProcessPoolReportAllocationDO.MODE_MANUAL);
        verify(orderProcessCompletionService).applyConfirmedAllocations(any(MesProProcessPoolEventDO.class),
                anyCollection());
    }

    @Test
    void shouldBlockWhenManualAllocationTargetsNonActiveOrder() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        givenSuccessPqcBinding(80);
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L, "2026-07-31T08:00:00")));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.confirmSubmission(
                MesTeamLeaderReportConfirmationReqBO.builder()
                        .eventId(1001L)
                        .leaderUserId(3001L)
                        .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                        .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                        .reviewSignatureId(9101L)
                        .reviewSignatureUserId(3001L)
                        .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                        .allocations(List.of(line(9999L, "80")))
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED.getCode(),
                ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void shouldBlockWhenAllocationTotalDoesNotEqualSubmittedQuantity() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        givenSuccessPqcBinding(80);
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L, "2026-07-31T08:00:00")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001", "200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of());
        when(orderProcessTargetService.requireTarget(activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                5001L, 6001L)).thenReturn(target("600"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.confirmSubmission(
                MesTeamLeaderReportConfirmationReqBO.builder()
                        .eventId(1001L)
                        .leaderUserId(3001L)
                        .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                        .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                        .reviewSignatureId(9101L)
                        .reviewSignatureUserId(3001L)
                        .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                        .allocations(List.of(line(8101L, "70")))
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void shouldBlockDuplicateConfirmationBeforeCreatingReview() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(allocation(9001L, "80")));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.confirmSubmission(
                MesTeamLeaderReportConfirmationReqBO.builder()
                        .eventId(1001L)
                        .leaderUserId(3001L)
                        .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                        .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                        .reviewSignatureId(9101L)
                        .reviewSignatureUserId(3001L)
                        .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                        .allocations(List.of(line(8101L, "80")))
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE.getCode(), ex.getCode());
        verify(scopeService).assertCanAccessEmployee(3001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 2001L);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void shouldConfirmManualAllocationAgainstPerProcessSnapshotTargetInsteadOfErpQuantity() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event("{\"outputQuantity\":300}"));
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        givenSuccessPqcBinding(300);
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L, "2026-07-31T08:00:00");
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(activeOrder));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001", "300")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "600")));
        when(orderProcessTargetService.requireTarget(activeOrder, 5001L, 6001L)).thenReturn(target("900"));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7002L);
            return 1;
        });
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        Long reviewId = service.confirmSubmission(MesTeamLeaderReportConfirmationReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                .allocations(List.of(line(8101L, "300")))
                .build());

        assertEquals(7002L, reviewId);
        verify(orderProcessTargetService).requireTarget(activeOrder, 5001L, 6001L);
        verify(allocationMapper).insertBatch(anyCollection());
    }

    private static MesTeamLeaderReportAllocationLineReqBO line(Long activeOrderId, String quantity) {
        return MesTeamLeaderReportAllocationLineReqBO.builder()
                .activeOrderId(activeOrderId)
                .allocatedQuantity(new BigDecimal(quantity))
                .build();
    }

    private static MesProProcessPoolEventDO event(String rawPayload) {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .poolId(100L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .actualEmployeeId(2001L)
                .rawPayload(rawPayload)
                .serverSubmitTime(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build();
    }

    private void givenSuccessPqcBinding(int qualifiedQuantity) {
        when(pqcRecordMapper.selectListByProductionSubmitEventId(1001L))
                .thenReturn(List.of(pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        when(eventMapper.selectByIdForUpdate(1101L)).thenReturn(pqcEvent());
        when(pqcTaskMapper.selectByIdForUpdate(5101L)).thenReturn(pqcTask(qualifiedQuantity));
        when(pqcPieceDetailMapper.selectListByTaskId(5101L)).thenReturn(successPqcPieces(qualifiedQuantity));
    }

    private static MesProProcessPoolPqcRecordDO pqcRecord(String inspectionResult) {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(1001L)
                .inspectionResult(inspectionResult)
                .serverSubmitTime(LocalDateTime.of(2026, 7, 31, 8, 45))
                .build();
    }

    private static MesProProcessPoolEventDO pqcEvent() {
        return MesProProcessPoolEventDO.builder()
                .id(1101L)
                .poolId(100L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .feedbackSourceId(5101L)
                .rawPayload("{\"inspectionResult\":\"SUCCESS\"}")
                .serverSubmitTime(LocalDateTime.of(2026, 7, 31, 8, 45))
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(Integer actualInspectionQuantity) {
        return MesPqcInspectionTaskDO.builder()
                .id(5101L)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .actualInspectionQuantity(actualInspectionQuantity)
                .taskStatus("SUBMITTED")
                .build();
    }

    private static List<MesPqcInspectionPieceDetailDO> successPqcPieces(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(sampleNo -> MesPqcInspectionPieceDetailDO.builder()
                        .id(5200L + sampleNo)
                        .taskId(5101L)
                        .sampleNo(sampleNo)
                        .itemCode("PQC-ITEM")
                        .judgement(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                        .build())
                .toList();
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long workOrderId, String joinedAt) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(3001L)
                .workOrderId(workOrderId)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.parse(joinedAt))
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, String code, String quantity) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .quantity(new BigDecimal(quantity))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long workOrderId, String quantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .activeOrderId(workOrderId.equals(9001L) ? 8101L : 8102L)
                .workOrderId(workOrderId)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .build();
    }

    private static MesTeamLeaderOrderProcessTarget target(String plannedQuantity) {
        return new MesTeamLeaderOrderProcessTarget(5001L, 6001L, new BigDecimal("300"),
                new BigDecimal("3.000000"), new BigDecimal(plannedQuantity));
    }

    private static void assertSavedLine(MesProcessPoolReportAllocationDO line, Long reviewId, Long activeOrderId,
                                        Long workOrderId, String quantity, String mode) {
        assertEquals(1001L, line.getEventId());
        assertEquals(reviewId, line.getReviewId());
        assertEquals(3001L, line.getLeaderUserId());
        assertEquals(activeOrderId, line.getActiveOrderId());
        assertEquals(workOrderId, line.getWorkOrderId());
        assertEquals(5001L, line.getRouteProcessId());
        assertEquals(6001L, line.getProcessId());
        assertEquals(mode, line.getAllocationMode());
        assertAmount(quantity, line.getAllocatedQuantity());
        assertNotNull(line.getConfirmedAt());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
