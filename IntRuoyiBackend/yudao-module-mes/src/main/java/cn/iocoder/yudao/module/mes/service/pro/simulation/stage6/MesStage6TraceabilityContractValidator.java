package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates the persisted release fixture and the real domain-trace result returned to the UI. */
public final class MesStage6TraceabilityContractValidator {

    public static final String RELEASE_SNAPSHOT_CONTRACT_NAME = "stage5.releaseSnapshot";
    public static final String RELEASE_SNAPSHOT_CONTRACT_VERSION = "stage5.releaseSnapshot.v1";
    public static final String TRACEABILITY_SNAPSHOT_CONTRACT_NAME = "stage6.traceabilitySnapshot";
    public static final String TRACEABILITY_SNAPSHOT_CONTRACT_VERSION = "stage6.traceabilitySnapshot.v1";
    public static final String RELEASED_STATUS = "RELEASED";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> CURRENT_DOMAIN_TRACE_ITEMS = Set.of(
            "WORK_ORDER", "ROUTE_PROCESS", "BATCH_RECORD_REPORT", "BATCH",
            "EXECUTION_SNAPSHOT", "FIELD_AUDIT_BASELINE");

    private MesStage6TraceabilityContractValidator() {
    }

    public static void validateReleaseSnapshot(Map<?, ?> snapshot) {
        requireEquals(RELEASE_SNAPSHOT_CONTRACT_NAME, snapshot, "contractName");
        requireEquals(RELEASE_SNAPSHOT_CONTRACT_VERSION, snapshot, "contractVersion");
        for (String field : List.of("batchExecutionId", "releaseReceiptId", "releaseDecisionId",
                "releasedAt", "releaseStatus", "threeFileEvidence", "sourceChain",
                "releaseApprovalWorkTaskId", "reportSnapshotHash", "version")) {
            require(snapshot, field);
        }
        requireEquals(RELEASED_STATUS, snapshot, "releaseStatus");
        requireHash(snapshot.get("reportSnapshotHash"), "reportSnapshotHash");
        List<?> files = list(snapshot.get("threeFileEvidence"), "threeFileEvidence");
        if (files.size() != 3) {
            throw new IllegalArgumentException("threeFileEvidence must contain three material categories");
        }
        files.forEach(value -> {
            Map<?, ?> file = map(value, "threeFileEvidence.item");
            require(file, "nodeType");
            if (list(file.get("sourceIds"), "threeFileEvidence.item.sourceIds").isEmpty()) {
                throw new IllegalArgumentException("threeFileEvidence.item.sourceIds must not be empty");
            }
            list(file.get("sha256"), "threeFileEvidence.item.sha256").forEach(hash ->
                    requireHash(hash, "threeFileEvidence.item.sha256"));
        });
        Map<?, ?> sourceChain = map(snapshot.get("sourceChain"), "sourceChain");
        for (String field : List.of("productionSourceIds", "pickListId", "backfillReceiptId")) {
            require(sourceChain, field);
        }
        if (list(sourceChain.get("productionSourceIds"), "sourceChain.productionSourceIds").isEmpty()) {
            throw new IllegalArgumentException("sourceChain.productionSourceIds must not be empty");
        }
    }

    public static void validateTraceabilitySnapshot(Map<?, ?> snapshot) {
        requireEquals(TRACEABILITY_SNAPSHOT_CONTRACT_NAME, snapshot, "contractName");
        requireEquals(TRACEABILITY_SNAPSHOT_CONTRACT_VERSION, snapshot, "contractVersion");
        for (String field : List.of("simulationRunId", "batchExecutionId", "releaseTransactionId",
                "releaseReceiptId", "releaseStatus", "tracePageRequest", "traceDetailRequest",
                "traceVerifyRequest", "items", "blockers", "complete", "frontendDisplayEntry",
                "backendTraceabilitySummary")) {
            require(snapshot, field);
        }
        requireEquals(RELEASED_STATUS, snapshot, "releaseStatus");
        validateRequest(snapshot.get("tracePageRequest"), "GET", "/mes/pro/batch-record-execution/domain-trace/page");
        validateRequest(snapshot.get("traceDetailRequest"), "GET", "/mes/pro/batch-record-execution/domain-trace/detail");
        validateRequest(snapshot.get("traceVerifyRequest"), "POST", "/mes/pro/batch-record-execution/domain-trace/verify");
        List<?> items = list(snapshot.get("items"), "items");
        Set<String> actualItems = items.stream().map(value -> {
            Map<?, ?> item = map(value, "items.item");
            require(item, "itemType");
            require(item, "status");
            require(item, "snapshotHash");
            requireHash(item.get("snapshotHash"), "items.item.snapshotHash");
            return String.valueOf(item.get("itemType"));
        }).collect(java.util.stream.Collectors.toSet());
        if (!actualItems.containsAll(CURRENT_DOMAIN_TRACE_ITEMS)) {
            throw new IllegalArgumentException("traceability items do not cover the current domain-trace contract");
        }
        List<?> blockers = list(snapshot.get("blockers"), "blockers");
        if (!blockers.isEmpty() || !Boolean.TRUE.equals(snapshot.get("complete"))) {
            throw new IllegalArgumentException("traceability snapshot must be complete without blockers");
        }
        Map<?, ?> backend = map(snapshot.get("backendTraceabilitySummary"), "backendTraceabilitySummary");
        requireEquals("VERIFIED", backend, "status");
        if (!(backend.get("blockers") instanceof List<?> backendBlockers) || !backendBlockers.isEmpty()) {
            throw new IllegalArgumentException("backendTraceabilitySummary.blockers must be empty");
        }
        Map<?, ?> frontend = map(snapshot.get("frontendDisplayEntry"), "frontendDisplayEntry");
        Map<?, ?> assertions = map(frontend.get("pageAssertions"), "frontendDisplayEntry.pageAssertions");
        for (String field : List.of("drawerDisplaysBatchRecordForm", "drawerDisplaysOperationAudit",
                "drawerDisplaysElectronicSignature", "drawerDisplaysReleaseEvents",
                "domainTraceDetailDisplaysItems")) {
            if (!Boolean.TRUE.equals(assertions.get(field))) {
                throw new IllegalArgumentException("frontend page assertion must be true: " + field);
            }
        }
    }

    private static void validateRequest(Object value, String method, String path) {
        Map<?, ?> request = map(value, "request");
        requireEquals(method, request, "method");
        requireEquals(path, request, "path");
    }

    private static void require(Map<?, ?> map, String field) {
        if (map == null || !map.containsKey(field) || map.get(field) == null) {
            throw new IllegalArgumentException("missing required field: " + field);
        }
    }

    private static void requireEquals(Object expected, Map<?, ?> map, String field) {
        require(map, field);
        if (!Objects.equals(expected, map.get(field))) {
            throw new IllegalArgumentException(field + " must equal " + expected);
        }
    }

    private static Map<?, ?> map(Object value, String field) {
        if (!(value instanceof Map<?, ?> result)) {
            throw new IllegalArgumentException(field + " must be an object");
        }
        return result;
    }

    private static List<?> list(Object value, String field) {
        if (!(value instanceof List<?> result)) {
            throw new IllegalArgumentException(field + " must be an array");
        }
        return result;
    }

    private static void requireHash(Object value, String field) {
        if (value == null || !SHA256.matcher(String.valueOf(value)).matches()) {
            throw new IllegalArgumentException(field + " must be a lowercase SHA-256 hash");
        }
    }
}
