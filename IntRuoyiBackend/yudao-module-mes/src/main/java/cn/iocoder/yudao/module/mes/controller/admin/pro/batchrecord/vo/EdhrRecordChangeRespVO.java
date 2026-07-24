package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class EdhrRecordChangeRespVO {

    private Long id;
    private String changeCode;
    private String changeType;
    private String targetScope;
    private Long batchExecutionId;
    private Long executionId;
    private Long sourceExecutionId;
    private Long newExecutionId;
    private Long sourceArchiveId;
    private Long newArchiveId;
    private String changeStatus;
    private String reasonCategory;
    private String reasonText;
    private Long requestedBy;
    private Long requestSignatureId;
    private Long approvedBy;
    private Long approvalSignatureId;
    private LocalDateTime effectiveAt;
    private String previousStatus;
    private String newStatus;
    private String previousHeadHash;
    private String newHeadHash;
    private String previousArchiveHash;
    private String newArchiveHash;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String bpmProcessInstanceId;
    private String bpmTaskId;
    private String remark;

}
