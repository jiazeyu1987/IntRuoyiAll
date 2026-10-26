package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SignatureCreationVerificationService {
    @Resource
    private GxpAuditEventMapper auditEventMapper;

    public SignatureCreationVerification verifySignatureCreation(SignatureAuditExpectation expected) {
        if (!Objects.equals(expected.tenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalArgumentException("Signature audit expectation belongs to another tenant");
        }
        var protocol = SignatureCreationProtocolRegistry.require(expected.evidenceFormatVersion());
        if (GxpAuditDevelopmentMode.disabled()) return result(GxpAuditDevelopmentMode.STATUS, "GXP_AUDIT_DEVELOPMENT_DISABLED", null);
        // Query exceptions deliberately propagate; missing evidence is only a successful empty read.
        var events = auditEventMapper.selectSignatureCreationEvents(expected.tenantId(), expected.signatureId().toString());
        if (events == null) throw new IllegalStateException("Signature creation audit query returned null");
        if (events.isEmpty()) return result("MISSING", "AUDIT_LINK_MISSING", null);
        if (events.size() != 1) return result("MISMATCH", "AUDIT_LINK_DUPLICATE", null);
        GxpAuditEventDO audit = events.get(0);
        if (!integrity(audit, expected.tenantId())) return result("MISMATCH", "AUDIT_INTEGRITY_FAILED", audit == null ? null : audit.getId());
        if (!Objects.equals(protocol.operationId(), audit.getOperationId())
                || !"SIGNATURE".equals(audit.getDomain()) || !"SIGNATURE_RECORD".equals(audit.getSubjectType())
                || !"CREATE".equals(audit.getAction())
                || !Objects.equals(protocol.sourceType(), audit.getSourceType())
                || !Objects.equals(protocol.sourceLocator(), audit.getSourceLocator())
                || !Objects.equals(expected.signatureId().toString(), audit.getSignatureRecordId())
                || !Objects.equals(expected.contentHash(), audit.getSignatureContentHash())
                || !Objects.equals(expected.subjectId(), audit.getSubjectId())
                || !Objects.equals(expected.subjectVersion(), audit.getSubjectVersion())
                || !Objects.equals(expected.subjectVersion(), audit.getBeforeObjectVersion())
                || !Objects.equals(expected.subjectVersion(), audit.getAfterObjectVersion())
                || !Objects.equals(expected.reason(), audit.getReason())
                || !Objects.equals(expected.operatorId(), audit.getActorId())
                || !Objects.equals(expected.idempotencyKey(), audit.getIdempotencyKey())
                || !"NO_SIGNATURE_RECORD".equals(audit.getBeforeState())
                || !"ELECTRONIC_SIGNATURE_RECORDED".equals(audit.getAfterState())
                || !"SIGNATURE_RECORD_CREATE".equals(audit.getReasonCode()) || !"USER".equals(audit.getReasonSource())
                || !Objects.equals(tree("{}"), tree(audit.getBeforeStateJson()))
                || !Objects.equals(tree(JsonUtils.toJsonString(expected.afterState())), tree(audit.getAfterStateJson()))
                || !Objects.equals(tree("[]"), tree(audit.getRelationManifestJson()))
                || !Objects.equals(tree("[]"), tree(audit.getEvidenceManifestJson()))
                || !actorMatches(audit.getAuthenticatedActorJson(), expected)
                || !actorMatches(audit.getPerformedByJson(), expected)) {
            return result("MISMATCH", "AUDIT_BINDING_MISMATCH", audit.getId());
        }
        return new SignatureCreationVerification("CONSISTENT", List.of(), audit.getId());
    }

    private static boolean actorMatches(String json, SignatureAuditExpectation expected) {
        return GxpCreationAuditVerificationService.originalActorMatches(json, expected.operatorId(), expected.tenantId());
    }

    private static boolean integrity(GxpAuditEventDO audit, Long tenantId) {
        return GxpCreationAuditVerificationService.originalIntegrity(audit, tenantId);
    }

    private static JsonNode tree(String json) {
        return GxpCreationAuditVerificationService.originalTree(json);
    }

    private static SignatureCreationVerification result(String status, String reason, Long eventId) {
        return new SignatureCreationVerification(status, List.of(reason), eventId);
    }
}
