package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned ReleaseWorkflow state machine.  Controllers can create and
 * observe records, but only this verifier-facing service can advance them.
 */
@Service
public class ReleaseWorkflowService {

    private static final String VERIFIER_ACTOR = "verifier:runtime-control";
    private static final Map<ReleaseWorkflowRecord.State, Set<ReleaseWorkflowRecord.State>> TRANSITIONS = transitions();

    private final RuntimeControlProperties properties;
    private final ReleaseWorkflowStore store;
    private final ReleaseWorkflowLease lease;

    @Autowired
    public ReleaseWorkflowService(RuntimeControlProperties properties, ReleaseWorkflowStore store) {
        this.properties = properties;
        this.store = store;
        this.lease = new ReleaseWorkflowLease(properties);
    }

    public ReleaseWorkflowService(RuntimeControlProperties properties) {
        this(properties, new ReleaseWorkflowStore(properties));
    }

    public ReleaseWorkflowRecord create(String reason, String sourceSelectionId) {
        return create("api", reason, sourceSelectionId);
    }

    public synchronized ReleaseWorkflowRecord create(String requestedBy, String reason, String sourceSelectionId) {
        String normalizedReason = validateReason(reason);
        String normalizedSourceSelectionId = requireText(sourceSelectionId, "sourceSelectionId");
        properties.getReleaseWorkflow().validate();
        properties.getReleaseWorkflow().validateApprovedCommits();
        if (!properties.getReleaseWorkflow().getApprovedSourceSelectionId().equals(normalizedSourceSelectionId)) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_SOURCE_SELECTION_NOT_APPROVED");
        }
        ReleaseWorkflowRecord existing = store.list().stream()
                .filter(record -> !record.state().isTerminal())
                .filter(record -> normalizedReason.equals(record.reason()))
                .filter(record -> normalizedSourceSelectionId.equals(record.sourceSelectionId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Instant now = Instant.now();
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ReleaseWorkflowContract.ReleaseWorkflowIdentity identity = ReleaseWorkflowContract.createIdentity(
                now, token, properties.getReleaseWorkflow().getPresetId(),
                properties.getReleaseWorkflow().getPresetVersion());
        ReleaseWorkflowRecord record = ReleaseWorkflowRecord.newWorkflow(identity.workflowId(), identity.releaseTag(),
                now, identity.presetId(), identity.presetVersion(), normalizedSourceSelectionId,
                properties.getReleaseWorkflow().getApprovedMaintenanceCommit(),
                properties.getReleaseWorkflow().getApprovedApplicationCommit(),
                properties.getReleaseWorkflow().getApprovedFrontendCommit())
                .withRequestContext(redactActor(requestedBy), normalizedReason, normalizedSourceSelectionId);
        store.create(record);
        return record;
    }

    public ReleaseWorkflowRecord require(String workflowId) {
        return store.require(workflowId);
    }

    public List<ReleaseWorkflowRecord> list() {
        return store.list();
    }

    public ReleaseWorkflowRecord heartbeat(String workflowId, long expectedStateVersion) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.stateVersion() != expectedStateVersion) {
            throw new ReleaseWorkflowStore.CasConflictException(workflowId, expectedStateVersion,
                    current.stateVersion());
        }
        return store.touch(current, Instant.now(), VERIFIER_ACTOR);
    }

    public ReleaseWorkflowRecord assignOperation(String workflowId, long expectedStateVersion,
                                                  String operationId) {
        return store.assignOperation(store.require(workflowId), expectedStateVersion,
                requireOperationId(operationId), VERIFIER_ACTOR);
    }

    public ReleaseWorkflowRecord bindArtifacts(String workflowId, long expectedStateVersion,
                                               String packageDigest, String manifestDigest) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.stateVersion() != expectedStateVersion) {
            throw new ReleaseWorkflowStore.CasConflictException(workflowId, expectedStateVersion,
                    current.stateVersion());
        }
        return store.bindArtifacts(current, packageDigest, manifestDigest, VERIFIER_ACTOR);
    }

    public ReleaseWorkflowRecord bindTestOperation(String workflowId, long expectedStateVersion,
                                                   String operationId, String evidencePath) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.stateVersion() != expectedStateVersion) {
            throw new ReleaseWorkflowStore.CasConflictException(workflowId, expectedStateVersion,
                    current.stateVersion());
        }
        return store.bindTestOperation(current, requireOperationId(operationId),
                requireText(evidencePath, "testOperationEvidencePath"), VERIFIER_ACTOR);
    }

    public ReleaseWorkflowRecord verifyAdvance(String workflowId, long expectedStateVersion,
                                               ReleaseWorkflowRecord.State targetState,
                                               String stage, boolean success,
                                               boolean zeroWriteEvidence) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.stateVersion() != expectedStateVersion) {
            throw new ReleaseWorkflowStore.CasConflictException(workflowId, expectedStateVersion,
                    current.stateVersion());
        }
        validateTransition(current.state(), targetState);
        String normalizedStage = requireText(stage, "stage");
        String errorCode = success ? null : "RELEASE_WORKFLOW_" + normalizedStage.toUpperCase().replace('-', '_') + "_FAILED";
        String failedStage = success ? null : normalizedStage;
        boolean retryable = success ? false : targetState != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED;
        return store.update(current, expectedStateVersion, targetState, VERIFIER_ACTOR,
                errorCode, failedStage, retryable, List.of("workflow/" + normalizedStage + ".json"),
                zeroWriteEvidence);
    }

    /** Deliberately rejects any client-provided state transition. */
    public ReleaseWorkflowRecord advanceFromClient(String workflowId, long expectedStateVersion,
                                                   ReleaseWorkflowRecord.State targetState) {
        throw new VerifierRequiredException();
    }

    public ReleaseWorkflowRecord cancel(String workflowId) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.state().isTerminal()) {
            throw new ReleaseWorkflowStore.TerminalWorkflowException(workflowId, current.state());
        }
        if (current.state().isWriteStage()) {
            return store.update(current, current.stateVersion(), ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                    VERIFIER_ACTOR, "RELEASE_WORKFLOW_CANCELLED_WRITE", current.state().name(), false,
                    List.of("workflow/cancel.json"), false);
        }
        return store.update(current, current.stateVersion(), ReleaseWorkflowRecord.State.CANCELED,
                VERIFIER_ACTOR, "RELEASE_WORKFLOW_CANCELLED", current.state().name(), false,
                List.of("workflow/cancel.json"), true);
    }

    public List<ReleaseWorkflowRecord> recoverStaleWorkflows(Instant now) {
        Duration timeout = properties.getReleaseWorkflow().getHeartbeatTimeout();
        return store.list().stream()
                .filter(record -> !record.state().isTerminal())
                .filter(record -> record.lastHeartbeatAt().plus(timeout).isBefore(now))
                .map(record -> recoverStale(record, now))
                .toList();
    }

    public OptionalLease acquireEnvironmentLease(String environment, String workflowId) {
        return new OptionalLease(lease.tryAcquire(environment, workflowId,
                properties.getReleaseWorkflow().getLeaseTtl()));
    }

    public ReleaseWorkflowRecord retry(String workflowId, boolean contentOrMigrationChanged) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.state() != ReleaseWorkflowRecord.State.FAILED || !current.retryable()) {
            throw new RetryNotAllowedException(workflowId);
        }
        if (contentOrMigrationChanged) {
            throw new RetryRequiresNewWorkflowException(workflowId);
        }
        return create(current.requestedBy(), current.reason(), current.sourceSelectionId());
    }

    /* Package-visible test hook keeps the production API free of time mutation. */
    void overrideHeartbeatForTest(String workflowId, Instant heartbeatAt) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        store.touch(current, heartbeatAt, "verifier:test-clock");
    }

    private ReleaseWorkflowRecord recoverStale(ReleaseWorkflowRecord current, Instant now) {
        if (current.state().isWriteStage()) {
            return store.update(current, current.stateVersion(), ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                    VERIFIER_ACTOR, "RELEASE_WORKFLOW_HEARTBEAT_TIMEOUT", current.state().name(), false,
                    List.of("workflow/recovery-required.json"), false);
        }
        return store.update(current, current.stateVersion(), ReleaseWorkflowRecord.State.FAILED,
                VERIFIER_ACTOR, "RELEASE_WORKFLOW_HEARTBEAT_TIMEOUT", current.state().name(), true,
                List.of("workflow/timeout.json"), true);
    }

    private static void validateTransition(ReleaseWorkflowRecord.State from,
                                           ReleaseWorkflowRecord.State to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidTransitionException(from, to);
        }
    }

    private static Map<ReleaseWorkflowRecord.State, Set<ReleaseWorkflowRecord.State>> transitions() {
        EnumMap<ReleaseWorkflowRecord.State, Set<ReleaseWorkflowRecord.State>> result =
                new EnumMap<>(ReleaseWorkflowRecord.State.class);
        result.put(ReleaseWorkflowRecord.State.SOURCE_FREEZING,
                EnumSet.of(ReleaseWorkflowRecord.State.PREFLIGHTING, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.CANCELED));
        result.put(ReleaseWorkflowRecord.State.PREFLIGHTING,
                EnumSet.of(ReleaseWorkflowRecord.State.TESTING, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.CANCELED));
        result.put(ReleaseWorkflowRecord.State.TESTING,
                EnumSet.of(ReleaseWorkflowRecord.State.BUILDING, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.CANCELED));
        result.put(ReleaseWorkflowRecord.State.BUILDING,
                EnumSet.of(ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.READY,
                EnumSet.of(ReleaseWorkflowRecord.State.TEST_DEPLOYING, ReleaseWorkflowRecord.State.CANCELED));
        result.put(ReleaseWorkflowRecord.State.TEST_DEPLOYING,
                EnumSet.of(ReleaseWorkflowRecord.State.TEST_DEPLOYED, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.TEST_DEPLOYED,
                EnumSet.of(ReleaseWorkflowRecord.State.TESTED, ReleaseWorkflowRecord.State.FAILED));
        result.put(ReleaseWorkflowRecord.State.TESTED,
                EnumSet.of(ReleaseWorkflowRecord.State.PROD_PREVIEW));
        result.put(ReleaseWorkflowRecord.State.PROD_PREVIEW,
                EnumSet.of(ReleaseWorkflowRecord.State.PROMOTING_PROD, ReleaseWorkflowRecord.State.FAILED));
        result.put(ReleaseWorkflowRecord.State.PROMOTING_PROD,
                EnumSet.of(ReleaseWorkflowRecord.State.COMPLETED, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                EnumSet.of(ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.FAILED));
        return Map.copyOf(result);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_" + name.toUpperCase() + "_INVALID");
        }
        return value;
    }

    private static String validateReason(String value) {
        String reason = requireText(value, "reason");
        if (reason.length() > 120) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_REASON_INVALID");
        }
        if (reason.matches("(?is).*(password|passwd|token|secret|private[_ -]?key)\\s*[:=].+")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_REASON_CONTAINS_SECRET");
        }
        return reason;
    }

    private static String requireOperationId(String value) {
        if (value == null || !value.matches("(?:op-)?[a-z0-9-]{8,64}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_OPERATION_ID_INVALID");
        }
        return value;
    }

    private static String redactActor(String value) {
        return value == null || value.isBlank() ? "api" : value.trim().replaceAll("[^a-zA-Z0-9._:-]", "_");
    }

    public static final class OptionalLease implements AutoCloseable {
        private final java.util.Optional<ReleaseWorkflowLease.Handle> handle;

        private OptionalLease(java.util.Optional<ReleaseWorkflowLease.Handle> handle) {
            this.handle = handle;
        }

        public boolean acquired() { return handle.isPresent(); }

        @Override
        public void close() {
            handle.ifPresent(ReleaseWorkflowLease.Handle::close);
        }
    }

    public static class VerifierRequiredException extends RuntimeException {
        public VerifierRequiredException() { super("RELEASE_WORKFLOW_VERIFIER_REQUIRED"); }
    }

    public static class InvalidTransitionException extends RuntimeException {
        public InvalidTransitionException(ReleaseWorkflowRecord.State from, ReleaseWorkflowRecord.State to) {
            super("RELEASE_WORKFLOW_TRANSITION_INVALID: " + from + " -> " + to);
        }
    }

    public static class RetryNotAllowedException extends RuntimeException {
        public RetryNotAllowedException(String workflowId) { super("RELEASE_WORKFLOW_RETRY_NOT_ALLOWED: " + workflowId); }
    }

    public static class RetryRequiresNewWorkflowException extends RuntimeException {
        public RetryRequiresNewWorkflowException(String workflowId) { super("RELEASE_WORKFLOW_RETRY_REQUIRES_NEW_WORKFLOW: " + workflowId); }
    }
}
