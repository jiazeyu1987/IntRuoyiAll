package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Existing authentic isolated DCC HMAC/unified signature/public approval host plus official station messages. */
@Import({NotifyMessageSendApiImpl.class, NotifySendServiceImpl.class, NotifyMessageServiceImpl.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/g49_project_application_notify_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccOfflineTrainingApprovalTransactionTest extends DccFileOwnerPublicApprovalTest {
    @Resource DccProjectCodeMapper projects;
    @MockitoBean NotifyTemplateService templates;
    @MockitoSpyBean NotifyMessageSendApiImpl messageApi;
    private DccOfflineTrainingRecordService training;

    @BeforeEach
    void receivingNodeAndOfficialMessagePorts() {
        engine.getRuntimeService().deleteProcessInstance(bpm,"isolated training fixture replacement");
        String xml=modelXml.replace("targetRef=\"DOC_CONTROL_REVIEW\"/>",
                "targetRef=\"TRAINING\"/><receiveTask id=\"TRAINING\" name=\"培训等待\"/>"
                        +"<sequenceFlow id=\"trainingToDoc\" sourceRef=\"TRAINING\" targetRef=\"DOC_CONTROL_REVIEW\"/>");
        engine.getRepositoryService().createDeployment().tenantId("1").addString("training-owner.bpmn20.xml",xml).deploy();
        bpm=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(DccControlledFileProcessDefinitionKeys.UPLOAD,
                Long.toString(FILE),Map.of("actor","99","controlledFileId",Long.toString(FILE),"needTraining",true),"1").getId();
        task=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult().getId();
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,need_training=1 WHERE id=?",bpm,FILE);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,status) VALUES(5,1,'正式训练项目','ENABLE')");
        when(accounts.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setTenantId(1L)
                .setStatus(0).setUsername("doc-control-88").setNickname("独立文控"));
        when(accounts.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L)
                .setStatus(0).setUsername("approver99").setNickname("批准人"));
        var roles=mock(RoleApi.class);var role=new RoleRespDTO();role.setId(8L);role.setCode("doc_control");role.setStatus(0);
        when(roles.getRoleByCode("doc_control")).thenReturn(role);
        var permissions=mock(PermissionApi.class);
        when(permissions.getUserRoleIdListByUserId(88L)).thenReturn(Set.of(8L));
        when(permissions.getUserRoleIdListByRoleIds(List.of(8L))).thenReturn(Set.of(88L));
        when(permissions.hasAnyPermissions(88L,"dcc:controlled-file:approve")).thenReturn(true);
        when(permissions.hasAnyPermissions(88L,"dcc:controlled-file:query")).thenReturn(true);
        var scope=mock(DccControlledFileAssignmentScopeService.class);
        when(scope.isWithinAssignedFileScope(88L,FILE)).thenReturn(true);
        var category=mock(DccControlledFileCategoryPermissionSupport.class);
        when(category.hasCategoryPermission(eq(2L),eq(88L),any())).thenReturn(true);
        var processes=mock(BpmProcessInstanceService.class);
        when(processes.getProcessInstance(anyString())).thenAnswer(call->engine.getRuntimeService()
                .createProcessInstanceQuery().processInstanceId(call.getArgument(0)).includeProcessVariables().singleResult());
        var definitions=mock(BpmProcessDefinitionService.class);
        when(definitions.getProcessDefinition(anyString())).thenAnswer(call->engine.getRepositoryService()
                .createProcessDefinitionQuery().processDefinitionId(call.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call->engine.getRepositoryService()
                .getBpmnModel(call.getArgument(0)));
        training=new DccOfflineTrainingRecordService();
        wire(training,"files",files,"masters",masters,"projects",projects,
                "projectAssignments",mock(DccProjectCodeAssignmentMapper.class),"fileScope",scope,"categories",category,
                "versions",DccControlledFileVersionPolicy.defaultPolicy(),"users",accounts,"roles",roles,"permissions",permissions,
                "processes",processes,"definitions",definitions,"runtime",engine.getRuntimeService(),"messages",messageApi);
        training=proxy(training);
        wire(workflow,"offlineTraining",training);
        var template=new NotifyTemplateDO();template.setId(6806L);template.setCode("dcc_task_assigned");
        template.setType(2);template.setNickname("DCC");template.setStatus(0);
        template.setContent("{processInstanceName} - {taskName}，{startUserNickname}");
        template.setParams(List.of("processInstanceName","taskName","startUserNickname","detailUrl"));
        when(templates.getNotifyTemplateByCodeFromCache("dcc_task_assigned")).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(call->
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(call.getArgument(1)));
    }

    @Test
    void signedApprovalCommitsWaitingExecutionAndOneMessageToIndependentDocumentControl() {
        login(99L);workflow.approveTask(99L,FILE,request(task,OWNER));
        var saved=files.selectById(FILE);
        assertEquals("PENDING_APPLICANT_TRAINING_RECORD",saved.getStatus());assertNotNull(saved.getApprovedTime());
        assertEquals(OWNER,saved.getFileOwnerUserId());assertEquals(1,signatures.selectListByControlledFileId(FILE).size());
        assertEquals(1,unifiedRows.selectList().size());
        assertEquals(0,engine.getTaskService().createTaskQuery().processInstanceId(bpm).count());
        assertEquals(1,engine.getRuntimeService().createExecutionQuery().processInstanceId(bpm).activityId("TRAINING").count());
        var message=jdbc.queryForMap("SELECT user_id,template_params FROM system_notify_message WHERE tenant_id=1");
        assertEquals(88L,((Number)message.get("user_id")).longValue());
        assertTrue(message.get("template_params").toString().contains(Long.toString(FILE)));
        assertTrue(message.get("template_params").toString().contains(bpm));
        assertEquals(1,training.listForActor(88L).size());assertTrue(training.listForActor(99L).isEmpty());
    }

    @Test
    void missingTemplateRollsBackBothAuthenticSignatureRowsOwnerAndRealApproval() {
        when(templates.getNotifyTemplateByCodeFromCache("dcc_task_assigned")).thenReturn(null);
        login(99L);assertThrows(RuntimeException.class,()->workflow.approveTask(99L,FILE,request(task,OWNER)));
        assertFullyRolledBackApproval();
    }

    @Test
    void failureAfterOfficialStationInsertRollsBackAuthenticSignaturesAndReceiveTransition() {
        doAnswer(call->{call.callRealMethod();throw new IllegalStateException("ISOLATED_OFFICIAL_MESSAGE_POST_INSERT_FAILURE");})
                .when(messageApi).sendSingleMessageIdempotentlyToAdmin(any());
        login(99L);assertThrows(IllegalStateException.class,()->workflow.approveTask(99L,FILE,request(task,OWNER)));
        assertFullyRolledBackApproval();
    }

    private void assertFullyRolledBackApproval() {
        var saved=files.selectById(FILE);
        assertEquals("PENDING_MATRIX_APPROVAL",saved.getStatus());assertNull(saved.getApprovedTime());
        assertNull(saved.getFileOwnerUserId());assertEquals(0,signatures.selectListByControlledFileId(FILE).size());
        assertEquals(0,unifiedRows.selectList().size());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));
        assertNotNull(engine.getTaskService().createTaskQuery().taskId(task).singleResult());
        assertEquals(0,engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(task).finished().count());
        assertEquals(0,engine.getRuntimeService().createExecutionQuery().processInstanceId(bpm).activityId("TRAINING").count());
    }
}
