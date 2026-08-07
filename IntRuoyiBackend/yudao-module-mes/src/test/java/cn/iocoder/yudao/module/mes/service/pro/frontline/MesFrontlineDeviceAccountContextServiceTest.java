package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineDeviceAccountContextServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;

    @Mock
    private ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider;
    @Mock
    private MesFrontlineDeviceAccountRouteBindingSource routeBindingSource;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesDvMachineryService machineryService;
    @Mock
    private MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;

    private MesFrontlineDeviceAccountContextServiceImpl contextService;

    @BeforeEach
    void setUp() {
        contextService = new MesFrontlineDeviceAccountContextServiceImpl(routeBindingSourceProvider, routeProcessMapper,
                routeVersionMapper, processService, workstationWorkerService, adminUserApi, permissionApi,
                routeService, workstationService, workstationMachineService, machineryService, employeeProfileMapper);
    }

    @Test
    void shouldListOnlyProcessesUnderBoundRoutesWithoutPreviousNextRestriction() {
        when(routeBindingSourceProvider.getIfAvailable()).thenReturn(routeBindingSource);
        when(routeBindingSource.listEnabledRouteBindings(LOGIN_USER_ID)).thenReturn(List.of(
                routeBinding(101L, "R-A", 301L, 501L),
                routeBinding(101L, "R-A", 302L, 502L),
                routeBinding(102L, "R-B", 303L, 503L)));
        when(routeProcessMapper.selectListByRouteIds(anyCollection())).thenReturn(List.of(
                routeProcess(1001L, 101L, 201L, 301L, 20),
                routeProcess(1002L, 101L, 202L, 302L, 10),
                routeProcess(1003L, 102L, 203L, 303L, 30),
                routeProcess(1999L, 199L, 299L, 399L, 1)));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(
                201L, enabledProcess(201L, "P-201", "Granulation"),
                202L, enabledProcess(202L, "P-202", "Drying"),
                203L, enabledProcess(203L, "P-203", "Packing"),
                299L, enabledProcess(299L, "P-299", "Unbound")));

        List<MesFrontlineRouteProcessCandidate> candidates = contextService.listSwitchableProcesses(LOGIN_USER_ID);

        assertEquals(List.of(1002L, 1001L, 1003L),
                candidates.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(List.of(202L, 201L, 203L),
                candidates.stream().map(MesFrontlineRouteProcessCandidate::processId).toList());
        assertEquals(List.of(502L, 501L, 503L),
                candidates.stream().map(MesFrontlineRouteProcessCandidate::deviceId).toList());
    }

    @Test
    void shouldFailFastWhenDeviceAccountHasNoRouteBinding() {
        when(routeBindingSourceProvider.getIfAvailable()).thenReturn(routeBindingSource);
        when(routeBindingSource.listEnabledRouteBindings(LOGIN_USER_ID)).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> contextService.listSwitchableProcesses(LOGIN_USER_ID));
    }

    @Test
    void shouldExposeMultipleDevicesForOneProcessAndKeepNoDeviceProcessAvailable() {
        when(routeBindingSourceProvider.getIfAvailable()).thenReturn(routeBindingSource);
        when(routeBindingSource.listEnabledRouteBindings(LOGIN_USER_ID)).thenReturn(List.of(
                routeBinding(101L, "R-A", 301L, 501L),
                routeBinding(101L, "R-A", 301L, 502L),
                routeBinding(102L, "R-B", 302L, null)));
        when(routeProcessMapper.selectListByRouteIds(anyCollection())).thenReturn(List.of(
                routeProcess(1001L, 101L, 201L, 301L, 10),
                routeProcess(1002L, 102L, 202L, 302L, 20)));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(
                201L, enabledProcess(201L, "P-201", "Granulation"),
                202L, enabledProcess(202L, "P-202", "Drying")));

        List<MesFrontlineRouteProcessCandidate> candidates = contextService.listSwitchableProcesses(LOGIN_USER_ID);

        assertEquals(List.of(1001L, 1001L, 1002L),
                candidates.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(501L, candidates.get(0).deviceId());
        assertEquals(502L, candidates.get(1).deviceId());
        assertEquals(null, candidates.get(2).deviceId());
    }

    @Test
    void shouldNotGrantPressurePumpProcessesByMenuPermissionWithoutRouteStartLeaderConfig() {
        lenient().when(permissionApi.hasAnyPermissions(eq(LOGIN_USER_ID),
                eq(MesFrontlineDeviceAccountContextServiceImpl.PRESSURE_PUMP_ALL_PROCESS_PERMISSION)))
                .thenReturn(true);
        when(routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(
                route(922119L, "RT000028", "球囊扩张压力泵")));
        when(routeVersionMapper.selectListByRouteIds(Set.of(922119L))).thenReturn(List.of(activeRouteVersion(
                9901L,
                922119L,
                """
                        {
                          "routeId": 922119,
                          "configSnapshots": {
                            "routeStartProductionLeaders": []
                          }
                        }
                        """)));
        when(routeBindingSourceProvider.getIfAvailable()).thenReturn(routeBindingSource);
        when(routeBindingSource.listEnabledRouteBindings(LOGIN_USER_ID)).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> contextService.listSwitchableProcesses(LOGIN_USER_ID));

        verify(routeProcessMapper, never()).selectListByRouteIds(Set.of(922119L));
    }

    @Test
    void shouldListRouteStartProductionLeaderProcessesByUserAndRoleRouteScopes() {
        when(routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(
                route(101L, "RT-A", "压力泵一线"),
                route(102L, "RT-B", "压力泵二线")));
        when(routeVersionMapper.selectListByRouteIds(Set.of(101L, 102L))).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, """
                        {
                          "routeId": 101,
                          "configSnapshots": {
                            "routeStartProductionLeaders": [
                              {
                                "productionLineId": 101,
                                "candidateSourceType": "USERS",
                                "candidateSourceIds": [9001],
                                "candidateSourceNames": ["张三"]
                              },
                              {
                                "productionLineId": 101,
                                "candidateSourceType": "ROLE",
                                "candidateSourceIds": [77],
                                "candidateSourceNames": ["压力泵生产组长"]
                              }
                            ]
                          }
                        }
                        """),
                activeRouteVersion(9002L, 102L, """
                        {
                          "routeId": 102,
                          "configSnapshots": {
                            "routeStartProductionLeaders": [
                              {
                                "productionLineId": 102,
                                "candidateSourceType": "ROLE",
                                "candidateSourceIds": [77],
                                "candidateSourceNames": ["压力泵生产组长"]
                              }
                            ]
                          }
                        }
                        """)));
        when(permissionApi.getUserRoleIdListByUserId(LOGIN_USER_ID)).thenReturn(Set.of(77L));
        when(routeProcessMapper.selectListByRouteIds(Set.of(101L, 102L))).thenReturn(List.of(
                routeProcess(1001L, 101L, 201L, 301L, 10),
                routeProcess(1002L, 101L, 202L, 302L, 20),
                routeProcess(1003L, 101L, 203L, 303L, 30),
                routeProcess(1004L, 102L, 204L, 304L, 40)));
        when(processService.getProcessMap(Set.of(201L, 202L, 203L, 204L))).thenReturn(Map.of(
                201L, enabledProcess(201L, "P-201", "装配"),
                202L, enabledProcess(202L, "P-202", "检验"),
                203L, enabledProcess(203L, "P-203", "包装前检查"),
                204L, enabledProcess(204L, "P-204", "包装")));
        when(workstationService.getWorkstationMap(Set.of(301L, 302L, 303L, 304L))).thenReturn(Map.of(
                301L, workstation(301L, "WS-301"),
                302L, workstation(302L, "WS-302"),
                303L, workstation(303L, "WS-303"),
                304L, workstation(304L, "WS-304")));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(Set.of(301L, 302L, 303L, 304L)))
                .thenReturn(List.of());

        List<MesFrontlineRouteProcessCandidate> candidates = contextService.listSwitchableProcesses(LOGIN_USER_ID);

        assertEquals(List.of(1001L, 1002L, 1003L, 1004L),
                candidates.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(List.of(301L, 302L, 303L, 304L),
                candidates.stream().map(MesFrontlineRouteProcessCandidate::workstationId).toList());
        verify(routeBindingSourceProvider, never()).getIfAvailable();
    }

    @Test
    void shouldFailWhenRouteStartProductionLeaderSnapshotUsesNonRouteScope() {
        when(routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(
                route(101L, "RT-A", "压力泵一线")));
        when(routeVersionMapper.selectListByRouteIds(Set.of(101L))).thenReturn(List.of(activeRouteVersion(
                9001L,
                101L,
                """
                        {
                          "routeId": 101,
                          "configSnapshots": {
                            "routeStartProductionLeaders": [
                              {
                                "productionLineId": 7001,
                                "candidateSourceType": "USERS",
                                "candidateSourceIds": [9001],
                                "candidateSourceNames": ["张三"]
                              }
                            ]
                          }
                        }
                        """)));

        assertThrows(ServiceException.class, () -> contextService.listSwitchableProcesses(LOGIN_USER_ID));
    }

    private static MesFrontlineDeviceRouteBinding routeBinding(Long routeId, String routeCode, Long workstationId,
                                                               Long deviceId) {
        return new MesFrontlineDeviceRouteBinding(LOGIN_USER_ID, routeId, routeCode, routeCode + " Name",
                deviceId, deviceId == null ? null : "D-" + deviceId,
                deviceId == null ? null : "Device " + deviceId,
                workstationId, "WS-" + workstationId, "Workstation " + workstationId);
    }

    private static MesProRouteProcessDO routeProcess(Long routeProcessId, Long routeId, Long processId,
                                                     Long workstationId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(processId)
                .workstationId(workstationId)
                .sort(sort)
                .build();
    }

    private static MesProProcessDO enabledProcess(Long id, String code, String name) {
        return MesProProcessDO.builder()
                .id(id)
                .code(code)
                .name(name)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesProRouteDO route(Long id, String code, String name) {
        return MesProRouteDO.builder()
                .id(id)
                .code(code)
                .name(name)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesMdWorkstationDO workstation(Long id, String code) {
        return workstation(id, code, null);
    }

    private static MesMdWorkstationDO workstation(Long id, String code, Long productionLineId) {
        return MesMdWorkstationDO.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .productionLineId(productionLineId)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesProRouteVersionDO activeRouteVersion(Long id, Long routeId, String routeSnapshotJson) {
        return MesProRouteVersionDO.builder()
                .id(id)
                .routeId(routeId)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson(routeSnapshotJson)
                .build();
    }

    private static MesDvMachineryDO machinery(Long id, String code) {
        return MesDvMachineryDO.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .build();
    }

}
