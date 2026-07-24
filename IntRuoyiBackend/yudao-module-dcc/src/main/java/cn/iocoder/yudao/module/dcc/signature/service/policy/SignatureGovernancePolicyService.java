package cn.iocoder.yudao.module.dcc.signature.service.policy;

public interface SignatureGovernancePolicyService {

    SignatureGovernancePolicyDecision evaluate(SignatureGovernancePolicyEvaluationCommand command);

    SignatureGovernancePolicyDecision requireAllowed(SignatureGovernancePolicyEvaluationCommand command);

    SignatureGovernancePolicyOverview describeModule(cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode moduleCode);

    SignatureGovernancePolicyDriftReport detectDrift(SignatureGovernancePolicyExpectedState expectedState);
}
