package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;

import java.util.List;

public record SignatureGovernancePolicyDecision(SignatureGovernancePolicyDecisionStatus status,
                                                 SignatureGovernanceModuleCode moduleCode,
                                                 String actionCode,
                                                 String policyVersion,
                                                 String policySourceCode,
                                                 String adapterCode,
                                                 String adapterVersion,
                                                 String evidenceSchemaVersion,
                                                 SignatureGovernanceAdapterProjection projection,
                                                 List<SignatureGovernancePolicyBlocker> blockers) {

    public SignatureGovernancePolicyDecision {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public static SignatureGovernancePolicyDecision allowed(SignatureGovernancePolicyEvaluationCommand command,
                                                            SignatureGovernancePolicySourceStatus sourceStatus,
                                                            SignatureGovernanceAdapterProjection projection) {
        return new SignatureGovernancePolicyDecision(SignatureGovernancePolicyDecisionStatus.ALLOWED,
                command.moduleCode(), command.actionCode(), sourceStatus.policyVersion(), sourceStatus.sourceCode(),
                projection.adapterCode(), projection.adapterVersion(), projection.evidenceSchemaVersion(),
                projection, List.of());
    }

    public static SignatureGovernancePolicyDecision blocked(SignatureGovernancePolicyEvaluationCommand command,
                                                            SignatureGovernancePolicySourceStatus sourceStatus,
                                                            String adapterCode,
                                                            String adapterVersion,
                                                            String evidenceSchemaVersion,
                                                            SignatureGovernancePolicyBlocker blocker) {
        return new SignatureGovernancePolicyDecision(SignatureGovernancePolicyDecisionStatus.BLOCKED,
                command.moduleCode(), command.actionCode(),
                sourceStatus == null ? null : sourceStatus.policyVersion(),
                sourceStatus == null ? null : sourceStatus.sourceCode(),
                adapterCode, adapterVersion, evidenceSchemaVersion, null, List.of(blocker));
    }
}
