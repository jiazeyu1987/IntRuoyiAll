package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.annotation.Resource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual immutable graphs, H2 round/attributes/frozen provenance; current preparation never substitutes another round. */
class DccRoundFlowContractTest extends DccBoundExecutionGraphContractTest {
    @Resource org.mybatis.spring.SqlSessionTemplate sql;
    DccBoundWorkflowContractResolver resolver;
    @BeforeEach void clearTaskOwnedFormBeforeRound(){DccObsoleteFormExecutionPreparationTest.clearTaskOwnedForm(jdbc);}
    @AfterEach void clearTaskOwnedFormAfterRound(){DccObsoleteFormExecutionPreparationTest.clearTaskOwnedForm(jdbc);}
    @BeforeEach void roundResolver(){resolver=new DccBoundWorkflowContractResolver();wire(resolver,"definitions",definitions,"processes",processes,"rounds",rounds,"attributes",attributeRows);
        for(var name:List.of("routes","forms"))if(Arrays.stream(resolver.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals(name)))
            wire(resolver,name,name.equals("routes")?routeSnapshots:actualForms());}
    Object actualForms(){var config=sql.getConfiguration();if(!config.hasMapper(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class))config.addMapper(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class);
        return sql.getMapper(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class);}
    DccApplicationRoundSummary summary(String type,String bpm,Integer round){var r=new DccApplicationRoundSummary();r.setControlledFileId(20L);r.setApplicationType(type);r.setBpmRound(bpm);r.setAttributeRound(round);return r;}
    Object resolve(DccApplicationRoundSummary row){return ReflectionTestUtils.invokeMethod(resolver,"resolveRoundFlowContract",files.selectById(20L),row);}
    Object field(Object r,String key){return ReflectionTestUtils.invokeMethod(r,key);}
    void publicProvenance()throws Exception{
        snapshotExecutedMatrix(canonicalMatrix(2));var rows=routeSnapshots.selectListByControlledFileId(20L);
        var runtime=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(rows.get(0).getRuntimeApprovalNodesJson(),Map.class);
        var nodes=new ArrayList<>((List<Object>)runtime.get("nodes"));var supervisor=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(0,"SUPERVISOR_REVIEW","主管",0,"USER",99L,List.of(99L),"ANY",null,false,List.of(99L));
        nodes.add(0,supervisor);runtime.put("nodes",nodes);String json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(runtime);
        jdbc.update("UPDATE dcc_controlled_file_route_snapshot SET runtime_approval_nodes_json=? WHERE controlled_file_id=20",json);
        var stage=DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).stageNo(0).stageOrder(0).stageCode("SUPERVISOR_REVIEW").stageName("主管")
                .candidateSourceType("USER").candidateSourceId(99L).candidateSourceIds("99").resolvedUserIds("99").approveMethod("ANY").requireAllApprovals(false).build();
        var first=rows.get(0);stage.setRouteVersionNo(first.getRouteVersionNo());stage.setSourceApprovalMatrixId(first.getSourceApprovalMatrixId());stage.setSourceApprovalMatrixVersion(first.getSourceApprovalMatrixVersion());stage.setSourceApprovalMatrixDigest(first.getSourceApprovalMatrixDigest());
        stage.setSourceApprovalMatrixJson(first.getSourceApprovalMatrixJson());stage.setRuntimeApprovalNodesJson(json);stage.setDerivationVersion(first.getDerivationVersion());routeSnapshots.insert(stage);
    }
    @Test void currentPublicAndHistoricalPublicRoundUseTheirOwnExactGraphAndPersistedProvenance()throws Exception{
        String bpm=publicActive(false);publicProvenance();var row=summary("UPLOAD",bpm,1);var ready=resolve(row);assertEquals("READY",field(ready,"status").toString());
        assertEquals(false,field(ready,"needTraining"));
        assertEquals(DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,field(ready,"flowRule"));String exact=files.selectById(20L).getPreparedProcessDefinitionId();
        engine.getRuntimeService().deleteProcessInstance(bpm,"historical completed source");jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='some-new-native-round',prepared_process_definition_id='new-definition',prepared_definition_digest=? WHERE id=20","b".repeat(64));
        var before=original(20L);var historical=resolve(row);assertEquals(exact,field(historical,"processDefinitionId"));assertEquals("NOT_RECORDED",field(historical,"status").toString());assertEquals("HISTORICAL_CONTRACT_NOT_RECORDED",field(historical,"unavailableReason"));assertNull(field(historical,"flowRule"));assertEquals(before,original(20L));
        assertEquals(false,field(historical,"needTraining"));
    }
    @Test void oldPublicAWithTrueRoundCannotBorrowCurrentBReplacedMatrixOrRuntime()throws Exception{
        String old=publicActive(true);publicProvenance();engine.getRuntimeService().deleteProcessInstance(old,"old A completed");
        jdbc.update("DELETE FROM dcc_controlled_file_route_snapshot WHERE controlled_file_id=20");snapshotExecutedMatrix(canonicalMatrix(3));
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='current-B',prepared_process_definition_id='B-definition',need_training=0 WHERE id=20");
        var before=original(20L);var result=resolve(summary("UPLOAD",old,1));assertEquals("NOT_RECORDED",field(result,"status").toString());assertEquals("HISTORICAL_CONTRACT_NOT_RECORDED",field(result,"unavailableReason"));assertNull(field(result,"executionContract"));assertNull(field(result,"flowRule"));assertEquals(true,field(result,"needTraining"));assertEquals(before,original(20L));
    }
    @Test void legacyHistoryReadsFrozenOldGraphDespiteCurrentPreparedAndNeverBackfills(){
        rejectedOriginalWithoutPreparedFields();String bpm=files.selectById(20L).getProcessInstanceId();jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='new-round',prepared_workflow_contract='PUBLIC_MATRIX_NATIVE_V1',prepared_process_definition_id='different',prepared_definition_digest=? WHERE id=20","b".repeat(64));
        var before=original(20L);var result=resolve(summary("UPLOAD",bpm,1));assertEquals("READY",field(result,"status").toString());assertEquals(DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL,field(result,"flowRule"));assertEquals(before,original(20L));
    }
    @ParameterizedTest @ValueSource(strings={"MISSING","UNFROZEN","UNKNOWN_MISSING"})
    void completedLegacyOrUnknownModelWithoutLaterSnapshotReturnsNotRecordedAfterIdentityProof(String absence){
        rejectedOriginalWithoutPreparedFields();String bpm=files.selectById(20L).getProcessInstanceId();
        if(absence.equals("UNKNOWN_MISSING")){var wrong=engine.getRepositoryService().getBpmnModel(exactId);wrong.getMainProcess().setName("完整但未登记历史图");org.mockito.Mockito.doReturn(wrong).when(definitions).getProcessDefinitionBpmnModel(org.mockito.ArgumentMatchers.anyString());}
        if(absence.equals("UNFROZEN"))jdbc.update("UPDATE dcc_project_application_attributes SET submitted=0 WHERE application_id=20");else jdbc.update("DELETE FROM dcc_project_application_attributes WHERE application_id=20");
        var before=original(20L);var result=resolve(summary("UPLOAD",bpm,1));assertEquals("NOT_RECORDED",field(result,"status").toString());assertEquals("HISTORICAL_CONTRACT_NOT_RECORDED",field(result,"unavailableReason"));assertNull(field(result,"executionContract"));assertNull(field(result,"flowRule"));assertEquals(before,original(20L));
    }
    @Test void identityCorrectCompletedUnknownGraphReturnsTypedUnsupportedAndNoGuessedLegacy(){
        String id=deploy(legacyXml.replace("name=\"培训等待\"","name=\"尚未登记历史模型\""));String bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(id).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("old")))).start().getId();
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,rounds.requireDraft("UPLOAD",20L),bpm,actual));engine.getRuntimeService().deleteProcessInstance(bpm,"actual completed unknown history");
        var before=original(20L);var result=resolve(summary("UPLOAD",bpm,1));assertEquals("UNSUPPORTED_DEFINITION",field(result,"status").toString());assertNull(field(result,"executionContract"));assertNull(field(result,"flowRule"));assertNotNull(field(result,"unavailableReason"));assertEquals(before,original(20L));
    }
    @ParameterizedTest @ValueSource(strings={"FILE","TYPE","ROUND","PROJECT","UNSUBMITTED","PREPARED","PROVENANCE","ACTIVE_UNKNOWN","TRAINING_MISSING","TRAINING_WRONG_TYPE"})
    void badCurrentIdentityNeverBecomesTypedUnavailable(String bad)throws Exception{
        String bpm=publicActive(false);publicProvenance();var row=summary("UPLOAD",bpm,1);
        switch(bad){case "FILE"->row.setControlledFileId(21L);case "TYPE"->row.setApplicationType("REVISION");case "ROUND"->row.setAttributeRound(2);
            case "PROJECT"->jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=6 WHERE id=20");case "UNSUBMITTED"->jdbc.update("UPDATE dcc_project_application_attributes SET submitted=0 WHERE application_id=20");
            case "PREPARED"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=NULL WHERE id=20");case "PROVENANCE"->jdbc.update("UPDATE dcc_controlled_file_route_snapshot SET source_approval_matrix_json=NULL WHERE controlled_file_id=20");
            case "TRAINING_MISSING"->engine.getRuntimeService().removeVariable(bpm,"needTraining");case "TRAINING_WRONG_TYPE"->engine.getRuntimeService().setVariable(bpm,"needTraining","false");
            case "ACTIVE_UNKNOWN"->org.mockito.Mockito.doReturn(engine.getRepositoryService().getBpmnModel(exactId)).when(definitions).getProcessDefinitionBpmnModel(org.mockito.ArgumentMatchers.anyString());}
        var before=original(20L);assertThrows(RuntimeException.class,()->resolve(row));assertEquals(before,original(20L));
    }
    @ParameterizedTest @ValueSource(strings={"APPLICATION_ID","APPLICATION_TYPE","APPLICATION_ROUND","TENANT"})
    void returnedAttributeIdentityIsValidatedEvenIfTheMapperLookupFilterWasCorrect(String conflict)throws Exception{
        String bpm=publicActive(false);publicProvenance();var row=attributeRows.find("UPLOAD",20L,1);
        switch(conflict){case "APPLICATION_ID"->row.setApplicationId(21L);case "APPLICATION_TYPE"->row.setApplicationType("REVISION");case "APPLICATION_ROUND"->row.setApplicationRound(2);case "TENANT"->row.setTenantId(2L);}
        var returned=org.mockito.Mockito.mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper.class);org.mockito.Mockito.when(returned.find("UPLOAD",20L,1)).thenReturn(row);wire(resolver,"attributes",returned);
        var before=original(20L);assertThrows(RuntimeException.class,()->resolve(summary("UPLOAD",bpm,1)));assertEquals(before,original(20L));
    }
    @Test void obsoleteHistoryMissingItsOwnPreparationIsNotRecordedAndNeverBorrowsNative()throws Exception{
        String nativeRound=publicActive(false);publicProvenance();
        jdbc.execute("CREATE TABLE IF NOT EXISTS bpm_form_action_instance(id BIGINT PRIMARY KEY,instance_code VARCHAR(128),tenant_id BIGINT,policy_id BIGINT,applicant_user_id BIGINT,status VARCHAR(64),data_domain VARCHAR(64),system_code VARCHAR(64),object_type VARCHAR(64),object_id VARCHAR(128),object_version VARCHAR(128),action_code VARCHAR(64),object_state VARCHAR(64),idempotency_key VARCHAR(128),business_context_json CLOB,form_data_json CLOB,bpm_process_instance_id VARCHAR(128),creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");
        java.nio.file.Path root=java.nio.file.Path.of("").toAbsolutePath();while(root!=null&&!java.nio.file.Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();String id=deploy(java.nio.file.Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/obsolete.bpmn20.xml")));
        engine.getIdentityService().setAuthenticatedUserId("99");String bpm;
        try{bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(id).businessKey("FORM_ACTION:ACTUAL-CODE")
                .variables(Map.of("formInstanceId",501L,"controlledFileId",20L,"systemCode","DCC","objectType","CONTROLLED_FILE","actionCode","OBSOLETE","objectId","20","objectVersion","A/1","fileVersion","A/1","tenantId",1L)).start().getId();}
        finally{engine.getIdentityService().setAuthenticatedUserId(null);}
        tx().executeWithoutResult(s->rounds.bind(5L,"OBSOLETE",20L,bpm));tx().executeWithoutResult(s->{attributes.beginDraft(99L,5L,"OBSOLETE",20L,1);attributes.saveDraft(99L,5L,"OBSOLETE",20L,1,actual);attributes.freeze(99L,5L,"OBSOLETE",20L,1);});
        engine.getRuntimeService().deleteProcessInstance(bpm,"completed historical obsolete form");
        jdbc.update("INSERT INTO bpm_form_action_instance(id,instance_code,tenant_id,applicant_user_id,status,system_code,object_type,object_id,object_version,action_code,form_data_json,bpm_process_instance_id) VALUES(501,'ACTUAL-CODE',1,99,'COMPLETED','DCC','CONTROLLED_FILE','20','A/1','OBSOLETE','{}',?)",bpm);
        var before=original(20L);var result=resolve(summary("OBSOLETE",bpm,1));assertEquals("NOT_RECORDED",field(result,"status").toString());assertNull(field(result,"flowRule"));assertEquals(501L,field(result,"formInstanceId"));assertEquals("ACTUAL-CODE",field(result,"formInstanceCode"));
        assertEquals(before,original(20L));assertEquals(nativeRound,files.selectById(20L).getProcessInstanceId());
    }
}
