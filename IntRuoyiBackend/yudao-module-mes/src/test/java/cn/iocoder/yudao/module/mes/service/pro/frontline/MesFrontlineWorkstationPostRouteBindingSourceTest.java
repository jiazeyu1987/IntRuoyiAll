package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineWorkstationPostRouteBindingSourceTest {

    private static final Long LOGIN_USER_ID = 9001L;

    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesDvMachineryService machineryService;

    private MesFrontlineWorkstationPostRouteBindingSource bindingSource;

    @BeforeEach
    void setUp() {
        bindingSource = new MesFrontlineWorkstationPostRouteBindingSource(adminUserApi, workstationWorkerService,
                workstationService, routeProcessMapper, routeService, workstationMachineService, machineryService);
    }

    @Test
    void shouldResolveEnabledRoutesFromLoginUserPostsWithMultipleAndNoDeviceWorkstations() {
        AdminUserRespDTO loginUser = new AdminUserRespDTO();
        loginUser.setId(LOGIN_USER_ID);
        loginUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
        loginUser.setPostIds(Set.of(701L));
        when(adminUserApi.getUser(LOGIN_USER_ID)).thenReturn(loginUser);
        when(workstationWorkerService.getWorkstationWorkerListByPostIds(Set.of(701L))).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(1L).workstationId(301L).postId(701L).build(),
                MesMdWorkstationWorkerDO.builder().id(2L).workstationId(302L).postId(701L).build()));
        when(workstationService.getWorkstationMap(Set.of(301L, 302L))).thenReturn(Map.of(
                301L, workstation(301L, "WS-301"),
                302L, workstation(302L, "WS-302")));
        when(routeProcessMapper.selectListByWorkstationIds(Set.of(301L, 302L))).thenReturn(List.of(
                routeProcess(1001L, 101L, 201L, 301L, 10),
                routeProcess(1002L, 102L, 202L, 302L, 20)));
        when(routeService.getRouteMap(Set.of(101L, 102L))).thenReturn(Map.of(
                101L, route(101L, "R-101", CommonStatusEnum.ENABLE.getStatus()),
                102L, route(102L, "R-102", CommonStatusEnum.ENABLE.getStatus())));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(Set.of(301L, 302L)))
                .thenReturn(List.of(
                        MesMdWorkstationMachineDO.builder().id(11L).workstationId(301L).machineryId(501L).build(),
                        MesMdWorkstationMachineDO.builder().id(12L).workstationId(301L).machineryId(502L).build()));
        when(machineryService.getMachineryMap(Set.of(501L, 502L))).thenReturn(Map.of(
                501L, machinery(501L, "D-501"),
                502L, machinery(502L, "D-502")));

        List<MesFrontlineDeviceRouteBinding> bindings = bindingSource.listEnabledRouteBindings(LOGIN_USER_ID);

        assertEquals(3, bindings.size());
        assertEquals(List.of(501L, 502L),
                bindings.subList(0, 2).stream().map(MesFrontlineDeviceRouteBinding::deviceId).toList());
        assertEquals(101L, bindings.get(0).routeId());
        assertEquals(301L, bindings.get(0).workstationId());
        assertEquals(102L, bindings.get(2).routeId());
        assertEquals(302L, bindings.get(2).workstationId());
        assertNull(bindings.get(2).deviceId());
    }

    @Test
    void shouldFailFastWhenFormalPostHasNoWorkstationBinding() {
        AdminUserRespDTO loginUser = new AdminUserRespDTO();
        loginUser.setId(LOGIN_USER_ID);
        loginUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
        loginUser.setPostIds(Set.of(701L));
        when(adminUserApi.getUser(LOGIN_USER_ID)).thenReturn(loginUser);
        when(workstationWorkerService.getWorkstationWorkerListByPostIds(Set.of(701L))).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> bindingSource.listEnabledRouteBindings(LOGIN_USER_ID));

        assertTrue(exception.getMessage().contains("post workstation binding"));
    }

    private static MesMdWorkstationDO workstation(Long id, String code) {
        return MesMdWorkstationDO.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesProRouteProcessDO routeProcess(Long id, Long routeId, Long processId, Long workstationId,
                                                     Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .workstationId(workstationId)
                .sort(sort)
                .build();
    }

    private static MesProRouteDO route(Long id, String code, Integer status) {
        return MesProRouteDO.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .status(status)
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
