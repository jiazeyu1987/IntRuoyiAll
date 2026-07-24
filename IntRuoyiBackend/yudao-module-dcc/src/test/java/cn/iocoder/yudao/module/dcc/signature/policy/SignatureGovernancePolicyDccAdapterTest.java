package cn.iocoder.yudao.module.dcc.signature.policy;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.service.file.DccSignatureVerificationService;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.DccSignatureGovernanceAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernanceOperationMode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyEvaluationCommand;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_ACTION_UNDEFINED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernancePolicyDccAdapterTest {

    @Test
    void execute_wrapsCurrentDccChainWithoutChangingStageActionOrEvidenceSchema() {
        CapturingDccSignatureVerificationService delegate = new CapturingDccSignatureVerificationService();
        DccSignatureGovernanceAdapter adapter = new DccSignatureGovernanceAdapter(delegate);

        SignatureGovernanceAdapterProjection projection = adapter.execute(signatureCommand(
                        DccSignatureGovernanceAdapter.ACTION_DOC_CONTROL_REVIEW_APPROVE),
                confirmedSource());

        assertEquals(1, delegate.calls);
        assertEquals(7L, delegate.actorId);
        assertEquals(901L, delegate.controlledFileId);
        assertEquals("task-1", delegate.taskId);
        assertEquals("DOC_CONTROL_REVIEW", delegate.stageCode);
        assertEquals("APPROVE", delegate.actionType);
        assertEquals("pw-1", delegate.password);
        assertEquals("approved in source workflow", delegate.comment);
        assertEquals(SignatureGovernanceModuleCode.DCC, projection.moduleCode());
        assertEquals(DccSignatureGovernanceAdapter.ADAPTER_CODE, projection.adapterCode());
        assertEquals(DccSignatureGovernanceAdapter.EVIDENCE_SCHEMA_VERSION, projection.evidenceSchemaVersion());
        assertEquals("policy-v1", projection.policyVersion());
        assertEquals(DccSignatureGovernanceAdapter.ACTION_DOC_CONTROL_REVIEW_APPROVE, projection.actionCode());
        assertEquals("DOC_CONTROL_REVIEW_APPROVE", projection.meaningCode());
        assertTrue(projection.sourceRecordRef().contains("901"));
    }

    @Test
    void execute_unknownActionFailsFastBeforeCallingCurrentDccChain() {
        CapturingDccSignatureVerificationService delegate = new CapturingDccSignatureVerificationService();
        DccSignatureGovernanceAdapter adapter = new DccSignatureGovernanceAdapter(delegate);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> adapter.execute(signatureCommand("DCC_UNKNOWN_ACTION"), confirmedSource()));

        assertEquals(SIGNATURE_GOVERNANCE_ACTION_UNDEFINED.getCode(), exception.getCode());
        assertEquals(0, delegate.calls);
    }

    private static SignatureGovernancePolicyEvaluationCommand signatureCommand(String actionCode) {
        return SignatureGovernancePolicyEvaluationCommand.signature(
                SignatureGovernanceModuleCode.DCC,
                actionCode,
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING,
                7L,
                901L,
                "task-1",
                "pw-1",
                "approved in source workflow");
    }

    private static SignatureGovernancePolicySourceStatus confirmedSource() {
        return SignatureGovernancePolicySourceStatus.confirmed(SignatureGovernanceModuleCode.DCC,
                "dcc-current-chain", "policy-v1", "qa-owner", "approved-source-ref");
    }

    private static class CapturingDccSignatureVerificationService implements DccSignatureVerificationService {

        private int calls;
        private Long actorId;
        private Long controlledFileId;
        private String taskId;
        private String stageCode;
        private String actionType;
        private String password;
        private String comment;

        @Override
        public void verifyPasswordAndCreateSignature(Long actorId, Long controlledFileId, String taskId,
                                                     String stageCode, String actionType, String password,
                                                     String comment) {
            this.calls++;
            this.actorId = actorId;
            this.controlledFileId = controlledFileId;
            this.taskId = taskId;
            this.stageCode = stageCode;
            this.actionType = actionType;
            this.password = password;
            this.comment = comment;
        }
    }
}
