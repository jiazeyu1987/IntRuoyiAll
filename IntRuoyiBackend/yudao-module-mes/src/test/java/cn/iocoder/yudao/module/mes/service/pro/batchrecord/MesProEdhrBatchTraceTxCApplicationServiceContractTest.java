package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchTraceTxCApplicationServiceContractTest {

    @Test
    void flow6ProvisionEventCarriesOnlyPersistedWitnessesIntoTxC() {
        MesProEdhrBatchTraceTxCCommand captured = new MesProEdhrBatchTraceTxCCommand();
        MesProEdhrBatchTraceTxCApplicationService service =
                new MesProEdhrBatchTraceTxCApplicationService(command -> {
                    captured.setBatchExecutionId(command.getBatchExecutionId())
                            .setEventId(command.getEventId())
                            .setIdempotencyKey(command.getIdempotencyKey())
                            .setExpectedSourceSnapshotHash(command.getExpectedSourceSnapshotHash())
                            .setExpectedSourceBundleHash(command.getExpectedSourceBundleHash())
                            .setExpectedCompletionBackfillReceiptHash(
                                    command.getExpectedCompletionBackfillReceiptHash())
                            .setExpectedSourceVersion(command.getExpectedSourceVersion());
                    return new MesProEdhrBatchTraceTxCResult().setStatus("TRACE_MAPPING_BLOCKED");
                });

        service.handle(new MesProEdhrBatchProvisionedEvent()
                .setTenantId(7L)
                .setBatchExecutionId(101L)
                .setProvisioningReceiptId(202L)
                .setEventId("flow6-open-101")
                .setIdempotencyKey("flow6-open-101:v1")
                .setExpectedSourceSnapshotHash("snapshot-v1")
                .setExpectedSourceBundleHash("bundle-v1")
                .setExpectedCompletionBackfillReceiptHash("receipt-v1")
                .setExpectedSourceVersion("3"));

        assertEquals(101L, captured.getBatchExecutionId());
        assertEquals("flow6-open-101", captured.getEventId());
        assertEquals("snapshot-v1", captured.getExpectedSourceSnapshotHash());
        assertEquals("bundle-v1", captured.getExpectedSourceBundleHash());
        assertEquals("receipt-v1", captured.getExpectedCompletionBackfillReceiptHash());
        assertEquals("3", captured.getExpectedSourceVersion());
    }

    @Test
    void flow6ProvisionEventRejectsMissingAuthoritativeTenantOrBatchIdentity() {
        MesProEdhrBatchTraceTxCApplicationService service =
                new MesProEdhrBatchTraceTxCApplicationService(command ->
                        new MesProEdhrBatchTraceTxCResult().setStatus("unexpected"));

        assertThrows(IllegalArgumentException.class, () -> service.handle(
                new MesProEdhrBatchProvisionedEvent().setBatchExecutionId(101L)
                        .setEventId("event").setIdempotencyKey("key")));
    }

    @Test
    void provisionEventIsConsumedOnlyAfterTheProvisionTransactionCommits() throws NoSuchMethodException {
        TransactionalEventListener listener = MesProEdhrBatchTraceTxCApplicationService.class
                .getDeclaredMethod("onBatchProvisioned", MesProEdhrBatchProvisionedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
    }

    @Test
    void producerRoutesAllActiveOrderAliasesThroughFlow4ReceiptBranch() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrBatchTraceTxCProducer.java"));

        assertTrue(source.contains("MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType(entryType)"));
    }

    @Test
    void producerUsesCentralTraceSourceHashValidationForEveryEvidenceItem() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrBatchTraceTxCProducer.java"));

        assertTrue(source.contains("MesProEdhrBatchTraceSourceHash.isValid(linkType, snapshotJson, snapshotHash)"),
                "Tx-C must use the shared trace source hash contract for receipt witness evidence");
        assertTrue(!source.contains("boolean externallyWitnessed"),
                "Tx-C must not keep a second hard-coded external witness allowlist");
    }
}
