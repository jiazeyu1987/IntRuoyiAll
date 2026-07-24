package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionApprovalSignatureCommand {

    private Long executionId;

    private String password;

    private String comment;

    private String processInstanceId;

    private String bpmTaskId;

    private String bpmTaskDefinitionKey;

    private String bpmTaskName;

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    private Long reviewSourceId;

    private String reviewSourceName;

    private String approvalResult;

    private String reason;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private String cellValuesHash;

    private MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand;
}
