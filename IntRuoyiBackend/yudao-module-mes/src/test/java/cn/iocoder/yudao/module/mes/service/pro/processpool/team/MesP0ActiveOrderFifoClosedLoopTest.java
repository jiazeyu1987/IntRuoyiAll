package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
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
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoTargetWorkOrder;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0ActiveOrderFifoClosedLoopTest {

    private static final Long EVENT_ID = 1001L;
    private static final Long LEADER_USER_ID = 3001L;

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
    @Mock
    private MesWorkOrderAbnormalStateService abnormalStateService;

    private MesTeamLeaderReportConfirmationService service;

    @BeforeEach
    void setUp() {
        MesTeamLeaderFifoAllocationService fifoAllocationService =
                new MesTeamLeaderFifoAllocationService(activeOrderMapper, workOrderMapper, allocationMapper,
                        orderProcessTargetService, abnormalStateService);
        service = new MesTeamLeaderReportConfirmationServiceImpl(scopeService, eventMapper, activeOrderMapper,
                workOrderMapper, reviewMapper, allocationMapper, quantityFragmentMapper, pqcRecordMapper,
                fifoAllocationService, processPoolFifoAllocationService, pqcTaskMapper, pqcPieceDetailMapper,
                orderProcessTargetService, orderProcessCompletionService, abnormalStateService);
    }

    @Test
    void shouldPassOnlyThisConfirmationQuantitiesToFifoConsumptionAcrossActiveOrders() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        givenSuccessPqcBinding(80);
        when(activeOrderMapper.selectActiveListByLeader(LEADER_USER_ID)).thenReturn(List.of(
                activeOrder(8102L, 9002L, "REMOVED", "2026-08-03T07:30:00"),
                activeOrder(8103L, 9003L, "ACTIVE", "2026-08-03T09:00:00"),
                activeOrder(8101L, 9001L, "ACTIVE", "2026-08-03T08:00:00")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L, 9003L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001"), workOrder(9003L, "WO-9003")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L, 9003L), 5001L, 6001L))
                .thenReturn(List.of(allocation(8101L, 9001L, "150"), allocation(8103L, 9003L, "170")));
        when(orderProcessTargetService.requireTarget(any(MesProcessPoolActiveOrderDO.class), eq(5001L), eq(6001L)))
                .thenReturn(target("200"));
        when(quantityFragmentMapper.selectListByProductionSubmitEventIdForUpdate(EVENT_ID))
                .thenReturn(List.of(quantityFragment("80")));
        when(processPoolFifoAllocationService.allocate(any(MesProcessPoolFifoAllocationCommand.class)))
                .thenReturn(MesProcessPoolFifoAllocationResult.of(List.of(fifoLine(9001L, "50"),
                        fifoLine(9003L, "30")), new BigDecimal("80")));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
            return 1;
        });
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        service.confirmSubmission(signedFifoReqWithoutExplicitLines());

        ArgumentCaptor<MesProcessPoolFifoAllocationCommand> fifoCaptor =
                ArgumentCaptor.forClass(MesProcessPoolFifoAllocationCommand.class);
        verify(processPoolFifoAllocationService).allocate(fifoCaptor.capture());
        List<MesProcessPoolFifoTargetWorkOrder> targets = fifoCaptor.getValue().getTargetWorkOrders();
        assertEquals(List.of(9001L, 9003L), targets.stream()
                .map(MesProcessPoolFifoTargetWorkOrder::getWorkOrderId)
                .toList());
        assertAmount("50", targets.get(0).getRequiredQuantity());
        assertAmount("30", targets.get(1).getRequiredQuantity());
        assertAmount("150", targets.get(0).getAlreadyAllocatedQuantity());
        assertAmount("170", targets.get(1).getAlreadyAllocatedQuantity());
    }

    @Test
    void shouldRejectManualAllocationWhenCurrentProcessRemainingIsInsufficientBeforeTerminalWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        givenSuccessPqcBinding(80);
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L, "ACTIVE", "2026-08-03T08:00:00");
        when(activeOrderMapper.selectActiveListByLeader(LEADER_USER_ID)).thenReturn(List.of(activeOrder));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder(9001L, "WO-9001")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(8101L, 9001L, "150")));
        when(orderProcessTargetService.requireTarget(activeOrder, 5001L, 6001L)).thenReturn(target("200"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedManualReq(line(8101L, "80"))));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH.getCode(),
                ex.getCode());
        verifyNoTerminalWrites();
    }

    @Test
    void shouldRejectManualAllocationWhenTotalDoesNotMatchSubmittedQuantityBeforeTerminalWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        givenSuccessPqcBinding(80);
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L, "ACTIVE", "2026-08-03T08:00:00");
        when(activeOrderMapper.selectActiveListByLeader(LEADER_USER_ID)).thenReturn(List.of(activeOrder));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder(9001L, "WO-9001")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of());
        when(orderProcessTargetService.requireTarget(activeOrder, 5001L, 6001L)).thenReturn(target("200"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedManualReq(line(8101L, "70"))));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH.getCode(), ex.getCode());
        verifyNoTerminalWrites();
    }

    private void givenSuccessPqcBinding(int qualifiedQuantity) {
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID))
                .thenReturn(List.of(pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        when(eventMapper.selectByIdForUpdate(1101L)).thenReturn(pqcEvent());
        when(pqcTaskMapper.selectByIdForUpdate(5101L)).thenReturn(pqcTask(qualifiedQuantity));
        when(pqcPieceDetailMapper.selectListByTaskId(5101L)).thenReturn(successPqcPieces(qualifiedQuantity));
    }

    private void verifyNoTerminalWrites() {
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
        verify(orderProcessCompletionService, never()).applyConfirmedAllocations(any(MesProProcessPoolEventDO.class),
                anyCollection());
    }

    private static MesTeamLeaderReportConfirmationReqBO signedFifoReqWithoutExplicitLines() {
        return baseReq().allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO).allocations(null).build();
    }

    private static MesTeamLeaderReportConfirmationReqBO signedManualReq(
            MesTeamLeaderReportAllocationLineReqBO line) {
        return baseReq().allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                .allocations(List.of(line))
                .build();
    }

    private static MesTeamLeaderReportConfirmationReqBO.MesTeamLeaderReportConfirmationReqBOBuilder baseReq() {
        return MesTeamLeaderReportConfirmationReqBO.builder()
                .eventId(EVENT_ID)
                .leaderUserId(LEADER_USER_ID)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .reviewRemark("P0 active-order FIFO confirmation")
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(LEADER_USER_ID)
                .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}");
    }

    private static MesTeamLeaderReportAllocationLineReqBO line(Long activeOrderId, String quantity) {
        return MesTeamLeaderReportAllocationLineReqBO.builder()
                .activeOrderId(activeOrderId)
                .allocatedQuantity(new BigDecimal(quantity))
                .build();
    }

    private static MesProProcessPoolEventDO event(String rawPayload) {
        return MesProProcessPoolEventDO.builder()
                .id(EVENT_ID)
                .poolId(100L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .actualEmployeeId(2001L)
                .rawPayload(rawPayload)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 10, 0))
                .build();
    }

    private static MesProProcessPoolPqcRecordDO pqcRecord(String inspectionResult) {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(inspectionResult)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 10, 5))
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
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 10, 5))
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

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long workOrderId, String status,
                                                           String joinedAt) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(LEADER_USER_ID)
                .workOrderId(workOrderId)
                .activeStatus(status)
                .joinedAt(LocalDateTime.parse(joinedAt))
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, String code) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .quantity(new BigDecimal("200"))
                .plannedStartTime(LocalDateTime.of(2026, 8, 3, 9, 0))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long activeOrderId, Long workOrderId,
                                                               String quantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .activeOrderId(activeOrderId)
                .workOrderId(workOrderId)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .build();
    }

    private static MesTeamLeaderOrderProcessTarget target(String plannedQuantity) {
        return new MesTeamLeaderOrderProcessTarget(5001L, 6001L, new BigDecimal("200"),
                BigDecimal.ONE, new BigDecimal(plannedQuantity));
    }

    private static MesProProcessPoolQuantityFragmentDO quantityFragment(String quantity) {
        return MesProProcessPoolQuantityFragmentDO.builder()
                .id(4101L)
                .poolId(100L)
                .eventId(EVENT_ID)
                .productionSubmitEventId(EVENT_ID)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .totalQuantity(new BigDecimal(quantity))
                .build();
    }

    private static MesProcessPoolFifoAllocationLineDO fifoLine(Long workOrderId, String quantity) {
        return MesProcessPoolFifoAllocationLineDO.builder()
                .sourceQuantityFragmentId(4101L)
                .targetWorkOrderId(workOrderId)
                .allocatedQuantity(new BigDecimal(quantity))
                .allocationStatus(MesProcessPoolFifoAllocationLineDO.STATUS_ALLOCATED)
                .build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
