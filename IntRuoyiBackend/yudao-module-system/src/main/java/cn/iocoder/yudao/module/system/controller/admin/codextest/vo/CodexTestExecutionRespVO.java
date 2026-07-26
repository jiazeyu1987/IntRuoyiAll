package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CodexTestExecutionRespVO {

    private Long id;
    private Long targetTenantId;
    private String targetTenantName;
    private String executionMode;
    private String status;
    private Long requestedBy;
    private Long runnerSessionId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String summary;
    private LocalDateTime createTime;
    private List<CaseResult> cases;

    @Data
    public static class CaseResult {
        private Long id;
        private Long caseId;
        private String caseNameSnapshot;
        private String methodTextSnapshot;
        private String testDataTextSnapshot;
        private Integer checkpointCount;
        private String status;
        private Long runnerSessionId;
        private LocalDateTime claimTime;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private String failureReason;
        private String progressPhase;
        private Integer currentMethodSort;
        private Integer currentCheckpointSort;
        private String progressMessage;
        private List<CheckpointResult> checkpointResults;
    }

    @Data
    public static class CheckpointResult {
        private Long id;
        private Integer checkpointSort;
        private String checkpointNameSnapshot;
        private String expectedTextSnapshot;
        private String actualText;
        private String status;
        private String mismatchDescription;
        private Long screenshotArtifactId;
        private LocalDateTime completedAt;
    }

}
