package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.ApprovalProcessOwnershipService;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.flowable.engine.*;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApprovalProcessTaskAuthorityTest {
    private final ApprovalFlowableTaskQueryMapper mapper = mock(ApprovalFlowableTaskQueryMapper.class);
    private final TaskService tasks = mock(TaskService.class);
    private final ProcessEngineConfiguration configuration = mock(ProcessEngineConfiguration.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ManagementService management = mock(ManagementService.class);
    private final ApprovalFlowableTaskQueryService service = new ApprovalFlowableTaskQueryService(mapper,
            tasks, mock(HistoryService.class), configuration, permissions, management,
            mock(ApprovalProcessOwnershipService.class));
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        when(configuration.getDatabaseType()).thenReturn("mysql");
        TaskQueryImpl query = mock(TaskQueryImpl.class);
        when(tasks.createTaskQuery()).thenReturn(query);
        when(query.taskCandidateOrAssigned("42")).thenReturn(query);
        when(management.executeCommand(any(ApprovalFlowableTaskQueryService.CandidateGroupsRead.class)))
                .thenReturn(List.of("reviewers"));
        when(mapper.ownerships(anyMap())).thenReturn(List.of());
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void scopesCurrentAuthorityAndReusesProvenanceChecks() {
        when(mapper.count(anyMap())).thenReturn(1L);
        assertTrue(service.hasCurrentProcessTaskAuthority("exact-process", 42L));
        ArgumentCaptor<Map<String,Object>> capture = ArgumentCaptor.forClass(Map.class);
        var order = inOrder(mapper);
        order.verify(mapper).countBrokenProvenance(capture.capture());
        order.verify(mapper).ownerships(anyMap());
        order.verify(mapper).count(anyMap());
        Map<String,Object> p = capture.getValue();
        assertEquals(7L, p.get("tenant"));
        assertEquals("7", p.get("tenantText"));
        assertEquals("exact-process", p.get("processInstanceId"));
        assertEquals(42L, p.get("userId"));
        assertEquals(false, p.get("global"));
        assertEquals(false, p.get("done"));
        assertEquals(List.of("reviewers"), p.get("candidateGroups"));
        assertEquals(false, p.get("registrationUpload"));
        assertEquals(false, p.get("registrationDownload"));
    }
    @Test void missingAuthorityIsDenied() {
        assertFalse(service.hasCurrentProcessTaskAuthority("exact-process",42L));
        verify(mapper).count(anyMap());
    }
    @Test void missingInputNeverQueries() {
        assertFalse(service.hasCurrentProcessTaskAuthority("exact-process",null));
        assertFalse(service.hasCurrentProcessTaskAuthority(null,42L));
        assertFalse(service.hasCurrentProcessTaskAuthority(" ",42L));
        verifyNoInteractions(mapper,tasks,permissions,management);
    }
    @Test void missingTenantFailsBeforeQuery() {
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> service.hasCurrentProcessTaskAuthority("exact-process",42L));
        verifyNoInteractions(mapper);
    }
    @Test void brokenProvenanceCannotGrantAccess() {
        when(mapper.countBrokenProvenance(anyMap())).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> service.hasCurrentProcessTaskAuthority("exact-process",42L));
        verify(mapper, never()).count(anyMap());
    }
}
