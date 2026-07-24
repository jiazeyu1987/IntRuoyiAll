package cn.iocoder.yudao.module.dcc.signature.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.IntAuthSignatureGovernanceAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernanceOperationMode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyEvaluationCommand;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernancePolicyIntAuthAdapterTest {

    @Test
    void intAuthAdapter_exposesReadOnlyProjectionWithoutClaimingProductionSigningAuthority() {
        IntAuthSignatureGovernanceAdapter adapter = new IntAuthSignatureGovernanceAdapter();

        assertEquals(SignatureGovernanceModuleCode.INTAUTH, adapter.getModuleCode());
        assertFalse(adapter.getSupportedOperationModes().contains(SignatureGovernanceOperationMode.PRODUCTION_SIGNING));
        assertTrue(adapter.getSupportedOperationModes().contains(SignatureGovernanceOperationMode.READ_ONLY_PROJECTION));
        assertTrue(adapter.findActionDefinition(IntAuthSignatureGovernanceAdapter.ACTION_SIGNATURE_PROJECTION)
                .isPresent());
    }

    @Test
    void execute_returnsCurrentSignatureProjectionBoundToPolicySource() {
        IntAuthSignatureGovernanceAdapter adapter = new IntAuthSignatureGovernanceAdapter();

        SignatureGovernanceAdapterProjection projection = adapter.execute(
                SignatureGovernancePolicyEvaluationCommand.signature(SignatureGovernanceModuleCode.INTAUTH,
                        IntAuthSignatureGovernanceAdapter.ACTION_SIGNATURE_PROJECTION,
                        SignatureGovernanceOperationMode.READ_ONLY_PROJECTION,
                        101L, 9001L, null, null, null),
                SignatureGovernancePolicySourceStatus.unconfirmed(SignatureGovernanceModuleCode.INTAUTH,
                        "intauth-readonly-signatures", "policy-v1", "QA approval is missing"));

        assertEquals("intauth-governance-adapter", projection.adapterCode());
        assertEquals("policy-v1", projection.policyVersion());
        assertEquals(IntAuthSignatureGovernanceAdapter.ACTION_SIGNATURE_PROJECTION, projection.actionCode());
        assertEquals("intauth-signature:9001", projection.sourceRecordRef());
    }
}
