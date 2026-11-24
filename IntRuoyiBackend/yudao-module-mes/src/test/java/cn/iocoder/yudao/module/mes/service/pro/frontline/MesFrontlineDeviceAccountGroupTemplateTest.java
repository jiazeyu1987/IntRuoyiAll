package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
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
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPackagingLeaderAccess;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesFrontlineDeviceAccountGroupTemplateTest {
    @Mock private ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider;
    @Mock private MesProRouteProcessMapper routeProcessMapper;
    @Mock private MesProRouteVersionMapper routeVersionMapper;
    @Mock private MesProProcessService processService;
    @Mock private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private PermissionApi permissionApi;
    @Mock private MesProRouteService routeService;
    @Mock private MesMdWorkstationService workstationService;
    @Mock private MesMdWorkstationMachineService workstationMachineService;
    @Mock private MesDvMachineryService machineryService;
    @Mock private MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    @Mock private MesFrontlineActiveOrderProcessService activeOrderProcessService;
    @Mock private MesPackagingLeaderAccess packagingAccess;
    private MesFrontlineDeviceAccountContextServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineDeviceAccountContextServiceImpl(routeBindingSourceProvider, routeProcessMapper,
                routeVersionMapper, processService, workstationWorkerService, adminUserApi, permissionApi,
                routeService, workstationService, workstationMachineService, machineryService, employeeProfileMapper,
                activeOrderProcessService);
        ReflectionTestUtils.setField(service, "packagingAccess", packagingAccess);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{\"configSnapshots\":{\"routeStartProductionLeaders\":[{\"productionLineId\":999,\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[7]}]}}"})
    void damagedGroupSnapshotsDoNotBlockValidFlowCandidates(String groupSnapshot) {
        when(routeService.getRouteListByStatus(0)).thenReturn(List.of(route(20L, "GROUP"), route(10L, "FLOW")));
        // Even an overbroad mapper result cannot feed GROUP snapshots to execution authorization.
        when(routeVersionMapper.selectListByRouteIds(Set.of(10L))).thenReturn(List.of(
                active(200L, 20L, groupSnapshot), active(100L, 10L, leaders(10L))));
        givenFlowProcess();

        List<MesFrontlineRouteProcessCandidate> result = service.listSwitchableProcesses(7L);

        assertEquals(List.of(10L), result.stream().map(MesFrontlineRouteProcessCandidate::routeId).toList());
        verifyNoInteractions(routeBindingSourceProvider);
    }

    @Test
    void evenValidGroupLeaderConfigurationDoesNotGrantExecution() {
        when(routeService.getRouteListByStatus(0)).thenReturn(List.of(route(20L, "GROUP"), route(10L, "FLOW")));
        when(routeVersionMapper.selectListByRouteIds(Set.of(10L))).thenReturn(List.of(
                active(200L, 20L, leaders(20L)), active(100L, 10L, leaders(10L))));
        givenFlowProcess();

        assertEquals(List.of(10L), service.listSwitchableProcesses(7L).stream()
                .map(MesFrontlineRouteProcessCandidate::routeId).toList());
        verify(routeProcessMapper).selectListByRouteIds(Set.of(10L));
    }

    @Test
    void packagingLeaderScopeAlsoExcludesGroupTemplates() {
        when(routeService.getRouteListByStatus(0)).thenReturn(List.of(route(20L, "GROUP"), route(10L, "FLOW")));
        when(packagingAccess.isPackagingLeader(7L)).thenReturn(true);
        when(packagingAccess.activeRouteIds(7L)).thenReturn(Set.of(10L, 20L));
        givenFlowProcess();

        assertEquals(List.of(10L), service.listSwitchableProcesses(7L).stream()
                .map(MesFrontlineRouteProcessCandidate::routeId).toList());
        verifyNoInteractions(routeVersionMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{\"configSnapshots\":{\"routeStartProductionLeaders\":[{\"productionLineId\":20,\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[7]}]}}"})
    void damagedFlowOrMismatchedScopeStillFailsStrictly(String flowSnapshot) {
        when(routeService.getRouteListByStatus(0)).thenReturn(List.of(route(10L, "FLOW")));
        when(routeVersionMapper.selectListByRouteIds(Set.of(10L)))
                .thenReturn(List.of(active(100L, 10L, flowSnapshot)));

        ServiceException error = assertThrows(ServiceException.class, () -> service.listSwitchableProcesses(7L));

        assertEquals(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID.getCode(), error.getCode());
        verifyNoInteractions(routeProcessMapper, routeBindingSourceProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN"})
    void missingOrUnknownNodeTypeCannotGrantExecution(String nodeType) {
        when(routeService.getRouteListByStatus(0)).thenReturn(List.of(route(10L, nodeType)));

        ServiceException error = assertThrows(ServiceException.class, () -> service.listSwitchableProcesses(7L));

        assertEquals(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID.getCode(), error.getCode());
        verifyNoInteractions(routeVersionMapper, routeProcessMapper, routeBindingSourceProvider);
    }

    private void givenFlowProcess() {
        when(routeProcessMapper.selectListByRouteIds(Set.of(10L))).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(1000L).routeId(10L).processId(100L).workstationId(50L).sort(1).build()));
        when(workstationService.getWorkstationMap(Set.of(50L))).thenReturn(Map.of(50L,
                MesMdWorkstationDO.builder().id(50L).code("WS-50").name("工位").status(0).build()));
        when(processService.getProcessMap(Set.of(100L))).thenReturn(Map.of(100L,
                MesProProcessDO.builder().id(100L).code("P-100").name("组装").status(0).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(Set.of(50L))).thenReturn(List.of());
    }

    private MesProRouteDO route(Long id, String nodeType) {
        return MesProRouteDO.builder().id(id).nodeType(nodeType).code("RT-" + id).name("路线" + id).status(0).build();
    }

    private MesProRouteVersionDO active(Long id, Long routeId, String snapshot) {
        return MesProRouteVersionDO.builder().id(id).routeId(routeId).active(true)
                .lifecycleStatus("ACTIVE").routeSnapshotJson(snapshot).build();
    }

    private String leaders(Long scopeId) {
        return "{\"configSnapshots\":{\"routeStartProductionLeaders\":[{\"productionLineId\":" + scopeId
                + ",\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[7]}]}}";
    }
}
