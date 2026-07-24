package cn.iocoder.yudao.module.dcc.signature.service.csv;

public record SignatureGovernanceCsvTraceRelation(String requirementRef,
                                                  String designRef,
                                                  String testRef,
                                                  String evidenceRef,
                                                  String owner,
                                                  SignatureGovernanceCsvMaterialStatus status,
                                                  String blockerRef,
                                                  String qualityApprovalRef) {

    public SignatureGovernanceCsvTraceRelation {
        requirementRef = trimToNull(requirementRef);
        designRef = trimToNull(designRef);
        testRef = trimToNull(testRef);
        evidenceRef = trimToNull(evidenceRef);
        owner = trimToNull(owner);
        blockerRef = trimToNull(blockerRef);
        qualityApprovalRef = trimToNull(qualityApprovalRef);
    }

    boolean isComplete() {
        return !isBlank(requirementRef) && !isBlank(designRef) && !isBlank(testRef)
                && !isBlank(evidenceRef) && !isBlank(owner)
                && SignatureGovernanceCsvMaterialStatus.APPROVED.equals(status)
                && !isBlank(blockerRef) && !isBlank(qualityApprovalRef);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
