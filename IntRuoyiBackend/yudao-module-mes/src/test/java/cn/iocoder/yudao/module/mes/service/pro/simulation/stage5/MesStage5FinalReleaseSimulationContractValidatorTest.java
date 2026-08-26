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
    void cleanupMustPhysicallyRemoveOwnedErpPickListBeforeRecreate() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("pickListMapper.hardDeleteById(pickList.getId())"),
                "Stage5 cleanup must physically remove the validated owned ERP pick list before recreation");
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
        releaseSnapshot.put("threeFileEvidence", List.of(
                Map.of("nodeType", "INCOMING_INSPECTION_REPORT", "sha256", List.of(hash('1'))),
                Map.of("nodeType", "STERILIZATION_REPORT", "sha256", List.of(hash('2'))),
                Map.of("nodeType", "FINISHED_PRODUCT_INSPECTION", "sha256", List.of(hash('3'), hash('4')))));
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
