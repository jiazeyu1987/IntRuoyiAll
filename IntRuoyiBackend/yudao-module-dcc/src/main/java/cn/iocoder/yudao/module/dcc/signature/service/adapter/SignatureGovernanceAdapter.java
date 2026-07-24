package cn.iocoder.yudao.module.dcc.signature.service.adapter;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernanceOperationMode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyEvaluationCommand;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SignatureGovernanceAdapter {

    SignatureGovernanceModuleCode getModuleCode();

    String getAdapterCode();

    String getAdapterVersion();

    String getEvidenceSchemaVersion();

    List<SignatureGovernanceActionDefinition> getActionDefinitions();

    Set<SignatureGovernanceOperationMode> getSupportedOperationModes();

    SignatureGovernanceAdapterProjection execute(SignatureGovernancePolicyEvaluationCommand command,
                                                 SignatureGovernancePolicySourceStatus sourceStatus);

    default Optional<SignatureGovernanceActionDefinition> findActionDefinition(String actionCode) {
        if (actionCode == null) {
            return Optional.empty();
        }
        return getActionDefinitions().stream()
                .filter(definition -> actionCode.equals(definition.actionCode()))
                .findFirst();
    }
}
