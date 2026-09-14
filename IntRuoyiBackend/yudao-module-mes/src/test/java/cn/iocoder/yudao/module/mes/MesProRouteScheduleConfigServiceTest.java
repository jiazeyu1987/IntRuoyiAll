package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteResourceCapacityPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteCandidateConfigService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator;
import cn.iocoder.yudao.module.mes.enums.cal.MesCalPlanStatusEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_CALENDAR_RULE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_INFINITE_DURATION_FORMULA_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_MANUAL_CAPACITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteScheduleConfigServiceTest {

    @InjectMocks
    private MesProRouteScheduleConfigServiceImpl service;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Mock
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private MesDvMachineryService machineryService;
    @Mock
    private MesDvMachineryProcessService machineryProcessService;
    @Mock
    private MesMdProductionLineService productionLineService;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Mock
    private MesCalPlanShiftService planShiftService;
    @Mock
    private MesCalPlanService planService;
    @Spy
    private CapacityWindowAllocator capacityWindowAllocator = new CapacityWindowAllocator();

    @BeforeEach
    void stubCurrentRouteProcessIdentity() {
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        lenient().when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(version());
    }

    @Test
    void getConfigListByRouteVersionId_shouldNormalizeHistoricalRouteProcessId() {
        MesProRouteScheduleConfigDO historicalConfig = MesProRouteScheduleConfigDO.builder()
                .id(700L)
                .routeVersionId(100L)
                .routeProcessId(199L)
                .build();
        when(routeVersionMapper.selectById(100L)).thenReturn(version());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(100L))
                .thenReturn(List.of(historicalConfig));
        when(routeProcessService.resolveCurrentRouteProcess(199L, 10L, null))
                .thenReturn(process());

        List<MesProRouteScheduleConfigDO> result = service.getConfigListByRouteVersionId(100L);

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getRouteProcessId());
    }

    @Test
    void saveConfig_shouldUpdateHistoricalRouteProcessConfigInsteadOfRejectingIt() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion("""
                {
                  "199": {
                    "id": 700,
                    "routeVersionId": 100,
                    "routeProcessId": 199,
                    "capacityMode": "RESOURCE_CALCULATED",
                    "nightShiftEnabled": false,
                    "configVersion": "CFG-OLD"
                  }
                }
                """));
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());
        when(routeProcessService.resolveCurrentRouteProcess(199L, 10L, null))
                .thenReturn(process());

        Long result = service.saveConfig(reqVO);

        assertEquals(100L, result);
        Map<?, ?> snapshot = captureScheduleSnapshot();
        assertFalse(snapshot.containsKey("199"));
        Map<?, ?> saved = (Map<?, ?>) snapshot.get("200");
        assertEquals(700L, ((Number) saved.get("id")).longValue());
        assertEquals(200L, ((Number) saved.get("routeProcessId")).longValue());
        assertEquals("MANUAL_OVERRIDE", saved.get("capacityMode"));
        assertEquals(0, ((BigDecimal) saved.get("hourlyCapacity")).compareTo(new BigDecimal("12")));
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldWriteDraftCandidateScheduleSnapshotWithoutUpdatingActiveConfig() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setRouteVersionId(1002L);
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "routeCode": "R-10",
                          "routeName": "测试路线",
                          "configSnapshots": {
                            "flowGraph": {"nodes": [
                              {"routeProcessId": 200, "processId": 300, "sort": 1},
                              {"routeProcessId": 201, "processId": 301, "sort": 2}
                            ]},
                            "products": [],
                            "scheduleConfigs": {
                              "200": {
                                "routeVersionId": 1002,
                                "routeProcessId": 200,
                                "capacityMode": "RESOURCE_CALCULATED",
                                "nightShiftEnabled": false,
                                "configVersion": "CFG-OLD"
                              },
                              "201": {
                                "routeVersionId": 1002,
                                "routeProcessId": 201,
                                "capacityMode": "RESOURCE_CALCULATED",
                                "nightShiftEnabled": false,
                                "configVersion": "CFG-KEEP"
                              }
                            },
                            "batchUseConfigs": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(routeVersionMapper.selectById(1002L)).thenReturn(candidate);
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        Long result = service.saveConfig(reqVO);

        assertEquals(1002L, result);
        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("scheduleConfigs"),
                snapshotCaptor.capture());
        Map<?, ?> snapshot = (Map<?, ?>) snapshotCaptor.getValue();
        assertTrue(snapshot.containsKey("200"));
        assertTrue(snapshot.containsKey("201"));
        Map<?, ?> updatedConfig = (Map<?, ?>) snapshot.get("200");
        assertEquals("MANUAL_OVERRIDE", updatedConfig.get("capacityMode"));
        assertEquals(0, ((BigDecimal) updatedConfig.get("hourlyCapacity")).compareTo(new BigDecimal("12")));
        Map<?, ?> unchangedConfig = (Map<?, ?>) snapshot.get("201");
        assertEquals("CFG-KEEP", unchangedConfig.get("configVersion"));
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
        verify(scheduleOrderProcessMapper, never()).updateById(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void getConfigRespListByRouteVersionId_shouldReadDraftCandidateScheduleSnapshot() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "routeCode": "R-10",
                          "routeName": "测试路线",
                          "configSnapshots": {
                            "flowGraph": {"nodes": [
                              {"routeProcessId": 200, "processId": 300, "sort": 1}
                            ]},
                            "products": [],
                            "scheduleConfigs": {
                              "200": {
                                "routeVersionId": 1002,
                                "routeProcessId": 200,
                                "capacityMode": "MANUAL_OVERRIDE",
                                "hourlyCapacity": 12,
                                "nightShiftEnabled": false,
                                "configVersion": "CFG-DRAFT",
                                "remark": "draft override"
                              }
                            },
                            "batchUseConfigs": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(routeVersionMapper.selectById(1002L)).thenReturn(candidate);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(process()));
        when(workstationService.getWorkstationList(Set.of(800L)))
                .thenReturn(List.of(MesMdWorkstationDO.builder()
                        .id(800L)
                        .processId(300L)
                        .shiftHours(new BigDecimal("10"))
                        .build()));

        List<MesProRouteScheduleConfigRespVO> rows = service.getConfigRespListByRouteVersionId(1002L);

        assertEquals(1, rows.size());
        assertEquals(1002L, rows.get(0).getRouteVersionId());
        assertEquals(200L, rows.get(0).getRouteProcessId());
        assertEquals("MANUAL_OVERRIDE", rows.get(0).getCapacityMode());
        assertEquals(0, rows.get(0).getHourlyCapacity().compareTo(new BigDecimal("12")));
        assertEquals(0, rows.get(0).getShiftHours().compareTo(new BigDecimal("10")));
        assertEquals(0, rows.get(0).getStandardShiftCapacity().compareTo(new BigDecimal("120")));
        verify(routeScheduleConfigMapper, never()).selectListByRouteVersionId(1002L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING_APPROVAL", "READY_TO_PUBLISH", "REJECTED", "CANCELLED"})
    void getConfigRespListByRouteVersionId_shouldReadCandidateScheduleSnapshotForReadonlyStatuses(
            String lifecycleStatus) {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1003L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(lifecycleStatus)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "routeCode": "R-10",
                          "routeName": "测试路线",
                          "configSnapshots": {
                            "flowGraph": {"nodes": [
                              {"routeProcessId": 200, "processId": 300, "sort": 1}
                            ]},
                            "products": [],
                            "scheduleConfigs": {
                              "200": {
                                "routeVersionId": 1003,
                                "routeProcessId": 200,
                                "capacityMode": "MANUAL_OVERRIDE",
                                "hourlyCapacity": 15,
                                "nightShiftEnabled": false,
                                "configVersion": "CFG-READONLY",
                                "remark": "readonly snapshot"
                              }
                            },
                            "batchUseConfigs": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(routeVersionMapper.selectById(1003L)).thenReturn(candidate);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(process()));
        when(workstationService.getWorkstationList(Set.of(800L)))
                .thenReturn(List.of(MesMdWorkstationDO.builder()
                        .id(800L)
                        .processId(300L)
                        .shiftHours(new BigDecimal("8"))
                        .build()));

        List<MesProRouteScheduleConfigRespVO> rows = service.getConfigRespListByRouteVersionId(1003L);

        assertEquals(1, rows.size());
        assertEquals(1003L, rows.get(0).getRouteVersionId());
        assertEquals(200L, rows.get(0).getRouteProcessId());
        assertEquals("MANUAL_OVERRIDE", rows.get(0).getCapacityMode());
        assertEquals(0, rows.get(0).getHourlyCapacity().compareTo(new BigDecimal("15")));
        assertEquals(0, rows.get(0).getShiftHours().compareTo(new BigDecimal("8")));
        assertEquals(0, rows.get(0).getStandardShiftCapacity().compareTo(new BigDecimal("120")));
        verify(routeScheduleConfigMapper, never()).selectListByRouteVersionId(1003L);
    }

    @Test
    void saveConfig_shouldRejectDraftLegacyFiniteHourlyModeAfterCapacityModeUnification() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setRouteVersionId(1002L);
        reqVO.setCapacityMode("FINITE_HOURLY");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .build();
        when(routeVersionMapper.selectById(1002L)).thenReturn(candidate);
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));

        assertEquals(PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID.getCode(), ex.getCode());
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(anyLong(), anyString(), any());
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldRejectActiveRouteVersionWithoutActiveWrite() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setRouteVersionId(99L);
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        when(routeVersionMapper.selectById(99L)).thenReturn(
                MesProRouteVersionDO.builder().id(99L).routeId(10L).active(Boolean.TRUE).build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));

        assertEquals(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(anyLong(), anyString(), any());
    }

    @Test
    void saveConfig_shouldRejectCancelledRouteVersionWithoutWrite() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setRouteVersionId(99L);
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        when(routeVersionMapper.selectById(99L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));

        assertEquals(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(anyLong(), anyString(), any());
    }

    @Test
    void saveConfig_shouldRejectFiniteCapacityWhenHourlyCapacityIsNotPositive() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(BigDecimal.ZERO);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));

        assertEquals(PRO_ROUTE_SCHEDULE_MANUAL_CAPACITY_REQUIRED.getCode(), ex.getCode());
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldRejectLegacyFiniteHourlyModeAfterCapacityModeUnification() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("FINITE_HOURLY");
        reqVO.setHourlyCapacity(new BigDecimal("12"));

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));

        assertEquals(PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID.getCode(), ex.getCode());
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldRejectMissingNightShiftFlag() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        reqVO.setNightShiftEnabled(null);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));

        assertEquals(1_040_271_043, ex.getCode());
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void getResourcePreview_shouldReturnMachineCapacityBlockerWhenMachineryProcessCapacityMissing() {
        when(routeProcessService.getRouteProcess(200L)).thenReturn(process());
        when(workstationService.getWorkstation(800L))
                .thenReturn(MesMdWorkstationDO.builder()
                        .id(800L)
                        .code("WS-01")
                        .name("焊接工作站")
                        .processId(300L)
                        .productionLineId(600L)
                        .shiftHours(new BigDecimal("10.5"))
                        .build());
        when(productionLineService.getProductionLineMap(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of(600L, MesMdProductionLineDO.builder()
                        .id(600L)
                        .name("一号产线")
                        .build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder()
                        .id(900L)
                        .workstationId(800L)
                        .machineryId(901L)
                        .quantity(2)
                        .build()));
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(machineryService.getMachineryMap(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of(901L, MesDvMachineryDO.builder()
                        .id(901L)
                        .code("EQ-01")
                        .name("焊接机")
                        .build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        MesProRouteResourceCapacityPreviewRespVO preview = service.getResourcePreview(200L);

        assertEquals(200L, preview.getRouteProcessId());
        assertEquals(300L, preview.getProcessId());
        assertEquals("MACHINE", preview.getCapacitySource());
        assertEquals(BigDecimal.ZERO, preview.getResourceCapacityHourly());
        assertEquals(1, preview.getWorkstationRows().size());
        assertEquals("一号产线", preview.getWorkstationRows().get(0).getProductionLineName());
        assertEquals(1, preview.getBlockingIssues().size());
        assertEquals("BLOCKED_NO_MACHINERY_PROCESS_CAPACITY", preview.getBlockingIssues().get(0).getCode());
        assertEquals(901L, preview.getBlockingIssues().get(0).getMachineryId());
    }

    @Test
    void getResourcePreview_shouldAcceptWorkstationProcessAliasAndUseCanonicalMachineCapacity() {
        when(routeProcessService.getRouteProcess(200L)).thenReturn(process());
        when(workstationService.getWorkstation(800L))
                .thenReturn(MesMdWorkstationDO.builder()
                        .id(800L)
                        .code("WS-ALIAS")
                        .name("历史工序工作站")
                        .processId(301L)
                        .productionLineId(600L)
                        .shiftHours(new BigDecimal("10"))
                        .build());
        when(productionLineService.getProductionLineMap(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of(600L, MesMdProductionLineDO.builder()
                        .id(600L)
                        .name("一号产线")
                        .build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder()
                        .id(900L)
                        .workstationId(800L)
                        .machineryId(901L)
                        .quantity(2)
                        .build()));
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(machineryService.getMachineryMap(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of(901L, MesDvMachineryDO.builder()
                        .id(901L)
                        .code("EQ-01")
                        .name("焊接机")
                        .build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(MesDvMachineryProcessDO.builder()
                        .id(701L)
                        .machineryId(901L)
                        .processId(300L)
                        .standardHourlyCapacity(new BigDecimal("12"))
                        .build()));

        MesProRouteResourceCapacityPreviewRespVO preview = service.getResourcePreview(200L);

        assertEquals("MACHINE", preview.getCapacitySource());
        assertEquals(0, preview.getResourceCapacityHourly().compareTo(new BigDecimal("24")));
        assertTrue(preview.getBlockingIssues().isEmpty());
        assertEquals(0, preview.getWorkstationRows().get(0).getHourlyCapacity().compareTo(new BigDecimal("24")));
    }

    @Test
    void getResourcePreview_shouldUseManualWorkerCapacityWhenNoMachineBindingsExist() {
        when(routeProcessService.getRouteProcess(200L)).thenReturn(process());
        when(workstationService.getWorkstation(800L))
                .thenReturn(MesMdWorkstationDO.builder()
                        .id(800L)
                        .code("WS-02")
                        .name("人工工作站")
                        .processId(300L)
                        .productionLineId(600L)
                        .singleStandardHourlyCapacity(new BigDecimal("4"))
                        .shiftHours(new BigDecimal("10.5"))
                        .build());
        when(productionLineService.getProductionLineMap(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of(600L, MesMdProductionLineDO.builder()
                        .id(600L)
                        .name("一号产线")
                        .build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(910L)
                        .workstationId(800L)
                        .quantity(null)
                        .build()));
        when(machineryService.getMachineryMap(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of());
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        MesProRouteResourceCapacityPreviewRespVO preview = service.getResourcePreview(200L);

        assertEquals("WORKER", preview.getCapacitySource());
        assertEquals(0, preview.getResourceCapacityHourly().compareTo(new BigDecimal("4")));
        assertEquals(0, preview.getBlockingIssues().size());
        assertEquals(0, preview.getWorkstationRows().get(0).getWorkerQuantity());
    }

    @Test
    void saveConfig_shouldAcceptManualOverrideAndKeepHourlyCapacityAsExplicitOverride() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        reqVO.setNightShiftEnabled(Boolean.FALSE);
        reqVO.setInfiniteDurationQuantityFactor(new BigDecimal("2"));
        reqVO.setInfiniteDurationBaseMinutes(new BigDecimal("8"));

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        service.saveConfig(reqVO);

        Map<?, ?> saved = (Map<?, ?>) captureScheduleSnapshot().get("200");
        assertEquals("MANUAL_OVERRIDE", saved.get("capacityMode"));
        assertEquals(0, ((BigDecimal) saved.get("hourlyCapacity")).compareTo(new BigDecimal("12")));
        assertNull(saved.get("infiniteDurationQuantityFactor"));
        assertNull(saved.get("infiniteDurationBaseMinutes"));
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void getConfigRespListByRouteVersionId_shouldExposeShiftCapacityForManualOverrideConfig() {
        when(routeVersionMapper.selectById(100L)).thenReturn(version());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(100L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(700L)
                        .routeVersionId(100L)
                        .routeProcessId(200L)
                        .capacityMode("MANUAL_OVERRIDE")
                        .hourlyCapacity(new BigDecimal("47.619048"))
                        .build()
        ));
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(process()));
        when(workstationService.getWorkstationList(Set.of(800L)))
                .thenReturn(List.of(MesMdWorkstationDO.builder()
                        .id(800L)
                        .processId(300L)
                        .shiftHours(new BigDecimal("10.5"))
                        .build()));

        List<MesProRouteScheduleConfigRespVO> rows = service.getConfigRespListByRouteVersionId(100L);

        assertEquals(1, rows.size());
        assertEquals(0, rows.get(0).getShiftHours().compareTo(new BigDecimal("10.5")));
        assertEquals(0, rows.get(0).getStandardShiftCapacity().compareTo(new BigDecimal("500.0000040")));
    }

    @Test
    void saveConfig_shouldRejectInfiniteFormulaWhenFactorOrBaseIsInvalid() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("INFINITE_FORMULA");
        reqVO.setInfiniteDurationQuantityFactor(new BigDecimal("-1"));
        reqVO.setInfiniteDurationBaseMinutes(new BigDecimal("5"));

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));

        assertEquals(PRO_ROUTE_SCHEDULE_INFINITE_DURATION_FORMULA_REQUIRED.getCode(), ex.getCode());
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldUpdateInfiniteFormulaAndNightShiftRequirementWithoutItemDimension() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setId(108L);
        reqVO.setItemId(9001L);
        reqVO.setCapacityMode("INFINITE_FORMULA");
        reqVO.setInfiniteDurationQuantityFactor(new BigDecimal("2.5"));
        reqVO.setInfiniteDurationBaseMinutes(new BigDecimal("8"));
        reqVO.setNightShiftEnabled(Boolean.TRUE);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());
        MesProScheduleCalendarRuleDO calendarRule = MesProScheduleCalendarRuleDO.builder().id(900L).build();
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(calendarRule);
        when(scheduleCalendarRuleMapper.selectById(900L)).thenReturn(calendarRule);
        stubAvailableNightShift();

        TenantContextHolder.setTenantId(1L);
        try {
            service.saveConfig(reqVO);
        } finally {
            TenantContextHolder.clear();
        }

        Map<?, ?> saved = (Map<?, ?>) captureScheduleSnapshot().get("200");
        assertFalse(saved.containsKey("itemId"));
        assertEquals("INFINITE_FORMULA", saved.get("capacityMode"));
        assertEquals(0, ((BigDecimal) saved.get("infiniteDurationQuantityFactor")).compareTo(new BigDecimal("2.5")));
        assertEquals(0, ((BigDecimal) saved.get("infiniteDurationBaseMinutes")).compareTo(new BigDecimal("8")));
        assertEquals(Boolean.TRUE, saved.get("nightShiftEnabled"));
        assertEquals(900L, ((Number) saved.get("calendarRuleId")).longValue());
        assertEquals("DAY_AND_NIGHT", saved.get("remark"));
        assertEquals(700L, ((Number) saved.get("id")).longValue());
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldRejectNightShiftWhenProductionLineHasNoNightShift() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        reqVO.setNightShiftEnabled(Boolean.TRUE);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());
        MesProScheduleCalendarRuleDO calendarRule = MesProScheduleCalendarRuleDO.builder().id(900L).build();
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(calendarRule);
        MesMdWorkstationDO workstation = nightShiftWorkstation();
        when(workstationService.getWorkstation(800L)).thenReturn(workstation);
        when(productionLineService.getProductionLine(1000L)).thenReturn(MesMdProductionLineDO.builder()
                .id(1000L)
                .name("吹球囊成型线")
                .calendarPlanId(1100L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        when(planService.getPlan(1100L)).thenReturn(MesCalPlanDO.builder()
                .id(1100L)
                .status(MesCalPlanStatusEnum.CONFIRMED.getStatus())
                .build());
        when(planShiftService.getPlanShiftListByPlanId(1100L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(1200L).name("白班").sort(1)
                        .startTime("08:00").endTime("16:00").build()));

        TenantContextHolder.setTenantId(1L);
        ServiceException ex;
        try {
            ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("吹球囊成型线"));
        assertTrue(ex.getMessage().contains("夜班班次"));
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(anyLong(), anyString(), any());
    }

    @Test
    void saveConfig_shouldRejectNightShiftWhenFutureNightCapacityPlanIsMissing() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        reqVO.setNightShiftEnabled(Boolean.TRUE);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());
        MesProScheduleCalendarRuleDO calendarRule = MesProScheduleCalendarRuleDO.builder().id(900L).build();
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(calendarRule);
        stubAvailableNightShift();
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        TenantContextHolder.setTenantId(1L);
        ServiceException ex;
        try {
            ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("夜班班次"));
        assertTrue(ex.getMessage().contains("产能计划"));
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(anyLong(), anyString(), any());
    }

    @Test
    void saveConfig_shouldRejectResourceCalculatedNightShiftWhenMachineProcessCapacityIsMissing() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("RESOURCE_CALCULATED");
        reqVO.setNightShiftEnabled(Boolean.TRUE);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());
        MesProScheduleCalendarRuleDO calendarRule = MesProScheduleCalendarRuleDO.builder().id(900L).build();
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(calendarRule);
        stubAvailableNightShift();
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(Set.of(800L))).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().id(1300L).workstationId(800L).machineryId(1400L).quantity(1).build()));
        when(machineryService.getMachineryMap(Set.of(1400L))).thenReturn(Map.of(1400L,
                MesDvMachineryDO.builder().id(1400L).code("DEV-BALLOON").name("吹球囊设备").build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                Set.of(1400L), Set.of(300L))).thenReturn(List.of());
        TenantContextHolder.setTenantId(1L);
        ServiceException ex;
        try {
            ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("DEV-BALLOON"));
        assertTrue(ex.getMessage().contains("设备工序小时产能"));
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(anyLong(), anyString(), any());
    }

    @Test
    void saveConfig_shouldUpdateSameRouteProcessConfigAcrossDifferentItems() {
        MesProRouteScheduleConfigSaveReqVO firstReq = baseReq();
        firstReq.setItemId(9001L);
        firstReq.setCapacityMode("MANUAL_OVERRIDE");
        firstReq.setHourlyCapacity(new BigDecimal("12"));
        MesProRouteScheduleConfigSaveReqVO secondReq = baseReq();
        secondReq.setItemId(9002L);
        secondReq.setCapacityMode("MANUAL_OVERRIDE");
        secondReq.setHourlyCapacity(new BigDecimal("25"));
        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        service.saveConfig(firstReq);
        service.saveConfig(secondReq);

        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(routeCandidateConfigService, times(2)).saveConfigSnapshot(eq(100L), eq("scheduleConfigs"),
                snapshotCaptor.capture());
        Map<?, ?> firstSaved = (Map<?, ?>) ((Map<?, ?>) snapshotCaptor.getAllValues().get(0)).get("200");
        Map<?, ?> secondSaved = (Map<?, ?>) ((Map<?, ?>) snapshotCaptor.getAllValues().get(1)).get("200");
        assertEquals(700L, ((Number) firstSaved.get("id")).longValue());
        assertFalse(firstSaved.containsKey("itemId"));
        assertEquals(0, ((BigDecimal) firstSaved.get("hourlyCapacity")).compareTo(new BigDecimal("12")));
        assertEquals(700L, ((Number) secondSaved.get("id")).longValue());
        assertFalse(secondSaved.containsKey("itemId"));
        assertEquals(0, ((BigDecimal) secondSaved.get("hourlyCapacity")).compareTo(new BigDecimal("25")));
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldNotSynchronizeActiveWipSnapshotsForDraftCandidate() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        reqVO.setNightShiftEnabled(Boolean.FALSE);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        service.saveConfig(reqVO);

        Map<?, ?> saved = (Map<?, ?>) captureScheduleSnapshot().get("200");
        assertEquals("MANUAL_OVERRIDE", saved.get("capacityMode"));
        assertEquals(0, ((BigDecimal) saved.get("hourlyCapacity")).compareTo(new BigDecimal("12")));
        verify(scheduleOrderProcessMapper, never()).updateById(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void saveConfig_shouldSaveResourceCalculatedDraftSnapshotWithoutActiveWipSync() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("RESOURCE_CALCULATED");
        reqVO.setHourlyCapacity(new BigDecimal("99"));

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        service.saveConfig(reqVO);

        Map<?, ?> saved = (Map<?, ?>) captureScheduleSnapshot().get("200");
        assertEquals("RESOURCE_CALCULATED", saved.get("capacityMode"));
        assertNull(saved.get("hourlyCapacity"));
        verify(scheduleOrderProcessMapper, never()).updateById(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void saveConfig_shouldIgnoreItemThatDoesNotBelongToRoute() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setItemId(9999L);
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        service.saveConfig(reqVO);

        Map<?, ?> saved = (Map<?, ?>) captureScheduleSnapshot().get("200");
        assertFalse(saved.containsKey("itemId"));
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldCreateDraftCandidateConfigWhenSnapshotDoesNotContainProcess() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));

        when(routeVersionMapper.selectById(100L)).thenReturn(emptyDraftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());

        Long result = service.saveConfig(reqVO);

        assertEquals(100L, result);
        Map<?, ?> saved = (Map<?, ?>) captureScheduleSnapshot().get("200");
        assertEquals("MANUAL_OVERRIDE", saved.get("capacityMode"));
        assertEquals(0, ((BigDecimal) saved.get("hourlyCapacity")).compareTo(new BigDecimal("12")));
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void saveConfig_shouldReturnBusinessErrorWhenNightShiftCalendarRuleIsMissing() {
        MesProRouteScheduleConfigSaveReqVO reqVO = baseReq();
        reqVO.setCapacityMode("MANUAL_OVERRIDE");
        reqVO.setHourlyCapacity(new BigDecimal("12"));
        reqVO.setNightShiftEnabled(Boolean.TRUE);

        when(routeVersionMapper.selectById(100L)).thenReturn(draftVersion());
        when(routeProcessService.resolveCurrentRouteProcess(200L, 10L, null)).thenReturn(process());
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(null);

        TenantContextHolder.setTenantId(1L);
        ServiceException ex;
        try {
            ex = assertThrows(ServiceException.class, () -> service.saveConfig(reqVO));
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(PRO_ROUTE_SCHEDULE_CALENDAR_RULE_REQUIRED.getCode(), ex.getCode());
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void getConfigRespListByRouteVersionId_shouldExposeShiftHoursAndStandardShiftCapacityForFiniteConfig() {
        when(routeVersionMapper.selectById(100L)).thenReturn(version());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(100L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(700L)
                        .routeVersionId(100L)
                        .routeProcessId(200L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("47.619048"))
                        .build()
        ));
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(process()));
        when(workstationService.getWorkstationList(Set.of(800L)))
                .thenReturn(List.of(MesMdWorkstationDO.builder()
                        .id(800L)
                        .processId(300L)
                        .shiftHours(new BigDecimal("10.5"))
                        .build()));

        List<MesProRouteScheduleConfigRespVO> rows = service.getConfigRespListByRouteVersionId(100L);

        assertEquals(1, rows.size());
        assertEquals(0, rows.get(0).getShiftHours().compareTo(new BigDecimal("10.5")));
        assertEquals(0, rows.get(0).getStandardShiftCapacity().compareTo(new BigDecimal("500.0000040")));
    }

    @Test
    void routeScheduleConfigResponse_shouldNotExposeProductDimensionFields() {
        Set<String> fieldNames = Arrays.stream(MesProRouteScheduleConfigRespVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());

        assertFalse(fieldNames.contains("itemId"));
        assertFalse(fieldNames.contains("itemCode"));
        assertFalse(fieldNames.contains("itemName"));
        assertFalse(fieldNames.contains("itemSpecification"));
    }

    @Test
    void routeScheduleConfigResponseJson_shouldNotSerializeProductDimensionFields() {
        MesProRouteScheduleConfigRespVO row = new MesProRouteScheduleConfigRespVO();
        row.setId(700L);
        row.setRouteVersionId(100L);
        row.setRouteProcessId(200L);
        row.setCapacityMode("FINITE_HOURLY");
        row.setHourlyCapacity(new BigDecimal("47.619048"));

        JsonNode json = new ObjectMapper().valueToTree(row);

        assertEquals(200L, json.path("routeProcessId").asLong());
        assertFalse(json.has("itemId"));
        assertFalse(json.has("itemCode"));
        assertFalse(json.has("itemName"));
        assertFalse(json.has("itemSpecification"));
    }

    @Test
    void getConfigRespListByRouteVersionId_shouldExposeMissingShiftHoursWithoutSystemException() {
        when(routeVersionMapper.selectById(100L)).thenReturn(version());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(100L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(700L)
                        .routeVersionId(100L)
                        .routeProcessId(200L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("47.619048"))
                        .build()
        ));
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(process()));
        when(workstationService.getWorkstationList(Set.of(800L)))
                .thenReturn(List.of());

        List<MesProRouteScheduleConfigRespVO> rows = service.getConfigRespListByRouteVersionId(100L);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).getShiftHours());
        assertNull(rows.get(0).getStandardShiftCapacity());
    }

    @Test
    void getConfigRespListByRouteVersionId_shouldExposeInvalidShiftHoursAsMissing() {
        when(routeVersionMapper.selectById(100L)).thenReturn(version());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(100L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(700L)
                        .routeVersionId(100L)
                        .routeProcessId(200L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("47.619048"))
                        .build()
        ));
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(process()));
        when(workstationService.getWorkstationList(Set.of(800L)))
                .thenReturn(List.of(MesMdWorkstationDO.builder()
                        .id(800L)
                        .processId(300L)
                        .shiftHours(BigDecimal.ZERO)
                        .build()));

        List<MesProRouteScheduleConfigRespVO> rows = service.getConfigRespListByRouteVersionId(100L);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).getShiftHours());
        assertNull(rows.get(0).getStandardShiftCapacity());
    }

    private MesProRouteScheduleConfigSaveReqVO baseReq() {
        MesProRouteScheduleConfigSaveReqVO reqVO = new MesProRouteScheduleConfigSaveReqVO();
        reqVO.setRouteVersionId(100L);
        reqVO.setRouteProcessId(200L);
        reqVO.setConfigVersion("CFG-1");
        reqVO.setNightShiftEnabled(Boolean.FALSE);
        return reqVO;
    }

    private Map<?, ?> captureScheduleSnapshot() {
        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(100L), eq("scheduleConfigs"),
                snapshotCaptor.capture());
        return (Map<?, ?>) snapshotCaptor.getValue();
    }

    private MesProRouteVersionDO draftVersion() {
        return draftVersion("""
                {
                  "200": {
                    "id": 700,
                    "routeVersionId": 100,
                    "routeProcessId": 200,
                    "capacityMode": "RESOURCE_CALCULATED",
                    "nightShiftEnabled": false,
                    "configVersion": "CFG-OLD"
                  }
                }
                """);
    }

    private MesProRouteVersionDO emptyDraftVersion() {
        return draftVersion("{}");
    }

    private MesProRouteVersionDO draftVersion(String scheduleConfigsJson) {
        return MesProRouteVersionDO.builder()
                .id(100L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "routeCode": "R-10",
                          "routeName": "测试路线",
                          "configSnapshots": {
                            "flowGraph": {"nodes": [
                              {"routeProcessId": 200, "processId": 300, "sort": 1},
                              {"routeProcessId": 201, "processId": 301, "sort": 2}
                            ]},
                            "products": [],
                            "scheduleConfigs": %s,
                            "batchUseConfigs": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """.formatted(scheduleConfigsJson))
                .build();
    }

    private MesProRouteVersionDO version() {
        return MesProRouteVersionDO.builder().id(100L).routeId(10L).active(Boolean.TRUE).build();
    }

    private MesProRouteProcessDO process() {
        return MesProRouteProcessDO.builder().id(200L).routeId(10L).processId(300L).workstationId(800L).build();
    }

    private void stubAvailableNightShift() {
        MesMdWorkstationDO workstation = nightShiftWorkstation();
        when(workstationService.getWorkstation(800L)).thenReturn(workstation);
        when(productionLineService.getProductionLine(1000L)).thenReturn(MesMdProductionLineDO.builder()
                .id(1000L)
                .name("吹球囊成型线")
                .calendarPlanId(1100L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        when(planService.getPlan(1100L)).thenReturn(MesCalPlanDO.builder()
                .id(1100L)
                .status(MesCalPlanStatusEnum.CONFIRMED.getStatus())
                .build());
        when(planShiftService.getPlanShiftListByPlanId(1100L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(1201L).name("夜班").sort(3)
                        .startTime("20:00").endTime("04:00").build()));
        lenient().when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(MesProCapacityPlanDO.builder()
                        .lineId(1000L)
                        .shiftId(1201L)
                        .enabled(Boolean.TRUE)
                        .capacityMinutes(480)
                        .build()));
    }

    private MesMdWorkstationDO nightShiftWorkstation() {
        return MesMdWorkstationDO.builder()
                .id(800L)
                .code("WS-BALLOON")
                .name("吹球囊成型工作站")
                .processId(300L)
                .productionLineId(1000L)
                .singleStandardHourlyCapacity(new BigDecimal("6"))
                .shiftHours(new BigDecimal("8"))
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }
}
