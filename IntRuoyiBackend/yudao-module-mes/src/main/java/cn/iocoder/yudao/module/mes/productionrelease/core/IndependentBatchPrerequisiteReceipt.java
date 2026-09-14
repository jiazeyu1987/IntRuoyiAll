package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class IndependentBatchPrerequisiteReceipt {

    public static final String CANONICAL_NAME = "IndependentBatchPrerequisiteReceipt";

    private String receiptId;
    private Long tenantId;
    private String entryType;
    private Long batchExecutionId;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeId;
    private String routeVersion;
    private String batchCode;
    private String sourceRelationId;
    private String sourceRelation;
    private List<String> sourceIds;
    private String sourceSnapshotHash;
    private String businessReason;
    private String issuerSystem;
    private Long issuerUserId;
    private String issuerUserRole;
    private String receiptHash;
    private Long issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revocationReason;
    private Integer credentialVersion;
    private String payloadHash;
    private String signature;
    private String auditEventId;
    private String idempotencyKey;
    private Integer version;

    public boolean isValidAt(LocalDateTime now) {
        return receiptId != null && !receiptId.isBlank()
                && batchExecutionId != null
                && sourceRelation != null && !sourceRelation.isBlank()
                && sourceSnapshotHash != null && !sourceSnapshotHash.isBlank()
                && receiptHash != null && !receiptHash.isBlank()
                && issuedBy != null && issuedAt != null
                && expiresAt != null && now != null
                && !now.isBefore(issuedAt) && now.isBefore(expiresAt)
                && revokedAt == null
                && version != null && version > 0;
    }

    public boolean isValidAt(Clock clock) {
        return isValidAt(LocalDateTime.now(clock));
    }

    public boolean isValidFor(String expectedEntryType, Clock clock) {
        return isValidAt(clock)
                && expectedEntryType != null
                && expectedEntryType.equals(entryType)
                && tenantId != null
                && workOrderId != null
                && workOrderCode != null && !workOrderCode.isBlank()
                && routeId != null
                && routeVersion != null && !routeVersion.isBlank()
                && batchCode != null && !batchCode.isBlank()
                && sourceRelationId != null && !sourceRelationId.isBlank()
                && sourceIds != null && !sourceIds.isEmpty()
                && businessReason != null && !businessReason.isBlank()
                && issuerSystem != null && !issuerSystem.isBlank()
                && issuerUserId != null
                && issuerUserRole != null && !issuerUserRole.isBlank()
                && credentialVersion != null && credentialVersion > 0
                && payloadHash != null && !payloadHash.isBlank()
                && signature != null && !signature.isBlank()
                && auditEventId != null && !auditEventId.isBlank()
                && idempotencyKey != null && !idempotencyKey.isBlank();
    }
}
