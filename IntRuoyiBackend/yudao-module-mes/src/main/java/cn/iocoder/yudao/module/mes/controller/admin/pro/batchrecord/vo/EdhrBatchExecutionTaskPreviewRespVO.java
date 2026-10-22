package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionTaskPreviewRespVO {

    private Long batchExecutionId;

    private Long taskId;

    private Long executionId;

    private Integer taskStatus;

    private Boolean executionCreated;

    private EdhrBatchExecutionReviewTimelineRespVO.FormViewModel formViewModel;

    private CompletionSourceMaterialization completionSourceMaterialization;

    @Data
    @Accessors(chain = true)
    public static class CompletionSourceMaterialization {
        private String status;
        private String message;
        private String receiptId;
        private String executionId;
        private Integer executionStatus;
        private String auditBatchId;
        private String sourceSnapshotHash;
        private String mappingHash;
        private String fieldAuditHeadHash;
        private Integer appliedFieldCount;
    }
}
