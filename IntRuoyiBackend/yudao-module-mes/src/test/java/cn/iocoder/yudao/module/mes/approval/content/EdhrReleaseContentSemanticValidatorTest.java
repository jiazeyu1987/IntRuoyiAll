package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.mes.approval.content.EdhrReleaseContentFixtures.*;

class EdhrReleaseContentSemanticValidatorTest {
    private final EdhrReleaseContentPolicy policy=new EdhrReleaseContentPolicy();
    private final EdhrReleaseContentSemanticValidator validator=new EdhrReleaseContentSemanticValidator(policy);
    @Test void fourSourceAndAuthorizationQuadrantsAreValidBusinessContent(){
        for(boolean active:new boolean[]{true,false})for(boolean managed:new boolean[]{true,false}){
            var root=content(active,managed);
            assertDoesNotThrow(()->policy.validate(root));
            assertDoesNotThrow(()->validator.validate(root));
            String token=validator.subjectVersion(root,guard());
            assertTrue(token.matches("EDHR1:[0-9a-f]{64}"));
            assertEquals(token,validator.subjectVersion(ApprovalContentJson.parse(ApprovalContentJson.canonical(root)),guard()));
        }
    }
    @Test void everyFormalRootIsRequiredAndUnknownKeysAreRejected(){
        for(String key:new String[]{"contentPolicyVersion","scopeCode","identity","workTask","release","finalization"}){
            var root=content(true,false);root.remove(key);assertThrows(IllegalArgumentException.class,()->validator.validate(root),key);
        }
        bad(true,false,r->r.put("unregistered",true));
        bad(true,false,r->obj(r,"/release").put("unregistered",true));
        bad(true,false,r->obj(r,"/identity").put("tenantId","9223372036854775808"));
    }
    @Test void mismatchedIdentitiesBranchesAndReceiptLinksFail(){
        bad(true,false,r->obj(r,"/workTask").put("businessScopeId","21"));
        bad(true,true,r->obj(r,"/finalization").putNull("applicationId"));
        bad(true,true,r->obj(r,"/workTask").putNull("assigneeUserId"));
        bad(true,true,r->obj(r,"/workTask/candidate").put("sourceType","USER"));
        bad(true,true,r->obj(r,"/workTask/responsibility").put("sourceKey","QA"));
        bad(true,false,r->obj(r,"/finalization").put("applicationId","95"));
        bad(true,false,r->obj(r,"/finalization/completionBackfillReceipt").put("receiptId","89"));
        bad(false,false,r->obj(r,"/finalization/independentPrerequisiteReceipt").put("receiptHash","b".repeat(64)));
        bad(true,false,r->obj(r,"/finalization").set("independentPrerequisiteReceipt",content(false,false).at("/finalization/independentPrerequisiteReceipt")));
        bad(false,false,r->obj(r,"/finalization").put("activeOrderVersion","1"));
    }
    @Test void orderingCountsAndFourMaterialIdentityRemainAuthoritative(){
        bad(true,false,r->obj(r,"/workTask/candidate").put("userSnapshot","3,2"));
        bad(true,false,r->obj(r,"/release/checkCount").put("required",9));
        bad(true,false,r->obj(r,"/release/precheckSnapshot/items/0").put("result","BLOCKER"));
        bad(true,false,r->obj(r,"/release/precheckSnapshot/items/1").put("code","DEVIATION_CLOSED"));
        bad(true,false,r->obj(r,"/finalization/materialManifest/materials/1").put("type",TYPES[0]));
        bad(true,false,r->obj(r,"/finalization/materialManifest/materials/0").put("batchExecutionId","31"));
        bad(false,false,r->((ArrayNode)r.at("/finalization/independentPrerequisiteReceipt/sourceEvidence")).add(r.at("/finalization/independentPrerequisiteReceipt/sourceEvidence/0").deepCopy()));
    }
    @Test void lossFactsRawSnapshotAndBusinessCalendarMustAgree(){
        bad(true,false,r->obj(r,"/finalization/completionBackfillReceipt").put("lossQuantity","1"));
        bad(true,false,r->obj(r,"/finalization/completionBackfillReceipt").put("hasActualLoss",true));
        bad(true,false,r->obj(r,"/finalization/completionBackfillReceipt").put("formalSourceSnapshotJsonRaw","[]"));
        bad(false,false,r->obj(r,"/finalization/independentPrerequisiteReceipt").put("issuedAt","2026-02-30T10:00:00+08:00"));
        var root=content(true,false);var receipt=obj(root,"/finalization/completionBackfillReceipt");
        receipt.put("hasActualLoss",true).put("lossQuantity","2.5").put("lossReportStatus","SUCCESS").put("lossRecordId","97").putNull("zeroLossConfirmationSnapshotRaw");
        assertDoesNotThrow(()->validator.validate(root));
    }
    @Test void changingFrozenBusinessOrBatchGuardChangesSubjectVersion(){
        var root=content(true,false);String before=validator.subjectVersion(root,guard());
        obj(root,"/release/product").put("name","revised device");assertNotEquals(before,validator.subjectVersion(root,guard()));
        var batch=guard();batch.put("attemptNo",2);assertNotEquals(validator.subjectVersion(root,guard()),validator.subjectVersion(root,batch));
    }
    @Test void completionExpectedVersionPreservesRealZeroAndIntegerBounds(){
        for(String version:new String[]{"0","2147483647"}){
            var root=content(true,false);obj(root,"/finalization/completionBackfillReceipt").put("expectedActiveOrderVersion",version);
            assertDoesNotThrow(()->validator.validate(root));
        }
        for(String version:new String[]{"-1","00","01","2147483648","9223372036854775808"})
            bad(true,false,r->obj(r,"/finalization/completionBackfillReceipt").put("expectedActiveOrderVersion",version));
    }
    private void bad(boolean active,boolean managed,Consumer<ObjectNode> mutate){var root=content(active,managed);mutate.accept(root);assertThrows(IllegalArgumentException.class,()->validator.validate(root));}
    static ObjectNode obj(ObjectNode root,String pointer){return (ObjectNode)root.at(pointer);}
}
