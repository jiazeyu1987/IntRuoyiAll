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
        Instant createdAt,
        Instant updatedAt,
        Instant lastHeartbeatAt,
        boolean zeroWriteEvidence,
        String requestedBy,
        String reason,
        String sourceSelectionId) {

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
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("lastHeartbeatAt") Instant lastHeartbeatAt,
            @JsonProperty("zeroWriteEvidence") boolean zeroWriteEvidence,
            @JsonProperty("requestedBy") String requestedBy,
            @JsonProperty("reason") String reason,
            @JsonProperty("sourceSelectionId") String sourceSelectionId) {
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
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.lastHeartbeatAt = Objects.requireNonNull(lastHeartbeatAt, "lastHeartbeatAt");
        this.zeroWriteEvidence = zeroWriteEvidence;
        this.requestedBy = trimToNull(requestedBy);
        this.reason = requireText(reason, "reason");
        this.sourceSelectionId = requireText(sourceSelectionId, "sourceSelectionId");
    }

    public static ReleaseWorkflowRecord newWorkflow(String workflowId, String releaseTag,
                                                     Instant createdAt, String presetId,
                                                     String presetVersion) {
        return new ReleaseWorkflowRecord(workflowId, releaseTag,
                ReleaseWorkflowContract.PUBLISH_SCOPE, presetId, presetVersion,
                State.SOURCE_FREEZING, 0, 1, null, null, null, false,
                List.of(), createdAt, createdAt, createdAt, false, null,
                "server-created", "approved-source");
    }

    public ReleaseWorkflowRecord withRequestContext(String requestedBy, String reason,
                                                     String sourceSelectionId) {
        return new ReleaseWorkflowRecord(workflowId, releaseTag, publishScope, presetId, presetVersion,
                state, stateVersion, attempt, operationId, errorCode, failedStage, retryable,
                evidenceRefs, createdAt, updatedAt, lastHeartbeatAt, zeroWriteEvidence,
                requestedBy, reason, sourceSelectionId);
    }

    public enum State {
        SOURCE_FREEZING,
        PREFLIGHTING,
        TESTING,
        BUILDING,
        READY,
        TEST_DEPLOYING,
        TEST_DEPLOYED,
        TESTED,
        PROD_PREVIEW,
        PROMOTING_PROD,
        COMPLETED,
        FAILED,
        CANCELED,
        RECOVERY_REQUIRED;

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED || this == CANCELED;
        }

        public boolean isWriteStage() {
            return this == BUILDING || this == TEST_DEPLOYING || this == PROMOTING_PROD
                    || this == RECOVERY_REQUIRED;
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
}
