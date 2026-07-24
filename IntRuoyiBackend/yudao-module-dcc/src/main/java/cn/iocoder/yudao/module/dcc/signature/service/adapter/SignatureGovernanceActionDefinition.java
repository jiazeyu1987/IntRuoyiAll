package cn.iocoder.yudao.module.dcc.signature.service.adapter;

public record SignatureGovernanceActionDefinition(String actionCode,
                                                   String meaningCode,
                                                   String sourceStageCode,
                                                   boolean authorizationRequired,
                                                   boolean passwordVerificationRequired,
                                                   String evidenceSchemaVersion) {

    public SignatureGovernanceActionDefinition {
        if (isBlank(actionCode) || isBlank(meaningCode) || isBlank(evidenceSchemaVersion)) {
            throw new IllegalArgumentException("Signature governance action requires action, meaning, and evidence schema");
        }
        actionCode = actionCode.trim();
        meaningCode = meaningCode.trim();
        sourceStageCode = trimToNull(sourceStageCode);
        evidenceSchemaVersion = evidenceSchemaVersion.trim();
    }

    public static SignatureGovernanceActionDefinition of(String actionCode,
                                                         String meaningCode,
                                                         String sourceStageCode,
                                                         boolean authorizationRequired,
                                                         boolean passwordVerificationRequired,
                                                         String evidenceSchemaVersion) {
        return new SignatureGovernanceActionDefinition(actionCode, meaningCode, sourceStageCode,
                authorizationRequired, passwordVerificationRequired, evidenceSchemaVersion);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
