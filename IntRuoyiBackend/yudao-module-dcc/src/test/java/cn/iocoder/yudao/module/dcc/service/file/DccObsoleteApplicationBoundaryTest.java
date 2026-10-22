package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DccObsoleteApplicationBoundaryTest {
    String json(String fields){return "{\"reason\":\"本次正式作废\",\"idempotencyKey\":\"actual-key\""+fields+"}";}
    DccControlledFileObsoleteReqVO req(String fields){return JsonUtils.parseObject(json(fields),DccControlledFileObsoleteReqVO.class);}
    boolean valid(DccControlledFileObsoleteReqVO req){try(var factory=Validation.buildDefaultValidatorFactory()){return factory.getValidator().validate(req).isEmpty();}}
    @Test void onlyExplicitKnownPhasesPassAndMissingPhaseCannotBecomeSubmit(){
        assertFalse(valid(req("")));
        assertFalse(valid(req(",\"phase\":\"LATEST_SUBMIT\"")));
        assertTrue(valid(req(",\"phase\":\"OPEN\"")));
        for(String phase:java.util.List.of("PREVIEW","SAVE","SUBMIT"))
            assertTrue(valid(req(",\"phase\":\""+phase+"\",\"formInstanceId\":\"901\",\"formInstanceCode\":\"FORM-901\",\"attributeRound\":2"+("SAVE".equals(phase)?",\"prepareExecution\":false":""))));
    }
    @Test void laterPhasesNeedTheExactPositiveFormIdentityAndReservedNumericExpectation(){
        for(String fields:java.util.List.of("",",\"formInstanceId\":-1,\"formInstanceCode\":\"FORM-901\",\"attributeRound\":2",
                ",\"formInstanceId\":901,\"formInstanceCode\":\" \",\"attributeRound\":2",
                ",\"formInstanceId\":901,\"formInstanceCode\":\"FORM-901\",\"attributeRound\":0"))
            assertFalse(valid(req(",\"phase\":\"SUBMIT\""+fields)));
        assertFalse(valid(JsonUtils.parseObject("{\"phase\":\"OPEN\",\"reason\":\" \",\"idempotencyKey\":\"k\"}",DccControlledFileObsoleteReqVO.class)));
    }
    @Test void publicBindingRetainsExactMatrixExpectationAndContextWithoutAuthorityFields(){
        var value=req(",\"phase\":\"SAVE\",\"prepareExecution\":true,\"formInstanceId\":\"9223372036854775000\",\"formInstanceCode\":\"REAL-CODE\",\"attributeRound\":4,\"applicationContextKey\":\"DCC_OBSOLETE:9223372036854775000:REAL-CODE\",\"expectedApprovalRouteId\":\"32\",\"expectedApprovalRouteVersionNo\":2,\"expectedApprovalMatrixDigest\":\"digest\",\"approvalMatrixBaselineToken\":\"server-token\",\"matrixChangeConfirmed\":true");
        assertTrue(valid(value));
        assertEquals(9223372036854775000L,org.springframework.test.util.ReflectionTestUtils.getField(value,"formInstanceId"));
        assertEquals("server-token",org.springframework.test.util.ReflectionTestUtils.getField(value,"approvalMatrixBaselineToken"));
        assertFalse(java.util.Arrays.stream(value.getClass().getDeclaredFields()).anyMatch(f->java.util.Set.of("supervisorUserId","preparedProcessDefinitionId").contains(f.getName())));
    }
    @Test void existingParentResponseSerializesRealChildLongIdentitiesWithoutLoss()throws Exception{
        var type=Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO");
        Object response=type.getDeclaredConstructor().newInstance();
        var methods=java.util.Map.of("setId",9223372036854775000L,"setControlledFileId",9223372036854775001L,"setDccProjectCodeId",9223372036854775002L);
        for(var entry:methods.entrySet())type.getMethod(entry.getKey(),Long.class).invoke(response,entry.getValue());
        cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO parent=(cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO)response;
        var encoded=JsonUtils.parseObject(JsonUtils.toJsonString(parent),java.util.Map.class);
        assertEquals("9223372036854775000",encoded.get("id"));assertEquals("9223372036854775001",encoded.get("controlledFileId"));assertEquals("9223372036854775002",encoded.get("dccProjectCodeId"));
    }
    @Test void saveMustExplicitlyChoosePlainDraftOrExecutionPreparationAndSavedBusinessFactsRemainReadable()throws Exception {
        var base=",\"phase\":\"SAVE\",\"formInstanceId\":901,\"formInstanceCode\":\"FORM-901\",\"attributeRound\":2";
        assertFalse(valid(req(base)));
        for(boolean prepare:java.util.List.of(false,true)){
            var value=req(base+",\"prepareExecution\":"+prepare);
            assertTrue(valid(value));assertEquals(prepare,org.springframework.test.util.ReflectionTestUtils.getField(value,"prepareExecution"));
        }
        var type=Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO");
        Object response=type.getDeclaredConstructor().newInstance();
        type.getMethod("setReason",String.class).invoke(response,"本轮已保存原因");
        type.getMethod("setIdempotencyKey",String.class).invoke(response,"actual-draft-key");
        type.getMethod("setSelectedSignoffDepartmentIds",java.util.List.class).invoke(response,java.util.List.of(9223372036854775000L));
        var encoded=JsonUtils.parseObject(JsonUtils.toJsonString(response),java.util.Map.class);
        assertEquals("本轮已保存原因",encoded.get("reason"));assertEquals("actual-draft-key",encoded.get("idempotencyKey"));
        assertEquals(java.util.List.of("9223372036854775000"),encoded.get("selectedSignoffDepartmentIds"));
    }
    @Test void reworkSourceGroupRequiresCompleteExactOldIdentityAndDigestWithoutChangingTargetIdentity() {
        String complete=",\"reworkSourceFormInstanceId\":\"9223372036854775000\",\"reworkSourceFormInstanceCode\":\"OLD-FORM\",\"reworkSourceBpmProcessInstanceId\":\"old-bpm\",\"reworkSourceAttributeRound\":1,\"reworkSourceDigest\":\""+"a".repeat(64)+"\"";
        var value=req(",\"phase\":\"OPEN\""+complete);assertTrue(valid(value));
        assertEquals(9223372036854775000L,org.springframework.test.util.ReflectionTestUtils.getField(value,"reworkSourceFormInstanceId"));
        for(String partial:java.util.List.of(",\"reworkSourceFormInstanceId\":5",",\"reworkSourceDigest\":\""+"a".repeat(64)+"\"",
            complete.replace("OLD-FORM"," OLD-FORM"),complete.replace("old-bpm"," "),complete.replace("\"reworkSourceAttributeRound\":1","\"reworkSourceAttributeRound\":0"),complete.replace("a".repeat(64),"bad")))
            assertFalse(valid(req(",\"phase\":\"OPEN\""+partial)));
    }
    @Test void reworkSourceReadExpectationsAreAllPresentOrAbsentAndCannotSelectByPartialTuple()throws Exception {
        var type=Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceReqVO");
        for(String json:java.util.List.of("{}","{\"reworkSourceFormInstanceId\":5,\"reworkSourceFormInstanceCode\":\"SOURCE\",\"reworkSourceBpmProcessInstanceId\":\"bpm\",\"reworkSourceAttributeRound\":1}")) {
            try(var factory=Validation.buildDefaultValidatorFactory()){assertTrue(factory.getValidator().validate(JsonUtils.parseObject(json,type)).isEmpty());}
        }
        for(String json:java.util.List.of("{\"reworkSourceFormInstanceId\":5}","{\"reworkSourceFormInstanceId\":-1,\"reworkSourceFormInstanceCode\":\"SOURCE\",\"reworkSourceBpmProcessInstanceId\":\"bpm\",\"reworkSourceAttributeRound\":1}")) {
            try(var factory=Validation.buildDefaultValidatorFactory()){assertFalse(factory.getValidator().validate(JsonUtils.parseObject(json,type)).isEmpty());}
        }
    }
    @Test void entryModeAndCandidateSourceFactsSerializeExactLongWithoutBecomingDraftId()throws Exception {
        var type=Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO");Object response=type.getConstructor().newInstance();
        type.getMethod("setApplicationEntryMode",String.class).invoke(response,"SOURCE_SELECTION_REQUIRED");
        var candidateType=Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceVO");Object candidate=candidateType.getConstructor().newInstance();
        candidateType.getMethod("setReworkSourceFormInstanceId",Long.class).invoke(candidate,9223372036854775000L);
        candidateType.getMethod("setReworkSourceFormInstanceCode",String.class).invoke(candidate,"OLD-REAL-CODE");
        candidateType.getMethod("setAvailable",boolean.class).invoke(candidate,false);
        candidateType.getMethod("setUnavailableReason",String.class).invoke(candidate,"SOURCE_EVIDENCE_REQUIRED");
        type.getMethod("setReworkSourceCandidates",java.util.List.class).invoke(response,java.util.List.of(candidate));
        var encoded=JsonUtils.parseObject(JsonUtils.toJsonString(response),java.util.Map.class);
        assertEquals("SOURCE_SELECTION_REQUIRED",encoded.get("applicationEntryMode"));assertNull(encoded.get("id"));
        var item=(java.util.Map)((java.util.List)encoded.get("reworkSourceCandidates")).get(0);
        assertEquals("9223372036854775000",item.get("reworkSourceFormInstanceId"));assertEquals(false,item.get("available"));assertEquals("SOURCE_EVIDENCE_REQUIRED",item.get("unavailableReason"));
    }
    @Test void unavailableCandidateExplicitlyPreservesUnrecordedReasonInsteadOfOmittingItsContractField() {
        var candidate=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceVO();
        candidate.setAvailable(false);candidate.setUnavailableReason("DCC_OBSOLETE_REWORK_RECORDED_PREPARATION_REQUIRED");
        var json=JsonUtils.parseObject(JsonUtils.toJsonString(candidate),java.util.Map.class);
        assertTrue(json.containsKey("reason"),"The real nullable reason must be present even with the current NON_NULL mapper");
        assertNull(json.get("reason"));assertEquals(false,json.get("available"));
    }
    @Test void originalRawMatrixNodeResponsePreservesAllLegitimateNullableRuleFacts() {
        var matrix=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO();
        var node=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO.RawNode();
        node.setId(701L);node.setStageType("REVIEW");matrix.setNodes(java.util.List.of(node));
        var response=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO();response.setReworkSourceApprovalMatrix(matrix);
        var json=JsonUtils.parseObject(JsonUtils.toJsonString(response),java.util.Map.class);
        var raw=(java.util.Map)((java.util.List)((java.util.Map)json.get("reworkSourceApprovalMatrix")).get("nodes")).get(0);
        assertEquals("701",raw.get("id"));
        for(String field:java.util.List.of("subjectName","marker","approveMethod","ruleRemark","candidateSourceId","approveRatio")){
            assertTrue(raw.containsKey(field),"Raw nullable field lost in actual response: "+field);assertNull(raw.get(field));
        }
    }
}
