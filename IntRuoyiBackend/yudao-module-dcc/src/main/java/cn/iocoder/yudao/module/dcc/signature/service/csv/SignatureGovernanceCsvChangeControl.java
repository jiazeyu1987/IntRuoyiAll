package cn.iocoder.yudao.module.dcc.signature.service.csv;

public record SignatureGovernanceCsvChangeControl(String changeControlId,
                                                  SignatureGovernanceCsvMaterialStatus status,
                                                  String evidenceRef) {

    public SignatureGovernanceCsvChangeControl {
        changeControlId = trimToNull(changeControlId);
        evidenceRef = trimToNull(evidenceRef);
    }

    boolean isApproved() {
        return !isBlank(changeControlId) && SignatureGovernanceCsvMaterialStatus.APPROVED.equals(status)
                && !isBlank(evidenceRef);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
