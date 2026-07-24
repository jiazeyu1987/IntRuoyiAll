package cn.iocoder.yudao.module.dcc.signature.service.csv;

public record SignatureGovernanceCsvQaApproval(String qualityApprovalRef,
                                               String approverUserId,
                                               SignatureGovernanceCsvMaterialStatus status,
                                               String signatureEvidenceRef) {

    public SignatureGovernanceCsvQaApproval {
        qualityApprovalRef = trimToNull(qualityApprovalRef);
        approverUserId = trimToNull(approverUserId);
        signatureEvidenceRef = trimToNull(signatureEvidenceRef);
    }

    boolean isApproved() {
        return !isBlank(qualityApprovalRef) && !isBlank(approverUserId)
                && SignatureGovernanceCsvMaterialStatus.APPROVED.equals(status)
                && !isBlank(signatureEvidenceRef);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
