package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesActiveOrderDossierCompletionServiceTest {
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProcessPoolActiveOrderDossierFileMapper files = mock(MesProcessPoolActiveOrderDossierFileMapper.class);
    private final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final NotifyMessageSendApi notices = mock(NotifyMessageSendApi.class);
    private final MesWorkTaskAuxiliaryAudit auxiliary = mock(MesWorkTaskAuxiliaryAudit.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final MesProWorkOrderMapper workOrders = mock(MesProWorkOrderMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrOperationAuditService audit = mock(MesProEdhrOperationAuditService.class);
    private final GxpAuditService ledger = mock(GxpAuditService.class);
    private final MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
    private final MesEdhrTaskDeadlineService deadlines = mock(MesEdhrTaskDeadlineService.class);
    private final MesActiveOrderDossierCompletionService service = new MesActiveOrderDossierCompletionService(orders, files, tasks,
            permissions, notices, auxiliary, users, workOrders, applications, mock(MesProEdhrNonconformanceReviewService.class),
            mock(MesEdhrBatchLifecycleGuard.class), audit, ledger, reviews);
    private MesProcessPoolActiveOrderDO order;

    @BeforeEach void setup() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "deadlineService", deadlines);
        login(7L, 1L);
        order = new MesProcessPoolActiveOrderDO().setId(10L).setLeaderUserId(7L).setWorkOrderId(20L).setActiveStatus("ACTIVE").setBusinessStatus("ACTIVE");
        order.setTenantId(1L);
        when(orders.selectByIdForUpdate(10L)).thenReturn(order);
        when(orders.selectById(10L)).thenReturn(order);
        var work = MesProWorkOrderDO.builder().id(20L).code("WO-20").batchCode("B-20").build();
        work.setTenantId(1L);
        when(workOrders.selectById(20L)).thenReturn(work);
        when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenReturn(true);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        when(tasks.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(call -> { ((MesProEdhrWorkTaskDO) call.getArgument(0)).setId(31L); return 1; });
        when(tasks.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
        when(tasks.update(any(MesProEdhrWorkTaskDO.class), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);
        when(notices.sendSingleMessageToAdmin(any())).thenReturn(500L);
        doAnswer(call -> { ((java.util.function.Supplier<?>) call.getArgument(1)).get(); return null; })
                .when(auxiliary).notifications(any(), any());
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); TenantContextHolder.clear(); }

    @Test void explicitOwnerCreatesCategorySpecificTodoAndAtomicNotice() {
        var task = service.assign(7L, 10L, "OTHER_FILE", 8L, "补齐设备说明");
        assertEquals("DOSSIER_COMPLETE", task.getTaskType());
        assertEquals("ACTIVE_ORDER_DOSSIER", task.getBusinessScopeType());
        assertEquals(10L, task.getBusinessScopeId());
        assertEquals("OTHER_FILE", task.getReviewSourceType());
        assertEquals(8L, task.getAssigneeUserId());
        assertEquals("8", task.getCandidateUserSnapshot());
        assertEquals("/mes/pro/feedback/edhr-dossier-completion?workTaskId=31", task.getActionUrl());
        verify(auxiliary).notifications(eq(task), any());
        verify(notices).sendSingleMessageToAdmin(argThat(request -> Long.valueOf(8).equals(request.getUserId())
                && "MES_EDHR_DOSSIER_COMPLETE_ASSIGNED".equals(request.getTemplateCode())
                && task.getActionUrl().equals(request.getTemplateParams().get("actionUrl"))));
    }
    @Test void otherLeaderCannotAssign() {
        login(9L, 1L);
        assertThrows(IllegalStateException.class, () -> service.assign(9L, 10L, "OTHER_FILE", 8L, "补件"));
        verify(tasks, never()).insert(any(MesProEdhrWorkTaskDO.class));
    }
    @Test void wrongTenantCannotAssign() {
        login(7L, 2L);
        assertThrows(IllegalStateException.class, () -> service.assign(7L, 10L, "OTHER_FILE", 8L, "补件"));
    }
    @Test void frozenOrderCannotAssign() {
        order.setBusinessStatus("FROZEN");
        assertThrows(IllegalStateException.class, () -> service.assign(7L, 10L, "OTHER_FILE", 8L, "补件"));
        verifyNoInteractions(auxiliary);
    }
    @Test void recipientMustHaveUploadAndPersonalTodoPermissions() {
        when(permissions.hasAnyPermissions(8L, "mes:pro-edhr-batch-execution:upload")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.assign(7L, 10L, "OTHER_FILE", 8L, "补件"));
        verify(tasks, never()).insert(any(MesProEdhrWorkTaskDO.class));
    }
    @Test void reassignmentCancelsOldResponsibilityBeforeCreatingNewOne() {
        var old = task(8L);
        var deadline = java.time.LocalDateTime.now().plusMinutes(30);
        old.setDueTime(deadline);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(old));
        service.assign(7L, 10L, "OTHER_FILE", 9L, "重新交接");
        assertEquals("CANCELED", old.getStatus());
        verify(tasks).insert(argThat((MesProEdhrWorkTaskDO task) -> Long.valueOf(9).equals(task.getAssigneeUserId())
                && deadline.equals(task.getDueTime())));
        verifyNoInteractions(deadlines);
    }
    @Test void repeatedSameOwnerDoesNotCreateAnotherTaskOrNotice() {
        var old = task(8L);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(old));
        assertSame(old, service.assign(7L, 10L, "OTHER_FILE", 8L, "补件"));
        verify(tasks, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verifyNoInteractions(auxiliary);
    }
    @Test void onlyActualUploadedRelationCompletesResponsibleCategory() {
        login(8L, 1L);
        var task = task(8L);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task));
        when(files.selectById(99L)).thenReturn(new MesProcessPoolActiveOrderDossierFileDO().setId(99L).setActiveOrderId(10L).setCategoryKey("OTHER_FILE").setOperatorId(8L));
        assertTrue(service.completeUploadedCategory(8L, 10L, "OTHER_FILE", 99L));
        verify(tasks).update(argThat((MesProEdhrWorkTaskDO update) -> "DONE".equals(update.getStatus())), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        var auditCommand = ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(audit).recordInCallerTransaction(auditCommand.capture());
        assertTrue(auditCommand.getValue().getMetadataJson().contains("99"));
    }
    @Test void otherUploaderDoesNotCloseAssignedResponsibility() {
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task(8L)));
        assertFalse(service.completeUploadedCategory(7L, 10L, "OTHER_FILE", 99L));
        verify(tasks, never()).update(any(MesProEdhrWorkTaskDO.class), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
    @Test void mismatchedUploadedCategoryCannotCompleteTask() {
        login(8L, 1L);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task(8L)));
        when(files.selectById(99L)).thenReturn(new MesProcessPoolActiveOrderDossierFileDO().setId(99L).setActiveOrderId(10L).setCategoryKey("STERILIZATION_FILE").setOperatorId(8L));
        assertThrows(IllegalStateException.class, () -> service.completeUploadedCategory(8L, 10L, "OTHER_FILE", 99L));
    }
    @Test void expiredApplicationRoundCannotUseOldCompletionEntry() {
        login(8L, 1L);
        var old = task(8L).setReviewSourceId(50L);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(old));
        when(applications.selectLatestByActiveOrderId(10L)).thenReturn(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO().setId(51L));
        assertThrows(IllegalStateException.class, () -> service.validateCompletionUpload(8L, 30L, 10L, "OTHER_FILE"));
        verifyNoInteractions(files);
    }
    @Test void reassigningOldEntryCannotUploadIntoNewTask() {
        login(8L, 1L);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task(8L).setId(32L)));
        assertThrows(IllegalStateException.class, () -> service.validateCompletionUpload(8L, 30L, 10L, "OTHER_FILE"));
    }
    @Test void duplicateSameCategoryTasksStopCompletion() {
        login(8L, 1L);
        when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task(8L), task(8L).setId(33L)));
        assertThrows(IllegalStateException.class, () -> service.completeUploadedCategory(8L, 10L, "OTHER_FILE", 99L));
        verifyNoInteractions(files);
    }
    private static MesProEdhrWorkTaskDO task(Long owner) { return new MesProEdhrWorkTaskDO().setId(30L).setTaskType("DOSSIER_COMPLETE")
            .setBusinessScopeType("ACTIVE_ORDER_DOSSIER").setBusinessScopeId(10L).setWorkOrderId(20L)
            .setReviewSourceType("OTHER_FILE").setAssigneeUserId(owner).setStatus("TODO"); }
    private static void login(Long actor, Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        var user = new LoginUser().setId(actor).setTenantId(tenant).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
