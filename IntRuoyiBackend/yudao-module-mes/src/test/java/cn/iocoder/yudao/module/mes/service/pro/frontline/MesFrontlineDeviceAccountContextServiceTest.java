package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
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
    private MesProProcessService processService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private AdminUserApi adminUserApi;

    private MesFrontlineDeviceAccountContextServiceImpl contextService;

    @BeforeEach
    void setUp() {
        contextService = new MesFrontlineDeviceAccountContextServiceImpl(routeBindingSourceProvider, routeProcessMapper,
                processService, workstationWorkerService, adminUserApi);
    }

    @Test
    void shouldListOnlyProcessesUnderBoundRoutesWithoutPreviousNextRestriction() {
        when(routeBindingSourceProvider.getIfAvailable()).thenReturn(routeBindingSource);
        when(routeBindingSource.listEnabledRouteBindings(LOGIN_USER_ID)).thenReturn(List.of(
                routeBinding(101L, "R-A"),
                routeBinding(102L, "R-B")));
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
    }

    @Test
    void shouldFailFastWhenDeviceAccountHasNoRouteBinding() {
        when(routeBindingSourceProvider.getIfAvailable()).thenReturn(routeBindingSource);
        when(routeBindingSource.listEnabledRouteBindings(LOGIN_USER_ID)).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> contextService.listSwitchableProcesses(LOGIN_USER_ID));
    }

    private static MesFrontlineDeviceRouteBinding routeBinding(Long routeId, String routeCode) {
        return new MesFrontlineDeviceRouteBinding(LOGIN_USER_ID, routeId, routeCode, routeCode + " Name",
                501L, "D-501", "Device 501", 601L, "WS-601", "Workstation 601");
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

}
