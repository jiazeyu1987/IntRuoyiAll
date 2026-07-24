package cn.iocoder.yudao.module.dcc.signature.service.retention;

public record SignatureGovernanceRetentionBucketState(
        boolean bucketExists,
        boolean versioningEnabled,
        boolean objectLockEnabled,
        boolean defaultRetentionEnabled,
        String defaultRetentionMode,
        boolean permissionsReadable) {
}
