package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.SignatureAuditExpectation;
import cn.iocoder.yudao.module.system.service.gxpaudit.SignatureCreationVerificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

/** One V1 verification core operating on a single immutable copy of persisted facts. */
@Service
public class ElectronicSignatureVerifiedFactService {
    private static final String SCOPE = "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT";
    private static final ObjectMapper FACT_READER = new ObjectMapper();
    private final ElectronicSignatureRecordMapper signatureRecordMapper;
    private final SignatureCreationVerificationService signatureCreationVerificationService;

    public ElectronicSignatureVerifiedFactService(ElectronicSignatureRecordMapper signatureRecordMapper,
            SignatureCreationVerificationService signatureCreationVerificationService) {
        this.signatureRecordMapper = signatureRecordMapper;
        this.signatureCreationVerificationService = signatureCreationVerificationService;
    }

    public ElectronicSignatureVerificationDTO verifyEvidence(Long signatureId) {
        return evaluate(load(signatureId)).verification();
    }

    public VerifiedSignatureFact requireVerifiedFact(Long signatureId) {
        FrozenFact fact = load(signatureId);
        // This stronger gate belongs only to token issuance; preserve the original diagnostic path.
        if (!Objects.equals(signatureId, fact.id()))
            throw exception(ESIGN_COMMAND_INVALID, "签名事实编号与请求不一致");
        Evaluation evaluation = evaluate(fact);
        ElectronicSignatureVerificationDTO verification = evaluation.verification();
        if (!"VALID".equals(verification.verificationStatus())
                || !"CONSISTENT".equals(verification.digestStatus())
                || !"CONSISTENT".equals(verification.timeConsistencyStatus())
                || !cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.auditBindingAccepted(verification.auditBindingStatus())
                || !Objects.equals(verification.storedContentHash(), verification.calculatedContentHash())
                || !Objects.equals(verification.storedEvidenceHash(), verification.calculatedEvidenceHash())
                || (!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() && !positive(evaluation.auditEventId())))
            throw exception(ESIGN_COMMAND_INVALID, "签名事实未通过完整核验");
        return new VerifiedSignatureFact(fact, verification, cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() ? -fact.id() : evaluation.auditEventId());
    }

    private FrozenFact load(Long signatureId) {
        positiveId(signatureId);
        var record = signatureRecordMapper.selectById(signatureId);
        if (record == null || !Objects.equals(record.getTenantId(), TenantContextHolder.getRequiredTenantId()))
            throw exception(ESIGN_COMMAND_INVALID, "签名记录不存在");
        return freeze(record);
    }

    private static FrozenFact freeze(ElectronicSignatureRecordDO record) {
        return new FrozenFact(
                record.getTenantId(),
                record.getId(),
                record.getModuleCode(),
                record.getActionCode(),
                record.getSubjectType(),
                record.getSubjectId(),
                record.getSubjectVersion(),
                record.getActorId(),
                record.getMeaningCode(),
                record.getMeaningLabel(),
                record.getReason(),
                record.getSignedAt(),
                record.getTimeEvidenceId(),
                record.getAuthenticationMethod(),
                record.getContentHash(),
                record.getBeforeContentHash(),
                record.getAfterContentHash(),
                record.getCanonicalContentJson(),
                record.getBeforeContentJson(),
                record.getAfterContentJson(),
                record.getFieldDiffJson(),
                record.getEvidenceHash(),
                record.getAlgorithm(),
                record.getKeyVersion(),
                record.getPolicyVersion(),
                record.getVerificationStatus(),
                record.getIdempotencyKey(),
                record.getCommandHash(),
                record.getProcessInstanceId(),
                record.getTaskId(),
                record.getNodeCode(),
                record.getNodeOrder(), record.getEvidenceFormatVersion(), record.getCommandHashVersion());
    }

