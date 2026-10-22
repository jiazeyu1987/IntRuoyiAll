package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmSupervisorHandoverReqVO;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmSupervisorHandoverMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.*;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BpmSupervisorHandoverServiceTest extends BaseMockitoUnitTest {
    @InjectMocks private BpmSupervisorHandoverService service;
    @Mock private TaskService taskService;
    @Mock private RuntimeService runtimeService;
    @Mock(answer = Answers.RETURNS_SELF) private org.flowable.task.api.TaskQuery taskQuery;
    @Mock(answer = Answers.RETURNS_SELF) private org.flowable.engine.runtime.ProcessInstanceQuery processQuery;
    @Mock private RepositoryService repositoryService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private ApprovalAuthorityApi approvalAuthority;
    @Mock private BpmSupervisorHandoverMapper handoverMapper;
    @Mock private BusinessApprovalRequestMapper requestMapper;
    @Mock private FormActionInstanceMapper formMapper;

    @BeforeEach void tenant() {
        TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    private BpmSupervisorHandoverReqVO request() {
        return new BpmSupervisorHandoverReqVO().setId("task-1").setPreviousAssigneeUserId(10L)
                .setAssigneeUserId(20L).setReason("原主管离职，交给接任主管");
    }

    private Task task() {
        when(adminUserApi.lockSignatureIdentity(20L)).thenReturn(
                new AdminUserRespDTO().setId(20L).setStatus(0).setNickname("接任主管"));
        when(handoverMapper.lockTask("task-1", "1")).thenReturn("task-1");
        Task task = mock(Task.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(task.getAssignee()).thenReturn("10");
        return task;
    }

    private void definition(Task task, String definitionKey) {
        when(task.getProcessDefinitionId()).thenReturn("definition-1");
        var definition = mock(ProcessDefinition.class);
        when(repositoryService.getProcessDefinition("definition-1")).thenReturn(definition);
        when(definition.getKey()).thenReturn(definitionKey);
    }

    @Test void disabledTargetCannotReceiveAnyTask() {
        when(adminUserApi.lockSignatureIdentity(20L)).thenReturn(new AdminUserRespDTO().setId(20L).setStatus(1));
        assertThrows(ServiceException.class, () -> service.handover(1L, request()));
        verifyNoInteractions(handoverMapper);
        verify(taskService, never()).setAssignee(anyString(), anyString());
    }

    @Test void staleSourceCannotOverwriteAnotherHandover() {
        var task = task();
        when(task.getAssignee()).thenReturn("11");
        assertThrows(ServiceException.class, () -> service.handover(1L, request()));
        verify(taskService, never()).setAssignee(anyString(), anyString());
    }

    @Test void nativeDccTaskCannotUseGenericHandover() {
        definition(task(), "dcc-controlled-file-approval");
        assertThrows(ServiceException.class, () -> service.handover(1L, request()));
        verify(taskService, never()).setAssignee(anyString(), anyString());
    }

    @Test void genericSupervisorHandoverRecordsReasonAndKeepsTaskPending() {
        Task task = task();
        definition(task, "generic-supervisor");
        when(task.getId()).thenReturn("task-1");
        when(task.getProcessInstanceId()).thenReturn("process-1");
        when(task.getTaskDefinitionKey()).thenReturn("supervisor");
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("generic-supervisor");
        UserTask node = new UserTask();
        node.setId("supervisor");
        BpmnModelUtils.addCandidateElements(BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy(), "1", node);
        process.addFlowElement(node);
        model.addProcess(process);
        when(repositoryService.getBpmnModel("definition-1")).thenReturn(model);
        var instance = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
        when(processQuery.singleResult()).thenReturn(instance);
        when(instance.getStartUserId()).thenReturn("30");
        when(adminUserApi.getUser(1L)).thenReturn(new AdminUserRespDTO().setId(1L).setNickname("管理员"));
        service.handover(1L, request());
        var order = inOrder(taskService);
        order.verify(taskService).setVariableLocal("task-1", BpmSupervisorHandoverService.HANDOVER_VARIABLE, true);
        order.verify(taskService).addComment(eq("task-1"), eq("process-1"), anyString(), contains("原主管离职"));
        order.verify(taskService).setOwner("task-1", "10");
        order.verify(taskService).setAssignee("task-1", "20");
        verify(taskService, never()).complete(anyString());
    }
}
