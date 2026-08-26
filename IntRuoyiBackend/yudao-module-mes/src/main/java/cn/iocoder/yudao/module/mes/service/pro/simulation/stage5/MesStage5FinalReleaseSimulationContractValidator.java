package cn.iocoder.yudao.module.mes.service.pro.simulation.stage5;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class MesStage5FinalReleaseSimulationContractValidator {

    public static final String CONTRACT_NAME = "batchExecutionDossierSnapshot";
    public static final String CONTRACT_VERSION = "stage4.v1";
    public static final String PENDING_RELEASE_STATUS = "PENDING_APPROVAL";
    public static final String PENDING_APPLICATION_STATUS = "MANAGER_RELEASE_PENDING";
    public static final String RELEASED_STATUS = "RELEASED";
    public static final String RELEASED_APPLICATION_STATUS = "RELEASED";
    public static final String UPSTREAM_CONTEXT_BLOCKER = "AUTHORITATIVE_UPSTREAM_CONTEXT_REQUIRED";
    public static final String RELEASE_SNAPSHOT_CONTRACT_NAME = "stage5.releaseSnapshot";
    public static final String RELEASE_SNAPSHOT_CONTRACT_VERSION = "stage5.releaseSnapshot.v1";
    public static final Set<String> REQUIRED_NODE_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private MesStage5FinalReleaseSimulationContractValidator() {
    }

    public static void validateInputFixture(Map<?, ?> dossierSnapshot,
                                            Map<?, ?> managerReleaseContext) {
        validateDossierSnapshot(dossierSnapshot);
        require(managerReleaseContext, "releaseApplicationId");
        require(managerReleaseContext, "releaseTransactionId");
        require(managerReleaseContext, "managerReleaseWorkTaskId");
        requireHash(managerReleaseContext.get("managerCandidateSnapshotHash"),
                "managerCandidateSnapshotHash");
        requireHash(managerReleaseContext.get("reportSnapshotHash"), "reportSnapshotHash");
        requireEquals(PENDING_RELEASE_STATUS, managerReleaseContext.get("releaseStatus"),
                "releaseStatus");
        requireEquals(PENDING_APPLICATION_STATUS, managerReleaseContext.get("applicationStatus"),
                "applicationStatus");
        require(managerReleaseContext, "transactionVersion");
        if (!Boolean.TRUE.equals(managerReleaseContext.get("candidateFrozen"))) {
            throw new IllegalArgumentException("candidateFrozen must be true");
        }
    }

    public static void validateDossierSnapshot(Map<?, ?> snapshot) {
        requireEquals(CONTRACT_NAME, snapshot.get("contractName"), "contractName");
        requireEquals(CONTRACT_VERSION, snapshot.get("contractVersion"), "contractVersion");
        for (String field : List.of("batchExecutionId", "incomingInspectionAttachmentId",
                "sterilizationAttachmentId", "hashes", "nodeStatuses",
                "dossierReadyForRelease", "blockers")) {
            require(snapshot, field);
        }
        if (!snapshot.containsKey("finalReleaseRecordId")) {
            throw new IllegalArgumentException("missing required field: finalReleaseRecordId");
        }
        List<?> finishedAttachmentIds = list(snapshot.get("finishedProductInspectionAttachmentIds"),
                "finishedProductInspectionAttachmentIds");
        if (finishedAttachmentIds.size() != 2) {
            throw new IllegalArgumentException("finishedProductInspectionAttachmentIds must contain report and record");
        }
        Map<?, ?> hashes = map(snapshot.get("hashes"), "hashes");
        requireHash(hashes.get("incomingInspectionAttachmentHash"), "incomingInspectionAttachmentHash");
        requireHash(hashes.get("sterilizationAttachmentHash"), "sterilizationAttachmentHash");
        List<?> finishedHashes = list(hashes.get("finishedProductInspectionAttachmentHashes"),
                "finishedProductInspectionAttachmentHashes");
        if (finishedHashes.size() != 2) {
            throw new IllegalArgumentException("finishedProductInspectionAttachmentHashes must contain report and record");
        }
        finishedHashes.forEach(value -> requireHash(value, "finishedProductInspectionAttachmentHashes.item"));
        Map<?, ?> nodeStatuses = map(snapshot.get("nodeStatuses"), "nodeStatuses");
        if (!Objects.equals(REQUIRED_NODE_TYPES, nodeStatuses.keySet())) {
            throw new IllegalArgumentException("nodeStatuses must use the four real report node types");
        }
        if (nodeStatuses.values().stream().anyMatch(value -> !Objects.equals("COMPLETED", value))) {
            throw new IllegalArgumentException("all four report nodes must be completed");
        }
        if (!Boolean.TRUE.equals(snapshot.get("dossierReadyForRelease"))) {
            throw new IllegalArgumentException("dossierReadyForRelease must be true");
        }
        if (snapshot.get("finalReleaseRecordId") != null) {
            throw new IllegalArgumentException("Stage5 input must not contain a release record");
        }
        List<?> blockers = list(snapshot.get("blockers"), "blockers");
        if (!blockers.isEmpty()) {
            throw new IllegalArgumentException("Stage5 input dossier must have no blockers");
        }
    }

    public static void validateOutput(Map<?, ?> output) {
        for (String field : List.of("simulationRunId", "batchExecutionId", "releaseApplicationId",
                "releaseTransactionId", "managerReleaseWorkTaskId", "releaseStatus",
                "applicationStatus", "sourceDossierHash", "precheckResult", "blockers",
                "batchExecutionDossierSnapshot", "managerReleaseContext", "finalReleaseReady")) {
            require(output, field);
        }
        requireEquals(RELEASED_STATUS, output.get("releaseStatus"), "releaseStatus");
        requireEquals(RELEASED_APPLICATION_STATUS, output.get("applicationStatus"),
                "applicationStatus");
        requireHash(output.get("sourceDossierHash"), "sourceDossierHash");
        validateDossierSnapshot(map(output.get("batchExecutionDossierSnapshot"),
                "batchExecutionDossierSnapshot"));
        Map<?, ?> dossier = map(output.get("batchExecutionDossierSnapshot"),
                "batchExecutionDossierSnapshot");
        Map<?, ?> managerContext = map(output.get("managerReleaseContext"), "managerReleaseContext");
        validateDossierSnapshot(dossier);
        requireEquals(RELEASED_STATUS, managerContext.get("releaseStatus"), "managerReleaseContext.releaseStatus");
        requireEquals(RELEASED_APPLICATION_STATUS, managerContext.get("applicationStatus"),
                "managerReleaseContext.applicationStatus");
        require(managerContext, "releaseDecisionId");
        if (!Boolean.TRUE.equals(managerContext.get("candidateFrozen"))) {
            throw new IllegalArgumentException("candidateFrozen must be true");
        }
        if (!(output.get("precheckResult") instanceof Map<?, ?> precheck)
                || !Boolean.TRUE.equals(precheck.get("passed"))) {
            throw new IllegalArgumentException("precheckResult.passed must be true");
        }
        if (!(output.get("blockers") instanceof List<?> blockers)) {
            throw new IllegalArgumentException("blockers must be an array");
        }
        if (!blockers.isEmpty() || !Boolean.TRUE.equals(output.get("finalReleaseReady"))) {
            throw new IllegalArgumentException("released Stage5 output must have no blockers and finalReleaseReady=true");
        }
        validateReleaseSnapshot(map(output.get("releaseSnapshot"), "releaseSnapshot"));
    }

    public static void validateReleaseSnapshot(Map<?, ?> snapshot) {
        requireEquals(RELEASE_SNAPSHOT_CONTRACT_NAME, snapshot.get("contractName"), "releaseSnapshot.contractName");
        requireEquals(RELEASE_SNAPSHOT_CONTRACT_VERSION, snapshot.get("contractVersion"),
                "releaseSnapshot.contractVersion");
        for (String field : List.of("batchExecutionId", "releaseReceiptId", "releaseDecisionId",
                "releasedAt", "releaseStatus", "threeFileEvidence", "sourceChain",
                "releaseApprovalWorkTaskId", "reportSnapshotHash", "version")) {
            require(snapshot, field);
        }
        requireEquals(RELEASED_STATUS, snapshot.get("releaseStatus"), "releaseSnapshot.releaseStatus");
        requireHash(snapshot.get("reportSnapshotHash"), "releaseSnapshot.reportSnapshotHash");
        List<?> fileEvidence = list(snapshot.get("threeFileEvidence"), "releaseSnapshot.threeFileEvidence");
        if (fileEvidence.size() != 3) {
            throw new IllegalArgumentException("releaseSnapshot.threeFileEvidence must contain three material categories");
        }
        fileEvidence.forEach(value -> {
            Map<?, ?> evidence = map(value, "releaseSnapshot.threeFileEvidence.item");
            require(evidence, "nodeType");
            List<?> hashes = list(evidence.get("sha256"), "releaseSnapshot.threeFileEvidence.item.sha256");
            if (hashes.isEmpty()) {
                throw new IllegalArgumentException("releaseSnapshot.threeFileEvidence.item.sha256 must not be empty");
            }
            hashes.forEach(hash -> requireHash(hash, "releaseSnapshot.threeFileEvidence.item.sha256.item"));
        });
        Map<?, ?> sourceChain = map(snapshot.get("sourceChain"), "releaseSnapshot.sourceChain");
        for (String field : List.of("productionSourceIds", "pickListId", "backfillReceiptId")) {
            require(sourceChain, field);
        }
        if (list(sourceChain.get("productionSourceIds"), "releaseSnapshot.sourceChain.productionSourceIds").isEmpty()) {
            throw new IllegalArgumentException("releaseSnapshot.sourceChain.productionSourceIds must not be empty");
        }
    }

    private static void require(Map<?, ?> map, String field) {
        if (map == null || !map.containsKey(field) || map.get(field) == null) {
            throw new IllegalArgumentException("missing required field: " + field);
        }
    }

    private static void requireEquals(Object expected, Object actual, String field) {
        if (!Objects.equals(expected, actual)) {
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
