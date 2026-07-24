package cn.iocoder.yudao.module.dcc.signature.service.adapter;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

public record SignatureGovernanceAdapterProjection(SignatureGovernanceModuleCode moduleCode,
                                                   String adapterCode,
                                                   String adapterVersion,
                                                   String evidenceSchemaVersion,
                                                   String policyVersion,
                                                   String actionCode,
                                                   String meaningCode,
                                                   String sourceRecordRef,
                                                   String projectionHash) {

    public SignatureGovernanceAdapterProjection {
        if (moduleCode == null || isBlank(adapterCode) || isBlank(adapterVersion)
                || isBlank(evidenceSchemaVersion) || isBlank(policyVersion)
                || isBlank(actionCode) || isBlank(meaningCode) || isBlank(sourceRecordRef)) {
            throw new IllegalArgumentException("Signature governance projection requires module, adapter, policy, action, and source");
        }
        adapterCode = adapterCode.trim();
        adapterVersion = adapterVersion.trim();
        evidenceSchemaVersion = evidenceSchemaVersion.trim();
        policyVersion = policyVersion.trim();
        actionCode = actionCode.trim();
        meaningCode = meaningCode.trim();
        sourceRecordRef = sourceRecordRef.trim();
        projectionHash = trimToNull(projectionHash);
    }

    public static SignatureGovernanceAdapterProjection of(SignatureGovernanceModuleCode moduleCode,
                                                          String adapterCode,
                                                          String adapterVersion,
                                                          String evidenceSchemaVersion,
                                                          String policyVersion,
                                                          String actionCode,
                                                          String meaningCode,
                                                          String sourceRecordRef,
                                                          String projectionHash) {
        return new SignatureGovernanceAdapterProjection(moduleCode, adapterCode, adapterVersion,
                evidenceSchemaVersion, policyVersion, actionCode, meaningCode, sourceRecordRef, projectionHash);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