    private Evaluation evaluate(FrozenFact record) {
        if ("V2".equals(record.evidenceFormatVersion())) return evaluateV2(record);
        if (record.evidenceFormatVersion() != null || record.commandHashVersion() != null)
            throw exception(ESIGN_COMMAND_INVALID, "未知签名证据协议");
        var reasons = new ArrayList<String>();
        JsonNode content = readFact(record.canonicalContentJson());
        // This read path supports the existing V1 storage contract only. Business content is
        // not a version envelope; same-named business keys cannot select a signature protocol.
        // A future formal version requires its persisted column and registered creation source.
        boolean formatSupported = content != null && content.isObject();
        String format = formatSupported ? "V1" : "UNSUPPORTED";
        String calculatedContent = null, calculatedEvidence = null;
        String digest;
        if (StrUtil.isBlank(record.canonicalContentJson())) {
            digest = "MISSING"; reasons.add("CONTENT_MISSING");
        } else if (!formatSupported || !"SHA-256".equals(record.algorithm())
                || !"system-local-v1".equals(record.keyVersion())) {
            digest = "MISMATCH"; reasons.add("FORMAT_UNSUPPORTED");
        } else {
            calculatedContent = DigestUtil.sha256Hex(ElectronicSignatureJsonCanonicalizer.canonicalize(record.canonicalContentJson()));
            if (readOptionalFact(record.beforeContentJson()) && readOptionalFact(record.afterContentJson())
                    && readOptionalFact(record.fieldDiffJson())) {
                calculatedEvidence = DigestUtil.sha256Hex(evidencePayload(record, calculatedContent));
            }
            digest = StrUtil.hasBlank(record.contentHash(), record.evidenceHash()) ? "MISSING"
                    : Objects.equals(record.contentHash(), calculatedContent)
                        && Objects.equals(record.evidenceHash(), calculatedEvidence) ? "CONSISTENT" : "MISMATCH";
            if (!"CONSISTENT".equals(digest)) reasons.add("MISSING".equals(digest) ? "DIGEST_MISSING" : "DIGEST_MISMATCH");
        }
        String time = timeConsistency(record, reasons);
        if (!"VALID".equals(record.verificationStatus())
                || StrUtil.hasBlank(record.moduleCode(), record.actionCode(), record.subjectType(),
                        record.subjectId(), record.subjectVersion(), record.meaningCode(), record.meaningLabel(),
                        record.reason(), record.policyVersion(), record.idempotencyKey())) {
            digest = "MISMATCH"; reasons.add("SIGNATURE_FACT_INVALID");
        }
        Long operator = operator(record, content, reasons);
        String audit;
        Long auditEventId = null;
        if (formatSupported && operator != null) {
            var result = signatureCreationVerificationService.verifySignatureCreation(new SignatureAuditExpectation(
                    record.tenantId(), record.id(), "V1", record.subjectType() + ":" + record.subjectId(),
                    record.subjectVersion(), operator, record.reason(), record.contentHash(),
                    record.idempotencyKey() == null ? null : "SIGNATURE:" + DigestUtil.sha256Hex(record.idempotencyKey()),
                    afterState(record)));
            audit = result.auditBindingStatus(); reasons.addAll(result.reasonCodes());
            auditEventId = result.auditEventId();
        } else {
            audit = StrUtil.isBlank(record.canonicalContentJson()) ? "MISSING" : "MISMATCH";
            if (!formatSupported && !StrUtil.isBlank(record.canonicalContentJson())) reasons.add("FORMAT_UNSUPPORTED");
        }
        String status = List.of(digest, time, audit).contains("MISMATCH") ? "MISMATCH"
                : List.of(digest, time, audit).contains("MISSING") ? "INCOMPLETE" : "VALID";
        if (frozenName(content) == null) reasons.add("IDENTITY_SNAPSHOT_MISSING");
        var verification = new ElectronicSignatureVerificationDTO(record.id(), status, record.contentHash(), calculatedContent,
                record.evidenceHash(), calculatedEvidence, record.algorithm(), record.keyVersion(), SCOPE,
                digest, time, record.timeEvidenceId() != null && record.timeEvidenceId().startsWith("SERVER_CLOCK:")
                    ? "SERVER_CLOCK" : "UNKNOWN", "NOT_BOUND", audit, reasons.stream().distinct().toList(),
                LocalDateTime.now(ZoneId.of("Asia/Shanghai")), format);
        return new Evaluation(verification, auditEventId);
    }

