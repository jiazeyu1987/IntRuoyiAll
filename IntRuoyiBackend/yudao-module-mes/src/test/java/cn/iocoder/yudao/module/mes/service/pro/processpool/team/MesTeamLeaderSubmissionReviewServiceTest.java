package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesTeamLeaderReviewSignatureContext;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderSubmissionReviewServiceTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesPqcProcessInspectionAggregationService processInspectionAggregationService;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private MesReportAllocationCommandService reportAllocationCommandService;
    @Mock
    private MesProProcessPoolEventRevisionMapper revisionMapper;
    @Mock
    private GxpAuditService gxpAuditService;
    @Mock
    private MesReleaseAffectedStateCollector affectedStateCollector;

    private MesTeamLeaderSubmissionReviewService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderSubmissionReviewServiceImpl(scopeService, eventMapper, reviewMapper,
                processInspectionAggregationService);
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "returnCorrectionResolver", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver.class)); }
        ReflectionTestUtils.setField(service, "signatureService", signatureService);
        ReflectionTestUtils.setField(service, "nonconformanceReviewService", org.mockito.Mockito.mock(
                cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
        ReflectionTestUtils.setField(service, "reportAllocationCommandService", reportAllocationCommandService);
        // The current review API binds the signature to its business object using its complete formal round context.
        lenient().when(signatureService.recordTeamLeaderReviewSignature(
                any(), any(), any(), any(MesTeamLeaderReviewSignatureContext.class))).thenReturn(9101L);
        ReflectionTestUtils.setField(service, "revisionMapper", revisionMapper);
        ReflectionTestUtils.setField(service, "gxpAuditService", gxpAuditService);
        // Unit boundary only; the real collector/transaction proof is in MesCompletionAggregationAuditTransactionTest.
        ReflectionTestUtils.setField(service, "affectedStateCollector", affectedStateCollector);
        ReflectionTestUtils.setField(service, "pqcTaskMapper", pqcTaskMapper);
        lenient().when(pqcTaskMapper.selectById(5101L)).thenReturn(MesPqcInspectionTaskDO.builder()
                .id(5101L).activeOrderId(8101L).build());
    }

    @Test
    void shouldReviewResponsibleEmployeeSubmissionWithoutChangingRawEvent() {
        MesProProcessPoolEventDO event = event();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
            return 1;
        });

        Long reviewId = service.reviewSubmission(displayed(reviewReq()));

        assertEquals(7001L, reviewId);
        verify(scopeService).assertCanAccessEmployee(3001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, 2001L);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        MesProcessPoolSubmissionReviewDO review = reviewCaptor.getValue();
        assertEquals(1001L, review.getEventId());
        assertEquals(3001L, review.getLeaderUserId());
        assertEquals(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, review.getLeaderType());
        assertEquals(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED, review.getReviewStatus());
        assertEquals("数据和签名一致", review.getReviewRemark());
        assertNotNull(review.getReviewedAt());
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
        assertEquals("{\"outputQuantity\":10}", event.getRawPayload());
        assertEquals(9001L, event.getSignatureId());
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7001L);
        verify(gxpAuditService).acquireLedgerLock();
        ArgumentCaptor<GxpAuditCommand> auditCaptor = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(gxpAuditService).append(auditCaptor.capture());
        assertEquals("mes.pqc.review.approve", auditCaptor.getValue().getOperationId());
        assertEquals("PQC_REVIEW:7001", auditCaptor.getValue().getIdempotencyKey());
        assertEquals("PQC_REVIEW_APPROVED", auditCaptor.getValue().getAfterState().getState());
        assertHasActiveOrderRelation(auditCaptor.getValue(), 8101L);
    }

    @Test
    void shouldNotAggregateRejectedPqcSubmission() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7002L);
            return 1;
        });

        Long reviewId = service.reviewSubmission(displayed(rejectedReviewReq()));

        assertEquals(7002L, reviewId);
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
        verify(gxpAuditService).acquireLedgerLock();
        ArgumentCaptor<GxpAuditCommand> rejectAuditCaptor = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(gxpAuditService).append(rejectAuditCaptor.capture());
        assertEquals("mes.pqc.review.reject", rejectAuditCaptor.getValue().getOperationId());
        assertEquals("PQC_REVIEW_REJECTED", rejectAuditCaptor.getValue().getAfterState().getState());
        assertHasActiveOrderRelation(rejectAuditCaptor.getValue(), 8101L);
    }

    @Test
    void shouldRejectApprovedProductionSubmissionThroughGenericReview() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(productionEvent());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED.getCode(),
                ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldDelegateProductionRejectionToAllocationRollbackService() {
        when(reportAllocationCommandService.rejectProductionSubmission(1001L, 3001L,
                "数量错误", "review-pass", null)).thenReturn(7401L);

        Long reviewId = service.reviewSubmission(rejectedProductionReviewReq());

        assertEquals(7401L, reviewId);
        verify(reportAllocationCommandService).rejectProductionSubmission(1001L, 3001L,
                "数量错误", "review-pass", null);
        verify(eventMapper, never()).selectByIdForUpdate(any());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectProductionLeaderReviewingPqcSubmission() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.reviewSubmission(displayed(productionReviewReq())));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_PQC_LEADER_REQUIRED.getCode(),
                ex.getCode());
        verify(scopeService, never()).assertCanAccessEmployee(any(), any(), any());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldRejectRejectedPqcReviewWithoutReason() {
        MesTeamLeaderSubmissionReviewReqBO req = rejectedReviewReq().setReviewRemark("  ");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED.getCode(),
                ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(any());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectReviewForOutOfScopeEmployee() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        doThrow(exception(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "员工"))
                .when(scopeService).assertCanAccessEmployee(3001L,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, 2001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED.getCode(), ex.getCode());
        assertEquals("班组长不在该员工的负责范围内", ex.getMessage());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectAdditionalPqcReviewWhenPreviousPqcReviewExists() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingReview());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(anyLong(), anyLong());
    }

    @Test
    void shouldReplaySameTerminalPqcReviewWithoutNewSignatureOrAggregate() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingApprovedReview());

        Long reviewId = service.reviewSubmission(displayed(reviewReq()));

        assertEquals(7004L, reviewId);
        verify(signatureService, never()).recordTeamLeaderReviewSignature(
                any(), any(), any(), any(MesTeamLeaderReviewSignatureContext.class));
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldReviewSignedCorrectionAfterRejectedPqcWithoutDeletingOldReview() {
        MesProProcessPoolEventDO corrected = event().setRawPayload(
                "{\"outputQuantity\":11,\"supersededReviewId\":7000}");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(corrected);
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingReview());
        bindRevision(corrected, 8001L);
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7010L);
            return 1;
        });

        assertEquals(7010L, service.reviewSubmission(displayed(reviewReq())));

        verify(reviewMapper).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(reviewMapper, never()).updateById(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7010L);
    }

    @Test
    void repeatedRejectionCorrectionAndApprovalPreserveEachReviewAndReplayLatestOnly() {
        MesProProcessPoolEventDO corrected = event().setRawPayload(
                "{\"outputQuantity\":11,\"supersededReviewId\":7000}");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(corrected);
        MesProcessPoolSubmissionReviewDO first = existingReview().setLeaderUserId(3001L)
                .setLeaderType("PQC").setReviewRemark(rejectedReviewReq().getReviewRemark());
        AtomicReference<MesProcessPoolSubmissionReviewDO> latest = new AtomicReference<>(first);
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenAnswer(invocation -> latest.get());
        List<MesProcessPoolSubmissionReviewDO> reviews = new ArrayList<>(List.of(first));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            MesProcessPoolSubmissionReviewDO next = invocation.getArgument(0);
            next.setId(7000L + reviews.size());
            reviews.add(next);
            latest.set(next);
            return 1;
        });
        bindRevision(corrected, 8001L);

        var capturedRejection=displayed(rejectedReviewReq());
        assertEquals(7001L, service.reviewSubmission(capturedRejection));
        assertEquals(7001L, service.reviewSubmission(capturedRejection));
        corrected.setRawPayload("{\"outputQuantity\":12,\"supersededReviewId\":7001}");
        bindRevision(corrected, 8002L);
        var capturedApproval=displayed(reviewReq());
        assertEquals(7002L, service.reviewSubmission(capturedApproval));
        assertEquals(7002L, service.reviewSubmission(capturedApproval));
        assertEquals(3, reviews.size());
        assertEquals("REJECTED", first.getReviewStatus());
        verify(signatureService, org.mockito.Mockito.times(2))
                .recordTeamLeaderReviewSignature(any(), any(), any(), any(MesTeamLeaderReviewSignatureContext.class));
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7002L);
        ArgumentCaptor<GxpAuditCommand> audits = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(gxpAuditService, org.mockito.Mockito.times(2)).append(audits.capture());
        assertEquals(List.of("PQC_REVIEW:7001", "PQC_REVIEW:7002"),
                audits.getAllValues().stream().map(GxpAuditCommand::getIdempotencyKey).toList());
        assertEquals(List.of("mes.pqc.review.reject", "mes.pqc.review.approve"),
                audits.getAllValues().stream().map(GxpAuditCommand::getOperationId).toList());
        for (int index = 0; index < audits.getAllValues().size(); index++) {
            GxpAuditCommand audit = audits.getAllValues().get(index);
            assertHasActiveOrderRelation(audit, 8101L);
            var after = cn.iocoder.yudao.framework.common.util.json.JsonUtils
                    .parseTree(audit.getAfterState().getCanonicalJson());
            var signature = cn.iocoder.yudao.framework.common.util.json.JsonUtils
                    .parseTree(after.path("review").path("reviewSignatureSnapshotJson").asText());
            assertEquals(8001L + index, signature.path("revisionId").asLong());
            assertEquals(7000L + index, signature.path("supersededReviewId").asLong());
            assertEquals(17501L + index, signature.path("revisionSignatureId").asLong());
            assertTrue(signature.path("payloadHash").isTextual());
        }
        verify(reviewMapper, never()).updateById(any(MesProcessPoolSubmissionReviewDO.class));
        verify(reviewMapper, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void payloadClaimWithoutSignedMatchingRevisionCannotReopenRejectedReview() {
        MesProProcessPoolEventDO corrected = event().setRawPayload(
                "{\"outputQuantity\":11,\"supersededReviewId\":7000}");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(corrected);
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingReview());
        assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void signedRevisionForDifferentPayloadCannotReopenRejectedReview() {
        MesProProcessPoolEventDO corrected = event().setRawPayload(
                "{\"outputQuantity\":11,\"supersededReviewId\":7000}");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(corrected);
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingReview());
        bindRevision(corrected, 8001L);
        corrected.setRawPayload("{\"outputQuantity\":12,\"supersededReviewId\":7000}");
        assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(signatureService, never()).recordTeamLeaderReviewSignature(any(), any(), any(), any(MesTeamLeaderReviewSignatureContext.class));
    }

    private void bindRevision(MesProProcessPoolEventDO event, Long id) {
        MesProProcessPoolEventRevisionDO revision = MesProProcessPoolEventRevisionDO.builder()
                .id(id).eventId(event.getId()).revisionStatus("EFFECTIVE")
                .revisionSignatureId(9500L + id).revisionSignatureUserId(2001L).modifiedByUserId(2001L)
                .revisionSignatureSnapshot("{\"signatureId\":" + (9500L + id)
                        + ",\"actorId\":2001,\"signedAt\":\"2026-09-28T10:30:00\"}")
                .afterPayload(event.getRawPayload()).build();
        revision.setTenantId(event.getTenantId());
        org.mockito.Mockito.when(revisionMapper.selectListByEventIdForUpdate(event.getId())).thenReturn(List.of(revision));
        var discovery=(cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver) ReflectionTestUtils.getField(service, "returnCorrectionResolver");
        org.mockito.Mockito.lenient().when(discovery.find(org.mockito.ArgumentMatchers.eq(event), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    MesProcessPoolSubmissionReviewDO previous=invocation.getArgument(1);
                    return previous!=null&&"REJECTED".equals(previous.getReviewStatus())
                        &&java.util.Objects.equals(previous.getId(),cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(revision.getAfterPayload()).path("supersededReviewId").longValue())
                        &&java.util.Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload())) ? revision : null;
                });
    }

    @Test
    void shouldPropagateAggregationFailureSoTransactionCanRollbackReviewAndSignature() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7005L);
            return 1;
        });
        doThrow(exception(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                "pqcProcessInspectionAggregateDetail"))
                .when(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7005L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.reviewSubmission(displayed(reviewReq())));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(reviewMapper).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7005L);
    }

    @Test
    void shouldAllowPqcReviewWhenLeaderIsActualInspector() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(eventWithActualEmployee(3001L));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7003L);
            return 1;
        });

        Long reviewId = service.reviewSubmission(displayed(reviewReq()));

        assertEquals(7003L, reviewId);
        verify(scopeService).assertCanAccessEmployee(3001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, 3001L);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(3001L, reviewCaptor.getValue().getLeaderUserId());
        assertEquals(9101L, reviewCaptor.getValue().getReviewSignatureId());
        verify(signatureService).recordTeamLeaderReviewSignature(3001L, "review-pass",
                "组长复核:PQC:1001:APPROVED", new MesTeamLeaderReviewSignatureContext(1001L, "APPROVED",
                        MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(eventWithActualEmployee(3001L).getRawPayload()),
                        null, null, null));
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7003L);
    }

    private MesTeamLeaderSubmissionReviewReqBO reviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据和签名一致")
                .signaturePassword("review-pass")
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"review\"}")
                .build();
    }

    @Test
    void shouldReviewEveryMemberOfPqcSubmissionGroup() {
        prepareSubmissionGroup(false);
        assertEquals(7001L, service.reviewSubmission(displayed(reviewReq())));
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7001L);
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1002L, 7002L);
        verify(gxpAuditService, org.mockito.Mockito.times(2)).append(any(GxpAuditCommand.class));
    }

    @Test
    void shouldFinishUnreviewedGroupMembersWithoutRewritingApprovedMember() {
        prepareSubmissionGroup(false);
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingApprovedReview());
        assertEquals(7004L, service.reviewSubmission(displayed(reviewReq())));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(org.mockito.ArgumentMatchers.eq(1001L), any());
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1002L, 7002L);
        verify(reviewMapper, org.mockito.Mockito.times(1)).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectCrossOrderSubmissionGroupBeforeWritingAnyReview() {
        prepareSubmissionGroup(true);
        assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(signatureService, never()).recordTeamLeaderReviewSignature(any(), any(), any(), any(MesTeamLeaderReviewSignatureContext.class));
    }

    @Test
    void shouldPreserveAnotherLeadersApprovedMemberWhileReviewingPendingMember() {
        prepareSubmissionGroup(false);
        lenient().when(reviewMapper.selectLatestByEventIdForUpdate(1002L)).thenReturn(existingApprovedReview()
                .setEventId(1002L).setLeaderUserId(3002L).setReviewRemark("Original approval"));
        assertEquals(7001L, service.reviewSubmission(displayed(reviewReq())));
        verify(reviewMapper, org.mockito.Mockito.times(1)).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(org.mockito.ArgumentMatchers.eq(1002L), any());
    }

    @Test
    void shouldRejectEntireSubmissionGroupWithoutAggregating() {
        prepareSubmissionGroup(false);
        assertEquals(7001L, service.reviewSubmission(displayed(rejectedReviewReq())));
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviews = ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper, org.mockito.Mockito.times(2)).insert(reviews.capture());
        assertTrue(reviews.getAllValues().stream().allMatch(review -> "REJECTED".equals(review.getReviewStatus())));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldRejectCrossActiveOrderGroupBeforeAnySignature() {
        prepareSubmissionGroup(false);
        when(pqcTaskMapper.selectById(5102L)).thenReturn(MesPqcInspectionTaskDO.builder().id(5102L).activeOrderId(8102L).build());
        assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));
        verify(signatureService, never()).recordTeamLeaderReviewSignature(any(), any(), any(), any(MesTeamLeaderReviewSignatureContext.class));
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectPendingMemberWithoutRewritingAnotherLeadersApprovedMember() {
        prepareSubmissionGroup(false);
        lenient().when(reviewMapper.selectLatestByEventIdForUpdate(1002L)).thenReturn(existingApprovedReview()
                .setEventId(1002L).setLeaderUserId(3002L).setReviewRemark("Original approval"));
        assertEquals(7001L, service.reviewSubmission(displayed(rejectedReviewReq())));
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviews = ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviews.capture());
        assertEquals(1001L, reviews.getValue().getEventId());
        assertEquals("REJECTED", reviews.getValue().getReviewStatus());
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
        verify(gxpAuditService).append(any(GxpAuditCommand.class));
    }

    @Test
    void shouldPrevalidateUncorrectedRejectedSiblingBeforeSigningFirstMember() {
        prepareSubmissionGroup(false);
        lenient().when(reviewMapper.selectLatestByEventIdForUpdate(1002L)).thenReturn(existingApprovedReview()
                .setEventId(1002L).setReviewStatus("REJECTED"));
        assertThrows(ServiceException.class, () -> service.reviewSubmission(displayed(reviewReq())));
        verify(signatureService, never()).recordTeamLeaderReviewSignature(any(), any(), any(), any(MesTeamLeaderReviewSignatureContext.class));
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"APPROVED,pending_review","APPROVED,void","APPROVED,external",
            "REJECTED,pending_review","REJECTED,void","REJECTED,external","APPROVED,open","REJECTED,open"})
    void pqcGroupUsesRealFreezeAuthorityBeforeFirstSignatureOrReview(String decision, String state) {
        prepareSubmissionGroup(false);
        when(pqcTaskMapper.selectById(5101L)).thenReturn(MesPqcInspectionTaskDO.builder().id(5101L).activeOrderId(8101L).workOrderId(4001L).build());
        when(pqcTaskMapper.selectById(5102L)).thenReturn(MesPqcInspectionTaskDO.builder().id(5102L).activeOrderId(8101L).workOrderId(4001L).build());
        var authority = cn.iocoder.yudao.module.mes.service.pro.MesSa09Sa14FreezeFixture.authority(4001L,state);
        if (state.equals("pending_review") || state.equals("void")) {
            var reviews = (cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper)
                    ReflectionTestUtils.getField(authority,"reviewMapper");
            org.mockito.Mockito.reset(reviews);
            org.mockito.Mockito.doReturn(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO()
                    .setId(909L).setWorkOrderId(4001L).setActiveOrderId(8101L)
                    .setReviewStatus(state.equals("pending_review") ? state : "closed")
                    .setDisposition(state.equals("void") ? "void" : null).setReviewCode("NCR-909"))
                    .when(reviews).selectFirstBlockingPqcSubmissionByActiveOrderId(8101L);
        }
        ReflectionTestUtils.setField(service,"nonconformanceReviewService",authority);
        var request = displayed(decision.equals("APPROVED") ? reviewReq() : rejectedReviewReq());
        if (state.equals("open")) {
            assertEquals(7001L,service.reviewSubmission(request));
            verify(reviewMapper,org.mockito.Mockito.times(2)).insert(any(MesProcessPoolSubmissionReviewDO.class));
        } else {
            var freezeFailure = assertThrows(ServiceException.class,()->service.reviewSubmission(request));
            assertEquals(state.equals("external")
                    ? cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_TEMPORARY_FROZEN_OPERATION_FORBIDDEN.getCode()
                    : cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), freezeFailure.getCode());
            verify(signatureService,never()).recordTeamLeaderReviewSignature(any(),any(),any(),any(MesTeamLeaderReviewSignatureContext.class));
            verify(reviewMapper,never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
            org.mockito.Mockito.verifyNoInteractions(processInspectionAggregationService,ReflectionTestUtils.getField(service,"handoffService"));
        }
    }

    private void prepareSubmissionGroup(boolean crossOrder) {
        MesProProcessPoolEventDO first = groupedEvent(1001L, 5101L);
        MesProProcessPoolEventDO second = groupedEvent(1002L, 5102L);
        if (crossOrder) second.setWorkOrderId(4002L);
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(first);
        lenient().when(eventMapper.selectList(any(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class)))
                .thenReturn(List.of(first, second));
        lenient().when(pqcTaskMapper.selectById(5102L)).thenReturn(MesPqcInspectionTaskDO.builder().id(5102L).activeOrderId(8101L).build());
        lenient().when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            MesProcessPoolSubmissionReviewDO review = invocation.getArgument(0);
            review.setId(review.getEventId() + 6000L);
            return 1;
        });
    }

    private static MesProProcessPoolEventDO groupedEvent(long id, long taskId) {
        MesProProcessPoolEventDO event = event().setId(id).setFeedbackSourceId(taskId)
                .setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setWorkOrderId(4001L)
                .setRouteId(5001L).setQaProcessId(6001L)
                .setRawPayload("{\"pqcSubmissionGroupId\":\"group-01\",\"activeOrderId\":8101}");
        event.setTenantId(1L);
        return event;
    }

    private MesTeamLeaderSubmissionReviewReqBO productionReviewReq() {
        return reviewReq()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);
    }

    private MesTeamLeaderSubmissionReviewReqBO rejectedProductionReviewReq() {
        return reviewReq()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .setReviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .setReviewRemark("数量错误");
    }

    private MesTeamLeaderSubmissionReviewReqBO rejectedReviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .reviewRemark("压力曲线异常，退回补正")
                .signaturePassword("review-pass")
                .reviewSignatureId(9102L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"reject\"}")
                .build();
    }

    private static MesProProcessPoolEventDO event() {
        return eventWithActualEmployee(2001L);
    }

    private static MesProProcessPoolEventDO productionEvent() {
        return eventWithActualEmployee(2001L)
                .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT);
    }

    private static MesProProcessPoolEventDO eventWithActualEmployee(Long actualEmployeeId) {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .feedbackSourceId(5101L)
                .actualEmployeeId(actualEmployeeId)
                .rawPayload("{\"outputQuantity\":10}")
                .serverSubmitTime(LocalDateTime.of(2026, 7, 30, 9, 10))
                .signatureId(9001L)
                .signatureUserId(actualEmployeeId)
                .build();
    }

    private static void assertHasActiveOrderRelation(GxpAuditCommand command, Long activeOrderId) {
        assertTrue(command.getLinks().stream().anyMatch(link ->
                "ACTIVE_ORDER".equals(link.objectType())
                        && activeOrderId.toString().equals(link.objectId())));
    }

    private static MesProcessPoolSubmissionReviewDO existingReview() {
        return MesProcessPoolSubmissionReviewDO.builder().reviewRound(0)
                .id(7000L)
                .eventId(1001L)
                .leaderUserId(3002L)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .reviewRemark("压力曲线异常，已退回")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 10, 30))
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO existingApprovedReview() {
        return MesProcessPoolSubmissionReviewDO.builder().reviewRound(0)
                .id(7004L)
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据和签名一致")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 10, 30))
                .build();
    }

    private MesTeamLeaderSubmissionReviewReqBO displayed(MesTeamLeaderSubmissionReviewReqBO request) {
        var event=eventMapper.selectByIdForUpdate(request.getEventId());
        if(event==null||!"PQC_INSPECTION".equals(event.getEventType()))return request;
        java.util.List<MesProProcessPoolEventDO> members=List.of(event);
        if(JsonUtils.parseTree(event.getRawPayload()).has("pqcSubmissionGroupId"))
            members=eventMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProProcessPoolEventDO>());
        return request.setExpectedReviews(members.stream().map(member->{
            var review=reviewMapper.selectLatestByEventIdForUpdate(member.getId());
            var versions=revisionMapper.selectListByEventIdForUpdate(member.getId());
            return new MesSubmissionReviewExpectedContext().setEventId(member.getId())
                    .setPayloadHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(member.getRawPayload()))
                    .setRevisionId(versions.isEmpty()?0L:versions.get(0).getId())
                    .setReviewId(review==null?0L:review.getId()).setReviewRound(review==null?0:review.getReviewRound());
        }).toList());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"payload","round","revision","missing","duplicate","secondMember"})
    void staleDisplayedPqcContextFailsBeforeAnySignatureReviewAggregateOrHandoff(String change) {
        if("secondMember".equals(change))prepareSubmissionGroup(false);
        else when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        var request=displayed(reviewReq());
        switch(change){
            case "payload"->when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event().setRawPayload("{\"outputQuantity\":11}"));
            case "round"->when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingReview());
            case "revision"->when(revisionMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(new MesProProcessPoolEventRevisionDO().setId(55L)));
            case "missing"->request.setExpectedReviews(null);
            case "duplicate"->request.setExpectedReviews(List.of(request.getExpectedReviews().get(0),request.getExpectedReviews().get(0)));
            case "secondMember"->request.getExpectedReviews().get(1).setPayloadHash("stale-other-member");
            default->throw new IllegalArgumentException(change);
        }
        var failure=assertThrows(IllegalStateException.class,()->service.reviewSubmission(request));
        assertTrue(failure.getMessage().contains("刷新"));
        org.mockito.Mockito.verifyNoInteractions(signatureService,processInspectionAggregationService);
        verify(reviewMapper,never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(eventMapper,never()).updateById(any(MesProProcessPoolEventDO.class));
    }

}
