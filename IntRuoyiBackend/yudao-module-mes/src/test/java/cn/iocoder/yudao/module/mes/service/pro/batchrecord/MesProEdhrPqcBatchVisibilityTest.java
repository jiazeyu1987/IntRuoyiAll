package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrPqcBatchVisibilityTest {
    private static final long USER = 346, BATCH = 1256, ACTIVE = 1009, WORK = 990274, ROUTE = 980091, VERSION = 759;
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProEdhrBatchExecutionVisibilityService service = new MesProEdhrBatchExecutionVisibilityService();
    private final MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO().setId(BATCH)
            .setTenantId(1L).setWorkOrderId(WORK).setRouteId(ROUTE).setRouteVersionId(VERSION);
    private final MesProcessPoolActiveOrderDO active = new MesProcessPoolActiveOrderDO().setId(ACTIVE)
            .setWorkOrderId(WORK).setRouteId(ROUTE).setRouteVersionId(VERSION).setLeaderUserId(341L);
    private final MesProEdhrBatchExecutionOriginDO origin = new MesProEdhrBatchExecutionOriginDO().setId(700L)
            .setTenantId(1L).setBatchExecutionId(BATCH).setActiveOrderId(ACTIVE).setWorkOrderId(WORK);
    private final MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
            .setId(245L).setActiveOrderId(ACTIVE).setWorkOrderId(WORK).setRouteId(ROUTE).setRouteVersionId(VERSION)
            .setBatchExecutionId(BATCH).setPqcReleaseWorkTaskId(2734L)
            .setApplicationStatus("MANAGER_RELEASE_PENDING").setPqcDecision("APPROVE");
    private final MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setId(2734L).setBatchExecutionId(BATCH)
            .setWorkOrderId(WORK).setRouteId(ROUTE).setTaskType("PQC_PRODUCTION_RELEASE")
            .setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(245L).setOwnershipLocked(true)
            .setCandidateUserSnapshot("1, 346, 347").setAssigneeUserId(1L).setStatus("DONE").setReason("APPROVE");

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L); active.setTenantId(1L); application.setTenantId(1L);
        var marketAccess = org.mockito.Mockito.mock(MesMarketReleaseAccess.class);
        ReflectionTestUtils.setField(service, "marketReleaseAccess", marketAccess);
        org.mockito.Mockito.lenient().when(marketAccess.get(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesMarketReleaseAccess.Access(true, true, false, false, null, null, null, null));
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        ReflectionTestUtils.setField(service, "workTaskMapper", tasks);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", mock(MesProEdhrReleaseTransactionMapper.class));
        ReflectionTestUtils.setField(service, "batchOriginMapper", origins);
        ReflectionTestUtils.setField(service, "activeOrderMapper", orders);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", applications);
        when(origins.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(origin));
        when(orders.selectById(ACTIVE)).thenReturn(active);
        when(applications.selectByBatchExecutionId(BATCH)).thenReturn(application);
        when(tasks.selectById(2734L)).thenReturn(task);
    }
    @AfterEach void cleanup() {
        for (var dependency : List.of(tasks, applications, origins, orders)) {
            assertTrue(mockingDetails(dependency).getInvocations().stream().noneMatch(i ->
                    i.getMethod().getName().startsWith("insert") || i.getMethod().getName().startsWith("update")
                            || i.getMethod().getName().startsWith("delete")));
        }
        TenantContextHolder.clear();
    }
    private boolean visible() { return service.canViewBatch(batch, List.of(), USER); }

    @ParameterizedTest @ValueSource(strings = {"REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING", "RELEASED"})
    void originalFrozenPqcCandidateCanReadApprovedBatchAndHistory(String state) {
        application.setApplicationStatus(state);
        assertTrue(visible());
        assertDoesNotThrow(() -> service.requireVisibleBatch(batch, List.of(), USER));
        verify(permissions, atLeastOnce()).hasAnyPermissions(USER, MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION);
    }
    @ParameterizedTest @ValueSource(strings = {"TODO", "DOING", "OVERDUE"})
    void pendingOriginalCandidateCanReadOnlyItsFormalBatch(String state) {
        application.setApplicationStatus("PQC_RELEASE_PENDING").setPqcDecision(null);
        task.setStatus(state).setReason(null).setBatchExecutionId(null);
        assertTrue(visible());
        task.setBatchExecutionId(BATCH + 1); assertFalse(visible());
    }
    @Test void sameRoleAssigneeOrPartialCandidateIdCannotReadBatch() {
        task.setCandidateUserSnapshot("1346, 3460").setAssigneeUserId(USER); assertFalse(visible());
        task.setCandidateUserSnapshot(null); assertFalse(visible());
        task.setCandidateUserSnapshot("346").setOwnershipLocked(false); assertFalse(visible());
    }
    @Test void wrongApplicationOrTaskScopeCannotGrantRead() {
        application.setBatchExecutionId(BATCH + 1); assertFalse(visible());
        application.setBatchExecutionId(BATCH).setActiveOrderId(ACTIVE + 1); assertFalse(visible());
        application.setActiveOrderId(ACTIVE).setRouteVersionId(VERSION + 1); assertFalse(visible());
        application.setRouteVersionId(VERSION); task.setId(2735L); assertFalse(visible());
        task.setId(2734L).setBusinessScopeId(246L); assertFalse(visible());
        task.setBusinessScopeId(245L).setBusinessScopeType("RELEASE_TRANSACTION"); assertFalse(visible());
        task.setBusinessScopeType("RELEASE_APPLICATION").setTaskType("RELEASE_APPROVE"); assertFalse(visible());
    }
    @Test void wrongTenantSourceWorkOrDeletedRowsCannotGrantRead() {
        application.setTenantId(2L); assertFalse(visible());
        application.setTenantId(1L); active.setTenantId(2L); assertFalse(visible());
        active.setTenantId(1L); origin.setActiveOrderId(ACTIVE + 1); assertFalse(visible());
        origin.setActiveOrderId(ACTIVE); task.setWorkOrderId(WORK + 1); assertFalse(visible());
        task.setWorkOrderId(WORK).setDeleted(true); assertFalse(visible());
        task.setDeleted(false); application.setDeleted(true); assertFalse(visible());
        application.setDeleted(false); batch.setDeleted(true); assertFalse(visible());
    }
    @ParameterizedTest @ValueSource(strings = {"CANCELED", "REJECTED", "TODO"})
    void approvedApplicationDoesNotAcceptInconsistentTaskState(String state) {
        task.setStatus(state); assertFalse(visible());
    }
    @Test void rejectionOrClosedLifecycleDoesNotCreateApprovalReadScope() {
        task.setReason("REJECT"); assertFalse(visible());
        task.setReason("APPROVE"); application.setPqcDecision("REJECT"); assertFalse(visible());
        application.setPqcDecision("APPROVE").setApplicationStatus("NONCONFORMANCE_VOID"); assertFalse(visible());
    }
}
