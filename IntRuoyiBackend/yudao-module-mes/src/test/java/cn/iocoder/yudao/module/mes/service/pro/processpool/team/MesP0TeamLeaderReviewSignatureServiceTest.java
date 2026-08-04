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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0TeamLeaderReviewSignatureServiceTest {

    private static final Long EVENT_ID = 1001L;
    private static final Long LEADER_USER_ID = 3001L;
    private static final Long REVIEW_SIGNATURE_ID = 9101L;
    private static final Long REVIEW_SIGNATURE_USER_ID = 3001L;
    private static final String REVIEW_SIGNATURE_SNAPSHOT = "{\"signature\":\"team-leader-review\"}";

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
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
    private MesPqcProcessInspectionAggregationService processInspectionAggregationService;

    private MesTeamLeaderSubmissionReviewService submissionReviewService;
    private MesTeamLeaderReportConfirmationService reportConfirmationService;

    @BeforeEach
    void setUp() {
        submissionReviewService = new MesTeamLeaderSubmissionReviewServiceImpl(scopeService, eventMapper, reviewMapper,
                processInspectionAggregationService);
        MesTeamLeaderFifoAllocationService fifoAllocationService =
                new MesTeamLeaderFifoAllocationService(activeOrderMapper, workOrderMapper, allocationMapper,
                        orderProcessTargetService);
        reportConfirmationService = new MesTeamLeaderReportConfirmationServiceImpl(scopeService, eventMapper,
                activeOrderMapper, workOrderMapper, reviewMapper, allocationMapper, quantityFragmentMapper,
                pqcRecordMapper, fifoAllocationService, processPoolFifoAllocationService, pqcTaskMapper,
                pqcPieceDetailMapper, orderProcessTargetService, orderProcessCompletionService);
    }

    @Test
    void reviewSubmissionShouldRejectMissingReviewSignatureBeforePersistingReview() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> submissionReviewService.reviewSubmission(unsignedReviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void reviewSubmissionShouldRejectSignatureUserDifferentFromLeader() {
        MesTeamLeaderSubmissionReviewReqBO reqBO = signedReviewReq();
        set(reqBO, "setReviewSignatureUserId", Long.class, 3999L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> submissionReviewService.reviewSubmission(reqBO));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void reviewSubmissionShouldRejectMissingReviewSignatureSnapshotBeforeLoadingEvent() {
        MesTeamLeaderSubmissionReviewReqBO reqBO = signedReviewReq();
        set(reqBO, "setReviewSignatureSnapshotJson", String.class, "   ");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> submissionReviewService.reviewSubmission(reqBO));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void reviewSubmissionShouldRejectMalformedReviewSignatureSnapshotBeforeLoadingEvent() {
        MesTeamLeaderSubmissionReviewReqBO reqBO = signedReviewReq();
        set(reqBO, "setReviewSignatureSnapshotJson", String.class, "not-json");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> submissionReviewService.reviewSubmission(reqBO));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void reviewSubmissionShouldPersistStructuredReviewSignature() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event("{\"outputQuantity\":80}"));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
            return 1;
        });

        Long reviewId = submissionReviewService.reviewSubmission(signedReviewReq());

        assertEquals(7001L, reviewId);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(captor.capture());
        MesProcessPoolSubmissionReviewDO review = captor.getValue();
        assertEquals(REVIEW_SIGNATURE_ID, review.getReviewSignatureId());
        assertEquals(REVIEW_SIGNATURE_USER_ID, review.getReviewSignatureUserId());
        assertEquals(REVIEW_SIGNATURE_SNAPSHOT, review.getReviewSignatureSnapshotJson());
    }

    @Test
    void confirmSubmissionShouldRejectMissingReviewSignatureBeforeReviewOrAllocationWrites() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> reportConfirmationService.confirmSubmission(unsignedConfirmReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void confirmSubmissionShouldRejectMissingReviewSignatureSnapshotBeforeReviewOrAllocationWrites() {
        MesTeamLeaderReportConfirmationReqBO reqBO = signedConfirmReq();
        set(reqBO, "setReviewSignatureSnapshotJson", String.class, "   ");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reportConfirmationService.confirmSubmission(reqBO));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void confirmSubmissionShouldRejectMalformedReviewSignatureSnapshotBeforeReviewOrAllocationWrites() {
        MesTeamLeaderReportConfirmationReqBO reqBO = signedConfirmReq();
        set(reqBO, "setReviewSignatureSnapshotJson", String.class, "not-json");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reportConfirmationService.confirmSubmission(reqBO));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void confirmSubmissionShouldRejectSignatureUserDifferentFromLeader() {
        MesTeamLeaderReportConfirmationReqBO reqBO = signedConfirmReq();
        set(reqBO, "setReviewSignatureUserId", Long.class, 3999L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reportConfirmationService.confirmSubmission(reqBO));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH.getCode(), ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(EVENT_ID);
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(allocationMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void confirmSubmissionShouldPersistReviewSignatureOnApprovalReview() {
        when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event("{\"outputQuantity\":80}"));
        when(allocationMapper.selectListByEventIdForUpdate(EVENT_ID)).thenReturn(List.of());
        when(pqcRecordMapper.selectListByProductionSubmitEventId(EVENT_ID)).thenReturn(List.of(
                pqcRecord(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)));
        when(eventMapper.selectByIdForUpdate(1101L)).thenReturn(pqcEvent());
        when(pqcTaskMapper.selectByIdForUpdate(5101L)).thenReturn(pqcTask(80));
        when(pqcPieceDetailMapper.selectListByTaskId(5101L)).thenReturn(successPqcPieces(80));
        MesProcessPoolActiveOrderDO activeOrder = activeOrder();
        when(activeOrderMapper.selectActiveListByLeader(LEADER_USER_ID)).thenReturn(List.of(activeOrder));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder()));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of());
        when(orderProcessTargetService.requireTarget(activeOrder, 5001L, 6001L)).thenReturn(target("80"));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7002L);
            return 1;
        });
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        Long reviewId = reportConfirmationService.confirmSubmission(signedConfirmReq());

        assertEquals(7002L, reviewId);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        MesProcessPoolSubmissionReviewDO review = reviewCaptor.getValue();
        assertEquals(REVIEW_SIGNATURE_ID, review.getReviewSignatureId());
        assertEquals(REVIEW_SIGNATURE_USER_ID, review.getReviewSignatureUserId());
        assertEquals(REVIEW_SIGNATURE_SNAPSHOT, review.getReviewSignatureSnapshotJson());
        verify(allocationMapper).insertBatch(anyCollection());
        verify(orderProcessCompletionService).applyConfirmedAllocations(any(MesProProcessPoolEventDO.class),
                anyCollection());
    }

    private static MesTeamLeaderSubmissionReviewReqBO unsignedReviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(EVENT_ID)
                .leaderUserId(LEADER_USER_ID)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据一致")
                .build();
    }

    private static MesTeamLeaderSubmissionReviewReqBO signedReviewReq() {
        MesTeamLeaderSubmissionReviewReqBO reqBO = unsignedReviewReq();
        set(reqBO, "setReviewSignatureId", Long.class, REVIEW_SIGNATURE_ID);
        set(reqBO, "setReviewSignatureUserId", Long.class, REVIEW_SIGNATURE_USER_ID);
        set(reqBO, "setReviewSignatureSnapshotJson", String.class, REVIEW_SIGNATURE_SNAPSHOT);
        return reqBO;
    }

    private static MesTeamLeaderReportConfirmationReqBO unsignedConfirmReq() {
        return MesTeamLeaderReportConfirmationReqBO.builder()
                .eventId(EVENT_ID)
                .leaderUserId(LEADER_USER_ID)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                .reviewRemark("确认分配")
                .allocations(List.of(MesTeamLeaderReportAllocationLineReqBO.builder()
                        .activeOrderId(8101L)
                        .allocatedQuantity(new BigDecimal("80"))
                        .build()))
                .build();
    }

    private static MesTeamLeaderReportConfirmationReqBO signedConfirmReq() {
        MesTeamLeaderReportConfirmationReqBO reqBO = unsignedConfirmReq();
        set(reqBO, "setReviewSignatureId", Long.class, REVIEW_SIGNATURE_ID);
        set(reqBO, "setReviewSignatureUserId", Long.class, REVIEW_SIGNATURE_USER_ID);
        set(reqBO, "setReviewSignatureSnapshotJson", String.class, REVIEW_SIGNATURE_SNAPSHOT);
        return reqBO;
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
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 9, 0))
                .build();
    }

    private static MesProProcessPoolPqcRecordDO pqcRecord(String inspectionResult) {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(inspectionResult)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 9, 5))
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
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 9, 5))
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
                .build();
    }

    private static MesTeamLeaderOrderProcessTarget target(String plannedQuantity) {
        return new MesTeamLeaderOrderProcessTarget(5001L, 6001L, new BigDecimal("80"),
                BigDecimal.ONE, new BigDecimal(plannedQuantity));
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(Object target, String setter, Class<T> type, T value) {
        try {
            Method method = target.getClass().getMethod(setter, type);
            method.invoke(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(target.getClass().getSimpleName() + " missing " + setter, ex);
        }
    }
}
