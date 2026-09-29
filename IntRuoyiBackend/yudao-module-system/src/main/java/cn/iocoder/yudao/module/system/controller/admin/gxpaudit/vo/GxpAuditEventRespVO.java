package cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class GxpAuditEventRespVO {

    private Long id;
    private Long tenantId;
    private Long ledgerSequence;
    private String operationId;
    private String domain;
    private String subjectType;
    private String subjectId;
    private String subjectVersion;
    private String action;
    private String reason;
    private Long actorId;
    private String actorUsername;
    private String actorDisplayName;
    private LocalDateTime serverOccurredAt;
    private String beforeState;
    private String beforeObjectVersion;
    private String beforeStateJson;
    private String afterState;
    private String afterObjectVersion;
    private String afterStateJson;
    private String policyVersion;
    private String idempotencyKey;
    private String requestId;
    private String signatureRecordId;
    private String signatureContentHash;
    private String idempotencyPayloadHash;
    private String canonicalEventJson;
    private String previousEventHash;
    private String eventHash;
    private String algorithm;
    private Integer eventSchemaVersion;
    private String canonicalizationVersion;
    private String resultStatus;
    private String reasonCode;
    private String reasonSource;
    private String transactionId;
    private String authenticatedActorJson;
    private String performedByJson;
    private String sourceType;
    private String sourceLocator;
    private String traceId;
    private String errorCode;
    private String attemptedOperationId;
    private String relationManifestJson;
    private String evidenceManifestJson;
    private String statePayloadHash;
    private String integrityStatus;
    private List<GxpAuditEventRelationRespVO> relations;
}
