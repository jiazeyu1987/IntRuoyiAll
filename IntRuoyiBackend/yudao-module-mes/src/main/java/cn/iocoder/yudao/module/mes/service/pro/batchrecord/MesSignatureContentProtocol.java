package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure MES V1 interpretation of a server-issued, fully verified frozen signature.
 * The projection never replaces the original subject, version, content or hashes.
 * Business authorization and consumer-specific evidence checks remain with callers.
 */
public final class MesSignatureContentProtocol {
    private static final ObjectMapper FACT_READER = new ObjectMapper();
    private static final Set<String> ACTION_CODES = new MesBatchRecordSignatureSubjectAdapter()
            .supportedActions().stream().map(SignatureActionDefinition::actionCode)
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> BUSINESS_KEYS = Set.of(
            "adapter", "executionId", "actionType", "processInstanceId", "bpmTaskId",
            "bpmTaskDefinitionKey", "bpmTaskName", "signatureCellKey", "signatureRowIndex",
            "signatureColumnIndex", "reviewSourceType", "reviewSourceId", "reviewSourceName",
            "approvalResult", "reason", "fieldAuditRevision", "fieldAuditHeadHash",
            "cellValuesHash", "signatureChallengeHash");

    private MesSignatureContentProtocol() {}

    public static VerifiedMesSignatureContent parseVerifiedContent(VerifiedSignatureFact fact,
            Long expectedTenantId) {
        if (fact == null) throw invalid("MES_SIGNATURE_VERIFIED_FACT_REQUIRED");
        if (!positive(expectedTenantId) || !Objects.equals(expectedTenantId, fact.tenantId()))
            throw invalid("MES_SIGNATURE_TENANT_CONFLICT");
        boolean v2="V2".equals(fact.verification().evidenceFormatVersion());
        if (!("V1".equals(fact.verification().evidenceFormatVersion()) || v2)
                || !"LOCAL_DIGEST_TIME_AND_CREATION_AUDIT".equals(fact.verification().verificationScope())
                || !MesBatchRecordSignatureSubjectAdapter.MODULE_CODE.equals(fact.moduleCode())
                || !MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE.equals(fact.subjectType()))
            throw invalid("MES_SIGNATURE_FORMAT_UNSUPPORTED");

        var read = interpret(new ContentFacts(fact.tenantId(), fact.moduleCode(), fact.subjectType(), fact.subjectId(),
                fact.subjectVersion(), fact.actionCode(), fact.reason(), fact.authenticationMethod(), fact.actorId(),
                fact.canonicalContentJson(), fact.verification().evidenceFormatVersion()), expectedTenantId);
        return new VerifiedMesSignatureContent(fact, read.subject(), read.businessContentJson(), read.identity(), read.userOpinion());
    }

