package cn.iocoder.yudao.module.signature.api.dto;

import java.time.LocalDateTime;

public record ElectronicSignatureResult(
        Long signatureId,
        String verificationStatus,
        LocalDateTime signedAt,
        String timeEvidenceId,
        String subjectVersion,
        String contentHash,
        String evidenceHash,
        String algorithm,
        String keyVersion) {
}
