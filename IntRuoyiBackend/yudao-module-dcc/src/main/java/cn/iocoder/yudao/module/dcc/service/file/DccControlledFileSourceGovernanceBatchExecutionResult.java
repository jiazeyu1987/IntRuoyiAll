package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileSourceGovernanceBatchExecutionResult(
        String taskKey,
        String batchStatus,
        int processedCount,
        int completedCount,
        int blockedCount,
        int failedCount,
        int remainingCount) {
}
