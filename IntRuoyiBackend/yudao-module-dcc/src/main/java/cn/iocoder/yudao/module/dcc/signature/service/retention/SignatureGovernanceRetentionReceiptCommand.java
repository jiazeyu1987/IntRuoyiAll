package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.time.Instant;

public record SignatureGovernanceRetentionReceiptCommand(
        String sourceType,
        Long sourceId,
        String objectKey,
        String versionId,
        String retentionMode,
        Instant retainUntil,
        String sha256,
        String evidenceHash,
        String archiveSha256,
        String signatureHash,
        String auditEventId) {
}
