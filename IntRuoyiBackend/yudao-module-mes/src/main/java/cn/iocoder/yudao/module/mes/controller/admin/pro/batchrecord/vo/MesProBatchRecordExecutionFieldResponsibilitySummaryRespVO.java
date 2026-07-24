package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityContextWarning;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityEvidenceStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO {

    private Long executionId;
    private String executionCode;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    private String batchRecordReportId;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String cellValuesHash;
    private MesProBatchRecordExecutionResponsibilityEvidenceStatus overallEvidenceStatus;
    private List<MesProBatchRecordExecutionResponsibilityReasonCode> overallReasonCodes;
    private List<MesProBatchRecordExecutionResponsibilityContextWarning> contextWarnings;
    private Long total;
    private List<MesProBatchRecordExecutionFieldResponsibilityItemRespVO> list;
}
