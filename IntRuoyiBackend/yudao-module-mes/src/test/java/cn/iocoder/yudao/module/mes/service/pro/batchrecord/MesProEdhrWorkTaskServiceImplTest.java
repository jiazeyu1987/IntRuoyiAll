package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskArchiveRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskAssignmentRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskCloseRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskReleaseApprovalRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskStatsRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementRevokeReqDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ADVANCE_PREREQUISITE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_RESPONSIBILITY_SCOPE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProEdhrWorkTaskServiceImpl.class, MesProEdhrCandidateResolver.class})
class MesProEdhrWorkTaskServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProRouteMapper routeMapper;

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
    void createInitialFillTask_skipsSpecialNoTemplateNodeAndAssignsFirstRouteForm() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3001L, 4105L);
        MesProEdhrBatchExecutionTaskDO specialTask = batchTask(3001L, 9101L, null, null,
                "INCOMING_INSPECTION_REPORT", "来料检报告", 0);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3001L, 9102L, 5101L,
                "REPORT-001", "ROUTE_FORM", "称量", 10);
        batchTaskMapper.insert(specialTask);
        batchTaskMapper.insert(routeTask);
        insertAssignmentRule(5101L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);

        workTaskService.createInitialFillTask(batch);

        List<MesProEdhrWorkTaskDO> tasks = workTaskMapper.selectList();
        assertEquals(1, tasks.size());
        MesProEdhrWorkTaskDO fillTask = tasks.get(0);
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_FILL, fillTask.getTaskType());
        assertEquals(routeTask.getId(), fillTask.getBatchTaskId());
        assertEquals(5101L, fillTask.getRouteProcessId());
        assertEquals("称量", fillTask.getProcessName());
        assertTrue(fillTask.getActionUrl().contains("/mes/pro/feedback/edhr-batch-execution/detail?id=" + batch.getId()));
        assertTrue(fillTask.getActionUrl().contains("batchTaskId=" + routeTask.getId()));
        assertTrue(fillTask.getActionUrl().contains("workTaskId=" + fillTask.getId()));

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(88L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_EDHR_FILL_TASK_ASSIGNED", notifyCaptor.getValue().getTemplateCode());
        assertEquals(fillTask.getActionUrl(), notifyCaptor.getValue().getTemplateParams().get("actionUrl"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            MesProEdhrWorkTaskPageReqVO pageReqVO = new MesProEdhrWorkTaskPageReqVO();
            pageReqVO.setPageNo(1);
            pageReqVO.setPageSize(10);
            PageResult<MesProEdhrWorkTaskRespVO> myPage = workTaskService.getMyPage(pageReqVO);
            assertEquals(1L, myPage.getTotal());
            assertEquals(fillTask.getId(), myPage.getList().get(0).getId());
            assertEquals(fillTask.getActionUrl(), myPage.getList().get(0).getActionUrl());
        }
    }

    @Test
    void getMyPage_keepsSharedProcessFormTaskVisibleToSecondCandidateAfterFirstCandidateOpensDetail() {
        MesProEdhrWorkTaskDO sharedTask = insertFillTask(2004L, 9104L, "shared-assist-candidates")
                .setAssigneeUserId(99L)
                .setCandidateSourceType("ASSIST_ROWS")
                .setCandidateUserSnapshot("99,100")
                .setResponsibilitySourceType("EDHR_PROCESS_FORM_FILLER")
                .setResponsibilitySourceKey("ROUTE|7001|REPORT-SHARED|8001")
                .setResponsibilitySourceVersion("8001")
                .setResponsibilityScopeJson("""
                        {"schemaVersion":2,"sourceType":"EDHR_PROCESS_FORM_FILLER","sourceKey":"ROUTE|7001|REPORT-SHARED|8001","sourceVersion":"8001","scopes":[
                          {"scopeKey":"AR_OPERATOR","resolvedUserIds":[99],"fillableScope":{"cells":[{"sourceTableIndex":0,"rowIndex":0,"columnIndex":1}]}},
                          {"scopeKey":"AR_REMARK","resolvedUserIds":[100],"fillableScope":{"cells":[{"sourceTableIndex":0,"rowIndex":0,"columnIndex":3}]}}
                        ]}
                        """);
        workTaskMapper.updateById(sharedTask);
        when(adminUserApi.getUserMap(Set.of(99L, 100L))).thenReturn(Map.of(
                99L, adminUser(99L, CommonStatusEnum.ENABLE.getStatus()).setNickname("员工甲"),
                100L, adminUser(100L, CommonStatusEnum.ENABLE.getStatus()).setNickname("员工乙")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertEquals(sharedTask.getId(), workTaskService.getAssignedTaskForDetail(
                    sharedTask.getId(), sharedTask.getExecutionId(), MesProEdhrWorkTaskService.TASK_TYPE_FILL).getId());
        }

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            MesProEdhrWorkTaskPageReqVO pageReqVO = new MesProEdhrWorkTaskPageReqVO();
            pageReqVO.setPageNo(1);
            pageReqVO.setPageSize(10);

            PageResult<MesProEdhrWorkTaskRespVO> myPage = workTaskService.getMyPage(pageReqVO);
            MesProEdhrWorkTaskStatsRespVO stats = workTaskService.getStats();

            assertEquals(1L, myPage.getTotal());
            assertEquals(sharedTask.getId(), myPage.getList().get(0).getId());
            assertEquals(1L, stats.getTodoCount());
            assertEquals(1L, stats.getFillCount());
        }
    }

    @Test
    void createInitialFillTask_createsAllOptionalCompanionTasksForSameProcess() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3060L, 4160L);
        MesProEdhrBatchExecutionTaskDO mainTask = batchTask(3060L, 9160L, 5160L,
                "REPORT-MAIN-OPTIONAL-COMPANION", "ROUTE_FORM", "配液", 10)
                .setBatchRecordSort(1)
                .setRequiredPolicy("REQUIRED");
        MesProEdhrBatchExecutionTaskDO lossTask = batchTask(3060L, 9161L, 5160L,
                "REPORT-LOSS-OPTIONAL-COMPANION", "ROUTE_FORM", "配液损耗单", 10)
                .setBatchRecordSort(2)
                .setRequiredFlag(false)
                .setRequiredPolicy("OPTIONAL");
        MesProEdhrBatchExecutionTaskDO inspectionTask = batchTask(3060L, 9162L, 5160L,
                "REPORT-IPQC-OPTIONAL-COMPANION", "ROUTE_FORM", "配液过程检验单", 10)
                .setBatchRecordSort(3)
                .setRequiredFlag(false)
                .setRequiredPolicy("OPTIONAL");
        batchTaskMapper.insert(mainTask);
        batchTaskMapper.insert(lossTask);
        batchTaskMapper.insert(inspectionTask);
        insertAssignmentRule(5160L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);

        workTaskService.createInitialFillTask(batch);

        List<Long> dispatchedBatchTaskIds = workTaskMapper.selectList().stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .map(MesProEdhrWorkTaskDO::getBatchTaskId)
                .sorted()
                .toList();
        assertEquals(List.of(mainTask.getId(), lossTask.getId(), inspectionTask.getId()),
                dispatchedBatchTaskIds);
    }

    @Test
    void createInitialFillTask_resolvesRoleGroupCandidateSnapshotFromAssignmentRule() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3002L, 4106L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3002L, 9103L, 5102L,
                "REPORT-002", "ROUTE_FORM", "灌装", 10);
        batchTaskMapper.insert(routeTask);
        insertAssignmentRule(5102L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120,
                "ROLE_GROUP", 7001L, 188L);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(7001L))).thenReturn(Set.of(188L, 189L));
        when(adminUserApi.getUserList(Set.of(188L, 189L))).thenReturn(List.of(
                adminUser(188L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(189L, CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUser(188L)).thenReturn(adminUser(188L, CommonStatusEnum.ENABLE.getStatus()));

        workTaskService.createInitialFillTask(batch);

        MesProEdhrWorkTaskDO fillTask = workTaskMapper.selectList().get(0);
        assertEquals("ROLE_GROUP", fillTask.getCandidateSourceType());
        assertEquals(7001L, fillTask.getCandidateSourceId());
        assertEquals("188,189", fillTask.getCandidateUserSnapshot());
    }

    @Test
    void createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3020L, 4120L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3020L, 9120L, 5120L,
                "REPORT-PERM-001", "ROUTE_FORM", "光固I", 10)
                .setBatchRecordVersionId(78020L);
        batchTaskMapper.insert(routeTask);
        insertProcessFormFillRule(5120L, "REPORT-PERM-001", 78020L, "USERS", "288,289", 180);
        when(adminUserApi.getUserList(List.of(288L, 289L))).thenReturn(List.of(
                adminUser(288L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(289L, CommonStatusEnum.ENABLE.getStatus())));

        workTaskService.createInitialFillTask(batch);

        MesProEdhrWorkTaskDO fillTask = workTaskMapper.selectList().get(0);
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_FILL, fillTask.getTaskType());
        assertEquals(routeTask.getId(), fillTask.getBatchTaskId());
        assertEquals(5120L, fillTask.getRouteProcessId());
        assertEquals("USERS", fillTask.getCandidateSourceType());
        assertNull(fillTask.getCandidateSourceId());
        assertEquals("288,289", fillTask.getCandidateUserSnapshot());
        assertEquals(288L, fillTask.getAssigneeUserId());
        assertEquals("EDHR_PROCESS_FORM_FILLER", fillTask.getResponsibilitySourceType());
        assertEquals("ROUTE|5120|REPORT-PERM-001|78020", fillTask.getResponsibilitySourceKey());
        assertEquals("78020", fillTask.getResponsibilitySourceVersion());
        assertFalse(Boolean.TRUE.equals(fillTask.getOwnershipLocked()));

        ArgumentCaptor<SystemEntitlementSyncReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(captor.capture());
        SystemEntitlementSyncReqDTO request = captor.getValue();
        assertEquals(122L, request.getTenantId());
        assertEquals("EDHR_WORK_TASK_ASSIGNEE", request.getSourceType());
        assertEquals("WORK_TASK|" + fillTask.getId(), request.getSourceKey());
        assertEquals("MES_EDHR_FILLER_MINIMAL", request.getPolicyCode());
        assertEquals(Set.of(288L, 289L), request.getResolvedUserIds());
        assertTrue(request.getSourceDigest().contains("responsibilitySourceKey=ROUTE|5120|REPORT-PERM-001|78020"));
    }

    @Test
    void createInitialFillTask_freezesAssistRowResponsibilityScopeAndCandidateUnion() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3025L, 4125L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3025L, 9125L, 5125L,
                "REPORT-ASSIST-SCOPE", "ROUTE_FORM", "光固II", 10)
                .setBatchRecordVersionId(78025L);
        batchTaskMapper.insert(routeTask);
        insertProcessFormFillRule(5125L, "REPORT-ASSIST-SCOPE", 78025L,
                "AR_001", "USERS", "288", 180,
                preciseCellScope("AR_001", 0, 1, 2));
        insertProcessFormFillRule(5125L, "REPORT-ASSIST-SCOPE", 78025L,
                "AR_002", "ROLE", "7001", 180,
                preciseCellScope("AR_002", 0, 1, 4));
        when(adminUserApi.getUserList(List.of(288L))).thenReturn(List.of(
                adminUser(288L, CommonStatusEnum.ENABLE.getStatus())));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(7001L))).thenReturn(Set.of(289L, 290L));
        when(adminUserApi.getUserList(Set.of(289L, 290L))).thenReturn(List.of(
                adminUser(289L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(290L, CommonStatusEnum.ENABLE.getStatus())));

        workTaskService.createInitialFillTask(batch);

        List<MesProEdhrWorkTaskDO> tasks = workTaskMapper.selectList();
        assertEquals(1, tasks.size());
        MesProEdhrWorkTaskDO fillTask = tasks.get(0);
        assertEquals(routeTask.getId(), fillTask.getBatchTaskId());
        assertEquals("288,289,290", fillTask.getCandidateUserSnapshot());
        assertEquals(288L, fillTask.getAssigneeUserId());
        assertEquals("ASSIST_ROWS", fillTask.getCandidateSourceType());
        assertNotNull(fillTask.getResponsibilityScopeJson());
        JSONObject snapshot = JSON.parseObject(fillTask.getResponsibilityScopeJson());
        assertEquals(2, snapshot.getIntValue("schemaVersion"));
        JSONArray scopes = snapshot.getJSONArray("scopes");
        assertEquals(2, scopes.size());
        JSONObject firstScope = scopes.getJSONObject(0);
        assertEquals("AR_001", firstScope.getString("scopeKey"));
        assertEquals(List.of(288), firstScope.getJSONArray("resolvedUserIds").toJavaList(Integer.class));
        assertEquals(2, firstScope.getJSONObject("fillableScope").getJSONArray("cells")
                .getJSONObject(0).getIntValue("columnIndex"));
        JSONObject secondScope = scopes.getJSONObject(1);
        assertEquals("AR_002", secondScope.getString("scopeKey"));
        assertEquals(List.of(289, 290), secondScope.getJSONArray("resolvedUserIds").toJavaList(Integer.class));
        assertEquals(4, secondScope.getJSONObject("fillableScope").getJSONArray("cells")
                .getJSONObject(0).getIntValue("columnIndex"));

        when(permissionApi.getUserRoleIdListByRoleIds(List.of(7001L))).thenReturn(Set.of(291L));
        MesProEdhrWorkTaskDO frozenTask = workTaskMapper.selectById(fillTask.getId());
        assertEquals("288,289,290", frozenTask.getCandidateUserSnapshot());
        assertEquals(fillTask.getResponsibilityScopeJson(), frozenTask.getResponsibilityScopeJson());
    }

    @Test
    void createInitialFillTask_usesRouteVersionedFormBindingRuleForDynamicRouteFormSlot() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3023L, 4123L)
                .setRouteVersionId(8123L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3023L, 9123L, 5123L,
                null, "ROUTE_FORM", "动态过程单", 10)
                .setFormBindingKey("FB-SLOT-FILLER")
                .setFormTemplateVersionId(7123L)
                .setFillableScopeJson(routeFormRangeScopeForTables(100, 0, 99999));
        insertBatch(batch);
        batchTaskMapper.insert(routeTask);
        insertProcessFormFillRule(5123L, "FB-SLOT-FILLER", 8123L,
                "ALL", "USERS", "388,389", 180, null);
        when(adminUserApi.getUserList(List.of(388L, 389L))).thenReturn(List.of(
                adminUser(388L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(389L, CommonStatusEnum.ENABLE.getStatus())));

        workTaskService.createInitialFillTask(batch);

        MesProEdhrWorkTaskDO fillTask = workTaskMapper.selectList().get(0);
        assertEquals(routeTask.getId(), fillTask.getBatchTaskId());
        assertEquals(388L, fillTask.getAssigneeUserId());
        assertEquals("USERS", fillTask.getCandidateSourceType());
        assertEquals("388,389", fillTask.getCandidateUserSnapshot());
        assertEquals("EDHR_PROCESS_FORM_FILLER", fillTask.getResponsibilitySourceType());
        assertEquals("ROUTE|5123|FB-SLOT-FILLER|8123", fillTask.getResponsibilitySourceKey());
        assertEquals("8123", fillTask.getResponsibilitySourceVersion());
        JSONObject snapshot = JSON.parseObject(fillTask.getResponsibilityScopeJson());
        JSONObject fillableScope = snapshot.getJSONArray("scopes").getJSONObject(0)
                .getJSONObject("fillableScope");
        assertEquals(100, fillableScope.getJSONArray("ranges").size());
        assertEquals(99999, fillableScope.getJSONArray("ranges").getJSONObject(0).getIntValue("endRow"));
        assertTrue(fillTask.getResponsibilitySourceDigest().matches("scopes-sha256=[0-9a-f]{64}"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(388L);
            assertEquals(fillTask.getId(), workTaskService.getAssignedTaskForDetail(
                    fillTask.getId(), fillTask.getExecutionId(), MesProEdhrWorkTaskService.TASK_TYPE_FILL).getId());
        }
    }

    @Test
    void createInitialFillTask_usesRouteVersionedRuleWhenFormCenterBindingStoredAsReportId() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3024L, 4124L)
                .setRouteVersionId(8124L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3024L, 9124L, 5124L,
                "FB-SLOT-FILLER-REPORT", "ROUTE_FORM", "动态过程单", 10)
                .setFormBindingKey("FB-SLOT-FILLER-REPORT")
                .setFormTemplateVersionId(7124L)
                .setFillableScopeJson(routeFormRangeScope(0, 0, 99999));
        insertBatch(batch);
        batchTaskMapper.insert(routeTask);
        insertProcessFormFillRule(5124L, "FB-SLOT-FILLER-REPORT", 8124L,
                "ALL", "USERS", "488,489", 180, null);
        when(adminUserApi.getUserList(List.of(488L, 489L))).thenReturn(List.of(
                adminUser(488L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(489L, CommonStatusEnum.ENABLE.getStatus())));

        workTaskService.createInitialFillTask(batch);

        MesProEdhrWorkTaskDO fillTask = workTaskMapper.selectList().get(0);
        assertEquals(routeTask.getId(), fillTask.getBatchTaskId());
        assertEquals(488L, fillTask.getAssigneeUserId());
        assertEquals("USERS", fillTask.getCandidateSourceType());
        assertEquals("EDHR_PROCESS_FORM_FILLER", fillTask.getResponsibilitySourceType());
        assertEquals("ROUTE|5124|FB-SLOT-FILLER-REPORT|8124", fillTask.getResponsibilitySourceKey());
        assertEquals("8124", fillTask.getResponsibilitySourceVersion());
    }

    @Test
    void createInitialFillTask_usesVersionIndependentProcessFormPermissionRuleForVersionedBatchTask() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3022L, 4122L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3022L, 9122L, 5122L,
                "REPORT-PERM-LATEST", "ROUTE_FORM", "粗洗", 10)
                .setBatchRecordVersionId(7901L);
        batchTaskMapper.insert(routeTask);
        when(routeProcessService.resolveFrozenRouteProcess(5122L, 4122L, 5122L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(5122L)
                        .routeId(4122L)
                        .processId(5122L)
                        .build());
        insertProcessFormFillRuleWithoutVersion(5122L, "REPORT-PERM-LATEST", "USERS", "149", 180);
        when(adminUserApi.getUserList(List.of(149L))).thenReturn(List.of(
                adminUser(149L, CommonStatusEnum.ENABLE.getStatus())));

        assertThrows(ServiceException.class, () -> workTaskService.createInitialFillTask(batch));
        assertTrue(workTaskMapper.selectList().isEmpty());
    }

    @Test
    void createInitialFillTask_skipsTodoAssignmentWhenBatchRecordBindingHasNoPermissionCandidate() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3021L, 4121L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3021L, 9121L, 5121L,
                "REPORT-BINDING-ONLY", "ROUTE_FORM", "清洗", 10);
        batchTaskMapper.insert(routeTask);

        workTaskService.createInitialFillTask(batch);

        assertTrue(workTaskMapper.selectList().isEmpty());
    }

    @Test
    void createInitialFillTask_prefersExplicitAssigneeInsideRoleGroupCandidateSnapshot() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3031L, 4131L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3031L, 9131L, 5131L,
                "REPORT-ROLE-ASSIGNEE", "ROUTE_FORM", "精检", 10);
        batchTaskMapper.insert(routeTask);
        insertAssignmentRule(5131L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120,
                "ROLE_GROUP", 7002L, 189L);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(7002L))).thenReturn(Set.of(188L, 189L));
        when(adminUserApi.getUserList(Set.of(188L, 189L))).thenReturn(List.of(
                adminUser(188L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(189L, CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUser(189L)).thenReturn(adminUser(189L, CommonStatusEnum.ENABLE.getStatus()));

        workTaskService.createInitialFillTask(batch);

        MesProEdhrWorkTaskDO fillTask = workTaskMapper.selectList().get(0);
        assertEquals(189L, fillTask.getAssigneeUserId());
        assertEquals("188,189", fillTask.getCandidateUserSnapshot());
    }

    @Test
    void createInitialFillTask_failsFastWhenExplicitAssigneeIsOutsideRoleGroupCandidateSnapshot() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3032L, 4132L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3032L, 9132L, 5132L,
                "REPORT-ROLE-INVALID", "ROUTE_FORM", "终检", 10);
        batchTaskMapper.insert(routeTask);
        insertAssignmentRule(5132L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120,
                "ROLE_GROUP", 7003L, 199L);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(7003L))).thenReturn(Set.of(188L, 189L));
        when(adminUserApi.getUserList(Set.of(188L, 189L))).thenReturn(List.of(
                adminUser(188L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(189L, CommonStatusEnum.ENABLE.getStatus())));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.createInitialFillTask(batch));

        assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID.getCode(), exception.getCode());
        assertTrue(workTaskMapper.selectList().isEmpty());
    }

    @Test
    void createInitialFillTask_failsFastWhenDeptGroupCandidatePoolIsEmpty() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3003L, 4107L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3003L, 9104L, 5103L,
                "REPORT-003", "ROUTE_FORM", "包装", 10);
        batchTaskMapper.insert(routeTask);
        insertAssignmentRule(5103L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120,
                "DEPT_GROUP", 8001L);
        when(adminUserApi.getUserListByDeptIds(Set.of(8001L))).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.createInitialFillTask(batch));

        assertEquals(PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY.getCode(), exception.getCode());
        assertTrue(workTaskMapper.selectList().isEmpty());
    }

    @Test
    void createInitialFillTask_failsFastWhenUserCandidateDoesNotExist() {
        MesProEdhrBatchExecutionDO batch = batchForInitialFill(3005L, 4108L);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3005L, 9106L, 5105L,
                "REPORT-004", "ROUTE_FORM", "称量复核", 10);
        batchTaskMapper.insert(routeTask);
        insertAssignmentRule(5105L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120,
                "USER", 7777L, 7777L);
        when(adminUserApi.getUser(7777L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.createInitialFillTask(batch));

        assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID.getCode(), exception.getCode());
        assertTrue(workTaskMapper.selectList().isEmpty());
    }

    @Test
    void createInitialFillTask_missingAdvancePrerequisitesFailsWithFullList() {
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(3004L)
                .setWorkOrderId(null)
                .setWorkOrderCode("")
                .setBatchCode("")
                .setProductId(null)
                .setRouteId(null);
        MesProEdhrBatchExecutionTaskDO routeTask = batchTask(3004L, 9105L, 5104L,
                null, "ROUTE_FORM", "", 10)
                .setProcessId(null);
        batchTaskMapper.insert(routeTask);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.createInitialFillTask(batch));

        assertEquals(PRO_EDHR_WORK_TASK_ADVANCE_PREREQUISITE_MISSING.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("工单"));
        assertTrue(exception.getMessage().contains("批次"));
        assertTrue(exception.getMessage().contains("产品"));
        assertTrue(exception.getMessage().contains("路线"));
        assertTrue(exception.getMessage().contains("工序"));
        assertTrue(exception.getMessage().contains("批记录绑定"));
        assertTrue(exception.getMessage().contains("权限"));
        assertTrue(workTaskMapper.selectList().isEmpty());
    }

    @Test
    void createNextFillAfterReview_missingSignatureCellFailsBeforeCreatingNextTask() {
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3010L, 9110L, 5110L,
                "REPORT-CURRENT", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3010L, 9111L, 5111L,
                "REPORT-NEXT", "ROUTE_FORM", "包装", 20);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(nextTask);
        insertAssignmentRule(5111L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(8010L, currentTask.getId(), "",
                99L, "99", "missing-signature")
                .setBatchExecutionId(3010L)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setRouteId(4101L);
        workTaskMapper.updateById(reviewTask);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.createNextFillAfterReview(reviewTask));
            assertEquals(PRO_EDHR_WORK_TASK_ADVANCE_PREREQUISITE_MISSING.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("签名位"));
        }

        assertEquals(1, workTaskMapper.selectList().size());
    }

    @Test
    void validateWritableFillTaskForExecution_allowsCandidatePoolMemberAndRejectsOutsider() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(2001L, 9101L, "candidate-fill")
                .setCandidateSourceType("ROLE_GROUP")
                .setCandidateSourceId(7001L)
                .setCandidateUserSnapshot("99,100");
        workTaskMapper.updateById(fillTask);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            MesProEdhrWorkTaskDO validated =
                    workTaskService.validateWritableFillTaskForExecution(fillTask.getId(), 2001L);
            assertEquals(fillTask.getId(), validated.getId());
        }

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.validateWritableFillTaskForExecution(fillTask.getId(), 2001L));
            assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH.getCode(), exception.getCode());
        }
    }

    @Test
    void validateWritableFillTaskForExecution_rejectsFormerOwnerHistoricalActionUrlAfterReassignment() {
        MesProEdhrWorkTaskDO transferredTask = insertFillTask(2003L, 9103L, "former-owner-action-url")
                .setAssigneeUserId(389L)
                .setCandidateSourceType("USERS")
                .setCandidateUserSnapshot("389,390")
                .setSourceUserId(288L);
        transferredTask.setActionUrl("/mes/pro/feedback/edhr-execution/form?id=2003"
                + "&executionId=2003&workTaskId=" + transferredTask.getId());
        workTaskMapper.updateById(transferredTask);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(288L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.validateWritableFillTaskForExecution(transferredTask.getId(), 2003L));
            assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH.getCode(), exception.getCode());
        }

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(389L);
            MesProEdhrWorkTaskDO validated =
                    workTaskService.validateWritableFillTaskForExecution(transferredTask.getId(), 2003L);
            assertEquals(transferredTask.getId(), validated.getId());
        }
    }

    @Test
    void getCandidateSignatureTodoPage_returnsResponsibilitySourceCandidatePoolAndInactionReason() {
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(9001L, 9901L, "R1C1",
                177L, "188,189", "candidate-readable")
                .setCandidateSourceType("ROLE_GROUP")
                .setCandidateSourceId(7001L)
                .setSourceUserId(166L);
        workTaskMapper.updateById(reviewTask);
        when(adminUserApi.getUserMap(Set.of(166L, 177L, 188L, 189L))).thenReturn(java.util.Map.of(
                166L, adminUser(166L, CommonStatusEnum.ENABLE.getStatus()).setNickname("提交人甲"),
                177L, adminUser(177L, CommonStatusEnum.ENABLE.getStatus()).setNickname("主审核人丁"),
                188L, adminUser(188L, CommonStatusEnum.ENABLE.getStatus()).setNickname("审核人乙"),
                189L, adminUser(189L, CommonStatusEnum.ENABLE.getStatus()).setNickname("审核人丙")));
        when(adminUserApi.getUserList(Set.of(166L, 177L))).thenReturn(List.of(
                adminUser(166L, CommonStatusEnum.ENABLE.getStatus()).setNickname("提交人甲"),
                adminUser(177L, CommonStatusEnum.ENABLE.getStatus()).setNickname("主审核人丁")));
        RoleRespDTO role = new RoleRespDTO();
        role.setId(7001L);
        role.setName("QA 复核组");
        when(roleApi.getRoleList(Set.of(7001L))).thenReturn(List.of(role));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(189L);
            var page = workTaskService.getCandidateSignatureTodoPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO());
            assertEquals(1, page.getList().size());
            var row = page.getList().get(0);
            assertEquals("候选审核池派发", row.getResponsibilitySource());
            assertEquals("QA 复核组", row.getCandidatePoolName());
            assertEquals("提交人甲", row.getSourceUserName());
            assertEquals("审核人乙(188)，审核人丙(189)", row.getCandidateSnapshotDisplay());
            assertEquals("当前用户在候选池中，需按候选审核路径处理", row.getInactionReason());
        }
    }

    @Test
    void getApprovalCenterTimelineTasks_skipsParticipantCheckWhenGlobalViewEnabled() {
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(9002L, 9902L, "R1C2",
                177L, "188,189", "approval-center-global")
                .setSourceUserId(166L);
        workTaskMapper.updateById(reviewTask);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(999L);
            List<MesProEdhrWorkTaskDO> timeline = workTaskService.getApprovalCenterTimelineTasks(
                    reviewTask.getId(), reviewTask.getExecutionId(), true);
            assertEquals(1, timeline.size());
            assertEquals(reviewTask.getId(), timeline.get(0).getId());
        }
    }

    @Test
    void validateWritableFillTaskForExecution_allowsReworkTaskForRevisionDraft() {
        MesProEdhrWorkTaskDO reworkTask = insertFillTask(2002L, 9102L, "rework-revision")
                .setTaskCode("EDHRT-REWORK-rework-revision")
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REWORK)
                .setSourceExecutionId(2001L)
                .setActionUrl("/mes/pro/feedback/edhr-execution/detail?id=2002&workTaskId=9102");
        workTaskMapper.updateById(reworkTask);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MesProEdhrWorkTaskDO validated =
                    workTaskService.validateWritableFillTaskForExecution(reworkTask.getId(), 2002L);
            assertEquals(reworkTask.getId(), validated.getId());
            assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_REWORK, validated.getTaskType());
        }
    }

    @Test
    void createNextFillAfterReview_pendingPeerReviewDoesNotCreateNextFill() {
        insertBatch(batchForInitialFill(3020L, 4120L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3020L, 9120L, 5120L,
                "REPORT-CURRENT", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3020L, 9121L, 5121L,
                "REPORT-NEXT", "ROUTE_FORM", "包装", 20);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(nextTask);
        insertAssignmentRule(5121L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);
        MesProEdhrWorkTaskDO completedReview = insertCandidateReviewTask(8020L, currentTask.getId(), "R1C1",
                99L, "99,100", "review-done")
                .setBatchExecutionId(3020L)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-INITIAL")
                .setBatchCode("BATCH-INITIAL")
                .setRouteId(4120L)
                .setStatus(MesProEdhrWorkTaskStatus.DONE);
        workTaskMapper.updateById(completedReview);
        MesProEdhrWorkTaskDO pendingReview = insertCandidateReviewTask(8020L, currentTask.getId(), "R1C2",
                100L, "99,100", "review-pending")
                .setBatchExecutionId(3020L)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-INITIAL")
                .setBatchCode("BATCH-INITIAL")
                .setRouteId(4120L);
        workTaskMapper.updateById(pendingReview);

        workTaskService.createNextFillAfterReview(completedReview);

        assertTrue(workTaskMapper.selectActiveListByExecutionAndType(8020L,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL).isEmpty());
    }

    @Test
    void createApproveTaskAfterAllReviewsDone_createsIndependentApproveTaskAndDoesNotCreateNextFill() {
        insertBatch(batchForInitialFill(3028L, 4128L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3028L, 9128L, 5128L,
                "REPORT-CURRENT", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3028L, 9129L, 5129L,
                "REPORT-NEXT", "ROUTE_FORM", "包装", 20);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(nextTask);
        insertAssignmentRule(5128L, MesProEdhrWorkTaskService.TASK_TYPE_APPROVE, 120);
        insertAssignmentRule(5129L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);
        MesProEdhrWorkTaskDO completedReview = completedReviewTask(8028L, currentTask, 4128L);

        MesProEdhrWorkTaskDO approveTask;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(77L);
            approveTask = workTaskService.createApproveTaskAfterReview(completedReview);
        }

        assertNotNull(approveTask);
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_APPROVE, approveTask.getTaskType());
        assertEquals(8028L, approveTask.getExecutionId());
        assertEquals(3028L, approveTask.getBatchExecutionId());
        assertEquals(currentTask.getId(), approveTask.getBatchTaskId());
        assertEquals(88L, approveTask.getAssigneeUserId());
        assertEquals("/mes/pro/feedback/edhr-approval/detail?id=8028&workTaskId=" + approveTask.getId(),
                approveTask.getActionUrl());
        assertTrue(workTaskMapper.selectActiveListByExecutionAndType(8028L,
                        MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .stream()
                .noneMatch(task -> nextTask.getId().equals(task.getBatchTaskId())));

        ArgumentCaptor<SystemEntitlementSyncReqDTO> syncCaptor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(syncCaptor.capture());
        assertEquals("MES_EDHR_APPROVAL_REVIEWER_MINIMAL", syncCaptor.getValue().getPolicyCode());
        assertEquals("WORK_TASK|" + approveTask.getId(), syncCaptor.getValue().getSourceKey());
        assertEquals(Set.of(88L), syncCaptor.getValue().getResolvedUserIds());
    }

    @Test
    void completeFillAndCreateNextFillAfterOrdinarySubmit_doesNotRequireReviewRule() throws Exception {
        insertBatch(batchForInitialFill(3055L, 4155L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3055L, 9155L, 5155L,
                "REPORT-CURRENT-ORDINARY", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SUBMITTED);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3055L, 9156L, 5156L,
                "REPORT-NEXT-ORDINARY", "ROUTE_FORM", "包装", 20)
                .setBatchRecordVersionId(78056L);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(nextTask);
        when(routeProcessService.resolveFrozenRouteProcess(5156L, 4155L, 5156L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(5156L)
                        .routeId(4155L)
                        .processId(5156L)
                        .build());
        insertProcessFormFillRule(5156L, "REPORT-NEXT-ORDINARY", 78056L, "USERS", "288,289", 180);
        when(adminUserApi.getUserList(List.of(288L, 289L))).thenReturn(List.of(
                adminUser(288L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(289L, CommonStatusEnum.ENABLE.getStatus())));
        MesProEdhrWorkTaskDO fillTask = insertFillTask(8055L, currentTask.getId(), "ordinary-submit")
                .setBatchExecutionId(3055L)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-INITIAL")
                .setBatchCode("BATCH-INITIAL")
                .setRouteId(4155L)
                .setRouteProcessId(currentTask.getRouteProcessId())
                .setProcessId(currentTask.getProcessId())
                .setProcessName(currentTask.getProcessName());
        workTaskMapper.updateById(fillTask);
        java.lang.reflect.Method method = MesProEdhrWorkTaskService.class.getMethod(
                "completeFillAndCreateNextFillAfterOrdinarySubmit", Long.class, Long.class);

        MesProEdhrWorkTaskDO completed;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            completed = (MesProEdhrWorkTaskDO) method.invoke(workTaskService, fillTask.getId(), 8055L);
        }

        assertEquals(fillTask.getId(), completed.getId());
        assertEquals(MesProEdhrWorkTaskStatus.DONE, workTaskMapper.selectById(fillTask.getId()).getStatus());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED,
                batchTaskMapper.selectById(currentTask.getId()).getStatus());
        List<MesProEdhrWorkTaskDO> nextFills = workTaskMapper.selectList().stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .filter(task -> nextTask.getId().equals(task.getBatchTaskId()))
                .toList();
        assertEquals(1, nextFills.size());
        assertEquals(288L, nextFills.get(0).getAssigneeUserId());
        assertTrue(workTaskMapper.selectActiveListByExecutionAndType(8055L,
                MesProEdhrWorkTaskService.TASK_TYPE_REVIEW).isEmpty());
        assertTrue(workTaskMapper.selectActiveListByExecutionAndType(8055L,
                MesProEdhrWorkTaskService.TASK_TYPE_APPROVE).isEmpty());
    }

    @Test
    void completeRouteFormFillAndCreateNextFill_marksFormCenterFillDoneAndCreatesNextFill() {
        insertBatch(batchForInitialFill(3065L, 4165L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3065L, 9165L, 5165L,
                "REPORT-FORMCENTER-CURRENT", "ROUTE_FORM", "粗洗损耗", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                .setFormSlotType("LOSS_REPORT")
                .setFormBindingKey("EDHR_RF_LOSS")
                .setFormTemplateId(25L)
                .setFormTemplateVersionId(2501L)
                .setFormTemplateVersionNo("V1")
                .setFormCenterInstanceId(40065L);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3065L, 9166L, 5166L,
                "REPORT-FORMCENTER-NEXT", "ROUTE_FORM", "精洗", 20)
                .setBatchRecordVersionId(78066L);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(nextTask);
        when(routeProcessService.resolveFrozenRouteProcess(5166L, 4165L, 5166L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(5166L)
                        .routeId(4165L)
                        .processId(5166L)
                        .build());
        insertProcessFormFillRule(5166L, "REPORT-FORMCENTER-NEXT", 78066L, "USERS", "288,289", 180);
        when(adminUserApi.getUserList(List.of(288L, 289L))).thenReturn(List.of(
                adminUser(288L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(289L, CommonStatusEnum.ENABLE.getStatus())));
        MesProEdhrWorkTaskDO fillTask = insertFillTask(8065L, currentTask.getId(), "formcenter-submit")
                .setBatchExecutionId(3065L)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-FORMCENTER")
                .setBatchCode("BATCH-FORMCENTER")
                .setRouteId(4165L)
                .setRouteProcessId(currentTask.getRouteProcessId())
                .setProcessId(currentTask.getProcessId())
                .setProcessName(currentTask.getProcessName())
                .setExecutionId(null);
        workTaskMapper.updateById(fillTask);

        MesProEdhrWorkTaskDO completed = workTaskService.completeRouteFormFillAndCreateNextFill(currentTask.getId(), 99L);

        assertEquals(fillTask.getId(), completed.getId());
        MesProEdhrWorkTaskDO completedTask = workTaskMapper.selectById(fillTask.getId());
        assertEquals(MesProEdhrWorkTaskStatus.DONE, completedTask.getStatus());
        assertEquals("FORM_CENTER_SUBMIT:表单中心路线表单提交", completedTask.getReason());
        MesProEdhrBatchExecutionTaskDO approvedTask = batchTaskMapper.selectById(currentTask.getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED, approvedTask.getStatus());
        assertEquals(99L, approvedTask.getOpenedBy());
        assertNotNull(approvedTask.getSubmittedAt());
        assertNotNull(approvedTask.getApprovedAt());
        List<MesProEdhrWorkTaskDO> nextFills = workTaskMapper.selectList().stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .filter(task -> nextTask.getId().equals(task.getBatchTaskId()))
                .toList();
        assertEquals(1, nextFills.size());
        assertEquals(288L, nextFills.get(0).getAssigneeUserId());
    }

    @Test
    void validateWritableApproveTask_rejectsReviewTaskAndOutsider() {
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(8029L, 9129L, "R1C1",
                188L, "188", "approve-wrong-type");
        MesProEdhrWorkTaskDO approveTask = insertApproveTask(8029L, 9129L, "R1C1",
                199L, "approve-owner");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            ServiceException wrongType = assertThrows(ServiceException.class,
                    () -> workTaskService.validateWritableApproveTask(reviewTask.getId(), 8029L));
            assertEquals(PRO_EDHR_WORK_TASK_STATUS_INVALID.getCode(), wrongType.getCode());

            ServiceException outsider = assertThrows(ServiceException.class,
                    () -> workTaskService.validateWritableApproveTask(approveTask.getId(), 8029L));
            assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH.getCode(), outsider.getCode());

            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(199L);
            MesProEdhrWorkTaskDO writable = workTaskService.validateWritableApproveTask(approveTask.getId(), 8029L);
            assertEquals(approveTask.getId(), writable.getId());
        }
    }

    @Test
    void selectActiveReviewByExecutionAndBpmTaskId_returnsFirstActiveSignatureCellWhenOneBpmTaskHasMultipleCells() {
        MesProEdhrWorkTaskDO first = insertCandidateReviewTask(8025L, 9125L, "R5C18",
                916L, "916", "review-multi-cell-first")
                .setBpmTaskId("bpm-task-8025");
        workTaskMapper.updateById(first);
        MesProEdhrWorkTaskDO second = insertCandidateReviewTask(8025L, 9125L, "R19C18",
                916L, "916", "review-multi-cell-second")
                .setBpmTaskId("bpm-task-8025");
        workTaskMapper.updateById(second);

        MesProEdhrWorkTaskDO selected =
                workTaskMapper.selectActiveReviewByExecutionAndBpmTaskId(8025L, "bpm-task-8025");

        assertEquals(first.getId(), selected.getId());
        assertEquals("R5C18", selected.getSignatureCellKey());
    }

    @Test
    void createNextFillAfterReview_sameProcessRecordWaitingCreatesPeerBeforeNextProcessFill() {
        insertBatch(batchForInitialFill(3030L, 4130L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3030L, 9130L, 5130L,
                "REPORT-CURRENT", "ROUTE_FORM", "批记录表单", 10)
                .setExecutionMode("PARALLEL")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO specialTask = batchTask(3030L, 9129L, null,
                null, "INCOMING_INSPECTION_REPORT", "来料检报告", 0)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        MesProEdhrBatchExecutionTaskDO peerTask = batchTask(3030L, 9131L, 5130L,
                "REPORT-PEER", "ROUTE_FORM", "损耗单", 10)
                .setExecutionMode("PARALLEL")
                .setBatchRecordSort(1)
                .setBatchRecordVersionId(78030L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3030L, 9132L, 5133L,
                "REPORT-NEXT", "ROUTE_FORM", "包装", 20);
        batchTaskMapper.insert(specialTask);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(peerTask);
        batchTaskMapper.insert(nextTask);
        when(routeProcessService.resolveFrozenRouteProcess(5130L, 4130L, 5130L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(5130L)
                        .routeId(4130L)
                        .processId(5130L)
                        .build());
        insertProcessFormFillRule(5130L, "REPORT-PEER", 78030L, "USERS", "188,189", 120);
        insertAssignmentRule(5133L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);
        when(adminUserApi.getUserList(List.of(188L, 189L))).thenReturn(List.of(
                adminUser(188L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(189L, CommonStatusEnum.ENABLE.getStatus())));
        MesProEdhrWorkTaskDO reviewTask = completedReviewTask(8030L, currentTask, 4130L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            workTaskService.createNextFillAfterReview(reviewTask);
        }

        List<MesProEdhrWorkTaskDO> fills = workTaskMapper.selectList().stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .toList();
        assertEquals(1, fills.size());
        assertEquals(peerTask.getId(), fills.get(0).getBatchTaskId());
        assertEquals(188L, fills.get(0).getAssigneeUserId());
        assertTrue(fills.stream().noneMatch(task -> nextTask.getId().equals(task.getBatchTaskId())));
    }

    @Test
    void createNextFillAfterReview_specialNodeWaitingDoesNotCreateNextFill() {
        insertBatch(batchForInitialFill(3040L, 4140L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3040L, 9140L, 5140L,
                "REPORT-CURRENT", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO specialTask = batchTask(3040L, 9141L, null,
                null, "STERILIZATION_REPORT", "灭菌报告", 15)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3040L, 9142L, 5142L,
                "REPORT-NEXT", "ROUTE_FORM", "包装", 20);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(specialTask);
        batchTaskMapper.insert(nextTask);
        insertAssignmentRule(5142L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);
        MesProEdhrWorkTaskDO reviewTask = completedReviewTask(8040L, currentTask, 4140L);

        workTaskService.createNextFillAfterReview(reviewTask);

        assertTrue(workTaskMapper.selectActiveListByExecutionAndType(8040L,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL).isEmpty());
    }

    @Test
    void createNextFillAfterSpecialNodeResolved_skippedSpecialDispatchesNextFillOnce() {
        insertBatch(batchForInitialFill(3045L, 4145L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3045L, 9145L, 5145L,
                "REPORT-CURRENT", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO specialTask = batchTask(3045L, 9146L, null,
                null, "INCOMING_INSPECTION_REPORT", "来料检报告", 15)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED)
                .setSkippedBy(99L)
                .setSkippedAt(LocalDateTime.of(2026, 7, 22, 10, 15));
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3045L, 9147L, 5147L,
                "REPORT-NEXT", "ROUTE_FORM", "包装", 20);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(specialTask);
        batchTaskMapper.insert(nextTask);
        insertAssignmentRule(5147L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            workTaskService.createNextFillAfterSpecialNodeResolved(specialTask);
            workTaskService.createNextFillAfterSpecialNodeResolved(specialTask);
        }

        List<MesProEdhrWorkTaskDO> fills = workTaskMapper.selectList().stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .filter(task -> nextTask.getId().equals(task.getBatchTaskId()))
                .toList();
        assertEquals(1, fills.size());
        assertNotNull(fills.get(0).getAssigneeUserId());
    }

    @Test
    void createNextFillAfterReview_allPrerequisitesMetCreatesNextFillOnce() {
        insertBatch(batchForInitialFill(3050L, 4150L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3050L, 9150L, 5150L,
                "REPORT-CURRENT", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO specialTask = batchTask(3050L, 9151L, null,
                null, "STERILIZATION_REPORT", "灭菌报告", 15)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED);
        MesProEdhrBatchExecutionTaskDO nextTask = batchTask(3050L, 9152L, 5152L,
                "REPORT-NEXT", "ROUTE_FORM", "包装", 20);
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(specialTask);
        batchTaskMapper.insert(nextTask);
        insertAssignmentRule(5152L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);
        MesProEdhrWorkTaskDO reviewTask = completedReviewTask(8050L, currentTask, 4150L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            workTaskService.createNextFillAfterReview(reviewTask);
            workTaskService.createNextFillAfterReview(reviewTask);
        }

        List<MesProEdhrWorkTaskDO> fills = workTaskMapper.selectList().stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .filter(task -> nextTask.getId().equals(task.getBatchTaskId()))
                .toList();
        assertEquals(1, fills.size());
        assertEquals(nextTask.getId(), fills.get(0).getBatchTaskId());
    }

    @Test
    void createNextFillAfterReview_createsOptionalCompanionsForNextProcess() {
        insertBatch(batchForInitialFill(3061L, 4161L));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTask(3061L, 9163L, 5161L,
                "REPORT-CURRENT-COMPANION", "ROUTE_FORM", "称量", 10)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO nextMainTask = batchTask(3061L, 9164L, 5162L,
                "REPORT-NEXT-MAIN-COMPANION", "ROUTE_FORM", "包装", 20)
                .setBatchRecordSort(1)
                .setRequiredPolicy("REQUIRED");
        MesProEdhrBatchExecutionTaskDO nextOptionalTask = batchTask(3061L, 9165L, 5162L,
                "REPORT-NEXT-OPTIONAL-COMPANION", "ROUTE_FORM", "包装过程检验单", 20)
                .setBatchRecordSort(2)
                .setRequiredFlag(false)
                .setRequiredPolicy("OPTIONAL");
        batchTaskMapper.insert(currentTask);
        batchTaskMapper.insert(nextMainTask);
        batchTaskMapper.insert(nextOptionalTask);
        insertAssignmentRule(5162L, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 120);
        MesProEdhrWorkTaskDO reviewTask = completedReviewTask(8061L, currentTask, 4161L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            workTaskService.createNextFillAfterReview(reviewTask);
        }

        List<Long> dispatchedBatchTaskIds = workTaskMapper.selectList().stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .filter(task -> Objects.equals(5162L, task.getRouteProcessId()))
                .map(MesProEdhrWorkTaskDO::getBatchTaskId)
                .sorted()
                .toList();
        assertEquals(List.of(nextMainTask.getId(), nextOptionalTask.getId()), dispatchedBatchTaskIds);
    }

    @Test
    void createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(501L, 2001L, "001");
        insertAssignmentRule(fillTask.getRouteProcessId(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, 120);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            MesProEdhrReviewTaskCreateCommand roleCandidateCommand =
                    reviewCommand("R1C1", 1, 1, 88L, "task-review-a")
                            .setCandidateSourceType("ROLE_GROUP")
                            .setCandidateSourceId(7001L)
                            .setCandidateUserSnapshot("88,89");
            List<MesProEdhrWorkTaskDO> reviewTasks = workTaskService.createReviewTasks(fillTask.getId(), 501L, List.of(
                    roleCandidateCommand,
                    reviewCommand("R1C2", 1, 2, 89L, "task-review-b")));

            assertEquals(2, reviewTasks.size());
        }

        MesProEdhrWorkTaskDO completedFillTask = workTaskMapper.selectById(fillTask.getId());
        assertEquals(MesProEdhrWorkTaskStatus.DONE, completedFillTask.getStatus());
        List<MesProEdhrWorkTaskDO> activeReviewTasks =
                workTaskMapper.selectActiveListByExecutionAndType(501L, MesProEdhrWorkTaskService.TASK_TYPE_REVIEW);
        assertEquals(2, activeReviewTasks.size());
        assertEquals(List.of("R1C1", "R1C2"), activeReviewTasks.stream()
                .map(MesProEdhrWorkTaskDO::getSignatureCellKey)
                .sorted()
                .toList());
        MesProEdhrWorkTaskDO roleCandidateTask = activeReviewTasks.stream()
                .filter(task -> "R1C1".equals(task.getSignatureCellKey()))
                .findFirst()
                .orElseThrow();
        assertEquals("ROLE_GROUP", roleCandidateTask.getCandidateSourceType());
        assertEquals(7001L, roleCandidateTask.getCandidateSourceId());
        assertEquals("88,89", roleCandidateTask.getCandidateUserSnapshot());
        assertTrue(activeReviewTasks.stream().allMatch(task -> task.getActionUrl().contains("workTaskId=" + task.getId())));
        verify(notifyMessageSendApi, org.mockito.Mockito.times(2)).sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));
        ArgumentCaptor<SystemEntitlementRevokeReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementRevokeReqDTO.class);
        verify(permissionApi).revokeEntitlementSource(captor.capture());
        assertEquals("EDHR_WORK_TASK_ASSIGNEE", captor.getValue().getSourceType());
        assertEquals("WORK_TASK|" + fillTask.getId(), captor.getValue().getSourceKey());
        assertEquals("MES_EDHR_FILLER_MINIMAL", captor.getValue().getPolicyCode());

        ArgumentCaptor<SystemEntitlementSyncReqDTO> syncCaptor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi, times(2)).syncEntitlementClaims(syncCaptor.capture());
        assertTrue(syncCaptor.getAllValues().stream()
                .allMatch(request -> "MES_EDHR_APPROVAL_REVIEWER_MINIMAL".equals(request.getPolicyCode())));
        assertEquals(Set.of("WORK_TASK|" + activeReviewTasks.get(0).getId(),
                        "WORK_TASK|" + activeReviewTasks.get(1).getId()),
                syncCaptor.getAllValues().stream()
                        .map(SystemEntitlementSyncReqDTO::getSourceKey)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void createReviewTasks_allowsCandidatePeersForSameSignatureCell() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(502L, 2002L, "002");
        insertAssignmentRule(fillTask.getRouteProcessId(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, 120);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            List<MesProEdhrWorkTaskDO> reviewTasks = workTaskService.createReviewTasks(fillTask.getId(), 502L, List.of(
                    reviewCommand("R1C1", 1, 1, 88L, "task-review-a")
                            .setCandidateSourceType("ROLE_GROUP")
                            .setCandidateSourceId(7001L)
                            .setCandidateUserSnapshot("88,89"),
                    reviewCommand("R1C1", 1, 1, 89L, "task-review-b")
                            .setCandidateSourceType("ROLE_GROUP")
                            .setCandidateSourceId(7001L)
                            .setCandidateUserSnapshot("88,89")));

            assertEquals(2, reviewTasks.size());
        }

        assertEquals(MesProEdhrWorkTaskStatus.DONE, workTaskMapper.selectById(fillTask.getId()).getStatus());
        List<MesProEdhrWorkTaskDO> reviewTasks =
                workTaskMapper.selectActiveListByExecutionAndType(502L, MesProEdhrWorkTaskService.TASK_TYPE_REVIEW);
        assertEquals(2, reviewTasks.size());
        assertTrue(reviewTasks.stream().allMatch(task -> "R1C1".equals(task.getSignatureCellKey())));
        assertEquals(List.of(88L, 89L), reviewTasks.stream()
                .map(MesProEdhrWorkTaskDO::getAssigneeUserId)
                .sorted()
                .toList());
        assertTrue(reviewTasks.stream().allMatch(task -> "ROLE_GROUP".equals(task.getCandidateSourceType())));
        assertTrue(reviewTasks.stream().allMatch(task -> Long.valueOf(7001L).equals(task.getCandidateSourceId())));
        assertTrue(reviewTasks.stream().allMatch(task -> "88,89".equals(task.getCandidateUserSnapshot())));
    }

    @Test
    void createReviewTasks_rejectsDuplicateCandidateInSameSignatureCellWithoutCompletingSubmitTask() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(512L, 2012L, "012");
        insertAssignmentRule(fillTask.getRouteProcessId(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, 120);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.createReviewTasks(fillTask.getId(), 512L, List.of(
                            reviewCommand("R1C1", 1, 1, 88L, "task-review-a"),
                            reviewCommand("R1C1", 1, 1, 88L, "task-review-b"))));
            assertEquals(PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("签字格候选人重复：R1C1"));
        }

        assertEquals(MesProEdhrWorkTaskStatus.TODO, workTaskMapper.selectById(fillTask.getId()).getStatus());
        assertTrue(workTaskMapper.selectActiveListByExecutionAndType(512L, MesProEdhrWorkTaskService.TASK_TYPE_REVIEW).isEmpty());
    }

    @Test
    void createReviewTasks_rejectsMissingFrozenCandidateSnapshotWithoutCompletingSubmitTask() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(513L, 2013L, "013");
        insertAssignmentRule(fillTask.getRouteProcessId(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, 120);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.createReviewTasks(fillTask.getId(), 513L, List.of(
                            reviewCommand("R1C1", 1, 1, 88L, "task-review-missing-snapshot")
                                    .setCandidateSourceType("ROLE_GROUP")
                                    .setCandidateSourceId(7001L)
                                    .setCandidateUserSnapshot(null))));
            assertEquals(PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("候选来源或候选快照"));
        }

        assertEquals(MesProEdhrWorkTaskStatus.TODO, workTaskMapper.selectById(fillTask.getId()).getStatus());
        assertTrue(workTaskMapper.selectActiveListByExecutionAndType(513L, MesProEdhrWorkTaskService.TASK_TYPE_REVIEW).isEmpty());
    }

    @Test
    void cancelPendingReviewTasks_cancelsOnlyOtherActiveReviewTasks() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(503L, 2003L, "003");
        insertAssignmentRule(fillTask.getRouteProcessId(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, 120);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            workTaskService.createReviewTasks(fillTask.getId(), 503L, List.of(
                    reviewCommand("R1C1", 1, 1, 88L, "task-review-a"),
                    reviewCommand("R1C2", 1, 2, 89L, "task-review-b")));
        }
        List<MesProEdhrWorkTaskDO> reviewTasks =
                workTaskMapper.selectActiveListByExecutionAndType(503L, MesProEdhrWorkTaskService.TASK_TYPE_REVIEW);
        Long rejectedTaskId = reviewTasks.get(0).getId();
        Long canceledTaskId = reviewTasks.get(1).getId();
        org.mockito.Mockito.clearInvocations(permissionApi);

        workTaskService.cancelPendingReviewTasks(503L, rejectedTaskId, "审核驳回");

        assertEquals(MesProEdhrWorkTaskStatus.TODO, workTaskMapper.selectById(rejectedTaskId).getStatus());
        MesProEdhrWorkTaskDO canceledTask = workTaskMapper.selectById(canceledTaskId);
        assertEquals(MesProEdhrWorkTaskStatus.CANCELED, canceledTask.getStatus());
        assertEquals("审核驳回", canceledTask.getReason());
        assertEquals("审核驳回", canceledTask.getRemark());
        ArgumentCaptor<SystemEntitlementRevokeReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementRevokeReqDTO.class);
        verify(permissionApi).revokeEntitlementSource(captor.capture());
        assertEquals("EDHR_WORK_TASK_ASSIGNEE", captor.getValue().getSourceType());
        assertEquals("WORK_TASK|" + canceledTaskId, captor.getValue().getSourceKey());
        assertEquals("MES_EDHR_APPROVAL_REVIEWER_MINIMAL", captor.getValue().getPolicyCode());
    }

    @Test
    void completeCandidateSignatureTask_cancelsPeerCandidateTodosInSameSignatureCell() {
        MesProEdhrWorkTaskDO candidateTask = insertCandidateReviewTask(504L, 2004L, "R3C1",
                88L, "88,89", "A");
        MesProEdhrWorkTaskDO peerTask = insertCandidateReviewTask(504L, 2004L, "R3C1",
                89L, "88,89", "B");
        MesProEdhrWorkTaskDO otherCellTask = insertCandidateReviewTask(504L, 2004L, "R3C2",
                89L, "88,89", "C");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            workTaskService.completeCandidateSignatureTask(candidateTask.getId(), 504L);
        }

        assertEquals(MesProEdhrWorkTaskStatus.DONE,
                workTaskMapper.selectById(candidateTask.getId()).getStatus());
        MesProEdhrWorkTaskDO canceledPeer = workTaskMapper.selectById(peerTask.getId());
        assertEquals(MesProEdhrWorkTaskStatus.CANCELED, canceledPeer.getStatus());
        assertEquals("同一签名位已有候选人完成", canceledPeer.getReason());
        assertEquals(MesProEdhrWorkTaskStatus.TODO,
                workTaskMapper.selectById(otherCellTask.getId()).getStatus());
        ArgumentCaptor<SystemEntitlementRevokeReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementRevokeReqDTO.class);
        verify(permissionApi, times(2)).revokeEntitlementSource(captor.capture());
        Set<String> sourceKeys = captor.getAllValues().stream()
                .map(SystemEntitlementRevokeReqDTO::getSourceKey)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("WORK_TASK|" + candidateTask.getId(), "WORK_TASK|" + peerTask.getId()), sourceKeys);
        assertTrue(captor.getAllValues().stream()
                .allMatch(request -> "MES_EDHR_APPROVAL_REVIEWER_MINIMAL".equals(request.getPolicyCode())));
    }

    @Test
    void completeCandidateSignatureTask_failsFastWhenCandidateSnapshotContainsInvalidToken() {
        MesProEdhrWorkTaskDO candidateTask = insertCandidateReviewTask(514L, 2014L, "R4C1",
                88L, "88,invalid-user", "invalid-candidate-snapshot");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.completeCandidateSignatureTask(candidateTask.getId(), 514L));
            assertEquals(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID.getCode(), exception.getCode());
        }

        assertEquals(MesProEdhrWorkTaskStatus.TODO,
                workTaskMapper.selectById(candidateTask.getId()).getStatus());
    }

    @Test
    void getApprovalCenterTodoPage_excludesPersonalFillTasksAndKeepsApprovalTasks() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(5110L, 2110L, "approval-center-fill")
                .setAssigneeUserId(810L);
        workTaskMapper.updateById(fillTask);
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(5111L, 2111L, "QA_REVIEW",
                810L, "810", "approval-center-review");
        MesProEdhrWorkTaskDO approveTask = insertApproveTask(5112L, 2112L, "QA_APPROVE",
                810L, "approval-center-approve");
        MesProEdhrWorkTaskDO releaseApproveTask = insertApproveTask(5113L, 2113L, null,
                        810L, "approval-center-release")
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE);
        workTaskMapper.updateById(releaseApproveTask);
        MesProEdhrWorkTaskPageReqVO reqVO = new MesProEdhrWorkTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(810L);
            PageResult<MesProEdhrWorkTaskRespVO> approvalPage =
                    workTaskService.getApprovalCenterTodoPage(reqVO, false);
            PageResult<MesProEdhrWorkTaskRespVO> personalPage = workTaskService.getMyPage(reqVO);

            assertEquals(3L, approvalPage.getTotal());
            assertFalse(approvalPage.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), fillTask.getId())));
            assertEquals(Set.of(
                            MesProEdhrWorkTaskService.TASK_TYPE_REVIEW,
                            MesProEdhrWorkTaskService.TASK_TYPE_APPROVE,
                            MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE),
                    approvalPage.getList().stream()
                            .map(MesProEdhrWorkTaskRespVO::getTaskType)
                            .collect(java.util.stream.Collectors.toSet()));
            assertTrue(personalPage.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), fillTask.getId())));
            assertTrue(personalPage.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), reviewTask.getId())));
            assertTrue(personalPage.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), approveTask.getId())));
        }
    }

    @Test
    void getMyPage_excludesTodoTasksFromTerminalBatches() {
        insertBatch(batchForInitialFill(1001L, 4101L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS));
        MesProEdhrWorkTaskDO activeTask = insertFillTask(5201L, 2201L, "active-batch")
                .setAssigneeUserId(99L)
                .setBatchExecutionId(1001L);
        workTaskMapper.updateById(activeTask);
        MesProEdhrBatchExecutionDO voidedBatch = batchForInitialFill(1002L, 4102L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED);
        insertBatch(voidedBatch);
        MesProEdhrWorkTaskDO voidedTask = insertFillTask(5202L, 2202L, "voided-batch")
                .setAssigneeUserId(99L)
                .setBatchExecutionId(voidedBatch.getId());
        workTaskMapper.updateById(voidedTask);
        MesProEdhrWorkTaskPageReqVO reqVO = new MesProEdhrWorkTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            PageResult<MesProEdhrWorkTaskRespVO> page = workTaskService.getMyPage(reqVO);
            MesProEdhrWorkTaskStatsRespVO stats = workTaskService.getStats();

            assertEquals(1L, page.getTotal());
            assertTrue(page.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), activeTask.getId())));
            assertFalse(page.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), voidedTask.getId())));
            assertEquals(1L, stats.getTodoCount());
            assertEquals(1L, stats.getFillCount());
        }
    }

    @Test
    void getApprovalCenterDonePage_excludesCompletedFillTasksAndKeepsApprovalHistory() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(5120L, 2120L, "approval-center-done-fill")
                .setAssigneeUserId(811L)
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCompletedAt(LocalDateTime.parse("2026-07-21T09:00:00"));
        workTaskMapper.updateById(fillTask);
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(5121L, 2121L, "QA_REVIEW",
                        811L, "811", "approval-center-done-review")
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCompletedAt(LocalDateTime.parse("2026-07-21T09:05:00"));
        workTaskMapper.updateById(reviewTask);
        MesProEdhrWorkTaskDO approveTask = insertApproveTask(5122L, 2122L, "QA_APPROVE",
                        811L, "approval-center-done-approve")
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCompletedAt(LocalDateTime.parse("2026-07-21T09:10:00"));
        workTaskMapper.updateById(approveTask);
        MesProEdhrWorkTaskPageReqVO reqVO = new MesProEdhrWorkTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(811L);
            PageResult<MesProEdhrWorkTaskRespVO> approvalPage =
                    workTaskService.getApprovalCenterDonePage(reqVO, false);
            PageResult<MesProEdhrWorkTaskRespVO> personalPage = workTaskService.getDonePage(reqVO);

            assertEquals(2L, approvalPage.getTotal());
            assertFalse(approvalPage.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), fillTask.getId())));
            assertEquals(Set.of(
                            MesProEdhrWorkTaskService.TASK_TYPE_REVIEW,
                            MesProEdhrWorkTaskService.TASK_TYPE_APPROVE),
                    approvalPage.getList().stream()
                            .map(MesProEdhrWorkTaskRespVO::getTaskType)
                            .collect(java.util.stream.Collectors.toSet()));
            assertTrue(personalPage.getList().stream()
                    .anyMatch(task -> Objects.equals(task.getId(), fillTask.getId())));
        }
    }

    @Test
    void processOverdueTasks_marksOnlyDueTodoAndDoingTasksAndSendsNotify() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 10, 0);
        MesProEdhrWorkTaskDO dueTodo = insertFillTask(601L, 2601L, "601")
                .setDueTime(now.minusMinutes(1));
        workTaskMapper.updateById(dueTodo);
        MesProEdhrWorkTaskDO dueDoing = insertFillTask(602L, 2602L, "602")
                .setStatus(MesProEdhrWorkTaskStatus.DOING)
                .setDueTime(now.minusMinutes(5));
        workTaskMapper.updateById(dueDoing);
        MesProEdhrWorkTaskDO futureTodo = insertFillTask(603L, 2603L, "603")
                .setDueTime(now.plusMinutes(5));
        workTaskMapper.updateById(futureTodo);
        MesProEdhrWorkTaskDO doneTask = insertFillTask(604L, 2604L, "604")
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setDueTime(now.minusMinutes(10));
        workTaskMapper.updateById(doneTask);

        int processed = workTaskService.processOverdueTasks(now, 20);

        assertEquals(2, processed);
        MesProEdhrWorkTaskDO overdueTodo = workTaskMapper.selectById(dueTodo.getId());
        assertEquals(MesProEdhrWorkTaskStatus.OVERDUE, overdueTodo.getStatus());
        assertEquals(now, overdueTodo.getOverdueAt());
        assertNull(overdueTodo.getCompletedAt());
        assertTrue(overdueTodo.getOverdueReason().contains("逾期自动处理"));
        assertTrue(overdueTodo.getReason().contains("逾期自动处理"));
        MesProEdhrWorkTaskDO overdueDoing = workTaskMapper.selectById(dueDoing.getId());
        assertEquals(MesProEdhrWorkTaskStatus.OVERDUE, overdueDoing.getStatus());
        assertEquals(now, overdueDoing.getOverdueAt());
        assertNull(overdueDoing.getCompletedAt());
        assertEquals(MesProEdhrWorkTaskStatus.TODO, workTaskMapper.selectById(futureTodo.getId()).getStatus());
        assertEquals(MesProEdhrWorkTaskStatus.DONE, workTaskMapper.selectById(doneTask.getId()).getStatus());
        verify(notifyMessageSendApi, org.mockito.Mockito.times(2)).sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));
    }

    @Test
    void bindExecution_bindsOverdueFillTask() {
        MesProEdhrWorkTaskDO overdueFillTask = insertFillTask(801L, 2801L, "bind-overdue")
                .setExecutionId(null)
                .setStatus(MesProEdhrWorkTaskStatus.OVERDUE);
        workTaskMapper.updateById(overdueFillTask);

        workTaskService.bindExecution(2801L, 9901L);

        MesProEdhrWorkTaskDO rebound = workTaskMapper.selectById(overdueFillTask.getId());
        assertEquals(9901L, rebound.getExecutionId());
        assertTrue(rebound.getActionUrl().contains("/mes/pro/feedback/edhr-execution/form?id=9901"));
        assertTrue(rebound.getActionUrl().contains("fillCarrier=FORM"));
        assertTrue(rebound.getActionUrl().contains("recordCategory=BATCH_RECORD"));
        assertTrue(rebound.getActionUrl().contains("workTaskId=" + overdueFillTask.getId()));
    }

    @Test
    void createReviewTasks_setsDueTimeFromReviewRule() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(605L, 2605L, "605");
        insertAssignmentRule(fillTask.getRouteProcessId(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, 120);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            List<MesProEdhrWorkTaskDO> reviewTasks = workTaskService.createReviewTasks(fillTask.getId(), 605L, List.of(
                    reviewCommand("R2C1", 2, 1, 88L, "task-review-due")));

            assertEquals(1, reviewTasks.size());
            assertNotNull(workTaskMapper.selectById(reviewTasks.get(0).getId()).getDueTime());
        }
    }

    @Test
    void createArchiveTaskAfterBatchClose_createsBatchArchiveTodoFromRouteRule() {
        MesProEdhrBatchExecutionDO batch = batchForArchive(1001L, 4101L);
        insertArchiveAssignmentRule(batch.getRouteId(), 188L, 240);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            workTaskService.createArchiveTaskAfterBatchClose(batch);
        }

        List<MesProEdhrWorkTaskDO> tasks = workTaskMapper.selectList();
        assertEquals(1, tasks.size());
        MesProEdhrWorkTaskDO archiveTask = tasks.get(0);
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE, archiveTask.getTaskType());
        assertEquals(MesProEdhrWorkTaskStatus.TODO, archiveTask.getStatus());
        assertEquals(1001L, archiveTask.getBatchExecutionId());
        assertEquals(null, archiveTask.getBatchTaskId());
        assertEquals("BATCH_ARCHIVE", archiveTask.getBusinessScopeType());
        assertEquals(1001L, archiveTask.getBusinessScopeId());
        assertEquals(188L, archiveTask.getAssigneeUserId());
        assertEquals(99L, archiveTask.getSourceUserId());
        assertEquals(null, archiveTask.getRouteProcessId());
        assertEquals("最终归档", archiveTask.getProcessName());
        assertNotNull(archiveTask.getDueTime());
        assertTrue(archiveTask.getActionUrl().contains("/mes/pro/feedback/edhr-batch-execution/detail?id=1001"));
        assertTrue(archiveTask.getActionUrl().contains("workTaskId=" + archiveTask.getId()));

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(188L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_EDHR_ARCHIVE_TASK_ASSIGNED", notifyCaptor.getValue().getTemplateCode());
    }

    @Test
    void createReleaseApprovalTaskAfterSubmit_createsTodoNotifyActionUrlAndEntitlement() {
        MesProEdhrBatchExecutionDO batch = batchForArchive(1002L, 4102L);
        insertRouteLevelAssignmentRule(batch.getRouteId(), MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE,
                188L, 180);
        MesProEdhrReleaseTransactionDO transaction = MesProEdhrReleaseTransactionDO.builder()
                .id(2002L)
                .batchExecutionId(batch.getId())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .build();

        MesProEdhrWorkTaskDO releaseTask;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("submitter");
            releaseTask = workTaskService.createReleaseApprovalTaskAfterSubmit(transaction, batch);
        }

        MesProEdhrWorkTaskDO saved = workTaskMapper.selectById(releaseTask.getId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE, saved.getTaskType());
        assertEquals(MesProEdhrWorkTaskStatus.TODO, saved.getStatus());
        assertEquals("RELEASE_TRANSACTION", saved.getBusinessScopeType());
        assertEquals(transaction.getId(), saved.getBusinessScopeId());
        assertEquals(batch.getId(), saved.getBatchExecutionId());
        assertEquals(188L, saved.getAssigneeUserId());
        assertNull(saved.getDueTime());
        assertTrue(saved.getActionUrl().contains("/mes/pro/feedback/edhr-batch-execution/detail?id=1002"));
        assertTrue(saved.getActionUrl().contains("workTaskId=" + saved.getId()));
        assertTrue(saved.getActionUrl().contains("focus=approval"));
        assertTrue(saved.getActionUrl().contains("releaseTransactionId=2002"));

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(188L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED", notifyCaptor.getValue().getTemplateCode());

        ArgumentCaptor<SystemEntitlementSyncReqDTO> entitlementCaptor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(entitlementCaptor.capture());
        assertEquals("MES_EDHR_RELEASE_APPROVER_MINIMAL", entitlementCaptor.getValue().getPolicyCode());
        assertEquals("EDHR_WORK_TASK_ASSIGNEE", entitlementCaptor.getValue().getSourceType());
        assertEquals(Set.of(188L), entitlementCaptor.getValue().getResolvedUserIds());
    }

    @Test
    void saveArchiveRule_createsAndUpdatesRouteArchiveAssignmentRule() {
        insertRoute(4103L);
        when(adminUserApi.getUser(188L)).thenReturn(adminUser(188L, CommonStatusEnum.ENABLE.getStatus()));
        when(adminUserApi.getUser(189L)).thenReturn(adminUser(189L, CommonStatusEnum.ENABLE.getStatus()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MesProEdhrWorkTaskAssignmentRuleRespVO created = workTaskService.saveArchiveRule(
                archiveRuleReq(4103L, 188L, 240, true, "最终归档责任人"));

        assertNotNull(created.getId());
        assertEquals("ROUTE", created.getScopeType());
        assertEquals(4103L, created.getScopeId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE, created.getTaskType());
        assertEquals(188L, created.getAssigneeUserId());
        assertEquals("USER", created.getCandidateSourceType());
        assertEquals(188L, created.getCandidateSourceId());
        assertEquals(240, created.getDueMinutes());
        assertEquals(true, created.getEnabled());
        assertEquals("最终归档责任人", created.getRemark());

        MesProEdhrWorkTaskAssignmentRuleRespVO updated = workTaskService.saveArchiveRule(
                archiveRuleReq(4103L, 189L, 360, false, "暂停最终归档"));

        assertEquals(created.getId(), updated.getId());
        assertEquals(189L, updated.getAssigneeUserId());
        assertEquals("USER", updated.getCandidateSourceType());
        assertEquals(189L, updated.getCandidateSourceId());
        assertEquals(360, updated.getDueMinutes());
        assertEquals(false, updated.getEnabled());
        assertEquals("暂停最终归档", updated.getRemark());
        assertEquals(1, assignmentRuleMapper.selectListByScopeAndType(
                "ROUTE", 4103L, MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE).size());
        }
    }

    @Test
    void saveReleaseApprovalRule_createsRouteReleaseApprovalAssignmentRule() {
        insertRoute(4104L);
        when(adminUserApi.getUser(188L)).thenReturn(adminUser(188L, CommonStatusEnum.ENABLE.getStatus()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MesProEdhrWorkTaskAssignmentRuleRespVO created = workTaskService.saveReleaseApprovalRule(
                    releaseApprovalRuleReq(4104L, "USER", 188L, true, "最终放行审批责任人"));

        assertNotNull(created.getId());
        assertEquals("ROUTE", created.getScopeType());
        assertEquals(4104L, created.getScopeId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE, created.getTaskType());
        assertEquals(188L, created.getAssigneeUserId());
        assertEquals("USER", created.getCandidateSourceType());
        assertEquals(188L, created.getCandidateSourceId());
        assertNull(created.getDueMinutes());
        assertEquals(true, created.getEnabled());
        }
    }

    @Test
    void saveReleaseApprovalRule_acceptsRoleCandidateWithoutDueMinutes() {
        insertRoute(4105L);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(7001L))).thenReturn(Set.of(188L, 189L));
        when(adminUserApi.getUserList(Set.of(188L, 189L))).thenReturn(List.of(
                adminUser(188L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(189L, CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MesProEdhrWorkTaskAssignmentRuleRespVO created = workTaskService.saveReleaseApprovalRule(
                    releaseApprovalRuleReq(4105L, "ROLE_GROUP", 7001L, true, "最终放行审批角色"));

        assertNotNull(created.getId());
        assertEquals("ROUTE", created.getScopeType());
        assertEquals(4105L, created.getScopeId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE, created.getTaskType());
        assertNull(created.getAssigneeUserId());
        assertEquals("ROLE_GROUP", created.getCandidateSourceType());
        assertEquals(7001L, created.getCandidateSourceId());
        assertNull(created.getDueMinutes());
        verify(roleApi).validRoleList(Set.of(7001L));
        }
    }

    @Test
    void createReleaseApprovalTaskAfterSubmit_allowsRoleCandidateUsersToRelease() {
        MesProEdhrBatchExecutionDO batch = batchForArchive(1003L, 4106L);
        insertRouteLevelAssignmentRule(batch.getRouteId(), MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE,
                null, null, "ROLE_GROUP", 7002L, null);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(7002L))).thenReturn(Set.of(188L, 189L));
        when(adminUserApi.getUserList(Set.of(188L, 189L))).thenReturn(List.of(
                adminUser(188L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(189L, CommonStatusEnum.ENABLE.getStatus())));
        MesProEdhrReleaseTransactionDO transaction = MesProEdhrReleaseTransactionDO.builder()
                .id(2003L)
                .batchExecutionId(batch.getId())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .build();

        MesProEdhrWorkTaskDO releaseTask;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("submitter");
            releaseTask = workTaskService.createReleaseApprovalTaskAfterSubmit(transaction, batch);
        }

        MesProEdhrWorkTaskDO saved = workTaskMapper.selectById(releaseTask.getId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE, saved.getTaskType());
        assertEquals(188L, saved.getAssigneeUserId());
        assertEquals("ROLE_GROUP", saved.getCandidateSourceType());
        assertEquals(7002L, saved.getCandidateSourceId());
        assertEquals("188,189", saved.getCandidateUserSnapshot());
        assertNull(saved.getDueTime());

        ArgumentCaptor<SystemEntitlementSyncReqDTO> entitlementCaptor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(entitlementCaptor.capture());
        assertEquals(Set.of(188L, 189L), entitlementCaptor.getValue().getResolvedUserIds());
    }

    @Test
    void saveArchiveRule_rejectsMissingRoute() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.saveArchiveRule(archiveRuleReq(4999L, 188L, 240, true, "不存在路线")));

        assertEquals(PRO_ROUTE_NOT_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void saveArchiveRule_rejectsMissingOrDisabledAssignee() {
        insertRoute(4104L);
        when(adminUserApi.getUser(188L)).thenReturn(null);

        ServiceException missingUser = assertThrows(ServiceException.class,
                () -> workTaskService.saveArchiveRule(archiveRuleReq(4104L, 188L, 240, true, "不存在责任人")));

        assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID.getCode(), missingUser.getCode());

        when(adminUserApi.getUser(189L)).thenReturn(adminUser(189L, CommonStatusEnum.DISABLE.getStatus()));

        ServiceException disabledUser = assertThrows(ServiceException.class,
                () -> workTaskService.saveArchiveRule(archiveRuleReq(4104L, 189L, 240, true, "禁用责任人")));

        assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID.getCode(), disabledUser.getCode());
    }

    @Test
    void saveCloseRule_createsAndUpdatesRouteCloseAssignmentRule() {
        insertRoute(4105L);
        when(adminUserApi.getUser(188L)).thenReturn(adminUser(188L, CommonStatusEnum.ENABLE.getStatus()));
        when(adminUserApi.getUser(189L)).thenReturn(adminUser(189L, CommonStatusEnum.ENABLE.getStatus()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MesProEdhrWorkTaskAssignmentRuleRespVO created = workTaskService.saveCloseRule(
                closeRuleReq(4105L, 188L, 120, true, "批次关闭责任人"));

        assertNotNull(created.getId());
        assertEquals("ROUTE", created.getScopeType());
        assertEquals(4105L, created.getScopeId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_CLOSE, created.getTaskType());
        assertEquals(188L, created.getAssigneeUserId());
        assertEquals("USER", created.getCandidateSourceType());
        assertEquals(188L, created.getCandidateSourceId());
        assertEquals(120, created.getDueMinutes());
        assertEquals(true, created.getEnabled());
        assertEquals("批次关闭责任人", created.getRemark());

        MesProEdhrWorkTaskAssignmentRuleRespVO updated = workTaskService.saveCloseRule(
                closeRuleReq(4105L, 189L, 180, false, "暂停批次关闭"));

        assertEquals(created.getId(), updated.getId());
        assertEquals(189L, updated.getAssigneeUserId());
        assertEquals("USER", updated.getCandidateSourceType());
        assertEquals(189L, updated.getCandidateSourceId());
        assertEquals(180, updated.getDueMinutes());
        assertEquals(false, updated.getEnabled());
        assertEquals("暂停批次关闭", updated.getRemark());
        assertEquals(1, assignmentRuleMapper.selectListByScopeAndType(
                "ROUTE", 4105L, MesProEdhrWorkTaskService.TASK_TYPE_CLOSE).size());
        }
    }

    @Test
    void validateAndCompleteArchiveTask_requiresAssigneeAndClosesTodo() {
        MesProEdhrBatchExecutionDO batch = batchForArchive(1002L, 4102L);
        insertArchiveAssignmentRule(batch.getRouteId(), 188L, 240);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            workTaskService.createArchiveTaskAfterBatchClose(batch);
        }
        MesProEdhrWorkTaskDO archiveTask = workTaskMapper.selectList().get(0);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.validateArchiveTask(archiveTask.getId(), batch.getId()));
            assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH.getCode(), exception.getCode());
        }

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            MesProEdhrWorkTaskDO validated = workTaskService.validateArchiveTask(archiveTask.getId(), batch.getId());
            assertEquals(archiveTask.getId(), validated.getId());
            workTaskService.completeArchiveTask(archiveTask.getId(), batch.getId());
        }

        MesProEdhrWorkTaskDO completed = workTaskMapper.selectById(archiveTask.getId());
        assertEquals(MesProEdhrWorkTaskStatus.DONE, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void cancelActiveTasksByBatch_cancelsTodoDoingAndOverdueButKeepsDone() {
        MesProEdhrWorkTaskDO todo = insertFillTask(701L, 2701L, "701")
                .setBatchExecutionId(1701L);
        workTaskMapper.updateById(todo);
        MesProEdhrWorkTaskDO doing = insertFillTask(702L, 2702L, "702")
                .setBatchExecutionId(1701L)
                .setStatus(MesProEdhrWorkTaskStatus.DOING);
        workTaskMapper.updateById(doing);
        MesProEdhrWorkTaskDO overdue = insertFillTask(703L, 2703L, "703")
                .setBatchExecutionId(1701L)
                .setStatus(MesProEdhrWorkTaskStatus.OVERDUE);
        workTaskMapper.updateById(overdue);
        MesProEdhrWorkTaskDO done = insertFillTask(704L, 2704L, "704")
                .setBatchExecutionId(1701L)
                .setStatus(MesProEdhrWorkTaskStatus.DONE);
        workTaskMapper.updateById(done);

        workTaskService.cancelActiveTasksByBatch(1701L, "质量终态拒收：批次终止");

        for (Long taskId : List.of(todo.getId(), doing.getId(), overdue.getId())) {
            MesProEdhrWorkTaskDO canceled = workTaskMapper.selectById(taskId);
            assertEquals(MesProEdhrWorkTaskStatus.CANCELED, canceled.getStatus());
            assertEquals("质量终态拒收：批次终止", canceled.getReason());
            assertNotNull(canceled.getCompletedAt());
        }
        assertEquals(MesProEdhrWorkTaskStatus.DONE, workTaskMapper.selectById(done.getId()).getStatus());
    }

    @Test
    void reassignFillTask_usesCurrentProcessFormPermissionRuleForUnfinishedTask() {
        MesProEdhrBatchExecutionTaskDO batchTask = batchTask(3090L, 9190L, 5190L,
                "REPORT-REASSIGN-001", "ROUTE_FORM", "光固I", 10)
                .setBatchRecordVersionId(78090L);
        batchTaskMapper.insert(batchTask);
        MesProEdhrWorkTaskDO fillTask = insertFillTask(8090L, batchTask.getId(), "reassign")
                .setRouteProcessId(5190L)
                .setCandidateSourceType("USERS")
                .setCandidateUserSnapshot("288")
                .setAssigneeUserId(288L);
        workTaskMapper.updateById(fillTask);
        insertProcessFormFillRule(5190L, "REPORT-REASSIGN-001", 78090L, "USERS", "389,390", 90);
        when(adminUserApi.getUserList(List.of(389L, 390L))).thenReturn(List.of(
                adminUser(389L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(390L, CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            MesProEdhrWorkTaskDO reassigned =
                    workTaskService.reassignFillTask(fillTask.getId(), "规则变更后重新派发");

            assertEquals(389L, reassigned.getAssigneeUserId());
            assertEquals("USERS", reassigned.getCandidateSourceType());
            assertNull(reassigned.getCandidateSourceId());
            assertEquals("389,390", reassigned.getCandidateUserSnapshot());
            assertEquals(99L, reassigned.getSourceUserId());
            assertEquals("规则变更后重新派发", reassigned.getReason());
        }
        ArgumentCaptor<SystemEntitlementSyncReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(captor.capture());
        SystemEntitlementSyncReqDTO request = captor.getValue();
        assertEquals("EDHR_WORK_TASK_ASSIGNEE", request.getSourceType());
        assertEquals("WORK_TASK|" + fillTask.getId(), request.getSourceKey());
        assertEquals(Set.of(389L, 390L), request.getResolvedUserIds());
        assertTrue(request.getSourceDigest().contains("candidateUserSnapshot=389,390"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reassignFillTask_sendsReassignmentNotifyWhenOwnerChanges() {
        MesProEdhrBatchExecutionTaskDO batchTask = batchTask(3092L, 9192L, 5192L,
                "REPORT-REASSIGN-003", "ROUTE_FORM", "光固I", 10)
                .setBatchRecordVersionId(78092L);
        batchTaskMapper.insert(batchTask);
        MesProEdhrWorkTaskDO fillTask = insertFillTask(8092L, batchTask.getId(), "reassign-notify")
                .setRouteProcessId(5192L)
                .setCandidateSourceType("USERS")
                .setCandidateUserSnapshot("288")
                .setAssigneeUserId(288L);
        workTaskMapper.updateById(fillTask);
        insertProcessFormFillRule(5192L, "REPORT-REASSIGN-003", 78092L, "USERS", "389,390", 90);
        when(adminUserApi.getUserList(List.of(389L, 390L))).thenReturn(List.of(
                adminUser(389L, CommonStatusEnum.ENABLE.getStatus()),
                adminUser(390L, CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            workTaskService.reassignFillTask(fillTask.getId(), "规则变更后重新派发");
        }

        MesProEdhrWorkTaskDO reassigned = workTaskMapper.selectById(fillTask.getId());
        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        NotifySendSingleToUserReqDTO notifyReq = notifyCaptor.getValue();
        assertEquals(389L, notifyReq.getUserId());
        assertEquals("MES_EDHR_FILL_TASK_REASSIGNED", notifyReq.getTemplateCode());
        Map<String, Object> params = (Map<String, Object>) notifyReq.getTemplateParams();
        assertEquals(reassigned.getActionUrl(), params.get("actionUrl"));
        assertEquals(reassigned.getId(), params.get("workTaskId"));
        assertEquals("规则变更后重新派发", params.get("reason"));
        assertTrue(String.valueOf(params.get("actionUrl")).contains("workTaskId=" + reassigned.getId()));
    }

    @Test
    void reassignFillTask_rejectsCompletedTaskAndKeepsSnapshot() {
        MesProEdhrBatchExecutionTaskDO batchTask = batchTask(3091L, 9191L, 5191L,
                "REPORT-REASSIGN-002", "ROUTE_FORM", "光固I", 10)
                .setBatchRecordVersionId(78091L);
        batchTaskMapper.insert(batchTask);
        MesProEdhrWorkTaskDO doneTask = insertFillTask(8091L, batchTask.getId(), "reassign-done")
                .setRouteProcessId(5191L)
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCandidateUserSnapshot("288");
        workTaskMapper.updateById(doneTask);
        insertProcessFormFillRule(5191L, "REPORT-REASSIGN-002", 78091L, "USERS", "389,390", 90);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.reassignFillTask(doneTask.getId(), "已完成任务不允许重新派发"));

        assertEquals(PRO_EDHR_WORK_TASK_STATUS_INVALID.getCode(), exception.getCode());
        assertEquals("288", workTaskMapper.selectById(doneTask.getId()).getCandidateUserSnapshot());
    }

    @Test
    void completeOptionalFillTaskBySkip_marksFillTaskDoneAndRevokesRuntimeEntitlement() {
        MesProEdhrBatchExecutionTaskDO batchTask = batchTask(3093L, 9193L, 5193L,
                "REPORT-OPTIONAL-SKIP-001", "ROUTE_FORM", "光固I可选表单", 10)
                .setRequiredFlag(false)
                .setRequiredPolicy("OPTIONAL");
        batchTaskMapper.insert(batchTask);
        MesProEdhrWorkTaskDO fillTask = insertFillTask(8093L, batchTask.getId(), "optional-skip")
                .setRouteProcessId(5193L)
                .setCandidateUserSnapshot("99");
        workTaskMapper.updateById(fillTask);

        workTaskService.completeOptionalFillTaskBySkip(fillTask.getId(), "本工序无需填写过程检验单");

        MesProEdhrWorkTaskDO completed = workTaskMapper.selectById(fillTask.getId());
        assertEquals(MesProEdhrWorkTaskStatus.DONE, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals("OPTIONAL_SKIP:本工序无需填写过程检验单", completed.getReason());
        assertEquals("本工序无需填写过程检验单", completed.getRemark());
        ArgumentCaptor<SystemEntitlementRevokeReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementRevokeReqDTO.class);
        verify(permissionApi).revokeEntitlementSource(captor.capture());
        assertEquals("EDHR_WORK_TASK_ASSIGNEE", captor.getValue().getSourceType());
        assertEquals("WORK_TASK|" + fillTask.getId(), captor.getValue().getSourceKey());
        assertEquals("MES_EDHR_FILLER_MINIMAL", captor.getValue().getPolicyCode());
    }

    @Test
    void completeOptionalFillTaskBySkip_rejectsNonFillTask() {
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(8094L, 9194L, "R1C1",
                99L, "99", "optional-skip-review");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workTaskService.completeOptionalFillTaskBySkip(reviewTask.getId(), "不能跳过审核任务"));

        assertEquals(PRO_EDHR_WORK_TASK_STATUS_INVALID.getCode(), exception.getCode());
        assertEquals(MesProEdhrWorkTaskStatus.TODO, workTaskMapper.selectById(reviewTask.getId()).getStatus());
    }

    @Test
    void getAssignedTaskForDetail_rejectsProcessFormFillTaskWithoutResponsibilityScopeSnapshot() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(8095L, 9195L, "missing-responsibility-scope")
                .setCandidateUserSnapshot("99")
                .setResponsibilitySourceType("EDHR_PROCESS_FORM_FILLER")
                .setResponsibilitySourceKey("ROUTE|5195|REPORT-MISSING-SCOPE|78095")
                .setResponsibilitySourceVersion("78095");
        workTaskMapper.updateById(fillTask);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> workTaskService.getAssignedTaskForDetail(fillTask.getId(), 8095L,
                            MesProEdhrWorkTaskService.TASK_TYPE_FILL));

            assertEquals(PRO_EDHR_WORK_TASK_RESPONSIBILITY_SCOPE_INVALID.getCode(), exception.getCode());
        }
    }

    @Test
    void bindExecution_updatesFillTaskActionUrlToRegisteredExecutionFormRoute() {
        MesProEdhrWorkTaskDO fillTask = insertFillTask(8092L, 9192L, "bind-execution-url")
                .setBatchExecutionId(3092L);
        workTaskMapper.updateById(fillTask);

        workTaskService.bindExecution(9192L, 8092L);

        MesProEdhrWorkTaskDO updated = workTaskMapper.selectById(fillTask.getId());
        assertTrue(updated.getActionUrl().contains("/mes/pro/feedback/edhr-execution/form?id=8092"));
        assertTrue(updated.getActionUrl().contains("workTaskId=" + fillTask.getId()));
        assertTrue(updated.getActionUrl().contains("fillCarrier=FORM"));
        assertTrue(updated.getActionUrl().contains("recordCategory=BATCH_RECORD"));
    }

    private MesProEdhrWorkTaskDO insertFillTask(Long executionId, Long batchTaskId, String suffix) {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHRT-FILL-" + suffix)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setBatchExecutionId(1001L)
                .setBatchTaskId(batchTaskId)
                .setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(batchTaskId)
                .setExecutionId(executionId)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setRouteId(4001L)
                .setRouteProcessId(5000L + executionId)
                .setProcessId(6001L)
                .setProcessName("称量")
                .setAssigneeUserId(99L)
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setActionUrl("/mes/pro/feedback/edhr-execution/detail?id=" + executionId + "&workTaskId=1")
                .setSignatureCellKey("");
        workTaskMapper.insert(task);
        return task;
    }

    private MesProEdhrWorkTaskDO insertCandidateReviewTask(Long executionId, Long batchTaskId, String signatureCellKey,
                                                           Long assigneeUserId, String candidateUserSnapshot,
                                                           String suffix) {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHRT-REVIEW-" + suffix)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)
                .setBatchExecutionId(1001L)
                .setBatchTaskId(batchTaskId)
                .setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(batchTaskId)
                .setExecutionId(executionId)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setRouteId(4001L)
                .setRouteProcessId(5504L)
                .setProcessId(6001L)
                .setProcessName("称量")
                .setAssigneeUserId(assigneeUserId)
                .setCandidateSourceType("USER_GROUP")
                .setCandidateSourceId(7001L)
                .setCandidateUserSnapshot(candidateUserSnapshot)
                .setSignatureCellKey(signatureCellKey)
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setActionUrl("/mes/pro/feedback/edhr-execution/detail?id=" + executionId);
        workTaskMapper.insert(task);
        return task;
    }

    private MesProEdhrWorkTaskDO insertApproveTask(Long executionId, Long batchTaskId, String signatureCellKey,
                                                   Long assigneeUserId, String suffix) {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHRT-APPROVE-" + suffix)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_APPROVE)
                .setBatchExecutionId(1001L)
                .setBatchTaskId(batchTaskId)
                .setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(batchTaskId)
                .setExecutionId(executionId)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setRouteId(4001L)
                .setRouteProcessId(5504L)
                .setProcessId(6001L)
                .setProcessName("称量")
                .setAssigneeUserId(assigneeUserId)
                .setSignatureCellKey(signatureCellKey)
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setActionUrl("/mes/pro/feedback/edhr-approval/detail?id=" + executionId);
        workTaskMapper.insert(task);
        return task;
    }

    private MesProEdhrBatchExecutionDO batchForInitialFill(Long batchExecutionId, Long routeId) {
        return new MesProEdhrBatchExecutionDO()
                .setId(batchExecutionId)
                .setBatchExecutionCode("BE-" + batchExecutionId)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-INITIAL")
                .setBatchCode("BATCH-INITIAL")
                .setProductId(3201L)
                .setRouteId(routeId);
    }

    private void insertBatch(MesProEdhrBatchExecutionDO batch) {
        batchExecutionMapper.insert(batch);
    }

    private MesProEdhrWorkTaskDO completedReviewTask(Long executionId, MesProEdhrBatchExecutionTaskDO currentTask,
                                                     Long routeId) {
        MesProEdhrWorkTaskDO reviewTask = insertCandidateReviewTask(executionId, currentTask.getId(), "R1C1",
                99L, "99", "review-done")
                .setBatchExecutionId(currentTask.getBatchExecutionId())
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-INITIAL")
                .setBatchCode("BATCH-INITIAL")
                .setRouteId(routeId)
                .setRouteProcessId(currentTask.getRouteProcessId())
                .setProcessId(currentTask.getProcessId())
                .setProcessName(currentTask.getProcessName())
                .setStatus(MesProEdhrWorkTaskStatus.DONE);
        workTaskMapper.updateById(reviewTask);
        return reviewTask;
    }

    private MesProEdhrBatchExecutionTaskDO batchTask(Long batchExecutionId, Long id, Long routeProcessId,
                                                     String reportId, String nodeType, String processName,
                                                     Integer routeProcessSort) {
        return new MesProEdhrBatchExecutionTaskDO()
                .setId(id)
                .setBatchExecutionId(batchExecutionId)
                .setNodeType(nodeType)
                .setRouteProcessId(routeProcessId)
                .setRouteProcessSort(routeProcessSort)
                .setProcessId(routeProcessId)
                .setProcessCode(nodeType)
                .setProcessName(processName)
                .setBatchRecordReportId(reportId)
                .setBatchRecordSort(0)
                .setExecutionMode("SEQUENTIAL")
                .setRequiredFlag(true)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
    }

    private MesProEdhrReviewTaskCreateCommand reviewCommand(String cellKey, Integer rowIndex, Integer columnIndex,
                                                            Long assigneeUserId, String bpmTaskId) {
        return new MesProEdhrReviewTaskCreateCommand()
                .setSignatureCellKey(cellKey)
                .setSignatureRowIndex(rowIndex)
                .setSignatureColumnIndex(columnIndex)
                .setReviewSourceType("POST")
                .setReviewSourceId(7001L)
                .setReviewSourceName("QA 岗")
                .setAssigneeUserId(assigneeUserId)
                .setCandidateSourceType("USER")
                .setCandidateSourceId(assigneeUserId)
                .setCandidateUserSnapshot(String.valueOf(assigneeUserId))
                .setBpmTaskId(bpmTaskId);
    }

    private void insertAssignmentRule(Long routeProcessId, String taskType, Integer dueMinutes) {
        insertAssignmentRule(routeProcessId, taskType, dueMinutes, null, null);
    }

    private void insertAssignmentRule(Long routeProcessId, String taskType, Integer dueMinutes,
                                      String candidateSourceType, Long candidateSourceId) {
        insertAssignmentRule(routeProcessId, taskType, dueMinutes, candidateSourceType, candidateSourceId, 88L);
    }

    private void insertAssignmentRule(Long routeProcessId, String taskType, Integer dueMinutes,
                                      String candidateSourceType, Long candidateSourceId, Long assigneeUserId) {
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO rule =
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO()
                        .setRouteProcessId(routeProcessId)
                        .setScopeType("ROUTE_PROCESS")
                        .setScopeId(routeProcessId)
                        .setTaskType(taskType)
                        .setAssigneeUserId(assigneeUserId)
                        .setReviewUserId(assigneeUserId)
                        .setCandidateSourceType(candidateSourceType)
                        .setCandidateSourceId(candidateSourceId)
                        .setDueMinutes(dueMinutes)
                        .setEnabled(true);
        assignmentRuleMapper.insert(rule);
        if (candidateSourceType == null || "USER".equals(candidateSourceType)) {
            Long sourceUserId = candidateSourceId != null ? candidateSourceId : assigneeUserId;
            when(adminUserApi.getUser(sourceUserId)).thenReturn(adminUser(sourceUserId, CommonStatusEnum.ENABLE.getStatus()));
        }
    }

    private void insertProcessFormFillRule(Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId,
                                           String candidateSourceType, String candidateSourceIds, Integer dueMinutes) {
        insertProcessFormFillRule(routeProcessId, batchRecordReportId, batchRecordVersionId,
                "ALL", candidateSourceType, candidateSourceIds, dueMinutes, preciseCellScope("ALL", 0, 0, 0));
    }

    private void insertProcessFormFillRule(Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId,
                                           String scopeKey, String candidateSourceType, String candidateSourceIds,
                                           Integer dueMinutes, String fillableScopeJson) {
        processFormPermissionRuleMapper.insert(processFormFillRule(routeProcessId, batchRecordReportId,
                batchRecordVersionId, scopeKey, candidateSourceType, candidateSourceIds, dueMinutes,
                fillableScopeJson));
    }

    private void insertProcessFormFillRuleWithoutVersion(Long routeProcessId, String batchRecordReportId,
                                                         String candidateSourceType, String candidateSourceIds,
                                                         Integer dueMinutes) {
        processFormPermissionRuleMapper.insert(processFormFillRule(routeProcessId, batchRecordReportId,
                null, "ALL", candidateSourceType, candidateSourceIds, dueMinutes,
                preciseCellScope("ALL", 0, 0, 0)));
    }

    private MesProEdhrProcessFormPermissionRuleDO processFormFillRule(Long routeProcessId, String batchRecordReportId,
                                                                      Long batchRecordVersionId,
                                                                      String candidateSourceType,
                                                                      String candidateSourceIds, Integer dueMinutes) {
        return processFormFillRule(routeProcessId, batchRecordReportId, batchRecordVersionId,
                "ALL", candidateSourceType, candidateSourceIds, dueMinutes, preciseCellScope("ALL", 0, 0, 0));
    }

    private MesProEdhrProcessFormPermissionRuleDO processFormFillRule(Long routeProcessId, String batchRecordReportId,
                                                                      Long batchRecordVersionId, String scopeKey,
                                                                      String candidateSourceType,
                                                                      String candidateSourceIds, Integer dueMinutes,
                                                                      String fillableScopeJson) {
        return new MesProEdhrProcessFormPermissionRuleDO()
                .setBatchRecordVersionId(batchRecordVersionId)
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(batchRecordReportId)
                .setRuleType("FILL")
                .setScopeKey(scopeKey)
                .setSignatureCellKey("")
                .setCandidateSourceType(candidateSourceType)
                .setCandidateSourceIds(candidateSourceIds)
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(dueMinutes)
                .setFillableScopeJson(fillableScopeJson)
                .setEnabled(true);
    }

    private String preciseCellScope(String scopeKey, int sourceTableIndex, int rowIndex, int columnIndex) {
        return """
                {"schemaVersion":2,"scopeKey":"%s","cells":[{"sourceTableIndex":%d,"rowIndex":%d,"columnIndex":%d}]}
                """.formatted(scopeKey, sourceTableIndex, rowIndex, columnIndex).trim();
    }

    private String routeFormRangeScope(int sourceTableIndex, int startRow, int endRow) {
        return """
                {"ranges":[{"sourceTableIndex":%d,"startRow":%d,"endRow":%d}]}
                """.formatted(sourceTableIndex, startRow, endRow).trim();
    }

    private String routeFormRangeScopeForTables(int sourceTableCount, int startRow, int endRow) {
        JSONArray ranges = new JSONArray();
        for (int sourceTableIndex = 0; sourceTableIndex < sourceTableCount; sourceTableIndex++) {
            JSONObject range = new JSONObject();
            range.put("sourceTableIndex", sourceTableIndex);
            range.put("startRow", startRow);
            range.put("endRow", endRow);
            ranges.add(range);
        }
        JSONObject scope = new JSONObject();
        scope.put("ranges", ranges);
        return JSON.toJSONString(scope);
    }

    private void insertArchiveAssignmentRule(Long routeId, Long assigneeUserId, Integer dueMinutes) {
        insertRouteLevelAssignmentRule(routeId, MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE, assigneeUserId,
                dueMinutes);
    }

    private void insertRouteLevelAssignmentRule(Long routeId, String taskType, Long assigneeUserId,
                                                Integer dueMinutes) {
        insertRouteLevelAssignmentRule(routeId, taskType, assigneeUserId, dueMinutes, "USER",
                assigneeUserId, assigneeUserId);
    }

    private void insertRouteLevelAssignmentRule(Long routeId, String taskType, Long assigneeUserId,
                                                Integer dueMinutes, String candidateSourceType,
                                                Long candidateSourceId, Long stubUserId) {
        MesProEdhrWorkTaskAssignmentRuleDO rule =
                new MesProEdhrWorkTaskAssignmentRuleDO()
                        .setRouteProcessId(null)
                        .setScopeType("ROUTE")
                        .setScopeId(routeId)
                        .setTaskType(taskType)
                        .setAssigneeUserId(assigneeUserId)
                        .setCandidateSourceType(candidateSourceType)
                        .setCandidateSourceId(candidateSourceId)
                        .setDueMinutes(dueMinutes)
                        .setEnabled(true);
        assignmentRuleMapper.insert(rule);
        if (stubUserId != null) {
            when(adminUserApi.getUser(stubUserId)).thenReturn(adminUser(stubUserId, CommonStatusEnum.ENABLE.getStatus()));
        }
    }

    private MesProEdhrWorkTaskArchiveRuleReqVO archiveRuleReq(Long routeId, Long assigneeUserId, Integer dueMinutes,
                                                              Boolean enabled, String remark) {
        return new MesProEdhrWorkTaskArchiveRuleReqVO()
                .setRouteId(routeId)
                .setAssigneeUserId(assigneeUserId)
                .setDueMinutes(dueMinutes)
                .setEnabled(enabled)
                .setRemark(remark);
    }

    private MesProEdhrWorkTaskCloseRuleReqVO closeRuleReq(Long routeId, Long assigneeUserId, Integer dueMinutes,
                                                          Boolean enabled, String remark) {
        return new MesProEdhrWorkTaskCloseRuleReqVO()
                .setRouteId(routeId)
                .setAssigneeUserId(assigneeUserId)
                .setDueMinutes(dueMinutes)
                .setEnabled(enabled)
                .setRemark(remark);
    }

    private MesProEdhrWorkTaskReleaseApprovalRuleReqVO releaseApprovalRuleReq(Long routeId,
                                                                              String candidateSourceType,
                                                                              Long candidateSourceId, Boolean enabled,
                                                                              String remark) {
        return new MesProEdhrWorkTaskReleaseApprovalRuleReqVO()
                .setRouteId(routeId)
                .setCandidateSourceType(candidateSourceType)
                .setCandidateSourceId(candidateSourceId)
                .setEnabled(enabled)
                .setRemark(remark);
    }

    private void insertRoute(Long routeId) {
        routeMapper.insert(MesProRouteDO.builder()
                .id(routeId)
                .code("ROUTE-" + routeId)
                .name("eDHR 路线 " + routeId)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
    }

    private AdminUserRespDTO adminUser(Long userId, Integer status) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setStatus(status);
        return user;
    }

    private MesProEdhrBatchExecutionDO batchForArchive(Long batchExecutionId, Long routeId) {
        return new MesProEdhrBatchExecutionDO()
                .setId(batchExecutionId)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setProductId(3201L)
                .setRouteId(routeId)
                .setRouteCode("ROUTE-001")
                .setRouteName("eDHR 路线")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED);
    }
}
