package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ApprovalTaskTimelineEntry {

    private String id;

    private ApprovalModuleCode moduleCode;

    private String sourceTaskType;

    private String sourceTaskId;

    private String businessKey;

    private String nodeCode;

    private String nodeName;

    private String action;

    private String actionLabel;

    private Long actorUserId;

    private LocalDateTime actedAt;

    private String comment;

    private String status;

    private String evidenceType;

    private String domainReferenceId;

    private String taskOutcome;
    private LocalDateTime taskCreatedAt;
    private LocalDateTime taskCompletedAt;
    /** Persisted business-task completer; never inferred from assignee or decision state. */
    private Long taskCompletedByUserId;
    private String businessStatus;
    private String sourceStatus;
    private cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult approvalResult;
    private String decisionState;
    private String decisionSourceStatus;
    private String approvalRemark;
    private String sourceRemark;
    private String terminationReason;
    private Long decisionActorUserId;
    private LocalDateTime decisionAt;
    private String evidenceId;
}
