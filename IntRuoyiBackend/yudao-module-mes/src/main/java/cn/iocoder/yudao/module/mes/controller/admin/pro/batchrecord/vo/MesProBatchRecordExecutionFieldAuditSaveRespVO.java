package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSaveRespVO {

    private Long executionId;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String cellValuesHash;
    private Long auditBatchId;
    private Long signatureId;
    private LocalDateTime changedAt;
    private Integer changedFieldCount;
    private MesProBatchRecordExecutionFieldAuditHashVerificationRespVO hashVerification;
}
