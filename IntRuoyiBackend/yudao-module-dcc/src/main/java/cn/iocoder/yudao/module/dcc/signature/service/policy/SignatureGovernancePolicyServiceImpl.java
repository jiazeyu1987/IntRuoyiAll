package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceActionDefinition;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterProjection;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_ACTION_UNDEFINED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_MODULE_ADAPTER_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_POLICY_SOURCE_MISSING;

public class SignatureGovernancePolicyServiceImpl implements SignatureGovernancePolicyService {

    private final SignatureGovernancePolicySourceProvider policySourceProvider;
    private final SignatureGovernanceAdapterRegistry adapterRegistry;

    public SignatureGovernancePolicyServiceImpl(SignatureGovernancePolicySourceProvider policySourceProvider,
                                                SignatureGovernanceAdapterRegistry adapterRegistry) {
        if (policySourceProvider == null || adapterRegistry == null) {
            throw new IllegalArgumentException("Signature governance policy service requires source provider and adapters");
        }
        this.policySourceProvider = policySourceProvider;
        this.adapterRegistry = adapterRegistry;
    }

    @Override
    public SignatureGovernancePolicyDecision evaluate(SignatureGovernancePolicyEvaluationCommand command) {
        Optional<SignatureGovernancePolicySourceStatus> sourceStatus =
                policySourceProvider.findByModule(command.moduleCode());
        if (sourceStatus.isEmpty()) {
            return blocked(command, null, null, policySourceMissing(command.moduleCode()));
        }
        SignatureGovernancePolicySourceStatus source = sourceStatus.get();
        if (isProductionSigning(command) && !source.authorityConfirmed()) {
            return blocked(command, source, null, authorityUnconfirmed(command.moduleCode(), source));
        }
        Optional<SignatureGovernanceAdapter> adapter = adapterRegistry.findByModule(command.moduleCode());
        if (adapter.isEmpty()) {
            return blocked(command, source, null, adapterMissing(command.moduleCode()));
        }
        SignatureGovernanceAdapter currentAdapter = adapter.get();
        if (!currentAdapter.getSupportedOperationModes().contains(command.operationMode())) {
            return blocked(command, source, currentAdapter, unsupportedMode(command, currentAdapter));
        }
        if (currentAdapter.findActionDefinition(command.actionCode()).isEmpty()) {
            return blocked(command, source, currentAdapter, actionUndefined(command));
        }
        SignatureGovernanceAdapterProjection projection = currentAdapter.execute(command, source);
        return SignatureGovernancePolicyDecision.allowed(command, source, projection);
    }

    @Override
    public SignatureGovernancePolicyDecision requireAllowed(SignatureGovernancePolicyEvaluationCommand command) {
        SignatureGovernancePolicyDecision decision = evaluate(command);
        if (SignatureGovernancePolicyDecisionStatus.ALLOWED.equals(decision.status())) {
            return decision;
        }
        throw toServiceException(decision.blockers().get(0));
    }

    @Override
    public SignatureGovernancePolicyOverview describeModule(SignatureGovernanceModuleCode moduleCode) {
        Optional<SignatureGovernancePolicySourceStatus> source = policySourceProvider.findByModule(moduleCode);
        Optional<SignatureGovernanceAdapter> adapter = adapterRegistry.findByModule(moduleCode);
        List<SignatureGovernancePolicyBlocker> blockers = new ArrayList<>();
        if (source.isEmpty()) {
            blockers.add(policySourceMissing(moduleCode));
        } else if (!source.get().authorityConfirmed()) {
            blockers.add(authorityUnconfirmed(moduleCode, source.get()));
        }
        if (adapter.isEmpty()) {
            blockers.add(adapterMissing(moduleCode));
        }
        return new SignatureGovernancePolicyOverview(moduleCode, source.isPresent(),
                source.map(SignatureGovernancePolicySourceStatus::authorityConfirmed).orElse(false),
                adapter.isPresent(),
                source.map(SignatureGovernancePolicySourceStatus::policyVersion).orElse(null),
                source.map(SignatureGovernancePolicySourceStatus::sourceCode).orElse(null),
                adapter.map(SignatureGovernanceAdapter::getAdapterCode).orElse(null),
                adapter.map(SignatureGovernanceAdapter::getAdapterVersion).orElse(null),
                adapter.map(SignatureGovernanceAdapter::getEvidenceSchemaVersion).orElse(null),
                blockers);
    }

