package cn.iocoder.yudao.module.system.service.gxpaudit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GxpAuditCommand {

    private String operationId;
    private String subjectId;
    private String subjectVersion;
    private String reason;
    private GxpAuditStateEnvelope beforeState;
    private GxpAuditStateEnvelope afterState;
    private String idempotencyKey;
    private String requestId;
    private String traceId;
    private String source;
    private String signatureRecordId;
    private String signatureContentHash;

}
