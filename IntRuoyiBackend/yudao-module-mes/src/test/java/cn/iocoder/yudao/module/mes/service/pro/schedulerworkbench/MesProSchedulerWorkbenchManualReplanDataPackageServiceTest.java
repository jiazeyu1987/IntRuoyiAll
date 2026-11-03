package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchManualReplanDataImportRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProSchedulerWorkbenchManualReplanDataPackageServiceTest {

    @InjectMocks
    private MesProSchedulerWorkbenchManualReplanDataPackageServiceImpl service;

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;

    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper projectBindingMapper;
    @Mock
    private cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProjectGuard projectGuard;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper routeMapper;

    @Test
    void importPackage_shouldRejectRoutesWithoutProjectsBeforeWriting() {
        byte[] payload = manualReplanDataPackageJson("[]").replace("\"routes\":[]", "\"routes\":[{\"id\":10}]")
                .getBytes(StandardCharsets.UTF_8);
        assertServiceException(() -> service.importPackage(payload), CONFIG_PACKAGE_CONTENT_INVALID,
                "数据包存在缺少项目代码的工艺流程，请补齐关联后重新导出");
        org.mockito.Mockito.verifyNoInteractions(routeMapper, workOrderMapper, scheduleOrderMapper);
    }

    @Test
    void importPackage_shouldRejectTwoRoutesForSameProjectBeforeWriting() {
        byte[] payload = manualReplanDataPackageJson("[]")
                .replace("\"routes\":[]", "\"routes\":[{\"id\":10},{\"id\":11}]")
                .replace("\"routeProjectBindings\":[]", "\"routeProjectBindings\":[{\"routeId\":10,\"dccProjectCodeId\":42},{\"routeId\":11,\"dccProjectCodeId\":42}]")
                .getBytes(StandardCharsets.UTF_8);
        assertServiceException(() -> service.importPackage(payload), CONFIG_PACKAGE_CONTENT_INVALID,
                "数据包项目关联无效：每条流程必须且只能关联一个独占项目");
        org.mockito.Mockito.verifyNoInteractions(routeMapper, workOrderMapper, scheduleOrderMapper);
    }

    @Test
    void importPackage_shouldRejectOccupiedProjectBeforeWriting() {
        byte[] payload = manualReplanDataPackageJson("[]")
                .replace("\"routes\":[]", "\"routes\":[{\"id\":10}]")
                .replace("\"routeProjectBindings\":[]", "\"routeProjectBindings\":[{\"routeId\":10,\"dccProjectCodeId\":42}]")
                .getBytes(StandardCharsets.UTF_8);
        org.mockito.Mockito.doThrow(new IllegalStateException("已有工艺流程")).when(projectGuard).requireAvailable(42L,10L);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.importPackage(payload));
        org.mockito.Mockito.verifyNoInteractions(routeMapper, workOrderMapper, scheduleOrderMapper);
    }

    @Test
    void importPackage_shouldFailFastWhenRequiredListMissing() {
        byte[] invalid = """
                {"packageVersion":"scheduler-manual-replan-data.v2"}
                """.getBytes(StandardCharsets.UTF_8);

        assertServiceException(() -> service.importPackage(invalid),
                CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包缺少 items");
    }

    @Test
    void importPackage_shouldUpsertScheduleOrderRowsAndReportCounts() {
        when(scheduleOrderMapper.selectById(8101L)).thenReturn(null);
        byte[] payload = manualReplanDataPackageJson("""
                [{"id":8101,"code":"SO-8101","workOrderId":7101,"autoSchedulable":true}]
                """).getBytes(StandardCharsets.UTF_8);

        MesProSchedulerWorkbenchManualReplanDataImportRespVO result = service.importPackage(payload);

        ArgumentCaptor<MesProScheduleOrderDO> captor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).insert(captor.capture());
        assertEquals(8101L, captor.getValue().getId());
        assertEquals("SO-8101", captor.getValue().getCode());
        assertEquals(0, result.getMasterDataCount());
        assertEquals(1, result.getScheduleOrderDataCount());
        assertEquals(0, result.getRuntimeDataCount());
    }

    @Test
    void importPackage_shouldRewriteTenantBaseRowsToCurrentTenant() {
        TenantContextHolder.setTenantId(200L);
        try {
            when(workOrderMapper.selectById(7101L)).thenReturn(null);
            byte[] payload = manualReplanDataPackageJson("[]", """
                    [{"id":7101,"code":"WO-7101","tenantId":100}]
                    """).getBytes(StandardCharsets.UTF_8);

            MesProSchedulerWorkbenchManualReplanDataImportRespVO result = service.importPackage(payload);

            ArgumentCaptor<MesProWorkOrderDO> captor = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
            verify(workOrderMapper).insert(captor.capture());
            assertEquals(7101L, captor.getValue().getId());
            assertEquals("WO-7101", captor.getValue().getCode());
            assertEquals(200L, captor.getValue().getTenantId());
            assertEquals(0, result.getMasterDataCount());
            assertEquals(1, result.getScheduleOrderDataCount());
            assertEquals(0, result.getRuntimeDataCount());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static String manualReplanDataPackageJson(String scheduleOrders) {
        return manualReplanDataPackageJson(scheduleOrders, "[]");
    }

    private static String manualReplanDataPackageJson(String scheduleOrders, String workOrders) {
        return """
                {
                  "packageVersion":"scheduler-manual-replan-data.v2",
                  "items":[],
                  "processes":[],
                  "routes":[],
                  "routeProjectBindings":[],
                  "routeVersions":[],
                  "routeProducts":[],
                  "routeProcesses":[],
                  "routeProcessFlowEdges":[],
                  "routeFlowConfigs":[],
                  "routeFlowProcessConfigs":[],
                  "routeFlowProcessBatchRecords":[],
                  "routeScheduleConfigs":[],
                  "productionLines":[],
                  "workstations":[],
                  "workstationMachines":[],
                  "workstationWorkers":[],
                  "calendarPlans":[],
                  "calendarPlanShifts":[],
                  "calendarHolidays":[],
                  "scheduleCalendarRules":[],
                  "capacityPlans":[],
                  "capacityActuals":[],
                  "materialStocks":[],
                  "workOrders":%s,
                  "scheduleOrders":%s,
                  "scheduleOrderProcesses":[],
                  "productionMaterialLists":[],
                  "tasks":[],
                  "taskScheduleExts":[],
                  "taskDependencies":[],
                  "feedbacks":[],
                  "scheduleIssues":[],
                  "scheduleOrderOperationLogs":[],
                  "replanExplanationSnapshots":[]
                }
                """.formatted(workOrders, scheduleOrders);
    }
}
