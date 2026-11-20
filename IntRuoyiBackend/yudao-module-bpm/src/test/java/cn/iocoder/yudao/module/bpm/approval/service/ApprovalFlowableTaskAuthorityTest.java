package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper;
import org.flowable.engine.*;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalFlowableTaskAuthorityTest {
    @Test
    void requireNativeTaskAuthorityRejectsMissingQualificationWithBusinessReason() {
        var mapper = mock(ApprovalFlowableTaskQueryMapper.class);
        var tasks = mock(TaskService.class);
        var query = mock(TaskQueryImpl.class, RETURNS_SELF);
        var configuration = mock(ProcessEngineConfiguration.class);
        var permissions = mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);
        when(configuration.getDatabaseType()).thenReturn("mysql");
        when(tasks.createTaskQuery()).thenReturn(query);
        when(mapper.ownerships(anyMap())).thenReturn(List.of());
        var service = new ApprovalFlowableTaskQueryService(mapper, tasks, mock(HistoryService.class),
                configuration, permissions, mock(ManagementService.class),
                mock(cn.iocoder.yudao.module.bpm.approval.service.ownership.ApprovalProcessOwnershipService.class));
        TenantContextHolder.setTenantId(1L);
        try {
            ServiceException error = assertThrows(ServiceException.class,
                    () -> service.requireNativeTaskAuthority("registration-task", 100L));
            assertEquals(403, error.getCode());
            assertTrue(error.getMessage().contains("不具备该待办的审批资格"));
            verify(mapper).count(argThat(parameters -> Boolean.FALSE.equals(parameters.get("global"))
                    && "registration-task".equals(parameters.get("taskId"))
                    && Long.valueOf(100).equals(parameters.get("userId"))));
        } finally {
            TenantContextHolder.clear();
        }
    }
}
