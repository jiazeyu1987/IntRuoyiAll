package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApiImpl;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.flowable.engine.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** W05 -> actual registered BPM API/service -> real two-version Flowable; only directory/definition permissions are isolated. */
class DccWorkflowExactPreparedStartTest {
    ProcessEngine engine;
    BpmProcessDefinitionService definitions;
    BpmProcessDefinitionInfoDO info;
    DccControlledFileWorkflowServiceImpl workflow;
    DccControlledFileDO file;
    DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route;
    String frozenId,latestId;
    @BeforeEach void fixture()throws Exception {
        TenantContextHolder.setTenantId(1L);
        engine=ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:w05_exact_start_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate("true").setAsyncExecutorActivate(false).buildProcessEngine();
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        var matcher=java.util.regex.Pattern.compile("SET @dcc_upload_bpmn = '([\\s\\S]*?)';")
                .matcher(Files.readString(root.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql")));
        assertTrue(matcher.find());String xml=matcher.group(1);
        frozenId=deploy(xml);latestId=deploy(xml.replace("name=\"培训等待\"","name=\"较新不同定义\""));
        definitions=mock(BpmProcessDefinitionService.class);
        when(definitions.getProcessDefinition(anyString())).thenAnswer(call->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(call.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call->engine.getRepositoryService().getBpmnModel(call.getArgument(0)));
        when(definitions.getActiveProcessDefinition(anyString())).thenAnswer(call->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(call.getArgument(0)).processDefinitionTenantId("1").latestVersion().singleResult());
        info=new BpmProcessDefinitionInfoDO();when(definitions.getProcessDefinitionInfo(anyString())).thenReturn(info);
        when(definitions.canUserStartProcessDefinition(info,99L)).thenReturn(true);
        var service=spy(new BpmProcessInstanceServiceImpl());ReflectionTestUtils.setField(service,"runtimeService",engine.getRuntimeService());
        ReflectionTestUtils.setField(service,"processDefinitionService",definitions);ReflectionTestUtils.setField(service,"adminUserApi",mock(AdminUserApi.class));
        doReturn(new BpmApprovalDetailRespVO().setActivityNodes(new ArrayList<>())).when(service).getApprovalDetail(eq(99L),any());
        var api=new BpmProcessInstanceApiImpl();ReflectionTestUtils.setField(api,"processInstanceService",service);
        var contracts=new DccApplicationExecutionContractService();ReflectionTestUtils.setField(contracts,"definitions",definitions);
        file=DccControlledFileDO.builder().id(10L).tenantId(1L).fileNumber("EXACT-10").versionNo("A/1").categoryId(2L).directoryId(3L)
                .needTraining(false).preparedWorkflowContract(DccApplicationExecutionContractService.LEGACY).preparedProcessDefinitionId(frozenId)
                .preparedDefinitionDigest(DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(frozenId))).build();
        route=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(
                cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(20L).versionNo(1).build(),List.of(
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(1,"MATRIX_REVIEW","会签",1,"DEPT",51L,List.of(51L),"ALL",100,true,List.of(99L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(2,"MATRIX_APPROVAL","批准",2,"USER",100L,List.of(100L),"ANY",null,false,List.of(100L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(3,"DOC_CONTROL_REVIEW","文控",3,"USER",101L,List.of(101L),"ANY",null,false,List.of(101L))));
        workflow=new DccControlledFileWorkflowServiceImpl();ReflectionTestUtils.setField(workflow,"bpmProcessInstanceApi",api);
        ReflectionTestUtils.setField(workflow,"executionContracts",contracts);ReflectionTestUtils.setField(workflow,"approvalRouteAssigneeResolver",new DccControlledFileApprovalRouteAssigneeResolver());
    }
    String deploy(String xml){var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("actual.bpmn20.xml",xml).deploy();
        return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId();}
    String start(){return ReflectionTestUtils.invokeMethod(workflow,"createApprovalProcess",99L,file,route,DccControlledFileProcessDefinitionKeys.UPLOAD);}
    @AfterEach void close(){if(engine!=null)engine.close();TenantContextHolder.clear();}
    @Test void exactPreparedIdStartsThroughOfficialBpmWithOriginalBusinessKeyAndNoLatestLookup(){
        String id=start();var instance=engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(id).includeProcessVariables().singleResult();
        assertEquals(frozenId,instance.getProcessDefinitionId());assertNotEquals(latestId,instance.getProcessDefinitionId());
        assertEquals("10",instance.getBusinessKey());assertEquals("DCC受控文件上传审批 EXACT-10 / A/1",instance.getName());
        assertEquals("99",instance.getStartUserId());assertEquals("1",instance.getTenantId());
        assertEquals(10L,instance.getProcessVariables().get("controlledFileId"));
        verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void invalidDigestAndOfficialStartPermissionDenialCreateNoInstance(){
        file.setPreparedDefinitionDigest("a".repeat(64));assertThrows(IllegalStateException.class,this::start);
        assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
        file.setPreparedDefinitionDigest(DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(frozenId)));
        when(definitions.canUserStartProcessDefinition(info,99L)).thenReturn(false);assertThrows(RuntimeException.class,this::start);
        assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
}
