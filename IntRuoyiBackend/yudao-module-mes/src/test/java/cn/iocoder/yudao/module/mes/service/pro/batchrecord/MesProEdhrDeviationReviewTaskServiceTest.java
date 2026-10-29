package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrDeviationReviewTaskServiceTest {
    @Mock private MesProEdhrWorkTaskMapper taskMapper;
    @Mock private AdminUserApi adminUserApi;
    @Mock private PermissionApi permissionApi;
    @Mock private NotifyMessageSendApi notifyApi;
    @Mock private MesWorkTaskAuxiliaryAudit auxiliaryAudit;
    private MesProEdhrDeviationReviewTaskService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(71L);
        service = new MesProEdhrDeviationReviewTaskService();
        ReflectionTestUtils.setField(service, "taskDeadlineService", mock(MesEdhrTaskDeadlineService.class));
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(service, "notifyApi", notifyApi);
        ReflectionTestUtils.setField(service, "auxiliaryAudit", auxiliaryAudit);
        lenient().when(adminUserApi.getUser(201L)).thenReturn(user(0));
        lenient().when(permissionApi.hasAnyPermissions(eq(201L), any(String[].class))).thenReturn(true);
    }

    @AfterEach void tearDown() { TenantContextHolder.clear(); }

    @Test
    void unchangedPendingOrCompletedResponsibilityDoesNotDuplicateTaskOrNotification() {
        var task = task("DONE", "1", "hash-1");
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        service.assign(deviation(), handling(), "QA", 201L);
        task.setStatus("TODO");
        service.assign(deviation(), handling(), "QA", 201L);
        verify(taskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verify(taskMapper, never()).update(any(MesProEdhrWorkTaskDO.class), any());
        verifyNoInteractions(notifyApi, auxiliaryAudit);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"PREPARER,DEVIATION_SIGN", "VERIFIER,DEVIATION_SIGN",
            "DEPARTMENT_OWNER,DEVIATION_SIGN", "QA,DEVIATION_APPROVE", "QUALITY_OWNER,DEVIATION_APPROVE",
            "MANAGEMENT_REP,DEVIATION_APPROVE", "CLOSE,DEVIATION_CLOSE"})
    void explicitResponsibilityCreatesCorrectTaskCategoryAndOneAuditedNotification(String node, String type) {
        when(taskMapper.selectList(any())).thenReturn(List.of());
        doAnswer(call -> { ((MesProEdhrWorkTaskDO) call.getArgument(0)).setId(6001L); return 1; })
                .when(taskMapper).insert(any(MesProEdhrWorkTaskDO.class));
        when(notifyApi.sendSingleMessageToAdmin(any())).thenReturn(5001L);
        doAnswer(call -> { call.<Supplier<List<Long>>>getArgument(1).get(); return null; })
                .when(auxiliaryAudit).notifications(any(), any());

        service.assign(deviation(), handling(), node, 201L);

        var inserted = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(taskMapper).insert(inserted.capture());
        assertEquals(type, inserted.getValue().getTaskType());
        assertEquals(node, inserted.getValue().getSignatureCellKey());
        assertEquals("1", inserted.getValue().getResponsibilitySourceVersion());
        assertEquals("hash-1", inserted.getValue().getResponsibilitySourceDigest());
        assertEquals(201L, inserted.getValue().getAssigneeUserId());
        assertEquals("201", inserted.getValue().getCandidateUserSnapshot());
        assertEquals("/mes/pro/feedback/edhr-deviation?deviationId=901", inserted.getValue().getActionUrl());
        verify(notifyApi).sendSingleMessageToAdmin(argThat(message -> message.getUserId().equals(201L)
                && message.getTemplateCode().equals("MES_EDHR_DEVIATION_REVIEW_ASSIGNED")));
    }

    @Test
    void oldVersionIsCancelledBeforeReplacementTaskIsInserted() {
        when(taskMapper.selectList(any())).thenReturn(List.of(task("TODO", "0", "old-hash")));
        when(taskMapper.update(any(MesProEdhrWorkTaskDO.class), any())).thenReturn(1);
        doAnswer(call -> { ((MesProEdhrWorkTaskDO) call.getArgument(0)).setId(6002L); return 1; })
                .when(taskMapper).insert(any(MesProEdhrWorkTaskDO.class));

        service.assign(deviation(), handling(), "QA", 201L);

        var order = inOrder(taskMapper);
        order.verify(taskMapper).selectList(any());
        order.verify(taskMapper).update(argThat(task -> "CANCELED".equals(task.getStatus())), any());
        order.verify(taskMapper).insert(any(MesProEdhrWorkTaskDO.class));
    }

    @Test
    void disabledOrUnentitledUserCannotReceiveTask() {
        when(adminUserApi.getUser(201L)).thenReturn(user(1));
        assertThrows(ServiceException.class, () -> service.assign(deviation(), handling(), "QA", 201L));
        verifyNoInteractions(taskMapper, auxiliaryAudit, notifyApi);
    }

    @Test
    void signerPermissionAndTenantMustMatchTheExplicitResponsibility() {
        when(permissionApi.hasAnyPermissions(201L, "mes:pro-edhr-deviation:quality-approve")).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.assign(deviation(), handling(), "QUALITY_OWNER", 201L));
        var otherTenantUser = user(0);
        otherTenantUser.setTenantId(72L);
        when(adminUserApi.getUser(201L)).thenReturn(otherTenantUser);
        assertThrows(ServiceException.class, () -> service.assign(deviation(), handling(), "QA", 201L));
        verifyNoInteractions(taskMapper, auxiliaryAudit, notifyApi);
    }

    @Test
    void taskCompletionPersistsExactSignatureAndVersionReceiptInTheSameUpdate() {
        when(taskMapper.update(any(MesProEdhrWorkTaskDO.class), any())).thenReturn(1);
        service.completeSigned(task("TODO", "1", "hash-1"), 9001L, "核对记录通过");
        var updated = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(taskMapper).update(updated.capture(), any());
        assertEquals("DONE", updated.getValue().getStatus());
        var receipt = com.alibaba.fastjson.JSON.parseObject(updated.getValue().getResponsibilityScopeJson());
        assertEquals(9001L, receipt.getLong("signatureId"));
        assertEquals(201L, receipt.getLong("actorUserId"));
        assertEquals("QA", receipt.getString("node"));
        assertEquals("1", receipt.getString("contentVersion"));
        assertEquals("hash-1", receipt.getString("contentHash"));
        assertEquals("核对记录通过", receipt.getString("userOpinion"));
    }

    @Test
    void noCurrentAssignedTaskRejectsSigningOrClosure() {
        when(taskMapper.selectList(any())).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.require(901L, handling(), "QA", 201L));
        verifyNoInteractions(auxiliaryAudit, notifyApi);
    }

    private MesProEdhrDeviationDO deviation() {
        return new MesProEdhrDeviationDO().setId(901L).setTenantId(71L).setDeviationCode("PC-202610-0001");
    }
    private AdminUserRespDTO user(int status) {
        var user = new AdminUserRespDTO();
        user.setId(201L);
        user.setTenantId(71L);
        user.setStatus(status);
        return user;
    }
    private MesProEdhrDeviationHandlingDO handling() {
        return new MesProEdhrDeviationHandlingDO().setId(7701L).setContentVersion(1).setContentHash("hash-1");
    }
    private MesProEdhrWorkTaskDO task(String status, String version, String hash) {
        return new MesProEdhrWorkTaskDO().setId(6001L).setStatus(status).setAssigneeUserId(201L)
                .setReviewSourceType("EDHR_DEVIATION_HANDLING").setReviewSourceId(7701L)
                .setTaskType("DEVIATION_APPROVE").setSignatureCellKey("QA")
                .setResponsibilitySourceVersion(version).setResponsibilitySourceDigest(hash);
    }
}
