package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesStage6TraceabilityContractTest {

    @Test
    void acceptsReleasedStage5FixtureAndVerifiedTraceabilitySnapshot() {
        Map<String, Object> releaseSnapshot = map(
                "contractName", "stage5.releaseSnapshot",
                "contractVersion", "stage5.releaseSnapshot.v1",
                "batchExecutionId", 101L,
                "releaseReceiptId", "EDHR_RELEASE_RECEIPT:1:2",
                "releaseDecisionId", 2L,
                "releasedAt", "2026-08-25T10:00:00",
                "releaseStatus", "RELEASED",
                "threeFileEvidence", List.of(
                        Map.of("nodeType", "INCOMING_INSPECTION", "sourceIds", List.of(1L),
                                "sha256", List.of("a".repeat(64))),
                        Map.of("nodeType", "STERILIZATION", "sourceIds", List.of(2L),
                                "sha256", List.of("b".repeat(64))),
                        Map.of("nodeType", "FINISHED_PRODUCT", "sourceIds", List.of(3L, 4L),
                                "sha256", List.of("c".repeat(64), "d".repeat(64)))
                ),
                "sourceChain", Map.of("productionSourceIds", List.of(10L), "pickListId", 11L,
                        "backfillReceiptId", "12"),
                "releaseApprovalWorkTaskId", 13L,
                "reportSnapshotHash", "e".repeat(64),
                "version", 3
        );
        Map<String, Object> traceSnapshot = map(
                "contractName", "stage6.traceabilitySnapshot",
                "contractVersion", "stage6.traceabilitySnapshot.v1",
                "simulationRunId", "STAGE6-IDI-001",
                "batchExecutionId", 101L,
                "releaseTransactionId", 1L,
                "releaseReceiptId", "EDHR_RELEASE_RECEIPT:1:2",
                "releaseStatus", "RELEASED",
                "tracePageRequest", Map.of("method", "GET", "path", "/mes/pro/batch-record-execution/domain-trace/page"),
                "traceDetailRequest", Map.of("method", "GET", "path", "/mes/pro/batch-record-execution/domain-trace/detail"),
                "traceVerifyRequest", Map.of("method", "POST", "path", "/mes/pro/batch-record-execution/domain-trace/verify"),
                "items", List.of(
                        Map.of("itemType", "WORK_ORDER", "status", "VERIFIED", "sourceId", 1L, "snapshotHash", "a".repeat(64)),
                        Map.of("itemType", "ROUTE_PROCESS", "status", "VERIFIED", "sourceId", 2L, "snapshotHash", "b".repeat(64)),
                        Map.of("itemType", "BATCH_RECORD_REPORT", "status", "VERIFIED", "sourceId", 3L, "snapshotHash", "c".repeat(64)),
                        Map.of("itemType", "BATCH", "status", "VERIFIED", "sourceId", 4L, "snapshotHash", "d".repeat(64)),
                        Map.of("itemType", "EXECUTION_SNAPSHOT", "status", "VERIFIED", "sourceId", 5L, "snapshotHash", "e".repeat(64)),
                        Map.of("itemType", "FIELD_AUDIT_BASELINE", "status", "VERIFIED", "sourceId", 6L, "snapshotHash", "f".repeat(64))
                ),
                "blockers", List.of(),
                "complete", true,
                "frontendDisplayEntry", Map.of("formTraceRoute", "/mes/pro/feedback/edhr-form-trace",
                        "traceDrawerComponent", "BatchExecutionTraceDrawer",
                        "domainTraceDetailRoute", "/mes/pro/feedback/edhr-domain-trace/detail",
                        "pageAssertions", Map.of("drawerDisplaysBatchRecordForm", true,
                                "drawerDisplaysOperationAudit", true,
                                "drawerDisplaysElectronicSignature", true,
                                "drawerDisplaysReleaseEvents", true,
                                "domainTraceDetailDisplaysItems", true)),
                "backendTraceabilitySummary", Map.of("status", "VERIFIED", "blockers", List.of())
        );

        assertDoesNotThrow(() -> MesStage6TraceabilityContractValidator.validateReleaseSnapshot(releaseSnapshot));
        assertDoesNotThrow(() -> MesStage6TraceabilityContractValidator.validateTraceabilitySnapshot(traceSnapshot));
    }

    @Test
    void rejectsPendingReleaseOrIncompleteTraceability() {
        assertThrows(IllegalArgumentException.class, () ->
                MesStage6TraceabilityContractValidator.validateReleaseSnapshot(
                        Map.of("contractName", "stage5.releaseSnapshot",
                                "contractVersion", "stage5.releaseSnapshot.v1",
                                "releaseStatus", "PENDING_APPROVAL")));
        assertThrows(IllegalArgumentException.class, () ->
                MesStage6TraceabilityContractValidator.validateTraceabilitySnapshot(
                        Map.of("contractName", "stage6.traceabilitySnapshot",
                                "contractVersion", "stage6.traceabilitySnapshot.v1",
                                "releaseStatus", "RELEASED",
                                "items", List.of(), "blockers", List.of(), "complete", true)));
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], values[index + 1]);
        }
        return result;
    }
}
