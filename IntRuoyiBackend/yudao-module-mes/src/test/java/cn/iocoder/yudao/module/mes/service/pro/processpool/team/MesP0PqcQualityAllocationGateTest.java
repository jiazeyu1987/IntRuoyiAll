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
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolAllocatableQuantityFragment;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoTargetWorkOrder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0PqcQualityAllocationGateTest {

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
    private MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    @Mock
    private MesProcessPoolFifoAllocationService processPoolFifoAllocationService;
    @Mock
    private MesTeamLeaderOrderProcessCompletionService orderProcessCompletionService;
    @Mock
    private MesWorkOrderAbnormalStateService abnormalStateService;
    @Mock
    private MesProductionReportManagementSummaryService reportManagementSummaryService;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;

    private MesTeamLeaderReportConfirmationService service;

    @BeforeEach
    void setUp() {
        MesTeamLeaderFifoAllocationService fifoAllocationService =
                new MesTeamLeaderFifoAllocationService(activeOrderMapper, workOrderMapper, allocationMapper,
                        orderProcessTargetService, abnormalStateService);
        service = new MesTeamLeaderReportConfirmationServiceImpl(scopeService, eventMapper, activeOrderMapper,
                workOrderMapper, reviewMapper, allocationMapper, quantityFragmentMapper, pqcRecordMapper,
                fifoAllocationService, processPoolFifoAllocationService, pqcTaskMapper, pqcPieceDetailMapper,
                orderProcessTargetService, orderProcessCompletionService, abnormalStateService,
                reportManagementSummaryService);
        ReflectionTestUtils.setField(service, "signatureService", signatureService);
        lenient().when(signatureService.recordTeamLeaderReviewSignature(any(), any(), any())).thenReturn(9101L);
    }

    @Test
    void shouldRejectPqcInspectionChildEventAsConfirmationRootBeforeAnyTerminalWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event(
                MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION, "{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedConfirmReq("80")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ROOT_EVENT_REQUIRED.getCode(),
                ex.getCode());
        verifyNoTerminalWrites();
    }

    @Test
    void shouldRejectMissingStructuredPqcBindingBeforeAnyTerminalWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(productionSubmitEvent("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID)).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedConfirmReq("80")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED.getCode(),
                ex.getCode());
        verifyNoTerminalWrites();
    }

    @Test
    void shouldRejectFailedPqcBindingBeforeAnyTerminalWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(productionSubmitEvent("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID)).thenReturn(List.of(
                pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedConfirmReq("80")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE.getCode(),
                ex.getCode());
        verifyNoTerminalWrites();
    }

    @Test
    void shouldPersistConfirmationOnlyAfterSuccessPqcBindingIsVerified() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(productionSubmitEvent("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID)).thenReturn(List.of(
                pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        when(eventMapper.selectByIdForUpdate(1101L)).thenReturn(pqcEvent(5101L));
        when(pqcTaskMapper.selectByIdForUpdate(5101L)).thenReturn(pqcTask(80));
        when(pqcPieceDetailMapper.selectListByTaskId(5101L)).thenReturn(List.of(
                pqcPiece(1, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(2, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(3, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(4, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(5, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(6, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(7, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(8, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(9, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(10, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(11, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(12, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(13, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(14, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(15, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(16, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(17, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(18, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(19, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(20, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(21, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(22, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(23, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(24, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(25, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(26, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(27, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(28, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(29, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(30, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(31, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(32, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(33, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(34, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(35, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(36, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(37, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(38, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(39, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(40, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(41, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(42, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(43, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(44, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(45, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(46, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(47, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(48, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(49, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(50, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(51, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(52, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(53, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(54, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(55, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(56, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(57, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(58, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(59, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(60, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(61, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(62, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(63, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(64, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(65, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(66, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(67, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(68, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(69, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(70, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(71, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(72, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(73, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(74, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(75, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(76, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(77, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(78, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(79, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(80, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        MesProcessPoolActiveOrderDO activeOrder = activeOrder();
        when(activeOrderMapper.selectActiveListByLeader(LEADER_USER_ID)).thenReturn(List.of(activeOrder));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder()));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of());
        when(orderProcessTargetService.requireTarget(activeOrder, 5001L, 6001L)).thenReturn(target("80"));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
            return 1;
        });
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        Long reviewId = service.confirmSubmission(signedConfirmReq("80"));

        assertEquals(7001L, reviewId);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(EVENT_ID, reviewCaptor.getValue().getEventId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolReportAllocationDO>> allocationCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(allocationMapper).insertBatch(allocationCaptor.capture());
        assertEquals(1, allocationCaptor.getValue().size());
    }

    @Test
    void shouldRejectSuccessPqcWhenAnyFormalSampleFailsBeforeTerminalWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(productionSubmitEvent("{\"outputQuantity\":2}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID)).thenReturn(List.of(
                pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        when(eventMapper.selectByIdForUpdate(1101L)).thenReturn(pqcEvent(5101L));
        when(pqcTaskMapper.selectByIdForUpdate(5101L)).thenReturn(pqcTask(2));
        when(pqcPieceDetailMapper.selectListByTaskId(5101L)).thenReturn(List.of(
                pqcPiece(1, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(2, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedConfirmReq("2")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE.getCode(),
                ex.getCode());
        verifyNoTerminalWrites();
    }

    @Test
    void shouldPersistFifoConsumptionFromProductionSubmitFragmentsBeforeTerminalWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(productionSubmitEvent("{\"outputQuantity\":2}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID)).thenReturn(List.of(
                pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        when(eventMapper.selectByIdForUpdate(1101L)).thenReturn(pqcEvent(5101L));
        when(pqcTaskMapper.selectByIdForUpdate(5101L)).thenReturn(pqcTask(2));
        when(pqcPieceDetailMapper.selectListByTaskId(5101L)).thenReturn(List.of(
                pqcPiece(1, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(2, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        MesProcessPoolActiveOrderDO activeOrder = activeOrder();
        when(activeOrderMapper.selectActiveListByLeader(LEADER_USER_ID)).thenReturn(List.of(activeOrder));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder()));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of());
        when(orderProcessTargetService.requireTarget(activeOrder, 5001L, 6001L)).thenReturn(target("2"));
        when(quantityFragmentMapper.selectListByProductionSubmitEventIdForUpdate(EVENT_ID)).thenReturn(List.of(
                quantityFragment("2")));
        when(processPoolFifoAllocationService.allocate(any(MesProcessPoolFifoAllocationCommand.class)))
                .thenReturn(MesProcessPoolFifoAllocationResult.of(List.of(fifoLine("2")), new BigDecimal("2")));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
            return 1;
        });
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        Long reviewId = service.confirmSubmission(signedFifoConfirmReq("2"));

        assertEquals(7001L, reviewId);
        ArgumentCaptor<MesProcessPoolFifoAllocationCommand> fifoCaptor =
                ArgumentCaptor.forClass(MesProcessPoolFifoAllocationCommand.class);
        verify(processPoolFifoAllocationService).allocate(fifoCaptor.capture());
        MesProcessPoolFifoAllocationCommand command = fifoCaptor.getValue();
        assertEquals(1, command.getFragments().size());
        MesProcessPoolAllocatableQuantityFragment fragment = command.getFragments().get(0);
        assertEquals(4101L, fragment.getSourceQuantityFragmentId());
        assertEquals(EVENT_ID, fragment.getSourceEventId());
        assertEquals(0, new BigDecimal("2").compareTo(fragment.getQuantity()));
        assertEquals(1, command.getTargetWorkOrders().size());
        MesProcessPoolFifoTargetWorkOrder target = command.getTargetWorkOrders().get(0);
        assertEquals(9001L, target.getWorkOrderId());
        assertEquals("WO-9001", target.getWorkOrderCode());
        assertEquals(0, new BigDecimal("2").compareTo(target.getRequiredQuantity()));
        verify(reviewMapper).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper).insertBatch(anyCollection());
    }

    @Test
    void shouldRejectFifoConfirmationWhenPersistedConsumptionLeavesQualifiedQuantityShort() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(productionSubmitEvent("{\"outputQuantity\":2}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID)).thenReturn(List.of(
                pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        when(eventMapper.selectByIdForUpdate(1101L)).thenReturn(pqcEvent(5101L));
        when(pqcTaskMapper.selectByIdForUpdate(5101L)).thenReturn(pqcTask(2));
        when(pqcPieceDetailMapper.selectListByTaskId(5101L)).thenReturn(List.of(
                pqcPiece(1, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS),
                pqcPiece(2, MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        MesProcessPoolActiveOrderDO activeOrder = activeOrder();
        when(activeOrderMapper.selectActiveListByLeader(LEADER_USER_ID)).thenReturn(List.of(activeOrder));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder()));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of());
        when(orderProcessTargetService.requireTarget(activeOrder, 5001L, 6001L)).thenReturn(target("2"));
        when(quantityFragmentMapper.selectListByProductionSubmitEventIdForUpdate(EVENT_ID)).thenReturn(List.of(
                quantityFragment("2")));
        when(processPoolFifoAllocationService.allocate(any(MesProcessPoolFifoAllocationCommand.class)))
                .thenReturn(MesProcessPoolFifoAllocationResult.of(List.of(fifoLine("1")), BigDecimal.ONE));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedFifoConfirmReq("2")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_QUANTITY_MISMATCH.getCode(),
                ex.getCode());
        verifyNoTerminalWrites();
    }

    @Test
    void shouldRejectDuplicateOrConcurrentConfirmationWithLockedAllocationStateBeforeDownstreamWrites() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(productionSubmitEvent("{\"outputQuantity\":2}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of(existingAllocation()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmSubmission(signedFifoConfirmReq("2")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE.getCode(), ex.getCode());
        verify(allocationMapper).selectListByEventIdForUpdate(EVENT_ID);
        verify(pqcRecordMapper, never()).selectListByProductionSubmitEventId(any());
        verify(quantityFragmentMapper, never()).selectListByProductionSubmitEventIdForUpdate(any());
        verify(processPoolFifoAllocationService, never()).allocate(any(MesProcessPoolFifoAllocationCommand.class));
        verifyNoTerminalWrites();
    }

    private void verifyNoTerminalWrites() {
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
        verify(orderProcessCompletionService, never()).applyConfirmedAllocations(any(MesProProcessPoolEventDO.class),
                anyCollection());
    }

    private static MesTeamLeaderReportConfirmationReqBO signedFifoConfirmReq(String quantity) {
        return MesTeamLeaderReportConfirmationReqBO.builder()
                .eventId(EVENT_ID)
                .leaderUserId(LEADER_USER_ID)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .reviewRemark("确认 FIFO 分配")
                .signaturePassword("sign-123")
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(LEADER_USER_ID)
                .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                .allocations(List.of(MesTeamLeaderReportAllocationLineReqBO.builder()
                        .activeOrderId(8101L)
                        .allocatedQuantity(new BigDecimal(quantity))
                        .build()))
                .build();
    }

    private static MesTeamLeaderReportConfirmationReqBO signedConfirmReq(String quantity) {
        return MesTeamLeaderReportConfirmationReqBO.builder()
                .eventId(EVENT_ID)
                .leaderUserId(LEADER_USER_ID)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                .reviewRemark("确认 FIFO 分配")
                .signaturePassword("sign-123")
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(LEADER_USER_ID)
                .reviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                .allocations(List.of(MesTeamLeaderReportAllocationLineReqBO.builder()
                        .activeOrderId(8101L)
                        .allocatedQuantity(new BigDecimal(quantity))
                        .build()))
                .build();
    }

    private static MesProProcessPoolEventDO productionSubmitEvent(String rawPayload) {
        return event(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT, rawPayload);
    }

    private static MesProProcessPoolEventDO event(String eventType, String rawPayload) {
        return MesProProcessPoolEventDO.builder()
                .id(EVENT_ID)
                .poolId(100L)
                .eventType(eventType)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .actualEmployeeId(2001L)
                .rawPayload(rawPayload)
                .reportOutputQuantity(JSON.parseObject(rawPayload).getBigDecimal("outputQuantity"))
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 10, 0))
                .build();
    }

    private static MesProProcessPoolEventDO pqcEvent(Long pqcTaskId) {
        return MesProProcessPoolEventDO.builder()
                .id(1101L)
                .poolId(100L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .feedbackSourceId(pqcTaskId)
                .rawPayload("{\"inspectionResult\":\"SUCCESS\"}")
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 10, 5))
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

    private static MesPqcInspectionTaskDO pqcTask(Integer actualInspectionQuantity) {
        return MesPqcInspectionTaskDO.builder()
                .id(5101L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .actualInspectionQuantity(actualInspectionQuantity)
                .taskStatus("SUBMITTED")
                .build();
    }

    private static MesPqcInspectionPieceDetailDO pqcPiece(Integer sampleNo, String judgement) {
        return MesPqcInspectionPieceDetailDO.builder()
                .id(5200L + sampleNo)
                .taskId(5101L)
                .sampleNo(sampleNo)
                .itemCode("PQC-ITEM")
                .judgement(judgement)
                .build();
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(LEADER_USER_ID)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 8, 3, 8, 0))
                .build();
    }

    private static MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .quantity(new BigDecimal("80"))
                .plannedStartTime(LocalDateTime.of(2026, 8, 3, 9, 0))
                .build();
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
                .sourceQuantityType("OUTPUT")
                .totalQuantity(new BigDecimal(quantity))
                .allocatedQuantity(BigDecimal.ZERO)
                .availableQuantity(new BigDecimal(quantity))
                .allocationStatus(MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE)
                .locked(Boolean.FALSE)
                .build();
    }

    private static MesProcessPoolFifoAllocationLineDO fifoLine(String quantity) {
        return MesProcessPoolFifoAllocationLineDO.builder()
                .sourceQuantityFragmentId(4101L)
                .targetWorkOrderId(9001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .allocationStatus(MesProcessPoolFifoAllocationLineDO.STATUS_ALLOCATED)
                .build();
    }

    private static MesProcessPoolReportAllocationDO existingAllocation() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(7301L)
                .eventId(EVENT_ID)
                .reviewId(7001L)
                .leaderUserId(LEADER_USER_ID)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal("2"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 3, 10, 20))
                .build();
    }

    private static MesTeamLeaderOrderProcessTarget target(String plannedQuantity) {
        return new MesTeamLeaderOrderProcessTarget(5001L, 6001L, new BigDecimal("80"),
                BigDecimal.ONE, new BigDecimal(plannedQuantity));
    }
}
