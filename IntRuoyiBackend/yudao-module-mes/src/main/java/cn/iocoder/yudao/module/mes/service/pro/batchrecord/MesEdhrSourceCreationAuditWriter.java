package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Writes the original owner hash first, then a real event and immutable binding in the caller's transaction. */
@Service
public class MesEdhrSourceCreationAuditWriter {
    private final GxpAuditService audit;
    private final MesEdhrSourceCreationBindingStore bindings;
    private final MesEdhrSourceOwnerIntegrity integrity;
    private final GxpCreationAuditVerificationService events;

    public MesEdhrSourceCreationAuditWriter(GxpAuditService audit, MesEdhrSourceCreationBindingStore bindings,
            MesEdhrSourceOwnerIntegrity integrity, GxpCreationAuditVerificationService events) {
        this.audit = audit; this.bindings = bindings; this.integrity = integrity; this.events = events;
    }

    @jakarta.annotation.Resource private cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi approvalAuthority;
    public void acquireLedgerLock() {
        audit.acquireLedgerLock();
        approvalAuthority.lockTenantApprovalAuthority(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
    }

    public ObjectNode issueRequest(MesIndependentBatchPrerequisiteReceiptDO row) { return integrity.independentRequest(row); }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void issue(MesIndependentBatchPrerequisiteReceiptDO row) {
        ObjectNode request = issueRequest(row);
        var encoded = MesEdhrSourceCreationCodec.encodeIssueRequestV1(request);
        ObjectNode state = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("stateKind", "ISSUE_STATE_V1").put("sourceRowId", row.getId()).put("receiptId", row.getReceiptId())
                .put("credentialVersion", row.getCredentialVersion()).put("issuerUserRole", row.getIssuerUserRole())
                .put("issuedAt", MesEdhrSourceCreationCodec.formatSecond(row.getIssuedAt()))
                .put("expiresAt", MesEdhrSourceCreationCodec.formatSecond(row.getExpiresAt()))
                .put("status", "ISSUED").putNull("revokedAt").putNull("revocationReason")
                .put("legacyOwnerAuditToken", row.getAuditEventId()).put("canonicalPayloadRaw", row.getCanonicalPayload())
                .put("sourceEvidenceJsonRaw", row.getSourceEvidenceJson()).put("payloadHash", row.getPayloadHash())
                .put("signature", row.getSignature()).put("receiptHash", row.getReceiptHash())
                .put("requestHashVersion", "INDEPENDENT_ISSUE_REQUEST_V1").put("requestHash", encoded.sha256());
        state.set("request", request);
        String version = row.getCredentialVersion().toString();
        String key = MesEdhrSourceCreationCodec.issueAuditKey(row.getTenantId(), row.getEntryType(), row.getIdempotencyKey());
        append(row.getTenantId(), "INDEPENDENT_ISSUE_V1", row.getId(), row.getReceiptId(), version, row.getReceiptHash(),
                "INDEPENDENT_ISSUE_REQUEST_V1", encoded.sha256(), "mes.independent-batch-prerequisite-receipt.issue",
                "MesIndependentBatchPrerequisiteReceiptServiceImpl#issue", "INDEPENDENT_RECEIPT:", "ABSENT", "ISSUED", "{}",
                MesEdhrSourceCreationCodec.encodeIssueStateV1(state).canonicalJson(), row.getBusinessReason(),
                "MES_INDEPENDENT_RECEIPT_ISSUE", "USER", key,
                List.of(source("INDEPENDENT_BATCH_PREREQUISITE_RECEIPT", row.getId(), version, row.getReceiptHash())),
                List.of(new GxpAuditEvidence("INDEPENDENT_PREREQUISITE_RECEIPT", row.getReceiptId(), version, row.getReceiptHash(), "CREATION")));
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void revoke(MesIndependentBatchPrerequisiteReceiptDO row, Long actor, MesEdhrSourceCreationEvidence creation) {
        var issue = events.requireVerifiedEvent(row.getTenantId(), creation.creationEventId());
        if (!creation.creationEventHash().equals(issue.eventHash())) throw new IllegalStateException("SOURCE_BINDING_MISMATCH");
        ObjectNode request = revokeRequest(row, actor);
        var encoded = MesEdhrSourceCreationCodec.encodeRevokeRequestV1(request);
        ObjectNode state = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("stateKind", "REVOKE_STATE_V1").put("creationEventId", creation.creationEventId())
                .put("creationEventHash", creation.creationEventHash()).put("status", "REVOKED")
                .put("revokedAt", MesEdhrSourceCreationCodec.formatSecond(row.getRevokedAt()))
                .put("revocationReason", row.getRevocationReason()).put("requestHashVersion", "INDEPENDENT_REVOKE_REQUEST_V1")
                .put("requestHash", encoded.sha256());
        state.set("creationState", MesEdhrSourceCreationCodec.decodeIssueStateV1(issue.columns().afterStateJson().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        state.set("revocationRequest", request);
        String version = row.getCredentialVersion().toString();
        String key = MesEdhrSourceCreationCodec.revokeAuditKey(row.getTenantId(), row.getReceiptId(), row.getCredentialVersion());
        append(row.getTenantId(), "INDEPENDENT_REVOKE_V1", row.getId(), row.getReceiptId(), version, row.getReceiptHash(),
                "INDEPENDENT_REVOKE_REQUEST_V1", encoded.sha256(), "mes.independent-batch-prerequisite-receipt.revoke",
                "MesIndependentBatchPrerequisiteReceiptServiceImpl#revoke", "INDEPENDENT_RECEIPT:", "ISSUED", "REVOKED", issue.columns().afterStateJson(),
                MesEdhrSourceCreationCodec.encodeRevokeStateV1(state).canonicalJson(), row.getRevocationReason(),
                "MES_INDEPENDENT_RECEIPT_REVOKE", "USER", key,
                List.of(source("INDEPENDENT_BATCH_PREREQUISITE_RECEIPT", row.getId(), version, row.getReceiptHash()),
                        new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", creation.creationEventId().toString(), null, creation.creationEventHash())), List.of());
    }

    public ObjectNode revokeRequest(MesIndependentBatchPrerequisiteReceiptDO row, Long actor) {
        return JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("requestKind", "INDEPENDENT_REVOKE_REQUEST_V1").put("tenantId", row.getTenantId())
                .put("authenticatedActor", actor).put("receiptId", row.getReceiptId())
                .put("credentialVersion", row.getCredentialVersion()).put("reason", row.getRevocationReason());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void material(MesProEdhrMaterialGateReceiptDO row) {
        ObjectNode request = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("requestKind", "MATERIAL_READY_REQUEST_V1").put("tenantId", row.getTenantId())
                .put("batchExecutionId", row.getBatchExecutionId()).put("version", row.getVersion()).put("issuedBy", row.getIssuedBy())
                .put("sourceSnapshotHash", row.getSourceSnapshotHash()).put("manifestHash", row.getManifestHash())
                .put("materialVersionSetHash", row.getMaterialVersionSetHash());
        request.set("materialTypeKeys", integrity.materialTypes(row));
        var encoded = MesEdhrSourceCreationCodec.encodeMaterialReadyRequestV1(request);
        ObjectNode state = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("stateKind", "MATERIAL_READY_STATE_V1").put("sourceRowId", row.getId()).put("receiptId", row.getReceiptId())
                .put("gateStatus", row.getGateStatus()).put("materialTypeKeysJsonRaw", row.getMaterialTypeKeysJson())
                .put("legacyOwnerAuditToken", row.getAuditEventId()).put("receiptHash", row.getReceiptHash())
                .put("requestHashVersion", "MATERIAL_READY_REQUEST_V1").put("requestHash", encoded.sha256());
        state.set("request", request);
        String version = row.getVersion().toString();
        String key = MesEdhrSourceCreationCodec.materialCreateAuditKey(row.getTenantId(), row.getBatchExecutionId(), row.getVersion());
        append(row.getTenantId(), "MATERIAL_READY_V1", row.getId(), row.getReceiptId(), version, row.getReceiptHash(),
                "MATERIAL_READY_REQUEST_V1", encoded.sha256(), "mes.edhr.material-gate.receipt.create", "MesReleaseMaterialGateReceiptPortImpl#persistReady",
                "MATERIAL_GATE_RECEIPT:", "ABSENT", "MATERIALS_READY", "{}", MesEdhrSourceCreationCodec.encodeMaterialReadyStateV1(state).canonicalJson(),
                "四材料门禁就绪回执创建", "MES_EDHR_MATERIALS_READY", "SYSTEM", key,
                List.of(source("EDHR_MATERIAL_GATE_RECEIPT", row.getId(), version, row.getReceiptHash())),
                List.of(new GxpAuditEvidence("MATERIAL_GATE_RECEIPT", row.getReceiptId(), version, row.getReceiptHash(), "CREATION")));
    }

    private void append(Long tenant, String protocol, Long row, String receipt, String version, String hash,
            String requestVersion, String requestHash, String operation, String method, String prefix, String before, String after,
            String beforeJson, String afterJson, String reason, String reasonCode, String reasonSource, String key,
            List<GxpAuditRelation> links, List<GxpAuditEvidence> evidence) {
        MesEdhrSourceCreationBindingStore.tenant(tenant); MesEdhrSourceCreationBindingStore.positive(row);
        var result = audit.append(GxpAuditCommand.builder().operationId(operation).subjectId(prefix + receipt).subjectVersion(version)
                .reason(reason).reasonCode(reasonCode).reasonSource(reasonSource).idempotencyKey(key).requestId(key)
                .sourceType("SERVICE_METHOD").sourceLocator("cn.iocoder.yudao.module.mes.service.pro.batchrecord." + method)
                .beforeState(GxpAuditStateEnvelope.builder().state(before).objectVersion(version).canonicalJson(beforeJson).build())
                .afterState(GxpAuditStateEnvelope.builder().state(after).objectVersion(version).canonicalJson(afterJson).build())
                .links(links).evidences(evidence).build());
        if (result == null || result.replayed() || result.eventId() == null || result.eventId() <= 0)
            throw new IllegalStateException("SOURCE_CREATION_APPEND_FAILED");
        bindings.insert(new MesEdhrSourceCreationBindingStore.BindingFact(tenant, protocol, row, receipt, version, hash,
                requestVersion, requestHash, result.eventId(), result.eventHash()));
    }

    private static GxpAuditRelation source(String type, Long row, String version, String hash) {
        return new GxpAuditRelation("SOURCE", type, row.toString(), version, hash);
    }
}
