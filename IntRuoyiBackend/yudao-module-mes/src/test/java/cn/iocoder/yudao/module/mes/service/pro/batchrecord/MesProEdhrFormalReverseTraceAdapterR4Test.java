package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewServiceImpl;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reader-only R4 fixture; the review and its signature snapshot are produced by the real review service. */
class MesProEdhrFormalReverseTraceAdapterR4Test {
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesProEdhrBatchExecutionTraceLinkMapper links = mock(MesProEdhrBatchExecutionTraceLinkMapper.class);
    private final MesProProcessPoolEventMapper events = mock(MesProProcessPoolEventMapper.class);
    private final MesProProcessPoolPqcRecordMapper records = mock(MesProProcessPoolPqcRecordMapper.class);
    private final MesProcessPoolSubmissionReviewMapper reviews = mock(MesProcessPoolSubmissionReviewMapper.class);
    private final MesProcessPoolActiveOrderCompletionReceiptMapper receipts = mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrReleaseTransactionMapper releases = mock(MesProEdhrReleaseTransactionMapper.class);
    private final List<Map<String, Object>> details = new ArrayList<>();
    private MesProcessPoolSubmissionReviewDO review;
    private MesProProcessPoolEventDO event;
    private MesProProcessPoolPqcRecordDO record;
    private final MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO().setId(9001L)
            .setTenantId(1L).setWorkOrderId(1001L).setRouteId(2001L).setRouteVersionId(2002L);

