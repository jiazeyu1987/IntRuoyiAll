package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                            .setExpectedSourceVersion(command.getExpectedSourceVersion())
                            .setExpectedSourceCredentialId(command.getExpectedSourceCredentialId())
                            .setExpectedSourceCredentialHash(command.getExpectedSourceCredentialHash());
                    return new MesProEdhrBatchTraceTxCResult().setStatus("TRACE_MAPPING_BLOCKED");
                });

        service.handle(new MesProEdhrBatchProvisionedEvent()
                .setTenantId(7L)
                .setBatchExecutionId(101L)
                .setEventId("flow6-open-101")
                .setIdempotencyKey("flow6-open-101:v1")
                .setExpectedSourceSnapshotHash("snapshot-v1")
                .setExpectedSourceBundleHash("bundle-v1")
                .setExpectedCompletionBackfillReceiptHash("receipt-v1")
                .setExpectedSourceVersion("3")
                .setExpectedSourceCredentialId("credential-101")
                .setExpectedSourceCredentialHash("credential-hash-v1"));

        assertEquals(101L, captured.getBatchExecutionId());
        assertEquals("flow6-open-101", captured.getEventId());
        assertEquals("snapshot-v1", captured.getExpectedSourceSnapshotHash());
        assertEquals("bundle-v1", captured.getExpectedSourceBundleHash());
        assertEquals("receipt-v1", captured.getExpectedCompletionBackfillReceiptHash());
        assertEquals("3", captured.getExpectedSourceVersion());
        assertEquals("credential-101", captured.getExpectedSourceCredentialId());
        assertEquals("credential-hash-v1", captured.getExpectedSourceCredentialHash());
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
}
