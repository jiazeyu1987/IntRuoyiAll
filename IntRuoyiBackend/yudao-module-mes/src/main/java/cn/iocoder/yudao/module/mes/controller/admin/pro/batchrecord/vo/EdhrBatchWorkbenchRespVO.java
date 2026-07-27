package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchWorkbenchRespVO {

    private Long batchExecutionId;
    private String batchExecutionCode;
    private String workOrderCode;
    private String batchCode;
    private String productName;
    private String productCode;
    private String routeName;
    private String routeCode;
    private Integer batchStatus;
    private String mainStage;
    private String mainStageLabel;
    private String stageOwnerRole;
    private Integer requiredProgress;
    private Integer blockedCount;
    private List<String> stageBlockers;
    private WorkbenchTaskSummary taskSummary;
    private WorkbenchReleaseSummary releaseSummary;
    private WorkbenchAuditSummary auditSummary;

    @Data
    @Accessors(chain = true)
    public static class WorkbenchTaskSummary {
        private Integer totalCount;
        private Integer approvedCount;
        private Integer submittedCount;
        private Integer reworkCount;
        private Integer blockedCount;
    }

    @Data
    @Accessors(chain = true)
    public static class WorkbenchReleaseSummary {
        private Long releaseTransactionId;
        private String releaseStatus;
        private String releaseStatusLabel;
        private Integer blockingCheckCount;
        private Integer failedCheckCount;
        private String precheckSummary;
        private String lastPrecheckAt;
    }

    @Data
    @Accessors(chain = true)
    public static class WorkbenchAuditSummary {
        private Long latestOperationAuditId;
        private String latestOperationAt;
        private Long fieldAuditBatchCount;
        private String latestFieldAuditAt;
        private String latestDomainTraceAt;
    }
}
