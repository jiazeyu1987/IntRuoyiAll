package cn.iocoder.yudao.module.mes.service.pro.simulation.stage5;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesStage5FinalReleaseSimulationContractValidatorTest {

    @Test
    void acceptsExactStage4DossierAndManagerContext() {
        assertDoesNotThrow(() -> MesStage5FinalReleaseSimulationContractValidator
                .validateInputFixture(dossier(), managerContext()));
    }

    @Test
    void rejectsStage4OutputVersionAlias() {
        Map<String, Object> snapshot = dossier();
        snapshot.put("contractVersion", "stage4-output-v1");
        assertThrows(IllegalArgumentException.class, () ->
                MesStage5FinalReleaseSimulationContractValidator.validateDossierSnapshot(snapshot));
    }

    @Test
    void rejectsLegacyAggregatedNodeKey() {
        Map<String, Object> snapshot = dossier();
        snapshot.put("nodeStatuses", Map.of(
                "INCOMING_INSPECTION_FILE", "COMPLETED",
                "STERILIZATION_REPORT", "COMPLETED",
                "FINISHED_PRODUCT_INSPECTION_REPORT", "COMPLETED",
                "FINISHED_PRODUCT_INSPECTION_RECORD", "COMPLETED"));
        assertThrows(IllegalArgumentException.class, () ->
                MesStage5FinalReleaseSimulationContractValidator.validateDossierSnapshot(snapshot));
    }

    @Test
    void rejectsUnfrozenManagerCandidate() {
        Map<String, Object> context = managerContext();
        context.put("candidateFrozen", false);
        assertThrows(IllegalArgumentException.class, () ->
                MesStage5FinalReleaseSimulationContractValidator.validateInputFixture(dossier(), context));
    }

    @Test
    void rejectsPendingStage5SetupOutputAsFinalRelease() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("simulationRunId", "STAGE5-TEST-1");
        output.put("batchExecutionId", "101");
        output.put("releaseApplicationId", "102");
        output.put("releaseTransactionId", "103");
        output.put("managerReleaseWorkTaskId", "104");
        output.put("releaseStatus", "PENDING_APPROVAL");
        output.put("applicationStatus", "MANAGER_RELEASE_PENDING");
        output.put("sourceDossierHash", hash('9'));
        output.put("precheckResult", Map.of("passed", true));
        output.put("finalReleaseReady", false);
        output.put("blockers", List.of(MesStage5FinalReleaseSimulationContractValidator.UPSTREAM_CONTEXT_BLOCKER));
        output.put("batchExecutionDossierSnapshot", dossier());
        output.put("managerReleaseContext", managerContext());
        assertThrows(IllegalArgumentException.class, () ->
                MesStage5FinalReleaseSimulationContractValidator.validateOutput(output));
    }

    @Test
    void acceptsReleasedOutputWithAuthoritativeReleaseSnapshot() {
        Map<String, Object> output = releasedOutput();
        assertDoesNotThrow(() -> MesStage5FinalReleaseSimulationContractValidator.validateOutput(output));
    }

    @Test
    void releaseSnapshotMustRejectBlankReceiptAndNonPositiveSourceIds() {
        final Map<String, Object> releaseSnapshot = new LinkedHashMap<>(releasedOutput().get("releaseSnapshot") instanceof Map<?, ?> snapshot
                ? (Map<String, Object>) snapshot
                : Map.of());
        releaseSnapshot.put("releaseReceiptId", " ");
        assertThrows(IllegalArgumentException.class, () ->
                MesStage5FinalReleaseSimulationContractValidator.validateReleaseSnapshot(releaseSnapshot));

        final Map<String, Object> nonPositiveSourceIdSnapshot = new LinkedHashMap<>((Map<String, Object>) releasedOutput().get("releaseSnapshot"));
        final Map<String, Object> sourceChain = new LinkedHashMap<>((Map<String, Object>) nonPositiveSourceIdSnapshot.get("sourceChain"));
        sourceChain.put("backfillReceiptId", "0");
        nonPositiveSourceIdSnapshot.put("sourceChain", sourceChain);
        assertThrows(IllegalArgumentException.class, () ->
                MesStage5FinalReleaseSimulationContractValidator.validateReleaseSnapshot(nonPositiveSourceIdSnapshot));
    }

    @Test
    void releaseSnapshotMustExposeFourMaterialEvidenceContract() throws Exception {
        String service = stage5Source();
        String validator = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationContractValidator.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        String workTaskPage = Files.readString(Path.of(
                "../../IntRuoyiFronted/src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");

        assertTrue(service.contains("snapshot.put(\"fourMaterialEvidence\""),
                "Stage5 release snapshot must publish the four-material evidence field");
        assertTrue(!service.contains("snapshot.put(\"threeFileEvidence\""),
                "Stage5 release snapshot must not expose the obsolete three-file field");
        assertTrue(validator.contains("\"fourMaterialEvidence\""),
                "Stage5 validator must require the four-material evidence field");
        assertTrue(!validator.contains("threeFileEvidence"),
                "Stage5 validator must not accept the obsolete three-file field");
        assertTrue(workTaskPage.contains("snapshot.fourMaterialEvidence"),
                "manager release page must read fourMaterialEvidence");
        assertTrue(workTaskPage.contains("evidence.length !== 4"),
                "manager release page must require all four material categories");
    }

    @Test
    void fixtureMustPopulateRequiredErpPickListSyncTimes() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains(".setSourceModifyTime(now)\n                .setLastSyncTime(now)"),
                "Stage5 pick-list fixture must populate erp_kingdee_production_pick_list.last_sync_time");
        assertTrue(source.contains(".setProductionOrderLineNo(1)\n                .setSourceModifyTime(now)\n"
                        + "                .setLastSyncTime(now)"),
                "Stage5 pick-list line fixture must populate last_sync_time");
    }

    @Test
    void fixtureMustUseCompactErpPickListEntryId() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains(".setSourceEntryId(pickListLineSourceEntryId(runId))"),
                "Stage5 pick-list line fixture must keep source_entry_id within the ERP column length");
    }

    @Test
    void fixtureMustAssignRequiredPickListBindingIds() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("new MesProcessPoolActiveOrderPickListBindingDO()\n                .setId(IdUtil.getSnowflake().nextId())"),
                "Stage5 pick-list binding fixture must provide its non-auto-increment primary key");
        assertTrue(source.contains("bindingItem.setId(IdUtil.getSnowflake().nextId())"),
                "Stage5 pick-list binding item fixture must provide its non-auto-increment primary key");
    }

    @Test
    void fixtureMustFreezeSpecialReportAttachmentOwners() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("String routeSnapshotJson = buildStage5RouteSnapshot(actorUserId, runId, routeId);"),
                "Stage5 batch fixture must create its frozen special-report responsibility snapshot");
        assertTrue(source.contains("configSnapshots.put(\"batchRecordAttachmentOwners\", owners);"),
                "Stage5 frozen route snapshot must retain special-report attachment owners");
        assertTrue(source.contains("owner.put(\"candidateSourceIds\", List.of(actorUserId));"),
                "Stage5 special-report attachment owners must be explicitly frozen to the simulation actor");
    }

    @Test
    void fixtureMustSurfaceExactTraceValidationScope() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("validator.validate(traceCommand)"),
                "Stage5 fixture must run the formal trace validator before persistence");
        assertTrue(source.contains("validation.blockerScope()"),
                "Stage5 fixture must expose the exact rejected trace source scope");
    }

    @Test
    void fixtureMustUseCompactTraceOriginKey() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains(".setOriginKey(traceOriginKey(runId))"),
                "Stage5 trace origin key must fit the authoritative database column");
        assertTrue(source.contains("return \"STAGE5:\" + shortRunId(runId) + \":ACTIVE_ORDER_COMPLETION\";"),
                "Stage5 trace origin key must remain deterministic and Stage-owned");
    }

    @Test
    void releaseSnapshotMustExposeBackfillReceiptIdInSourceChain() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("\"backfillReceiptId\", String.valueOf(origin.getCompletionBackfillReceiptId())"),
                "Stage5 release snapshot sourceChain must use the formal backfillReceiptId field");
    }

    @Test
    void managerReleaseTaskMustUseCandidateRoleGroupSource() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/"
                        + "MesProductionReleaseManagerStageInitializerImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains(".setCandidateSourceType(\"ROLE_GROUP\")"),
                "Manager release task must use the candidate-pool ROLE_GROUP source type");
    }

    @Test
    void cleanupMustScopeBackfillsByOwnedActiveOrderAndTypes() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("item.getActiveOrderId()"),
                "Stage5 cleanup must prove every backfill belongs to the owned active order");
        assertTrue(source.contains("MesProcessPoolActiveOrderCompletionBackfillDO::getBackfillType"),
                "Stage5 cleanup must validate the exact formal backfill types");
        assertTrue(source.contains("TYPE_BATCH_RECORD")
                        && source.contains("TYPE_PROCESS_INSPECTION")
                        && source.contains("TYPE_LOSS_REPORT"),
                "Stage5 cleanup must require batch-record, process-inspection and loss-report backfills");
    }

    @Test
    void cleanupMustRetainAppendOnlyTraceChain() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("immutable audit evidence"),
                "Stage5 cleanup must document retention of immutable trace audit evidence");
        assertTrue(!source.contains("traceLinks.forEach(item -> traceLinkMapper.deleteById(item.getId()))"),
                "Stage5 cleanup must not delete append-only trace links");
        assertTrue(!source.contains("traceManifests.forEach(item -> traceManifestMapper.deleteById(item.getId()))"),
                "Stage5 cleanup must not delete append-only trace manifests");
        assertTrue(!source.contains("originMapper.deleteById(origins.get(0).getId())"),
                "Stage5 cleanup must not delete append-only trace origins");
    }

    @Test
    void cleanupMustRetainUpstreamErpPickListAndSources() throws Exception {
        String cleanupMethod = methodBlock(stage5Source(), "private String cleanupPreviousSimulation");

        assertTrue(!cleanupMethod.contains("pickListMapper.hardDeleteById(pickList.getId())"),
                "Stage5 cleanup must retain the upstream ERP pick list");
        assertTrue(!cleanupMethod.contains("completionReceiptMapper.deleteById(completionReceipt.getId())"),
                "Stage5 cleanup must retain the upstream completion receipt");
    }

    @Test
    void stage5MustUseExactStage4RunEvidencePayloads() throws Exception {
        String source = stage5Source();
        assertTrue(source.contains("buildDossierSnapshot(fixture.batch(), runId, stage4RunId,\n"
                        + "                fixture.sourceSnapshotHash())"),
                "Stage5 dossier snapshot must be scoped to the exact Stage4 run id");
        assertTrue(source.contains("buildReportEvidences(fixture.batch().getId(), stage4RunId)"),
                "Stage5 manager report snapshot must be scoped to the exact Stage4 run id");

        String reportEvidenceMethod = methodBlock(source,
                "private List<MesProductionReleaseReportNodeEvidence> buildReportEvidences");
        assertTrue(reportEvidenceMethod.contains("MesProductionReleaseReportNodeEvidence.fromPayloadJson"),
                "Stage5 must consume the release-report evidence persisted by Stage4");
        assertTrue(source.contains("stage4ReasonText(stage4RunId)"),
                "Stage5 report evidence must match the Stage4 attachment marker");
        assertTrue(!reportEvidenceMethod.contains("new MesProductionReleaseReportNodeEvidence()"),
                "Stage5 must not synthesize new report evidence from latest attachments");
        assertTrue(!reportEvidenceMethod.contains("STE-STAGE5-"),
                "Stage5 must not replace the Stage4 sterilization batch evidence");

        String dossierMethod = methodBlock(source, "private Map<String, Object> buildDossierSnapshot");
        assertTrue(dossierMethod.contains("loadStage4Attachments(batch.getId(), stage4RunId)"),
                "Stage5 dossier snapshot must read only the exact Stage4 upload run");
        assertTrue(!dossierMethod.contains("STE-STAGE5-"),
                "Stage5 dossier snapshot must retain the Stage4 sterilization batch number");
    }

    @Test
    void stage5CleanupMustOnlyRemoveStage5OwnedReleaseRows() throws Exception {
        String cleanupMethod = methodBlock(stage5Source(), "private String cleanupPreviousSimulation");

        assertTrue(cleanupMethod.contains("marker(previousRunId)"),
                "Stage5 cleanup must use the caller supplied Stage5 run id");
        assertTrue(!cleanupMethod.contains("runIdFromMarker(previous.getRemark())"),
                "Stage5 cleanup must not derive its run id from the upstream Stage4 batch remark");
        assertTrue(!cleanupMethod.contains("attachmentMapper.deleteById"),
                "Stage5 cleanup must not delete Stage4 material attachments");
        assertTrue(!cleanupMethod.contains("batchTaskMapper.deleteById"),
                "Stage5 cleanup must not delete Stage4 material tasks");
        assertTrue(!cleanupMethod.contains("completionBackfillMapper.deleteById"),
                "Stage5 cleanup must not delete Flow4 completion backfill rows");
        assertTrue(!cleanupMethod.contains("completionReceiptMapper.deleteById"),
                "Stage5 cleanup must not delete Flow4 completion receipts");
        assertTrue(!cleanupMethod.contains("bindingMapper.deleteById"),
                "Stage5 cleanup must not delete active-order pick-list bindings");
        assertTrue(!cleanupMethod.contains("pickListMapper.hardDeleteById"),
                "Stage5 cleanup must not delete ERP pick lists");
        assertTrue(!cleanupMethod.contains("activeOrderMapper.deleteById"),
                "Stage5 cleanup must not delete the upstream active order");
        assertTrue(!cleanupMethod.contains("workOrderMapper.deleteById"),
                "Stage5 cleanup must not delete the upstream work order");
        assertTrue(!cleanupMethod.contains("batchExecutionMapper.deleteById"),
                "Stage5 cleanup must not delete the upstream Stage2.5 batch execution");
        assertTrue(cleanupMethod.contains("applicationMapper.deleteById(application.getId())"),
                "Stage5 cleanup should remove its own release application");
        assertTrue(cleanupMethod.contains("releaseTransactionMapper.deleteById(transaction.getId())"),
                "Stage5 cleanup should remove its own pending release transaction");
        assertTrue(cleanupMethod.contains("workTaskMapper.deleteById(managerTask.getId())"),
                "Stage5 cleanup should remove its own manager release work task");
    }

    @Test
    void stage5ReleaseSnapshotMustUseAuthoritativeUpstreamOrigin() throws Exception {
        String source = stage5Source();
        String releaseSnapshotMethod = methodBlock(source, "public Map<String, Object> getReleaseSnapshot");
        assertTrue(releaseSnapshotMethod.contains("buildReportEvidences(batch.getId(), stage4RunIdFromBatch(batch))"),
                "Stage5 release snapshot must use the latest Stage4 run material evidence");
        assertTrue(releaseSnapshotMethod.contains("requireSingleOrigin(batch.getId())"),
                "Stage5 release snapshot must read the existing Flow6/Flow7 origin on the batch");

        String originMethod = methodBlock(source, "private MesProEdhrBatchExecutionOriginDO requireSingleOrigin");
        assertTrue(!originMethod.contains("traceOriginKey(runId)"),
                "Stage5 release snapshot must not expect a Stage5 synthetic trace origin");
        assertTrue(originMethod.contains("origin.getCompletionBackfillReceiptId()"),
                "Stage5 release snapshot origin must expose the completion backfill receipt");
        assertTrue(originMethod.contains("origin.getPickListId()"),
                "Stage5 release snapshot origin must expose the pick-list source");
    }

    private static String stage5Source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static String methodBlock(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "missing method: " + signature);
        int next = source.indexOf("\n    private ", start + signature.length());
        if (next < 0) {
            next = source.indexOf("\n    public ", start + signature.length());
        }
        return next < 0 ? source.substring(start) : source.substring(start, next);
    }

    private static Map<String, Object> dossier() {
        Map<String, Object> hashes = Map.of(
                "incomingInspectionAttachmentHash", hash('1'),
                "sterilizationAttachmentHash", hash('2'),
                "finishedProductInspectionAttachmentHashes", List.of(hash('3'), hash('4')));
        Map<String, Object> nodes = new LinkedHashMap<>();
        for (String node : MesStage5FinalReleaseSimulationContractValidator.REQUIRED_NODE_TYPES) {
            nodes.put(node, "COMPLETED");
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractName", "batchExecutionDossierSnapshot");
        snapshot.put("contractVersion", "stage4.v1");
        snapshot.put("batchExecutionId", "101");
        snapshot.put("sourceSnapshotHash", hash('0'));
        snapshot.put("incomingInspectionAttachmentId", "201");
        snapshot.put("sterilizationAttachmentId", "202");
        snapshot.put("finishedProductInspectionAttachmentIds", List.of("203", "204"));
        snapshot.put("hashes", hashes);
        snapshot.put("nodeStatuses", nodes);
        snapshot.put("dossierReadyForRelease", true);
        snapshot.put("finalReleaseRecordId", null);
        snapshot.put("blockers", List.of());
        return snapshot;
    }

    private static Map<String, Object> managerContext() {
        return new LinkedHashMap<>(Map.of(
                "releaseApplicationId", "102",
                "releaseTransactionId", "103",
                "managerReleaseWorkTaskId", "104",
                "managerCandidateSnapshotHash", hash('5'),
                "reportSnapshotHash", hash('6'),
                "releaseStatus", "PENDING_APPROVAL",
                "applicationStatus", "MANAGER_RELEASE_PENDING",
                "transactionVersion", 1,
                "candidateFrozen", true));
    }

    private static Map<String, Object> releasedOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("simulationRunId", "STAGE5-TEST-RELEASED");
        output.put("batchExecutionId", "101");
        output.put("releaseApplicationId", "102");
        output.put("releaseTransactionId", "103");
        output.put("managerReleaseWorkTaskId", "104");
        output.put("releaseStatus", "RELEASED");
        output.put("applicationStatus", "RELEASED");
        output.put("sourceDossierHash", hash('9'));
        output.put("precheckResult", Map.of("passed", true, "finalReleaseReady", true));
        output.put("finalReleaseReady", true);
        output.put("blockers", List.of());
        output.put("batchExecutionDossierSnapshot", releasedDossier());
        Map<String, Object> context = managerContext();
        context.put("releaseStatus", "RELEASED");
        context.put("applicationStatus", "RELEASED");
        context.put("candidateFrozen", true);
        context.put("releaseDecisionId", "105");
        output.put("managerReleaseContext", context);
        Map<String, Object> releaseSnapshot = new LinkedHashMap<>();
        releaseSnapshot.put("contractName", "stage5.releaseSnapshot");
        releaseSnapshot.put("contractVersion", "stage5.releaseSnapshot.v1");
        releaseSnapshot.put("batchExecutionId", "101");
        releaseSnapshot.put("releaseReceiptId", "103");
        releaseSnapshot.put("releaseDecisionId", "105");
        releaseSnapshot.put("releasedAt", "2026-08-25T12:00:00");
        releaseSnapshot.put("releaseStatus", "RELEASED");
        releaseSnapshot.put("fourMaterialEvidence", List.of(
                Map.of("nodeType", "INCOMING_INSPECTION_REPORT", "attachmentIds", List.of("301"), "sha256", List.of(hash('1'))),
                Map.of("nodeType", "STERILIZATION_REPORT", "attachmentIds", List.of("302"), "sha256", List.of(hash('2'))),
                Map.of("nodeType", "FINISHED_PRODUCT_INSPECTION_REPORT", "attachmentIds", List.of("303"), "sha256", List.of(hash('3'))),
                Map.of("nodeType", "FINISHED_PRODUCT_INSPECTION_RECORD", "attachmentIds", List.of("304"), "sha256", List.of(hash('4')))));
        releaseSnapshot.put("sourceChain", Map.of(
                "productionSourceIds", List.of("201"),
                "pickListId", "202",
                "backfillReceiptId", "203"));
        releaseSnapshot.put("releaseApprovalWorkTaskId", "104");
        releaseSnapshot.put("reportSnapshotHash", hash('6'));
        releaseSnapshot.put("version", 2);
        output.put("releaseSnapshot", releaseSnapshot);
        return output;
    }

    private static Map<String, Object> releasedDossier() {
        return dossier();
    }

    private static String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
