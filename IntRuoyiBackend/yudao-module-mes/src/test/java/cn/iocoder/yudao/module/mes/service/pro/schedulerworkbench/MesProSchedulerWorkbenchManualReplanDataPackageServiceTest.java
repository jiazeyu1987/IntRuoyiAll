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

    @Test
    void importPackage_shouldFailFastWhenRequiredListMissing() {
        byte[] invalid = """
                {"packageVersion":"scheduler-manual-replan-data.v1"}
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
                  "packageVersion":"scheduler-manual-replan-data.v1",
                  "items":[],
                  "processes":[],
                  "routes":[],
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
