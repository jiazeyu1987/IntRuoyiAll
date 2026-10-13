package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewReceiptDO;
import cn.iocoder.yudao.module.signature.service.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ApprovalCenterReceiptBindingTest {
    @Test void sameKeyCannotReplayThroughAnotherModuleAndNoAuthenticationRuns() {
        var identity=new ApprovalIdentity(1L,"EDHR","EDHR_WORK_TASK","2",null,"EDHR_RELEASE","3");
        var old=new BpmApprovalReviewReceiptDO();old.setIdentityJson(ApprovalCenterReviewFacade.identityJson(identity));
        var receipts=mock(ApprovalCenterReceiptService.class);when(receipts.findVerified(eq(4L),anyString())).thenReturn(old);
        var auth=mock(ElectronicSignatureAuthenticationFacade.class);
        var facade=new ApprovalCenterReviewFacade(mock(ApprovalReviewPrepareService.class),receipts,auth,mock(ApprovalCenterReviewExecutor.class));
        var command=new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.BPM).setSourceTaskType("EDHR_WORK_TASK").setSourceTaskId("2").setReason("reviewed").setIdempotencyKey(UUID.randomUUID().toString());
        assertThrows(IllegalArgumentException.class,()->facade.review(4L,command));
        assertThrows(IllegalArgumentException.class,()->ApprovalCenterReviewFacade.requestHash(4L,identity,command));
        verifyNoInteractions(auth);
    }

    @Test void sameModuleSameKeyReplaysProducerIdentityWithNullProcessWithoutAuthentication() {
        var identity=new ApprovalIdentity(1L,"EDHR","EDHR_WORK_TASK","2",null,"EDHR_RELEASE","3");
        var identityJson=ApprovalCenterReviewFacade.identityJson(identity);
        assertTrue(ApprovalContentJson.parse(identityJson).has("processInstanceId"));
        assertTrue(ApprovalContentJson.parse(identityJson).get("processInstanceId").isNull());
        var command=new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.EDHR)
                .setSourceTaskType("EDHR_WORK_TASK").setSourceTaskId("2").setResult(ApprovalTaskReviewResult.APPROVE)
                .setReason("reviewed").setIdempotencyKey(UUID.randomUUID().toString())
                .setPrepareId(UUID.randomUUID().toString()).setExpectedSubjectVersion("EDHR1:version")
                .setExpectedContentHash("a".repeat(64)).setContentPolicyVersion("approval-content-v1");
        var old=new BpmApprovalReviewReceiptDO();old.setIdentityJson(identityJson);
        old.setRequestHash(ApprovalCenterReviewFacade.requestHash(4L,identity,command));
        var receipts=mock(ApprovalCenterReceiptService.class);
        when(receipts.findVerified(4L,command.getIdempotencyKey())).thenReturn(old);
        var response=new ApprovalDecisionReceipt("receipt",command.getIdempotencyKey(),command.getPrepareId(),"signature","b".repeat(64),"APPROVE","RELEASED","2026-10-10T00:00:00Z");
        when(receipts.response(old)).thenReturn(response);
        var prepares=mock(ApprovalReviewPrepareService.class);var auth=mock(ElectronicSignatureAuthenticationFacade.class);
        var executor=mock(ApprovalCenterReviewExecutor.class);
        var facade=new ApprovalCenterReviewFacade(prepares,receipts,auth,executor);
        assertSame(response,facade.review(4L,command));
        verifyNoInteractions(prepares,auth,executor);
    }

    @Test void receiptRejectsValidSignatureFromWrongDomainSubjectOrStablePrepareContext() {
        var row=new BpmApprovalReviewReceiptDO();row.setTenantId(1L);row.setActorId(4L);row.setOperatorId(4L);row.setIdentityDomain("SYSTEM_USER");row.setIdempotencyKey(UUID.randomUUID().toString());row.setPrepareId(UUID.randomUUID().toString());row.setActionCode("APPROVE");row.setReason("reviewed");row.setRequestHash("a".repeat(64));row.setSubjectVersion("EDHR1:"+"b".repeat(64));row.setPreviewContentHash("c".repeat(64));row.setContentPolicyVersion("approval-content-v1");row.setIdentityJson(ApprovalCenterReviewFacade.identityJson(new ApprovalIdentity(1L,"EDHR","EDHR_WORK_TASK","2",null,"EDHR_RELEASE","3")));
        var content=ApprovalContentJson.parse("{\"evidenceFormatVersion\":\"V2\",\"commandHashVersion\":\"V2\",\"signatureIdentity\":{\"domain\":\"SYSTEM_USER\",\"signerId\":\"4\",\"operatorId\":\"4\",\"tenantId\":\"1\"}}");
        var fact=mock(ElectronicSignatureVerifiedFactService.VerifiedSignatureFact.class);
        when(fact.tenantId()).thenReturn(1L);when(fact.moduleCode()).thenReturn("BPM");when(fact.subjectType()).thenReturn("BPM_APPROVAL_TASK");when(fact.subjectId()).thenReturn("EDHR_RELEASE:3");when(fact.actionCode()).thenReturn("APPROVE");when(fact.reason()).thenReturn("reviewed");when(fact.idempotencyKey()).thenReturn("AC:"+row.getIdempotencyKey()+":CENTER");when(fact.taskId()).thenReturn("2");
        var stable=new LinkedHashMap<String,Object>();stable.put("itemCode","CENTER");stable.put("centerRequestHash",row.getRequestHash());stable.put("prepareId",row.getPrepareId());stable.put("contentPolicyVersion",row.getContentPolicyVersion());stable.put("previewContentHash",row.getPreviewContentHash());stable.put("identity",ApprovalContentJson.parse(row.getIdentityJson()));
        var request=new LinkedHashMap<String,Object>();request.put("commandHashVersion","V2");request.put("tenantId","1");request.put("operatorId","4");request.put("identityDomain","SYSTEM_USER");request.put("actorId","4");request.put("moduleCode","BPM");request.put("actionCode","APPROVE");request.put("subjectType","BPM_APPROVAL_TASK");request.put("stableSubjectKey",stable);request.put("expectedSubjectVersion",row.getSubjectVersion());request.put("reason","reviewed");request.put("idempotencyKey","AC:"+row.getIdempotencyKey()+":CENTER");request.put("businessOccurredAt",null);request.put("businessTimeZone",null);
        when(fact.commandHash()).thenReturn(ElectronicSignatureV2Protocol.hash(request));
        assertDoesNotThrow(()->ApprovalCenterReceiptService.requireCenterSignatureBinding(row,fact,content));
        var wrongDomain=content.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)wrongDomain.get("signatureIdentity")).put("domain","MES_EMPLOYEE_PROFILE");
        assertThrows(IllegalStateException.class,()->ApprovalCenterReceiptService.requireCenterSignatureBinding(row,fact,wrongDomain));
        when(fact.subjectId()).thenReturn("EDHR_RELEASE:999");
        assertThrows(IllegalStateException.class,()->ApprovalCenterReceiptService.requireCenterSignatureBinding(row,fact,content));
        when(fact.subjectId()).thenReturn("EDHR_RELEASE:3");row.setPrepareId(UUID.randomUUID().toString());
        assertThrows(IllegalStateException.class,()->ApprovalCenterReceiptService.requireCenterSignatureBinding(row,fact,content));
    }
}
