package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

import java.util.Map;

public record SignatureGovernancePolicyExpectedState(SignatureGovernanceModuleCode moduleCode,
                                                     String policyVersion,
                                                     String policySourceCode,
                                                     String adapterCode,
                                                     String adapterVersion,
                                                     String evidenceSchemaVersion,
                                                     Map<String, String> meaningByActionCode) {

    public SignatureGovernancePolicyExpectedState {
        if (moduleCode == null) {
            throw new IllegalArgumentException("Expected signature governance state requires module");
        }
        meaningByActionCode = meaningByActionCode == null ? Map.of() : Map.copyOf(meaningByActionCode);
    }

    public static SignatureGovernancePolicyExpectedState of(SignatureGovernanceModuleCode moduleCode,
                                                            String policyVersion,
                                                            String policySourceCode,
                                                            String adapterCode,
                                                            String adapterVersion,
                                                            String evidenceSchemaVersion,
                                                            Map<String, String> meaningByActionCode) {
        return new SignatureGovernancePolicyExpectedState(moduleCode, policyVersion, policySourceCode,
                adapterCode, adapterVersion, evidenceSchemaVersion, meaningByActionCode);
    }
}
