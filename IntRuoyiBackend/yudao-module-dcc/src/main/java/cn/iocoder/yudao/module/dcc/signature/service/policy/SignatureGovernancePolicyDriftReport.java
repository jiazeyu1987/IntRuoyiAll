package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

import java.util.List;

public record SignatureGovernancePolicyDriftReport(SignatureGovernanceModuleCode moduleCode,
                                                   SignatureGovernancePolicyDriftStatus status,
                                                   List<SignatureGovernancePolicyDrift> drifts,
                                                   List<SignatureGovernancePolicyBlocker> blockers) {

    public SignatureGovernancePolicyDriftReport {
        drifts = drifts == null ? List.of() : List.copyOf(drifts);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public static SignatureGovernancePolicyDriftReport drifted(SignatureGovernanceModuleCode moduleCode,
                                                               List<SignatureGovernancePolicyDrift> drifts) {
        return new SignatureGovernancePolicyDriftReport(moduleCode, SignatureGovernancePolicyDriftStatus.DRIFTED,
                drifts, List.of());
    }

    public static SignatureGovernancePolicyDriftReport aligned(SignatureGovernanceModuleCode moduleCode) {
        return new SignatureGovernancePolicyDriftReport(moduleCode, SignatureGovernancePolicyDriftStatus.ALIGNED,
                List.of(), List.of());
    }

    public static SignatureGovernancePolicyDriftReport blocked(SignatureGovernanceModuleCode moduleCode,
                                                               List<SignatureGovernancePolicyBlocker> blockers) {
        return new SignatureGovernancePolicyDriftReport(moduleCode, SignatureGovernancePolicyDriftStatus.BLOCKED,
                List.of(), blockers);
    }
}
