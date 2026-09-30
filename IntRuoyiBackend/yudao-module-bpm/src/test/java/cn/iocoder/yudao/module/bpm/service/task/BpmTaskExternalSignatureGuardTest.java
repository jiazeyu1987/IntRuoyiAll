package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPROVAL_REQUIRES_DCC_SIGNATURE;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPROVAL_REQUIRES_EDHR_SIGNATURE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmTaskExternalSignatureGuardTest extends BaseMockitoUnitTest {

    private static final List<String> DCC_THREE_WORKFLOW_KEYS = List.of(
            "dcc-controlled-file-upload",
            "dcc-controlled-file-revision",
            "dcc-controlled-file-obsolete"
    );

    @Test
    void assertGenericTaskMutationAllowed_blocksEvenFormCenterOwnedDccProcess() {
        TenantContextHolder.setTenantId(122L);
        Task task = mock(Task.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(task.getProcessDefinitionId()).thenReturn("definition-1");
        org.mockito.Mockito.lenient().when(task.getProcessInstanceId()).thenReturn("form-center-process-1");
        when(processDefinitionService.getProcessDefinition("definition-1")).thenReturn(definition);
        when(definition.getKey()).thenReturn("dcc-controlled-file-approval");
        org.mockito.Mockito.lenient().when(formActionInstanceMapper.selectByProcessInstanceId(122L, "form-center-process-1"))
                .thenReturn(new FormActionInstanceDO().setId(100L));

        assertServiceException(() -> guard.assertGenericTaskMutationAllowed(task),
                TASK_APPROVAL_REQUIRES_DCC_SIGNATURE);
    }

    @Mock
    private cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService processDefinitionService;
    @Mock
    private FormActionInstanceMapper formActionInstanceMapper;

    @InjectMocks
    private BpmTaskExternalSignatureGuard guard;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void assertGenericApproveOrRejectAllowed_blocksDccControlledFileProcess() {
        TenantContextHolder.setTenantId(122L);
        Task task = mock(Task.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(task.getProcessDefinitionId()).thenReturn("definition-1");
        when(task.getProcessInstanceId()).thenReturn("dcc-process-1");
        when(processDefinitionService.getProcessDefinition("definition-1")).thenReturn(definition);
        when(definition.getKey()).thenReturn("dcc-controlled-file-approval");

        assertServiceException(() -> guard.assertGenericApproveOrRejectAllowed(task),
                TASK_APPROVAL_REQUIRES_DCC_SIGNATURE);
    }

    @Test
    void assertGenericApproveOrRejectAllowed_blocksThreeDccWorkflowProcesses() {
        for (String key : DCC_THREE_WORKFLOW_KEYS) {
            TenantContextHolder.setTenantId(122L);
            Task task = mock(Task.class);
            ProcessDefinition definition = mock(ProcessDefinition.class);
            when(task.getProcessDefinitionId()).thenReturn("definition-" + key);
            when(task.getProcessInstanceId()).thenReturn("dcc-process-" + key);
            when(processDefinitionService.getProcessDefinition("definition-" + key)).thenReturn(definition);
            when(definition.getKey()).thenReturn(key);

            assertServiceException(() -> guard.assertGenericApproveOrRejectAllowed(task),
                    TASK_APPROVAL_REQUIRES_DCC_SIGNATURE);
            TenantContextHolder.clear();
        }
    }

    @Test
    void assertGenericTaskMutationAllowed_blocksDccControlledFileProcess() {
        TenantContextHolder.setTenantId(122L);
        Task task = mock(Task.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(task.getProcessDefinitionId()).thenReturn("definition-1");
        when(processDefinitionService.getProcessDefinition("definition-1")).thenReturn(definition);
        when(definition.getKey()).thenReturn("dcc-controlled-file-approval");

        assertServiceException(() -> guard.assertGenericTaskMutationAllowed(task),
                TASK_APPROVAL_REQUIRES_DCC_SIGNATURE);
    }

    @Test
    void assertGenericApproveOrRejectAllowed_allowsFormCenterOwnedDccProcess() {
        TenantContextHolder.setTenantId(122L);
        Task task = mock(Task.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(task.getProcessDefinitionId()).thenReturn("definition-1");
        when(task.getProcessInstanceId()).thenReturn("form-center-process-1");
        when(processDefinitionService.getProcessDefinition("definition-1")).thenReturn(definition);
        when(definition.getKey()).thenReturn("dcc-controlled-file-approval");
        when(formActionInstanceMapper.selectByProcessInstanceId(122L, "form-center-process-1"))
                .thenReturn(new FormActionInstanceDO().setId(100L));

        guard.assertGenericApproveOrRejectAllowed(task);
    }

    @Test
    void assertGenericApproveOrRejectAllowed_blocksEdhrBatchRecordProcess() {
        Task task = mock(Task.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(task.getProcessDefinitionId()).thenReturn("definition-edhr");
        when(processDefinitionService.getProcessDefinition("definition-edhr")).thenReturn(definition);
        when(definition.getKey()).thenReturn("mes-edhr-approval-v1");

        assertServiceException(() -> guard.assertGenericApproveOrRejectAllowed(task),
                TASK_APPROVAL_REQUIRES_EDHR_SIGNATURE);
    }

    @Test
    void assertGenericApproveOrRejectAllowed_allowsFormCenterOwnedEdhrActionProcess() {
        TenantContextHolder.setTenantId(122L);
        Task task = mock(Task.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(task.getProcessDefinitionId()).thenReturn("definition-edhr");
        when(task.getProcessInstanceId()).thenReturn("form-center-edhr-release-process-1");
        when(processDefinitionService.getProcessDefinition("definition-edhr")).thenReturn(definition);
        when(definition.getKey()).thenReturn("mes-edhr-approval-v1");
        when(formActionInstanceMapper.selectByProcessInstanceId(122L, "form-center-edhr-release-process-1"))
                .thenReturn(new FormActionInstanceDO().setId(101L));

        guard.assertGenericApproveOrRejectAllowed(task);
    }

    @Test
    void assertGenericApproveOrRejectAllowed_allowsFormCenterOwnedEdhrActionProcessByTaskTenant() {
        Task task = mock(Task.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(task.getProcessDefinitionId()).thenReturn("definition-edhr");
        when(task.getProcessInstanceId()).thenReturn("form-center-edhr-release-process-2");
        when(task.getTenantId()).thenReturn("122");
        when(processDefinitionService.getProcessDefinition("definition-edhr")).thenReturn(definition);
        when(definition.getKey()).thenReturn("mes-edhr-approval-v1");
        when(formActionInstanceMapper.selectByProcessInstanceId(122L, "form-center-edhr-release-process-2"))
                .thenReturn(new FormActionInstanceDO().setId(102L));

        guard.assertGenericApproveOrRejectAllowed(task);
    }
}
