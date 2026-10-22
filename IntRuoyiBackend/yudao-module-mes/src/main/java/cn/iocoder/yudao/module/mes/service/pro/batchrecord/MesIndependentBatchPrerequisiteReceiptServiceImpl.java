package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptDO;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_HASH_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_PERMISSION_DENIED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_PREREQUISITE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_SIGNATURE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_SOURCE_CHANGED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED;

@Service
public class MesIndependentBatchPrerequisiteReceiptServiceImpl
        implements MesIndependentBatchPrerequisiteReceiptService {

    private static final String RECEIPT_TYPE = "IndependentBatchPrerequisiteReceipt";
    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final Set<String> ENTRY_TYPES = Set.of("MANUAL", "SCHEDULED", "PQC_INDEPENDENT");

    @Resource
    private MesIndependentBatchPrerequisiteReceiptStore store;
    @Resource private MesEdhrSourceCreationAuditWriter creationWriter;
    @Resource private MesEdhrSourceCreationVerificationService creationVerifier;
    @Resource private MesEdhrSourceCreationBindingStore creationBindings;

    @Resource
    private MesProWorkOrderService workOrderService;

    @Value("${mes.pro.edhr.independent-receipt.issuer-system:}")
    private String issuerSystem;
    @Value("${mes.pro.edhr.independent-receipt.signing-secret:}")
    private String signingSecret;

    private final Clock clock;

    public MesIndependentBatchPrerequisiteReceiptServiceImpl() {
        this(Clock.systemUTC());
    }

    MesIndependentBatchPrerequisiteReceiptServiceImpl(Clock clock) {
        this.clock = clock;
    }

    MesIndependentBatchPrerequisiteReceiptServiceImpl(MesIndependentBatchPrerequisiteReceiptStore store,
                                                      Clock clock, String issuerSystem, String signingSecret) {
        this.store = store;
        this.clock = clock;
        this.issuerSystem = issuerSystem;
        this.signingSecret = signingSecret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesIndependentBatchPrerequisiteReceipt issue(MesIndependentBatchPrerequisiteReceiptIssueCommand command,
                                                        Long tenantId, Long actorUserId) {
        requireBackendIdentity(tenantId, actorUserId);
        validateIssueCommand(command);
        creationWriter.acquireLedgerLock();
        String resolvedIssuerSystem = requireConfigured(issuerSystem);
        String secret = requireConfigured(signingSecret);
        workOrderService.validateWorkOrderExistsForUpdate(command.getWorkOrderId());
        MesIndependentBatchPrerequisiteReceiptDO existing = store.selectByIdempotencyKey(
                tenantId, command.getEntryType(), command.getIdempotencyKey());
        MesIndependentBatchPrerequisiteReceipt candidate = buildReceipt(command, tenantId, actorUserId,
                resolvedIssuerSystem, existing == null ? LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS) : existing.getIssuedAt());
        String payload = MesIndependentBatchPrerequisiteReceiptCanonicalizer.canonicalPayload(candidate);
        candidate.setPayloadHash(MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(payload));
        candidate.setSignature(sign(payload, secret));
        candidate.setReceiptHash(MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(
                candidate.getPayloadHash() + "|" + candidate.getSignature()));
        if (existing != null) {
            creationVerifier.requireIndependentCreation(tenantId, existing.getReceiptId());
            if (!creationWriter.issueRequest(toDO(candidate, payload)).equals(creationWriter.issueRequest(existing))) {
                throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT);
            }
            return toReceipt(existing);
        }
        MesIndependentBatchPrerequisiteReceiptDO data = toDO(candidate, payload);
        store.insert(data);
        creationWriter.issue(data);
        return candidate;
    }

    @Override
    @Transactional(readOnly = true)
    public MesIndependentBatchPrerequisiteReceipt verify(MesIndependentBatchPrerequisiteReceiptVerifyCommand command,
                                                         Long tenantId) {
        if (tenantId == null) throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH);
        if (command == null || blank(command.getReceiptId()) || blank(command.getEntryType())
                || blank(command.getSourceSnapshotHash())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_PREREQUISITE_MISSING);
        }
        MesIndependentBatchPrerequisiteReceiptDO data = store.selectByReceiptId(tenantId, command.getReceiptId());
        if (data == null) {
            MesIndependentBatchPrerequisiteReceiptDO unscoped = store.selectByReceiptIdUnscoped(command.getReceiptId());
            if (unscoped != null && !tenantId.equals(unscoped.getTenantId())) {
                throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH);
            }
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_PREREQUISITE_MISSING);
        }
        if (!tenantId.equals(data.getTenantId())) throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH);
        MesIndependentBatchPrerequisiteReceipt receipt = toReceipt(data);
        creationVerifier.requireIndependentCreation(tenantId, data.getReceiptId());
        if (!ENTRY_TYPES.contains(receipt.getEntryType()) || !receipt.getEntryType().equals(command.getEntryType())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        }
        if (blank(receipt.getIssuerSystem()) || !requireConfigured(issuerSystem).equals(receipt.getIssuerSystem())
                || receipt.getIssuerUserId() == null || blank(receipt.getIssuerUserRole())
                || receipt.getCredentialVersion() == null || receipt.getCredentialVersion() <= 0
                || blank(receipt.getAuditEventId()) || blank(receipt.getIdempotencyKey())
                || blank(receipt.getSourceRelationId()) || blank(receipt.getSourceRelationVersion())
                || blank(receipt.getSourceRelationSnapshotHash())
                || !validEvidence(receipt.getSourceEvidence())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        }
        if (!command.getSourceSnapshotHash().equals(receipt.getSourceSnapshotHash())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_SOURCE_CHANGED);
        }
        if (STATUS_REVOKED.equals(receipt.getStatus()) || receipt.getRevokedAt() != null) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (receipt.getIssuedAt() == null || receipt.getExpiresAt() == null
                || receipt.getIssuedAt().isAfter(now) || !receipt.getExpiresAt().isAfter(now)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED);
        }
        String payload = MesIndependentBatchPrerequisiteReceiptCanonicalizer.canonicalPayload(receipt);
        String payloadHash = MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(payload);
        if (!payload.equals(data.getCanonicalPayload()) || !payloadHash.equals(receipt.getPayloadHash())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_HASH_INVALID);
        }
        String expectedSignature = sign(payload, requireConfigured(signingSecret));
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                String.valueOf(receipt.getSignature()).getBytes(StandardCharsets.UTF_8))) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_SIGNATURE_INVALID);
        }
        String expectedReceiptHash = MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(
                receipt.getPayloadHash() + "|" + receipt.getSignature());
        if (!expectedReceiptHash.equals(receipt.getReceiptHash()) || !STATUS_ISSUED.equals(receipt.getStatus())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        }
        return receipt;
    }

    @Override
    @Transactional(readOnly = true)
    public MesEdhrVerifiedIndependentReceiptFact getCreationVerifiedByReceiptId(Long tenantId, String receiptId,
            String entryType, String sourceSnapshotHash) {
        return new MesEdhrVerifiedIndependentReceiptFact(getVerifiedByReceiptId(tenantId, receiptId, entryType, sourceSnapshotHash),
                creationVerifier.requireIndependentCreation(tenantId, receiptId));
    }

    @Override
    @Transactional(readOnly = true)
    public MesIndependentBatchPrerequisiteReceipt getVerifiedByReceiptId(Long tenantId, String receiptId,
                                                                         String entryType, String sourceSnapshotHash) {
        return verify(new MesIndependentBatchPrerequisiteReceiptVerifyCommand()
                .setReceiptId(receiptId)
                .setEntryType(entryType)
                .setSourceSnapshotHash(sourceSnapshotHash), tenantId);
    }

    @Override
    @Transactional
    public MesIndependentBatchPrerequisiteReceipt revoke(MesIndependentBatchPrerequisiteReceiptRevokeCommand command,
                                                         Long tenantId, Long actorUserId) {
        requireBackendIdentity(tenantId, actorUserId);
        if (command == null || blank(command.getReceiptId()) || blank(command.getReason())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_PREREQUISITE_MISSING);
        }
        creationWriter.acquireLedgerLock();
        MesIndependentBatchPrerequisiteReceiptDO data = store.selectByReceiptId(tenantId, command.getReceiptId());
        if (data == null) {
            MesIndependentBatchPrerequisiteReceiptDO unscoped = store.selectByReceiptIdUnscoped(command.getReceiptId());
            if (unscoped != null && !tenantId.equals(unscoped.getTenantId())) {
                throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH);
            }
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_PREREQUISITE_MISSING);
        }
        MesEdhrSourceCreationEvidence creation = creationVerifier.requireIndependentCreation(tenantId, data.getReceiptId());
        if (STATUS_REVOKED.equals(data.getStatus())) {
            String priorReason = data.getRevocationReason();
            data.setRevocationReason(command.getReason());
            String requestHash = MesEdhrSourceCreationCodec.encodeRevokeRequestV1(creationWriter.revokeRequest(data, actorUserId)).sha256();
            data.setRevocationReason(priorReason);
            var bindings = creationBindings.readBySourceRow(tenantId, "INDEPENDENT_REVOKE_V1", data.getId());
            if (bindings.size() != 1 || !bindings.get(0).fact().requestHash().equals(requestHash))
                throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT);
            return toReceipt(data);
        }
        if (!STATUS_ISSUED.equals(data.getStatus()) || data.getRevokedAt() != null)
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED);
        data.setStatus(STATUS_REVOKED).setRevokedAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS))
                .setRevocationReason(command.getReason());
        store.update(data);
        creationWriter.revoke(data, actorUserId, creation);
        return toReceipt(data);
    }

    private MesIndependentBatchPrerequisiteReceipt buildReceipt(
            MesIndependentBatchPrerequisiteReceiptIssueCommand command, Long tenantId, Long actorUserId,
            String resolvedIssuerSystem, LocalDateTime issuedAt) {
        Duration validity = switch (command.getEntryType()) {
            case "MANUAL" -> Duration.ofHours(24);
            case "SCHEDULED" -> Duration.ofHours(48);
            case "PQC_INDEPENDENT" -> Duration.ofHours(4);
            default -> throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        };
        return new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId(UUID.randomUUID().toString())
                .setTenantId(tenantId).setEntryType(command.getEntryType())
                .setWorkOrderId(command.getWorkOrderId()).setWorkOrderCode(command.getWorkOrderCode())
                .setRouteId(command.getRouteId()).setRouteVersionId(command.getRouteVersionId())
                .setRouteVersion(command.getRouteVersion()).setBatchCode(command.getBatchCode())
                .setSourceRelationId(command.getSourceRelationId())
                .setSourceRelationVersion(command.getSourceRelationVersion())
                .setSourceRelationSnapshotHash(command.getSourceRelationSnapshotHash())
                .setSourceObjectType(command.getSourceObjectType()).setSourceObjectId(command.getSourceObjectId())
                .setMaterialSourceType(command.getMaterialSourceType()).setMaterialSourceId(command.getMaterialSourceId())
                .setSourceContextHash(command.getSourceContextHash()).setSourceSnapshotHash(command.getSourceSnapshotHash())
                .setBusinessReason(command.getBusinessReason()).setIssuerSystem(resolvedIssuerSystem)
                .setIssuerUserId(actorUserId).setIssuerUserRole("BACKEND_CONTROLLED")
                .setIssuedAt(issuedAt).setExpiresAt(issuedAt.plus(validity))
                .setCredentialVersion(1L).setStatus(STATUS_ISSUED)
                .setAuditEventId("independent-receipt-issue:" + UUID.randomUUID())
                .setIdempotencyKey(command.getIdempotencyKey()).setSourceEvidence(command.getSourceEvidence());
    }

    private void validateIssueCommand(MesIndependentBatchPrerequisiteReceiptIssueCommand command) {
        if (command == null || !ENTRY_TYPES.contains(command.getEntryType())
                || command.getWorkOrderId() == null || blank(command.getWorkOrderCode())
                || command.getRouteId() == null || command.getRouteVersionId() == null
                || blank(command.getRouteVersion()) || blank(command.getBatchCode())
                || blank(command.getSourceRelationId()) || blank(command.getSourceRelationVersion())
                || blank(command.getSourceRelationSnapshotHash()) || blank(command.getSourceObjectType())
                || blank(command.getSourceObjectId()) || blank(command.getMaterialSourceType())
                || blank(command.getMaterialSourceId()) || blank(command.getSourceContextHash())
                || blank(command.getSourceSnapshotHash()) || blank(command.getBusinessReason())
                || blank(command.getIdempotencyKey()) || !validEvidence(command.getSourceEvidence())) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        }
    }

    private boolean validEvidence(List<MesBatchExecutionSourceEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) return false;
        return evidence.stream().allMatch(item -> item != null && !blank(item.getSourceType())
                && !blank(item.getSourceId()) && !blank(item.getSourceVersion())
                && !blank(item.getSourceSnapshotHash()) && !blank(item.getPayloadHash())
                && !blank(item.getSignature()) && !blank(item.getRelationStatus()));
    }

    private MesIndependentBatchPrerequisiteReceiptDO toDO(MesIndependentBatchPrerequisiteReceipt value,
                                                           String canonicalPayload) {
        try {
            return new MesIndependentBatchPrerequisiteReceiptDO()
                    .setReceiptId(value.getReceiptId()).setTenantId(value.getTenantId()).setEntryType(value.getEntryType())
                    .setWorkOrderId(value.getWorkOrderId()).setWorkOrderCode(value.getWorkOrderCode())
                    .setRouteId(value.getRouteId()).setRouteVersionId(value.getRouteVersionId()).setRouteVersion(value.getRouteVersion())
                    .setBatchCode(value.getBatchCode()).setSourceRelationId(value.getSourceRelationId())
                    .setSourceRelationVersion(value.getSourceRelationVersion()).setSourceRelationSnapshotHash(value.getSourceRelationSnapshotHash())
                    .setSourceObjectType(value.getSourceObjectType()).setSourceObjectId(value.getSourceObjectId())
                    .setMaterialSourceType(value.getMaterialSourceType()).setMaterialSourceId(value.getMaterialSourceId())
                    .setSourceContextHash(value.getSourceContextHash()).setSourceSnapshotHash(value.getSourceSnapshotHash())
                    .setBusinessReason(value.getBusinessReason()).setIssuerSystem(value.getIssuerSystem())
                    .setIssuerUserId(value.getIssuerUserId()).setIssuerUserRole(value.getIssuerUserRole())
                    .setIssuedAt(value.getIssuedAt()).setExpiresAt(value.getExpiresAt()).setRevokedAt(value.getRevokedAt())
                    .setRevocationReason(value.getRevocationReason()).setCredentialVersion(value.getCredentialVersion())
                    .setStatus(value.getStatus()).setCanonicalPayload(canonicalPayload)
                    .setSourceEvidenceJson(new com.fasterxml.jackson.databind.ObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS).writeValueAsString(value.getSourceEvidence()))
                    .setReceiptHash(value.getReceiptHash()).setPayloadHash(value.getPayloadHash())
                    .setSignature(value.getSignature()).setAuditEventId(value.getAuditEventId())
                    .setIdempotencyKey(value.getIdempotencyKey());
        } catch (Exception e) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        }
    }

    private MesIndependentBatchPrerequisiteReceipt toReceipt(MesIndependentBatchPrerequisiteReceiptDO value) {
        try {
            return new MesIndependentBatchPrerequisiteReceipt().setReceiptId(value.getReceiptId()).setTenantId(value.getTenantId())
                    .setEntryType(value.getEntryType()).setWorkOrderId(value.getWorkOrderId()).setWorkOrderCode(value.getWorkOrderCode())
                    .setRouteId(value.getRouteId()).setRouteVersionId(value.getRouteVersionId()).setRouteVersion(value.getRouteVersion())
                    .setBatchCode(value.getBatchCode()).setSourceRelationId(value.getSourceRelationId())
                    .setSourceRelationVersion(value.getSourceRelationVersion()).setSourceRelationSnapshotHash(value.getSourceRelationSnapshotHash())
                    .setSourceObjectType(value.getSourceObjectType()).setSourceObjectId(value.getSourceObjectId())
                    .setMaterialSourceType(value.getMaterialSourceType()).setMaterialSourceId(value.getMaterialSourceId())
                    .setSourceContextHash(value.getSourceContextHash()).setSourceSnapshotHash(value.getSourceSnapshotHash())
                    .setBusinessReason(value.getBusinessReason()).setIssuerSystem(value.getIssuerSystem())
                    .setIssuerUserId(value.getIssuerUserId()).setIssuerUserRole(value.getIssuerUserRole())
                    .setIssuedAt(value.getIssuedAt()).setExpiresAt(value.getExpiresAt()).setRevokedAt(value.getRevokedAt())
                    .setRevocationReason(value.getRevocationReason()).setCredentialVersion(value.getCredentialVersion())
                    .setStatus(value.getStatus()).setCanonicalPayload(value.getCanonicalPayload())
                    .setReceiptHash(value.getReceiptHash()).setPayloadHash(value.getPayloadHash())
                    .setSignature(value.getSignature()).setAuditEventId(value.getAuditEventId()).setIdempotencyKey(value.getIdempotencyKey())
                    .setSourceEvidence(JsonUtils.getObjectMapper().readValue(value.getSourceEvidenceJson(),
                            JsonUtils.getObjectMapper().getTypeFactory().constructCollectionType(List.class, MesBatchExecutionSourceEvidence.class)));
        } catch (Exception e) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        }
    }

    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_SIGNATURE_INVALID);
        }
    }

    private void requireBackendIdentity(Long tenantId, Long actorUserId) {
        if (tenantId == null || tenantId <= 0 || actorUserId == null || actorUserId <= 0) {
            throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_PERMISSION_DENIED);
        }
    }

    private String requireConfigured(String value) {
        if (blank(value)) throw exception(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID);
        return value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
