package cn.iocoder.yudao.module.dcc.signature.service.csv;

import java.util.List;

public record SignatureGovernanceCsvMaterial(SignatureGovernanceCsvMaterialType type,
                                             String documentId,
                                             String version,
                                             SignatureGovernanceCsvMaterialStatus status,
                                             String owner,
                                             List<String> reviewers,
                                             List<String> approvers,
                                             String sourceEvidence,
                                             String changeControlId,
                                             String signatureMeaningCode) {

    public SignatureGovernanceCsvMaterial {
        if (type == null) {
            throw new IllegalArgumentException("CSV quality material type is required");
        }
        documentId = trimToNull(documentId);
        version = trimToNull(version);
        owner = trimToNull(owner);
        reviewers = copyTrimmed(reviewers);
        approvers = copyTrimmed(approvers);
        sourceEvidence = trimToNull(sourceEvidence);
        changeControlId = trimToNull(changeControlId);
        signatureMeaningCode = trimToNull(signatureMeaningCode);
    }

    boolean isComplete() {
        return !isBlank(documentId) && !isBlank(version) && status != null && !isBlank(owner)
                && !reviewers.isEmpty() && !approvers.isEmpty() && !isBlank(sourceEvidence)
                && !isBlank(changeControlId) && !isBlank(signatureMeaningCode);
    }

    boolean isApproved() {
        return isComplete() && SignatureGovernanceCsvMaterialStatus.APPROVED.equals(status);
    }

    private static List<String> copyTrimmed(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(SignatureGovernanceCsvMaterial::trimToNull)
                .filter(value -> value != null)
                .toList();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
