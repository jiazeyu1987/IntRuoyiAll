package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

import java.util.List;

public record SignatureGovernancePolicyOverview(SignatureGovernanceModuleCode moduleCode,
                                                boolean policySourcePresent,
                                                boolean authorityConfirmed,
                                                boolean adapterRegistered,
                                                String policyVersion,
                                                String policySourceCode,
                                                String adapterCode,
                                                String adapterVersion,
                                                String evidenceSchemaVersion,
                                                List<SignatureGovernancePolicyBlocker> blockers) {

    public SignatureGovernancePolicyOverview {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