    @Override
    public SignatureGovernancePolicyDriftReport detectDrift(SignatureGovernancePolicyExpectedState expectedState) {
        SignatureGovernancePolicyOverview overview = describeModule(expectedState.moduleCode());
        if (!overview.blockers().isEmpty()) {
            return SignatureGovernancePolicyDriftReport.blocked(expectedState.moduleCode(), overview.blockers());
        }
        List<SignatureGovernancePolicyDrift> drifts = new ArrayList<>();
        addDrift(drifts, "policyVersion", expectedState.policyVersion(), overview.policyVersion());
        addDrift(drifts, "policySourceCode", expectedState.policySourceCode(), overview.policySourceCode());
        addDrift(drifts, "adapterCode", expectedState.adapterCode(), overview.adapterCode());
        addDrift(drifts, "adapterVersion", expectedState.adapterVersion(), overview.adapterVersion());
        addDrift(drifts, "evidenceSchemaVersion", expectedState.evidenceSchemaVersion(),
                overview.evidenceSchemaVersion());
        SignatureGovernanceAdapter adapter = adapterRegistry.findByModule(expectedState.moduleCode()).orElseThrow();
        for (var expectedMeaning : expectedState.meaningByActionCode().entrySet()) {
            String actualMeaning = adapter.findActionDefinition(expectedMeaning.getKey())
                    .map(SignatureGovernanceActionDefinition::meaningCode)
                    .orElse(null);
            addDrift(drifts, "action:" + expectedMeaning.getKey() + ":meaningCode",
                    expectedMeaning.getValue(), actualMeaning);
        }
        if (drifts.isEmpty()) {
            return SignatureGovernancePolicyDriftReport.aligned(expectedState.moduleCode());
        }
        return SignatureGovernancePolicyDriftReport.drifted(expectedState.moduleCode(), drifts);
    }

    private static boolean isProductionSigning(SignatureGovernancePolicyEvaluationCommand command) {
        return SignatureGovernanceOperationMode.PRODUCTION_SIGNING.equals(command.operationMode());
    }

    private static SignatureGovernancePolicyDecision blocked(SignatureGovernancePolicyEvaluationCommand command,
                                                             SignatureGovernancePolicySourceStatus source,
                                                             SignatureGovernanceAdapter adapter,
                                                             SignatureGovernancePolicyBlocker blocker) {
        return SignatureGovernancePolicyDecision.blocked(command, source,
                adapter == null ? null : adapter.getAdapterCode(),
                adapter == null ? null : adapter.getAdapterVersion(),
                adapter == null ? null : adapter.getEvidenceSchemaVersion(),
                blocker);
    }

    private static SignatureGovernancePolicyBlocker policySourceMissing(SignatureGovernanceModuleCode moduleCode) {
        return SignatureGovernancePolicyBlocker.of(SignatureGovernancePolicyBlockerCode.POLICY_SOURCE_MISSING,
                moduleCode + " authoritative signature policy source is missing",
                "The signature request must fail fast and the business state must not advance");
    }

    private static SignatureGovernancePolicyBlocker adapterMissing(SignatureGovernanceModuleCode moduleCode) {
        return SignatureGovernancePolicyBlocker.of(SignatureGovernancePolicyBlockerCode.MODULE_ADAPTER_MISSING,
                moduleCode + " signature governance adapter is not registered",
                "The signature request must fail fast and the business state must not advance");
    }

    private static SignatureGovernancePolicyBlocker actionUndefined(
            SignatureGovernancePolicyEvaluationCommand command) {
        return SignatureGovernancePolicyBlocker.of(SignatureGovernancePolicyBlockerCode.ACTION_UNDEFINED,
                command.moduleCode() + " action is not registered: " + command.actionCode(),
                "The signature request must fail fast and no default action policy may be used");
    }

    private static SignatureGovernancePolicyBlocker authorityUnconfirmed(
            SignatureGovernanceModuleCode moduleCode, SignatureGovernancePolicySourceStatus source) {
        return SignatureGovernancePolicyBlocker.of(SignatureGovernancePolicyBlockerCode.AUTHORITY_SOURCE_UNCONFIRMED,
                moduleCode + " policy source is not confirmed: " + source.sourceCode(),
                "The source may be used for read-only projection only and must not replace production signing");
    }

    private static SignatureGovernancePolicyBlocker unsupportedMode(
            SignatureGovernancePolicyEvaluationCommand command, SignatureGovernanceAdapter adapter) {
        return SignatureGovernancePolicyBlocker.of(SignatureGovernancePolicyBlockerCode.MODULE_ADAPTER_MISSING,
                adapter.getAdapterCode() + " does not support " + command.operationMode(),
                "The signature request must fail fast because the adapter cannot execute this mode");
    }

    private static ServiceException toServiceException(SignatureGovernancePolicyBlocker blocker) {
        ErrorCode errorCode = switch (blocker.code()) {
            case POLICY_SOURCE_MISSING -> SIGNATURE_GOVERNANCE_POLICY_SOURCE_MISSING;
            case MODULE_ADAPTER_MISSING, PASSWORD_VERIFICATION_SOURCE_UNREACHABLE,
                    EVIDENCE_HASH_CONFIG_MISSING -> SIGNATURE_GOVERNANCE_MODULE_ADAPTER_MISSING;
            case ACTION_UNDEFINED -> SIGNATURE_GOVERNANCE_ACTION_UNDEFINED;
            case AUTHORITY_SOURCE_UNCONFIRMED -> SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION;
        };
        return exception(errorCode);
    }

    private static void addDrift(List<SignatureGovernancePolicyDrift> drifts, String fieldName,
                                 String expectedValue, String actualValue) {
        if (!Objects.equals(expectedValue, actualValue)) {
            drifts.add(SignatureGovernancePolicyDrift.of(fieldName, expectedValue, actualValue));
        }
    }
}