    private static String timeConsistency(FrozenFact record, List<String> reasons) {
        if (record.signedAt() == null || StrUtil.isBlank(record.timeEvidenceId())) {
            reasons.add("TIME_EVIDENCE_MISSING"); return "MISSING";
        }
        if (!record.timeEvidenceId().startsWith("SERVER_CLOCK:")) {
            reasons.add("TIME_EVIDENCE_INVALID"); return "MISSING";
        }
        try {
            LocalDateTime original = LocalDateTime.parse(record.timeEvidenceId().substring("SERVER_CLOCK:".length()));
            if (original.getNano() != 0 || record.signedAt().getNano() != 0 || !original.equals(record.signedAt())) {
                reasons.add("TIME_VALUE_MISMATCH"); return "MISMATCH";
            }
            return "CONSISTENT";
        } catch (DateTimeParseException ex) {
            reasons.add("TIME_EVIDENCE_INVALID"); return "MISSING";
        }
    }

    private static Long operator(FrozenFact record, JsonNode content, List<String> reasons) {
        JsonNode identity = content == null ? null : content.get("signatureIdentity");
        if (identity == null || identity.isNull()) {
            if ("SESSION_PLUS_PASSWORD".equals(record.authenticationMethod()) && positive(record.actorId())) return record.actorId();
        } else if (identity.isObject()) {
            String domain = text(identity, "domain");
            Long signer = strictId(identity.get("signerId")), tenant = strictId(identity.get("tenantId")), operator = strictId(identity.get("operatorId"));
            if (Objects.equals(record.actorId(), signer) && Objects.equals(record.tenantId(), tenant) && operator != null
                    && ("SYSTEM_USER".equals(domain) && "SESSION_PLUS_SELECTED_USER_PASSWORD".equals(record.authenticationMethod())
                    || "MES_EMPLOYEE_PROFILE".equals(domain) && "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD".equals(record.authenticationMethod()))) return operator;
        }
        reasons.add("IDENTITY_BINDING_MISMATCH"); return null;
    }

