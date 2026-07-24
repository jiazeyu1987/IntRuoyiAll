package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * T10 RED plan holder. This class intentionally does not match Surefire's
 * default *Test naming pattern, so normal regression runs do not execute it.
 *
 * Explicit RED command after T7/T8/T9 real gates pass:
 * mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchReadonlyReviewRedPlan" test "-Djdk.net.URLClassPath.disableClassPathURLCheck=true"
 */
class MesProEdhrBatchReadonlyReviewRedPlan {

    @Test
    @Disabled("T10 RED prep only: enable after T7/T8/T9 real E2E gates pass; expected RED is incomplete closed-batch route timeline.")
    void reviewTimeline_closedBatchIncludesCompleteRouteAndAllSpecialNodes() {
        throw new AssertionError("Expected RED: readonly review must include complete route plus incoming inspection, sterilization, finished product inspection report and record.");
    }

    @Test
    @Disabled("T10 RED prep only: enable after T7/T8/T9 real E2E gates pass; expected RED is missing normal form readonly facts.")
    void reviewTimeline_closedBatchIncludesReadonlyNormalFormsAndFieldValues() {
        throw new AssertionError("Expected RED: every normal route form must expose readonly form model, field values, process metadata and approved execution status.");
    }

    @Test
    @Disabled("T10 RED prep only: enable after T7/T8/T9 real E2E gates pass; expected RED is missing special-node attachment or skip evidence.")
    void reviewTimeline_closedBatchIncludesSpecialNodeAttachmentOrSkipEvidence() {
        throw new AssertionError("Expected RED: each special node must expose attachment evidence or skipped status, operator and operation time.");
    }

    @Test
    @Disabled("T10 RED prep only: enable after T7/T8/T9 real E2E gates pass; expected RED is missing signature and approval review evidence.")
    void reviewTimeline_closedBatchIncludesSignatureApprovalAndReworkEvidence() {
        throw new AssertionError("Expected RED: readonly review must expose SUBMIT, FORM_REVIEW, APPROVE signatures, approval results and rework/reject history.");
    }

    @Test
    @Disabled("T10 RED prep only: enable after T7/T8/T9 real E2E gates pass; expected RED is missing audit advance tracking evidence.")
    void reviewTimeline_closedBatchIncludesFieldAuditAdvanceTrackingAndManifestEvidence() {
        throw new AssertionError("Expected RED: readonly review must expose field audit chain, task advance events, domain trace, attachment manifest and archive manifest evidence.");
    }

    @Test
    @Disabled("T10 RED prep only: enable after T7/T8/T9 real E2E gates pass; expected RED is closed batch still exposes write entry.")
    void readonlyReview_closedBatchHasNoMesWriteEntry() {
        throw new AssertionError("Expected RED: closed-batch detail and review pages must not expose writable form, close, special-node complete/skip, field-audit save or approval write entry.");
    }
}
