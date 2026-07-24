package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchRouteConfigImportRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteResourceService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProSchedulerWorkbenchRouteConfigPackageServiceTest {

    @InjectMocks
    private MesProSchedulerWorkbenchRouteConfigPackageServiceImpl service;

    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteFlowConfigService routeFlowConfigService;
    @Mock
    private MesProRouteScheduleConfigService routeScheduleConfigService;
    @Mock
    private MesProRouteResourceService routeResourceService;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesDvMachineryMapper machineryMapper;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exportPackage_shouldContainScheduleUseConfigScheduleConfigAndResources() throws Exception {
        MesProRouteRespVO route = new MesProRouteRespVO()
                .setId(10L)
                .setCode("ROUTE-001")
                .setName("导管路线")
                .setActiveRouteVersionId(100L);
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(route), 1L));
        when(routeFlowConfigService.getRouteFlowProcessConfigList(10L, "SCHEDULE"))
                .thenReturn(List.of(useProcess(200L)));
        when(routeScheduleConfigService.getConfigRespListByRouteVersionId(100L))
                .thenReturn(List.of(scheduleConfig(200L)));
        when(routeResourceService.getResourcePage(any()))
                .thenReturn(new PageResult<>(List.of(resourceRow()), 1L));

        byte[] exported = service.exportPackage();
        JsonNode root = objectMapper.readTree(new String(exported, StandardCharsets.UTF_8));

        assertEquals("scheduler-route-config.v1", root.path("packageVersion").asText());
        assertEquals(1, root.path("routes").size());
        JsonNode routeNode = root.path("routes").get(0);
        assertEquals(10L, routeNode.path("routeId").asLong());
        assertEquals(1, routeNode.path("useProcessConfigs").size());
        assertEquals(1, routeNode.path("scheduleConfigs").size());
        assertEquals(1, routeNode.path("resources").size());
    }

    @Test
    void importPackage_shouldFailFastWhenRoutesMissing() {
        byte[] invalid = "{\"packageVersion\":\"scheduler-route-config.v1\",\"routes\":[]}"
                .getBytes(StandardCharsets.UTF_8);

        assertServiceException(() -> service.importPackage(invalid),
                CONFIG_PACKAGE_CONTENT_INVALID, "排产路线配置包 routes 不能为空");
    }

    @Test
    void importPackage_shouldExposeSpecificErrorWhenRouteMissing() throws Exception {
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage payload =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage();
        payload.setPackageVersion("scheduler-route-config.v1");
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload route =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload();
        route.setRouteId(10L);
        route.setRouteCode("ROUTE-404");
        route.setRouteVersionId(100L);
        route.setUseProcessConfigs(List.of());
        route.setScheduleConfigs(List.of());
        route.setResources(List.of());
        payload.setRoutes(List.of(route));
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(), 0L));

        assertServiceException(() -> service.importPackage(objectMapper.writeValueAsBytes(payload)),
                CONFIG_PACKAGE_REFERENCE_MISSING,
                "路线编码【ROUTE-404】在目标环境不存在");
    }

    @Test
    void importPackage_shouldExposeSpecificErrorWhenWorkstationMissing() throws Exception {
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage payload =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage();
        payload.setPackageVersion("scheduler-route-config.v1");
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload route =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload();
        route.setRouteId(10L);
        route.setRouteCode("ROUTE-001");
        route.setRouteVersionId(100L);
        route.setUseProcessConfigs(List.of(useProcess(200L).setProcessCode("PROC-A").setSort(1)));
        route.setScheduleConfigs(List.of(scheduleConfig(200L)));
        route.setResources(List.of(workerResourceRow()));
        payload.setRoutes(List.of(route));

        MesProRouteRespVO targetRoute = new MesProRouteRespVO()
                .setId(110L)
                .setCode("ROUTE-001")
                .setActiveRouteVersionId(1010L);
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(targetRoute), 1L));
        when(routeProcessMapper.selectByRouteIdAndSort(110L, 1))
                .thenReturn(MesProRouteProcessDO.builder().id(1200L).routeId(110L).processId(310L).sort(1).build());
        when(workstationMapper.selectByCode("WS-001")).thenReturn(null);

        assertServiceException(() -> service.importPackage(objectMapper.writeValueAsBytes(payload)),
                CONFIG_PACKAGE_REFERENCE_MISSING,
                "工位编码【WS-001】在目标环境不存在");
    }

    @Test
    void importPackage_shouldReplayUseScheduleAndResourceConfigs() throws Exception {
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage payload =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage();
        payload.setPackageVersion("scheduler-route-config.v1");
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload route =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload();
        route.setRouteId(10L);
        route.setRouteCode("ROUTE-001");
        route.setRouteVersionId(100L);
        route.setUseProcessConfigs(List.of(useProcess(200L).setProcessCode("PROC-A").setSort(1)));
        route.setScheduleConfigs(List.of(scheduleConfig(200L)));
        route.setResources(List.of(resourceRow()));
        payload.setRoutes(List.of(route));

        MesProRouteRespVO targetRoute = new MesProRouteRespVO()
                .setId(10L)
                .setCode("ROUTE-001")
                .setActiveRouteVersionId(100L);
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(targetRoute), 1L));
        when(routeProcessMapper.selectByRouteIdAndSort(10L, 1))
                .thenReturn(MesProRouteProcessDO.builder().id(200L).routeId(10L).processId(210L).sort(1).build());
        when(workstationMapper.selectByCode("WS-001"))
                .thenReturn(MesMdWorkstationDO.builder().id(400L).code("WS-001").build());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(401L).workstationId(400L).postId(1800L).build()));

        MesProSchedulerWorkbenchRouteConfigImportRespVO respVO =
                service.importPackage(objectMapper.writeValueAsBytes(payload));

        ArgumentCaptor<MesProRouteFlowConfigSaveReqVO> useConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowConfigSaveReqVO.class);
        ArgumentCaptor<MesProRouteScheduleConfigSaveReqVO> scheduleCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigSaveReqVO.class);
        verify(routeFlowConfigService).saveRouteFlowConfigForConfigPackageImport(useConfigCaptor.capture());
        verify(routeScheduleConfigService).saveConfig(scheduleCaptor.capture());
        assertEquals(10L, useConfigCaptor.getValue().getRouteId());
        assertEquals("由排产员工作台路线配置包导入", useConfigCaptor.getValue().getRemark());
        assertEquals("MANUAL_OVERRIDE", scheduleCaptor.getValue().getCapacityMode());
        assertEquals(new BigDecimal("25"), scheduleCaptor.getValue().getHourlyCapacity());
        assertEquals("历史小时产能已按产能覆盖口径导入", scheduleCaptor.getValue().getRemark());
        assertFalse(scheduleCaptor.getValue().getRemark().contains("LEGACY"));
        assertFalse(scheduleCaptor.getValue().getRemark().contains("Imported"));
        assertEquals(1, respVO.getRouteCount());
        assertEquals(1, respVO.getFlowConfigProcessCount());
        assertEquals(1, respVO.getScheduleConfigCount());
        assertEquals(1, respVO.getResourceCount());
    }

    @Test
    void importPackage_shouldMapCrossTenantRouteProcessAndResourcesByBusinessKeys() throws Exception {
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage payload =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage();
        payload.setPackageVersion("scheduler-route-config.v1");
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload route =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload();
        route.setRouteId(10L);
        route.setRouteCode("ROUTE-001");
        route.setRouteVersionId(100L);
        route.setUseProcessConfigs(List.of(useProcess(200L).setProcessCode("PROC-A").setSort(1)));
        route.setScheduleConfigs(List.of(scheduleConfig(200L)));
        route.setResources(List.of(workerResourceRow(), machineResourceRow()));
        payload.setRoutes(List.of(route));

        MesProRouteRespVO targetRoute = new MesProRouteRespVO()
                .setId(110L)
                .setCode("ROUTE-001")
                .setActiveRouteVersionId(1010L);
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(targetRoute), 1L));
        when(routeProcessMapper.selectByRouteIdAndSort(110L, 1))
                .thenReturn(MesProRouteProcessDO.builder().id(1200L).routeId(110L).processId(310L).sort(1).build());
        when(workstationMapper.selectByCode("WS-001"))
                .thenReturn(MesMdWorkstationDO.builder().id(1400L).code("WS-001").build());
        when(machineryMapper.selectByCode("MC-001"))
                .thenReturn(MesDvMachineryDO.builder().id(1500L).code("MC-001").build());
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().id(1600L).workstationId(1400L).machineryId(1500L).build()));
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder().id(1700L).workstationId(1400L).postId(1800L).build()));

        MesProSchedulerWorkbenchRouteConfigImportRespVO respVO =
                service.importPackage(objectMapper.writeValueAsBytes(payload));

        ArgumentCaptor<MesProRouteFlowConfigSaveReqVO> useConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowConfigSaveReqVO.class);
        ArgumentCaptor<MesProRouteScheduleConfigSaveReqVO> scheduleCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigSaveReqVO.class);
        verify(routeFlowConfigService).saveRouteFlowConfigForConfigPackageImport(useConfigCaptor.capture());
        verify(routeScheduleConfigService).saveConfig(scheduleCaptor.capture());

        assertEquals(110L, useConfigCaptor.getValue().getRouteId());
        assertEquals(1200L, useConfigCaptor.getValue().getProcessConfigs().get(0).getRouteProcessId());
        assertEquals(null, scheduleCaptor.getValue().getId());
        assertEquals(1010L, scheduleCaptor.getValue().getRouteVersionId());
        assertEquals(1200L, scheduleCaptor.getValue().getRouteProcessId());
        assertEquals(1, respVO.getRouteCount());
        assertEquals(1, respVO.getFlowConfigProcessCount());
        assertEquals(1, respVO.getScheduleConfigCount());
        assertEquals(2, respVO.getResourceCount());
    }

    @Test
    void importPackage_shouldFailFastWhenTargetPostBindingMissing() throws Exception {
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage payload =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage();
        payload.setPackageVersion("scheduler-route-config.v1");
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload route =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload();
        route.setRouteId(10L);
        route.setRouteCode("ROUTE-001");
        route.setRouteVersionId(100L);
        route.setUseProcessConfigs(List.of(useProcess(200L).setProcessCode("PROC-A").setSort(1)));
        route.setScheduleConfigs(List.of(scheduleConfig(200L)));
        route.setResources(List.of(workerResourceRow()));
        payload.setRoutes(List.of(route));

        MesProRouteRespVO targetRoute = new MesProRouteRespVO()
                .setId(110L)
                .setCode("ROUTE-001")
                .setActiveRouteVersionId(1010L);
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(targetRoute), 1L));
        when(routeProcessMapper.selectByRouteIdAndSort(110L, 1))
                .thenReturn(MesProRouteProcessDO.builder().id(1200L).routeId(110L).processId(310L).sort(1).build());
        when(workstationMapper.selectByCode("WS-001"))
                .thenReturn(MesMdWorkstationDO.builder().id(1400L).code("WS-001").build());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of());

        assertServiceException(() -> service.importPackage(objectMapper.writeValueAsBytes(payload)),
                CONFIG_PACKAGE_REFERENCE_MISSING,
                "工位编码【WS-001】未绑定岗位【1800】");
    }

    @Test
    void importPackage_shouldFailFastWhenWorkerBindingMissingAndSourcePostIdMissing() throws Exception {
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage payload =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage();
        payload.setPackageVersion("scheduler-route-config.v1");
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload route =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload();
        route.setRouteId(10L);
        route.setRouteCode("ROUTE-001");
        route.setRouteVersionId(100L);
        route.setUseProcessConfigs(List.of(useProcess(200L).setProcessCode("PROC-A").setSort(1)));
        route.setScheduleConfigs(List.of(scheduleConfig(200L)));
        route.setResources(List.of(workerResourceRow().setPostId(null)));
        payload.setRoutes(List.of(route));

        MesProRouteRespVO targetRoute = new MesProRouteRespVO()
                .setId(110L)
                .setCode("ROUTE-001")
                .setActiveRouteVersionId(1010L);
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(targetRoute), 1L));
        when(routeProcessMapper.selectByRouteIdAndSort(110L, 1))
                .thenReturn(MesProRouteProcessDO.builder().id(1200L).routeId(110L).processId(310L).sort(1).build());
        when(workstationMapper.selectByCode("WS-001"))
                .thenReturn(MesMdWorkstationDO.builder().id(1400L).code("WS-001").build());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of());

        assertServiceException(() -> service.importPackage(objectMapper.writeValueAsBytes(payload)),
                CONFIG_PACKAGE_REFERENCE_MISSING,
                "工位编码【WS-001】未绑定人力资源");
    }

    @Test
    void importPackage_shouldResolveProcessByRouteSortWhenCodeMatchesMultipleCandidates() throws Exception {
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage payload =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigPackage();
        payload.setPackageVersion("scheduler-route-config.v1");
        MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload route =
                new MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.RouteConfigRoutePayload();
        route.setRouteId(10L);
        route.setRouteCode("ROUTE-001");
        route.setRouteVersionId(100L);
        route.setUseProcessConfigs(List.of(useProcess(200L).setProcessCode("PROC-A").setSort(1)));
        route.setScheduleConfigs(List.of(scheduleConfig(200L)));
        route.setResources(List.of(machineResourceRow()));
        payload.setRoutes(List.of(route));

        MesProRouteRespVO targetRoute = new MesProRouteRespVO()
                .setId(110L)
                .setCode("ROUTE-001")
                .setActiveRouteVersionId(1010L);
        MesProRouteProcessDO targetRouteProcess = MesProRouteProcessDO.builder()
                .id(1200L).routeId(110L).processId(310L).sort(1).build();
        when(routeService.getRoutePageRespVO(any())).thenReturn(new PageResult<>(List.of(targetRoute), 1L));
        when(routeProcessMapper.selectByRouteIdAndSort(110L, 1)).thenReturn(targetRouteProcess);
        when(processMapper.selectById(310L)).thenReturn(MesProProcessDO.builder().id(310L).code("PROC-A").build());
        when(workstationMapper.selectByCode("WS-001"))
                .thenReturn(MesMdWorkstationDO.builder().id(1400L).code("WS-001").build());
        when(machineryMapper.selectByCode("MC-001"))
                .thenReturn(MesDvMachineryDO.builder().id(1500L).code("MC-001").build());
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().id(1600L).workstationId(1400L).machineryId(1500L).build()));
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of());

        service.importPackage(objectMapper.writeValueAsBytes(payload));

        ArgumentCaptor<MesProRouteScheduleConfigSaveReqVO> scheduleCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigSaveReqVO.class);
        verify(routeScheduleConfigService).saveConfig(scheduleCaptor.capture());
        assertEquals(1200L, scheduleCaptor.getValue().getRouteProcessId());
    }

    private MesProRouteFlowProcessConfigRespVO useProcess(Long routeProcessId) {
        MesProRouteFlowProcessConfigRespVO vo = new MesProRouteFlowProcessConfigRespVO();
        vo.setRouteProcessId(routeProcessId);
        vo.setEnabled(true);
        vo.setRemark("schedule");
        return vo;
    }

    private MesProRouteScheduleConfigRespVO scheduleConfig(Long routeProcessId) {
        MesProRouteScheduleConfigRespVO vo = new MesProRouteScheduleConfigRespVO();
        vo.setId(300L);
        vo.setRouteProcessId(routeProcessId);
        vo.setRouteVersionId(100L);
        vo.setCapacityMode("FINITE_HOURLY");
        vo.setHourlyCapacity(new BigDecimal("25"));
        vo.setNightShiftEnabled(false);
        vo.setConfigVersion("CFG-1");
        return vo;
    }

    private MesProRouteResourceRespVO resourceRow() {
        MesProRouteResourceRespVO vo = new MesProRouteResourceRespVO();
        vo.setResourceType("WORKER");
        vo.setRouteProcessId(200L);
        vo.setProcessCode("PROC-A");
        vo.setSort(1);
        vo.setWorkstationCode("WS-001");
        vo.setWorkstationId(400L);
        vo.setWorkstationWorkerId(401L);
        vo.setPostId(1800L);
        vo.setWorkerQuantity(5);
        vo.setSingleStandardHourlyCapacity(new BigDecimal("30"));
        return vo;
    }

    private MesProRouteResourceRespVO workerResourceRow() {
        MesProRouteResourceRespVO vo = resourceRow();
        vo.setRouteProcessId(200L);
        vo.setProcessCode("PROC-A");
        vo.setSort(1);
        vo.setWorkstationCode("WS-001");
        return vo;
    }

    private MesProRouteResourceRespVO machineResourceRow() {
        MesProRouteResourceRespVO vo = new MesProRouteResourceRespVO();
        vo.setResourceType("MACHINE");
        vo.setRouteProcessId(200L);
        vo.setProcessCode("PROC-A");
        vo.setSort(1);
        vo.setWorkstationId(500L);
        vo.setWorkstationCode("WS-001");
        vo.setWorkstationMachineId(501L);
        vo.setMachineryId(600L);
        vo.setMachineryCode("MC-001");
        vo.setMachineryQuantity(2);
        vo.setMachineryStandardHourlyCapacity(new BigDecimal("40"));
        return vo;
    }
}
