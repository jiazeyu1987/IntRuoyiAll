package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchTaskVisibilityServiceTest {

    private static final Long BATCH_ID = 100L;
    private static final Long ROUTE_ID = 200L;
    private static final Long TASK_ID = 300L;
    private static final Long ROUTE_PROCESS_ID = 400L;
    private static final Long PROCESS_ID = 500L;
    private static final Long USER_ID = 600L;

    private MesProEdhrBatchTaskVisibilityService service;

    @Mock
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;

    @BeforeEach
    void setUp() {
        service = new MesProEdhrBatchTaskVisibilityService();
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "assignmentRuleMapper", assignmentRuleMapper);
        ReflectionTestUtils.setField(service, "routeProcessService", routeProcessService);
    }

    @Test
    void resolve_shouldUseFrozenRouteProcessForFillRuleVisibility() {
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(BATCH_ID)
                .setRouteId(ROUTE_ID);
        MesProEdhrBatchExecutionTaskDO task = new MesProEdhrBatchExecutionTaskDO()
                .setId(TASK_ID)
                .setBatchExecutionId(BATCH_ID)
                .setRouteProcessId(ROUTE_PROCESS_ID)
                .setProcessId(PROCESS_ID)
                .setBatchRecordReportId("RPT-FROZEN");
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(ROUTE_PROCESS_ID)
                .routeId(ROUTE_ID)
                .processId(PROCESS_ID)
                .build();
        MesProEdhrWorkTaskAssignmentRuleDO fillRule = new MesProEdhrWorkTaskAssignmentRuleDO()
                .setRouteProcessId(ROUTE_PROCESS_ID)
                .setTaskType("FILL")
                .setCandidateSourceType("USER")
                .setCandidateSourceId(USER_ID)
                .setEnabled(true);

        when(permissionApi.hasAnyPermissions(USER_ID, MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION))
                .thenReturn(false);
        when(workTaskMapper.selectTimelineListByBatchExecutionId(BATCH_ID)).thenReturn(List.of());
        when(assignmentRuleMapper.selectEnabledByScopeAndType("ROUTE", ROUTE_ID, "CLOSE")).thenReturn(null);
        when(routeProcessService.resolveFrozenRouteProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID))
                .thenReturn(frozenRouteProcess);
        when(assignmentRuleMapper.selectEnabledByRouteProcessAndType(ROUTE_PROCESS_ID, "FILL"))
                .thenReturn(fillRule);

        MesProEdhrBatchTaskVisibilityService.VisibilityScope result =
                service.resolve(batch, List.of(task), USER_ID);

        assertEquals(MesProEdhrBatchTaskVisibilityService.VISIBILITY_MODE_ASSIGNED, result.mode());
        assertEquals(List.of(TASK_ID), result.tasks().stream().map(MesProEdhrBatchExecutionTaskDO::getId).toList());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID);
    }
}
