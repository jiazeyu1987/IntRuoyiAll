package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionAttachmentVoidCommand {

    private Long executionId;
    private Long workTaskId;
    private Long auditBatchId;
    private Long signatureId;
    private String fieldKey;
    private String fieldPath;
    private String attachmentGroupKey;
    private Long operatorId;
    private String operatorName;
    private String reasonCategory;
    private String reasonText;
}
