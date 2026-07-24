package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernanceOperationMode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyEvaluationCommand;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesEdhrSignatureGovernanceAdapterTest {

    @Test
    void edhrAdapter_exposesExistingBatchRecordApprovalSignatureChainAsReadOnlyPolicyProjection() {
        MesEdhrSignatureGovernanceAdapter adapter = new MesEdhrSignatureGovernanceAdapter();

        assertEquals(SignatureGovernanceModuleCode.EDHR, adapter.getModuleCode());
        assertFalse(adapter.getSupportedOperationModes().contains(SignatureGovernanceOperationMode.PRODUCTION_SIGNING));
        assertTrue(adapter.getSupportedOperationModes().contains(SignatureGovernanceOperationMode.READ_ONLY_PROJECTION));
        assertEquals(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE,
                adapter.findActionDefinition(MesEdhrSignatureGovernanceAdapter.ACTION_BATCH_RECORD_APPROVE)
                        .orElseThrow().sourceStageCode());
    }

    @Test
    void execute_returnsProjectionWithEdhrExecutionSourceReference() {
        MesEdhrSignatureGovernanceAdapter adapter = new MesEdhrSignatureGovernanceAdapter();

        SignatureGovernanceAdapterProjection projection = adapter.execute(
                SignatureGovernancePolicyEvaluationCommand.signature(SignatureGovernanceModuleCode.EDHR,
                        MesEdhrSignatureGovernanceAdapter.ACTION_BATCH_RECORD_APPROVE,
                        SignatureGovernanceOperationMode.READ_ONLY_PROJECTION,
                        101L, 880077L, "task-1", null, null),
                SignatureGovernancePolicySourceStatus.confirmed(SignatureGovernanceModuleCode.EDHR,
                        "edhr-current-signature-chain", "policy-v1", "qa-owner", "edhr-approval-ref"));

        assertEquals("edhr-governance-adapter", projection.adapterCode());
        assertEquals("policy-v1", projection.policyVersion());
        assertEquals("edhr-batch-record:880077", projection.sourceRecordRef());
    }
}
