package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.approval.DccApprovalTaskAdapter;
import cn.iocoder.yudao.module.dcc.approval.DccProjectProductTaskDelegate;
import cn.iocoder.yudao.module.dcc.approval.DccOfflineTrainingTaskDelegate;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateService;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import jakarta.annotation.Resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** The current File and real receive execution exist; no Flowable user task belongs to doc control yet. */
@Import({NotifyMessageSendApiImpl.class, NotifySendServiceImpl.class, NotifyMessageServiceImpl.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/g49_project_application_notify_tables.sql", executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/clean.sql", executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccOfflineTrainingEntryDatabaseTest extends DccWorkflowAttributesIntegrationTest {
    @MockitoBean NotifyTemplateService templates;
    @MockitoSpyBean NotifyMessageSendApiImpl messageApi;
    private ProcessEngine engine;
    private BpmTaskService tasks;
    private BpmProcessInstanceService processes;
    private String processId;
    private DccOfflineTrainingRecordService training;
    private PermissionApi permissions;
    private RoleApi roles;
    private AdminUserApi users;
    private DccControlledFileAssignmentScopeService scope;
    private DccControlledFileCategoryPermissionSupport category;
    private BpmProcessDefinitionService definitions;

    @BeforeEach
    void receivingTrainingFixture() {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(dataSource);
        config.setTransactionManager(manager);
        config.setDatabaseType("mysql");
        config.setDatabaseSchemaUpdate("true");
        config.setAsyncExecutorActivate(false);
        config.setDisableIdmEngine(true);
        engine = config.buildProcessEngine();
        engine.getRepositoryService().createDeployment().tenantId("1").addString("offline-training.bpmn20.xml", """
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:flowable="http://flowable.org/bpmn" targetNamespace="g57-offline-training">
                  <process id="dcc-controlled-file-upload" name="Actual offline training" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="startApproval" sourceRef="start" targetRef="MATRIX_APPROVAL"/>
                    <userTask id="MATRIX_APPROVAL" name="批准" flowable:assignee="99"/>
                    <sequenceFlow id="approvalTraining" sourceRef="MATRIX_APPROVAL" targetRef="TRAINING"/>
                    <receiveTask id="TRAINING" name="培训等待"/>
                    <sequenceFlow id="trainingReview" sourceRef="TRAINING" targetRef="DOC_CONTROL_REVIEW"/>
                    <userTask id="DOC_CONTROL_REVIEW" name="文控审核" flowable:assignee="88"/>
                    <sequenceFlow id="reviewEnd" sourceRef="DOC_CONTROL_REVIEW" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """).deploy();
        processId = engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(
                "dcc-controlled-file-upload", "20", Map.of("controlledFileId", "20"), "1").getId();
        engine.getTaskService().complete(engine.getTaskService().createTaskQuery()
                .processInstanceId(processId).taskDefinitionKey("MATRIX_APPROVAL").singleResult().getId());
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_TRAINING_RECORD',need_training=1,"
                + "approved_time=CURRENT_TIMESTAMP,process_instance_id=?,process_definition_key='dcc-controlled-file-upload' WHERE id=20", processId);
        tasks = mock(BpmTaskService.class);
        when(tasks.getTaskTodoPage(eq(88L), any())).thenAnswer(call -> {
            var request = call.getArgument(1, cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO.class);
            var actual = engine.getTaskService().createTaskQuery().taskAssignee("88")
                    .processDefinitionKey(request.getProcessDefinitionKey()).list();
            return new PageResult<>(actual, (long) actual.size());
        });
        processes = mock(BpmProcessInstanceService.class);
        when(processes.getProcessInstance(anyString())).thenAnswer(call -> engine.getRuntimeService()
                .createProcessInstanceQuery().processInstanceId(call.getArgument(0)).includeProcessVariables().singleResult());
        training = new DccOfflineTrainingRecordService();
        permissions = mock(PermissionApi.class);
        roles = mock(RoleApi.class);
        users = mock(AdminUserApi.class);
        scope = mock(DccControlledFileAssignmentScopeService.class);
        category = mock(DccControlledFileCategoryPermissionSupport.class);
        definitions = mock(BpmProcessDefinitionService.class);
        when(definitions.getProcessDefinition(anyString())).thenAnswer(call -> engine.getRepositoryService()
                .createProcessDefinitionQuery().processDefinitionId(call.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call ->
                engine.getRepositoryService().getBpmnModel(call.getArgument(0)));
        var role = new RoleRespDTO(); role.setId(8L); role.setCode("doc_control"); role.setStatus(0);
        when(roles.getRoleByCode("doc_control")).thenReturn(role);
        when(users.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setTenantId(1L)
                .setUsername("doc-control-88").setNickname("独立文控").setStatus(0));
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L)
                .setUsername("applicant-99").setNickname("原申请人").setStatus(0));
        when(permissions.getUserRoleIdListByUserId(88L)).thenReturn(Set.of(8L));
        when(permissions.getUserRoleIdListByRoleIds(List.of(8L))).thenReturn(Set.of(88L));
        when(permissions.hasAnyPermissions(88L,"dcc:controlled-file:approve")).thenReturn(true);
        when(permissions.hasAnyPermissions(88L,"dcc:controlled-file:query")).thenReturn(true);
        when(category.hasCategoryPermission(eq(2L),eq(88L),any())).thenReturn(true);
        when(scope.isWithinAssignedFileScope(88L,20L)).thenReturn(true);
        wire(training,"files",files,"masters",masters,"projects",projects,
                "projectAssignments",mock(DccProjectCodeAssignmentMapper.class),"fileScope",scope,"categories",category,
                "versions",DccControlledFileVersionPolicy.defaultPolicy(),"users",users,"roles",roles,"permissions",permissions,
                "processes",processes,"definitions",definitions,"runtime",engine.getRuntimeService(),"messages",messageApi);
        wire(workflow,"offlineTraining",training,"bpmProcessInstanceService",processes);
        var template = new NotifyTemplateDO(); template.setId(6806L); template.setCode("dcc_task_assigned");
        template.setType(2); template.setNickname("DCC"); template.setStatus(0);
        template.setContent("{processInstanceName} - {taskName}，{startUserNickname}");
        template.setParams(List.of("processInstanceName","taskName","startUserNickname","detailUrl"));
        when(templates.getNotifyTemplateByCodeFromCache("dcc_task_assigned")).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(call ->
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(call.getArgument(1)));
    }

    @AfterEach
    void closeOwnEngine() {
        if (engine != null) {
            engine.getRepositoryService().createDeploymentQuery().list().forEach(deployment ->
                    engine.getRepositoryService().deleteDeployment(deployment.getId(), true));
            engine.close();
        }
    }

    @Test
    void independentDocControlReceivesNativeTodoWhileActualTrainingHasNoUserTask() {
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(processId).count());
        assertEquals(1, engine.getRuntimeService().createExecutionQuery()
                .processInstanceId(processId).activityId("TRAINING").count());
        var adapter = new DccApprovalTaskAdapter(tasks, processes, workflow, files,
                mock(DccFileCategoryMapper.class), mock(DccControlledFileRouteSnapshotMapper.class),
                mock(DccProjectProductTaskDelegate.class), new DccOfflineTrainingTaskDelegate(training));
        var page = adapter.page(ApprovalTaskQueryContext.of(88L, ApprovalTaskViewType.TODO,
                ApprovalModuleCode.DCC, null, 1, 20, false));
        assertEquals(1L, page.getTotal(), "A legitimate independent doc control account must discover offline training upload");
        assertEquals("DCC_OFFLINE_TRAINING_RECORD", page.getList().get(0).getSourceTaskType());
        var row = page.getList().get(0);
        assertEquals("20",row.getBusinessKey());assertEquals(processId,row.getProcessInstanceId());
        assertEquals(Set.of("PROCESS_IN_MODULE"),row.getAvailableActions());assertEquals(false,row.getRequiresSignature());
        assertFalse(row.getDetailQuery().containsKey("taskId"));assertFalse(row.getDetailQuery().containsKey("handling"));
        assertEquals("1",row.getDetailQuery().get("management"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"DISABLED","FOREIGN_TENANT","NO_ROLE","CATEGORY","FILE_SCOPE","PROJECT_SCOPE","NO_APPROVE","NO_QUERY"})
    void everyEntryUsesTheSameEnabledRoleCategoryAndHardScopes(String missing) {
        switch (missing) {
            case "DISABLED" -> when(users.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setTenantId(1L).setStatus(1));
            case "FOREIGN_TENANT" -> when(users.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setTenantId(2L).setStatus(0));
            case "NO_ROLE" -> when(permissions.getUserRoleIdListByUserId(88L)).thenReturn(Set.of(1L));
            case "CATEGORY" -> when(category.hasCategoryPermission(eq(2L),eq(88L),any())).thenReturn(false);
            case "FILE_SCOPE" -> when(scope.isWithinAssignedFileScope(88L,20L)).thenReturn(false);
            case "PROJECT_SCOPE" -> {
                when(permissions.hasAnyPermissions(88L,"dcc:project-code-assignment:execute")).thenReturn(true);
                var assignments=mock(DccProjectCodeAssignmentMapper.class);
                when(assignments.selectActiveProjectCodeIdsByAssigneeUserId(eq(1L),eq(88L),any())).thenReturn(List.of(777L));
                wire(training,"projectAssignments",assignments);
            }
            case "NO_APPROVE" -> when(permissions.hasAnyPermissions(88L,"dcc:controlled-file:approve")).thenReturn(false);
            case "NO_QUERY" -> when(permissions.hasAnyPermissions(88L,"dcc:controlled-file:query")).thenReturn(false);
            default -> fail("Unknown explicit scenario");
        }
        assertTrue(training.listForActor(88L).isEmpty());
        assertFalse(training.canUpload(88L,files.selectById(20L)));
        assertEquals("DCC_OFFLINE_TRAINING_ACTOR_FORBIDDEN",assertThrows(IllegalStateException.class,
                ()->training.requireUpload(88L,files.selectById(20L))).getMessage());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));
    }

    @Test
    void metadataEntryDoesNotGrantTheOriginalBinaryAndQueriesDoNotCreateWork() {
        var file=files.selectById(20L);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        wire(guard,"assignmentScopeService",scope,"offlineTraining",training,"bpmTaskService",tasks);
        assertTrue(guard.isAllowed(88L,file,false,null));
        var query=new DccControlledFileQueryServiceImpl();
        var directories=mock(cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService.class);
        when(directories.getAuthorizedDirectoryIds(eq(88L),any())).thenReturn(Set.of());
        wire(query,"assignmentScopeService",scope,"directoryAccessPermissionService",directories,
                "permissionSupport",category,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                "controlledFileMapper",files);
        Boolean binary=ReflectionTestUtils.invokeMethod(query,"canReadBinary",88L,file,
                cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.PREVIEW,false);
        assertEquals(false,binary);
        int before=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class);
        assertEquals(1,training.listForActor(88L).size());
        assertEquals(1,training.listForActor(88L).size());
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));
        assertEquals(0,engine.getTaskService().createTaskQuery().processInstanceId(processId).count());
    }

    @Test
    void staleWaitAndWrongProcessBusinessIdentityAreErrorsRatherThanEmptySuccess() {
        engine.getRuntimeService().trigger(training.requireUpload(88L,files.selectById(20L)).executionId());
        assertEquals("DCC_OFFLINE_TRAINING_EXECUTION_REQUIRED",assertThrows(IllegalStateException.class,
                ()->training.listForActor(88L)).getMessage());
        var wrong=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId("dcc-controlled-file-upload",
                "404",Map.of("controlledFileId","20"),"1");
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=? WHERE id=20",wrong.getId());
        assertEquals("DCC_OFFLINE_TRAINING_PROCESS_INVALID",assertThrows(IllegalStateException.class,
                ()->training.listForActor(88L)).getMessage());
    }

    @Test
    void nativeKeywordAndAdvancementUseOnlyTheCurrentPersistedTrainingState() {
        var delegate=new DccOfflineTrainingTaskDelegate(training);
        assertEquals(1,delegate.list(ApprovalTaskQueryContext.of(88L,ApprovalTaskViewType.TODO,
                ApprovalModuleCode.DCC,"N-1",1,20)).size());
        assertTrue(delegate.list(ApprovalTaskQueryContext.of(88L,ApprovalTaskViewType.TODO,
                ApprovalModuleCode.DCC,"different file",1,20)).isEmpty());
        jdbc.update("UPDATE dcc_controlled_file SET training_record_file_id=101,status='PENDING_DOC_CONTROL_REVIEW' WHERE id=20");
        engine.getRuntimeService().trigger(engine.getRuntimeService().createExecutionQuery()
                .processInstanceId(processId).activityId("TRAINING").singleResult().getId());
        assertTrue(training.listForActor(88L).isEmpty());
        assertFalse(training.canUpload(88L,files.selectById(20L)));
        assertEquals(1,engine.getTaskService().createTaskQuery().processInstanceId(processId)
                .taskDefinitionKey("DOC_CONTROL_REVIEW").count());
    }

    @Test
    void officialStationMessageReplayIsExactAndDoesNotAlterSignedPeople() {
        new TransactionTemplate(manager).executeWithoutResult(s->training.notifyWaiting(files.selectById(20L)));
        var message=jdbc.queryForMap("SELECT user_id,template_params,business_key FROM system_notify_message WHERE tenant_id=1");
        assertEquals(88L,((Number)message.get("user_id")).longValue());
        assertTrue(message.get("template_params").toString().contains("DCC_OFFLINE_TRAINING_RECORD"));
        assertTrue(message.get("template_params").toString().contains("management=1"));
        new TransactionTemplate(manager).executeWithoutResult(s->training.notifyWaiting(files.selectById(20L)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));
        assertEquals(99L,files.selectById(20L).getRequesterId());
    }

    private static void wire(Object target,Object... fields) {
        for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(target,(String)fields[i],fields[i+1]);
    }

    @Test
    void revisionReceiveWaitUsesItsOwnDefinitionAndLegacyNullKeyIsNormallyOutsideThisLane() {
        var legacy=files.selectById(20L);legacy.setProcessDefinitionKey(null);
        assertFalse(DccOfflineTrainingRecordService.isNativeWaitingFile(legacy));
        assertFalse(training.canUpload(88L,legacy));
        String revision="""
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="g57-revision">
                <process id="dcc-controlled-file-revision" isExecutable="true"><startEvent id="start"/>
                <sequenceFlow id="begin" sourceRef="start" targetRef="TRAINING"/>
                <receiveTask id="TRAINING"/><sequenceFlow id="done" sourceRef="TRAINING" targetRef="end"/>
                <endEvent id="end"/></process></definitions>
                """;
        engine.getRepositoryService().createDeployment().tenantId("1").addString("revision-training.bpmn20.xml",revision).deploy();
        engine.getRuntimeService().deleteProcessInstance(processId,"isolated revision namespace fixture replacement");
        jdbc.update("UPDATE dcc_controlled_file SET version_no='A/2',change_type='REVISION' WHERE id=20");
        var baseline=files.selectById(20L);baseline.setId(21L);baseline.setVersionNo("A/1");baseline.setChangeType("NEW");
        baseline.setStatus("ACTIVE");baseline.setNeedTraining(false);baseline.setProcessInstanceId(null);baseline.setProcessDefinitionKey(null);
        baseline.setControlledTime(java.time.LocalDateTime.now().minusDays(1));baseline.setActivatedTime(baseline.getControlledTime());files.insert(baseline);
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=21,current_active_controlled_file_id=21 WHERE id=10");
        String round=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId("dcc-controlled-file-revision",
                "20",Map.of("controlledFileId","20"),"1").getId();
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,process_definition_key='dcc-controlled-file-revision' WHERE id=20",round);
        assertEquals(round,training.requireUpload(88L,files.selectById(20L)).processInstanceId());
        assertEquals(1,training.listForActor(88L).size());
    }
}
