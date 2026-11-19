package cn.iocoder.yudao.module.bpm.controller.admin.task;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalFlowableTaskQueryService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
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
    private final HistoricProcessInstance process = mock(HistoricProcessInstance.class);

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(controller, "approvalTaskQueryService", authority);
        ReflectionTestUtils.setField(controller, "permissionApi", permissions);
        ReflectionTestUtils.setField(controller, "taskService", tasks);
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
}
