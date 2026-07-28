package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrWorkTaskLegacyProcessTest {

    private static final Long ROUTE_ID = 8100L;
    private static final Long BATCH_EXECUTION_ID = 8200L;
    private static final Long BATCH_TASK_ID = 8300L;
    private static final Long HISTORICAL_ROUTE_PROCESS_ID = 8400L;
    private static final Long CURRENT_ROUTE_PROCESS_ID = 8500L;
    private static final Long HISTORICAL_PROCESS_ID = 9400L;
    private static final Long CURRENT_PROCESS_ID = 9500L;
    private static final Long BATCH_RECORD_VERSION_ID = 9600L;

    private MesProEdhrWorkTaskServiceImpl service;

    @Mock
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Mock
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Mock
    private NotifyMessageSendApi notifyMessageSendApi;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private MesProEdhrCandidateResolver candidateResolver;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private PermissionApi permissionApi;

    @BeforeEach
    void setUp() {
        service = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchExecutionMapper);
        ReflectionTestUtils.setField(service, "assignmentRuleMapper", assignmentRuleMapper);
        ReflectionTestUtils.setField(service, "batchTaskMapper", batchTaskMapper);
        ReflectionTestUtils.setField(service, "processFormPermissionRuleMapper", processFormPermissionRuleMapper);
        ReflectionTestUtils.setField(service, "notifyMessageSendApi", notifyMessageSendApi);
        ReflectionTestUtils.setField(service, "routeMapper", routeMapper);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "roleApi", roleApi);
        ReflectionTestUtils.setField(service, "deptApi", deptApi);
        ReflectionTestUtils.setField(service, "candidateResolver", candidateResolver);
        ReflectionTestUtils.setField(service, "routeProcessService", routeProcessService);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void createInitialFillTask_shouldUseFrozenRouteProcessAssignmentRuleForHistoricalTask() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProEdhrBatchExecutionTaskDO batchTask = batchTask();
        MesProRouteProcessDO frozenRouteProcess = frozenRouteProcess();
        MesProEdhrWorkTaskAssignmentRuleDO frozenRule = assignmentRule();
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(List.of(batchTask));
        when(routeProcessService.resolveFrozenRouteProcess(
                HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, HISTORICAL_PROCESS_ID)).thenReturn(frozenRouteProcess);
        when(assignmentRuleMapper.selectEnabledByRouteProcessAndType(
                HISTORICAL_ROUTE_PROCESS_ID, MesProEdhrWorkTaskService.TASK_TYPE_FILL)).thenReturn(frozenRule);
        when(candidateResolver.resolveAssignmentRule(frozenRule))
                .thenReturn(new MesProEdhrCandidateResolver.MesProEdhrCandidateContract(
                        "USER", 88L, "88"));
        when(adminUserApi.getUser(88L)).thenReturn(adminUser(88L));
        when(workTaskMapper.insert(org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskDO.class)))
                .thenAnswer(invocation -> {
                    MesProEdhrWorkTaskDO task = invocation.getArgument(0, MesProEdhrWorkTaskDO.class);
                    task.setId(9900L);
                    return 1;
                });

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");

            service.createInitialFillTask(batch);
        }

        verify(routeProcessService, atLeastOnce()).resolveFrozenRouteProcess(
                HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, HISTORICAL_PROCESS_ID);
        verify(routeProcessService, never()).resolveCurrentRouteProcess(
                HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, HISTORICAL_PROCESS_ID);
        verify(assignmentRuleMapper, atLeastOnce()).selectEnabledByRouteProcessAndType(
                HISTORICAL_ROUTE_PROCESS_ID, MesProEdhrWorkTaskService.TASK_TYPE_FILL);
        assertEquals(HISTORICAL_ROUTE_PROCESS_ID, insertedTask().getRouteProcessId());
        assertEquals(HISTORICAL_PROCESS_ID, insertedTask().getProcessId());
    }

    @Test
    void createInitialFillTask_shouldUseFrozenBatchRecordVersionPermissionRule() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProEdhrBatchExecutionTaskDO batchTask = batchTask();
        MesProRouteProcessDO frozenRouteProcess = frozenRouteProcess();
        MesProEdhrProcessFormPermissionRuleDO frozenVersionRule = processFormPermissionRule();
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(List.of(batchTask));
        when(batchTaskMapper.selectById(BATCH_TASK_ID)).thenReturn(batchTask);
        when(routeProcessService.resolveFrozenRouteProcess(
                HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, HISTORICAL_PROCESS_ID)).thenReturn(frozenRouteProcess);
        when(processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                HISTORICAL_ROUTE_PROCESS_ID, "RPT-LEGACY", BATCH_RECORD_VERSION_ID))
                .thenReturn(List.of(frozenVersionRule));
        when(candidateResolver.resolveProcessFormRule(frozenVersionRule))
                .thenReturn(new MesProEdhrCandidateResolver.MesProEdhrCandidateContract(
                        "USER", null, "99"));
        when(workTaskMapper.insert(org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskDO.class)))
                .thenAnswer(invocation -> {
                    MesProEdhrWorkTaskDO task = invocation.getArgument(0, MesProEdhrWorkTaskDO.class);
                    task.setId(9901L);
                    return 1;
                });

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");

            service.createInitialFillTask(batch);
        }

        verify(processFormPermissionRuleMapper, atLeastOnce()).selectEnabledFillRulesForRouteOrReport(
                HISTORICAL_ROUTE_PROCESS_ID, "RPT-LEGACY", BATCH_RECORD_VERSION_ID);
        verify(processFormPermissionRuleMapper, never()).selectEnabledFillRulesForRouteOrReport(
                HISTORICAL_ROUTE_PROCESS_ID, "RPT-LEGACY");
        verify(assignmentRuleMapper, never()).selectEnabledByRouteProcessAndType(
                HISTORICAL_ROUTE_PROCESS_ID, MesProEdhrWorkTaskService.TASK_TYPE_FILL);
        assertEquals(99L, insertedTask().getAssigneeUserId());
    }

    @Test
    void calculateDueTime_shouldUseFrozenRouteProcessRuleForHistoricalWorkTask() throws Exception {
        MesProEdhrBatchExecutionTaskDO batchTask = batchTask();
        MesProRouteProcessDO frozenRouteProcess = frozenRouteProcess();
        MesProEdhrWorkTaskAssignmentRuleDO frozenRule = assignmentRule();
        when(batchTaskMapper.selectById(BATCH_TASK_ID)).thenReturn(batchTask);
        when(routeProcessService.resolveFrozenRouteProcess(
                HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, HISTORICAL_PROCESS_ID)).thenReturn(frozenRouteProcess);
        when(assignmentRuleMapper.selectEnabledByRouteProcessAndType(
                HISTORICAL_ROUTE_PROCESS_ID, MesProEdhrWorkTaskService.TASK_TYPE_FILL)).thenReturn(frozenRule);
        when(candidateResolver.resolveAssignmentRule(frozenRule))
                .thenReturn(new MesProEdhrCandidateResolver.MesProEdhrCandidateContract(
                        "USER", 88L, "88"));
        Method method = MesProEdhrWorkTaskServiceImpl.class.getDeclaredMethod(
                "calculateDueTime", MesProEdhrWorkTaskDO.class, MesProEdhrBatchExecutionDO.class);
        method.setAccessible(true);
        method.invoke(service, new MesProEdhrWorkTaskDO()
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setBatchTaskId(BATCH_TASK_ID)
                .setRouteId(ROUTE_ID)
                .setRouteProcessId(HISTORICAL_ROUTE_PROCESS_ID)
                .setProcessId(HISTORICAL_PROCESS_ID), batch());

        verify(assignmentRuleMapper).selectEnabledByRouteProcessAndType(
                HISTORICAL_ROUTE_PROCESS_ID, MesProEdhrWorkTaskService.TASK_TYPE_FILL);
        verify(routeProcessService, never()).resolveCurrentRouteProcess(
                HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, HISTORICAL_PROCESS_ID);
    }

    private MesProEdhrWorkTaskDO insertedTask() {
        return org.mockito.Mockito.mockingDetails(workTaskMapper).getInvocations().stream()
                .filter(invocation -> "insert".equals(invocation.getMethod().getName()))
                .map(invocation -> invocation.getArgument(0, MesProEdhrWorkTaskDO.class))
                .findFirst()
                .orElseThrow();
    }

    private MesProEdhrBatchExecutionDO batch() {
        return new MesProEdhrBatchExecutionDO()
                .setId(BATCH_EXECUTION_ID)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-LEGACY")
                .setBatchCode("BATCH-LEGACY")
                .setProductId(3201L)
                .setRouteId(ROUTE_ID);
    }

    private MesProEdhrBatchExecutionTaskDO batchTask() {
        return new MesProEdhrBatchExecutionTaskDO()
                .setId(BATCH_TASK_ID)
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setNodeType("ROUTE_FORM")
                .setRouteProcessId(HISTORICAL_ROUTE_PROCESS_ID)
                .setRouteProcessSort(10)
                .setProcessId(HISTORICAL_PROCESS_ID)
                .setProcessName("旧工序报表")
                .setBatchRecordReportId("RPT-LEGACY")
                .setBatchRecordVersionId(BATCH_RECORD_VERSION_ID)
                .setBatchRecordSort(0)
                .setExecutionMode("SEQUENTIAL")
                .setRequiredFlag(true)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
    }

    private MesProRouteProcessDO frozenRouteProcess() {
        return MesProRouteProcessDO.builder()
                .id(HISTORICAL_ROUTE_PROCESS_ID)
                .routeId(ROUTE_ID)
                .processId(HISTORICAL_PROCESS_ID)
                .sort(10)
                .build();
    }

    private MesProEdhrWorkTaskAssignmentRuleDO assignmentRule() {
        return new MesProEdhrWorkTaskAssignmentRuleDO()
                .setId(9100L)
                .setRouteProcessId(HISTORICAL_ROUTE_PROCESS_ID)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setCandidateSourceType("USER")
                .setCandidateSourceId(88L)
                .setAssigneeUserId(88L)
                .setDueMinutes(120)
                .setEnabled(true);
    }

    private MesProEdhrProcessFormPermissionRuleDO processFormPermissionRule() {
        return new MesProEdhrProcessFormPermissionRuleDO()
                .setId(9200L)
                .setRouteProcessId(HISTORICAL_ROUTE_PROCESS_ID)
                .setBatchRecordReportId("RPT-LEGACY")
                .setBatchRecordVersionId(BATCH_RECORD_VERSION_ID)
                .setRuleType("FILL")
                .setSignatureCellKey("")
                .setCandidateSourceType("USER")
                .setCandidateSourceIds("99")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(45)
                .setEnabled(true);
    }

    private AdminUserRespDTO adminUser(Long userId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setStatus(0);
        return user;
    }
}
