package cn.iocoder.yudao.module.dcc.signature.policy;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceActionDefinition;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterRegistry;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernanceOperationMode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyDecision;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyDecisionStatus;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyDriftReport;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyDriftStatus;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyEvaluationCommand;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyExpectedState;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyOverview;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyConfiguration;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyService;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyServiceImpl;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceProvider;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicySourceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_ACTION_UNDEFINED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_MODULE_ADAPTER_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_POLICY_SOURCE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernancePolicyServiceTest {

    @Test
    void evaluate_blocksWhenAuthoritativePolicySourceIsMissing() {
        SignatureGovernancePolicyService service = policyService(Map.of(),
                List.of(testAdapter(SignatureGovernanceModuleCode.DCC, "dcc-adapter-v1", "dcc-evidence-v1")));

        SignatureGovernancePolicyDecision decision = service.evaluate(command(
                SignatureGovernanceModuleCode.DCC,
                "DCC_DOC_CONTROL_REVIEW_APPROVE",
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING));

        assertEquals(SignatureGovernancePolicyDecisionStatus.BLOCKED, decision.status());
        assertEquals(SignatureGovernancePolicyBlockerCode.POLICY_SOURCE_MISSING, decision.blockers().get(0).code());
        assertTrue(decision.blockers().get(0).message().contains("DCC"));
        assertTrue(decision.blockers().get(0).impact().contains("business state"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.requireAllowed(command(
                SignatureGovernanceModuleCode.DCC,
                "DCC_DOC_CONTROL_REVIEW_APPROVE",
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING)));
        assertEquals(SIGNATURE_GOVERNANCE_POLICY_SOURCE_MISSING.getCode(), exception.getCode());
    }

    @Test
    void describeModule_exposesMissingAdapterRegistrationWithoutDefaultCompliance() {
        SignatureGovernancePolicyService service = policyService(Map.of(
                SignatureGovernanceModuleCode.EDHR, confirmedSource(SignatureGovernanceModuleCode.EDHR,
                        "edhr-current-chain", "policy-v1")), List.of());

        SignatureGovernancePolicyOverview overview = service.describeModule(SignatureGovernanceModuleCode.EDHR);

        assertFalse(overview.adapterRegistered());
        assertEquals(SignatureGovernancePolicyBlockerCode.MODULE_ADAPTER_MISSING,
                overview.blockers().get(0).code());
        assertTrue(overview.blockers().get(0).impact().contains("business state"));

        SignatureGovernancePolicyDecision decision = service.evaluate(command(
                SignatureGovernanceModuleCode.EDHR,
                "EDHR_BATCH_RECORD_APPROVE",
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING));

        assertEquals(SignatureGovernancePolicyDecisionStatus.BLOCKED, decision.status());
        assertEquals(SignatureGovernancePolicyBlockerCode.MODULE_ADAPTER_MISSING,
                decision.blockers().get(0).code());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.requireAllowed(command(
                SignatureGovernanceModuleCode.EDHR,
                "EDHR_BATCH_RECORD_APPROVE",
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING)));
        assertEquals(SIGNATURE_GOVERNANCE_MODULE_ADAPTER_MISSING.getCode(), exception.getCode());
    }

    @Test
    void evaluate_blocksUnregisteredActionAndDoesNotTreatItAsCompliant() {
        SignatureGovernanceAdapter adapter = testAdapter(SignatureGovernanceModuleCode.DCC,
                "dcc-adapter-v1", "dcc-evidence-v1");
        SignatureGovernancePolicyService service = policyService(Map.of(
                SignatureGovernanceModuleCode.DCC, confirmedSource(SignatureGovernanceModuleCode.DCC,
                        "dcc-current-chain", "policy-v1")), List.of(adapter));

        SignatureGovernancePolicyDecision decision = service.evaluate(command(
                SignatureGovernanceModuleCode.DCC,
                "DCC_UNKNOWN_ACTION",
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING));

        assertEquals(SignatureGovernancePolicyDecisionStatus.BLOCKED, decision.status());
        assertEquals(SignatureGovernancePolicyBlockerCode.ACTION_UNDEFINED, decision.blockers().get(0).code());
        assertTrue(decision.blockers().get(0).message().contains("DCC_UNKNOWN_ACTION"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.requireAllowed(command(
                SignatureGovernanceModuleCode.DCC,
                "DCC_UNKNOWN_ACTION",
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING)));
        assertEquals(SIGNATURE_GOVERNANCE_ACTION_UNDEFINED.getCode(), exception.getCode());
    }

    @Test
    void evaluate_allowsIntAuthReadOnlyProjectionButBlocksProductionSourceReplacement() {
        SignatureGovernanceAdapter adapter = testAdapter(SignatureGovernanceModuleCode.INTAUTH,
                "intauth-adapter-v1", "intauth-signature-v1");
        SignatureGovernancePolicyService service = policyService(Map.of(
                SignatureGovernanceModuleCode.INTAUTH, unconfirmedSource(SignatureGovernanceModuleCode.INTAUTH,
                        "intauth-readonly-signatures", "policy-v1")), List.of(adapter));

        SignatureGovernancePolicyDecision readOnly = service.evaluate(command(
                SignatureGovernanceModuleCode.INTAUTH,
                "INTAUTH_SIGNATURE_PROJECTION",
                SignatureGovernanceOperationMode.READ_ONLY_PROJECTION));

        assertEquals(SignatureGovernancePolicyDecisionStatus.ALLOWED, readOnly.status());
        assertEquals("policy-v1", readOnly.policyVersion());
        assertEquals("intauth-readonly-signatures", readOnly.policySourceCode());

        SignatureGovernancePolicyDecision production = service.evaluate(command(
                SignatureGovernanceModuleCode.INTAUTH,
                "INTAUTH_SIGNATURE_PROJECTION",
                SignatureGovernanceOperationMode.PRODUCTION_SIGNING));

        assertEquals(SignatureGovernancePolicyDecisionStatus.BLOCKED, production.status());
        assertEquals(SignatureGovernancePolicyBlockerCode.AUTHORITY_SOURCE_UNCONFIRMED,
                production.blockers().get(0).code());
        assertTrue(production.blockers().get(0).impact().contains("replace"));
    }

    @Test
    void detectDrift_reportsPolicySourceAdapterEvidenceAndMeaningDifferences() {
        SignatureGovernancePolicyService service = policyService(Map.of(
                SignatureGovernanceModuleCode.DCC, confirmedSource(SignatureGovernanceModuleCode.DCC,
                        "dcc-current-chain-v2", "policy-v2")), List.of(testAdapter(
                SignatureGovernanceModuleCode.DCC, "dcc-adapter-v2", "dcc-evidence-v2")));

        SignatureGovernancePolicyExpectedState expected = SignatureGovernancePolicyExpectedState.of(
                SignatureGovernanceModuleCode.DCC,
                "policy-v1",
                "dcc-current-chain-v1",
                "dcc-governance-adapter",
                "dcc-adapter-v1",
                "dcc-evidence-v1",
                Map.of("DCC_DOC_CONTROL_REVIEW_APPROVE", "DOC_CONTROL_REVIEW_APPROVED"));

        SignatureGovernancePolicyDriftReport report = service.detectDrift(expected);

        assertEquals(SignatureGovernancePolicyDriftStatus.DRIFTED, report.status());
        assertTrue(report.drifts().stream().anyMatch(drift -> drift.fieldName().equals("policyVersion")));
        assertTrue(report.drifts().stream().anyMatch(drift -> drift.fieldName().equals("policySourceCode")));
        assertTrue(report.drifts().stream().anyMatch(drift -> drift.fieldName().equals("adapterVersion")));
        assertTrue(report.drifts().stream().anyMatch(drift -> drift.fieldName().equals("evidenceSchemaVersion")));
        assertTrue(report.drifts().stream().anyMatch(drift ->
                drift.fieldName().equals("action:DCC_DOC_CONTROL_REVIEW_APPROVE:meaningCode")));
        assertTrue(report.drifts().stream().allMatch(drift -> drift.impact().contains("review")));
    }

    @Test
    void policyConfiguration_registersServiceWithInjectedAuthoritativeSourceProviderAndAdapters() {
        SignatureGovernancePolicyConfiguration configuration = new SignatureGovernancePolicyConfiguration();
        SignatureGovernanceAdapter adapter = testAdapter(SignatureGovernanceModuleCode.DCC,
                "dcc-adapter-v1", "dcc-evidence-v1");
        SignatureGovernanceAdapterRegistry registry = configuration.signatureGovernanceAdapterRegistry(
                List.of(adapter));
        SignatureGovernancePolicySourceProvider provider = moduleCode -> Optional.ofNullable(Map.of(
                SignatureGovernanceModuleCode.DCC, confirmedSource(SignatureGovernanceModuleCode.DCC,
                        "dcc-current-chain", "policy-v1")).get(moduleCode));

        SignatureGovernancePolicyService service = configuration.signatureGovernancePolicyService(provider,
                registry);

        SignatureGovernancePolicyDecision decision = service.evaluate(command(
                SignatureGovernanceModuleCode.DCC,
                "DCC_DOC_CONTROL_REVIEW_APPROVE",
                SignatureGovernanceOperationMode.READ_ONLY_PROJECTION));

        assertEquals(SignatureGovernancePolicyDecisionStatus.ALLOWED, decision.status());
        assertTrue(service instanceof SignatureGovernancePolicyServiceImpl);
    }

    private static SignatureGovernancePolicyService policyService(
            Map<SignatureGovernanceModuleCode, SignatureGovernancePolicySourceStatus> sources,
            List<SignatureGovernanceAdapter> adapters) {
        SignatureGovernancePolicySourceProvider provider = moduleCode -> Optional.ofNullable(sources.get(moduleCode));
        return new SignatureGovernancePolicyServiceImpl(provider, new SignatureGovernanceAdapterRegistry(adapters));
    }

    private static SignatureGovernancePolicyEvaluationCommand command(SignatureGovernanceModuleCode moduleCode,
                                                                       String actionCode,
                                                                       SignatureGovernanceOperationMode mode) {
        return SignatureGovernancePolicyEvaluationCommand.of(moduleCode, actionCode, mode);
    }

    private static SignatureGovernancePolicySourceStatus confirmedSource(SignatureGovernanceModuleCode moduleCode,
                                                                         String sourceCode,
                                                                         String policyVersion) {
        return SignatureGovernancePolicySourceStatus.confirmed(moduleCode, sourceCode, policyVersion,
                "qa-owner", "approved-source-ref");
    }

    private static SignatureGovernancePolicySourceStatus unconfirmedSource(SignatureGovernanceModuleCode moduleCode,
                                                                           String sourceCode,
                                                                           String policyVersion) {
        return SignatureGovernancePolicySourceStatus.unconfirmed(moduleCode, sourceCode, policyVersion,
                "authority source is not approved");
    }

    private static SignatureGovernanceAdapter testAdapter(SignatureGovernanceModuleCode moduleCode,
                                                          String adapterVersion,
                                                          String evidenceSchemaVersion) {
        return new SignatureGovernanceAdapter() {

            @Override
            public SignatureGovernanceModuleCode getModuleCode() {
                return moduleCode;
            }

            @Override
            public String getAdapterCode() {
                return moduleCode.name().toLowerCase() + "-governance-adapter";
            }

            @Override
            public String getAdapterVersion() {
                return adapterVersion;
            }

            @Override
            public String getEvidenceSchemaVersion() {
                return evidenceSchemaVersion;
            }

            @Override
            public List<SignatureGovernanceActionDefinition> getActionDefinitions() {
                String actionCode = switch (moduleCode) {
                    case DCC -> "DCC_DOC_CONTROL_REVIEW_APPROVE";
                    case EDHR -> "EDHR_BATCH_RECORD_APPROVE";
                    case SHOWROOM -> "SHOWROOM_CHANGE_REQUEST_APPROVE";
                    case INTAUTH -> "INTAUTH_SIGNATURE_PROJECTION";
                };
                String meaningCode = switch (moduleCode) {
                    case DCC -> "DOC_CONTROL_REVIEW_APPROVE";
                    case EDHR -> "BATCH_RECORD_APPROVE";
                    case SHOWROOM -> "CHANGE_REQUEST_APPROVE";
                    case INTAUTH -> "SIGNATURE_PROJECTION";
                };
                return List.of(SignatureGovernanceActionDefinition.of(actionCode, meaningCode,
                        "test-stage", true, true, evidenceSchemaVersion));
            }

            @Override
            public Set<SignatureGovernanceOperationMode> getSupportedOperationModes() {
                return Set.of(SignatureGovernanceOperationMode.PRODUCTION_SIGNING,
                        SignatureGovernanceOperationMode.READ_ONLY_PROJECTION);
            }

            @Override
            public SignatureGovernanceAdapterProjection execute(SignatureGovernancePolicyEvaluationCommand command,
                                                                SignatureGovernancePolicySourceStatus sourceStatus) {
                SignatureGovernanceActionDefinition definition = getActionDefinitions().get(0);
                return SignatureGovernanceAdapterProjection.of(moduleCode, getAdapterCode(), getAdapterVersion(),
                        getEvidenceSchemaVersion(), sourceStatus.policyVersion(), definition.actionCode(),
                        definition.meaningCode(), "test-source", "projection-hash");
            }
        };
    }
}
