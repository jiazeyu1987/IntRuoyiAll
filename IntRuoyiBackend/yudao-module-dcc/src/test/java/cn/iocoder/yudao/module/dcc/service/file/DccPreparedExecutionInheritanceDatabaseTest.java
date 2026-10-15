package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import jakarta.annotation.Resource;
import org.flowable.engine.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual initializer/attributes/rounds/prepared CAS in isolated H2, actual immutable v4 in Flowable.
 * Directory/category permissions are explicit external ports; no Query/Revision or production DB write. */
@Import({DccWorkingApplicationDraftInitializer.class,DccApplicationRoundService.class,
        DccProjectApplicationSnapshotService.class,DccProjectAttributesService.class,
        DccPreparedExecutionInheritanceDatabaseTest.Beans.class})
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_prepared_inheritance;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=5000")
class DccPreparedExecutionInheritanceDatabaseTest extends BaseDbUnitTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class Beans {
        @org.springframework.context.annotation.Bean JdbcTemplate jdbc(DataSource source){return new JdbcTemplate(source);}
    }
    @Resource DccWorkingApplicationDraftInitializer initializer;
    @Resource DccApplicationRoundService rounds;
    @Resource DccProjectApplicationSnapshotService snapshots;
    @Resource DccProjectAttributesService attributes;
    @Resource(name="dccControlledFileMapper") DccControlledFileMapper files;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masters;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper attributeRows;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper routeSnapshots;
    @Resource JdbcTemplate jdbc;
    @Resource PlatformTransactionManager transactions;
    @MockitoBean DccProjectAccessService access;
    @MockitoBean DccControlledFileCategoryPermissionSupport permissions;
    DccApplicationExecutionContractService contracts;
    ProcessEngine engine;
    BpmProcessDefinitionService definitions;
    BpmProcessInstanceService processes;
    String exactId,legacyXml;
    DccExecutionDefinitionTransitions transitions=new DccExecutionDefinitionTransitions();
    final DccProjectAttributes actual=new DccProjectAttributes(List.of("CE"),null,"Y","N","N",null);
    TransactionTemplate tx(){return new TransactionTemplate(transactions);}
    static void wire(Object target,Object... values){for(int i=0;i<values.length;i+=2)ReflectionTestUtils.setField(target,(String)values[i],values[i+1]);}
    @BeforeEach void seed()throws Exception{
        TenantContextHolder.setTenantId(1L);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status,default_attributes_json,deleted) VALUES(5,1,'PREPARED','PREPARED','ENABLE',?,0)",attributes.encode(actual));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status,deleted) VALUES(10,1,2,3,'SOP','PREPARED-1',5,6,'PREPARED-1','ACTIVE_CHAIN',0)");
        when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        engine=ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().setJdbcUrl("jdbc:h2:mem:w05_inherit_engine_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate("true").setAsyncExecutorActivate(false).buildProcessEngine();
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        var matcher=java.util.regex.Pattern.compile("SET @dcc_upload_bpmn = '([\\s\\S]*?)';").matcher(Files.readString(root.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql")));
        assertTrue(matcher.find());legacyXml=matcher.group(1);exactId=deploy(legacyXml);
        definitions=mock(BpmProcessDefinitionService.class);
        when(definitions.getProcessDefinition(anyString())).thenAnswer(c->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(c.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(c->engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
        var reader=new BpmProcessInstanceServiceImpl();wire(reader,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService());
        processes=mock(BpmProcessInstanceService.class);
        when(processes.getProcessInstance(anyString())).thenAnswer(c->reader.getProcessInstance(c.getArgument(0)));
        when(processes.getHistoricProcessInstance(anyString())).thenAnswer(c->reader.getHistoricProcessInstance(c.getArgument(0)));
        var boundContracts=new DccBoundWorkflowContractResolver();wire(boundContracts,"definitions",definitions,"processes",processes,"rounds",rounds,"attributes",attributeRows);
        var effective=new DccLifecycleEffectiveContractResolver();wire(effective,"boundContracts",boundContracts);
        contracts=new DccApplicationExecutionContractService();wire(contracts,"definitions",definitions,"files",files);
        // Reflection keeps the behavioral RED compilable before the new official method and dependencies exist.
        for(String field:List.of("rounds","effectiveContractResolver","sourceAttributes","projectAccess","categoryPermissions","transitions","masters","processes","routeSnapshots","routeResolver"))
            if(Arrays.stream(contracts.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals(field)))
                wire(contracts,field,switch(field){case "rounds"->rounds;case "effectiveContractResolver"->effective;case "sourceAttributes"->attributeRows;
                    case "projectAccess"->access;case "categoryPermissions"->permissions;case "transitions"->transitions;case "masters"->masters;case "processes"->processes;
                    case "routeSnapshots"->routeSnapshots;case "routeResolver"->new DccControlledFileApprovalRouteAssigneeResolver();default->throw new AssertionError(field);});
        add(20L,null);tx().executeWithoutResult(s->initializer.initialize(99L,null,20L,actual));
    }
    @AfterEach void close(){if(engine!=null)engine.close();TenantContextHolder.clear();}
    String deploy(String xml){var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("real.bpmn20.xml",xml).deploy();return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId();}
    void add(Long id,Long source){files.insert(DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).dccProjectCodeId(5L).categoryId(2L).directoryId(3L)
            .fileTypeTaxonomyId(6L).fileNumber("PREPARED-1").fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf").sourceFileId(100L).originalFileId(100L).sourceSha256("a".repeat(64))
            .requesterId(99L).submitterId(99L).versionNo(source==null?"A/1":"A/1-"+(id-20)).changeType("NEW").processType("CONTROLLED_FILE").status("WORKING").predecessorControlledFileId(source)
            .previousSourceSha256(source==null?null:"a".repeat(64)).build());}
    void copy(Long id,Long source){add(id,source);tx().executeWithoutResult(s->initializer.initialize(99L,source,id,null));}
    void prepareRoot(){tx().executeWithoutResult(s->contracts.prepareLegacy(99L,files.selectById(20L),exactId));}
    void startThroughActualWorkflowAndOfficialBpm(Long fileId,String definitionId){
        var info=new cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO();
        when(definitions.getProcessDefinitionInfo(anyString())).thenReturn(info);when(definitions.canUserStartProcessDefinition(info,99L)).thenReturn(true);
        var service=spy(new BpmProcessInstanceServiceImpl());wire(service,"runtimeService",engine.getRuntimeService(),"processDefinitionService",definitions,
                "adminUserApi",mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class));
        doReturn(new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO().setActivityNodes(new ArrayList<>())).when(service).getApprovalDetail(eq(99L),any());
        var api=new cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApiImpl();wire(api,"processInstanceService",service);
        var workflow=new DccControlledFileWorkflowServiceImpl();wire(workflow,"executionContracts",contracts,"bpmProcessInstanceApi",api,
                "approvalRouteAssigneeResolver",new DccControlledFileApprovalRouteAssigneeResolver());
        var route=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(30L).versionNo(1).build(),List.of(
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(1,"MATRIX_REVIEW","会签",1,"DEPT",51L,List.of(51L),"ALL",100,true,List.of(99L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(2,"MATRIX_APPROVAL","批准",2,"USER",99L,List.of(99L),"ANY",null,false,List.of(99L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(3,"DOC_CONTROL_REVIEW","文控",3,"USER",99L,List.of(99L),"ANY",null,false,List.of(99L))));
        var file=files.selectById(fileId);String key="NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION;
        String bpm=ReflectionTestUtils.invokeMethod(workflow,"createApprovalProcess",99L,file,route,key);
        var instance=engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(bpm).singleResult();
        assertEquals(definitionId,instance.getProcessDefinitionId());assertEquals(fileId.toString(),instance.getBusinessKey());
        assertEquals("99",instance.getStartUserId());assertEquals("1",instance.getTenantId());
    }
    DccApplicationExecutionContractService.Prepared inherit(Long id){var file=files.selectById(id);
        var source=snapshots.readReservedDraft(99L,5L,"NEW".equals(file.getChangeType())?"UPLOAD":"REVISION",id);
        return tx().execute(s->ReflectionTestUtils.invokeMethod(contracts,"inheritPrepared",99L,id,source.getSourceApplicationId(),source.getSourceApplicationRound()));}
    Map<String,Object> original(Long id){var row=jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=?",id);
        row.replaceAll((key,value)->value instanceof byte[] bytes?HexFormat.of().formatHex(bytes):value);return row;}
    @Test void consecutiveCopiesInheritTheExactPreparedRootWithoutChangingItsHistoryOrUsingLatest(){
        prepareRoot();var before=original(20L);copy(21L,20L);var first=inherit(21L);copy(22L,21L);
        String latest=deploy(legacyXml.replace("name=\"培训等待\"","name=\"新图\""));var second=inherit(22L);
        assertEquals(first,second);assertEquals(exactId,second.processDefinitionId());assertNotEquals(latest,second.processDefinitionId());
        assertEquals(before,original(20L));assertEquals(21L,snapshots.readReservedDraft(99L,5L,"UPLOAD",22L).getSourceApplicationId());
        assertEquals(second,inherit(22L));startThroughActualWorkflowAndOfficialBpm(22L,exactId);verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    void rejectedOriginalWithoutPreparedFields(){
        String bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(exactId).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"needTraining",false,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("OLD-OBLIGATION")))).start().getId();
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,rounds.requireDraft("UPLOAD",20L),bpm,actual));
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED',process_instance_id=?,process_definition_key=? WHERE id=20",bpm,DccControlledFileProcessDefinitionKeys.UPLOAD);
        engine.getRuntimeService().deleteProcessInstance(bpm,"actual old rejected round");
    }
    @Test void exactHistoricalSourceRoundRestoresContractAcrossUnpreparedIntermediateBodyWithoutBackfill(){
        rejectedOriginalWithoutPreparedFields();var before=original(20L);copy(21L,20L);copy(22L,21L);
        deploy(legacyXml.replace("name=\"培训等待\"","name=\"最新不同图\""));var prepared=inherit(22L);
        assertEquals(exactId,prepared.processDefinitionId());assertEquals(DccApplicationExecutionContractService.LEGACY,prepared.executionContract());
        assertNull(files.selectById(21L).getPreparedWorkflowContract());assertEquals(before,original(20L));
        verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void absentAuthoritativeRootIsRejectedWithZeroPreparedWrites(){copy(21L,20L);assertThrows(RuntimeException.class,()->inherit(21L));assertNull(files.selectById(21L).getPreparedWorkflowContract());}
    @ParameterizedTest @ValueSource(strings={"TENANT","MASTER","PROJECT","TYPE","CATEGORY","ROUND","CYCLE","PARTIAL","ACTOR"})
    void mismatchedOrCorruptSourceNeverPreparesTarget(String kind){
        prepareRoot();copy(21L,20L);
        switch(kind){
            case "TENANT"->jdbc.update("UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=20");
            case "MASTER"->jdbc.update("UPDATE dcc_controlled_file SET master_id=11 WHERE id=20");
            case "PROJECT"->jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=6 WHERE id=20");
            case "TYPE"->jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION' WHERE id=20");
            case "CATEGORY"->jdbc.update("UPDATE dcc_controlled_file SET category_id=3 WHERE id=20");
            case "ROUND"->jdbc.update("UPDATE dcc_project_application_attributes SET source_application_round=99 WHERE application_id=21");
            case "CYCLE"->{jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=NULL,prepared_process_definition_id=NULL,prepared_definition_digest=NULL,predecessor_controlled_file_id=21 WHERE id=20");jdbc.update("UPDATE dcc_project_application_attributes SET source_application_id=21,source_application_round=1 WHERE application_id=20");}
            case "PARTIAL"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=NULL WHERE id=20");
            case "ACTOR"->jdbc.update("UPDATE dcc_controlled_file SET requester_id=88 WHERE id=21");
        }
        assertThrows(RuntimeException.class,()->inherit(21L));assertNull(files.selectById(21L).getPreparedWorkflowContract());
    }
    @Test void lateCallerFailureRollsBackPreparationAndOriginalSourceRemainsUnchanged(){
        prepareRoot();copy(21L,20L);var before=original(20L);
        assertThrows(IllegalStateException.class,()->tx().execute(s->{inherit(21L);throw new IllegalStateException("late caller failure");}));
        assertNull(files.selectById(21L).getPreparedProcessDefinitionId());assertEquals(before,original(20L));
    }
    @Test void missingOuterTransactionIsRejectedBeforeAnyPreparation(){prepareRoot();copy(21L,20L);
        assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(contracts,"inheritPrepared",99L,21L,20L,1));assertNull(files.selectById(21L).getPreparedWorkflowContract());}
    @Test void candidateBeforeItsAttributeInitializationInheritsOnlyTheExactSelectedBody(){
        prepareRoot();add(21L,20L);jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_controlled_file_id=20,selected_iteration_version_no='A/1' WHERE id=21");
        var prepared=tx().execute(s->ReflectionTestUtils.<DccApplicationExecutionContractService.Prepared>invokeMethod(contracts,"inheritPrepared",99L,21L,20L,1));
        assertEquals(exactId,prepared.processDefinitionId());assertNull(attributeRows.find("UPLOAD",21L,1));
        tx().executeWithoutResult(s->initializer.initialize(99L,20L,21L,null));assertEquals(20L,attributeRows.find("UPLOAD",21L,1).getSourceApplicationId());
    }
    String controlledRootAndRevisionTarget()throws Exception{
        rejectedOriginalWithoutPreparedFields();jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
        add(21L,20L);jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION',revision_base_active_controlled_file_id=20,version_no='A/1-1' WHERE id=21");
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        var matcher=java.util.regex.Pattern.compile("SET @dcc_revision_bpmn = '([\\s\\S]*?)';").matcher(Files.readString(root.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql")));
        assertTrue(matcher.find());return deploy(matcher.group(1));
    }
    @Test void controlledUploadRequiresAnExplicitExactRevisionTransitionAndKeepsOldHistory()throws Exception{
        String revision=controlledRootAndRevisionTarget();var before=original(20L);
        assertThrows(RuntimeException.class,()->tx().execute(s->ReflectionTestUtils.invokeMethod(contracts,"inheritPrepared",99L,21L,20L,1)));
        assertNull(files.selectById(21L).getPreparedProcessDefinitionId());
        transitions.setDefinitionTransitions(List.of(new DccExecutionDefinitionTransitions.Transition(exactId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exactId)),revision,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revision)))));
        var prepared=tx().execute(s->ReflectionTestUtils.<DccApplicationExecutionContractService.Prepared>invokeMethod(contracts,"inheritPrepared",99L,21L,20L,1));
        assertEquals(revision,prepared.processDefinitionId());assertEquals(DccControlledFileProcessDefinitionKeys.REVISION,prepared.processKey());
        assertNotEquals(exactId,prepared.processDefinitionId());assertEquals(before,original(20L));
        tx().executeWithoutResult(s->initializer.initialize(99L,20L,21L,null));
        assertNull(attributeRows.find("REVISION",21L,1).getSourceApplicationId(),"controlled baseline starts a new attribute application as before");
        startThroughActualWorkflowAndOfficialBpm(21L,revision);
        verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @ParameterizedTest @ValueSource(strings={"DIGEST","WRONG_KEY","DUPLICATE","MISSING_ID","UNKNOWN_GRAPH"})
    void invalidTransitionNeverPreparesOrStartsTheNewRevision(String failure)throws Exception{
        String revision=controlledRootAndRevisionTarget();String sourceDigest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exactId));
        if(failure.equals("UNKNOWN_GRAPH")){
            String xml=new String(new org.flowable.bpmn.converter.BpmnXMLConverter().convertToXML(engine.getRepositoryService().getBpmnModel(revision)),java.nio.charset.StandardCharsets.UTF_8);
            revision=deploy(xml.replace("name=\"培训等待\"","name=\"未登记新图\""));
        }
        String targetId=failure.equals("WRONG_KEY")?exactId:failure.equals("MISSING_ID")?"missing":revision;
        String targetDigest=failure.equals("DIGEST")?"b".repeat(64):DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revision));
        var mapping=new DccExecutionDefinitionTransitions.Transition(exactId,sourceDigest,targetId,targetDigest);
        transitions.setDefinitionTransitions(failure.equals("DUPLICATE")?List.of(mapping,mapping):List.of(mapping));
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.inheritPrepared(99L,21L,20L,1)));
        assertNull(files.selectById(21L).getPreparedWorkflowContract());assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    String rejectedPublicSource()throws Exception{
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String publicId=deploy(Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml")));
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(publicId));
        var verified=contracts.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC,publicId,digest,DccControlledFileProcessDefinitionKeys.UPLOAD);
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=? WHERE id=20",verified.executionContract(),verified.processDefinitionId(),verified.definitionDigest());
        String bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(publicId).businessKey("20").variables(Map.of("controlledFileId",20L)).start().getId();
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,rounds.requireDraft("UPLOAD",20L),bpm,actual));
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED',process_instance_id=?,process_definition_key=? WHERE id=20",bpm,DccControlledFileProcessDefinitionKeys.UPLOAD);
        engine.getRuntimeService().deleteProcessInstance(bpm,"public source fixture rejected history");return publicId;
    }
    @Test void preparedPublicHistoricalRoundRetainsItsExactFullGraphAndStartsTheSameDefinition()throws Exception{
        String publicId=rejectedPublicSource();var before=original(20L);copy(21L,20L);copy(22L,21L);
        var prepared=inherit(22L);assertEquals(DccApplicationExecutionContractService.PUBLIC,prepared.executionContract());assertEquals(publicId,prepared.processDefinitionId());
        startThroughActualWorkflowAndOfficialBpm(22L,publicId);assertEquals(before,original(20L));verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @ParameterizedTest @ValueSource(strings={"ABSENT","PARTIAL","WRONG_DEFINITION","WRONG_BUSINESS","WRONG_VARIABLE"})
    void publicSourceNeedsItsOwnPreparedAndBoundIdentity(String conflict)throws Exception{
        rejectedPublicSource();copy(21L,20L);
        switch(conflict){
            case "ABSENT"->jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=NULL,prepared_process_definition_id=NULL,prepared_definition_digest=NULL WHERE id=20");
            case "PARTIAL"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=NULL WHERE id=20");
            case "WRONG_DEFINITION"->jdbc.update("UPDATE dcc_controlled_file SET prepared_process_definition_id=? WHERE id=20",exactId);
            case "WRONG_BUSINESS","WRONG_VARIABLE"->{var history=mock(org.flowable.engine.history.HistoricProcessInstance.class);
                when(history.getId()).thenReturn(files.selectById(20L).getProcessInstanceId());when(history.getTenantId()).thenReturn("1");
                when(history.getBusinessKey()).thenReturn(conflict.equals("WRONG_BUSINESS")?"21":"20");when(history.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.UPLOAD);
                when(history.getProcessDefinitionId()).thenReturn(files.selectById(20L).getPreparedProcessDefinitionId());
                when(history.getProcessVariables()).thenReturn(Map.of("controlledFileId",conflict.equals("WRONG_VARIABLE")?21L:20L));
                when(processes.getHistoricProcessInstance(anyString())).thenReturn(history);}
        }
        assertThrows(RuntimeException.class,()->inherit(21L));assertNull(files.selectById(21L).getPreparedWorkflowContract());
    }
    DccApplicationExecutionContractService.Prepared verifyExisting(Long target,Long source,Integer round){
        return tx().execute(s->ReflectionTestUtils.invokeMethod(contracts,"verifyInheritedPrepared",99L,target,source,round));
    }
    @Test void submittedCandidateReplayProvesItsExactSourceWithoutAnyPreparationOrHistoryWrite(){
        prepareRoot();copy(21L,20L);var prepared=inherit(21L);
        String bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(exactId).businessKey("21")
                .variables(Map.of("controlledFileId",21L,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("REPLAY")))).start().getId();
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"UPLOAD",21L,rounds.requireDraft("UPLOAD",21L),bpm,actual));
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_REVIEW',process_instance_id=?,process_definition_key=? WHERE id=21",bpm,DccControlledFileProcessDefinitionKeys.UPLOAD);
        var sourceBefore=original(20L);var targetBefore=original(21L);var attrsBefore=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");
        deploy(legacyXml.replace("name=\"培训等待\"","name=\"更新图\""));
        assertEquals(prepared,verifyExisting(21L,20L,1));assertEquals(targetBefore,original(21L));assertEquals(sourceBefore,original(20L));assertEquals(attrsBefore,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
        assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void uninitializedCandidateReplayVerifiesSameSourceAndDoesNotCreateAttributes(){
        prepareRoot();add(21L,20L);jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_controlled_file_id=20,selected_iteration_version_no='A/1' WHERE id=21");
        var prepared=tx().execute(s->contracts.inheritPrepared(99L,21L,20L,1));var before=original(21L);
        assertEquals(prepared,verifyExisting(21L,20L,1));assertEquals(before,original(21L));assertNull(attributeRows.find("UPLOAD",21L,1));
    }
    @ParameterizedTest @ValueSource(strings={"SOURCE","ROUND","PARTIAL","DEFINITION","PREDECESSOR","SELECTED_VERSION","HASH","PERMISSION"})
    void replaySourceConflictRejectsReadOnlyWithoutRepairingExistingFacts(String conflict){
        prepareRoot();add(21L,20L);jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_controlled_file_id=20,selected_iteration_version_no='A/1' WHERE id=21");
        tx().execute(s->contracts.inheritPrepared(99L,21L,20L,1));
        switch(conflict){
            case "SOURCE"->jdbc.update("UPDATE dcc_controlled_file SET category_id=3 WHERE id=20");
            case "PARTIAL"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=NULL WHERE id=21");
            case "DEFINITION"->jdbc.update("UPDATE dcc_controlled_file SET prepared_process_definition_id='missing' WHERE id=21");
            case "PREDECESSOR"->jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=22 WHERE id=21");
            case "SELECTED_VERSION"->jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_version_no='A/2' WHERE id=21");
            case "HASH"->jdbc.update("UPDATE dcc_controlled_file SET source_sha256=? WHERE id=21","b".repeat(64));
            case "PERMISSION"->when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(false);
        }
        var before=original(21L);assertThrows(RuntimeException.class,()->verifyExisting(21L,20L,conflict.equals("ROUND")?99:1));assertEquals(before,original(21L));
    }
    @Test void missingCrossKeyRegistrationRejectsReplayWithoutRewritingExistingRevision()throws Exception{
        String revision=controlledRootAndRevisionTarget();var mapping=new DccExecutionDefinitionTransitions.Transition(exactId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exactId)),revision,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revision)));
        transitions.setDefinitionTransitions(List.of(mapping));tx().execute(s->contracts.inheritPrepared(99L,21L,20L,1));var before=original(21L);
        assertEquals(revision,verifyExisting(21L,20L,1).processDefinitionId());transitions.setDefinitionTransitions(List.of());
        assertThrows(RuntimeException.class,()->verifyExisting(21L,20L,1));assertEquals(before,original(21L));
    }
    @Test void completedCrossKeyCreationReplaysItsStoredEvidenceAfterMasterMovesToTheTarget()throws Exception{
        String revision=controlledRootAndRevisionTarget();
        transitions.setDefinitionTransitions(List.of(new DccExecutionDefinitionTransitions.Transition(exactId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exactId)),revision,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revision)))));
        var prepared=tx().execute(s->contracts.inheritPrepared(99L,21L,20L,1));
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP WHERE id=21");
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',superseded_by_file_id=21 WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=21,current_active_controlled_file_id=21 WHERE id=10");
        var sourceBefore=original(20L);var targetBefore=original(21L);
        assertEquals(prepared,verifyExisting(21L,20L,1));assertEquals(sourceBefore,original(20L));assertEquals(targetBefore,original(21L));
        add(22L,20L);jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION',revision_base_active_controlled_file_id=20 WHERE id=22");
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.inheritPrepared(99L,22L,20L,1)),"first creation must still require the current controlled source");
        assertNull(files.selectById(22L).getPreparedWorkflowContract());
    }
    String canonicalMatrix(int version)throws Exception{
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        var raw=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(Files.readString(root.resolve("doc/tasks/20261011-dcc-w02-types-matrix/immutable-public-matrix-v2.fixture.json")),Map.class);
        raw.put("categoryId","2");raw.put("fileTypeTaxonomyId","6");raw.put("routeId",String.valueOf(30+version));raw.put("routeVersionNo",version);
        var template=(Map<String,Object>)((List<?>)raw.get("nodes")).get(0);var nodes=new ArrayList<Map<String,Object>>();
        for(int stage=1;stage<=3;stage++){var row=new TreeMap<>(template);row.put("id",String.valueOf(stage));row.put("stageNo",stage==3?4:stage);row.put("stageOrder",stage);row.put("sort",stage);
            row.put("stageCode",null);row.put("stageName","原阶段"+stage);row.put("stageType",stage==1?"SIGNOFF":stage==2?"APPROVAL":"DOC_CONTROL");
            row.put("candidateSourceType",stage==1?"DEPT":stage==2?"USER":"POSITION");row.put("candidateSourceId",null);row.put("candidateSourceIds",stage==1?"51":stage==2?"99":"10");
            row.put("subjectType",stage==1?"DEPT":stage==2?"USER":"DCC_POSITION");row.put("subjectId",null);row.put("marker",stage==3?"▲":"●");
            row.put("approveMethod",stage==1?"ALL":"ANY");row.put("approveRatio",stage==1?100:null);row.put("requireAllApprovals",stage==1);row.put("required",true);nodes.add(row);}
        raw.put("nodes",nodes);return DccApprovalMatrixCanonicalizer.canonicalize(raw);
    }
    void snapshotExecutedMatrix(String canonical){
        var baseline=DccApprovalMatrixSourceService.readCanonical(canonical);
        var raw=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(canonical,Map.class);
        var nodes=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(raw.get("nodes")),cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.class);
        var projection=cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.project(nodes,"NEW",List.of(51L));
        var resolved=projection.nodes().stream().map(node->new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(node.getStageNo(),node.getStageCode(),node.getStageName(),node.getStageOrder(),node.getCandidateSourceType(),
                Long.valueOf(node.getCandidateSourceIds()),List.of(Long.valueOf(node.getCandidateSourceIds())),node.getApproveMethod(),node.getApproveRatio(),node.getRequireAllApprovals(),List.of(99L))).toList();
        var route=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).versionNo(baseline.getRouteVersionNo()).build(),
                resolved,baseline,cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.VERSION,projection.groups());
        var workflow=new DccControlledFileWorkflowServiceImpl();wire(workflow,"routeSnapshotMapper",routeSnapshots);
        tx().executeWithoutResult(s->ReflectionTestUtils.invokeMethod(workflow,"persistApprovalRouteSnapshots",20L,route));
    }
    Object facts(Long target,Long source,Integer round){return tx().execute(s->ReflectionTestUtils.invokeMethod(contracts,"readInheritedDraftFacts",99L,target,source,round));}
    Object field(Object record,String name){return ReflectionTestUtils.getField(record,name);}
    void saveFactsToNewTarget(Long id,Object facts){
        // Explicit consumer adapter only; W08 owns the actual Query/Revision writes.
        jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=?,effective_mode=?,effective_date=?,selected_signoff_department_ids_json=?,distribution_department_ids_json=?,need_training=? WHERE id=?",
                field(facts,"approvalMatrixCanonical"),field(facts,"effectiveMode"),field(facts,"effectiveDate"),field(facts,"selectedSignoffDepartmentIdsJson"),field(facts,"distributionDepartmentIdsJson"),field(facts,"needTraining"),id);
    }
    @Test void unsentWorkingCopiesKeepOriginalBaselineAndIndependentSavedSelections()throws Exception{
        String original=canonicalMatrix(1);prepareRoot();jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=?,effective_mode='AFTER_STANDARDIZATION_SIGNATURE',effective_date=NULL,selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[\"52\",\"53\"]',need_training=1 WHERE id=20",original);
        var before=original(20L);copy(21L,20L);inherit(21L);saveFactsToNewTarget(21L,facts(21L,20L,1));copy(22L,21L);inherit(22L);saveFactsToNewTarget(22L,facts(22L,21L,1));
        var target=files.selectById(22L);assertEquals(original,target.getDraftApprovalMatrixJson());assertEquals("AFTER_STANDARDIZATION_SIGNATURE",target.getEffectiveMode());assertNull(target.getEffectiveDate());
        assertEquals("[\"51\"]",target.getSelectedSignoffDepartmentIdsJson());assertEquals("[\"52\",\"53\"]",target.getDistributionDepartmentIdsJson());assertTrue(target.getNeedTraining());assertEquals(before,original(20L));
    }
    @Test void rejectedPublicRoundInheritsActuallyExecutedAfterMatrixThroughTwoCopiesRatherThanOriginalBeforeOrCurrent()throws Exception{
        String before=canonicalMatrix(1),executed=canonicalMatrix(2),current=canonicalMatrix(3);rejectedPublicSource();
        jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=?,effective_mode='PRESET_DATE',effective_date='2099-12-20',selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[]',need_training=0 WHERE id=20",before);
        snapshotExecutedMatrix(executed);var originalBefore=original(20L);copy(21L,20L);inherit(21L);Object first=facts(21L,20L,1);
        assertEquals(executed,field(first,"approvalMatrixCanonical"));assertNotEquals(before,field(first,"approvalMatrixCanonical"));assertNotEquals(current,field(first,"approvalMatrixCanonical"));
        assertNotNull(field(first,"runtimeApprovalNodesJson"));saveFactsToNewTarget(21L,first);copy(22L,21L);inherit(22L);saveFactsToNewTarget(22L,facts(22L,21L,1));
        assertEquals(executed,files.selectById(22L).getDraftApprovalMatrixJson());assertEquals(originalBefore,original(20L));
        assertEquals(executed,routeSnapshots.selectListByControlledFileId(20L).get(0).getSourceApprovalMatrixJson());
    }
    @Test void legacyAbsentRawAndCrossActionAreExplicitFactsRatherThanInventedPublicBefore()throws Exception{
        prepareRoot();copy(21L,20L);inherit(21L);Object legacy=facts(21L,20L,1);assertNull(field(legacy,"approvalMatrixCanonical"));
        assertEquals(true,field(legacy,"sameApplicationType"));
    }
}
