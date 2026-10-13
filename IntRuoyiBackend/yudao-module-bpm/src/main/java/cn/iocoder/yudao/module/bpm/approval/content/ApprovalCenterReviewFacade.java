package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureAuthenticationFacade;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

@Service
public class ApprovalCenterReviewFacade {
    private final ApprovalReviewPrepareService prepares; private final ApprovalCenterReceiptService receipts;
    private final ElectronicSignatureAuthenticationFacade authentication; private final ApprovalCenterReviewExecutor executor;
    public ApprovalCenterReviewFacade(ApprovalReviewPrepareService prepares,ApprovalCenterReceiptService receipts,ElectronicSignatureAuthenticationFacade authentication,ApprovalCenterReviewExecutor executor) {this.prepares=prepares;this.receipts=receipts;this.authentication=authentication;this.executor=executor;}
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public ApprovalDecisionReceipt review(Long actor,ApprovalTaskReviewCommand command) {
        if(actor==null || actor<=0 || command.getIdempotencyKey()==null || !command.getIdempotencyKey().matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) throw new IllegalArgumentException("APPROVAL_IDEMPOTENCY_KEY_INVALID");
        String reason=command.getReason()==null?null:command.getReason().replaceAll("^[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+|[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+$", "");
        if(reason==null || reason.isEmpty() || reason.length()>4000) throw new IllegalArgumentException("APPROVAL_REVIEW_REASON_INVALID");command.setReason(reason);
        var old=receipts.findVerified(actor,command.getIdempotencyKey());
        ApprovalIdentity identity;
        if(old!=null) { var raw=ApprovalContentJson.parse(old.getIdentityJson());identity=new ApprovalIdentity(Long.valueOf(raw.path("tenantId").textValue()),raw.path("ownerCode").textValue(),raw.path("sourceTaskType").textValue(),raw.path("sourceTaskId").textValue(),raw.get("processInstanceId").isNull()?null:raw.get("processInstanceId").textValue(),raw.path("domainSubjectType").textValue(),raw.path("domainSubjectId").textValue()); }
        else {var p=prepares.require(actor,command.getPrepareId(),true);identity=new ApprovalIdentity(p.getTenantId(),p.getOwnerCode(),p.getSourceTaskType(),p.getSourceTaskId(),p.getProcessInstanceId(),p.getSubjectType(),p.getSubjectId());}
        if(command.getModuleCode()==null || !Objects.equals(command.getModuleCode().name(),identity.ownerCode()) || !Objects.equals(command.getSourceTaskType(),identity.sourceTaskType()) || !Objects.equals(command.getSourceTaskId(),identity.sourceTaskId()) || !Objects.equals(command.getProcessInstanceId(),identity.processInstanceId())) throw new IllegalArgumentException("APPROVAL_REQUEST_IDENTITY_MISMATCH");
        String hash=requestHash(actor,identity,command);
        if(old!=null) {if(!hash.equals(old.getRequestHash())) throw new IllegalArgumentException("APPROVAL_IDEMPOTENCY_CONFLICT");return receipts.response(old);}
        var signer=authentication.authenticate(new SignatureAuthenticationRequest(TenantContextHolder.getRequiredTenantId(),"SYSTEM_USER",actor,actor,command.getIdempotencyKey(),command.getSignaturePassword()));
        return executor.execute(actor,command,signer,hash);
    }
    public static String identityJson(ApprovalIdentity identity) {
        var values=new LinkedHashMap<String,Object>(); values.put("tenantId",identity.tenantId().toString());values.put("ownerCode",identity.ownerCode());values.put("sourceTaskType",identity.sourceTaskType());values.put("sourceTaskId",identity.sourceTaskId());values.put("processInstanceId",identity.processInstanceId());values.put("domainSubjectType",identity.domainSubjectType());values.put("domainSubjectId",identity.domainSubjectId());var json=(com.fasterxml.jackson.databind.node.ObjectNode)ApprovalContentJson.parse(JsonUtils.toJsonString(values));if(identity.processInstanceId()==null) json.putNull("processInstanceId");return ApprovalContentJson.canonical(json);
    }
    static String requestHash(Long actor,ApprovalIdentity identity,ApprovalTaskReviewCommand command) {
        if(command.getModuleCode()==null || !command.getModuleCode().name().equals(identity.ownerCode())) throw new IllegalArgumentException("APPROVAL_REQUEST_OWNER_MISMATCH");
        var values=new LinkedHashMap<String,Object>();values.put("requestHashVersion","AC_REVIEW_V1");values.put("tenantId",identity.tenantId().toString());values.put("actorId",actor.toString());values.put("operatorId",actor.toString());values.put("identity",ApprovalContentJson.parse(identityJson(identity)));values.put("prepareId",command.getPrepareId());values.put("expectedSubjectVersion",command.getExpectedSubjectVersion());values.put("expectedContentHash",command.getExpectedContentHash());values.put("contentPolicyVersion",command.getContentPolicyVersion());values.put("action",command.getResult().name());values.put("reason",command.getReason());values.put("idempotencyKey",command.getIdempotencyKey());return ApprovalContentJson.hash(ApprovalContentJson.canonical(ApprovalContentJson.parse(JsonUtils.toJsonString(values))));
    }
}
