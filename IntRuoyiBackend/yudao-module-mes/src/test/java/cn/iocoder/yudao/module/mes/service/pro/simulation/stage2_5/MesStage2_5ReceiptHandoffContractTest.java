package cn.iocoder.yudao.module.mes.service.pro.simulation.stage2_5;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesStage2_5ReceiptHandoffContractTest {

    private static final Path IMPLEMENTATION = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage2_5/"
                    + "MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java");

    @Test
    void stage2_5DoesNotInvokeReleaseDossierWriters() throws Exception {
        String source = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);

        assertFalse(source.contains("MesPqcReleaseDossierPort"));
        assertFalse(source.contains("MesPqcReleaseDossierPlan"));
        assertFalse(source.contains("MesPqcReleaseDossierWriteResult"));
        assertFalse(source.contains("dossierPort"));
        assertTrue(source.contains("completionReceiptMapper"));
        assertTrue(source.contains("buildBackfillReceipt"));
        assertTrue(source.contains("MesProcessPoolActiveOrderDO activeOrder = template"));
        assertTrue(source.contains("STAGE2_5_STAGE1_SOURCE_REQUIRED"));
        assertFalse(source.contains("simulateActiveOrderCompletion(validated.getActorUserId(), activeOrder.getId(),"));
        assertTrue(source.contains("activeOrderCompletionService.complete(validated.getActorUserId(),"));
        assertFalse(source.contains("MesProcessPoolActiveOrderDO activeOrder = createFixture"));
        assertFalse(source.contains("setCompletionBackfillReceipt(receipt)"));
        assertTrue(source.contains("setSourceContextHash(receipt.getSourceSnapshotHash())"));
        assertFalse(source.contains("setSourceContextHash(receipt.getSourceContextHash())"));
        assertTrue(source.contains("backfillReceipt.getBatchRecordId()"));
        assertTrue(source.contains("backfillReceipt.getProcessInspectionId()"));
    }

    @Test
    void erpSourceFidMustBeLengthBoundedAndSeparateFromAuditMarker() throws Exception {
        String source = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);

        assertTrue(source.contains("setSourceFid(sourceFid(runId, actorUserId))"));
        assertTrue(source.contains("setSourceFid(sourceEntryFid(runId, actorUserId, row.getId()))"));
        assertTrue(source.contains("private String sourceFid(String runId, Long actorUserId)"));
        assertTrue(source.contains("private String sourceEntryFid(String runId, Long actorUserId, Long sourceItemId)"));
        assertTrue(source.contains("\"S25-\" + DigestUtil.sha256Hex(runId + \"|\" + actorUserId).substring(0, 60)"));
        assertTrue(source.contains("\"S25E-\" + DigestUtil.sha256Hex(runId + \"|\" + actorUserId + \"|\" + sourceItemId).substring(0, 59)"));
        assertTrue(source.contains(".setId(IdUtil.getSnowflake().nextId())"));
        assertTrue(source.contains("MesProcessPoolActiveOrderPickListBindingItemDO.builder()")
                && source.contains(".id(IdUtil.getSnowflake().nextId())"));
        assertFalse(source.contains("setSourceFid(marker(runId, actorUserId)"));
    }

    @Test
    void stage1SourceSnapshotHashMustBeCanonicalSha256() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("DigestUtil.sha256Hex(JsonUtils.toJsonString(value))"));
        assertFalse(source.contains("Integer.toHexString(JsonUtils.toJsonString(value).hashCode())"));
    }

    @Test
    void stage1MustMaterializeFormalProductIssueForStage25() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("createFormalProductIssue"));
        assertTrue(source.contains("productIssueMapper.selectListByWorkOrderIdForUpdate"));
        assertTrue(source.contains("productIssueLineMapper.deleteByIssueId"));
        assertTrue(source.contains("productIssueDetailMapper.deleteByIssueId"));
        assertTrue(source.contains("MesWmProductIssueStatusEnum.FINISHED"));
        assertTrue(source.contains("batchMapper.insert(batch)"));
        assertTrue(source.contains("materialStockMapper.insert(stock)"));
    }

    @Test
    void downstreamStagesMustCarryTheExistingBatchIdentity() throws Exception {
        String stage4 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage4/"
                        + "MesStage4DossierUploadSimulationServiceImpl.java"), StandardCharsets.UTF_8);
        String stage5 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
                        + "MesStage5FinalReleaseSimulationServiceImpl.java"), StandardCharsets.UTF_8);
        assertTrue(stage4.contains("requireStage2_5Batch(command)"));
        assertFalse(stage4.contains("MesProEdhrBatchExecutionDO batch = createFixture"));
        assertTrue(stage5.contains("loadStage4Fixture(actorUserId, batchExecutionId, stage4RunId)"));
        assertFalse(stage5.contains("Fixture fixture = createFixture(actorUserId, runId)"));
    }

    @Test
    void cleanupMustBeScopedToTheCurrentSimulationActor() throws Exception {
        String stage1 = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
                        + "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"), StandardCharsets.UTF_8);
        String stage25 = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);
        assertTrue(stage1.contains("like(MesProWorkOrderDO::getRemark, \"][actorUserId=\" + actorUserId + \"]\")"));
        assertTrue(stage25.contains("cleanupOwnedBatches(validated.getActorUserId())"));
        assertTrue(stage25.contains("like(MesProEdhrBatchExecutionDO::getRemark, \"][actorUserId=\" + actorUserId + \"]\")"));
    }

    @Test
    void cleanupMustSkipNonCanonicalBatchMarkers() throws Exception {
        String stage25 = Files.readString(IMPLEMENTATION, StandardCharsets.UTF_8);

        assertTrue(stage25.contains("String runId = tryRunIdFromMarker(batch.getRemark(), actorUserId)"));
        assertTrue(stage25.contains("if (runId == null)"));
        assertTrue(stage25.contains("continue;"));
        assertFalse(stage25.contains("runIdFromMarker(batch.getRemark(), actorUserId);"));
    }
}
