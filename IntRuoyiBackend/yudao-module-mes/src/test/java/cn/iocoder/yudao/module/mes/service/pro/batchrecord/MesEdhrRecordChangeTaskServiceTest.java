package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrRecordChangeTaskServiceTest {
    @InjectMocks private MesEdhrRecordChangeTaskService service;
    @Mock private MesProEdhrWorkTaskMapper taskMapper;
    @Mock private AdminUserApi userApi;
    @Mock private PermissionApi permissionApi;
    @Mock private NotifyMessageSendApi notifyApi;
    @Mock private MesEdhrTaskDeadlineService deadlineService;

    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    private MesProEdhrRecordChangeEventDO event() {
        return new MesProEdhrRecordChangeEventDO().setId(7L).setRequestedBy(10L)
                .setChangeCode("CHANGE-7").setChangeType("REOPEN").setTargetScope("BATCH")
                .setChangeStatus("SUBMITTED").setBatchExecutionId(30L).setReasonText("受控重开原因")
                .setApprovedAt(java.time.LocalDateTime.of(2026, 10, 28, 12, 0));
    }

    private void authorized() {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(20L); user.setTenantId(1L); user.setStatus(0);
        when(userApi.getUser(20L)).thenReturn(user);
        when(permissionApi.hasAnyPermissions(eq(20L), any(String[].class))).thenReturn(true);
    }

    @Test void selection_requiresExplicitIndependentReviewer() {
        assertThrows(IllegalArgumentException.class, () -> service.validateApprover(null, 10L));
        assertThrows(IllegalArgumentException.class, () -> service.validateApprover(10L, 10L));
        verifyNoInteractions(userApi, permissionApi, taskMapper, notifyApi);
    }

    @Test void selection_rejectsForeignTenantAndMissingPermission() {
        AdminUserRespDTO user = new AdminUserRespDTO(); user.setId(20L); user.setTenantId(2L);
        when(userApi.getUser(20L)).thenReturn(user);
        assertThrows(IllegalArgumentException.class, () -> service.validateApprover(20L, 10L));
        user.setTenantId(1L);
        assertThrows(IllegalArgumentException.class, () -> service.validateApprover(20L, 10L));
        verifyNoInteractions(taskMapper, notifyApi);
    }

    @Test void assignment_freezesSelectedReviewerWithoutInboxNotification() {
        authorized();
        when(taskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(call -> {
            ((MesProEdhrWorkTaskDO) call.getArgument(0)).setId(99L); return 1;
        });
        MesProEdhrWorkTaskDO task = service.assign(event(), 20L);
        assertEquals("RECORD_CHANGE_APPROVE", task.getTaskType());
        assertEquals("RECORD_CHANGE", task.getBusinessScopeType());
        assertEquals(7L, task.getBusinessScopeId());
        assertEquals(20L, task.getAssigneeUserId());
        assertEquals("20", task.getCandidateUserSnapshot());
        assertEquals(10L, task.getSourceUserId());
        assertEquals("TODO", task.getStatus());
        assertEquals("/mes/pro/feedback/edhr-change?changeEventId=7", task.getActionUrl());
        verifyNoInteractions(notifyApi);
    }

    @Test void assignmentReplay_doesNotDuplicateTaskOrNoticeAndCannotReplaceReviewer() {
        authorized();
        MesProEdhrWorkTaskDO frozen = new MesProEdhrWorkTaskDO().setId(99L).setAssigneeUserId(20L);
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(frozen);
        assertSame(frozen, service.assign(event(), 20L));
        verify(taskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verifyNoInteractions(notifyApi);
    }

    @Test void review_rejectsUnassignedUserSelfReviewAndClosedTask() {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setId(99L).setAssigneeUserId(20L).setStatus("TODO");
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);
            assertThrows(IllegalArgumentException.class, () -> service.requireReviewer(event()));
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(21L);
            assertThrows(IllegalArgumentException.class, () -> service.requireReviewer(event()));
            task.setStatus("DONE");
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            assertThrows(IllegalArgumentException.class, () -> service.requireReviewer(event()));
        }
        verifyNoInteractions(notifyApi);
    }

    @Test void approval_closesOnlyFrozenTaskWithoutInboxNotification() {
        authorized();
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setId(99L).setAssigneeUserId(20L)
                .setStatus("TODO").setActionUrl("/mes/pro/feedback/edhr-change?changeEventId=7");
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(taskMapper.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            service.complete(event().setChangeStatus("EFFECTIVE").setApprovedBy(20L).setApprovalSignatureId(50L)
                    .setEffectiveAt(java.time.LocalDateTime.of(2026, 10, 28, 12, 0)));
        }
        var update = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(taskMapper).updateById(update.capture());
        assertEquals(99L, update.getValue().getId());
        assertEquals("DONE", update.getValue().getStatus());
        assertNotNull(update.getValue().getCompletedAt());
        verifyNoInteractions(notifyApi);
    }

    @Test void rejectedDecision_completesApprovalWithoutNoticeOrCorrectionTask() {
        negativeDecision("REJECTED");
        verify(taskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
    }

    @Test void returnedDecision_assignsOriginalRequesterAndUsesCorrectionUrl() {
        when(taskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(call -> {
            ((MesProEdhrWorkTaskDO) call.getArgument(0)).setId(100L); return 1;
        });
        negativeDecision("RETURNED");
        var task = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(taskMapper).insert(task.capture());
        assertEquals("RECORD_CHANGE_CORRECTION", task.getValue().getTaskType());
        assertEquals("RECORD_CHANGE_CORRECTION", task.getValue().getBusinessScopeType());
        assertEquals(10L, task.getValue().getAssigneeUserId());
        assertEquals(20L, task.getValue().getSourceUserId());
        assertEquals("证据不足", task.getValue().getReason());
        assertTrue(task.getValue().getActionUrl().contains("changeEventId=7"));
        verify(deadlineService).applyToWorkTask(task.getValue());
    }

    private void negativeDecision(String decision) {
        authorized();
        var task = new MesProEdhrWorkTaskDO().setId(99L).setAssigneeUserId(20L).setStatus("TODO")
                .setActionUrl("/mes/pro/feedback/edhr-change?changeEventId=7");
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(taskMapper.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            service.completeNegativeDecision(event().setChangeStatus(decision).setApprovedBy(20L)
                    .setApprovalSignatureId(51L).setDecisionReason("证据不足"));
        }
        var update = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(taskMapper).updateById(update.capture());
        assertEquals("DONE", update.getValue().getStatus());
        verifyNoInteractions(notifyApi);
    }

    @Test void correction_rejectsOtherApplicantAndRequiresCurrentApplyPermission() {
        var correction = new MesProEdhrWorkTaskDO().setId(100L).setAssigneeUserId(10L).setStatus("TODO");
        when(taskMapper.selectActiveByBusinessScopeAndType("RECORD_CHANGE_CORRECTION", 7L, "RECORD_CHANGE_CORRECTION"))
                .thenReturn(correction);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            assertThrows(IllegalArgumentException.class, () -> service.requireRequester(event()));
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);
            assertThrows(IllegalArgumentException.class, () -> service.requireRequester(event()));
            when(permissionApi.hasAnyPermissions(10L, "mes:pro-edhr-change:reopen")).thenReturn(true);
            assertDoesNotThrow(() -> service.requireRequester(event()));
        }
    }

    @Test void assignmentDoesNotDependOnNotificationReceipt() {
        authorized();
        when(taskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(call -> {
            ((MesProEdhrWorkTaskDO) call.getArgument(0)).setId(99L); return 1;
        });
        assertEquals("TODO", service.assign(event(), 20L).getStatus());
        verifyNoInteractions(notifyApi);
    }
}
