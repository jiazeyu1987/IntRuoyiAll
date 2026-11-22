package cn.iocoder.yudao.module.bpm.controller.admin.task;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalFlowableTaskQueryService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailReqVO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmProcessInstanceCandidateAccessTest {
    private final BpmProcessInstanceController controller = new BpmProcessInstanceController();
    private final ApprovalFlowableTaskQueryService authority = mock(ApprovalFlowableTaskQueryService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final BpmTaskService tasks = mock(BpmTaskService.class);
    private final BpmProcessInstanceService processes = mock(BpmProcessInstanceService.class);
    private final HistoricProcessInstance process = mock(HistoricProcessInstance.class);

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(controller, "approvalTaskQueryService", authority);
        ReflectionTestUtils.setField(controller, "permissionApi", permissions);
        ReflectionTestUtils.setField(controller, "taskService", tasks);
        ReflectionTestUtils.setField(controller, "processInstanceService", processes);
        when(process.getId()).thenReturn("process-1");
        when(process.getTenantId()).thenReturn("1");
        when(process.getStartUserId()).thenReturn("99");
        when(tasks.getRunningTaskListByProcessInstanceId("process-1", null, null)).thenReturn(List.of());
        when(tasks.getTaskListByProcessInstanceId("process-1", true)).thenReturn(List.of());
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }
    private void check() { ReflectionTestUtils.invokeMethod(controller, "assertCanReadProcessInstance", 2L, process); }

    @Test void currentAuthorizedCandidateCanRead() {
        when(authority.hasCurrentProcessTaskAuthority("process-1", 2L)).thenReturn(true);
        assertDoesNotThrow(this::check);
    }
    @Test void unrelatedOrRevokedCandidateCannotRead() {
        when(authority.hasCurrentProcessTaskAuthority("process-1", 2L)).thenReturn(false);
        assertThrows(RuntimeException.class, this::check);
    }
    @Test void historicalSelectionAloneCannotRead() {
        assertThrows(RuntimeException.class, this::check);
        verify(authority).hasCurrentProcessTaskAuthority("process-1", 2L);
    }
    @Test void foreignTenantCannotUseCandidateAuthority() {
        when(process.getTenantId()).thenReturn("2");
        when(authority.hasCurrentProcessTaskAuthority("process-1", 2L)).thenReturn(true);
        assertThrows(RuntimeException.class, this::check);
        verifyNoInteractions(authority, permissions);
    }

    private void readApproval() {
        ReflectionTestUtils.invokeMethod(controller, "assertCanReadApprovalDetail", 2L,
                new BpmApprovalDetailReqVO().setProcessInstanceId("process-1"));
    }
    private void ownRegistrationProcess() {
        when(processes.getHistoricProcessInstance("process-1")).thenReturn(process);
        when(process.getStartUserId()).thenReturn("2");
        when(process.getProcessDefinitionKey()).thenReturn("dcc-registration-certificate-access");
        when(permissions.hasAnyPermissions(2L, "bpm:task:query")).thenReturn(true);
    }
    @Test void approvalCenterApplicantCanReadOwnRegistrationWithoutGenericQuery() {
        ownRegistrationProcess();
        assertDoesNotThrow(this::readApproval);
        verifyNoInteractions(authority);
    }
    @Test void applicantCannotUseOwnRegistrationReadForForeignTenant() {
        ownRegistrationProcess();
        when(process.getTenantId()).thenReturn("2");
        assertThrows(RuntimeException.class, this::readApproval);
    }
    @Test void applicantCannotUseOwnRegistrationReadForAnotherStarter() {
        ownRegistrationProcess();
        when(process.getStartUserId()).thenReturn("99");
        when(authority.hasCurrentProcessTaskAuthority("process-1", 2L)).thenReturn(true);
        assertThrows(RuntimeException.class, this::readApproval);
        verifyNoInteractions(authority);
    }
    @Test void applicantCannotUseOwnRegistrationReadForOtherBusinessFlow() {
        ownRegistrationProcess();
        when(process.getProcessDefinitionKey()).thenReturn("another-business");
        assertThrows(RuntimeException.class, this::readApproval);
    }
    @Test void applicantWithoutApprovalCenterPermissionCannotRead() {
        ownRegistrationProcess();
        when(permissions.hasAnyPermissions(2L, "bpm:task:query")).thenReturn(false);
        assertThrows(RuntimeException.class, this::readApproval);
    }
    @Test void applicantCannotPredictUnstartedProcessWithoutGenericQuery() {
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(controller,
                "assertCanReadApprovalDetail", 2L,
                new BpmApprovalDetailReqVO().setProcessDefinitionId("registration-definition")));
        verifyNoInteractions(processes);
    }
    @Test void genericQueryStillRequiresExistingProcessAuthority() {
        ownRegistrationProcess();
        when(permissions.hasAnyPermissions(2L, "bpm:process-instance:query")).thenReturn(true);
        when(process.getStartUserId()).thenReturn("99");
        assertThrows(RuntimeException.class, this::readApproval);
        when(authority.hasCurrentProcessTaskAuthority("process-1", 2L)).thenReturn(true);
        assertDoesNotThrow(this::readApproval);
    }
}
