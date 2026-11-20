package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.ApprovalProcessOwnershipService;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.flowable.engine.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalTodoCountQueryTest {
    @Test
    void countPreservesOwnershipValidationAndDoesNotHydrateFlowableTasks() {
        var mapper = mock(ApprovalFlowableTaskQueryMapper.class);
        var tasks = mock(TaskService.class);
        var history = mock(HistoryService.class);
        var config = mock(ProcessEngineConfiguration.class);
        var permissions = mock(PermissionApi.class);
        var management = mock(ManagementService.class);
        var ownership = mock(ApprovalProcessOwnershipService.class);
        when(config.getDatabaseType()).thenReturn("mysql");
        when(mapper.ownerships(anyMap())).thenReturn(List.of());
        when(mapper.count(anyMap())).thenReturn(319L);
        var service = new ApprovalFlowableTaskQueryService(mapper, tasks, history, config, permissions, management, ownership);
        var context = ApprovalTaskQueryContext.of(100L, ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM,
                null, 1, 10, true).withWindow(new ApprovalQueryWindow(0, 0));
        TenantContextHolder.setTenantId(123L);
        try {
            var page = service.todo(context, "BPM");
            assertEquals(319L, page.getTotal());
            assertTrue(page.getList().isEmpty());
            verify(mapper).countBrokenProvenance(argThat(p -> p.get("tenant").equals(123L)
                    && p.get("userId").equals(100L) && p.get("global").equals(true)));
            verify(mapper).ownerships(anyMap());
            verify(mapper, never()).window(anyMap());
            verifyNoInteractions(tasks, history, ownership);
            when(mapper.countBrokenProvenance(anyMap())).thenReturn(1L);
            assertThrows(IllegalStateException.class, () -> service.todo(context, "BPM"));
            verify(mapper, times(1)).count(anyMap());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
