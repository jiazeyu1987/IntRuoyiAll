package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditExportRespVO {

    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private Long executionId;
    private Long recordCount;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String cellValuesHash;
    private MesProBatchRecordExecutionFieldAuditHashVerificationRespVO hashVerification;
    private LocalDateTime generatedAt;
    private byte[] content;
}