    private static ReadMesSignatureContent interpret(ContentFacts fact, Long expectedTenantId) {
        boolean v2 = "V2".equals(fact.format());        MesBatchRecordSignatureSubjectAdapter.DecodedSubject subject = decodeSubject(fact.subjectId());
        if (!ACTION_CODES.contains(fact.actionCode()) || !Objects.equals(subject.actionType(), fact.actionCode()))
            throw invalid("MES_SIGNATURE_ACTION_CONFLICT");
        if (!Objects.equals(MesBatchRecordSignatureSubjectAdapter.subjectVersion(fact.subjectId()), fact.subjectVersion()))
            throw invalid("MES_SIGNATURE_VERSION_CONFLICT");

        ObjectNode content = readObject(fact.canonicalContentJson());
        VerifiedSignatureIdentity identity = readIdentity(content.get("signatureIdentity"), fact, expectedTenantId);
        ObjectNode business = content.deepCopy();
        if(v2) {
            for(var key:Set.of("evidenceFormatVersion","commandHashVersion","signatureIdentity","signatureTime","signatureImage","nodePosition")) {
                if(!business.has(key)) throw invalid("MES_SIGNATURE_METADATA_MISSING");
                business.remove(key);
            }
        } else business.remove("signatureIdentity");
        String userOpinion = userOpinion(subject, fact);
        String expectedJson = MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                fact.subjectId(), fact.actionCode(), fact.reason());
        ObjectNode expectedBusiness = readObject(expectedJson);
        if (!validBusinessFields(business, subject.opinionSource() != null) || !business.equals(expectedBusiness))
            throw invalid("MES_SIGNATURE_BUSINESS_CONFLICT");
        return new ReadMesSignatureContent(subject, JsonUtils.toJsonString(business),
                identity, userOpinion);
    }

    /** Diagnostic interpretation only; does not issue a verified fact or authorize an action. */
    public static ReadMesSignatureContent readDiagnosticContent(
            cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO evidence,
            cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO verification, Long tenant) {
        if (evidence == null || verification == null || !positive(tenant)
                || !Objects.equals(tenant, cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(evidence.id(), verification.signatureId()) || !positive(evidence.actorId())
                || !Set.of("V1", "V2").contains(verification.evidenceFormatVersion())
                || !"LOCAL_DIGEST_TIME_AND_CREATION_AUDIT".equals(verification.verificationScope())
                || !Objects.equals(evidence.contentHash(), verification.storedContentHash())
                || !Objects.equals(evidence.evidenceHash(), verification.storedEvidenceHash())
                || !"MES".equals(evidence.moduleCode()) || !"MES_BATCH_RECORD".equals(evidence.subjectType()))
            throw invalid("MES_SIGNATURE_DIAGNOSTIC_FACT_CONFLICT");
        return interpret(new ContentFacts(tenant, evidence.moduleCode(), evidence.subjectType(), evidence.subjectId(),
                evidence.subjectVersion(), evidence.actionCode(), evidence.reason(), evidence.authenticationMethod(), evidence.actorId(),
                evidence.canonicalContentJson(), verification.evidenceFormatVersion()), tenant);
    }
    private record ContentFacts(Long tenantId, String moduleCode, String subjectType, String subjectId,
            String subjectVersion, String actionCode, String reason, String authenticationMethod, Long actorId,
            String canonicalContentJson, String format) {}
    public record ReadMesSignatureContent(MesBatchRecordSignatureSubjectAdapter.DecodedSubject subject,
            String businessContentJson, VerifiedSignatureIdentity identity, String userOpinion) {}
    private static MesBatchRecordSignatureSubjectAdapter.DecodedSubject decodeSubject(String subjectId) {
        final byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(subjectId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("MES_SIGNATURE_SUBJECT_INVALID", ex);
        }
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException ex) {
            throw new IllegalArgumentException("MES_SIGNATURE_SUBJECT_INVALID", ex);
        }
        // Preserve the existing 17/18 parser and its original Base64 read set.
        try {
            return MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subjectId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("MES_SIGNATURE_SUBJECT_INVALID", ex);
        }
    }

    private static ObjectNode readObject(String json) {
        final JsonNode content;
        try {
            content = FACT_READER.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("MES_SIGNATURE_BUSINESS_CONFLICT", ex);
        }
        if (content == null || !content.isObject()) throw invalid("MES_SIGNATURE_BUSINESS_CONFLICT");
        return (ObjectNode) content;
    }

    private static String userOpinion(MesBatchRecordSignatureSubjectAdapter.DecodedSubject subject,
            ContentFacts fact) {
        if (StrUtil.isBlank(fact.reason())) throw invalid("MES_SIGNATURE_BUSINESS_CONFLICT");
        if (subject.opinionSource() == null) return null;
        if ("USER".equals(subject.opinionSource())) return fact.reason();
        if (!Objects.equals(fact.actionCode(), fact.reason())) throw invalid("MES_SIGNATURE_BUSINESS_CONFLICT");
        return null;
    }

    private static boolean validBusinessFields(ObjectNode business, boolean hasOpinionSource) {
        if (business.size() != BUSINESS_KEYS.size() + (hasOpinionSource ? 1 : 0)) return false;
        var fields = business.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (!field.getValue().isTextual()
                    || !(BUSINESS_KEYS.contains(field.getKey())
                    || hasOpinionSource && "opinionSource".equals(field.getKey()))) return false;
        }
        return true;
    }

    private static VerifiedSignatureIdentity readIdentity(JsonNode identity, ContentFacts fact,
            Long expectedTenantId) {
        if (identity == null || identity.isNull()) {
            if ("SESSION_PLUS_PASSWORD".equals(fact.authenticationMethod()) && positive(fact.actorId())) return null;
            throw invalid("MES_SIGNATURE_IDENTITY_CONFLICT");
        }
        if (!identity.isObject()) throw invalid("MES_SIGNATURE_IDENTITY_CONFLICT");
        String domain = text(identity, "domain");
        boolean v2="V2".equals(fact.format());
        Long tenantId = v2 ? strictTextId(identity.get("tenantId")) : strictId(identity.get("tenantId"));
        Long signerId = v2 ? strictTextId(identity.get("signerId")) : strictId(identity.get("signerId"));
        Long operatorId = v2 ? strictTextId(identity.get("operatorId")) : strictId(identity.get("operatorId"));
        if (tenantId == null || signerId == null || operatorId == null
                || !Objects.equals(tenantId, expectedTenantId) || !Objects.equals(tenantId, fact.tenantId())
                || !Objects.equals(signerId, fact.actorId())
                || !("SYSTEM_USER".equals(domain)
                    && ("SESSION_PLUS_SELECTED_USER_PASSWORD".equals(fact.authenticationMethod()) || v2 && "SESSION_PLUS_PASSWORD".equals(fact.authenticationMethod()) && Objects.equals(signerId,operatorId))
                    || "MES_EMPLOYEE_PROFILE".equals(domain)
                    && "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD".equals(fact.authenticationMethod())))
            throw invalid("MES_SIGNATURE_IDENTITY_CONFLICT");
        String displayName = text(identity, "displayName");
        return new VerifiedSignatureIdentity(domain, tenantId, signerId, operatorId,
                StrUtil.isBlank(displayName) ? null : displayName);
    }

    private static Long strictId(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToLong()
                && value.longValue() > 0 ? value.longValue() : null;
    }
    private static Long strictTextId(JsonNode value) {
        if(value==null || !value.isTextual() || !value.textValue().matches("[1-9][0-9]*")) return null;
        try { return Long.valueOf(value.textValue()); } catch(NumberFormatException e) { throw invalid("MES_SIGNATURE_IDENTITY_CONFLICT"); }
    }

    private static boolean positive(Long id) { return id != null && id > 0; }

    private static String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    private static IllegalArgumentException invalid(String code) { return new IllegalArgumentException(code); }

    public static final class VerifiedMesSignatureContent {
        private final VerifiedSignatureFact signatureFact;
        private final MesBatchRecordSignatureSubjectAdapter.DecodedSubject subject;
        private final String businessContentJson;
        private final VerifiedSignatureIdentity identity;
        private final String userOpinion;

        private VerifiedMesSignatureContent(VerifiedSignatureFact signatureFact,
                MesBatchRecordSignatureSubjectAdapter.DecodedSubject subject, String businessContentJson,
                VerifiedSignatureIdentity identity, String userOpinion) {
            this.signatureFact = signatureFact;
            this.subject = subject;
            this.businessContentJson = businessContentJson;
            this.identity = identity;
            this.userOpinion = userOpinion;
        }

        public VerifiedSignatureFact signatureFact() { return signatureFact; }
        public String formatVersion() { return "V1"; }
        public MesBatchRecordSignatureSubjectAdapter.DecodedSubject subject() { return subject; }
        public String businessContentJson() { return businessContentJson; }
        public VerifiedSignatureIdentity identity() { return identity; }
        public String opinionSource() { return subject.opinionSource(); }
        public String userOpinion() { return userOpinion; }
    }

    public static final class VerifiedSignatureIdentity {
        private final String domain;
        private final Long tenantId;
        private final Long signerId;
        private final Long operatorId;
        private final String displayName;

        private VerifiedSignatureIdentity(String domain, Long tenantId, Long signerId, Long operatorId,
                String displayName) {
            this.domain = domain;
            this.tenantId = tenantId;
            this.signerId = signerId;
            this.operatorId = operatorId;
            this.displayName = displayName;
        }

        public String domain() { return domain; }
        public Long tenantId() { return tenantId; }
        public Long signerId() { return signerId; }
        public Long operatorId() { return operatorId; }
        public String displayName() { return displayName; }
    }
}
