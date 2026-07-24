package cn.iocoder.yudao.module.dcc.signature.service.adapter;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernanceOperationMode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyEvaluationCommand;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_ACTION_UNDEFINED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION;

@Component
public class IntAuthSignatureGovernanceAdapter implements SignatureGovernanceAdapter {

    public static final String ADAPTER_CODE = "intauth-governance-adapter";
    public static final String ADAPTER_VERSION = "intauth-adapter-v1";
    public static final String EVIDENCE_SCHEMA_VERSION = "intauth-signature-v1";
    public static final String ACTION_SIGNATURE_PROJECTION = "INTAUTH_SIGNATURE_PROJECTION";

    private static final String MEANING_SIGNATURE_PROJECTION = "SIGNATURE_PROJECTION";

    @Override
    public SignatureGovernanceModuleCode getModuleCode() {
        return SignatureGovernanceModuleCode.INTAUTH;
    }

    @Override
    public String getAdapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Override
    public String getEvidenceSchemaVersion() {
        return EVIDENCE_SCHEMA_VERSION;
    }

    @Override
    public List<SignatureGovernanceActionDefinition> getActionDefinitions() {
        return List.of(SignatureGovernanceActionDefinition.of(ACTION_SIGNATURE_PROJECTION,
                MEANING_SIGNATURE_PROJECTION, "INTAUTH_SIGNATURE", true, true, EVIDENCE_SCHEMA_VERSION));
    }

    @Override
    public Set<SignatureGovernanceOperationMode> getSupportedOperationModes() {
        return Set.of(SignatureGovernanceOperationMode.READ_ONLY_PROJECTION);
    }

    @Override
    public SignatureGovernanceAdapterProjection execute(SignatureGovernancePolicyEvaluationCommand command,
                                                        SignatureGovernancePolicySourceStatus sourceStatus) {
        SignatureGovernanceActionDefinition definition = findActionDefinition(command.actionCode())
                .orElseThrow(() -> exception(SIGNATURE_GOVERNANCE_ACTION_UNDEFINED));
        if (!SignatureGovernanceOperationMode.READ_ONLY_PROJECTION.equals(command.operationMode())
                || command.sourceRecordId() == null) {
            throw exception(SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION);
        }
        return SignatureGovernanceAdapterProjection.of(getModuleCode(), getAdapterCode(), getAdapterVersion(),
                getEvidenceSchemaVersion(), sourceStatus.policyVersion(), definition.actionCode(),
                definition.meaningCode(), "intauth-signature:" + command.sourceRecordId(),
                projectionHash(command));
    }

    private static String projectionHash(SignatureGovernancePolicyEvaluationCommand command) {
        return command.moduleCode() + "|" + command.actionCode() + "|" + command.sourceRecordId();
    }
}
