package cn.iocoder.yudao.module.dcc.signature.service.retention;

public record SignatureGovernanceRetentionPrecheckCommand(
        String endpoint,
        String bucketName,
        boolean objectLockEnabled,
        boolean versioningEnabled,
        boolean defaultRetentionEnabled,
        String retentionMode,
        boolean permissionsVerified,
        Long ownerUserId,
        Long sampleDccSignatureId,
        Long sampleEdhrArchiveId) {
}
