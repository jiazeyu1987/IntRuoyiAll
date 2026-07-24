package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ApprovalTaskSummary {

    private String id;

    private ApprovalModuleCode moduleCode;

    private String sourceTaskType;

    private String sourceTaskId;

    private String businessKey;

    private String businessTitle;

    private String businessCode;

    private String businessStatus;

    private Boolean businessDeleted;

    private String currentNodeCode;

    private String currentNodeName;

    private Long initiatorUserId;

    private Long assigneeUserId;

    private String assigneeUserName;

    private String processInstanceId;

    private LocalDateTime initiatedAt;

    private LocalDateTime taskCreatedAt;

    private LocalDateTime taskCompletedAt;

    private ApprovalTaskReviewResult approvalResult;

    private String approvalRemark;

    private Boolean requiresSignature;

    private String detailRoute;

    @Builder.Default
    private Map<String, String> detailQuery = new LinkedHashMap<>();

    private String decisionDetailRoute;

    @Builder.Default
    private Map<String, String> decisionDetailQuery = new LinkedHashMap<>();

    @Builder.Default
    private Set<String> availableActions = new LinkedHashSet<>();

    @Builder.Default
    private Set<ApprovalTaskCapability> capabilities = new LinkedHashSet<>();
}
