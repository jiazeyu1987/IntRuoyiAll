package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrTaskDeadlineService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesWorkTaskAuxiliaryAudit;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesReleaseAttachmentUploadServiceTest {
    private final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProWorkOrderMapper orders = mock(MesProWorkOrderMapper.class);
    private final RoleApi roles = mock(RoleApi.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final NotifyMessageSendApi notices = mock(NotifyMessageSendApi.class);
    private final MesWorkTaskAuxiliaryAudit auxiliary = mock(MesWorkTaskAuxiliaryAudit.class);
    private final MesReleaseAttachmentUploadService service = new MesReleaseAttachmentUploadService(tasks,
            applications, orders, roles, permissions, users, mock(MesEdhrTaskDeadlineService.class), notices, auxiliary);
    private final List<MesProEdhrWorkTaskDO> inserted = new ArrayList<>();
    private MesProcessPoolActiveOrderReleaseApplicationDO application;

    @BeforeEach void setup() {
        login(71L, 1L);
        application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(31L).setActiveOrderId(20L)
                .setWorkOrderId(30L).setBatchExecutionId(10L).setRouteId(40L)
                .setPqcDecision("APPROVE").setApplicationStatus("MANAGER_RELEASE_PENDING");
        application.setTenantId(1L);
        var order = MesProWorkOrderDO.builder().id(30L).code("WO-30").batchCode("B-30").build();
        order.setTenantId(1L);
        when(orders.selectById(30L)).thenReturn(order);
        when(applications.selectList(any(Wrapper.class))).thenAnswer(call -> List.of(application));
        for (int i = 0; i < 4; i++) {
            var definition = MesReleaseAttachmentUploadService.DEFINITIONS.get(i);
            long id = 100L + i;
            when(roles.getRoleByCode(definition.roleCode())).thenReturn(new RoleRespDTO().setId(id).setStatus(0));
            when(permissions.getUserRoleIdListByRoleIds(List.of(id))).thenReturn(Set.of(71L, 72L, 73L));
        }
        when(users.getUserList(anyCollection())).thenReturn(List.of(
                new AdminUserRespDTO().setId(72L).setStatus(0), new AdminUserRespDTO().setId(71L).setStatus(0),
                new AdminUserRespDTO().setId(73L).setStatus(1)));
        when(tasks.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(call -> {
            var task = (MesProEdhrWorkTaskDO) call.getArgument(0);
            task.setId(200L + inserted.size());
            inserted.add(task);
            return 1;
        });
        when(tasks.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
        when(tasks.selectById(anyLong())).thenAnswer(call -> inserted.stream()
                .filter(task -> task.getId().equals(call.getArgument(0))).findFirst().orElse(null));
        when(tasks.selectList(any(Wrapper.class))).thenAnswer(call -> inserted.stream()
                .filter(task -> !"DONE".equals(task.getStatus())).toList());
        when(notices.sendSingleMessageToAdmin(any())).thenReturn(500L);
        doAnswer(call -> { ((Supplier<?>) call.getArgument(1)).get(); return null; })
                .when(auxiliary).notifications(any(), any());
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); TenantContextHolder.clear(); }

    @Test void createsFourIndependentSharedDutiesAndNotifiesEveryEnabledRoleMember() {
        service.initialize(application, 7L);
        assertEquals(4, inserted.size());
        assertEquals(4, inserted.stream().map(MesProEdhrWorkTaskDO::getResponsibilitySourceKey).distinct().count());
        assertEquals(2, inserted.stream().filter(t -> "FINISHED_PRODUCT_FILE".equals(t.getReviewSourceType())).count());
        for (var task : inserted) {
            assertEquals("DOSSIER_COMPLETE", task.getTaskType());
            assertEquals("71,72", task.getCandidateUserSnapshot());
            assertNull(task.getAssigneeUserId());
            assertEquals(20L, task.getBusinessScopeId());
            assertEquals(31L, task.getReviewSourceId());
            assertTrue(task.getActionUrl().contains("batchExecutionId=10"));
            assertTrue(task.getActionUrl().endsWith("workTaskId=" + task.getId()));
        }
        var captor = org.mockito.ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notices, times(8)).sendSingleMessageToAdmin(captor.capture());
        assertEquals(Set.of(71L, 72L), captor.getAllValues().stream().map(NotifySendSingleToUserReqDTO::getUserId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(4, service.pending(71L, 20L).size());
    }

    @Test void uploadCompletesOnlySelectedReportDutyAndRejectsWrongCategoryOrCandidate() {
        service.initialize(application, 7L);
        assertThrows(IllegalStateException.class, () -> service.validateUpload(71L, 202L, 20L, "STERILIZATION_FILE"));
        login(99L, 1L);
        assertThrows(IllegalStateException.class, () -> service.validateUpload(99L, 202L, 20L, "FINISHED_PRODUCT_FILE"));
        login(71L, 1L);
        when(tasks.update(any(MesProEdhrWorkTaskDO.class), any(Wrapper.class))).thenAnswer(call -> {
            var changes = (MesProEdhrWorkTaskDO) call.getArgument(0);
            assertEquals("DONE", changes.getStatus());
            assertEquals(71L, changes.getAssigneeUserId());
            assertNotNull(changes.getCompletedAt());
            inserted.get(2).setStatus("DONE").setAssigneeUserId(changes.getAssigneeUserId());
            return 1;
        });
        assertTrue(service.complete(71L, 202L, 20L, "FINISHED_PRODUCT_FILE"));
        assertEquals("TODO", inserted.get(3).getStatus());
        assertTrue(service.validateUpload(71L, 203L, 20L, "FINISHED_PRODUCT_FILE"));
    }

    @Test void staleRoundAndCrossTenantIdentityCannotUseUploadTask() {
        service.initialize(application, 7L);
        application.setId(32L);
        assertTrue(service.pending(71L, 20L).isEmpty());
        assertThrows(IllegalStateException.class, () -> service.validateUpload(71L, 200L, 20L, "INCOMING_INSPECTION_FILE"));
        TenantContextHolder.setTenantId(2L);
        assertThrows(IllegalStateException.class, () -> service.pending(71L, 20L));
        assertThrows(IllegalStateException.class, () -> service.initialize(application, 7L));
    }

    @Test void missingRoleOrIncompleteUserLookupFailsExplicitly() {
        when(roles.getRoleByCode(anyString())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.initialize(application, 7L));
        assertTrue(inserted.isEmpty());
    }

    private static void login(Long actor, Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        var login = new LoginUser().setId(actor).setTenantId(tenant).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }
}
