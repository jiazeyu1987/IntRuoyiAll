package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class MesStage4DossierUploadSimulationContractValidator {

    public static final String SCHEMA_VERSION = "stage3.batchExecutionSnapshot.v1";
    public static final String INPUT_STATUS = "DOSSIER_UPLOAD_PENDING";
    public static final String OUTPUT_CONTRACT_NAME = "batchExecutionDossierSnapshot";
    public static final String OUTPUT_CONTRACT_VERSION = "stage4.v1";
    public static final Set<String> REQUIRED_NODE_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private MesStage4DossierUploadSimulationContractValidator() {
    }

    public static void validateInput(Map<String, Object> snapshot) {
        requireEquals(SCHEMA_VERSION, snapshot.get("schemaVersion"), "schemaVersion");
        requireEquals(INPUT_STATUS, snapshot.get("status"), "status");
        requireNonBlank(snapshot, "simulationRunId");
        requireNonBlank(snapshot, "sourceInputContract");
        for (String field : List.of("batchExecutionId", "batchExecutionCode", "batchCode",
                "activeContextKey", "activeOrderId", "activeOrderCode", "workOrderId", "workOrderCode",
                "erpWorkOrderNo", "routeId", "routeVersionId", "routeVersionNo", "materialIssueSource",
                "batchRecordLinks", "processInspectionLinks",
                "hasLoss", "lossReportRequirement", "optionalLossReportLinks", "specialNodeUploadStatus",
                "sourceHash", "blockers")) {
            require(snapshot, field);
        }
        Map<?, ?> lossRequirement = map(snapshot.get("lossReportRequirement"), "lossReportRequirement");
        require(lossRequirement, "required");
        require(lossRequirement, "status");
        boolean hasLoss = Boolean.TRUE.equals(snapshot.get("hasLoss"));
        if (hasLoss != Boolean.TRUE.equals(lossRequirement.get("required"))) {
            throw new IllegalArgumentException("hasLoss and lossReportRequirement.required are inconsistent");
        }
        requireEquals(hasLoss ? "COMPLETED" : "NOT_REQUIRED", lossRequirement.get("status"),
                "lossReportRequirement.status");
        List<?> lossLinks = list(snapshot.get("optionalLossReportLinks"), "optionalLossReportLinks");
        if (hasLoss && lossLinks.isEmpty()) {
            throw new IllegalArgumentException("lossReportLinks are required when hasLoss is true");
        }
        if (!hasLoss && !lossLinks.isEmpty()) {
            throw new IllegalArgumentException("lossReportLinks must be empty when hasLoss is false");
        }
        Map<?, ?> nodeStatus = map(snapshot.get("specialNodeUploadStatus"), "specialNodeUploadStatus");
        requireEquals("PENDING_UPLOAD", nodeStatus.get("overallStatus"),
                "specialNodeUploadStatus.overallStatus");
        List<?> requiredNodes = list(nodeStatus.get("requiredNodes"),
                "specialNodeUploadStatus.requiredNodes");
        if (requiredNodes.size() != REQUIRED_NODE_TYPES.size()) {
            throw new IllegalArgumentException("requiredNodes must contain exactly four nodes");
        }
        Set<String> actualNodeTypes = requiredNodes.stream()
                .map(item -> {
                    Map<?, ?> node = map(item, "requiredNodes.item");
                    require(node, "nodeType");
                    requireEquals("PENDING_UPLOAD", node.get("status"), "requiredNodes.item.status");
                    return String.valueOf(node.get("nodeType"));
                })
                .collect(java.util.stream.Collectors.toSet());
        if (!Objects.equals(REQUIRED_NODE_TYPES, actualNodeTypes)) {
            throw new IllegalArgumentException("requiredNodes must contain the four real release report node types");
        }
    }

    public static void validateOutput(Map<String, Object> snapshot) {
        requireEquals(OUTPUT_CONTRACT_NAME, snapshot.get("contractName"), "contractName");
        requireEquals(OUTPUT_CONTRACT_VERSION, snapshot.get("contractVersion"), "contractVersion");
        requireNonBlank(snapshot, "batchExecutionId");
        Object sourceSnapshotHash = snapshot.get("sourceSnapshotHash");
        requireHash(sourceSnapshotHash, "sourceSnapshotHash");
        requireNonBlank(snapshot, "incomingInspectionAttachmentId");
        requireNonBlank(snapshot, "sterilizationAttachmentId");
        List<?> finishedAttachmentIds = list(snapshot.get("finishedProductInspectionAttachmentIds"),
                "finishedProductInspectionAttachmentIds");
        if (finishedAttachmentIds.size() != 2) {
            throw new IllegalArgumentException("finished product inspection must contain report and record attachments");
        }
        if (finishedAttachmentIds.stream().anyMatch(value -> String.valueOf(value).isBlank())) {
            throw new IllegalArgumentException("finished product inspection attachment ids are required");
        }
        Map<?, ?> hashes = map(snapshot.get("hashes"), "hashes");
        requireHash(hashes.get("incomingInspectionAttachmentHash"), "incomingInspectionAttachmentHash");
        requireHash(hashes.get("sterilizationAttachmentHash"), "sterilizationAttachmentHash");
        List<?> finishedHashes = list(hashes.get("finishedProductInspectionAttachmentHashes"),
                "finishedProductInspectionAttachmentHashes");
        if (finishedHashes.size() != 2 || finishedHashes.stream().anyMatch(value -> !SHA256.matcher(String.valueOf(value)).matches())) {
            throw new IllegalArgumentException("finished product inspection attachment hashes are invalid");
        }
        Map<?, ?> fileIds = exactNodeMap(snapshot.get("fileId"), "fileId");
        Map<?, ?> fileNames = exactNodeMap(snapshot.get("fileName"), "fileName");
        Map<?, ?> storagePaths = exactNodeMap(snapshot.get("storagePath"), "storagePath");
        Map<?, ?> attachmentAudit = exactNodeMap(snapshot.get("attachmentAudit"), "attachmentAudit");
        Map<?, ?> fileEvidence = exactNodeMap(snapshot.get("fileEvidence"), "fileEvidence");
        fileIds.values().forEach(value -> requireNonBlankValue(value, "fileId"));
        fileNames.values().forEach(value -> requireNonBlankValue(value, "fileName"));
        storagePaths.values().forEach(value -> requireNonBlankValue(value, "storagePath"));
        attachmentAudit.values().forEach(value -> map(value, "attachmentAudit.item"));
        fileEvidence.forEach((nodeKey, value) -> {
            Map<?, ?> evidence = map(value, "fileEvidence.item");
            requireEquals(nodeKey, evidence.get("nodeKey"), "fileEvidence.item.nodeKey");
            requireNonBlank(evidence, "fileId");
            requireHash(evidence.get("sha256"), "fileEvidence.item.sha256");
            requireHash(evidence.get("sourceSnapshotHash"), "fileEvidence.item.sourceSnapshotHash");
            requireEquals(sourceSnapshotHash, evidence.get("sourceSnapshotHash"),
                    "fileEvidence.item.sourceSnapshotHash");
            requireNonBlank(evidence, "source");
            requireEquals("COMPLETED", evidence.get("completionStatus"),
                    "fileEvidence.item.completionStatus");
        });
        requireNonBlank(snapshot, "sterilizationBatchNo");
        requireHash(snapshot.get("reportSnapshotHash"), "reportSnapshotHash");
        Map<?, ?> nodeStatuses = map(snapshot.get("nodeStatuses"), "nodeStatuses");
        if (!Objects.equals(REQUIRED_NODE_TYPES, nodeStatuses.keySet())) {
            throw new IllegalArgumentException("nodeStatuses must use only the four real release report node types");
        }
        if (nodeStatuses.values().stream().anyMatch(value -> !Objects.equals("COMPLETED", value))) {
            throw new IllegalArgumentException("all four real release report nodes must be completed");
        }
        if (!Boolean.TRUE.equals(snapshot.get("dossierReadyForRelease"))) {
            throw new IllegalArgumentException("successful Stage4 output must be release-ready");
        }
        if (snapshot.get("finalReleaseRecordId") != null) {
            throw new IllegalArgumentException("Stage4 must not create a final release record");
        }
        if (!(snapshot.get("blockers") instanceof List<?> blockers) || !blockers.isEmpty()) {
            throw new IllegalArgumentException("successful Stage4 output must have no blockers");
        }
    }

    private static void require(Map<?, ?> map, String key) {
        if (!map.containsKey(key) || map.get(key) == null) {
            throw new IllegalArgumentException("missing required field: " + key);
        }
    }

    private static void requireNonBlank(Map<?, ?> map, String key) {
        require(map, key);
        if (String.valueOf(map.get(key)).isBlank()) {
            throw new IllegalArgumentException("blank required field: " + key);
        }
    }

    private static void requireNonBlankValue(Object value, String field) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(field + " contains a blank value");
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

    private static Map<?, ?> exactNodeMap(Object value, String field) {
        Map<?, ?> result = map(value, field);
        if (!Objects.equals(REQUIRED_NODE_TYPES, result.keySet())) {
            throw new IllegalArgumentException(field + " must use only the four real release report node types");
        }
        return result;
    }

    private static void requireHash(Object value, String field) {
        if (value == null || !SHA256.matcher(String.valueOf(value)).matches()) {
            throw new IllegalArgumentException(field + " must be a lowercase SHA-256 hash");
        }
    }
}
