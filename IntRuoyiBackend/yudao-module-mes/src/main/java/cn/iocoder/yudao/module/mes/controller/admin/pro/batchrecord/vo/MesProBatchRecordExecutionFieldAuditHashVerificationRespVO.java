package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditHashVerificationRespVO {

    private String status;
    private String calculatedHeadHash;
    private String storedHeadHash;
    private Long checkedBatchCount;
    private Long checkedItemCount;
    private Long brokenBatchId;
    private Long brokenItemId;
    private String failedReason;
    private LocalDateTime checkedAt;
    private String attachmentChainStatus;
    private Integer checkedAttachmentCount;
    private String attachmentChainHeadHash;
    private Integer attachmentChainIssueCount;
    private String attachmentChainFailedReason;
}
