package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewReceiptDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalReviewReceiptMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ApprovalCenterReceiptService {
    private final BpmApprovalReviewReceiptMapper mapper;
    private final ElectronicSignatureVerifiedFactService signatures;
    private final GxpCreationAuditVerificationService audits;
    public ApprovalCenterReceiptService(BpmApprovalReviewReceiptMapper mapper, ElectronicSignatureVerifiedFactService signatures, GxpCreationAuditVerificationService audits) { this.mapper = mapper; this.signatures = signatures; this.audits = audits; }
    public BpmApprovalReviewReceiptDO findVerified(Long actor, String key) {
        return verify(actor,mapper.selectExact(TenantContextHolder.getRequiredTenantId(),key));
    }
    public BpmApprovalReviewReceiptDO findVerifiedCurrent(Long actor,String key) {
        return verify(actor,mapper.selectExactCurrent(TenantContextHolder.getRequiredTenantId(),key));
    }
    private BpmApprovalReviewReceiptDO verify(Long actor,BpmApprovalReviewReceiptDO row) {
        if (row == null) return null;
        if (!Objects.equals(row.getActorId(), actor) || !Objects.equals(row.getOperatorId(), actor) || !Objects.equals(row.getTenantId(),TenantContextHolder.getRequiredTenantId()) || !"AC_REVIEW_V1".equals(row.getRequestHashVersion()) || !Objects.equals(row.getReceiptRecordHash(), recordHash(row)) || !Objects.equals(row.getPreviewContentHash(),ApprovalContentJson.hash(row.getCanonicalBusinessContentJson()))) throw new IllegalStateException("APPROVAL_RECEIPT_INTEGRITY_FAILED");
        var signature = signatures.requireVerifiedFact(row.getCenterSignatureId());
        if ((!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() && !Objects.equals(signature.auditEventId(),row.getSignatureCreationAuditId())) || !Objects.equals(signature.actorId(),actor) || !Objects.equals(signature.evidenceHash(),row.getCenterEvidenceHash()) || !Objects.equals(signature.subjectVersion(),row.getSubjectVersion())) throw new IllegalStateException("APPROVAL_RECEIPT_SIGNATURE_MISMATCH");
        var content = ApprovalContentJson.parse(signature.canonicalContentJson());
        requireCenterSignatureBinding(row,signature,content);
        if (!Objects.equals(content.get("approvalContent"),ApprovalContentJson.parse(row.getCanonicalBusinessContentJson()))) throw new IllegalStateException("APPROVAL_RECEIPT_CONTENT_MISMATCH");
        if(!Objects.equals(content.path("action").asText(),row.getActionCode()) || !Objects.equals(content.path("reason").asText(),row.getReason())) throw new IllegalStateException("APPROVAL_RECEIPT_DECISION_MISMATCH");
        var links=ApprovalContentJson.parse(row.getDomainLinksJson());
        if (!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) {
        var domain=audits.requireVerifiedEvent(row.getTenantId(),Long.valueOf(links.path("businessAuditEventId").textValue()));
        var domainEnvelope=ApprovalContentJson.parse(domain.canonicalEventJson());
        if(!Objects.equals(domain.eventHash(),links.path("businessAuditEventHash").textValue()) || !"mes.market-release.approve".equals(domainEnvelope.path("operationId").asText()) || !row.getCenterSignatureId().toString().equals(domainEnvelope.path("signatureRecordId").asText())) throw new IllegalStateException("APPROVAL_RECEIPT_DOMAIN_CREATION_MISMATCH");
        }
        var response=response(row);
        if(response==null || !row.getId().toString().equals(response.receiptId()) || !row.getCenterSignatureId().toString().equals(response.signatureId()) || !Objects.equals(row.getIdempotencyKey(),response.idempotencyKey()) || !Objects.equals(row.getPrepareId(),response.prepareId()) || !Objects.equals(row.getCenterEvidenceHash(),response.evidenceHash()) || !Objects.equals(row.getResultCode(),response.resultCode()) || !Objects.equals(row.getResultingStatus(),response.resultingStatus()) || !Objects.equals(row.getCommittedAt().toString()+"+08:00",response.committedAt())) throw new IllegalStateException("APPROVAL_RECEIPT_RESPONSE_MISMATCH");
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) return row;
        var event = audits.requireVerifiedEvent(row.getTenantId(),row.getReceiptAuditId()); var source = ApprovalContentJson.parse(event.canonicalEventJson());
        if (!"APPROVAL_CENTER_REVIEW_COMMIT".equals(source.path("operationId").asText()) || !sourceLocator(row.getId()).equals(source.path("sourceLocator").asText()) || !Objects.equals(source.path("subjectId").asText(),row.getId().toString()) || !Objects.equals(ApprovalContentJson.parse(source.path("afterStateJson").asText()),ApprovalContentJson.parse(JsonUtils.toJsonString(protectedState(row))))) throw new IllegalStateException("APPROVAL_RECEIPT_CREATION_MISMATCH");
        return row;
    }
    public ApprovalDecisionReceipt response(BpmApprovalReviewReceiptDO row) { return JsonUtils.parseObject(row.getResponseJson(),ApprovalDecisionReceipt.class); }
    static void requireCenterSignatureBinding(BpmApprovalReviewReceiptDO row,
            ElectronicSignatureVerifiedFactService.VerifiedSignatureFact fact,com.fasterxml.jackson.databind.JsonNode content) {
        var identity=ApprovalContentJson.parse(row.getIdentityJson());
        var signer=content.path("signatureIdentity");
        if(!"SYSTEM_USER".equals(row.getIdentityDomain()) || !"SYSTEM_USER".equals(signer.path("domain").asText())
                || !row.getActorId().toString().equals(signer.path("signerId").asText())
                || !row.getOperatorId().toString().equals(signer.path("operatorId").asText())
                || !row.getTenantId().toString().equals(signer.path("tenantId").asText())
                || !Objects.equals(row.getTenantId(),fact.tenantId())
                || !"V2".equals(content.path("evidenceFormatVersion").asText())
                || !"V2".equals(content.path("commandHashVersion").asText())
                || !"BPM".equals(fact.moduleCode()) || !"BPM_APPROVAL_TASK".equals(fact.subjectType())
                || !Objects.equals(fact.subjectId(),identity.path("domainSubjectType").asText()+":"+identity.path("domainSubjectId").asText())
                || !Objects.equals(fact.actionCode(),row.getActionCode()) || !Objects.equals(fact.reason(),row.getReason())
                || !Objects.equals(fact.idempotencyKey(),"AC:"+row.getIdempotencyKey()+":CENTER")
                || !Objects.equals(fact.taskId(),identity.path("sourceTaskId").textValue())
                || !Objects.equals(fact.processInstanceId(),identity.path("processInstanceId").isNull()?null:identity.path("processInstanceId").textValue()))
            throw new IllegalStateException("APPROVAL_RECEIPT_SIGNATURE_CONTEXT_MISMATCH");
        var stable=new LinkedHashMap<String,Object>();stable.put("itemCode","CENTER");stable.put("centerRequestHash",row.getRequestHash());stable.put("prepareId",row.getPrepareId());stable.put("contentPolicyVersion",row.getContentPolicyVersion());stable.put("previewContentHash",row.getPreviewContentHash());stable.put("identity",identity);
        var request=new LinkedHashMap<String,Object>();request.put("commandHashVersion","V2");request.put("tenantId",row.getTenantId().toString());request.put("operatorId",row.getOperatorId().toString());request.put("identityDomain",row.getIdentityDomain());request.put("actorId",row.getActorId().toString());request.put("moduleCode","BPM");request.put("actionCode",row.getActionCode());request.put("subjectType","BPM_APPROVAL_TASK");request.put("stableSubjectKey",stable);request.put("expectedSubjectVersion",row.getSubjectVersion());request.put("reason",row.getReason());request.put("idempotencyKey","AC:"+row.getIdempotencyKey()+":CENTER");request.put("businessOccurredAt",null);request.put("businessTimeZone",null);
        if(!Objects.equals(fact.commandHash(),cn.iocoder.yudao.module.signature.service.ElectronicSignatureV2Protocol.hash(request)))
            throw new IllegalStateException("APPROVAL_RECEIPT_SIGNATURE_COMMAND_MISMATCH");
    }
    public static Map<String,Object> protectedState(BpmApprovalReviewReceiptDO row) {
        var values = new LinkedHashMap<String,Object>(); values.put("receiptId",row.getId().toString()); values.put("receiptAuditId",row.getReceiptAuditId().toString()); values.put("tenantId",row.getTenantId().toString()); values.put("actorId",row.getActorId().toString()); values.put("operatorId",row.getOperatorId().toString()); values.put("identityDomain",row.getIdentityDomain()); values.put("idempotencyKey",row.getIdempotencyKey()); values.put("requestHash",row.getRequestHash()); values.put("requestHashVersion",row.getRequestHashVersion()); values.put("prepareId",row.getPrepareId()); values.put("identity",ApprovalContentJson.parse(row.getIdentityJson())); values.put("contentPolicyVersion",row.getContentPolicyVersion()); values.put("subjectVersion",row.getSubjectVersion()); values.put("previewContentHash",row.getPreviewContentHash()); values.put("canonicalBusinessContentJson",row.getCanonicalBusinessContentJson()); values.put("action",row.getActionCode()); values.put("reason",row.getReason()); values.put("centerSignatureId",row.getCenterSignatureId().toString()); values.put("centerEvidenceHash",row.getCenterEvidenceHash()); values.put("signatureCreationAuditId",row.getSignatureCreationAuditId().toString()); values.put("resultCode",row.getResultCode()); values.put("resultingStatus",row.getResultingStatus()); values.put("domainLinks",ApprovalContentJson.parse(row.getDomainLinksJson())); values.put("response",ApprovalContentJson.parse(row.getResponseJson())); values.put("committedAt",row.getCommittedAt().toString()); return values;
    }
    public static String sourceLocator(Long receiptId) { if(receiptId==null || receiptId<=0) throw new IllegalArgumentException("APPROVAL_RECEIPT_ID_INVALID"); return "ApprovalCenterReviewExecutor.execute#receiptId="+receiptId; }
    public static String recordHash(BpmApprovalReviewReceiptDO row) { return ApprovalContentJson.hash(ApprovalContentJson.canonical(ApprovalContentJson.parse(JsonUtils.toJsonString(protectedState(row))))); }
}
