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
        assertTrue(source.contains("simulateActiveOrderCompletion(validated.getActorUserId(), activeOrder.getId())"));
        assertTrue(source.contains("setCompletionBackfillReceipt(receipt)"));
        assertTrue(source.contains("backfillReceipt.getBatchRecordId()"));
        assertTrue(source.contains("backfillReceipt.getProcessInspectionId()"));
    }
}