    private static Long strictId(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() && node.longValue() > 0 ? node.longValue() : null;
    }
    private static boolean positive(Long id) { return id != null && id > 0; }
    private static void positiveId(Long id) { if (!positive(id)) throw exception(ESIGN_COMMAND_INVALID, "签名编号必须为正整数"); }
    private static String text(JsonNode node, String field) { var value = node.get(field); return value != null && value.isTextual() ? value.textValue() : null; }
    private static JsonNode readFact(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return FACT_READER.readTree(json); }
        catch (JsonProcessingException ex) { return null; }
    }
    private static boolean readOptionalFact(String json) { return StrUtil.isBlank(json) || readFact(json) != null; }
    private static String frozenName(JsonNode content) {
        var identity = content == null ? null : content.get("signatureIdentity");
        var name = identity == null || !identity.isObject() ? null : text(identity, "displayName");
        return StrUtil.isBlank(name) ? null : name;
    }

    private static String evidencePayload(FrozenFact r, String contentHash) {
        // Preserve the V1 byte protocol, including its original minute/second textual representation.
        String originalTime = r.timeEvidenceId() != null && r.timeEvidenceId().startsWith("SERVER_CLOCK:")
                ? r.timeEvidenceId().substring("SERVER_CLOCK:".length()) : String.valueOf(r.signedAt());
        return String.join("|", String.valueOf(r.tenantId()), String.valueOf(r.actorId()), r.moduleCode(),
                r.actionCode(), r.subjectType(), r.subjectId(), r.subjectVersion(), r.meaningCode(),
                r.meaningLabel(), r.reason(), originalTime, r.timeEvidenceId(), r.authenticationMethod(), contentHash,
                StrUtil.nullToEmpty(r.beforeContentHash()), StrUtil.nullToEmpty(r.afterContentHash()),
                StrUtil.nullToEmpty(ElectronicSignatureJsonCanonicalizer.canonicalize(r.beforeContentJson())),
                StrUtil.nullToEmpty(ElectronicSignatureJsonCanonicalizer.canonicalize(r.afterContentJson())),
                StrUtil.nullToEmpty(ElectronicSignatureJsonCanonicalizer.canonicalize(r.fieldDiffJson())),
                "SHA-256", r.keyVersion(), r.policyVersion(), r.verificationStatus());
    }

    private static Map<String, Object> afterState(FrozenFact r) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("signatureId", r.id()); state.put("moduleCode", r.moduleCode()); state.put("actionCode", r.actionCode());
        state.put("subjectType", r.subjectType()); state.put("subjectId", r.subjectId()); state.put("subjectVersion", r.subjectVersion());
        state.put("actorId", r.actorId()); state.put("meaningCode", r.meaningCode()); state.put("meaningLabel", r.meaningLabel());
        state.put("reason", r.reason()); state.put("signedAt", r.signedAt()); state.put("authenticationMethod", r.authenticationMethod());
        state.put("contentHash", r.contentHash()); state.put("beforeContentHash", r.beforeContentHash()); state.put("afterContentHash", r.afterContentHash());
        state.put("evidenceHash", r.evidenceHash()); state.put("policyVersion", r.policyVersion()); state.put("verificationStatus", r.verificationStatus());
        state.put("processInstanceId", r.processInstanceId()); state.put("taskId", r.taskId()); state.put("nodeCode", r.nodeCode()); state.put("nodeOrder", r.nodeOrder());
        return state;
    }

    private record Evaluation(ElectronicSignatureVerificationDTO verification, Long auditEventId) {}

    private record FrozenFact(
            Long tenantId,
            Long id,
            String moduleCode,
            String actionCode,
            String subjectType,
            String subjectId,
            String subjectVersion,
            Long actorId,
            String meaningCode,
            String meaningLabel,
            String reason,
            LocalDateTime signedAt,
            String timeEvidenceId,
            String authenticationMethod,
            String contentHash,
            String beforeContentHash,
            String afterContentHash,
            String canonicalContentJson,
            String beforeContentJson,
            String afterContentJson,
            String fieldDiffJson,
            String evidenceHash,
            String algorithm,
            String keyVersion,
            String policyVersion,
            String verificationStatus,
            String idempotencyKey,
            String commandHash,
            String processInstanceId,
            String taskId,
            String nodeCode,
            Integer nodeOrder, String evidenceFormatVersion, String commandHashVersion) {}

    private Evaluation evaluateV2(FrozenFact frozen) {
        var r = ElectronicSignatureRecordDO.builder().id(frozen.id()).moduleCode(frozen.moduleCode()).actionCode(frozen.actionCode()).subjectType(frozen.subjectType()).subjectId(frozen.subjectId()).subjectVersion(frozen.subjectVersion()).actorId(frozen.actorId()).meaningCode(frozen.meaningCode()).meaningLabel(frozen.meaningLabel()).reason(frozen.reason()).signedAt(frozen.signedAt()).timeEvidenceId(frozen.timeEvidenceId()).authenticationMethod(frozen.authenticationMethod()).contentHash(frozen.contentHash()).beforeContentHash(frozen.beforeContentHash()).afterContentHash(frozen.afterContentHash()).canonicalContentJson(frozen.canonicalContentJson()).beforeContentJson(frozen.beforeContentJson()).afterContentJson(frozen.afterContentJson()).fieldDiffJson(frozen.fieldDiffJson()).evidenceHash(frozen.evidenceHash()).algorithm(frozen.algorithm()).keyVersion(frozen.keyVersion()).policyVersion(frozen.policyVersion()).verificationStatus(frozen.verificationStatus()).idempotencyKey(frozen.idempotencyKey()).commandHash(frozen.commandHash()).processInstanceId(frozen.processInstanceId()).taskId(frozen.taskId()).nodeCode(frozen.nodeCode()).nodeOrder(frozen.nodeOrder()).evidenceFormatVersion(frozen.evidenceFormatVersion()).commandHashVersion(frozen.commandHashVersion()).build();
        r.setTenantId(frozen.tenantId()); var reasons = new ArrayList<String>();
        JsonNode content = readFact(r.getCanonicalContentJson());
        boolean format = content != null && content.isObject() && "V2".equals(r.getCommandHashVersion())
                && "V2".equals(text(content, "evidenceFormatVersion")) && "V2".equals(text(content, "commandHashVersion"))
                && "SHA-256".equals(r.getAlgorithm()) && "digest-format-v2".equals(r.getKeyVersion());
        Long operator = null;
        try {
            if (!format) throw new IllegalArgumentException();
            var identity = content.get("signatureIdentity"); var keys = new java.util.HashSet<String>(); identity.fieldNames().forEachRemaining(keys::add);
            if (!keys.equals(java.util.Set.of("domain", "signerId", "displayName", "operatorId", "tenantId")) || frozenName(content) == null) throw new IllegalArgumentException();
            Long actor = v2Id(identity.get("signerId")), tenant = v2Id(identity.get("tenantId")); operator = v2Id(identity.get("operatorId"));
            if (!Objects.equals(actor, r.getActorId()) || !Objects.equals(tenant, r.getTenantId())) throw new IllegalArgumentException();
            String domain = text(identity, "domain"); String method = "MES_EMPLOYEE_PROFILE".equals(domain) ? "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD" : "SYSTEM_USER".equals(domain) ? actor.equals(operator) ? "SESSION_PLUS_PASSWORD" : "SESSION_PLUS_SELECTED_USER_PASSWORD" : null;
            if (!Objects.equals(method, r.getAuthenticationMethod())) throw new IllegalArgumentException();
            var time = content.get("signatureTime"); keys.clear(); time.fieldNames().forEachRemaining(keys::add);
            if (!keys.equals(java.util.Set.of("localDateTime", "zoneId", "precision", "source")) || !"Asia/Shanghai".equals(text(time,"zoneId")) || !"SECOND".equals(text(time,"precision")) || !"SERVER_CLOCK".equals(text(time,"source")) || !Objects.equals("SERVER_CLOCK:" + text(time,"localDateTime"),r.getTimeEvidenceId())) throw new IllegalArgumentException();
            var image = content.get("signatureImage"); if (image == null || !image.isObject() || image.size()!=2 || !image.has("state") || !image.has("snapshot")) throw new IllegalArgumentException();
            if("NOT_REQUIRED".equals(text(image,"state"))) { if(!image.get("snapshot").isNull()) throw new IllegalArgumentException(); }
            else if("CAPTURED".equals(text(image,"state"))) {
                var snapshot=image.get("snapshot");
                if(!snapshot.isObject() || !snapshot.has("imageId") || !snapshot.has("versionNo") || !snapshot.has("fileId") || !snapshot.has("contentType") || !snapshot.has("fileSize") || !snapshot.has("sha256") || !snapshot.has("imageStatus") || !snapshot.has("verifiedStatus") || !"ACTIVE".equals(text(snapshot,"imageStatus")) || !"VALID".equals(text(snapshot,"verifiedStatus")) || !text(snapshot,"sha256").matches("[0-9a-f]{64}")) throw new IllegalArgumentException();
                for(String idKey:java.util.Set.of("imageId","versionNo","fileId","fileSize")) if(!text(snapshot,idKey).matches("[1-9][0-9]*")) throw new IllegalArgumentException();
            } else throw new IllegalArgumentException();
            var position = content.get("nodePosition"); if (position == null || !position.isObject() || position.size()!=4 || !Objects.equals(text(position,"applicability"),r.getTaskId()==null?"NOT_APPLICABLE":"TASK_CONTEXT") || !Objects.equals(text(position,"taskId"),r.getTaskId()) || !Objects.equals(text(position,"nodeCode"),r.getNodeCode()) || !Objects.equals(text(position,"processInstanceId"),r.getProcessInstanceId())) throw new IllegalArgumentException();
        } catch (RuntimeException e) { format = false; operator = null; reasons.add("FORMAT_VERSION_MISMATCH"); }
        String calculatedContent = content == null ? null : DigestUtil.sha256Hex(ElectronicSignatureJsonCanonicalizer.canonicalize(r.getCanonicalContentJson()));
        String calculatedEvidence = format ? ElectronicSignatureV2Protocol.hash(ElectronicSignatureV2Protocol.evidence(r, content)) : null;
        String digest = format && Objects.equals(calculatedContent,r.getContentHash()) && Objects.equals(calculatedEvidence,r.getEvidenceHash()) ? "CONSISTENT" : "MISMATCH";
        String time = timeConsistency(frozen,reasons); Long auditId = null; String auditStatus = "MISMATCH";
        if (format && operator != null) {
            var audit = signatureCreationVerificationService.verifySignatureCreation(new SignatureAuditExpectation(r.getTenantId(),r.getId(),"V2",r.getSubjectType()+":"+r.getSubjectId(),r.getSubjectVersion(),operator,r.getReason(),r.getContentHash(),"SIGNATURE:"+DigestUtil.sha256Hex(r.getIdempotencyKey()),ElectronicSignatureTransactionExecutor.afterState(r)));
            auditStatus = audit.auditBindingStatus(); auditId = audit.auditEventId(); reasons.addAll(audit.reasonCodes());
        }
        String status = "CONSISTENT".equals(digest) && "CONSISTENT".equals(time) && cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.auditBindingAccepted(auditStatus) ? "VALID" : "MISMATCH";
        return new Evaluation(new ElectronicSignatureVerificationDTO(r.getId(),status,r.getContentHash(),calculatedContent,r.getEvidenceHash(),calculatedEvidence,r.getAlgorithm(),r.getKeyVersion(),SCOPE,digest,time,"SERVER_CLOCK","NOT_BOUND",auditStatus,reasons,LocalDateTime.now(ZoneId.of("Asia/Shanghai")),"V2"),auditId);
    }
    private static Long v2Id(JsonNode value) {
        if (value == null || !value.isTextual() || !value.textValue().matches("[1-9][0-9]*")) throw new IllegalArgumentException("SIGNATURE_V2_ID_INVALID");
        return Long.valueOf(value.textValue());
    }

    /** Server-internal proof of the frozen facts at verification time, not a request DTO. */
    public static final class VerifiedSignatureFact {
        private final FrozenFact fact;
        private final ElectronicSignatureVerificationDTO verification;
        private final Long auditEventId;

        private VerifiedSignatureFact(FrozenFact fact, ElectronicSignatureVerificationDTO verification,
                Long auditEventId) {
            this.fact = fact;
            this.verification = verification;
            this.auditEventId = auditEventId;
        }

        public Long tenantId() { return fact.tenantId(); }
        public Long id() { return fact.id(); }
        public String moduleCode() { return fact.moduleCode(); }
        public String actionCode() { return fact.actionCode(); }
        public String subjectType() { return fact.subjectType(); }
        public String subjectId() { return fact.subjectId(); }
        public String subjectVersion() { return fact.subjectVersion(); }
        public Long actorId() { return fact.actorId(); }
        public String meaningCode() { return fact.meaningCode(); }
        public String meaningLabel() { return fact.meaningLabel(); }
        public String reason() { return fact.reason(); }
        public LocalDateTime signedAt() { return fact.signedAt(); }
        public String timeEvidenceId() { return fact.timeEvidenceId(); }
        public String authenticationMethod() { return fact.authenticationMethod(); }
        public String contentHash() { return fact.contentHash(); }
        public String beforeContentHash() { return fact.beforeContentHash(); }
        public String afterContentHash() { return fact.afterContentHash(); }
        public String canonicalContentJson() { return fact.canonicalContentJson(); }
        public String beforeContentJson() { return fact.beforeContentJson(); }
        public String afterContentJson() { return fact.afterContentJson(); }
        public String fieldDiffJson() { return fact.fieldDiffJson(); }
        public String evidenceHash() { return fact.evidenceHash(); }
        public String algorithm() { return fact.algorithm(); }
        public String keyVersion() { return fact.keyVersion(); }
        public String policyVersion() { return fact.policyVersion(); }
        public String verificationStatus() { return fact.verificationStatus(); }
        public String idempotencyKey() { return fact.idempotencyKey(); }
        public String commandHash() { return fact.commandHash(); }
        public String processInstanceId() { return fact.processInstanceId(); }
        public String taskId() { return fact.taskId(); }
        public String nodeCode() { return fact.nodeCode(); }
        public Integer nodeOrder() { return fact.nodeOrder(); }
        public ElectronicSignatureVerificationDTO verification() { return verification; }
        public Long auditEventId() { return auditEventId; }
    }
}
