package cn.iocoder.yudao.module.system.service.gxpaudit;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GxpAuditCommand {

    private Integer eventSchemaVersion;
    private String operationId;
    private String subjectId;
    private String subjectVersion;
    private String reason;
    private GxpAuditStateEnvelope beforeState;
    private GxpAuditStateEnvelope afterState;
    private String idempotencyKey;
    private String requestId;
    private String transactionId;
    private String resultStatus;
    private String reasonCode;
    private String reasonSource;
    private String traceId;
    private String source;
    private String sourceType;
    private String sourceLocator;
    private String authenticatedActor;
    private String performedBy;
    private List<GxpAuditRelation> links;
    private List<GxpAuditEvidence> evidences;
    private String relationManifest;
    private String evidenceManifest;
    private String statePayloadHash;
    private String errorCode;
    private String attemptedOperationId;
    private String signatureRecordId;
    private String signatureContentHash;

}
