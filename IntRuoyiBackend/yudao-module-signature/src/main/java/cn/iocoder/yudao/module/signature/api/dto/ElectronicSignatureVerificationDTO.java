package cn.iocoder.yudao.module.signature.api.dto;

public record ElectronicSignatureVerificationDTO(
        Long signatureId,
        String verificationStatus,
        String storedContentHash,
        String calculatedContentHash,
        String storedEvidenceHash,
        String calculatedEvidenceHash,
        String algorithm,
        String keyVersion
) {
}
