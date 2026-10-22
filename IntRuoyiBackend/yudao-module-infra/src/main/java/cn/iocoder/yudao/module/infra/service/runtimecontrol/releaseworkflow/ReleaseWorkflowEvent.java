package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReleaseWorkflowEvent(
        long sequence,
        String workflowId,
        ReleaseWorkflowRecord.State fromState,
        ReleaseWorkflowRecord.State toState,
        long stateVersion,
        String actor,
        String errorCode,
        String failedStage,
        boolean retryable,
        List<String> evidenceRefs,
        Map<String, String> details,
        Instant occurredAt) {

    @JsonCreator
    public ReleaseWorkflowEvent(
            @JsonProperty("sequence") long sequence,
            @JsonProperty("workflowId") String workflowId,
            @JsonProperty("fromState") ReleaseWorkflowRecord.State fromState,
            @JsonProperty("toState") ReleaseWorkflowRecord.State toState,
            @JsonProperty("stateVersion") long stateVersion,
            @JsonProperty("actor") String actor,
            @JsonProperty("errorCode") String errorCode,
            @JsonProperty("failedStage") String failedStage,
            @JsonProperty("retryable") boolean retryable,
            @JsonProperty("evidenceRefs") List<String> evidenceRefs,
            @JsonProperty("details") Map<String, String> details,
            @JsonProperty("occurredAt") Instant occurredAt) {
        this.sequence = sequence;
        this.workflowId = workflowId;
        this.fromState = fromState;
        this.toState = toState;
        this.stateVersion = stateVersion;
        this.actor = actor;
        this.errorCode = errorCode;
        this.failedStage = failedStage;
        this.retryable = retryable;
        this.evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        this.details = details == null ? Map.of() : Map.copyOf(details);
        this.occurredAt = occurredAt;
    }
}
