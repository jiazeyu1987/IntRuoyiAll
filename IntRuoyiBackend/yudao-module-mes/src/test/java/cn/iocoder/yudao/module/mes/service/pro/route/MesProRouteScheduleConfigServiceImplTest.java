package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteResourceCapacityPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteScheduleConfigServiceImplTest {

    @InjectMocks
    private MesProRouteScheduleConfigServiceImpl scheduleConfigService;

    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private MesMdProductionLineService productionLineService;
    @Mock
    private MesDvMachineryService machineryService;
    @Mock
    private MesDvMachineryProcessService machineryProcessService;

    @Test
    void getConfigRespListByRouteVersionId_shouldPreserveDraftClientRouteProcessId() {
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(633L)
                .routeId(980091L)
                .versionNo("V3")
                .active(false)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": -8, "processId": 9908090105, "sort": 8}
                              ]
                            },
                            "scheduleConfigs": {
                              "-8": {
                                "routeVersionId": 633,
                                "routeId": 980091,
                                "routeProcessId": -8,
                                "sort": 8,
                                "capacityMode": "RESOURCE_CALCULATED",
                                "nightShiftEnabled": false
                              }
                            }
                          }
                        }
                        """)
                .build();
        when(routeVersionMapper.selectById(633L)).thenReturn(routeVersion);
        when(routeProcessMapper.selectListByRouteId(980091L)).thenReturn(List.of());

        List<MesProRouteScheduleConfigRespVO> configs =
                scheduleConfigService.getConfigRespListByRouteVersionId(633L);

        assertEquals(1, configs.size());
        assertEquals(-8L, configs.get(0).getRouteProcessId());
        assertEquals("RESOURCE_CALCULATED", configs.get(0).getCapacityMode());
    }

    @Test
    void getResourcePreview_shouldAllowExplicitBoundWorkstationFromDifferentProcess() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(701L)
                .routeId(70L)
                .processId(901L)
                .workstationId(801L)
                .build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(801L)
                .processId(902L)
                .code("WS-SHARED")
                .name("共享工作站")
                .productionLineId(1001L)
                .singleStandardHourlyCapacity(new BigDecimal("6"))
                .shiftHours(new BigDecimal("8"))
                .build();
        when(routeProcessService.getRouteProcess(701L)).thenReturn(routeProcess);
        when(workstationService.getWorkstation(801L)).thenReturn(workstation);
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any())).thenReturn(List.of());
        when(productionLineService.getProductionLineMap(any()))
                .thenReturn(Map.of(1001L, MesMdProductionLineDO.builder().id(1001L).name("一号产线").build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of());
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(any(), any()))
                .thenReturn(List.of());

        MesProRouteResourceCapacityPreviewRespVO preview = scheduleConfigService.getResourcePreview(701L);

        assertTrue(preview.getBlockingIssues().stream()
                .noneMatch(issue -> "BLOCKED_WORKSTATION_PROCESS_MISMATCH".equals(issue.getCode())));
        assertEquals("WORKER", preview.getCapacitySource());
        assertEquals(0, preview.getResourceCapacityHourly().compareTo(new BigDecimal("6")));
        assertEquals(801L, preview.getWorkstationRows().get(0).getWorkstationId());
        assertEquals(0, preview.getWorkstationRows().get(0).getShiftHours().compareTo(new BigDecimal("8")));
    }
}
