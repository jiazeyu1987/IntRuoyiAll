package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/** Stable result contract returned by flow 6 after create/reuse. */
@Data
@Accessors(chain = true)
public class MesBatchExecutionProvisionResult {

    private Long batchExecutionId;
    private boolean created;
    private String batchProvisionReceiptId;
    private String batchProvisionStatus;
    private String batchExecutionSourceRelationId;
    private String sourceContextHash;
    private String entryType;
    private String credentialId;
    private String auditEventId;
}
