package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSignatureAttachCommand {

    private Long signatureId;

    private Long executionId;

    private Long auditBatchId;

    private String signatureChallengeHash;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private String cellValuesHash;
}
