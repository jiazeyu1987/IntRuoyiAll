package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void flow7TraceMappingMustRunAfterP2ProvisionTransactionCommits() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                        + "MesProEdhrBatchExecutionServiceImpl.java"));

        assertFalse(source.contains("MesProEdhrBatchTraceTxCApplicationService batchTraceTxCApplicationService"),
                "P2不能把Flow7应用服务注入到批次创建事务中同步执行");
        assertFalse(source.contains("batchTraceTxCApplicationService.handle(event)"),
                "Flow7来源映射失败不能回滚P2已经完成的正式批记录回填");
        assertTrue(source.contains("applicationEventPublisher.publishEvent(event)"),
                "P2必须在正式批次事务边界发布Flow7事件，由提交后的监听器处理");
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
