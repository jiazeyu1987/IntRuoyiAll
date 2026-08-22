package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Canonical Flow 6 credential for an independent batch entry. */
@Data
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceipt {

    private String receiptId;
    private Long tenantId;
    private String entryType;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeId;
    private Long routeVersionId;
    private String routeVersion;
    private String batchCode;
    private String sourceRelationId;
    private String sourceRelationVersion;
    private String sourceRelationSnapshotHash;
    private String sourceObjectType;
    private String sourceObjectId;
    private String materialSourceType;
    private String materialSourceId;
    private String sourceContextHash;
    private String sourceSnapshotHash;
    private String businessReason;
    private String issuerSystem;
    private Long issuerUserId;
    private String issuerUserRole;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revocationReason;
    private Long credentialVersion;
    private String status;
    private String receiptHash;
    private String payloadHash;
    private String signature;
    private String auditEventId;
    private String idempotencyKey;
    private java.util.List<MesBatchExecutionSourceEvidence> sourceEvidence;
}
