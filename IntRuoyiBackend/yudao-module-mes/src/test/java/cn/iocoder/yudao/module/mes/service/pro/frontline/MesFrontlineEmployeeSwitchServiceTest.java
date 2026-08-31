package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MesFrontlineEmployeeSwitchServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;
    private static final Long ACTIVE_ORDER_ID = 48L;
    private static final Long ROUTE_ID = 101L;
    private static final Long ROUTE_PROCESS_ID = 1001L;
    private static final Long PROCESS_ID = 201L;
    private static final Long WORKSTATION_ID = 301L;

    @Mock
    private ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider;
    @Mock
    private MesFrontlineDeviceAccountRouteBindingSource routeBindingSource;
    @Mock
    private ObjectProvider<MesFrontlineTemplateBindingSource> templateBindingSourceProvider;
    @Mock
    private MesFrontlineTemplateBindingSource templateBindingSource;
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
    private MesFrontlineRuntimeConfigService runtimeConfigService;
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
    @Mock
    private MesFrontlineActiveOrderProcessService activeOrderProcessService;

    private MesFrontlineDeviceAccountContextServiceImpl contextService;
    private MesFrontlineEmployeeSwitchServiceImpl employeeSwitchService;

    @BeforeEach
    void setUp() {
        contextService = new MesFrontlineDeviceAccountContextServiceImpl(routeBindingSourceProvider, routeProcessMapper,
                routeVersionMapper, processService, workstationWorkerService, adminUserApi, permissionApi, routeService,
                workstationService, workstationMachineService, machineryService, employeeProfileMapper,
                activeOrderProcessService);
        MesFrontlineTemplateResolverImpl templateResolver = new MesFrontlineTemplateResolverImpl(templateBindingSourceProvider);
        employeeSwitchService = new MesFrontlineEmployeeSwitchServiceImpl(templateResolver, runtimeConfigService);
    }

    @Test
    void shouldListAllEnabledLeaderPersonnelWithoutProcessWorkstationBinding() {
        givenBoundProcess();
        when(employeeProfileMapper.selectList(any())).thenReturn(
                List.of(),
                List.of(),
                List.of(
                        employeeProfile(8801L, 10001L, "E1001", "Alice", true),
                        employeeProfile(8802L, 10002L, "E1002", "Bob", true),
                        employeeProfile(8803L, 20001L, "E2001", "Disabled", false)));

        List<MesFrontlineEmployeeCandidate> candidates = contextService.listEmployeeCandidates(LOGIN_USER_ID,
                ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(List.of(10001L, 10002L),
                candidates.stream().map(MesFrontlineEmployeeCandidate::userId).toList());
    }

    @Test
    void shouldSwitchActualEmployeeWithoutChangingLoginAccountOrAddingSecondVerification() {
        givenBoundProcess();
        givenRuntimeConfigEmployee(8801L, 10001L, "Alice", "FORMAL");

        MesFrontlineEmployeeSwitchResult result = employeeSwitchService.switchActualEmployee(
                new MesFrontlineEmployeeSwitchCommand(LOGIN_USER_ID, ACTIVE_ORDER_ID, ROUTE_ID,
                        ROUTE_PROCESS_ID, PROCESS_ID, 10001L));

        assertEquals(LOGIN_USER_ID, result.loginUserId());
        assertEquals(10001L, result.actualEmployeeId());
        assertFalse(result.extraVerificationRequired());
        assertEquals("TPL-201-E1001", result.template().templateNo());
        verify(templateBindingSourceProvider, never()).getIfAvailable();
        assertTrue(Arrays.stream(MesFrontlineEmployeeSwitchCommand.class.getDeclaredFields())
                .map(Field::getName)
                .noneMatch(name -> name.contains("password")
                        || name.contains("verification")
                        || name.contains("captcha")
                        || name.contains("scan")
                        || name.contains("impersonate")));
    }

    @Test
    void shouldSwitchTemporaryEmployeeFromRuntimeConfigWithoutSystemUser() {
        givenBoundProcess();
        givenRuntimeConfigEmployee(8801L, null, "临时工甲", "TEMPORARY");

        MesFrontlineEmployeeSwitchResult result = employeeSwitchService.switchActualEmployee(
                new MesFrontlineEmployeeSwitchCommand(LOGIN_USER_ID, ACTIVE_ORDER_ID, ROUTE_ID,
                        ROUTE_PROCESS_ID, PROCESS_ID, 8801L));

        assertEquals(LOGIN_USER_ID, result.loginUserId());
        assertEquals(8801L, result.actualEmployeeId());
        assertFalse(result.extraVerificationRequired());
        assertEquals("TPL-201-TMP", result.template().templateNo());
    }

    @Test
    void shouldRejectActualEmployeeOutsideCurrentProcessBinding() {
        givenBoundProcess();
        givenRuntimeConfigEmployee(8801L, 10001L, "Alice", "FORMAL");

        assertThrows(ServiceException.class, () -> employeeSwitchService.switchActualEmployee(
                new MesFrontlineEmployeeSwitchCommand(LOGIN_USER_ID, ACTIVE_ORDER_ID, ROUTE_ID,
                        ROUTE_PROCESS_ID, PROCESS_ID, 20001L)));
    }

    private void givenBoundProcess() {
        when(routeBindingSourceProvider.getIfAvailable()).thenReturn(routeBindingSource);
        when(routeBindingSource.listEnabledRouteBindings(LOGIN_USER_ID)).thenReturn(List.of(
                new MesFrontlineDeviceRouteBinding(LOGIN_USER_ID, ROUTE_ID, "R-101", "Route 101",
                        501L, "D-501", "Device 501", WORKSTATION_ID, "WS-301", "Workstation 301")));
        when(routeProcessMapper.selectListByRouteIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder()
                        .id(ROUTE_PROCESS_ID)
                        .routeId(ROUTE_ID)
                        .processId(PROCESS_ID)
                        .workstationId(WORKSTATION_ID)
                        .sort(10)
                        .build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(
                PROCESS_ID, MesProProcessDO.builder()
                        .id(PROCESS_ID)
                        .code("P-201")
                        .name("Granulation")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build()));
    }

    private void givenEmployeeCandidates() {
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationId(WORKSTATION_ID)).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(1L).workstationId(WORKSTATION_ID).postId(701L).build()));
        when(adminUserApi.getUserListByPostIds(Set.of(701L))).thenReturn(List.of(
                enabledUser(10001L, "E1001", "Alice")));
    }

    private void givenRuntimeConfigEmployee(Long employeeProfileId, Long systemUserId, String employeeName,
                                            String employeeType) {
        when(runtimeConfigService.getRuntimeConfig(LOGIN_USER_ID, ACTIVE_ORDER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRuntimeConfig(ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID,
                        List.of(new MesFrontlineTeamEmployeeOption(employeeProfileId, systemUserId,
                                systemUserId == null ? "TMP-001" : "E1001", employeeName, employeeName,
                                employeeType)),
                        List.of(), List.of(), List.of(), null,
                        List.of(new MesFrontlineEmployeeSwitchResult(LOGIN_USER_ID,
                                systemUserId == null ? employeeProfileId : systemUserId,
                                ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID, false,
                                new MesFrontlineTemplateDescriptor(
                                        systemUserId == null ? "TPL-201-TMP" : "TPL-201-E1001",
                                        "BATCH_RECORD", ROUTE_PROCESS_ID, PROCESS_ID,
                                        systemUserId == null ? employeeProfileId : systemUserId))),
                        "snapshot-001", "hash-001"));
    }

    private static MesProcessPoolTeamEmployeeProfileDO employeeProfile(Long id, Long systemUserId,
                                                                        String employeeCode, String employeeName,
                                                                        boolean enabled) {
        return MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(id)
                .leaderUserId(LOGIN_USER_ID)
                .systemUserId(systemUserId)
                .employeeCode(employeeCode)
                .employeeName(employeeName)
                .displayName(employeeName)
                .enabled(enabled)
                .build();
    }

    private static AdminUserRespDTO enabledUser(Long id, String username, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }

    private static AdminUserRespDTO disabledUser(Long id, String username, String nickname) {
        AdminUserRespDTO user = enabledUser(id, username, nickname);
        user.setStatus(CommonStatusEnum.DISABLE.getStatus());
        return user;
    }

}
