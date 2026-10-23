package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

/** Exact active/historical identity and full graph classification with real Flowable/round/attribute facts. */
class DccBoundExecutionGraphContractTest extends DccPreparedExecutionInheritanceDatabaseTest {
    @BeforeEach void leafBoundResolver(){var resolver=new DccBoundWorkflowContractResolver();wire(resolver,"definitions",definitions,"processes",processes,"rounds",rounds);
        if(Arrays.stream(resolver.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("attributes")))wire(resolver,"attributes",attributeRows);wire(contracts,"boundContracts",resolver);}
    String publicActive(boolean training)throws Exception{
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String id=deploy(Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml")));
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(id));
        var prepared=contracts.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC,id,digest,DccControlledFileProcessDefinitionKeys.UPLOAD);
        String bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(id).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"needTraining",training,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("dept-51")))).start().getId();
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,rounds.requireDraft("UPLOAD",20L),bpm,actual));
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=?,process_instance_id=?,process_definition_key=?,status='PENDING_MATRIX_REVIEW',need_training=? WHERE id=20",
                prepared.executionContract(),id,digest,bpm,DccControlledFileProcessDefinitionKeys.UPLOAD,training);
        return bpm;
    }
    Object resolve(){return ReflectionTestUtils.invokeMethod(contracts,"resolveBoundExecution",files.selectById(20L));}
    Object value(Object result,String field){return ReflectionTestUtils.invokeMethod(result,field);}
    @Test void registeredPublicUsesFrozenWholeGraphInRuntimeAndHistoryWithoutLatest()throws Exception{
        String bpm=publicActive(true);Object runtime=resolve();assertEquals("PUBLIC_MATRIX_NATIVE_V1",value(runtime,"executionContract"));
        assertEquals(true,value(runtime,"trainingAfterDocumentControl"));assertEquals(true,value(runtime,"active"));
        String oldId=files.selectById(20L).getPreparedProcessDefinitionId();deploy(legacyXml.replace("name=\"培训等待\"","name=\"不同后部署图\""));
        assertEquals(oldId,value(resolve(),"processDefinitionId"));engine.getRuntimeService().deleteProcessInstance(bpm,"actual historical fixture");
        assertEquals(false,value(resolve(),"active"));assertEquals(oldId,value(resolve(),"processDefinitionId"));verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void legacyBoundGraphIsProvenByExactModelWithoutChangingHistoricalPreparation(){
        rejectedOriginalWithoutPreparedFields();var before=original(20L);Object result=resolve();assertEquals(DccApplicationExecutionContractService.LEGACY,value(result,"executionContract"));
        assertEquals(false,value(result,"trainingAfterDocumentControl"));assertEquals(before,original(20L));verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void actualLegacyV4HistoryWithoutLaterRoundTableFactRemainsReadOnlyVerifiable(){
        rejectedOriginalWithoutPreparedFields();jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id=20");var before=original(20L);
        Object result=resolve();assertEquals(DccApplicationExecutionContractService.LEGACY,value(result,"executionContract"));assertNull(value(result,"attributeRound"));assertEquals(before,original(20L));
    }
    @ParameterizedTest @ValueSource(strings={"PREPARED_ID","PREPARED_DIGEST","PARTIAL","TENANT","BUSINESS_KEY","VARIABLE","BOUND_ROUND","UNREGISTERED","PROJECT","ATTRIBUTE_PROJECT","ATTRIBUTE_UNSUBMITTED","ATTRIBUTE_MISSING"})
    void mismatchedPreparedOrExactBoundSourceFailsRatherThanKeyBasedDispatch(String failure)throws Exception{
        String bpm=publicActive(true);
        switch(failure){
            case "PREPARED_ID"->jdbc.update("UPDATE dcc_controlled_file SET prepared_process_definition_id=? WHERE id=20",exactId);
            case "PREPARED_DIGEST"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=? WHERE id=20","b".repeat(64));
            case "PARTIAL"->jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=NULL WHERE id=20");
            case "TENANT"->jdbc.update("UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=20");
            case "VARIABLE"->engine.getRuntimeService().setVariable(bpm,"controlledFileId",21L);
            case "BOUND_ROUND"->jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id=20");
            case "PROJECT"->jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=6 WHERE id=20");
            case "ATTRIBUTE_PROJECT"->jdbc.update("UPDATE dcc_project_application_attributes SET project_code_id=6 WHERE application_id=20");
            case "ATTRIBUTE_UNSUBMITTED"->jdbc.update("UPDATE dcc_project_application_attributes SET submitted=0 WHERE application_id=20");
            case "ATTRIBUTE_MISSING"->jdbc.update("DELETE FROM dcc_project_application_attributes WHERE application_id=20");
            case "BUSINESS_KEY"->{var wrong=mock(org.flowable.engine.runtime.ProcessInstance.class);when(wrong.getId()).thenReturn(bpm);when(wrong.getTenantId()).thenReturn("1");when(wrong.getBusinessKey()).thenReturn("21");when(wrong.getProcessDefinitionId()).thenReturn(files.selectById(20L).getPreparedProcessDefinitionId());when(wrong.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.UPLOAD);when(wrong.getProcessVariables()).thenReturn(Map.of("controlledFileId",20L));when(processes.getProcessInstance(bpm)).thenReturn(wrong);}
            case "UNREGISTERED"->doReturn(engine.getRepositoryService().getBpmnModel(exactId)).when(definitions).getProcessDefinitionBpmnModel(anyString());
        }
        var before=original(20L);assertThrows(RuntimeException.class,this::resolve);assertEquals(before,original(20L));verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void catalogMismatchLogsBoundDiagnosticContextAndRethrowsOriginalFailure() throws Exception {
        publicActive(true);
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=? WHERE id=20", "PUBLIC_MATRIX_NATIVE_V2");
        String runtimeDefinitionId=files.selectById(20L).getPreparedProcessDefinitionId();
        Logger logger=(Logger) LoggerFactory.getLogger(DccBoundWorkflowContractResolver.class);
        var appender=new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        var before=original(20L);
        try {
            var failure=assertThrows(IllegalStateException.class,this::resolve);
            assertEquals("EXECUTION_CONTRACT_UNREGISTERED_DEFINITION",failure.getMessage());
            var event=appender.list.stream().filter(e->e.getMessage().contains("DCC_EXECUTION_CONTRACT_MISMATCH")).findFirst().orElseThrow();
            var text=event.getFormattedMessage();
            assertAll(() -> assertTrue(text.contains("fileId=20")),() -> assertTrue(text.contains("processDefinitionId="+runtimeDefinitionId)),
                    () -> assertTrue(text.contains("contract=PUBLIC_MATRIX_NATIVE_V2")),() -> assertTrue(text.contains("processKey="+DccControlledFileProcessDefinitionKeys.UPLOAD)),
                    () -> assertTrue(text.contains("digest=")));
            assertEquals(Level.ERROR,event.getLevel());
            assertEquals(before,original(20L));
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
