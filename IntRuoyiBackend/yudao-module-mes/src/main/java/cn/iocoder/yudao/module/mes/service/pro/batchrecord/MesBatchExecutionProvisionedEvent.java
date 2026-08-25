package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public record MesBatchExecutionProvisionedEvent(
        Long tenantId,
        Long batchExecutionId,
        String idempotencyKey,
        String sourceSnapshotHash,
        String sourceBundleHash,
        String completionBackfillReceiptHash,
        String sourceVersion,
        Long capturedBy) {
}
