package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual Query + HistoryGuard + single round leaf; immutable graphs/rounds/attributes are actual H2/Flowable. */
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w08_round_flow_query;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=5000")
class DccW08RoundFlowQueryDatabaseTest extends DccRoundFlowContractTest {
    @Resource DccControlledFileApplicationEvidenceMapper evidenceRows;
    DccControlledFileQueryServiceImpl query;

    private String publicRound(boolean training)throws Exception {
        engine.getIdentityService().setAuthenticatedUserId("99");
        try{return publicActive(training);}finally{engine.getIdentityService().setAuthenticatedUserId(null);}
    }
    private DccControlledFileQueryServiceImpl query() {
        resolver=spy(resolver);
        query=new DccControlledFileQueryServiceImpl();
        for(var field:query.getClass().getDeclaredFields())
            if(!Modifier.isStatic(field.getModifiers())&&field.isAnnotationPresent(Resource.class))
                wire(query,field.getName(),mock(field.getType()));
        var detail=new DccControlledFileDetailAuthorizationGuard();
        for(var field:detail.getClass().getDeclaredFields())
            if(!Modifier.isStatic(field.getModifiers())&&field.isAnnotationPresent(Resource.class))wire(detail,field.getName(),mock(field.getType()));
        var scope=(DccControlledFileAssignmentScopeService)ReflectionTestUtils.getField(detail,"assignmentScopeService");
        when(scope.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        var history=spy(new DccApplicationHistoryGuard());wire(history,"processes",processes);
        wire(query,"controlledFileMapper",files,"applicationEvidenceMapper",evidenceRows,"detailAuthorizationGuard",detail,"applicationHistoryGuard",history);
        if(Arrays.stream(query.getClass().getDeclaredFields()).anyMatch(field->field.getName().equals("roundFlowContracts")))wire(query,"roundFlowContracts",resolver);
        return query;
    }
    private List<DccApplicationRoundSummary> rows(){return tx().execute(status->query.listApplicationRounds(99L,20L));}
    private DccApplicationRoundSummary only(){var result=rows();assertEquals(1,result.size());return result.get(0);}

    @Test void currentPublicReadyIsAttachedOnlyAfterTheActualRowAndHistoryGuards()throws Exception{
        String bpm=publicRound(true);publicProvenance();query();var before=original(20L);
        var result=only();assertEquals("UPLOAD",result.getApplicationType());assertEquals(bpm,result.getBpmRound());assertEquals(1,result.getAttributeRound());
        var flow=result.getFlowContract();assertNotNull(flow);assertEquals(DccBoundWorkflowContractResolver.RoundContractStatus.READY,flow.status());
        assertEquals(true,flow.needTraining());assertEquals(bpm,flow.bpmRound());assertEquals(20L,flow.controlledFileId());
        assertEquals(files.selectById(20L).getPreparedProcessDefinitionId(),flow.processDefinitionId());assertEquals(before,original(20L));
        var order=inOrder((DccApplicationHistoryGuard)ReflectionTestUtils.getField(query,"applicationHistoryGuard"),resolver);
        order.verify((DccApplicationHistoryGuard)ReflectionTestUtils.getField(query,"applicationHistoryGuard")).require(any(),eq("UPLOAD"),eq(bpm));
        order.verify(resolver).resolveRoundFlowContract(any(),any());
    }
    @Test void completedPublicWithoutItsOwnFrozenSourceKeepsTheRowAndTypedNotRecorded()throws Exception{
        String old=publicRound(false);publicProvenance();engine.getRuntimeService().deleteProcessInstance(old,"completed original history");
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='current-other',prepared_process_definition_id='other-definition',prepared_definition_digest=? WHERE id=20","b".repeat(64));
        query();var before=original(20L);var row=only();assertEquals(old,row.getBpmRound());assertNotNull(row.getFlowContract());
        assertEquals(DccBoundWorkflowContractResolver.RoundContractStatus.NOT_RECORDED,row.getFlowContract().status());
        assertEquals("HISTORICAL_CONTRACT_NOT_RECORDED",row.getFlowContract().unavailableReason());assertNull(row.getFlowContract().flowRule());
        assertEquals(before,original(20L));
    }
    @Test void actualCompletedUnknownModelRemainsSelectableWithoutAnyGuessedFlow(){
        String id=deploy(legacyXml.replace("name=\"培训等待\"","name=\"unknown exact historical graph\""));
        engine.getIdentityService().setAuthenticatedUserId("99");String bpm;
        try{bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(id).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"needTraining",false,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("UNKNOWN")))).start().getId();}
        finally{engine.getIdentityService().setAuthenticatedUserId(null);}
        tx().executeWithoutResult(status->snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,rounds.requireDraft("UPLOAD",20L),bpm,actual));
        engine.getRuntimeService().deleteProcessInstance(bpm,"actual completed unknown history");query();var row=only();
        assertEquals(bpm,row.getBpmRound());assertNotNull(row.getFlowContract());
        assertEquals(DccBoundWorkflowContractResolver.RoundContractStatus.UNSUPPORTED_DEFINITION,row.getFlowContract().status());
        assertNull(row.getFlowContract().executionContract());assertNull(row.getFlowContract().flowRule());
    }
    @ParameterizedTest @ValueSource(strings={"actor","badRow","history"})
    void allOriginalGuardsCompleteBeforeAnyFlowLeafCanRead(String invalid)throws Exception{
        String bpm=publicRound(false);publicProvenance();query();
        if("actor".equals(invalid)){assertThrows(RuntimeException.class,()->tx().execute(status->query.listApplicationRounds(77L,20L)));}
        else if("history".equals(invalid)){
            doThrow(new IllegalStateException("actual history permission identity refused")).when((DccApplicationHistoryGuard)ReflectionTestUtils.getField(query,"applicationHistoryGuard")).require(any(),any(),any());
            assertEquals("actual history permission identity refused",assertThrows(IllegalStateException.class,this::rows).getMessage());
        }else{var mapping=mock(DccControlledFileApplicationEvidenceMapper.class);var bad=summary("UPLOAD",bpm,0);
            when(mapping.selectApplicationRounds(1L,5L,20L)).thenReturn(List.of(summary("UPLOAD",bpm,1),bad));wire(query,"applicationEvidenceMapper",mapping);
            assertThrows(IllegalStateException.class,this::rows);}
        verify(resolver,never()).resolveRoundFlowContract(any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"tenant","file","type","attribute","bpm","nullResult"})
    void aFlowResultForAnyOtherTupleCannotBeAttachedToThisPersistedRow(String invalid)throws Exception{
        publicRound(false);publicProvenance();query();
        doAnswer(call->{var original=(DccBoundWorkflowContractResolver.RoundFlowContract)call.callRealMethod();
            if("nullResult".equals(invalid))return null;
            return new DccBoundWorkflowContractResolver.RoundFlowContract("tenant".equals(invalid)?2L:original.tenantId(),
                    "file".equals(invalid)?21L:original.controlledFileId(),"type".equals(invalid)?"REVISION":original.applicationType(),
                    "attribute".equals(invalid)?2:original.attributeRound(),"bpm".equals(invalid)?"other-bpm":original.bpmRound(),original.processDefinitionKey(),
                    original.processDefinitionId(),original.deploymentId(),original.definitionDigest(),original.executionContract(),original.flowRule(),original.needTraining(),
                    original.formInstanceId(),original.formInstanceCode(),original.status(),original.unavailableReason());}).when(resolver).resolveRoundFlowContract(any(),any());
        assertThrows(IllegalStateException.class,this::rows);
    }
    @Test void exactReadFailureMustNotBecomeAFalseOldRuleOrEmptyList()throws Exception{
        publicRound(false);publicProvenance();query();
        doThrow(new IllegalStateException("actual definition model read failed")).when(resolver).resolveRoundFlowContract(any(),any());
        assertEquals("actual definition model read failed",assertThrows(IllegalStateException.class,this::rows).getMessage());
    }
    @ParameterizedTest @ValueSource(strings={"missing","unfrozen"})
    void typedMissingFlowContractDoesNotPreventTheOriginalHistoricalEvidenceState(String state){
        engine.getIdentityService().setAuthenticatedUserId("99");
        try{rejectedOriginalWithoutPreparedFields();}finally{engine.getIdentityService().setAuthenticatedUserId(null);}
        String bpm=files.selectById(20L).getProcessInstanceId();
        if("missing".equals(state))jdbc.update("DELETE FROM dcc_project_application_attributes WHERE application_id=20");
        else jdbc.update("UPDATE dcc_project_application_attributes SET submitted=0 WHERE application_id=20");
        query();var before=original(20L);var row=only();
        assertNotNull(row.getFlowContract());assertEquals(DccBoundWorkflowContractResolver.RoundContractStatus.NOT_RECORDED,row.getFlowContract().status());
        assertNull(row.getFlowContract().flowRule());
        var evidence=tx().execute(status->query.getApplicationEvidence(99L,20L,"UPLOAD",bpm));
        assertFalse(evidence.recorded());assertEquals("missing".equals(state)?"NOT_RECORDED":"NOT_FROZEN",evidence.unavailableReason());
        assertEquals(bpm,evidence.bpmRound());assertEquals(1,evidence.attributeRound());assertEquals(before,original(20L));
    }
    @Test void mixedActualPublicHistoryAndCurrentLegacyRoundKeepIndependentResultsAndValidateAllRowsFirst()throws Exception{
        String old=publicRound(true);publicProvenance();engine.getRuntimeService().deleteProcessInstance(old,"completed A public history");
        engine.getIdentityService().setAuthenticatedUserId("99");String current;
        try{current=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(exactId).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"needTraining",false,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("CURRENT-DECLARED-ROUND")))).start().getId();}
        finally{engine.getIdentityService().setAuthenticatedUserId(null);}
        tx().executeWithoutResult(status->{int next=rounds.reserveDraft(5L,"UPLOAD",20L);assertEquals(2,next);
            var oldAttrs=attributeRows.find("UPLOAD",20L,1);var submitted=new cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO();
            org.springframework.beans.BeanUtils.copyProperties(oldAttrs,submitted);submitted.setId(null);submitted.setCreateTime(null);submitted.setUpdateTime(null);
            submitted.setCreator(null);submitted.setUpdater(null);submitted.setApplicationRound(next);assertEquals(1,attributeRows.insert(submitted));
            rounds.bindReservedDraft(5L,"UPLOAD",20L,next,current);});
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,process_definition_key=?,prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=?,need_training=0 WHERE id=20",
                current,DccControlledFileProcessDefinitionKeys.UPLOAD,DccApplicationExecutionContractService.LEGACY,exactId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exactId)));
        query();var before=original(20L);var result=rows();assertEquals(2,result.size());
        var currentRow=result.stream().filter(row->row.getBpmRound().equals(current)).findFirst().orElseThrow();
        var historyRow=result.stream().filter(row->row.getBpmRound().equals(old)).findFirst().orElseThrow();
        assertEquals(DccBoundWorkflowContractResolver.RoundContractStatus.READY,currentRow.getFlowContract().status());
        assertEquals(DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL,currentRow.getFlowContract().flowRule());
        assertEquals(false,currentRow.getFlowContract().needTraining());
        assertEquals(DccBoundWorkflowContractResolver.RoundContractStatus.NOT_RECORDED,historyRow.getFlowContract().status());
        assertNull(historyRow.getFlowContract().flowRule());assertEquals(true,historyRow.getFlowContract().needTraining());
        assertNotEquals(currentRow.getFlowContract().processDefinitionId(),historyRow.getFlowContract().processDefinitionId());
        assertEquals(before,original(20L));
        var history=(DccApplicationHistoryGuard)ReflectionTestUtils.getField(query,"applicationHistoryGuard");var order=inOrder(history,resolver);
        order.verify(history).require(any(),eq("UPLOAD"),eq(current));order.verify(history).require(any(),eq("UPLOAD"),eq(old));
        order.verify(resolver).resolveRoundFlowContract(any(),argThat(row->row.getBpmRound().equals(current)));
        order.verify(resolver).resolveRoundFlowContract(any(),argThat(row->row.getBpmRound().equals(old)));
    }
}
