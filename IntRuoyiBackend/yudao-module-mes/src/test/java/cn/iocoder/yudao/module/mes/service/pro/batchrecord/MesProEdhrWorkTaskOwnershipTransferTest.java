package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_OWNERSHIP_TRANSFER_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProEdhrWorkTaskServiceImpl.class, MesProEdhrCandidateResolver.class})
class MesProEdhrWorkTaskOwnershipTransferTest extends BaseDbUnitTest {

    private static final String FILLER_SOURCE_TYPE = "EDHR_PROCESS_FORM_FILLER";
    private static final String WORK_TASK_SOURCE_TYPE = "EDHR_WORK_TASK_ASSIGNEE";
    private static final String FILLER_POLICY = "MES_EDHR_FILLER_MINIMAL";

    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;

    @MockitoBean
    private NotifyMessageSendApi notifyMessageSendApi;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private RoleApi roleApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;
    @MockitoBean
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @MockitoBean
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @MockitoBean
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @MockitoBean
    private MesProRouteMapper routeMapper;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(122L);
        lenient().when(routeProcessService.resolveCurrentRouteProcess(any(), any(), any()))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        lenient().when(routeProcessService.resolveFrozenRouteProcess(any(), any(), any()))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_transfersSameSourceActiveTaskAndRuntimeClaim() {
        String sourceKey = "ROUTE|5901|REPORT-OWN-001|88002";
        insertBatchTask(19001L, 39001L, 5901L, "REPORT-OWN-001", 88002L);
        MesProEdhrWorkTaskDO task = insertFillTask(39001L, 19001L, 5901L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey)
                .setResponsibilitySourceVersion("88002")
                .setResponsibilitySourceDigest("old")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L, 503L))).thenReturn(List.of(
                adminUser(502L), adminUser(503L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            workTaskService.reconcileProcessFormFillTaskOwnership(
                    sourceKey,
                    processFormRule(5901L, "REPORT-OWN-001", 88002L, "502,503"),
                    "填写人配置变更");
        }

        MesProEdhrWorkTaskDO transferred = workTaskMapper.selectById(task.getId());
        assertEquals(task.getId(), transferred.getId());
        assertEquals(502L, transferred.getAssigneeUserId());
        assertEquals("USERS", transferred.getCandidateSourceType());
        assertEquals("502,503", transferred.getCandidateUserSnapshot());
        assertEquals(sourceKey, transferred.getResponsibilitySourceKey());
        assertEquals("88002", transferred.getResponsibilitySourceVersion());
        assertEquals(113L, transferred.getOwnershipLastTransferredBy());
        assertEquals("填写人配置变更", transferred.getReason());
        assertTrue(transferred.getActionUrl().startsWith("/mes/pro/feedback/edhr-execution/form?"));
        assertTrue(transferred.getActionUrl().contains("workTaskId=" + task.getId()));
        assertTrue(transferred.getActionUrl().contains("fillCarrier=FORM"));

        ArgumentCaptor<SystemEntitlementSyncReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(captor.capture());
        SystemEntitlementSyncReqDTO request = captor.getValue();
        assertEquals(122L, request.getTenantId());
        assertEquals(WORK_TASK_SOURCE_TYPE, request.getSourceType());
        assertEquals("WORK_TASK|" + task.getId(), request.getSourceKey());
        assertEquals(FILLER_POLICY, request.getPolicyCode());
        assertEquals(Set.of(502L, 503L), request.getResolvedUserIds());
        assertTrue(request.getSourceDigest().contains("responsibilitySourceKey=" + sourceKey));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reconcileProcessFormFillTaskOwnership_transfersSameSourceActiveTaskAndSendsReassignmentNotify() {
        String sourceKey = "ROUTE|5911|REPORT-OWN-011|88012";
        insertBatchTask(19101L, 39101L, 5911L, "REPORT-OWN-011", 88012L);
        MesProEdhrWorkTaskDO task = insertFillTask(39101L, 19101L, 5911L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey)
                .setResponsibilitySourceVersion("88012")
                .setResponsibilitySourceDigest("old")
                .setOwnershipLocked(false)
                .setReason("原填写人");
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L, 503L))).thenReturn(List.of(
                adminUser(502L), adminUser(503L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            workTaskService.reconcileProcessFormFillTaskOwnership(
                    sourceKey,
                    processFormRule(5911L, "REPORT-OWN-011", 88012L, "502,503"),
                    "填写人配置变更");
        }

        MesProEdhrWorkTaskDO transferred = workTaskMapper.selectById(task.getId());
        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        NotifySendSingleToUserReqDTO notifyReq = notifyCaptor.getValue();
        assertEquals(502L, notifyReq.getUserId());
        assertEquals("MES_EDHR_FILL_TASK_REASSIGNED", notifyReq.getTemplateCode());
        Map<String, Object> params = (Map<String, Object>) notifyReq.getTemplateParams();
        assertEquals(transferred.getActionUrl(), params.get("actionUrl"));
        assertEquals(transferred.getId(), params.get("workTaskId"));
        assertEquals("填写人配置变更", params.get("reason"));
        assertTrue(String.valueOf(params.get("actionUrl")).contains("workTaskId=" + transferred.getId()));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_doesNotNotifyWhenOwnerUnchanged() {
        String sourceKey = "ROUTE|5912|REPORT-OWN-012|88013";
        insertBatchTask(19102L, 39102L, 5912L, "REPORT-OWN-012", 88013L);
        MesProEdhrWorkTaskDO task = insertFillTask(39102L, 19102L, 5912L, 502L, "502")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey)
                .setResponsibilitySourceVersion("88013")
                .setResponsibilitySourceDigest("old")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            workTaskService.reconcileProcessFormFillTaskOwnership(
                    sourceKey,
                    processFormRule(5912L, "REPORT-OWN-012", 88013L, "502"),
                    "填写人配置变更");
        }

        verify(notifyMessageSendApi, never()).sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_failsFastWhenRuntimeEntitlementSyncFails() {
        String sourceKey = "ROUTE|5913|REPORT-OWN-013|88014";
        insertBatchTask(19103L, 39103L, 5913L, "REPORT-OWN-013", 88014L);
        MesProEdhrWorkTaskDO task = insertFillTask(39103L, 19103L, 5913L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey)
                .setResponsibilitySourceVersion("88014")
                .setResponsibilitySourceDigest("old")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));
        doThrow(new IllegalStateException("entitlement sync failed"))
                .when(permissionApi).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
                security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
                workTaskService.reconcileProcessFormFillTaskOwnership(
                        sourceKey,
                        processFormRule(5913L, "REPORT-OWN-013", 88014L, "502"),
                        "填写人配置变更");
            }
        });

        assertEquals("entitlement sync failed", exception.getMessage());
        verify(notifyMessageSendApi, never()).sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_failsFastWhenReassignmentNotifyFails() {
        String sourceKey = "ROUTE|5914|REPORT-OWN-014|88015";
        insertBatchTask(19104L, 39104L, 5914L, "REPORT-OWN-014", 88015L);
        MesProEdhrWorkTaskDO task = insertFillTask(39104L, 19104L, 5914L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey)
                .setResponsibilitySourceVersion("88015")
                .setResponsibilitySourceDigest("old")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));
        doThrow(new IllegalStateException("notify failed"))
                .when(notifyMessageSendApi).sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
                security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
                workTaskService.reconcileProcessFormFillTaskOwnership(
                        sourceKey,
                        processFormRule(5914L, "REPORT-OWN-014", 88015L, "502"),
                        "填写人配置变更");
            }
        });

        assertEquals("notify failed", exception.getMessage());
        verify(permissionApi).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_rejectsActiveSameRouteTaskWithoutSourceMarker() {
        insertBatchTask(19002L, 39002L, 5902L, "REPORT-OWN-002", 88003L);
        MesProEdhrWorkTaskDO task = insertFillTask(39002L, 19002L, 5902L, 501L, "501");
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.reconcileProcessFormFillTaskOwnership(
                        "ROUTE|5902|REPORT-OWN-002|88003",
                        processFormRule(5902L, "REPORT-OWN-002", 88003L, "502"),
                        "填写人配置变更"));

        assertEquals(PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING.getCode(), exception.getCode());
        assertEquals(501L, workTaskMapper.selectById(task.getId()).getAssigneeUserId());
        verify(permissionApi, never()).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_skipsLegacySourceLessTaskForFormLevelRule() {
        String reportId = "REPORT-OWN-102";
        Long versionId = 88102L;
        String formSourceKey = "FORM|" + reportId + "|" + versionId;
        insertBatchTask(19120L, 39120L, 5920L, reportId, versionId);
        MesProEdhrWorkTaskDO legacyRouteTask = insertFillTask(39120L, 19120L, 5920L, 501L, "501");
        insertBatchTask(19121L, 39121L, 5921L, reportId, versionId);
        MesProEdhrWorkTaskDO formTask = insertFillTask(39121L, 19121L, 5921L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(formSourceKey)
                .setResponsibilitySourceVersion(String.valueOf(versionId))
                .setResponsibilitySourceDigest("old")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(formTask);
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            workTaskService.reconcileProcessFormFillTaskOwnership(
                    formSourceKey,
                    processFormRule(MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID,
                            reportId, versionId, "502"),
                    "填写人配置变更");
        }

        MesProEdhrWorkTaskDO unchangedLegacyTask = workTaskMapper.selectById(legacyRouteTask.getId());
        assertEquals(501L, unchangedLegacyTask.getAssigneeUserId());
        assertEquals(null, unchangedLegacyTask.getResponsibilitySourceKey());
        MesProEdhrWorkTaskDO transferredFormTask = workTaskMapper.selectById(formTask.getId());
        assertEquals(502L, transferredFormTask.getAssigneeUserId());
        assertEquals(formSourceKey, transferredFormTask.getResponsibilitySourceKey());
        verify(permissionApi).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_rejectsLockedTaskAndKeepsOwner() {
        String sourceKey = "ROUTE|5903|REPORT-OWN-003|88004";
        insertBatchTask(19003L, 39003L, 5903L, "REPORT-OWN-003", 88004L);
        MesProEdhrWorkTaskDO task = insertFillTask(39003L, 19003L, 5903L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey)
                .setResponsibilitySourceVersion("88004")
                .setOwnershipLocked(true);
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.reconcileProcessFormFillTaskOwnership(
                        sourceKey,
                        processFormRule(5903L, "REPORT-OWN-003", 88004L, "502"),
                        "填写人配置变更"));

        assertEquals(PRO_EDHR_WORK_TASK_OWNERSHIP_TRANSFER_LOCKED.getCode(), exception.getCode());
        assertEquals(501L, workTaskMapper.selectById(task.getId()).getAssigneeUserId());
        verify(permissionApi, never()).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
    }

    private void insertBatchTask(Long batchTaskId, Long batchExecutionId, Long routeProcessId,
                                 String reportId, Long versionId) {
        batchTaskMapper.insert(new MesProEdhrBatchExecutionTaskDO()
                .setId(batchTaskId)
                .setBatchExecutionId(batchExecutionId)
                .setNodeType("ROUTE_FORM")
                .setRouteProcessId(routeProcessId)
                .setRouteProcessSort(10)
                .setProcessId(routeProcessId)
                .setProcessCode("P-" + routeProcessId)
                .setProcessName("eDHR 填写")
                .setBatchRecordReportId(reportId)
                .setBatchRecordVersionId(versionId)
                .setBatchRecordSort(0)
                .setExecutionMode("SEQUENTIAL")
                .setRequiredFlag(true)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING));
    }

    private MesProEdhrWorkTaskDO insertFillTask(Long executionId, Long batchTaskId, Long routeProcessId,
                                                Long assigneeUserId, String candidateSnapshot) {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHRT-OWN-" + executionId)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setBatchExecutionId(39000L)
                .setBatchTaskId(batchTaskId)
                .setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(batchTaskId)
                .setExecutionId(executionId)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-OWN")
                .setBatchCode("BATCH-OWN")
                .setRouteId(4901L)
                .setRouteProcessId(routeProcessId)
                .setProcessId(routeProcessId)
                .setProcessName("eDHR 填写")
                .setAssigneeUserId(assigneeUserId)
                .setCandidateSourceType("USERS")
                .setCandidateUserSnapshot(candidateSnapshot)
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setDueTime(LocalDateTime.now().plusHours(1))
                .setActionUrl("/mes/pro/feedback/edhr-execution/detail?id=" + executionId)
                .setSignatureCellKey("");
        workTaskMapper.insert(task);
        return task;
    }

    private MesProEdhrProcessFormPermissionRuleDO processFormRule(Long routeProcessId, String reportId,
                                                                  Long versionId, String candidateSourceIds) {
        return new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(reportId)
                .setBatchRecordVersionId(versionId)
                .setRuleType("FILL")
                .setSignatureCellKey("")
                .setCandidateSourceType("USERS")
                .setCandidateSourceIds(candidateSourceIds)
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(90)
                .setEnabled(true);
    }

    private AdminUserRespDTO adminUser(Long userId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }
}
