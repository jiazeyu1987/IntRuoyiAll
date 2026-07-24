package cn.iocoder.yudao.module.dcc.signature.service.retention;

public record SignatureGovernanceRecoverySample(
        SignatureGovernanceRecoverySampleType sampleType,
        String objectKey,
        String versionId,
        String expectedSha256,
        String restoredSha256,
        String expectedDomainHash,
        String restoredDomainHash) {
}
