package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesStage4DossierUploadSimulationContractValidatorTest {

    @Test
    void acceptsLossFreeStage2Point5EquivalentInput() {
        assertDoesNotThrow(() -> MesStage4DossierUploadSimulationContractValidator.validateInput(input()));
    }

    @Test
    void rejectsLegacyAggregatedNodeKey() {
        Map<String, Object> input = input();
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(Map.of("nodeType", "INCOMING_INSPECTION_FILE"));
        nodes.add(Map.of("nodeType", "STERILIZATION_REPORT"));
        nodes.add(Map.of("nodeType", "FINISHED_PRODUCT_INSPECTION_REPORT"));
        nodes.add(Map.of("nodeType", "FINISHED_PRODUCT_INSPECTION_RECORD"));
        input.put("specialNodeUploadStatus", Map.of("overallStatus", "PENDING_UPLOAD",
                "requiredNodes", nodes));
        assertThrows(IllegalArgumentException.class,
                () -> MesStage4DossierUploadSimulationContractValidator.validateInput(input));
    }

    @Test
    void rejectsMissingLossLinksWhenLossIsRequired() {
        Map<String, Object> input = input();
        input.put("hasLoss", true);
        input.put("lossReportRequirement", Map.of("required", true, "status", "COMPLETED"));
        assertThrows(IllegalArgumentException.class,
                () -> MesStage4DossierUploadSimulationContractValidator.validateInput(input));
    }

    @Test
    void acceptsFourNodeDossierOutputWithoutFinalReleaseRecord() {
        String sourceSnapshotHash = hash('7');
        Map<String, Object> hashes = Map.of(
                "incomingInspectionAttachmentHash", hash('1'),
                "sterilizationAttachmentHash", hash('2'),
                "finishedProductInspectionAttachmentHashes", List.of(hash('3'), hash('4')));
        Map<String, Object> nodeStatuses = new LinkedHashMap<>();
        for (String node : MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES) {
            nodeStatuses.put(node, "COMPLETED");
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("contractName", "batchExecutionDossierSnapshot");
        output.put("contractVersion", "stage4.v1");
        output.put("batchExecutionId", "1");
        output.put("sourceSnapshotHash", sourceSnapshotHash);
        output.put("incomingInspectionAttachmentId", "2");
        output.put("sterilizationAttachmentId", "3");
        output.put("finishedProductInspectionAttachmentIds", List.of("4", "5"));
        output.put("hashes", hashes);
        Map<String, Object> fileIds = new LinkedHashMap<>();
        Map<String, Object> fileNames = new LinkedHashMap<>();
        Map<String, Object> storagePaths = new LinkedHashMap<>();
        Map<String, Object> attachmentAudit = new LinkedHashMap<>();
        Map<String, Object> fileEvidence = new LinkedHashMap<>();
        for (String node : MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES) {
            fileIds.put(node, "file-" + node);
            fileNames.put(node, node + ".pdf");
            storagePaths.put(node, "stage4/" + node + ".pdf");
            attachmentAudit.put(node, Map.of("verificationStatus", "VALID"));
            fileEvidence.put(node, Map.of(
                    "nodeKey", node,
                    "fileId", "file-" + node,
                    "sha256", hash('6'),
                    "sourceSnapshotHash", sourceSnapshotHash,
                    "source", "STAGE4_INDEPENDENT_FIXTURE_FORMAL_ATTACHMENT_UPLOAD",
                    "completionStatus", "COMPLETED"));
        }
        output.put("fileId", fileIds);
        output.put("fileName", fileNames);
        output.put("storagePath", storagePaths);
        output.put("attachmentAudit", attachmentAudit);
        output.put("fileEvidence", fileEvidence);
        output.put("sterilizationBatchNo", "STE-STAGE4-TEST-1");
        output.put("reportSnapshotHash", hash('5'));
        output.put("nodeStatuses", nodeStatuses);
        output.put("dossierReadyForRelease", true);
        output.put("finalReleaseRecordId", null);
        output.put("blockers", List.of());
        assertDoesNotThrow(() -> MesStage4DossierUploadSimulationContractValidator.validateOutput(output));
    }

    @Test
    void rejectsFileEvidenceWithoutSourceOrCompletion() {
        Map<String, Object> output = validOutput();
        Map<String, Object> evidence = new LinkedHashMap<>((Map<String, Object>)
                ((Map<?, ?>) output.get("fileEvidence")).get("INCOMING_INSPECTION_REPORT"));
        evidence.put("source", "");
        evidence.put("completionStatus", "PENDING_UPLOAD");
        ((Map<String, Object>) output.get("fileEvidence")).put("INCOMING_INSPECTION_REPORT", evidence);
        assertThrows(IllegalArgumentException.class,
                () -> MesStage4DossierUploadSimulationContractValidator.validateOutput(output));
    }

    @Test
    void rejectsOutputWithoutFormalSourceSnapshot() {
        Map<String, Object> output = validOutput();
        output.remove("sourceSnapshotHash");
        output.put("routeBindingSnapshotHash", hash('8'));
        assertThrows(IllegalArgumentException.class,
                () -> MesStage4DossierUploadSimulationContractValidator.validateOutput(output));
    }

    @Test
    void rejectsFileEvidenceWithoutFormalSourceSnapshot() {
        Map<String, Object> output = validOutput();
        Map<String, Object> evidence = new LinkedHashMap<>((Map<String, Object>)
                ((Map<?, ?>) output.get("fileEvidence")).get("INCOMING_INSPECTION_REPORT"));
        evidence.remove("sourceSnapshotHash");
        ((Map<String, Object>) output.get("fileEvidence")).put("INCOMING_INSPECTION_REPORT", evidence);
        assertThrows(IllegalArgumentException.class,
                () -> MesStage4DossierUploadSimulationContractValidator.validateOutput(output));
    }

    private Map<String, Object> validOutput() {
        String sourceSnapshotHash = hash('7');
        Map<String, Object> hashes = Map.of(
                "incomingInspectionAttachmentHash", hash('1'),
                "sterilizationAttachmentHash", hash('2'),
                "finishedProductInspectionAttachmentHashes", List.of(hash('3'), hash('4')));
        Map<String, Object> nodeStatuses = new LinkedHashMap<>();
        Map<String, Object> fileIds = new LinkedHashMap<>();
        Map<String, Object> fileNames = new LinkedHashMap<>();
        Map<String, Object> storagePaths = new LinkedHashMap<>();
        Map<String, Object> attachmentAudit = new LinkedHashMap<>();
        Map<String, Object> fileEvidence = new LinkedHashMap<>();
        for (String node : MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES) {
            nodeStatuses.put(node, "COMPLETED");
            fileIds.put(node, "file-" + node);
            fileNames.put(node, node + ".pdf");
            storagePaths.put(node, "stage4/" + node + ".pdf");
            attachmentAudit.put(node, Map.of("verificationStatus", "VALID"));
            fileEvidence.put(node, Map.of(
                    "nodeKey", node,
                    "fileId", "file-" + node,
                    "sha256", hash('6'),
                    "sourceSnapshotHash", sourceSnapshotHash,
                    "source", "STAGE4_INDEPENDENT_FIXTURE_FORMAL_ATTACHMENT_UPLOAD",
                    "completionStatus", "COMPLETED"));
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("contractName", "batchExecutionDossierSnapshot");
        output.put("contractVersion", "stage4.v1");
        output.put("batchExecutionId", "1");
        output.put("sourceSnapshotHash", sourceSnapshotHash);
        output.put("incomingInspectionAttachmentId", "2");
        output.put("sterilizationAttachmentId", "3");
        output.put("finishedProductInspectionAttachmentIds", List.of("4", "5"));
        output.put("hashes", hashes);
        output.put("fileId", fileIds);
        output.put("fileName", fileNames);
        output.put("storagePath", storagePaths);
        output.put("attachmentAudit", attachmentAudit);
        output.put("fileEvidence", fileEvidence);
        output.put("sterilizationBatchNo", "STE-STAGE4-TEST-1");
        output.put("reportSnapshotHash", hash('5'));
        output.put("nodeStatuses", nodeStatuses);
        output.put("dossierReadyForRelease", true);
        output.put("finalReleaseRecordId", null);
        output.put("blockers", List.of());
        return output;
    }

    private Map<String, Object> input() {
        List<Map<String, Object>> nodes = MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES.stream()
                .map(node -> {
                    Map<String, Object> nodeSnapshot = new LinkedHashMap<>();
                    nodeSnapshot.put("nodeType", node);
                    nodeSnapshot.put("status", "PENDING_UPLOAD");
                    return nodeSnapshot;
                })
                .toList();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("schemaVersion", "stage3.batchExecutionSnapshot.v1");
        input.put("simulationRunId", "STAGE4-TEST-1");
        input.put("sourceInputContract", "backfillResultSnapshot.v1");
        for (String field : List.of("batchExecutionId", "batchExecutionCode", "batchCode", "activeContextKey",
                "activeOrderId", "activeOrderCode", "workOrderId", "workOrderCode", "erpWorkOrderNo",
                "routeId", "routeVersionId", "routeVersionNo")) {
            input.put(field, field);
        }
        input.put("materialIssueSource", Map.of());
        input.put("batchRecordLinks", List.of());
        input.put("processInspectionLinks", List.of());
        input.put("hasLoss", false);
        input.put("lossReportRequirement", Map.of("required", false, "status", "NOT_REQUIRED"));
        input.put("optionalLossReportLinks", List.of());
        input.put("specialNodeUploadStatus", Map.of("overallStatus", "PENDING_UPLOAD", "requiredNodes", nodes));
        input.put("sourceHash", Map.of());
        input.put("status", "DOSSIER_UPLOAD_PENDING");
        input.put("blockers", List.of());
        return input;
    }

    private static String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
