package cn.iocoder.yudao.module.dcc.signature.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.ConfigurableSignatureGovernancePolicySourceProvider;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurableSignatureGovernancePolicySourceProviderTest {

    @Test
    void findByModule_returnsEmptyWhenNoAuthoritativeSourceIsConfigured() {
        ConfigurableSignatureGovernancePolicySourceProvider provider =
                new ConfigurableSignatureGovernancePolicySourceProvider();

        assertTrue(provider.findByModule(SignatureGovernanceModuleCode.DCC).isEmpty());
    }

    @Test
    void findByModule_mapsConfirmedSourceOnlyFromExplicitModuleConfiguration() {
        ConfigurableSignatureGovernancePolicySourceProvider.ModulePolicySourceProperties source =
                new ConfigurableSignatureGovernancePolicySourceProvider.ModulePolicySourceProperties();
        source.setSourceCode("dcc-current-signature-chain");
        source.setPolicyVersion("policy-v1");
        source.setAuthorityConfirmed(true);
        source.setOwner("qa-owner");
        source.setApprovalRef("dcc-policy-approval-20260528");
        ConfigurableSignatureGovernancePolicySourceProvider provider =
                new ConfigurableSignatureGovernancePolicySourceProvider();
        provider.setModules(Map.of(SignatureGovernanceModuleCode.DCC, source));

        SignatureGovernancePolicySourceStatus status = provider.findByModule(SignatureGovernanceModuleCode.DCC)
                .orElseThrow();

        assertEquals(SignatureGovernanceModuleCode.DCC, status.moduleCode());
        assertEquals("dcc-current-signature-chain", status.sourceCode());
        assertEquals("policy-v1", status.policyVersion());
        assertTrue(status.authorityConfirmed());
        assertEquals("dcc-policy-approval-20260528", status.approvalRef());
    }

    @Test
    void findByModule_preservesUnconfirmedAuthorityAsBlockedSourceInsteadOfTreatingItAsProductionReady() {
        ConfigurableSignatureGovernancePolicySourceProvider.ModulePolicySourceProperties source =
                new ConfigurableSignatureGovernancePolicySourceProvider.ModulePolicySourceProperties();
        source.setSourceCode("intauth-readonly-signature-source");
        source.setPolicyVersion("policy-v1");
        source.setAuthorityConfirmed(false);
        source.setBlockerReason("IntAuth source is read-only until QA approval");
        ConfigurableSignatureGovernancePolicySourceProvider provider =
                new ConfigurableSignatureGovernancePolicySourceProvider();
        provider.setModules(Map.of(SignatureGovernanceModuleCode.INTAUTH, source));

        SignatureGovernancePolicySourceStatus status = provider.findByModule(SignatureGovernanceModuleCode.INTAUTH)
                .orElseThrow();

        assertFalse(status.authorityConfirmed());
        assertEquals("IntAuth source is read-only until QA approval", status.blockerReason());
    }
}
