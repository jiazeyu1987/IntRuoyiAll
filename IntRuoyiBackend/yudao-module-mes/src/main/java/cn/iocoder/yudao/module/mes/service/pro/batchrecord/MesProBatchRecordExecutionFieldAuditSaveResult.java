package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSaveResult {

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private String cellValuesHash;

    private Long auditBatchId;

    private Long signatureId;

    private LocalDateTime changedAt;

    private Integer changedFieldCount;

    private MesProBatchRecordExecutionFieldAuditHashVerification hashVerification;
}
