package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineEmployeeSwitchServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;
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
    private MesProProcessService processService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private AdminUserApi adminUserApi;

    private MesFrontlineDeviceAccountContextServiceImpl contextService;
    private MesFrontlineEmployeeSwitchServiceImpl employeeSwitchService;

    @BeforeEach
    void setUp() {
        contextService = new MesFrontlineDeviceAccountContextServiceImpl(routeBindingSourceProvider, routeProcessMapper,
                processService, workstationWorkerService, adminUserApi);
        MesFrontlineTemplateResolverImpl templateResolver = new MesFrontlineTemplateResolverImpl(templateBindingSourceProvider);
        employeeSwitchService = new MesFrontlineEmployeeSwitchServiceImpl(contextService, templateResolver);
    }

    @Test
    void shouldListOnlyEmployeesBoundToCurrentProcessWorkstation() {
        givenBoundProcess();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationId(WORKSTATION_ID)).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(1L).workstationId(WORKSTATION_ID).postId(701L).build(),
                MesMdWorkstationWorkerDO.builder().id(2L).workstationId(WORKSTATION_ID).postId(702L).build()));
        when(adminUserApi.getUserListByPostIds(Set.of(701L, 702L))).thenReturn(List.of(
                enabledUser(10001L, "E1001", "Alice"),
                enabledUser(10002L, "E1002", "Bob"),
                disabledUser(20001L, "E2001", "Disabled")));

        List<MesFrontlineEmployeeCandidate> candidates = contextService.listEmployeeCandidates(LOGIN_USER_ID,
                ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(List.of(10001L, 10002L),
                candidates.stream().map(MesFrontlineEmployeeCandidate::userId).toList());
    }

    @Test
    void shouldSwitchActualEmployeeWithoutChangingLoginAccountOrAddingSecondVerification() {
        givenBoundProcess();
        givenEmployeeCandidates();
        when(templateBindingSourceProvider.getIfAvailable()).thenReturn(templateBindingSource);
        when(templateBindingSource.findTemplate(any(MesFrontlineTemplateRequest.class))).thenReturn(
                new MesFrontlineTemplateDescriptor("TPL-201-E1001", "BATCH_RECORD",
                        ROUTE_PROCESS_ID, PROCESS_ID, 10001L));

        MesFrontlineEmployeeSwitchResult result = employeeSwitchService.switchActualEmployee(
                new MesFrontlineEmployeeSwitchCommand(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID,
                        PROCESS_ID, 10001L));

        assertEquals(LOGIN_USER_ID, result.loginUserId());
        assertEquals(10001L, result.actualEmployeeId());
        assertFalse(result.extraVerificationRequired());
        assertEquals("TPL-201-E1001", result.template().templateNo());
        assertTrue(Arrays.stream(MesFrontlineEmployeeSwitchCommand.class.getDeclaredFields())
                .map(Field::getName)
                .noneMatch(name -> name.contains("password")
                        || name.contains("verification")
                        || name.contains("captcha")
                        || name.contains("scan")
                        || name.contains("impersonate")));
    }

    @Test
    void shouldRejectActualEmployeeOutsideCurrentProcessBinding() {
        givenBoundProcess();
        givenEmployeeCandidates();

        assertThrows(ServiceException.class, () -> employeeSwitchService.switchActualEmployee(
                new MesFrontlineEmployeeSwitchCommand(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID,
                        PROCESS_ID, 20001L)));
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
