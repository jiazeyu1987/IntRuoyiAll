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
        assertTrue(source.contains("simulateActiveOrderCompletion(validated.getActorUserId(), activeOrder.getId(),")
                        && source.contains("\"2.5\", validated.getSimulationRunId()"));
        assertTrue(source.contains("setCompletionBackfillReceipt(receipt)"));
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
}
