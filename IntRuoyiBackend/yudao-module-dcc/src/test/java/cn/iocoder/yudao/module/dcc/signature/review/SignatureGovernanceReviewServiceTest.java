package cn.iocoder.yudao.module.dcc.signature.review;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBatchCommand;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBatchEvaluation;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBatchStatus;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewClosureCommand;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewClosureResult;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewFindingCode;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewRemediation;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewRemediationStatus;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewService;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewServiceImpl;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewSnapshotItem;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewSourceProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernanceReviewServiceTest {

    private final SignatureGovernanceReviewService service = new SignatureGovernanceReviewServiceImpl();

    @Test
    void createBatch_blocksMissingReviewOwnerPeriodSourcePermissionProjectionAndSignatureStrategy() {
        SignatureGovernanceReviewBatchEvaluation result = service.createBatch(new SignatureGovernanceReviewBatchCommand(
                "",
                "",
                "",
                LocalDate.parse("2026-06-30"),
                "quarterly signature governance review",
                Set.of(SignatureGovernanceModuleCode.DCC, SignatureGovernanceModuleCode.EDHR,
                        SignatureGovernanceModuleCode.SHOWROOM, SignatureGovernanceModuleCode.INTAUTH),
                Set.of(SignatureGovernanceModuleCode.DCC),
                List.of(projection(SignatureGovernanceModuleCode.DCC, "dcc_electronic_signature", "710088",
                        "dcc-hash-001", SignatureGovernanceReviewFindingCode.VALID)),
                false));

        assertEquals(SignatureGovernanceReviewBatchStatus.BLOCKED, result.status());
        assertFalse(result.collectable());
        assertEquals(Set.of(
                SignatureGovernanceReviewBlockerCode.REVIEW_OWNER_MISSING,
                SignatureGovernanceReviewBlockerCode.PERIOD_RULE_MISSING,
                SignatureGovernanceReviewBlockerCode.DATA_SOURCE_PERMISSION_MISSING,
                SignatureGovernanceReviewBlockerCode.SAMPLE_PROJECTION_MISSING,
                SignatureGovernanceReviewBlockerCode.REVIEW_SIGNATURE_STRATEGY_MISSING), blockerCodes(result));
        assertTrue(result.batchId().isEmpty());
        assertTrue(result.snapshotHash().isEmpty());
        assertTrue(result.blockers().stream().allMatch(blocker -> !blocker.impact().isBlank()));
    }

    @Test
    void evaluateBatch_freezesSnapshotHashAndPreservesSignatureExceptionFindings() {
        List<SignatureGovernanceReviewSourceProjection> projections = List.of(
                projection(SignatureGovernanceModuleCode.DCC, "dcc_electronic_signature", "710088",
                        "dcc-hash-001", SignatureGovernanceReviewFindingCode.SIGNATURE_PERMISSION_EXCEPTION),
                projection(SignatureGovernanceModuleCode.EDHR, "mes_batch_record_signature", "810099",
                        "edhr-hash-001", SignatureGovernanceReviewFindingCode.SIGNATURE_LOCK_EXCEPTION),
                projection(SignatureGovernanceModuleCode.SHOWROOM, "showroom_change_request_signature", "910011",
                        "showroom-hash-001", SignatureGovernanceReviewFindingCode.SIGNATURE_FAILURE_RECORDED),
                projection(SignatureGovernanceModuleCode.INTAUTH, "electronic_signatures", "auth-1001",
                        "intauth-hash-001", SignatureGovernanceReviewFindingCode.ABNORMAL_SIGNATURE_EVIDENCE));

        SignatureGovernanceReviewBatchEvaluation result = service.evaluateBatch(readyCommand(projections));
        SignatureGovernanceReviewBatchEvaluation reversed = service.evaluateBatch(readyCommand(List.of(
                projections.get(3), projections.get(2), projections.get(1), projections.get(0))));

        assertEquals(SignatureGovernanceReviewBatchStatus.COLLECTED, result.status());
        assertTrue(result.collectable());
        assertTrue(result.blockers().isEmpty());
        assertFalse(result.batchId().isEmpty());
        assertFalse(result.snapshotHash().isEmpty());
        assertEquals(result.snapshotHash(), reversed.snapshotHash());
        assertEquals(Set.of(
                SignatureGovernanceReviewFindingCode.SIGNATURE_PERMISSION_EXCEPTION,
                SignatureGovernanceReviewFindingCode.SIGNATURE_LOCK_EXCEPTION,
                SignatureGovernanceReviewFindingCode.SIGNATURE_FAILURE_RECORDED,
                SignatureGovernanceReviewFindingCode.ABNORMAL_SIGNATURE_EVIDENCE), findingCodes(result));
        assertThrows(UnsupportedOperationException.class, () -> result.snapshotItems().add(
                item("manual", SignatureGovernanceReviewFindingCode.VALID)));
    }

    @Test
    void signReview_blocksOpenOverdueAndPendingRemediationWithTypedBlockers() {
        SignatureGovernanceReviewClosureResult result = service.signReview(new SignatureGovernanceReviewClosureCommand(
                "review-20260630-001",
                "snapshot-hash-001",
                true,
                false,
                List.of(item("dcc:710088", SignatureGovernanceReviewFindingCode.ABNORMAL_SIGNATURE_EVIDENCE)),
                List.of(
                        remediation("dcc:710088", SignatureGovernanceReviewRemediationStatus.OPEN),
                        remediation("edhr:810099", SignatureGovernanceReviewRemediationStatus.OVERDUE),
                        remediation("showroom:910011", SignatureGovernanceReviewRemediationStatus.PENDING_REVIEW))));

        assertEquals(SignatureGovernanceReviewBatchStatus.BLOCKED, result.status());
        assertFalse(result.signed());
        assertFalse(result.closed());
        assertEquals(Set.of(
                SignatureGovernanceReviewBlockerCode.OPEN_REMEDIATION,
                SignatureGovernanceReviewBlockerCode.OVERDUE_REMEDIATION,
                SignatureGovernanceReviewBlockerCode.PENDING_REMEDIATION_REVIEW), closureBlockerCodes(result));
    }

    @Test
    void closeBatch_blocksUnresolvedExceptionAndMissingSignatureStrategy() {
        SignatureGovernanceReviewClosureResult result = service.closeBatch(new SignatureGovernanceReviewClosureCommand(
                "review-20260630-001",
                "snapshot-hash-001",
                false,
                false,
                List.of(item("dcc:710088", SignatureGovernanceReviewFindingCode.ABNORMAL_SIGNATURE_EVIDENCE)),
                List.of(remediation("dcc:710088", SignatureGovernanceReviewRemediationStatus.OPEN))));

        assertEquals(SignatureGovernanceReviewBatchStatus.BLOCKED, result.status());
        assertFalse(result.signed());
        assertFalse(result.closed());
        assertEquals(Set.of(
                SignatureGovernanceReviewBlockerCode.REVIEW_SIGNATURE_STRATEGY_MISSING,
                SignatureGovernanceReviewBlockerCode.REVIEW_SIGNATURE_MISSING,
                SignatureGovernanceReviewBlockerCode.OPEN_REMEDIATION), closureBlockerCodes(result));
    }

    @Test
    void closeBatch_allowsClosedRemediationOrApprovedExceptionAfterReviewSignature() {
        SignatureGovernanceReviewClosureResult result = service.closeBatch(new SignatureGovernanceReviewClosureCommand(
                "review-20260630-001",
                "snapshot-hash-001",
                true,
                true,
                List.of(
                        item("dcc:710088", SignatureGovernanceReviewFindingCode.ABNORMAL_SIGNATURE_EVIDENCE),
                        item("edhr:810099", SignatureGovernanceReviewFindingCode.SIGNATURE_LOCK_EXCEPTION)),
                List.of(
                        remediation("dcc:710088", SignatureGovernanceReviewRemediationStatus.CLOSED),
                        remediation("edhr:810099", SignatureGovernanceReviewRemediationStatus.EXCEPTION_APPROVED))));

        assertEquals(SignatureGovernanceReviewBatchStatus.CLOSED, result.status());
        assertTrue(result.signed());
        assertTrue(result.closed());
        assertTrue(result.blockers().isEmpty());
    }

    private static SignatureGovernanceReviewBatchCommand readyCommand(
            List<SignatureGovernanceReviewSourceProjection> projections) {
        return new SignatureGovernanceReviewBatchCommand(
                "qa-owner-1001",
                "2026-Q2",
                "review-rule-v1",
                LocalDate.parse("2026-06-30"),
                "quarterly signature governance review",
                Set.of(SignatureGovernanceModuleCode.DCC, SignatureGovernanceModuleCode.EDHR,
                        SignatureGovernanceModuleCode.SHOWROOM, SignatureGovernanceModuleCode.INTAUTH),
                Set.of(SignatureGovernanceModuleCode.DCC, SignatureGovernanceModuleCode.EDHR,
                        SignatureGovernanceModuleCode.SHOWROOM, SignatureGovernanceModuleCode.INTAUTH),
                projections,
                true);
    }

    private static SignatureGovernanceReviewSourceProjection projection(SignatureGovernanceModuleCode moduleCode,
                                                                        String sourceTable,
                                                                        String sourceId,
                                                                        String sourceHash,
                                                                        SignatureGovernanceReviewFindingCode findingCode) {
        return new SignatureGovernanceReviewSourceProjection(moduleCode, sourceTable, sourceId, sourceHash,
                moduleCode.name() + "_ACTION", moduleCode.name() + "_MEANING", findingCode);
    }

    private static SignatureGovernanceReviewSnapshotItem item(String sourceRef,
                                                              SignatureGovernanceReviewFindingCode findingCode) {
        assertNotNull(sourceRef);
        return new SignatureGovernanceReviewSnapshotItem(SignatureGovernanceModuleCode.DCC, "review_source",
                sourceRef, "source-hash-" + sourceRef, "DCC_ACTION", "DCC_MEANING", findingCode);
    }

    private static SignatureGovernanceReviewRemediation remediation(
            String sourceRef, SignatureGovernanceReviewRemediationStatus status) {
        return new SignatureGovernanceReviewRemediation(sourceRef, status, "evidence-" + sourceRef,
                "reviewer-1001", "exception-" + sourceRef);
    }

    private static Set<SignatureGovernanceReviewBlockerCode> blockerCodes(
            SignatureGovernanceReviewBatchEvaluation result) {
        return result.blockers().stream().map(blocker -> blocker.code()).collect(Collectors.toSet());
    }

    private static Set<SignatureGovernanceReviewBlockerCode> closureBlockerCodes(
            SignatureGovernanceReviewClosureResult result) {
        return result.blockers().stream().map(blocker -> blocker.code()).collect(Collectors.toSet());
    }

    private static Set<SignatureGovernanceReviewFindingCode> findingCodes(
            SignatureGovernanceReviewBatchEvaluation result) {
        return result.snapshotItems().stream().map(item -> item.findingCode()).collect(Collectors.toSet());
    }
}
