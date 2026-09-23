package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Durable workflow state.  The record deliberately contains only redacted
 * identifiers and evidence references; secrets and infrastructure values do
 * not belong in the workflow journal.
 */
public record ReleaseWorkflowRecord(
        String workflowId,
        String releaseTag,
        String publishScope,
        String presetId,
        String presetVersion,
        State state,
        long stateVersion,
        int attempt,
        String operationId,
        String errorCode,
        String failedStage,
        boolean retryable,
        List<String> evidenceRefs,
        String packageDigest,
        String manifestDigest,
        String testOperationId,
        String testOperationEvidencePath,
        Instant createdAt,
        Instant updatedAt,
        Instant lastHeartbeatAt,
        boolean zeroWriteEvidence,
        String requestedBy,
        String reason,
        String sourceSelectionId,
        String maintenanceCommit,
        String applicationCommit,
        String frontendCommit,
        BackupIntent backupIntent) {

    /** Schema v1 records have no independent target intent and retain the test/promote flow. */
    public ReleaseWorkflowRecord(String workflowId, String releaseTag, String publishScope, String presetId,
            String presetVersion, State state, long stateVersion, int attempt, String operationId,
            String errorCode, String failedStage, boolean retryable, List<String> evidenceRefs,
            String packageDigest, String manifestDigest, String testOperationId, String testOperationEvidencePath,
            Instant createdAt, Instant updatedAt, Instant lastHeartbeatAt, boolean zeroWriteEvidence,
            String requestedBy, String reason, String sourceSelectionId, String maintenanceCommit,
            String applicationCommit, String frontendCommit) {
        this(workflowId, releaseTag, publishScope, presetId, presetVersion, state, stateVersion, attempt,
                operationId, errorCode, failedStage, retryable, evidenceRefs, packageDigest, manifestDigest,
                testOperationId, testOperationEvidencePath, createdAt, updatedAt, lastHeartbeatAt,
                zeroWriteEvidence, requestedBy, reason, sourceSelectionId, maintenanceCommit,
                applicationCommit, frontendCommit, null);
    }

    @JsonCreator
    public ReleaseWorkflowRecord(
            @JsonProperty("workflowId") String workflowId,
            @JsonProperty("releaseTag") String releaseTag,
            @JsonProperty("publishScope") String publishScope,
            @JsonProperty("presetId") String presetId,
            @JsonProperty("presetVersion") String presetVersion,
            @JsonProperty("state") State state,
            @JsonProperty("stateVersion") long stateVersion,
            @JsonProperty("attempt") int attempt,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("errorCode") String errorCode,
            @JsonProperty("failedStage") String failedStage,
            @JsonProperty("retryable") boolean retryable,
            @JsonProperty("evidenceRefs") List<String> evidenceRefs,
            @JsonProperty("packageDigest") String packageDigest,
            @JsonProperty("manifestDigest") String manifestDigest,
            @JsonProperty("testOperationId") String testOperationId,
            @JsonProperty("testOperationEvidencePath") String testOperationEvidencePath,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("lastHeartbeatAt") Instant lastHeartbeatAt,
            @JsonProperty("zeroWriteEvidence") boolean zeroWriteEvidence,
            @JsonProperty("requestedBy") String requestedBy,
            @JsonProperty("reason") String reason,
            @JsonProperty("sourceSelectionId") String sourceSelectionId,
            @JsonProperty("maintenanceCommit") String maintenanceCommit,
            @JsonProperty("applicationCommit") String applicationCommit,
            @JsonProperty("frontendCommit") String frontendCommit,
            @JsonProperty("backupIntent") BackupIntent backupIntent) {
        this.workflowId = requireText(workflowId, "workflowId");
        this.releaseTag = requireText(releaseTag, "releaseTag");
        this.publishScope = Objects.requireNonNull(publishScope, "publishScope");
        if (!ReleaseWorkflowContract.PUBLISH_SCOPE.equals(publishScope)) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_SCOPE_INVALID");
        }
        this.presetId = requireText(presetId, "presetId");
        this.presetVersion = requireText(presetVersion, "presetVersion");
        this.state = Objects.requireNonNull(state, "state");
        if (stateVersion < 0 || attempt < 1) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_VERSION_INVALID");
        }
        this.stateVersion = stateVersion;
        this.attempt = attempt;
        this.operationId = trimToNull(operationId);
        this.errorCode = trimToNull(errorCode);
        this.failedStage = trimToNull(failedStage);
        this.retryable = retryable;
        this.evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        this.packageDigest = optionalDigest(packageDigest, "packageDigest");
        this.manifestDigest = optionalDigest(manifestDigest, "manifestDigest");
        if ((this.packageDigest == null) != (this.manifestDigest == null)) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_DIGEST_PAIR_INVALID");
        }
        this.testOperationId = trimToNull(testOperationId);
        this.testOperationEvidencePath = trimToNull(testOperationEvidencePath);
        if ((this.testOperationId == null) != (this.testOperationEvidencePath == null)) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_TEST_OPERATION_PAIR_INVALID");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.lastHeartbeatAt = Objects.requireNonNull(lastHeartbeatAt, "lastHeartbeatAt");
        this.zeroWriteEvidence = zeroWriteEvidence;
        this.requestedBy = trimToNull(requestedBy);
        this.reason = requireText(reason, "reason");
        this.sourceSelectionId = requireText(sourceSelectionId, "sourceSelectionId");
        this.maintenanceCommit = requireCommit(maintenanceCommit, "maintenanceCommit");
        this.applicationCommit = requireCommit(applicationCommit, "applicationCommit");
        this.frontendCommit = requireCommit(frontendCommit, "frontendCommit");
        this.backupIntent = backupIntent;
        if ((state == State.BACKUP_DEPLOYING || state == State.BACKUP_FINALIZING || state == State.BACKUP_DEPLOYED
                || workflowId.startsWith("rw-backup-")) && backupIntent == null) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_BACKUP_INTENT_MISSING");
        }
    }

    public static ReleaseWorkflowRecord newWorkflow(String workflowId, String releaseTag,
                                                     Instant createdAt, String presetId,
                                                     String presetVersion, String sourceSelectionId,
                                                     String maintenanceCommit, String applicationCommit,
                                                     String frontendCommit) {
        return new ReleaseWorkflowRecord(workflowId, releaseTag,
                ReleaseWorkflowContract.PUBLISH_SCOPE, presetId, presetVersion,
                State.SOURCE_FREEZING, 0, 1, null, null, null, false,
                List.of(), null, null, null, null, createdAt, createdAt, createdAt, false, null,
                "server-created", sourceSelectionId, maintenanceCommit, applicationCommit, frontendCommit);
    }

    public ReleaseWorkflowRecord withRequestContext(String requestedBy, String reason,
                                                     String sourceSelectionId) {
        return new ReleaseWorkflowRecord(workflowId, releaseTag, publishScope, presetId, presetVersion,
                state, stateVersion, attempt, operationId, errorCode, failedStage, retryable,
                evidenceRefs, packageDigest, manifestDigest, testOperationId, testOperationEvidencePath,
                createdAt, updatedAt, lastHeartbeatAt, zeroWriteEvidence,
                requestedBy, reason, sourceSelectionId, maintenanceCommit, applicationCommit, frontendCommit, backupIntent);
    }

    public ReleaseWorkflowRecord withBackupIntent(BackupIntent intent) {
        if (backupIntent != null && !backupIntent.equals(intent)) {
            throw new IllegalStateException("RELEASE_WORKFLOW_BACKUP_INTENT_IMMUTABLE");
        }
        return new ReleaseWorkflowRecord(workflowId, releaseTag, publishScope, presetId, presetVersion,
                state, stateVersion, attempt, operationId, errorCode, failedStage, retryable,
                evidenceRefs, packageDigest, manifestDigest, testOperationId, testOperationEvidencePath,
                createdAt, updatedAt, lastHeartbeatAt, zeroWriteEvidence, requestedBy, reason,
                sourceSelectionId, maintenanceCommit, applicationCommit, frontendCommit, intent);
    }

    public String targetEnvironment() { return backupIntent == null ? "test" : "backup"; }

    public record BackupIntent(String authorizationId, String previewId, String targetFingerprint,
                               String idempotencyKey) {
        public BackupIntent {
            requireText(authorizationId, "authorizationId");
            requireText(previewId, "previewId");
            requireText(idempotencyKey, "idempotencyKey");
            if (optionalDigest(targetFingerprint, "targetFingerprint") == null) {
                throw new IllegalArgumentException("RELEASE_WORKFLOW_TARGET_FINGERPRINT_MISSING");
            }
        }
    }

    public enum State {
        SOURCE_FREEZING,
        PREFLIGHTING,
        TESTING,
        BUILDING,
        READY,
        TEST_DEPLOYING,
        TEST_DEPLOYED,
        BACKUP_DEPLOYING,
        BACKUP_FINALIZING,
        BACKUP_DEPLOYED,
        TESTED,
        PROD_PREVIEW,
        PROMOTING_PROD,
        COMPLETED,
        FAILED,
        CANCELED,
        RECOVERY_REQUIRED;

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED || this == CANCELED || this == BACKUP_DEPLOYED;
        }

        public boolean isWriteStage() {
            return this == BUILDING || this == TEST_DEPLOYING || this == BACKUP_DEPLOYING || this == BACKUP_FINALIZING || this == PROMOTING_PROD
                    || this == RECOVERY_REQUIRED;
        }

        public boolean isHeartbeatMonitored() {
            return !isTerminal() && this != READY && this != TEST_DEPLOYED && this != TESTED;
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_" + name.toUpperCase() + "_INVALID");
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String optionalDigest(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_" + name.toUpperCase() + "_INVALID");
        }
        return value.toLowerCase();
    }

    private static String requireCommit(String value, String name) {
        if (value == null || !value.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_" + name.toUpperCase() + "_INVALID");
        }
        return value.toLowerCase();
    }
}
