package cn.iocoder.yudao.module.dcc.signature.service.adapter;

import cn.iocoder.yudao.module.dcc.service.file.DccSignatureVerificationService;
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
public class DccSignatureGovernanceAdapter implements SignatureGovernanceAdapter {

    public static final String ADAPTER_CODE = "dcc-governance-adapter";
    public static final String ADAPTER_VERSION = "dcc-adapter-v1";
    public static final String EVIDENCE_SCHEMA_VERSION = "dcc-evidence-v1";
    public static final String ACTION_DOC_CONTROL_REVIEW_APPROVE = "DCC_DOC_CONTROL_REVIEW_APPROVE";

    private static final String STAGE_DOC_CONTROL_REVIEW = "DOC_CONTROL_REVIEW";
    private static final String SOURCE_ACTION_APPROVE = "APPROVE";
    private static final String MEANING_DOC_CONTROL_REVIEW_APPROVE = "DOC_CONTROL_REVIEW_APPROVE";

    private final DccSignatureVerificationService signatureVerificationService;

    public DccSignatureGovernanceAdapter(DccSignatureVerificationService signatureVerificationService) {
        if (signatureVerificationService == null) {
            throw new IllegalArgumentException("DCC signature governance adapter requires current DCC signature service");
        }
        this.signatureVerificationService = signatureVerificationService;
    }

    @Override
    public SignatureGovernanceModuleCode getModuleCode() {
        return SignatureGovernanceModuleCode.DCC;
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
        return List.of(SignatureGovernanceActionDefinition.of(ACTION_DOC_CONTROL_REVIEW_APPROVE,
                MEANING_DOC_CONTROL_REVIEW_APPROVE, STAGE_DOC_CONTROL_REVIEW, true, true,
                EVIDENCE_SCHEMA_VERSION));
    }

    @Override
    public Set<SignatureGovernanceOperationMode> getSupportedOperationModes() {
        return Set.of(SignatureGovernanceOperationMode.PRODUCTION_SIGNING,
                SignatureGovernanceOperationMode.READ_ONLY_PROJECTION);
    }

    @Override
    public SignatureGovernanceAdapterProjection execute(SignatureGovernancePolicyEvaluationCommand command,
                                                        SignatureGovernancePolicySourceStatus sourceStatus) {
        SignatureGovernanceActionDefinition definition = findActionDefinition(command.actionCode())
                .orElseThrow(() -> exception(SIGNATURE_GOVERNANCE_ACTION_UNDEFINED));
        if (SignatureGovernanceOperationMode.PRODUCTION_SIGNING.equals(command.operationMode())) {
            requireSignatureInputs(command);
            signatureVerificationService.verifyPasswordAndCreateSignature(command.actorId(), command.sourceRecordId(),
                    command.taskId(), STAGE_DOC_CONTROL_REVIEW, SOURCE_ACTION_APPROVE, command.password(),
                    command.comment());
        }
        return SignatureGovernanceAdapterProjection.of(getModuleCode(), getAdapterCode(), getAdapterVersion(),
                getEvidenceSchemaVersion(), sourceStatus.policyVersion(), definition.actionCode(),
                definition.meaningCode(), "dcc-controlled-file:" + command.sourceRecordId() + ":task:"
                        + command.taskId(), projectionHash(command));
    }

    private static void requireSignatureInputs(SignatureGovernancePolicyEvaluationCommand command) {
        if (command.actorId() == null || command.sourceRecordId() == null || command.taskId() == null
                || command.password() == null) {
            throw exception(SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION);
        }
    }

    private static String projectionHash(SignatureGovernancePolicyEvaluationCommand command) {
        return command.moduleCode() + "|" + command.actionCode() + "|" + command.sourceRecordId() + "|"
                + command.taskId();
    }
}
