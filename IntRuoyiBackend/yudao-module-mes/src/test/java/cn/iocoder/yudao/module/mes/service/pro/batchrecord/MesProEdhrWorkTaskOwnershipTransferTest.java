package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

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
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;

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

    @ParameterizedTest
    @ValueSource(strings = {"FILL", "REWORK"})
    void reconcileProcessFormFillTaskOwnership_refreshesAssistScopeForNewCandidatesOnly(String taskType) {
        String reportId = "REPORT-OWN-SCOPE";
        String sourceKey = "FORM|" + reportId + "|88160";
        insertBatchTask(19160L, 39160L, 5960L, reportId, 88160L);
        MesProEdhrWorkTaskDO task = insertFillTask(39160L, 19160L, 5960L, 501L, "501")
                .setTaskType(taskType)
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey).setResponsibilitySourceVersion("88160")
                .setResponsibilityScopeJson("{\"schemaVersion\":2,\"scopes\":[{\"scopeKey\":\"ALL\","
                        + "\"candidateSourceType\":\"ROLE\",\"candidateSourceIds\":[910415],"
                        + "\"resolvedUserIds\":[501],\"fillableScope\":" + ownershipFillableScope() + "}]}")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        insertBatchTask(19161L, 39161L, 5960L, reportId, 88161L);
        MesProEdhrWorkTaskDO otherVersion = insertFillTask(39161L, 19161L, 5960L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey).setResponsibilitySourceVersion("88160")
                .setResponsibilityScopeJson(task.getResponsibilityScopeJson());
        workTaskMapper.updateById(otherVersion);
        when(adminUserApi.getUserList(List.of(502L, 503L))).thenReturn(List.of(adminUser(502L), adminUser(503L)));

        workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey,
                processFormRule(0L, reportId, 88160L, "502,503"), "保留现有填写范围");

        MesProEdhrWorkTaskDO updated = workTaskMapper.selectById(task.getId());
        JSONObject snapshot = JSON.parseObject(updated.getResponsibilityScopeJson());
        JSONObject scope = snapshot.getJSONArray("scopes").getJSONObject(0);
        assertEquals(2, snapshot.getIntValue("schemaVersion"));
        assertEquals(FILLER_SOURCE_TYPE, snapshot.getString("sourceType"));
        assertEquals(sourceKey, snapshot.getString("sourceKey"));
        assertEquals("88160", snapshot.getString("sourceVersion"));
        assertEquals("USERS", scope.getString("candidateSourceType"));
        assertEquals(List.of(502L, 503L), scope.getJSONArray("candidateSourceIds").toJavaList(Long.class));
        assertEquals(List.of(502L, 503L), scope.getJSONArray("resolvedUserIds").toJavaList(Long.class));
        assertEquals(JSON.parseObject(ownershipFillableScope()), scope.getJSONObject("fillableScope"));
        assertEquals("scopes-sha256=" + DigestUtil.sha256Hex(updated.getResponsibilityScopeJson()),
                updated.getResponsibilitySourceDigest());
        assertEquals(taskType, updated.getTaskType());
        assertEquals(task.getBatchTaskId(), updated.getBatchTaskId());
        MesProEdhrBatchExecutionServiceImpl executionService = new MesProEdhrBatchExecutionServiceImpl();
        List<?> visible = ReflectionTestUtils.invokeMethod(executionService, "resolveVisibleAssistScopes", updated, 502L);
        assertEquals(1, visible.size());
        List<?> otherCandidateVisible = ReflectionTestUtils.invokeMethod(
                executionService, "resolveVisibleAssistScopes", updated, 503L);
        assertEquals(1, otherCandidateVisible.size());
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                executionService, "resolveVisibleAssistScopes", updated, 501L));
        MesProEdhrWorkTaskDO unchanged = workTaskMapper.selectById(otherVersion.getId());
        assertEquals("501", unchanged.getCandidateUserSnapshot());
        assertEquals(otherVersion.getResponsibilityScopeJson(), unchanged.getResponsibilityScopeJson());
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_preservesEveryAssistRowAndItsCandidateIsolation() {
        String reportId = "REPORT-OWN-ASSIST";
        String sourceKey = "FORM|" + reportId + "|88170";
        insertBatchTask(19170L, 39170L, 5970L, reportId, 88170L);
        MesProEdhrWorkTaskDO task = insertFillTask(39170L, 19170L, 5970L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey).setResponsibilitySourceVersion("88170")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        MesProEdhrProcessFormPermissionRuleDO equipment = processFormRule(0L, reportId, 88170L, "502")
                .setRuleType("EQUIPMENT_FILL").setScopeKey("EQUIPMENT-ROW");
        MesProEdhrProcessFormPermissionRuleDO quality = processFormRule(0L, reportId, 88170L, "503")
                .setRuleType("QUALITY_FILL").setScopeKey("QUALITY-ROW")
                .setFillableScopeJson("{\"cells\":[{\"sourceTableIndex\":1,\"rowIndex\":8,\"columnIndex\":3}]}");
        when(processFormPermissionRuleMapper.selectEnabledFillRules(0L, reportId, 88170L))
                .thenReturn(List.of(equipment, quality));
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));
        when(adminUserApi.getUserList(List.of(503L))).thenReturn(List.of(adminUser(503L)));

        workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, equipment, "辅助行候选更新");

        MesProEdhrWorkTaskDO updated = workTaskMapper.selectById(task.getId());
        assertEquals("ASSIST_ROWS", updated.getCandidateSourceType());
        assertEquals("502,503", updated.getCandidateUserSnapshot());
        JSONObject snapshot = JSON.parseObject(updated.getResponsibilityScopeJson());
        assertEquals(2, snapshot.getJSONArray("scopes").size());
        JSONObject first = snapshot.getJSONArray("scopes").getJSONObject(0);
        JSONObject second = snapshot.getJSONArray("scopes").getJSONObject(1);
        assertEquals("EQUIPMENT-ROW", first.getString("scopeKey"));
        assertEquals(List.of(502L), first.getJSONArray("resolvedUserIds").toJavaList(Long.class));
        assertEquals(JSON.parseObject(equipment.getFillableScopeJson()), first.getJSONObject("fillableScope"));
        assertEquals("QUALITY-ROW", second.getString("scopeKey"));
        assertEquals(List.of(503L), second.getJSONArray("resolvedUserIds").toJavaList(Long.class));
        assertEquals(JSON.parseObject(quality.getFillableScopeJson()), second.getJSONObject("fillableScope"));
        MesProEdhrBatchExecutionServiceImpl executionService = new MesProEdhrBatchExecutionServiceImpl();
        List<?> equipmentVisible = ReflectionTestUtils.invokeMethod(
                executionService, "resolveVisibleAssistScopes", updated, 502L);
        List<?> qualityVisible = ReflectionTestUtils.invokeMethod(
                executionService, "resolveVisibleAssistScopes", updated, 503L);
        assertEquals(1, equipmentVisible.size());
        assertEquals(1, qualityVisible.size());
        assertEquals("EQUIPMENT-ROW", ReflectionTestUtils.invokeMethod(equipmentVisible.get(0), "scopeKey"));
        assertEquals("QUALITY-ROW", ReflectionTestUtils.invokeMethod(qualityVisible.get(0), "scopeKey"));
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                executionService, "resolveVisibleAssistScopes", updated, 501L));
        verify(processFormPermissionRuleMapper).selectEnabledFillRules(0L, reportId, 88170L);
        verify(processFormPermissionRuleMapper, never()).selectEnabledFillRules(5970L, reportId, 88170L);
        verify(processFormPermissionRuleMapper, never()).selectEnabledFillRules(0L, reportId, 88171L);
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_explicitDisabledFormalRuleKeepsExistingTaskResponsibility() {
        String reportId = "REPORT-OWN-DISABLED";
        String sourceKey = "FORM|" + reportId + "|88180";
        insertBatchTask(19180L, 39180L, 5980L, reportId, 88180L);
        MesProEdhrWorkTaskDO task = insertFillTask(39180L, 19180L, 5980L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey).setResponsibilitySourceVersion("88180")
                .setResponsibilitySourceDigest("existing-frozen-digest")
                .setResponsibilityScopeJson("{\"existingFrozenResponsibility\":true}")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        MesProEdhrProcessFormPermissionRuleDO disabled = processFormRule(0L, reportId, 88180L, "502")
                .setId(19180L).setEnabled(false);
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));
        when(processFormPermissionRuleMapper.selectEnabledFillRules(0L, reportId, 88180L)).thenReturn(List.of());
        when(processFormPermissionRuleMapper.selectListByRouteProcessReportAndVersion(0L, reportId, 88180L))
                .thenReturn(List.of(disabled));

        workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, disabled, "显式禁用配置");

        MesProEdhrWorkTaskDO unchanged = workTaskMapper.selectById(task.getId());
        assertEquals(501L, unchanged.getAssigneeUserId());
        assertEquals("501", unchanged.getCandidateUserSnapshot());
        assertEquals(task.getResponsibilityScopeJson(), unchanged.getResponsibilityScopeJson());
        assertEquals(task.getResponsibilitySourceDigest(), unchanged.getResponsibilitySourceDigest());
        verify(permissionApi, never()).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
        verify(notifyMessageSendApi, never()).sendSingleMessageToAdmin(any());

        when(processFormPermissionRuleMapper.selectListByRouteProcessReportAndVersion(0L, reportId, 88180L))
                .thenReturn(List.of());
        ServiceException missing = assertThrows(ServiceException.class,
                () -> workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, disabled, "不存在的正式规则"));
        assertEquals(PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING.getCode(), missing.getCode());
        disabled.setEnabled(true);
        ServiceException enabledMissing = assertThrows(ServiceException.class,
                () -> workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, disabled, "启用来源缺失"));
        assertEquals(PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING.getCode(), enabledMissing.getCode());
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_disabledAnchorStillRefreshesRemainingEnabledAssistRows() {
        String reportId = "REPORT-OWN-PARTIAL-DISABLE";
        String sourceKey = "FORM|" + reportId + "|88190";
        insertBatchTask(19190L, 39190L, 5990L, reportId, 88190L);
        MesProEdhrWorkTaskDO task = insertFillTask(39190L, 19190L, 5990L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey).setResponsibilitySourceVersion("88190")
                .setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        MesProEdhrProcessFormPermissionRuleDO disabled = processFormRule(0L, reportId, 88190L, "502")
                .setId(19190L).setRuleType("EQUIPMENT_FILL").setScopeKey("EQUIPMENT-ROW").setEnabled(false);
        MesProEdhrProcessFormPermissionRuleDO remaining = processFormRule(0L, reportId, 88190L, "503")
                .setRuleType("QUALITY_FILL").setScopeKey("QUALITY-ROW");
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));
        when(adminUserApi.getUserList(List.of(503L))).thenReturn(List.of(adminUser(503L)));
        when(processFormPermissionRuleMapper.selectEnabledFillRules(0L, reportId, 88190L))
                .thenReturn(List.of(remaining));

        workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, disabled, "禁用部分辅助行");

        MesProEdhrWorkTaskDO updated = workTaskMapper.selectById(task.getId());
        assertEquals("503", updated.getCandidateUserSnapshot());
        JSONObject snapshot = JSON.parseObject(updated.getResponsibilityScopeJson());
        assertEquals(1, snapshot.getJSONArray("scopes").size());
        assertEquals("QUALITY-ROW", snapshot.getJSONArray("scopes").getJSONObject(0).getString("scopeKey"));
        MesProEdhrBatchExecutionServiceImpl executionService = new MesProEdhrBatchExecutionServiceImpl();
        List<?> visible = ReflectionTestUtils.invokeMethod(executionService, "resolveVisibleAssistScopes", updated, 503L);
        assertEquals(1, visible.size());
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                executionService, "resolveVisibleAssistScopes", updated, 502L));
        verify(processFormPermissionRuleMapper, never()).selectListByRouteProcessReportAndVersion(0L, reportId, 88190L);
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
    void reconcileProcessFormFillTaskOwnership_migratesExactRouteFillerToFormAndRuntimeClaim() {
        String reportId = "REPORT-OWN-MIGRATE";
        Long versionId = 88130L;
        String formKey = "FORM|" + reportId + "|" + versionId;
        insertBatchTask(19130L, 39130L, 5930L, reportId, versionId);
        MesProEdhrWorkTaskDO task = insertFillTask(39130L, 19130L, 5930L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey("ROUTE|5930|" + reportId + "|" + versionId)
                .setResponsibilitySourceVersion(String.valueOf(versionId)).setOwnershipLocked(false);
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L, 503L))).thenReturn(List.of(adminUser(502L), adminUser(503L)));
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            workTaskService.reconcileProcessFormFillTaskOwnership(formKey,
                    processFormRule(0L, reportId, versionId, "502,503"), "表单填写人统一配置");
        }
        MesProEdhrWorkTaskDO updated = workTaskMapper.selectById(task.getId());
        assertEquals(formKey, updated.getResponsibilitySourceKey());
        assertEquals(FILLER_SOURCE_TYPE, updated.getResponsibilitySourceType());
        assertEquals("88130", updated.getResponsibilitySourceVersion());
        assertEquals(502L, updated.getAssigneeUserId());
        assertEquals("502,503", updated.getCandidateUserSnapshot());
        assertEquals("USERS", updated.getCandidateSourceType());
        ArgumentCaptor<SystemEntitlementSyncReqDTO> captor = ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(captor.capture());
        assertEquals("WORK_TASK|" + task.getId(), captor.getValue().getSourceKey());
        assertEquals(Set.of(502L, 503L), captor.getValue().getResolvedUserIds());
        assertTrue(captor.getValue().getSourceDigest().contains("responsibilitySourceKey=" + formKey));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_doesNotMigrateOtherRouteSourcesOrTargets() {
        String reportId = "REPORT-OWN-MIGRATE-BOUNDARY";
        Long versionId = 88131L;
        List<MesProEdhrWorkTaskDO> tasks = new java.util.ArrayList<>();
        for (int index = 0; index < 5; index++) {
            long routeProcessId = 5940L + index;
            String taskReportId = index == 3 ? "OTHER-REPORT" : reportId;
            Long taskVersionId = index == 4 ? 88132L : versionId;
            insertBatchTask(19140L + index, 39140L + index, routeProcessId, taskReportId, taskVersionId);
            String sourceKey = "ROUTE|" + routeProcessId + "|" + taskReportId + "|" + taskVersionId;
            if (index == 1) sourceKey = "ROUTE|9999|" + reportId + "|" + versionId;
            if (index == 2) sourceKey = "ROUTE|" + routeProcessId + "|" + reportId + "|88132";
            MesProEdhrWorkTaskDO task = insertFillTask(39140L + index, 19140L + index, routeProcessId, 501L, "501")
                    .setResponsibilitySourceType(index == 0 ? "OTHER_SOURCE" : FILLER_SOURCE_TYPE)
                    .setResponsibilitySourceKey(sourceKey).setOwnershipLocked(false);
            workTaskMapper.updateById(task);
            tasks.add(task);
        }
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));
        workTaskService.reconcileProcessFormFillTaskOwnership("FORM|" + reportId + "|" + versionId,
                processFormRule(0L, reportId, versionId, "502"), "统一配置");
        for (MesProEdhrWorkTaskDO task : tasks) {
            MesProEdhrWorkTaskDO unchanged = workTaskMapper.selectById(task.getId());
            assertEquals(501L, unchanged.getAssigneeUserId());
            assertEquals(task.getResponsibilitySourceKey(), unchanged.getResponsibilitySourceKey());
            assertEquals("501", unchanged.getCandidateUserSnapshot());
        }
        verify(permissionApi, never()).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_rejectsLockedRouteToFormMigration() {
        String reportId = "REPORT-OWN-MIGRATE-LOCKED";
        insertBatchTask(19150L, 39150L, 5950L, reportId, 88150L);
        String routeKey = "ROUTE|5950|" + reportId + "|88150";
        MesProEdhrWorkTaskDO task = insertFillTask(39150L, 19150L, 5950L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE).setResponsibilitySourceKey(routeKey)
                .setOwnershipLocked(true);
        workTaskMapper.updateById(task);
        when(adminUserApi.getUserList(List.of(502L))).thenReturn(List.of(adminUser(502L)));
        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.reconcileProcessFormFillTaskOwnership("FORM|" + reportId + "|88150",
                        processFormRule(0L, reportId, 88150L, "502"), "统一配置"));
        assertEquals(PRO_EDHR_WORK_TASK_OWNERSHIP_TRANSFER_LOCKED.getCode(), exception.getCode());
        MesProEdhrWorkTaskDO unchanged = workTaskMapper.selectById(task.getId());
        assertEquals(501L, unchanged.getAssigneeUserId());
        assertEquals(routeKey, unchanged.getResponsibilitySourceKey());
        assertEquals("501", unchanged.getCandidateUserSnapshot());
        assertEquals(task.getResponsibilityScopeJson(), unchanged.getResponsibilityScopeJson());
        verify(permissionApi, never()).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
    }

    @Test
    void reconcileProcessFormFillTaskOwnership_rejectsLockedTaskAndKeepsOwner() {
        String sourceKey = "ROUTE|5903|REPORT-OWN-003|88004";
        insertBatchTask(19003L, 39003L, 5903L, "REPORT-OWN-003", 88004L);
        MesProEdhrWorkTaskDO task = insertFillTask(39003L, 19003L, 5903L, 501L, "501")
                .setResponsibilitySourceType(FILLER_SOURCE_TYPE)
                .setResponsibilitySourceKey(sourceKey)
                .setResponsibilitySourceVersion("88004")
                .setResponsibilityScopeJson("{\"lockedEvidence\":true}")
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
        assertEquals(task.getResponsibilityScopeJson(), workTaskMapper.selectById(task.getId()).getResponsibilityScopeJson());
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
        MesProEdhrProcessFormPermissionRuleDO rule = new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(reportId)
                .setBatchRecordVersionId(versionId)
                .setRuleType("FILL")
                .setScopeKey("ALL")
                .setFillableScopeJson(ownershipFillableScope())
                .setSignatureCellKey("")
                .setCandidateSourceType("USERS")
                .setCandidateSourceIds(candidateSourceIds)
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(90)
                .setEnabled(true);
        lenient().when(processFormPermissionRuleMapper.selectEnabledFillRules(routeProcessId, reportId, versionId))
                .thenReturn(List.of(rule));
        return rule;
    }

    private AdminUserRespDTO adminUser(Long userId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }

    private String ownershipFillableScope() {
        return "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":2,\"endRow\":6}]}";
    }
}
