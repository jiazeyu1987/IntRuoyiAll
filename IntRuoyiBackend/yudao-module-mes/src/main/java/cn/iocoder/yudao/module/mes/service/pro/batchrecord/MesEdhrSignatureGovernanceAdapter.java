package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceActionDefinition;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;
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
public class MesEdhrSignatureGovernanceAdapter implements SignatureGovernanceAdapter {

    public static final String ADAPTER_CODE = "edhr-governance-adapter";
    public static final String ADAPTER_VERSION = "edhr-adapter-v1";
    public static final String EVIDENCE_SCHEMA_VERSION = "edhr-signature-v1";
    public static final String ACTION_BATCH_RECORD_APPROVE = "EDHR_BATCH_RECORD_APPROVE";

    private static final String MEANING_BATCH_RECORD_APPROVE = "BATCH_RECORD_APPROVE";

    @Override
    public SignatureGovernanceModuleCode getModuleCode() {
        return SignatureGovernanceModuleCode.EDHR;
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
        return List.of(SignatureGovernanceActionDefinition.of(ACTION_BATCH_RECORD_APPROVE,
                MEANING_BATCH_RECORD_APPROVE, MesProBatchRecordExecutionSignatureService.ACTION_APPROVE,
                true, true, EVIDENCE_SCHEMA_VERSION));
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
                definition.meaningCode(), "edhr-batch-record:" + command.sourceRecordId(),
                projectionHash(command));
    }

    private static String projectionHash(SignatureGovernancePolicyEvaluationCommand command) {
        return command.moduleCode() + "|" + command.actionCode() + "|" + command.sourceRecordId();
    }
}
