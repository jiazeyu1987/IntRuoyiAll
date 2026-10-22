package cn.iocoder.yudao.module.signature.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ElectronicSignatureVerificationDTO(
        Long signatureId,
        String verificationStatus,
        String storedContentHash,
        String calculatedContentHash,
        String storedEvidenceHash,
        String calculatedEvidenceHash,
        String algorithm,
        String keyVersion,
        String verificationScope,
        String digestStatus,
        String timeConsistencyStatus,
        String timeSource,
        String timeTrustStatus,
        String auditBindingStatus,
        List<String> reasonCodes,
        LocalDateTime verifiedAt,
        String evidenceFormatVersion
) {
    public ElectronicSignatureVerificationDTO {
        reasonCodes = List.copyOf(reasonCodes);
    }
}
