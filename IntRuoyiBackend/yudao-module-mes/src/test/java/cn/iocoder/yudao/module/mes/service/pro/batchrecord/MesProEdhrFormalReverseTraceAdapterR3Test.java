package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** R3: fixtures use the producer's receipt witness, never an invented frozen event payload. */
class MesProEdhrFormalReverseTraceAdapterR3Test {
    private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 9, 24, 9, 0);
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesProEdhrBatchExecutionTraceLinkMapper links = mock(MesProEdhrBatchExecutionTraceLinkMapper.class);
    private final MesProProcessPoolEventMapper events = mock(MesProProcessPoolEventMapper.class);
    private final MesProcessPoolSubmissionReviewMapper reviews = mock(MesProcessPoolSubmissionReviewMapper.class);
    private final MesProcessPoolActiveOrderCompletionReceiptMapper receipts = mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrReleaseTransactionMapper releases = mock(MesProEdhrReleaseTransactionMapper.class);

    @Test
    void receiptWitnessCannotProveCurrentParameterPayload() {
        var batch = frozenBatch(9001L, 6001L, 7101L);
        currentEvent(32);
        assertMissing(Category.PARAMETER, batch);
    }

    @Test
    void receiptWitnessCannotProveCurrentEquipmentPayload() {
        var batch = frozenBatch(9001L, 6001L, 7101L);
        currentEvent(32);
        assertMissing(Category.EQUIPMENT, batch);
    }

    @Test
    void laterCorrectionCannotBecomeTheFrozenParameterOrDeviceFact() {
        var batch = frozenBatch(9001L, 6001L, 7101L);
        var event = currentEvent(32);
        // The production correction service writes afterPayload back to this current row.
        event.setRawPayload(payload(99, 88));
        assertMissing(Category.PARAMETER, batch);
        assertMissing(Category.EQUIPMENT, batch);
    }

    @Test
    void legacyReceiptCannotUseEvenAnExactCurrentReviewAsAFrozenSnapshot() {
        var batch = frozenBatch(9001L, 6001L, 7101L);
        currentEvent(32);
        var original = review(7101L, 31L);
        var extra = review(7102L, 41L);
        when(reviews.selectById(7101L)).thenReturn(original);
        when(reviews.selectListByEventId(5001L)).thenReturn(List.of(original, extra));

        assertMissing(Category.PERSON, batch);
        verify(reviews, never()).selectListByEventId(5001L);
    }

    @Test
    void legacyBatchesSharingOneEventCannotBorrowEachOthersCurrentReviews() {
        var first = frozenBatch(9001L, 6001L, 7101L);
        var second = frozenBatch(9002L, 6002L, 7102L);
        currentEvent(32);
        var firstReview = review(7101L, 31L);
        var secondReview = review(7102L, 41L);
        when(reviews.selectById(7101L)).thenReturn(firstReview);
        when(reviews.selectById(7102L)).thenReturn(secondReview);
        when(reviews.selectListByEventId(5001L)).thenReturn(List.of(firstReview, secondReview));

        assertMissing(Category.PERSON, first);
        assertMissing(Category.PERSON, second);
    }

    @Test
    void missingAllocationReviewIdCannotBeFilledFromEventReviews() {
        var batch = frozenBatch(9001L, 6001L, null);
        currentEvent(32);
        when(reviews.selectListByEventId(5001L)).thenReturn(List.of(review(7102L, 41L)));
        assertMissing(Category.PERSON, batch);
    }

    @Test
    void legacyReceiptIsNotUpgradedEvenWhenCurrentReviewLooksComplete() {
        var batch = frozenBatch(9001L, 6001L, 7101L);
        currentEvent(32);
        var original = review(7101L, 31L);
        when(reviews.selectById(7101L)).thenReturn(original);
        when(reviews.selectListByEventId(5001L)).thenReturn(List.of(original));
        assertMissing(Category.PERSON, batch);
    }

    private void assertMissing(Category category, MesProEdhrBatchExecutionDO batch) {
        var error = assertThrows(IllegalStateException.class, () -> adapter(category).readCatalog(batch));
        assertTrue(error.getMessage().startsWith("SOURCE_MISSING:"), error.getMessage());
    }

    private MesProEdhrFormalReverseTraceAdapter adapter(Category category) {
        return new MesProEdhrFormalReverseTraceAdapter(category, null, origins, links, null, null, null,
                events, null, reviews, applications, releases, receipts);
    }

    private MesProProcessPoolEventDO currentEvent(int value) {
        var event = new MesProProcessPoolEventDO().setId(5001L)
                .setEventType("PRODUCTION_SUBMIT").setWorkOrderId(1001L).setRouteId(2001L)
                .setRouteProcessId(3001L).setProcessId(4001L).setRawPayload(payload(value, 77))
                .setActualEmployeeId(21L).setSignatureUserId(22L).setServerSubmitTime(SUBMITTED_AT);
        event.setTenantId(1L);
        when(events.selectProductionSubmitsByIds(List.of(5001L))).thenReturn(List.of(event));
        return event;
    }

    private String payload(int value, int device) {
        return JsonUtils.toJsonString(Map.of("deviceParameterReadings", List.of(Map.of(
                "parameterCode", "TEMP", "value", value, "deviceId", device, "unit", "C")),
                "selectedDevices", List.of(Map.of("deviceId", device, "deviceCode", "D" + device))));
    }

    private MesProcessPoolSubmissionReviewDO review(long id, long actor) {
        var review = new MesProcessPoolSubmissionReviewDO().setId(id).setEventId(5001L)
                .setLeaderUserId(actor).setLeaderType("PRODUCTION").setReviewStatus("APPROVED")
                .setReviewedAt(SUBMITTED_AT.plusHours(1)).setReviewSignatureId(id + 100)
                .setReviewSignatureUserId(actor)
                .setReviewSignatureSnapshotJson(JsonUtils.toJsonString(Map.of("signatureId", id + 100,
                        "actorId", actor, "actionType", "TEAM_LEADER_REVIEW", "processPoolEventId", 5001L,
                        "eventType", "PRODUCTION_SUBMIT", "leaderType", "PRODUCTION",
                        "reviewStatus", "APPROVED", "reviewedAt", SUBMITTED_AT.plusHours(1))));
        review.setTenantId(1L);
        return review;
    }

    private MesProEdhrBatchExecutionDO frozenBatch(long batchId, long orderId, Long reviewId) {
        var batch = new MesProEdhrBatchExecutionDO().setId(batchId).setTenantId(1L)
                .setWorkOrderId(1001L).setRouteId(2001L).setRouteVersionId(2002L).setAggregateHash("batch-hash");
        batch.setUpdateTime(SUBMITTED_AT.plusDays(1));
        var allocation = new MesProcessPoolReportAllocationDO().setId(batchId + 100)
                .setEventId(5001L).setReviewId(reviewId).setLeaderUserId(reviewId != null && reviewId == 7102L ? 41L : 31L)
                .setActiveOrderId(orderId).setWorkOrderId(1001L).setRouteProcessId(3001L).setProcessId(4001L)
                .setAllocatedQuantity(BigDecimal.ONE).setAllocationMode("MANUAL").setLifecycleStatus("CURRENT")
                .setCreatedVersion(1).setConfirmedAt(SUBMITTED_AT.plusHours(1));
        allocation.setTenantId(1L);
        String snapshot = JsonUtils.toJsonString(Map.of(
                "activeOrderBinding", Map.of("id", orderId, "tenantId", 1L, "workOrderId", 1001L,
                        "routeId", 2001L, "routeVersionId", 2002L, "leaderUserId", 31L),
                "workOrderBinding", Map.of("id", 1001L), "allocations", List.of(allocation),
                "snapshots", List.of(Map.of("routeProcessId", 3001L, "processId", 4001L)),
                "pqcTasks", List.of(), "pqcDetails", List.of()));
        var receipt = new MesProcessPoolActiveOrderCompletionReceiptDO().setId(batchId + 1000)
                .setActiveOrderId(orderId).setWorkOrderId(1001L).setCompletedVersion(1)
                .setRouteId(2001L).setRouteVersionId(2002L)
                .setFormalSourceSnapshotJson(snapshot).setLossConditionFactsJson("[]")
                .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .setCompletionStatus("SUCCESS").setBatchRecordStatus("SUCCESS").setProcessInspectionStatus("SUCCESS")
                .setBatchRecordId(batchId + 2000).setProcessInspectionId(batchId + 3000);
        receipt.setTenantId(1L);
        receipt.setSourceSnapshotHash(DigestUtil.sha256Hex(DigestUtil.sha256Hex(snapshot) + "|[]"));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        var origin = new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(batchId).setTenantId(1L)
                .setActiveOrderId(orderId).setWorkOrderId(1001L).setCompletionVersion(1)
                .setCompletionBackfillReceiptId(receipt.getId()).setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash());
        when(origins.selectListByBatchExecutionId(batchId)).thenReturn(List.of(origin));
        when(receipts.selectByIdAndTenantId(receipt.getId(), 1L)).thenReturn(receipt);
        String witness = JsonUtils.toJsonString(Map.of("sourceType", "PRODUCTION_SUBMIT",
                "sourceId", receipt.getBatchRecordId(), "witnessHash", receipt.getReceiptHash()));
        var link = new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batchId).setTenantId(1L)
                .setLinkType("PRODUCTION_SUBMIT").setSourceObjectId(receipt.getBatchRecordId()).setRelationStatus("BOUND")
                .setSnapshotJson(witness).setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", witness));
        when(links.selectListByBatchExecutionId(batchId)).thenReturn(List.of(link));
        when(applications.selectListByBatchExecutionIds(List.of(batchId))).thenReturn(List.of());
        return batch;
    }
}
