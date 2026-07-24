package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityContextWarning;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityEvidenceStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilityExportRespVO {

    private String fileName;
    private String format;
    private String contentType;
    private String contentBase64;
    private String sha256;
    private Long recordCount;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String cellValuesHash;
    private MesProBatchRecordExecutionResponsibilityEvidenceStatus evidenceStatus;
    private List<MesProBatchRecordExecutionResponsibilityReasonCode> reasonCodes;
    private List<MesProBatchRecordExecutionResponsibilityContextWarning> contextWarnings;
    private LocalDateTime generatedAt;
}
