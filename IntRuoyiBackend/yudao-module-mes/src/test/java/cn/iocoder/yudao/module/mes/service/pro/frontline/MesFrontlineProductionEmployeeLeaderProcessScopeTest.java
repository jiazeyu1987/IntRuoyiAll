package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineProductionEmployeeLeaderProcessScopeTest {

    private static final Long EMPLOYEE_USER_ID = 9101L;
    private static final Long LEADER_USER_ID = 9001L;

    @Mock
    private ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider;
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

    @InjectMocks
    private MesFrontlineDeviceAccountContextServiceImpl contextService;

    @Test
    void productionEmployeeSeesEveryProcessOwnedByItsLeaderWithoutProcessBinding() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                MesProcessPoolTeamEmployeeProfileDO.builder()
                        .id(8801L)
                        .leaderUserId(LEADER_USER_ID)
                        .systemUserId(EMPLOYEE_USER_ID)
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(
                route(101L, "RT-A"), route(102L, "RT-B")));
        when(routeVersionMapper.selectListByRouteIds(Set.of(101L, 102L))).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, routeSnapshot(101L)),
                activeRouteVersion(9002L, 102L, routeSnapshot(102L))));
        when(routeProcessMapper.selectListByRouteIds(Set.of(101L, 102L))).thenReturn(List.of(
                routeProcess(1001L, 101L, 201L, 301L, 10),
                routeProcess(1002L, 101L, 202L, 302L, 20),
                routeProcess(1003L, 102L, 203L, 303L, 30)));
        when(processService.getProcessMap(Set.of(201L, 202L, 203L))).thenReturn(Map.of(
                201L, enabledProcess(201L, "P-201"),
                202L, enabledProcess(202L, "P-202"),
                203L, enabledProcess(203L, "P-203")));
        when(workstationService.getWorkstationMap(Set.of(301L, 302L, 303L))).thenReturn(Map.of(
                301L, workstation(301L),
                302L, workstation(302L),
                303L, workstation(303L)));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(anyCollection()))
                .thenReturn(List.of());

        List<MesFrontlineRouteProcessCandidate> candidates =
                contextService.listSwitchableProcesses(EMPLOYEE_USER_ID);

        assertEquals(List.of(1001L, 1002L, 1003L),
                candidates.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        verify(routeBindingSourceProvider, never()).getIfAvailable();
    }

    @Test
    void disabledProductionEmployeeProfileFailsWithoutDeviceAccountFallback() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LEADER_USER_ID, Boolean.FALSE)));

        ServiceException error = assertThrows(ServiceException.class,
                () -> contextService.listSwitchableProcesses(EMPLOYEE_USER_ID));

        assertEquals(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID.getCode(), error.getCode());
        verify(routeBindingSourceProvider, never()).getIfAvailable();
    }

    @Test
    void multipleProductionLeaderOwnershipFailsWithoutDeviceAccountFallback() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LEADER_USER_ID, Boolean.TRUE),
                employeeProfile(8802L, 9002L, Boolean.TRUE)));

        ServiceException error = assertThrows(ServiceException.class,
                () -> contextService.listSwitchableProcesses(EMPLOYEE_USER_ID));

        assertEquals(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID.getCode(), error.getCode());
        verify(routeBindingSourceProvider, never()).getIfAvailable();
    }

    @Test
    void productionLeaderWithoutFormalRouteFailsWithoutDeviceAccountFallback() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, LEADER_USER_ID, Boolean.TRUE)));
        when(routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class,
                () -> contextService.listSwitchableProcesses(EMPLOYEE_USER_ID));

        assertEquals(PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED.getCode(), error.getCode());
        verify(routeBindingSourceProvider, never()).getIfAvailable();
    }

    private static MesProcessPoolTeamEmployeeProfileDO employeeProfile(Long id, Long leaderUserId,
                                                                        Boolean enabled) {
        return MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(id)
                .leaderUserId(leaderUserId)
                .systemUserId(EMPLOYEE_USER_ID)
                .enabled(enabled)
                .build();
    }

    private static String routeSnapshot(Long routeId) {
        return """
                {
                  "routeId": %d,
                  "configSnapshots": {
                    "routeStartProductionLeaders": [
                      {
                        "productionLineId": %d,
                        "candidateSourceType": "USERS",
                        "candidateSourceIds": [%d]
                      }
                    ]
                  }
                }
                """.formatted(routeId, routeId, LEADER_USER_ID);
    }

    private static MesProRouteDO route(Long id, String code) {
        return MesProRouteDO.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesProRouteVersionDO activeRouteVersion(Long id, Long routeId, String snapshot) {
        return MesProRouteVersionDO.builder()
                .id(id)
                .routeId(routeId)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson(snapshot)
                .build();
    }

    private static MesProRouteProcessDO routeProcess(Long id, Long routeId, Long processId,
                                                     Long workstationId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .workstationId(workstationId)
                .sort(sort)
                .build();
    }

    private static MesProProcessDO enabledProcess(Long id, String code) {
        return MesProProcessDO.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesMdWorkstationDO workstation(Long id) {
        return MesMdWorkstationDO.builder()
                .id(id)
                .code("WS-" + id)
                .name("Workstation " + id)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

}
