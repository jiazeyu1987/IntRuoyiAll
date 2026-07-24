package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

public record SignatureGovernancePolicySourceStatus(SignatureGovernanceModuleCode moduleCode,
                                                     String sourceCode,
                                                     String policyVersion,
                                                     boolean authorityConfirmed,
                                                     String owner,
                                                     String approvalRef,
                                                     String blockerReason) {

    public SignatureGovernancePolicySourceStatus {
        if (moduleCode == null || isBlank(sourceCode) || isBlank(policyVersion)) {
            throw new IllegalArgumentException("Signature governance policy source requires module, source, and version");
        }
        sourceCode = sourceCode.trim();
        policyVersion = policyVersion.trim();
        owner = trimToNull(owner);
        approvalRef = trimToNull(approvalRef);
        blockerReason = trimToNull(blockerReason);
    }

    public static SignatureGovernancePolicySourceStatus confirmed(SignatureGovernanceModuleCode moduleCode,
                                                                  String sourceCode,
                                                                  String policyVersion,
                                                                  String owner,
                                                                  String approvalRef) {
        if (isBlank(owner) || isBlank(approvalRef)) {
            throw new IllegalArgumentException("Confirmed policy source requires owner and approval reference");
        }
        return new SignatureGovernancePolicySourceStatus(moduleCode, sourceCode, policyVersion, true,
                owner, approvalRef, null);
    }

    public static SignatureGovernancePolicySourceStatus unconfirmed(SignatureGovernanceModuleCode moduleCode,
                                                                    String sourceCode,
                                                                    String policyVersion,
                                                                    String blockerReason) {
        return new SignatureGovernancePolicySourceStatus(moduleCode, sourceCode, policyVersion, false,
                null, null, blockerReason);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
