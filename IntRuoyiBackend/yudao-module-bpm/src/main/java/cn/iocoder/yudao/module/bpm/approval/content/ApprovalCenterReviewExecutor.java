package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProviderRegistry;
import cn.iocoder.yudao.module.bpm.approval.service.signature.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewReceiptDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalReviewReceiptMapper;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.service.*;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class ApprovalCenterReviewExecutor {
    private final GxpAuditService audit;
    private final ApprovalAuthorityApi authority;
    private final List<SignatureAuthenticationApi> identities;
    private final ApprovalContentResolverRegistry contents;
    private final ApprovalTaskProviderRegistry providers;
    private final ApprovalReviewPrepareService prepares;
    private final ApprovalCenterReceiptService receipts;
    private final BpmApprovalReviewReceiptMapper receiptMapper;
    private final ElectronicSignatureTransactionExecutor signatures;
    private final ElectronicSignatureVerifiedFactService verifier;
    private final ApprovalSignatureImageSnapshotProvider images;
    private final AdminUserApi users;
    private final Clock clock;
    @jakarta.annotation.Resource private ApprovalSignatureAuthorizationService signatureAuthorization;
    public ApprovalCenterReviewExecutor(GxpAuditService audit,ApprovalAuthorityApi authority,List<SignatureAuthenticationApi> identities,ApprovalContentResolverRegistry contents,ApprovalTaskProviderRegistry providers,ApprovalReviewPrepareService prepares,ApprovalCenterReceiptService receipts,BpmApprovalReviewReceiptMapper receiptMapper,ElectronicSignatureTransactionExecutor signatures,ElectronicSignatureVerifiedFactService verifier,ApprovalSignatureImageSnapshotProvider images,AdminUserApi users,Clock clock) { this.audit=audit;this.authority=authority;this.identities=identities;this.contents=contents;this.providers=providers;this.prepares=prepares;this.receipts=receipts;this.receiptMapper=receiptMapper;this.signatures=signatures;this.verifier=verifier;this.images=images;this.users=users;this.clock=clock; }
    @Transactional(rollbackFor = Exception.class)
    @GxpWriteOperation(operationId = "APPROVAL_CENTER_REVIEW_COMMIT")
    public ApprovalDecisionReceipt execute(Long actor,ApprovalTaskReviewCommand command,AuthenticatedSigner signer,String requestHash) {
        audit.acquireLedgerLock(); var prior=receipts.findVerifiedCurrent(actor,command.getIdempotencyKey());
        if(!Objects.equals(signer.actorId(),actor) || !Objects.equals(signer.operatorId(),actor) || !Objects.equals(signer.tenantId(),TenantContextHolder.getRequiredTenantId()) || !Objects.equals(signer.requestIdentity(),command.getIdempotencyKey())) throw new IllegalArgumentException("APPROVAL_SIGNER_REQUEST_MISMATCH");
        if(prior!=null) { var priorIdentity=ApprovalContentJson.parse(prior.getIdentityJson());if(command.getModuleCode()==null || !command.getModuleCode().name().equals(priorIdentity.path("ownerCode").asText()) || !Objects.equals(command.getSourceTaskType(),priorIdentity.path("sourceTaskType").textValue()) || !Objects.equals(command.getSourceTaskId(),priorIdentity.path("sourceTaskId").textValue()) || !Objects.equals(command.getProcessInstanceId(),priorIdentity.path("processInstanceId").isNull()?null:priorIdentity.path("processInstanceId").textValue()) || !requestHash.equals(prior.getRequestHash())) throw new IllegalArgumentException("APPROVAL_IDEMPOTENCY_CONFLICT"); return receipts.response(prior); }
        var matches=identities.stream().filter(p->p.identityDomain().equals(signer.domain())).toList(); if(matches.size()!=1) throw new IllegalStateException("SIGNATURE_IDENTITY_PROVIDER_NOT_UNIQUE");
        matches.get(0).lockAndRevalidate(signer,clock.instant()); authority.lockTenantApprovalAuthority(signer.tenantId());
        var prepared=prepares.require(actor,command.getPrepareId(),true);
        var identity=new ApprovalIdentity(prepared.getTenantId(),prepared.getOwnerCode(),prepared.getSourceTaskType(),prepared.getSourceTaskId(),prepared.getProcessInstanceId(),prepared.getSubjectType(),prepared.getSubjectId());
        if(command.getModuleCode()==null || !Objects.equals(command.getModuleCode().name(),identity.ownerCode()) || !Objects.equals(command.getSourceTaskType(),identity.sourceTaskType()) || !Objects.equals(command.getSourceTaskId(),identity.sourceTaskId()) || !Objects.equals(command.getProcessInstanceId(),identity.processInstanceId())) throw new IllegalArgumentException("APPROVAL_PREPARE_IDENTITY_MISMATCH");
        var provider=providers.requireProvider(command.getModuleCode()); if(!provider.isVisibleTo(actor) || !(provider instanceof ApprovalTrustedDecisionProvider decision)) throw new IllegalArgumentException("APPROVAL_TRUSTED_PROVIDER_REQUIRED");
        var locked=contents.require(command.getModuleCode()).lockAndAuthorize(actor,identity);
        signatureAuthorization.requireAuthorized(actor);
        if(!Objects.equals(command.getExpectedSubjectVersion(),prepared.getSubjectVersion()) || !Objects.equals(command.getExpectedContentHash(),prepared.getPreviewContentHash()) || !Objects.equals(command.getContentPolicyVersion(),prepared.getContentPolicyVersion()) || !Objects.equals(locked.subjectVersion(),prepared.getSubjectVersion()) || !Objects.equals(locked.contentHash(),prepared.getPreviewContentHash()) || !locked.canonicalBusinessContentJson().equals(prepared.getCanonicalBusinessContentJson())) throw new IllegalArgumentException("STALE_CONTENT");
        if(!locked.allowedActions().contains(command.getResult().name())) throw new IllegalArgumentException("APPROVAL_ACTION_UNSUPPORTED");
        var image=images.requireActiveSnapshotLocked(actor); var user=users.getUser(actor); if(user==null || user.getNickname()==null || user.getNickname().isBlank()) throw new IllegalStateException("SIGNATURE_DISPLAY_NAME_MISSING");
        var imageJson=ApprovalContentJson.parse(JsonUtils.toJsonString(Map.of("state","CAPTURED","snapshot",Map.of("imageStatus",image.getImageStatus(),"verifiedStatus",image.getVerifiedStatus(),"imageId",image.getImageId().toString(),"versionNo",image.getVersionNo().toString(),"fileId",image.getFileId().toString(),"fileUrl",image.getFileUrl(),"sha256",image.getSha256(),"contentType",image.getContentType(),"fileSize",image.getFileSize().toString()))));
        var business=new LinkedHashMap<String,Object>();business.put("approvalContent",ApprovalContentJson.parse(locked.canonicalBusinessContentJson()));business.put("action",command.getResult().name());business.put("reason",command.getReason());
        var stable=new LinkedHashMap<String,Object>();stable.put("itemCode","CENTER");stable.put("centerRequestHash",requestHash);stable.put("prepareId",command.getPrepareId());stable.put("contentPolicyVersion",locked.contentPolicyVersion());stable.put("previewContentHash",locked.contentHash());stable.put("identity",ApprovalContentJson.parse(ApprovalCenterReviewFacade.identityJson(identity)));
        var signatureCommand=new ElectronicSignatureCommand("BPM",command.getResult().name(),"BPM_APPROVAL_TASK",identity.domainSubjectType()+":"+identity.domainSubjectId(),locked.subjectVersion(),null,command.getReason(),"AC:"+command.getIdempotencyKey()+":CENTER",null,null);
        var subject=new SignatureSubjectSnapshot(signatureCommand.subjectType(),signatureCommand.subjectId(),locked.subjectVersion(),JsonUtils.toJsonString(business),null,null,null,identity.processInstanceId(),identity.sourceTaskId(),null,null);
        var result=signatures.executeAuthenticated(new AuthenticatedSignatureRequest(signatureCommand,subject,new SignatureActionDefinition("BPM",command.getResult().name(),"BPM_APPROVAL_TASK",command.getResult().name(),"审批确认","approval-content-v1"),ApprovalContentJson.parse(JsonUtils.toJsonString(stable)),user.getNickname(),imageJson),signer);
        var verified=verifier.requireVerifiedFact(result.signatureId()); Long receiptId=IdWorker.getId();
        String receiptLocator=ApprovalCenterReceiptService.sourceLocator(receiptId);
        Long receiptAuditId=audit.reserveEventId("APPROVAL_CENTER_REVIEW_COMMIT",receiptId.toString(),receiptLocator,"AC:"+command.getIdempotencyKey());
        var domain=decision.decide(actor,receiptId,command,locked,verified); images.markReferencedLocked(actor,image.getImageId());
        var row=new BpmApprovalReviewReceiptDO();row.setId(receiptId);row.setTenantId(signer.tenantId());row.setActorId(actor);row.setOperatorId(actor);row.setIdentityDomain(signer.domain());row.setIdempotencyKey(command.getIdempotencyKey());row.setRequestHash(requestHash);row.setRequestHashVersion("AC_REVIEW_V1");row.setPrepareId(command.getPrepareId());row.setIdentityJson(ApprovalCenterReviewFacade.identityJson(identity));row.setContentPolicyVersion(locked.contentPolicyVersion());row.setSubjectVersion(locked.subjectVersion());row.setPreviewContentHash(locked.contentHash());row.setCanonicalBusinessContentJson(locked.canonicalBusinessContentJson());row.setActionCode(command.getResult().name());row.setReason(command.getReason());row.setCenterSignatureId(result.signatureId());row.setCenterEvidenceHash(result.evidenceHash());row.setSignatureCreationAuditId(verified.auditEventId());row.setResultCode(domain.resultCode());row.setResultingStatus(domain.resultingStatus());row.setDomainLinksJson(domain.domainLinksJson());row.setCommittedAt(result.signedAt());
        var response=new ApprovalDecisionReceipt(receiptId.toString(),command.getIdempotencyKey(),command.getPrepareId(),result.signatureId().toString(),result.evidenceHash(),domain.resultCode(),domain.resultingStatus(),result.signedAt().toString()+"+08:00");row.setResponseJson(JsonUtils.toJsonString(response));row.setReceiptAuditId(receiptAuditId);row.setReceiptRecordHash(ApprovalCenterReceiptService.recordHash(row));
        var event=audit.append(GxpAuditCommand.builder().reservedEventId(receiptAuditId).operationId("APPROVAL_CENTER_REVIEW_COMMIT").subjectId(receiptId.toString()).subjectVersion(locked.subjectVersion()).reason(command.getReason()).beforeState(GxpAuditStateEnvelope.builder().state("NO_APPROVAL_RECEIPT").objectVersion(locked.subjectVersion()).canonicalJson("{}").build()).afterState(GxpAuditStateEnvelope.builder().state("APPROVAL_REVIEW_COMMITTED").objectVersion(locked.subjectVersion()).canonicalJson(JsonUtils.toJsonString(ApprovalCenterReceiptService.protectedState(row))).build()).idempotencyKey("AC:"+command.getIdempotencyKey()).source("ApprovalCenterReviewExecutor.execute").sourceType("SERVICE_METHOD").sourceLocator(receiptLocator).signatureRecordId(result.signatureId().toString()).signatureContentHash(result.contentHash()).build());row.setReceiptAuditId(event.eventId());
        if(receiptMapper.insert(row)!=1) throw new IllegalStateException("APPROVAL_RECEIPT_INSERT_FAILED"); receipts.findVerified(actor,command.getIdempotencyKey()); return response;
    }
}
