package cn.iocoder.yudao.module.showroom.workflow;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernanceOperationMode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyEvaluationCommand;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalSignatureService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomSignatureGovernanceAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomSignatureGovernanceAdapterTest {

    @Test
    void showroomAdapter_exposesPublicityApprovalSignatureChainAsReadOnlyPolicyProjection() {
        ShowroomSignatureGovernanceAdapter adapter = new ShowroomSignatureGovernanceAdapter();

        assertEquals(SignatureGovernanceModuleCode.SHOWROOM, adapter.getModuleCode());
        assertFalse(adapter.getSupportedOperationModes().contains(SignatureGovernanceOperationMode.PRODUCTION_SIGNING));
        assertTrue(adapter.getSupportedOperationModes().contains(SignatureGovernanceOperationMode.READ_ONLY_PROJECTION));
        assertEquals(ShowroomApprovalSignatureService.APPROVAL_STAGE_PUBLICITY,
                adapter.findActionDefinition(ShowroomSignatureGovernanceAdapter.ACTION_CHANGE_REQUEST_APPROVE)
                        .orElseThrow().sourceStageCode());
    }

    @Test
    void execute_returnsProjectionWithShowroomChangeRequestSourceReference() {
        ShowroomSignatureGovernanceAdapter adapter = new ShowroomSignatureGovernanceAdapter();

        SignatureGovernanceAdapterProjection projection = adapter.execute(
                SignatureGovernancePolicyEvaluationCommand.signature(SignatureGovernanceModuleCode.SHOWROOM,
                        ShowroomSignatureGovernanceAdapter.ACTION_CHANGE_REQUEST_APPROVE,
                        SignatureGovernanceOperationMode.READ_ONLY_PROJECTION,
                        101L, 770066L, null, null, null),
                SignatureGovernancePolicySourceStatus.confirmed(SignatureGovernanceModuleCode.SHOWROOM,
                        "showroom-current-signature-chain", "policy-v1", "qa-owner", "showroom-approval-ref"));

        assertEquals("showroom-governance-adapter", projection.adapterCode());
        assertEquals("policy-v1", projection.policyVersion());
        assertEquals("showroom-change-request:770066", projection.sourceRecordRef());
    }
}
