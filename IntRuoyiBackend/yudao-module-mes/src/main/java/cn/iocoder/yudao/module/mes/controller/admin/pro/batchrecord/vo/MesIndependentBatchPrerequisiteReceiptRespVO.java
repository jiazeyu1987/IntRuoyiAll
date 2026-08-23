package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceiptRespVO {
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

    public static MesIndependentBatchPrerequisiteReceiptRespVO from(MesIndependentBatchPrerequisiteReceipt receipt) {
        MesIndependentBatchPrerequisiteReceiptRespVO result = new MesIndependentBatchPrerequisiteReceiptRespVO();
        result.receiptId = receipt.getReceiptId(); result.tenantId = receipt.getTenantId();
        result.entryType = receipt.getEntryType(); result.workOrderId = receipt.getWorkOrderId();
        result.workOrderCode = receipt.getWorkOrderCode(); result.routeId = receipt.getRouteId();
        result.routeVersionId = receipt.getRouteVersionId(); result.routeVersion = receipt.getRouteVersion();
        result.batchCode = receipt.getBatchCode(); result.sourceRelationId = receipt.getSourceRelationId();
        result.sourceRelationVersion = receipt.getSourceRelationVersion();
        result.sourceRelationSnapshotHash = receipt.getSourceRelationSnapshotHash();
        result.sourceObjectType = receipt.getSourceObjectType(); result.sourceObjectId = receipt.getSourceObjectId();
        result.materialSourceType = receipt.getMaterialSourceType(); result.materialSourceId = receipt.getMaterialSourceId();
        result.sourceContextHash = receipt.getSourceContextHash(); result.sourceSnapshotHash = receipt.getSourceSnapshotHash();
        result.businessReason = receipt.getBusinessReason(); result.issuerSystem = receipt.getIssuerSystem();
        result.issuerUserId = receipt.getIssuerUserId(); result.issuerUserRole = receipt.getIssuerUserRole();
        result.issuedAt = receipt.getIssuedAt(); result.expiresAt = receipt.getExpiresAt();
        result.revokedAt = receipt.getRevokedAt(); result.revocationReason = receipt.getRevocationReason();
        result.credentialVersion = receipt.getCredentialVersion(); result.status = receipt.getStatus();
        result.receiptHash = receipt.getReceiptHash(); result.payloadHash = receipt.getPayloadHash();
        result.signature = receipt.getSignature(); result.auditEventId = receipt.getAuditEventId();
        result.idempotencyKey = receipt.getIdempotencyKey();
        return result;
    }
}
