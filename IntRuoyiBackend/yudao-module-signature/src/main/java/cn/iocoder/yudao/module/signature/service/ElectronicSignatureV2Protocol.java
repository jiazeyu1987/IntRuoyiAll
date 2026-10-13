package cn.iocoder.yudao.module.signature.service;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.signature.api.dto.AuthenticatedSignatureRequest;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ElectronicSignatureV2Protocol {
    private ElectronicSignatureV2Protocol() { }
    public static String canonical(Object value) { return ElectronicSignatureJsonCanonicalizer.canonicalize(JsonUtils.toJsonString(value)); }
    public static String hash(Object value) { return DigestUtil.sha256Hex(canonical(value)); }
    public static String requestHash(AuthenticatedSignatureRequest request, AuthenticatedSigner signer) {
        var c = request.command(); var envelope = new LinkedHashMap<String, Object>();
        envelope.put("commandHashVersion", "V2"); envelope.put("tenantId", signer.tenantId().toString()); envelope.put("operatorId", signer.operatorId().toString()); envelope.put("identityDomain", signer.domain()); envelope.put("actorId", signer.actorId().toString()); envelope.put("moduleCode", c.moduleCode()); envelope.put("actionCode", c.actionCode()); envelope.put("subjectType", c.subjectType()); envelope.put("stableSubjectKey", request.stableSubjectKey()); envelope.put("expectedSubjectVersion", c.expectedSubjectVersion()); envelope.put("reason", c.reason()); envelope.put("idempotencyKey", c.idempotencyKey()); envelope.put("businessOccurredAt", c.businessOccurredAt()); envelope.put("businessTimeZone", c.businessTimeZone());
        return hash(envelope);
    }
    public static Map<String, Object> evidence(ElectronicSignatureRecordDO r, JsonNode content) {
        var values = new LinkedHashMap<String, Object>();
        values.put("evidenceFormatVersion", "V2"); values.put("tenantId", r.getTenantId().toString()); values.put("actorIdentity", content.get("signatureIdentity")); values.put("moduleCode", r.getModuleCode()); values.put("actionCode", r.getActionCode()); values.put("subjectType", r.getSubjectType()); values.put("subjectId", r.getSubjectId()); values.put("subjectVersion", r.getSubjectVersion()); values.put("meaningCode", r.getMeaningCode()); values.put("meaningLabel", r.getMeaningLabel()); values.put("reason", r.getReason()); values.put("signatureTime", content.get("signatureTime")); values.put("timeEvidenceId", r.getTimeEvidenceId()); values.put("authenticationMethod", r.getAuthenticationMethod()); values.put("contentHash", r.getContentHash()); values.put("beforeContentHash", r.getBeforeContentHash()); values.put("afterContentHash", r.getAfterContentHash()); values.put("beforeContentJson", tree(r.getBeforeContentJson())); values.put("afterContentJson", tree(r.getAfterContentJson())); values.put("fieldDiffJson", tree(r.getFieldDiffJson())); values.put("processInstanceId", r.getProcessInstanceId()); values.put("taskId", r.getTaskId()); values.put("nodeCode", r.getNodeCode()); values.put("nodeOrder", r.getNodeOrder()); values.put("algorithm", "SHA-256"); values.put("keyVersion", "digest-format-v2"); values.put("policyVersion", r.getPolicyVersion()); values.put("captureStatus", "RECORDED");
        return values;
    }
    private static JsonNode tree(String value) { return value == null ? null : JsonUtils.parseTree(value); }
}