    @BeforeEach
    void setUp() {
        var submittedAt = LocalDateTime.of(2026, 9, 24, 9, 0);
        event = new MesProProcessPoolEventDO().setId(5101L).setEventType("PQC_INSPECTION")
                .setWorkOrderId(1001L).setRouteId(2001L).setRouteProcessId(3001L).setProcessId(4001L)
                .setQaProcessId(7101L)
                .setActualEmployeeId(21L).setSignatureUserId(21L).setSignatureId(8100L)
                .setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(8001L)
                .setRecordbookSourceType("MES_PQC_INSPECTION_TASK").setRecordbookSourceId(8001L)
                .setServerSubmitTime(submittedAt);
        event.setTenantId(1L);
        event.setRawPayload(JsonUtils.toJsonString(Map.ofEntries(
                Map.entry("submittedEventId", 5101L), Map.entry("pqcTaskId", 8001L),
                Map.entry("activeOrderId", 6001L), Map.entry("workOrderId", 1001L),
                Map.entry("routeId", 2001L), Map.entry("routeVersionId", 2002L),
                Map.entry("routeProcessId", 3001L), Map.entry("processId", 4001L),
                Map.entry("qaProcessId", 7101L), Map.entry("actualEmployeeId", 21L),
                Map.entry("inspectionResult", "SUCCESS"), Map.entry("actualInspectionQuantity", 1),
                Map.entry("scrapQuantity", 0), Map.entry("itemResults", List.of(Map.of(
                        "itemCode", "QA-R4", "sampleValues", List.of("合格")))))));
        record = new MesProProcessPoolPqcRecordDO().setId(5201L).setEventId(5101L)
                .setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(7101L)
                .setActualEmployeeId(21L).setSignatureUserId(21L).setSignatureId(8100L)
                .setProcessInspectionReviewId(6101L).setServerSubmitTime(submittedAt);
        record.setTenantId(1L);
        record.setRawPayload(event.getRawPayload());
        when(events.selectById(5101L)).thenReturn(event);
        when(events.selectByIdForUpdate(5101L)).thenReturn(event);
        when(records.selectByEventId(5101L)).thenReturn(record);
        var signatures = mock(MesProBatchRecordExecutionSignatureService.class);
        var formalReviewContext = new MesTeamLeaderReviewSignatureContext(5101L, "APPROVED",
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()), null, null, null);
        when(signatures.recordTeamLeaderReviewSignature(31L, "test-password", "组长复核:PQC:5101:APPROVED",
                formalReviewContext)).thenReturn(9101L);
        when(reviews.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            review = invocation.getArgument(0);
            review.setId(6101L);
            review.setTenantId(1L);
            return 1;
        });
        var aggregation = mock(MesPqcProcessInspectionAggregationService.class);
        var producer = new MesTeamLeaderSubmissionReviewServiceImpl(mock(MesTeamLeaderScopeService.class),
                events, reviews, aggregation);
        { org.springframework.test.util.ReflectionTestUtils.setField(producer, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        { org.springframework.test.util.ReflectionTestUtils.setField(producer, "returnCorrectionResolver", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver.class)); }
        ReflectionTestUtils.setField(producer, "signatureService", signatures);
        ReflectionTestUtils.setField(producer, "revisionMapper",
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper.class));
        // Real review producer; only its audit and task persistence boundaries are test doubles.
        var audit = mock(GxpAuditService.class);
        var tasks = mock(MesPqcInspectionTaskMapper.class);
        var task = new MesPqcInspectionTaskDO().setId(8001L).setActiveOrderId(6001L)
                .setWorkOrderId(1001L).setRouteId(2001L).setRouteVersionId(2002L)
                .setRouteProcessId(3001L).setProcessId(4001L).setQaProcessId(7101L)
                .setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).setSubmittedEventId(5101L);
        task.setTenantId(1L);
        when(tasks.selectById(8001L)).thenReturn(task);
        ReflectionTestUtils.setField(producer, "gxpAuditService", audit);
        ReflectionTestUtils.setField(producer, "affectedStateCollector",
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector.class));
        ReflectionTestUtils.setField(producer, "pqcTaskMapper", tasks);
        assertEquals(6101L, producer.reviewSubmission(MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(5101L).leaderUserId(31L).leaderType("PQC").reviewStatus("APPROVED")
                .expectedReviews(List.of(new cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesSubmissionReviewExpectedContext()
                        .setEventId(5101L).setPayloadHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(event.getRawPayload()))
                        .setRevisionId(0L).setReviewId(0L).setReviewRound(0)))
                .signaturePassword("test-password").build()));
        verify(signatures).recordTeamLeaderReviewSignature(31L, "test-password", "组长复核:PQC:5101:APPROVED",
                formalReviewContext);
        verify(signatures, never()).recordTeamLeaderReviewSignature(any(), any(), any(), any(), any(), any());
        assertEquals(1L, event.getTenantId());
        assertEquals(event.getTenantId(), review.getTenantId());
        verify(aggregation).aggregateApprovedPqcSubmission(5101L, 6101L);
        var signatureSnapshot = JsonUtils.parseTree(review.getReviewSignatureSnapshotJson());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                signatureSnapshot.path("payloadHash").asText());
        assertEquals(9101L, signatureSnapshot.path("signatureId").asLong());
        assertEquals(31L, signatureSnapshot.path("actorId").asLong());
        assertEquals(5101L, signatureSnapshot.path("processPoolEventId").asLong());
        assertEquals(MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW,
                signatureSnapshot.path("actionType").asText());
        assertEquals("PQC_INSPECTION", signatureSnapshot.path("eventType").asText());
        assertEquals("PQC", signatureSnapshot.path("leaderType").asText());
        assertEquals("APPROVED", signatureSnapshot.path("reviewStatus").asText());
        verify(audit).acquireLedgerLock();
        verify(tasks).selectById(8001L);
        verify(audit).append(argThat(command -> "mes.pqc.review.approve".equals(command.getOperationId())
                && "9101".equals(command.getSignatureRecordId())
                && command.getLinks().stream().anyMatch(link -> "ACTIVE_ORDER".equals(link.objectType())
                && "6001".equals(link.objectId()))));
        when(reviews.selectById(6101L)).thenAnswer(invocation -> review);
        clearInvocations(reviews, events);
        details.add(new LinkedHashMap<>(Map.ofEntries(Map.entry("id", 7001L), Map.entry("tenantId", 1L),
                Map.entry("pqcTaskId", 8001L), Map.entry("eventId", 5101L), Map.entry("reviewId", 6101L),
                Map.entry("sourcePqcRecordId", 5201L), Map.entry("activeOrderId", 6001L),
                Map.entry("workOrderId", 1001L), Map.entry("routeId", 2001L), Map.entry("routeVersionId", 2002L),
                Map.entry("routeProcessId", 3001L), Map.entry("processId", 4001L))));
        freeze();
    }

    @Test
    void realReviewProducerYieldsTwoActionsForOneReviewerDistinctFromSubmitter() {
        var adapter = adapter();
        var evidence = adapter.readEvidence(batch, List.of(condition("PQC_REVIEW", 31L), condition("PQC_REVIEW_SIGNATURE", 31L)));
        assertEquals(2, evidence.items().size());
        assertEquals(List.of("PQC_REVIEW", "PQC_REVIEW_SIGNATURE"),
                evidence.items().stream().map(item -> item.getSourceAction()).sorted().toList());
        assertTrue(evidence.items().stream().allMatch(item -> "review:6101".equals(item.getSourceRef())
                && "FLOW-05".equals(item.getSourceStage()) && review.getReviewedAt().equals(item.getRecordedAt())));
        assertTrue(adapter.evaluate(batch, List.of(condition("PQC_REVIEW", 31L))).matches().get(0).matched());
        assertTrue(adapter.readCatalog(batch).items().stream().anyMatch(item ->
                "PERSON:SYSTEM_USER:PQC_REVIEW_SIGNATURE:31".equals(item.getEvidenceKey())));
        assertEquals(31L, review.getReviewSignatureUserId());
        assertEquals(21L, record.getSignatureUserId());
    }

    @Test
    void extraReviewsAndLaterCorrectionCannotReplaceTheFrozenReviewId() {
        var extra = new MesProcessPoolSubmissionReviewDO().setReviewRound(0).setId(6102L).setEventId(5101L).setLeaderUserId(99L);
        when(reviews.selectListByEventId(5101L)).thenReturn(List.of(review, extra));
        when(reviews.selectLatestByEventIdForUpdate(5101L)).thenReturn(extra);
        // Correction changes payload/aggregate rows but reuses processInspectionReviewId; original receipt is unchanged.
        event.setRawPayload("{\"corrected\":true}");
        record.setRawPayload(event.getRawPayload());
        assertEquals(1, adapter().readEvidence(batch, List.of(condition("PQC_REVIEW", 31L))).items().size());
        assertFalse(adapter().evaluate(batch, List.of(condition("PQC_REVIEW", 99L))).matches().get(0).matched());
        verify(reviews, never()).selectListByEventId(anyLong());
        verify(reviews, never()).selectLatestByEventIdForUpdate(anyLong());
        verify(reviews, never()).selectById(6102L);
    }

    @Test
    void multipleSamplesWithSameReviewProduceOnlyOnePairOfReviewFacts() {
        details.add(new LinkedHashMap<>(details.get(0)));
        details.get(1).put("id", 7002L);
        freeze();
        assertEquals(2, adapter().readEvidence(batch,
                List.of(condition("PQC_REVIEW", 31L), condition("PQC_REVIEW_SIGNATURE", 31L))).items().size());
        verify(reviews, times(1)).selectById(6101L);
    }

    @Test
    void secondDetailCannotHideAConflictingReviewBehindEventDeduplication() {
        details.add(new LinkedHashMap<>(details.get(0)));
        details.get(1).put("id", 7002L);
        details.get(1).put("reviewId", 6102L);
        freeze();
        assertBlocked("SOURCE_CONFLICT:");
    }

    @Test
    void missingFrozenReviewIdCannotBeFilledFromCurrentPqcRecord() {
        details.get(0).remove("reviewId");
        freeze();
        assertBlocked("SOURCE_MISSING:");
        verify(reviews, never()).selectById(anyLong());
    }

    @Test
    void missingExactReviewDoesNotUseLatestReview() {
        review = null;
        assertBlocked("SOURCE_MISSING:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"tenant", "event", "id", "role", "status", "signer"})
    void conflictingReviewIdentityIsRejected(String field) {
        switch (field) {
            case "tenant" -> review.setTenantId(2L);
            case "event" -> review.setEventId(5102L);
            case "id" -> review.setId(6102L);
            case "role" -> review.setLeaderType("PRODUCTION");
            case "status" -> review.setReviewStatus("REJECTED");
            case "signer" -> review.setReviewSignatureUserId(32L);
        }
        assertBlocked("SOURCE_CONFLICT:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"actor", "time", "signatureId", "signer", "snapshot"})
    void missingReviewEvidenceIsRejected(String field) {
        switch (field) {
            case "actor" -> review.setLeaderUserId(null);
            case "time" -> review.setReviewedAt(null);
            case "signatureId" -> review.setReviewSignatureId(null);
            case "signer" -> review.setReviewSignatureUserId(null);
            case "snapshot" -> review.setReviewSignatureSnapshotJson(null);
        }
        assertBlocked("SOURCE_MISSING:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"actorId", "signatureId", "processPoolEventId", "actionType", "eventType", "leaderType", "reviewStatus"})
    void signatureBindingConflictsAreRejected(String field) {
        JSONObject signature = JSON.parseObject(review.getReviewSignatureSnapshotJson());
        signature.put(field, "999");
        review.setReviewSignatureSnapshotJson(signature.toJSONString());
        assertBlocked("SOURCE_CONFLICT:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{}"})
    void nonObjectOrIncompleteSignatureIsExplicitlyRejected(String signature) {
        review.setReviewSignatureSnapshotJson(signature);
        assertBlocked("SOURCE_CONFLICT:");
    }

    @Test
    void exactReviewReadFailurePropagatesUnchanged() {
        var failure = new IllegalStateException("review storage unavailable");
        when(reviews.selectById(6101L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> adapter().readCatalog(batch)));
    }

    private void assertBlocked(String prefix) {
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter().readCatalog(batch)).getMessage().startsWith(prefix));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter().evaluate(batch,
                List.of(condition("PQC_REVIEW", 31L)))).getMessage().startsWith(prefix));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter().readEvidence(batch,
                List.of(condition("PQC_REVIEW", 31L)))).getMessage().startsWith(prefix));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void largeSignatureIdsRemainExactAsJsonLongOrCanonicalString(boolean textual) {
        long signatureId = 9007199254740993L;
        review.setReviewSignatureId(signatureId);
        JSONObject signature = JSON.parseObject(review.getReviewSignatureSnapshotJson());
        signature.put("signatureId", textual ? Long.toString(signatureId) : signatureId);
        review.setReviewSignatureSnapshotJson(signature.toJSONString());
        assertEquals(1, adapter().readEvidence(batch, List.of(condition("PQC_REVIEW_SIGNATURE", 31L))).items().size());
        signature.put("signatureId", textual ? "9007199254740992" : 9007199254740992L);
        review.setReviewSignatureSnapshotJson(signature.toJSONString());
        assertBlocked("SOURCE_CONFLICT:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"31.0", "0031", "+31", "3.1e1"})
    void nonCanonicalSignatureActorDoesNotBecomeAnExactIdentity(String actor) {
        JSONObject signature = JSON.parseObject(review.getReviewSignatureSnapshotJson());
        signature.put("actorId", actor);
        review.setReviewSignatureSnapshotJson(signature.toJSONString());
        assertBlocked("SOURCE_CONFLICT:");
    }

    private Condition condition(String action, Long actor) {
        return new Condition().setConditionId(action).setEvidenceKey("PERSON:SYSTEM_USER:" + action + ":" + actor)
                .setSourceView("RECORDED").setOperator("EQ").setValue(actor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6101.5", "6101.0", "\"06101\"", "\"+6101\"", "\"6101.0\"", "\"6.101e3\"", "9223372036854775808", "\"9223372036854775808\"", "true", "{}", "[]", "\"\""})
    void invalidFrozenReviewIdNeverCoercesToAnotherRecord(String jsonValue) {
        details.get(0).put("reviewId", JSON.parse(jsonValue));
        freeze();
        assertBlocked("SOURCE_CONFLICT:");
        verify(reviews, never()).selectById(anyLong());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void frozenReviewIdSupportsExactLargeIntegerAndRuntimeString(boolean textual) {
        long id = 9007199254740993L;
        review.setId(id);
        details.get(0).put("reviewId", textual ? Long.toString(id) : id);
        when(reviews.selectById(id)).thenReturn(review);
        freeze();
        var evidence = adapter().readEvidence(batch, List.of(condition("PQC_REVIEW", 31L)));
        assertEquals(1, evidence.items().size());
        assertEquals("review:" + id, evidence.items().get(0).getSourceRef());
        verify(reviews).selectById(id);
    }

    private MesProEdhrFormalReverseTraceAdapter adapter() {
        return new MesProEdhrFormalReverseTraceAdapter(Category.PERSON, null, origins, links, null, null, null,
                events, records, reviews, applications, releases, receipts);
    }

    private void freeze() {
        // Isolate PQC personnel. Production categories are intentionally empty, not synthesized from current records.
        String json = JsonUtils.toJsonString(Map.of("activeOrderBinding", Map.of("id", 6001L, "tenantId", 1L,
                        "workOrderId", 1001L, "routeId", 2001L, "routeVersionId", 2002L),
                "workOrderBinding", Map.of("id", 1001L), "allocations", List.of(),
                "productionFacts", Map.of("formatVersion", 1, "events", List.of(), "reviews", List.of()),
                "pqcTasks", List.of(Map.of("id", 8001L, "tenantId", 1L, "activeOrderId", 6001L,
                        "workOrderId", 1001L, "routeId", 2001L, "routeVersionId", 2002L,
                        "routeProcessId", 3001L, "processId", 4001L, "qaProcessId", 7101L, "submittedEventId", 5101L)),
                "pqcDetails", details));
        var receipt = new MesProcessPoolActiveOrderCompletionReceiptDO().setId(8101L).setActiveOrderId(6001L)
                .setWorkOrderId(1001L).setRouteId(2001L).setRouteVersionId(2002L).setCompletedVersion(1)
                .setFormalSourceSnapshotJson(json).setLossConditionFactsJson("[]")
                .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .setCompletionStatus("SUCCESS").setBatchRecordStatus("SUCCESS").setProcessInspectionStatus("SUCCESS")
                .setBatchRecordId(8201L).setProcessInspectionId(8301L);
        receipt.setTenantId(1L);
        receipt.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(
                json, receipt.getLossConditionFactsJson()));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receipts.selectByIdAndTenantId(8101L, 1L)).thenReturn(receipt);
        when(origins.selectListByBatchExecutionId(9001L)).thenReturn(List.of(new MesProEdhrBatchExecutionOriginDO()
                .setBatchExecutionId(9001L).setTenantId(1L).setActiveOrderId(6001L).setWorkOrderId(1001L)
                .setCompletionVersion(1).setCompletionBackfillReceiptId(8101L)
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash()).setSourceSnapshotHash(receipt.getSourceSnapshotHash())));
        when(links.selectListByBatchExecutionId(9001L)).thenReturn(List.of());
        when(applications.selectListByBatchExecutionIds(List.of(9001L))).thenReturn(List.of());
    }
}
