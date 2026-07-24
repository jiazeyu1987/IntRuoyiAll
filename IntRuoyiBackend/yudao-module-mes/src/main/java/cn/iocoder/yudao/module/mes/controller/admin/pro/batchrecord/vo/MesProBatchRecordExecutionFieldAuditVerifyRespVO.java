package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditVerifyRespVO {

    private Long executionId;
    private MesProBatchRecordExecutionFieldAuditHashVerificationRespVO hashVerification;
    private Long verifiedCount;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String cellValuesHash;
    private LocalDateTime checkedAt;
}
