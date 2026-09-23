package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
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
                .filter(record -> record.backupIntent() == null)
                .filter(record -> !record.state().isTerminal())
                .filter(record -> normalizedReason.equals(record.reason()))
                .filter(record -> normalizedSourceSelectionId.equals(record.sourceSelectionId()))
                .filter(this::hasCurrentApprovedSourceTuple)
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

    private boolean hasCurrentApprovedSourceTuple(ReleaseWorkflowRecord record) {
        RuntimeControlProperties.ReleaseWorkflow workflow = properties.getReleaseWorkflow();
        return workflow.getPresetId().equals(record.presetId())
                && workflow.getPresetVersion().equals(record.presetVersion())
                && ReleaseWorkflowContract.PUBLISH_SCOPE.equals(record.publishScope())
                && workflow.getApprovedMaintenanceCommit().equals(record.maintenanceCommit())
                && workflow.getApprovedApplicationCommit().equals(record.applicationCommit())
                && workflow.getApprovedFrontendCommit().equals(record.frontendCommit());
    }

    public synchronized ReleaseWorkflowRecord createBackup(ReleaseWorkflowBackupAuthorizationService.Grant grant) {
        var preview = grant.preview();
        String workflowId = "rw-backup-" + ReleaseWorkflowBackupAuthorizationService.digest(
                preview.actor() + "|" + preview.idempotencyKey()).substring(0, 32);
        ReleaseWorkflowRecord existing = store.list().stream().filter(record -> workflowId.equals(record.workflowId()))
                .findFirst().orElse(null);
        if (existing != null) {
            if (existing.backupIntent() == null || !existing.reason().equals(preview.reason())
                    || !existing.sourceSelectionId().equals(preview.sourceSelectionId())
                    || !existing.backupIntent().targetFingerprint().equals(preview.targetFingerprint())
                    || !existing.backupIntent().authorizationId().equals(grant.authorizationId())) {
                throw new IllegalStateException("RELEASE_WORKFLOW_IDEMPOTENCY_CONFLICT");
            }
            return existing;
        }
        Instant now = Instant.now();
        String releaseTag = "release-review-" + workflowId.substring("rw-backup-".length());
        ReleaseWorkflowRecord created = new ReleaseWorkflowRecord(workflowId, releaseTag,
                ReleaseWorkflowContract.PUBLISH_SCOPE, preview.presetId(), preview.presetVersion(),
                ReleaseWorkflowRecord.State.SOURCE_FREEZING, 0, 1, null, null, null, false, List.of(),
                null, null, null, null, now, now, now, false, preview.actor(), preview.reason(),
                preview.sourceSelectionId(), preview.maintenanceCommit(), preview.applicationCommit(),
                preview.frontendCommit(), new ReleaseWorkflowRecord.BackupIntent(grant.authorizationId(), preview.previewId(),
                        preview.targetFingerprint(), preview.idempotencyKey()));
        try {
            store.create(created);
            return created;
        } catch (ReleaseWorkflowStore.DuplicateWorkflowException conflict) {
            // A concurrent process committed this deterministic request identity first.
            return createBackup(grant);
        }
    }

    public ReleaseWorkflowRecord require(String workflowId) {
        return store.require(workflowId);
    }

    public List<ReleaseWorkflowRecord> list() {
        return store.list();
    }

    public List<ReleaseWorkflowEvent> readJournal(String workflowId) {
        return store.readJournal(workflowId);
    }

    public ReleaseWorkflowRecord heartbeat(String workflowId, long expectedStateVersion) {
        return heartbeat(workflowId, expectedStateVersion, Instant.now());
    }

    public ReleaseWorkflowRecord heartbeat(String workflowId, long expectedStateVersion, Instant heartbeatAt) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.stateVersion() != expectedStateVersion) {
            throw new ReleaseWorkflowStore.CasConflictException(workflowId, expectedStateVersion,
                    current.stateVersion());
        }
        if (heartbeatAt == null) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_HEARTBEAT_AT_INVALID");
        }
        return store.touch(current, heartbeatAt, VERIFIER_ACTOR);
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
        return verifyAdvance(workflowId, expectedStateVersion, targetState, stage, success,
                zeroWriteEvidence, null);
    }

    public ReleaseWorkflowRecord verifyAdvance(String workflowId, long expectedStateVersion,
                                               ReleaseWorkflowRecord.State targetState,
                                               String stage, boolean success,
                                               boolean zeroWriteEvidence,
                                               List<String> evidenceRefs) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.stateVersion() != expectedStateVersion) {
            throw new ReleaseWorkflowStore.CasConflictException(workflowId, expectedStateVersion,
                    current.stateVersion());
        }
        if (!success && targetState == ReleaseWorkflowRecord.State.FAILED
                && current.state().isWriteStage() && !zeroWriteEvidence) {
            targetState = ReleaseWorkflowRecord.State.RECOVERY_REQUIRED;
        }
        validateTransition(current.state(), targetState);
        String normalizedStage = requireText(stage, "stage");
        String errorCode = success ? null : "RELEASE_WORKFLOW_" + normalizedStage.toUpperCase().replace('-', '_') + "_FAILED";
        String failedStage = success ? null : normalizedStage;
        boolean retryable = success ? false : targetState != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                && targetState != ReleaseWorkflowRecord.State.BACKUP_FINALIZING;
        List<String> normalizedEvidenceRefs = evidenceRefs == null || evidenceRefs.isEmpty()
                ? List.of("workflow/" + normalizedStage + ".json")
                : List.copyOf(evidenceRefs);
        return store.update(current, expectedStateVersion, targetState, VERIFIER_ACTOR,
                errorCode, failedStage, retryable, normalizedEvidenceRefs,
                zeroWriteEvidence);
    }

    /** Called by the orchestrator only after fresh, complete build-only and executor-termination proof. */
    ReleaseWorkflowRecord retireVerifiedBackupBuildFailure(String workflowId, long version, String actor, String digest) {
        var current = store.require(workflowId);
        if (current.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED || current.backupIntent() == null
                || !java.util.Objects.equals(current.requestedBy(), actor) || digest == null || !digest.matches("[0-9a-f]{64}")
                || current.packageDigest() != null || current.manifestDigest() != null) {
            throw new IllegalStateException("BUILD_RECOVERY_COMPLETION_BINDING_INVALID");
        }
        return store.update(current, version, ReleaseWorkflowRecord.State.FAILED, actor,
                "RELEASE_WORKFLOW_VERIFIED_BUILD_FAILURE", "VERIFIED_BUILD_FAILURE", false,
                List.of("build-retirement-proof:" + digest, "build-retirement-actor:" + actor), false);
    }

    public ReleaseWorkflowRecord failTestAcceptance(String workflowId, long expectedStateVersion,
                                                     ReleaseWorkflowTestResult testResult,
                                                     String conclusion, String acceptedBy) {
        if (testResult != ReleaseWorkflowTestResult.FAIL) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_TEST_RESULT_INVALID");
        }
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.stateVersion() != expectedStateVersion) {
            throw new ReleaseWorkflowStore.CasConflictException(workflowId, expectedStateVersion,
                    current.stateVersion());
        }
        validateTransition(current.state(), ReleaseWorkflowRecord.State.FAILED);
        Instant acceptedAt = Instant.now();
        String actor = redactActor(acceptedBy);
        Map<String, String> details = new LinkedHashMap<>();
        details.put("testResult", testResult.name());
        details.put("conclusion", requireText(conclusion, "testConclusion"));
        details.put("acceptedBy", actor);
        details.put("acceptedAt", acceptedAt.toString());
        details.put("workflowId", current.workflowId());
        details.put("releaseTag", current.releaseTag());
        if (current.packageDigest() != null) {
            details.put("packageDigest", current.packageDigest());
        }
        if (current.manifestDigest() != null) {
            details.put("manifestDigest", current.manifestDigest());
        }
        if (current.testOperationId() != null) {
            details.put("publishTestOperationId", current.testOperationId());
        }
        if (current.testOperationEvidencePath() != null) {
            details.put("publishTestOperationEvidencePath", current.testOperationEvidencePath());
        }
        return store.update(current, expectedStateVersion, ReleaseWorkflowRecord.State.FAILED,
                actor, "RELEASE_WORKFLOW_TEST_ACCEPTANCE_FAILED", "TEST_ACCEPTANCE",
                false, List.of("workflow/journal:test-acceptance-failed"), false, details);
    }

    /** Deliberately rejects any client-provided state transition. */
    public ReleaseWorkflowRecord advanceFromClient(String workflowId, long expectedStateVersion,
                                                   ReleaseWorkflowRecord.State targetState) {
        throw new VerifierRequiredException();
    }

    public ReleaseWorkflowRecord cancel(String workflowId) {
        ReleaseWorkflowRecord current = store.require(workflowId);
        if (current.state() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING) {
            throw new IllegalStateException("RELEASE_WORKFLOW_ACCEPTED_PUBLICATION_CANNOT_BE_CANCELED");
        }
        if (current.state().isTerminal()) {
            throw new ReleaseWorkflowStore.TerminalWorkflowException(workflowId, current.state());
        }
        if (current.state().isWriteStage() || (current.backupIntent() != null && current.operationId() != null)) {
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
                .filter(record -> record.state().isHeartbeatMonitored())
                .filter(record -> record.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED)
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
        if (current.backupIntent() != null) {
            throw new RetryRequiresNewWorkflowException(workflowId);
        }
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
        if (current.state().isWriteStage() || (current.backupIntent() != null && current.operationId() != null)) {
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
                        ReleaseWorkflowRecord.State.CANCELED, ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.PREFLIGHTING,
                EnumSet.of(ReleaseWorkflowRecord.State.TESTING, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.CANCELED, ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.TESTING,
                EnumSet.of(ReleaseWorkflowRecord.State.BUILDING, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.CANCELED, ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.BUILDING,
                EnumSet.of(ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.READY,
                EnumSet.of(ReleaseWorkflowRecord.State.TEST_DEPLOYING, ReleaseWorkflowRecord.State.BACKUP_DEPLOYING,
                        ReleaseWorkflowRecord.State.CANCELED, ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                        ReleaseWorkflowRecord.State.FAILED));
        result.put(ReleaseWorkflowRecord.State.BACKUP_DEPLOYING,
                EnumSet.of(ReleaseWorkflowRecord.State.BACKUP_FINALIZING, ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                        ReleaseWorkflowRecord.State.FAILED));
        result.put(ReleaseWorkflowRecord.State.BACKUP_FINALIZING,
                EnumSet.of(ReleaseWorkflowRecord.State.BACKUP_FINALIZING, ReleaseWorkflowRecord.State.BACKUP_DEPLOYED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.TEST_DEPLOYING,
                EnumSet.of(ReleaseWorkflowRecord.State.TEST_DEPLOYED, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.TEST_DEPLOYED,
                EnumSet.of(ReleaseWorkflowRecord.State.TESTED, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.TESTED,
                EnumSet.of(ReleaseWorkflowRecord.State.PROD_PREVIEW));
        result.put(ReleaseWorkflowRecord.State.PROD_PREVIEW,
                EnumSet.of(ReleaseWorkflowRecord.State.PROMOTING_PROD, ReleaseWorkflowRecord.State.FAILED));
        result.put(ReleaseWorkflowRecord.State.PROMOTING_PROD,
                EnumSet.of(ReleaseWorkflowRecord.State.COMPLETED, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED));
        result.put(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                EnumSet.of(ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.FAILED,
                        ReleaseWorkflowRecord.State.BACKUP_FINALIZING));
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
