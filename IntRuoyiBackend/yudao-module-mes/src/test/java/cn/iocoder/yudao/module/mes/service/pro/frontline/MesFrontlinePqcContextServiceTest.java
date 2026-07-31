package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_PROCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlinePqcContextServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;
    private static final Long WORK_ORDER_ID = 1001L;
    private static final Long ROUTE_ID = 2001L;
    private static final Long PRODUCT_ID = 3001L;
    private static final Long ROUTE_PROCESS_ID = 4001L;
    private static final Long PROCESS_ID = 5001L;

    @Mock
    private MesProProcessPoolMapper processPoolMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private MesFrontlineTemplateResolver templateResolver;

    private MesFrontlinePqcContextService service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlinePqcContextServiceImpl(processPoolMapper, workOrderMapper, routeMapper,
                routeProductMapper, routeProcessMapper, processService, itemService, scopeMapper, adminUserApi,
                templateResolver);
    }

    @Test
    void shouldListActiveOrdersFromCurrentActiveProcessPools() {
        when(processPoolMapper.selectActiveList()).thenReturn(List.of(
                activePool(WORK_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)),
                activePool(WORK_ORDER_ID, ROUTE_ID, 4002L, 5002L,
                        LocalDateTime.of(2026, 8, 1, 9, 0))));
        givenWorkOrderProductAndRoute();

        List<MesFrontlineActiveOrderCandidate> orders = service.listActiveOrders();

        assertEquals(1, orders.size());
        assertEquals(WORK_ORDER_ID, orders.get(0).workOrderId());
        assertEquals("WO-PQC-001", orders.get(0).workOrderCode());
        assertEquals(PRODUCT_ID, orders.get(0).productId());
        assertEquals(ROUTE_ID, orders.get(0).routeId());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), orders.get(0).latestSubmitTime());
    }

    @Test
    void shouldLoadProcessesFromSelectedActiveOrderProductRoute() {
        when(processPoolMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activePool(WORK_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10),
                routeProcess(4002L, ROUTE_ID, 5002L, 20)));
        when(processService.getProcessMap(Set.of(PROCESS_ID, 5002L))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序"),
                5002L, process(5002L, "P-2", "末工序")));

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(List.of(ROUTE_PROCESS_ID, 4002L),
                processes.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(List.of("首工序", "末工序"),
                processes.stream().map(MesFrontlineRouteProcessCandidate::processName).toList());
    }

    @Test
    void shouldListAllPqcEmployeesAndPqcLeaders() {
        when(scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC)).thenReturn(List.of(
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8001L),
                scope(7002L, SCOPE_TYPE_PROCESS, null),
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8002L)));
        when(adminUserApi.getUserList(Set.of(7001L, 7002L, 8001L, 8002L))).thenReturn(List.of(
                enabledUser(7001L, "pqc-leader-a", "PQC组长A"),
                enabledUser(7002L, "pqc-leader-b", "PQC组长B"),
                enabledUser(8001L, "pqc-employee-a", "PQC员工A"),
                enabledUser(8002L, "pqc-employee-b", "PQC员工B")));

        List<MesFrontlineEmployeeCandidate> employees = service.listPqcEmployeeCandidates();

        assertEquals(Set.of(7001L, 7002L, 8001L, 8002L),
                employees.stream().map(MesFrontlineEmployeeCandidate::userId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void shouldFailFastWhenSelectedOrderIsNotActive() {
        when(processPoolMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID));
    }

    @Test
    void shouldSwitchPqcEmployeeOnlyAfterActiveOrderProcessAndPersonnelValidation() {
        when(processPoolMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activePool(WORK_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序")));
        when(scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC)).thenReturn(List.of(
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8001L)));
        when(adminUserApi.getUserList(Set.of(7001L, 8001L))).thenReturn(List.of(
                enabledUser(7001L, "pqc-leader-a", "PQC组长A"),
                enabledUser(8001L, "pqc-employee-a", "PQC员工A")));
        when(templateResolver.resolve(any(MesFrontlineTemplateRequest.class))).thenReturn(
                new MesFrontlineTemplateDescriptor("PQC_SIMPLIFIED", "PQC", ROUTE_PROCESS_ID, PROCESS_ID, 8001L));

        MesFrontlineEmployeeSwitchResult result = service.switchPqcActualEmployee(LOGIN_USER_ID, WORK_ORDER_ID,
                ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID, 8001L);

        assertEquals(LOGIN_USER_ID, result.loginUserId());
        assertEquals(8001L, result.actualEmployeeId());
        assertEquals("PQC_SIMPLIFIED", result.template().templateNo());
    }

    private void givenWorkOrderProductAndRoute() {
        when(workOrderMapper.selectListByIds(Set.of(WORK_ORDER_ID))).thenReturn(List.of(workOrder(WORK_ORDER_ID, PRODUCT_ID)));
        when(itemService.getItemMap(Set.of(PRODUCT_ID))).thenReturn(Map.of(PRODUCT_ID,
                MesMdItemDO.builder().id(PRODUCT_ID).code("ITEM-PQC").name("PQC 产品").build()));
        when(routeMapper.selectListByIdsIgnoreDeleted(Set.of(ROUTE_ID))).thenReturn(List.of(route(ROUTE_ID)));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
    }

    private static MesProProcessPoolDO activePool(Long workOrderId, Long routeId, Long routeProcessId, Long processId,
                                                  LocalDateTime latestSubmitTime) {
        return MesProProcessPoolDO.builder()
                .workOrderId(workOrderId)
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .poolStatus(MesProProcessPoolDO.STATUS_ACTIVE)
                .latestSubmitTime(latestSubmitTime)
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, Long productId) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code("WO-PQC-001")
                .name("PQC 活跃订单")
                .productId(productId)
                .build();
    }

    private static MesProRouteDO route(Long id) {
        return MesProRouteDO.builder()
                .id(id)
                .code("ROUTE-PQC")
                .name("PQC 产品路线")
                .build();
    }

    private static MesProRouteProcessDO routeProcess(Long routeProcessId, Long routeId, Long processId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(processId)
                .workstationId(6001L)
                .sort(sort)
                .build();
    }

    private static MesProProcessDO process(Long id, String code, String name) {
        return MesProProcessDO.builder()
                .id(id)
                .code(code)
                .name(name)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesProcessPoolTeamLeaderScopeDO scope(Long leaderUserId, String scopeType, Long employeeUserId) {
        return MesProcessPoolTeamLeaderScopeDO.builder()
                .leaderUserId(leaderUserId)
                .leaderType(LEADER_TYPE_PQC)
                .scopeType(scopeType)
                .employeeUserId(employeeUserId)
                .enabled(Boolean.TRUE)
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
}

