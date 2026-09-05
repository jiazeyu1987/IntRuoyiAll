package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileSourceGovernancePreparationResult(
        String taskKey,
        String batchStatus,
        String ruleVersion,
        String schemaVersion,
        String manifestSha256,
        String requestSha256,
        Long snapshotMaxControlledFileId,
        Long startAfterControlledFileId,
        Long lastControlledFileId,
        int totalCount,
        int readyCount,
        int blockedCount) {
}
