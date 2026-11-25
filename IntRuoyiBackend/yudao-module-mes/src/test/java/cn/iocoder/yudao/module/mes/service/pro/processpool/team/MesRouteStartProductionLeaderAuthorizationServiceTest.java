package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesRouteStartProductionLeaderAuthorizationServiceTest {

    private static final Long ADMIN_USER_ID = 1L;
    private static final String TEAM_LEADER_MAINTAIN_PERMISSION = "mes:pro-process-pool-team-leader:maintain";

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private MesPackagingLeaderAccess packagingAccess;

    private MesRouteStartProductionLeaderAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new MesRouteStartProductionLeaderAuthorizationServiceImpl(routeMapper, routeProcessMapper,
                routeVersionMapper, permissionApi);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "packagingAccess", packagingAccess);
        lenient().when(routeMapper.selectById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> flowRoute(invocation.getArgument(0)));
        lenient().when(routeMapper.selectBatchIds(any())).thenAnswer(invocation -> {
            java.util.Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().map(MesRouteStartProductionLeaderAuthorizationServiceTest::flowRoute).toList();
        });
    }

    @Test
    void assertCanJoinRoute_allowsConfiguredUser() {
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USERS", 3001L)));

        assertDoesNotThrow(() -> service.assertCanJoinRoute(3001L, 101L));

        verify(permissionApi, never()).hasAnyPermissions(any(), any());
        verify(adminUserApi, never()).getUserMap(any());
    }

    @Test
    void assertCanJoinRoute_allowsMemberOfConfiguredRole() {
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "ROLE", 77L)));
        when(permissionApi.getUserRoleIdListByUserId(3001L)).thenReturn(Set.of(77L));

        assertDoesNotThrow(() -> service.assertCanJoinRoute(3001L, 101L));
    }

    @Test
    void assertCanJoinRoute_rejectsUnassignedUserWithConfiguredLeaderNames() {
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USER", 3002L, 3003L)));
        when(adminUserApi.getUserMap(Set.of(3002L, 3003L))).thenReturn(Map.of(
                3002L, user(3002L, "张三"), 3003L, user(3003L, "李四")));
        lenient().when(permissionApi.hasAnyPermissions(3001L, TEAM_LEADER_MAINTAIN_PERMISSION)).thenReturn(true);

        ServiceException error = assertThrows(ServiceException.class, () -> service.assertCanJoinRoute(3001L, 101L));

        assertEquals("当前工艺路线的生产组长是[张三、李四]，你没有权限。", error.getMessage());
        verify(permissionApi, never()).hasAnyPermissions(3001L, TEAM_LEADER_MAINTAIN_PERMISSION);
    }

    @Test
    void assertCanJoinRoute_rejectsWrongRoleAndShowsActualRoleMembers() {
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "ROLE", 77L)));
        when(permissionApi.getUserRoleIdListByUserId(3001L)).thenReturn(Set.of(88L));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(77L))).thenReturn(Set.of(3002L));
        when(adminUserApi.getUserMap(Set.of(3002L))).thenReturn(Map.of(3002L, user(3002L, "王五")));

        ServiceException error = assertThrows(ServiceException.class, () -> service.assertCanJoinRoute(3001L, 101L));

        assertEquals("当前工艺路线的生产组长是[王五]，你没有权限。", error.getMessage());
    }

    @Test
    void assertCanJoinRoute_rejectsRouteWithNoConfiguredLeader() {
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, emptyRouteStartLeaderSnapshot(101L)));

        ServiceException error = assertThrows(ServiceException.class, () -> service.assertCanJoinRoute(3001L, 101L));

        assertEquals("当前工艺路线未配置生产组长，无法加入活跃订单。", error.getMessage());
        verify(adminUserApi, never()).getUserMap(any());
    }

    @Test
    void assertCanJoinRoute_rejectsRouteWithoutActiveVersion() {
        ServiceException error = assertThrows(ServiceException.class, () -> service.assertCanJoinRoute(3001L, 101L));

        assertEquals("当前工艺路线未配置生产组长，无法加入活跃订单。", error.getMessage());
    }

    @Test
    void assertCanJoinRoute_doesNotAuthorizeFromInactiveVersion() {
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USERS", 3001L))
                        .setActive(false));

        ServiceException error = assertThrows(ServiceException.class, () -> service.assertCanJoinRoute(3001L, 101L));

        assertEquals("当前工艺路线未配置生产组长，无法加入活跃订单。", error.getMessage());
    }

    private static AdminUserRespDTO user(Long id, String name) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(name);
        return user;
    }

    @Test
    void listResponsibleRoutes_ignoresMaintainPermissionAndReturnsOnlyConfiguredUserRoutes() {
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USER", ADMIN_USER_ID)),
                activeRouteVersion(9002L, 102L, emptyRouteStartLeaderSnapshot(102L))));
        when(routeMapper.selectBatchIds(Set.of(101L))).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).nodeType("FLOW").code("R-PUMP").name("球囊扩张压力泵").build()));

        List<MesProRouteDO> result = service.listResponsibleRoutes(ADMIN_USER_ID);

        assertEquals(List.of(101L), result.stream().map(MesProRouteDO::getId).toList());
        verify(permissionApi, never()).hasAnyPermissions(ADMIN_USER_ID, TEAM_LEADER_MAINTAIN_PERMISSION);
        verify(permissionApi, never()).getUserRoleIdListByUserId(ADMIN_USER_ID);
    }

    @Test
    void listResponsibleRoutes_matchesConfiguredRoleWithoutUsingMaintenanceScope() {
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USER", 999L)),
                activeRouteVersion(9002L, 102L, routeStartLeaderSnapshot(102L, "ROLE", 77L))));
        when(permissionApi.getUserRoleIdListByUserId(3001L)).thenReturn(Set.of(77L));
        when(routeMapper.selectBatchIds(Set.of(102L))).thenReturn(List.of(
                MesProRouteDO.builder().id(102L).nodeType("FLOW").code("R-PRESS").name("按压式球囊扩充压力泵").build()));

        List<MesProRouteDO> result = service.listResponsibleRoutes(3001L);

        assertEquals(List.of(102L), result.stream().map(MesProRouteDO::getId).toList());
        verify(permissionApi, never()).hasAnyPermissions(3001L, TEAM_LEADER_MAINTAIN_PERMISSION);
    }

    @Test
    void listResponsibleRoutes_ignoresActiveSnapshotWhoseRouteWasDeletedBeforeParsingResponsibilities() {
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USER", ADMIN_USER_ID))));
        when(routeMapper.selectBatchIds(Set.of(101L))).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).nodeType("FLOW").code("R-PUMP").name("球囊扩张压力泵").build()));

        List<MesProRouteDO> result = service.listResponsibleRoutes(ADMIN_USER_ID);

        assertEquals(List.of(101L), result.stream().map(MesProRouteDO::getId).toList());
        verify(routeVersionMapper).selectActiveListWithExistingRoutes();
        verify(routeVersionMapper, never()).selectList(any());
        verify(permissionApi, never()).hasAnyPermissions(ADMIN_USER_ID, TEAM_LEADER_MAINTAIN_PERMISSION);
        verify(permissionApi, never()).getUserRoleIdListByUserId(ADMIN_USER_ID);
    }

    @Test
    void activeVersionMapperFiltersOutDeletedParentRoutesBeforeScopeParsing() throws NoSuchMethodException {
        Select select = MesProRouteVersionMapper.class
                .getDeclaredMethod("selectActiveListWithExistingRoutes")
                .getAnnotation(Select.class);

        String sql = String.join(" ", select.value());

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertNotNull(select),
                () -> org.junit.jupiter.api.Assertions.assertTrue(sql.contains("INNER JOIN mes_pro_route r")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(sql.contains("r.deleted = b'0'")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(sql.contains("v.deleted = b'0'")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(sql.contains("v.active = b'1'")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(sql.contains("v.lifecycle_status = 'ACTIVE'")));
    }

    @Test
    void listResponsibleRoutes_failsWhenExistingRouteFilteredActiveVersionCannotLoadRouteSummary() {
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USER", ADMIN_USER_ID))));
        when(routeMapper.selectBatchIds(Set.of(101L))).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> service.listResponsibleRoutes(ADMIN_USER_ID));
    }

    @Test
    void listAuthorizedRouteProcesses_ignoresMaintainPermissionAndReturnsOnlyConfiguredRouteProcesses() {
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, routeStartLeaderSnapshot(101L, "USER", ADMIN_USER_ID)),
                activeRouteVersion(9002L, 102L, emptyRouteStartLeaderSnapshot(102L))));
        lenient().when(permissionApi.hasAnyPermissions(ADMIN_USER_ID, TEAM_LEADER_MAINTAIN_PERMISSION))
                .thenReturn(true);
        when(routeProcessMapper.selectListByRouteIds(Set.of(101L))).thenReturn(List.of(
                routeProcess(1002L, 101L, 202L, 20),
                routeProcess(1001L, 101L, 201L, 10)));

        List<MesProRouteProcessDO> result = service.listAuthorizedRouteProcesses(ADMIN_USER_ID);

        assertEquals(List.of(1001L, 1002L), result.stream().map(MesProRouteProcessDO::getId).toList());
        verify(permissionApi, never()).getUserRoleIdListByUserId(ADMIN_USER_ID);
    }

    @Test
    void assertCanMaintainRouteProcess_rejectsTeamLeaderMaintainerWithoutRouteStartSnapshotMembership() {
        when(routeProcessMapper.selectById(1001L)).thenReturn(routeProcess(1001L, 101L, 201L, 10));
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, emptyRouteStartLeaderSnapshot(101L)));
        lenient().when(permissionApi.hasAnyPermissions(ADMIN_USER_ID, TEAM_LEADER_MAINTAIN_PERMISSION))
                .thenReturn(true);

        assertThrows(ServiceException.class, () -> service.assertCanMaintainRouteProcess(ADMIN_USER_ID, 1001L));

        verify(permissionApi, never()).getUserRoleIdListByUserId(ADMIN_USER_ID);
    }

    @Test
    void listResponsibleRoutes_ignoresMalformedGroupSnapshotBeforeParsingFlowResponsibilities() {
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(
                activeRouteVersion(41L, 201L, routeStartLeaderSnapshot(101L, "USERS", 3001L)),
                activeRouteVersion(48L, 101L, routeStartLeaderSnapshot(101L, "USERS", 3001L))));
        when(routeMapper.selectBatchIds(Set.of(201L, 101L))).thenReturn(List.of(groupRoute(201L), flowRoute(101L)));

        assertEquals(List.of(101L), service.listResponsibleRoutes(3001L).stream().map(MesProRouteDO::getId).toList());
    }

    @Test
    void listAuthorizedRouteProcesses_ignoresGroupSnapshotAndPackagingGroupScope() {
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(
                activeRouteVersion(41L, 201L, "{malformed"),
                activeRouteVersion(48L, 101L, routeStartLeaderSnapshot(101L, "USERS", 3001L))));
        when(routeMapper.selectBatchIds(Set.of(201L, 101L))).thenReturn(List.of(groupRoute(201L), flowRoute(101L)));
        when(packagingAccess.activeRouteIds(3001L)).thenReturn(Set.of(201L));
        when(routeMapper.selectBatchIds(Set.of(201L))).thenReturn(List.of(groupRoute(201L)));
        when(routeProcessMapper.selectListByRouteIds(Set.of(101L))).thenReturn(List.of(routeProcess(1001L, 101L, 1L, 1)));

        assertEquals(List.of(1001L), service.listAuthorizedRouteProcesses(3001L).stream()
                .map(MesProRouteProcessDO::getId).toList());
        verify(routeProcessMapper, never()).selectListByRouteIds(Set.of(201L, 101L));
    }

    @Test
    void listResponsibleRoutes_doesNotReturnPackagingGroupWithoutFlowResponsibility() {
        when(packagingAccess.activeRouteIds(3001L)).thenReturn(Set.of(201L));
        when(routeMapper.selectBatchIds(Set.of(201L))).thenReturn(List.of(groupRoute(201L)));

        assertEquals(List.of(), service.listResponsibleRoutes(3001L));
    }

    @Test
    void assertCanJoinRoute_rejectsGroupBeforeReadingItsInheritedSnapshot() {
        when(routeMapper.selectById(201L)).thenReturn(groupRoute(201L));

        ServiceException error = assertThrows(ServiceException.class, () -> service.assertCanJoinRoute(3001L, 201L));

        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED.getCode(), error.getCode());
        verify(routeVersionMapper, never()).selectActiveByRouteId(201L);
    }

    @Test
    void assertCanMaintainRouteProcess_rejectsGroupEvenWithMatchingLeader() {
        when(routeProcessMapper.selectById(2001L)).thenReturn(routeProcess(2001L, 201L, 1L, 1));
        when(routeMapper.selectById(201L)).thenReturn(groupRoute(201L));

        assertThrows(ServiceException.class, () -> service.assertCanMaintainRouteProcess(3001L, 2001L));

        verify(routeVersionMapper, never()).selectActiveByRouteId(201L);
    }

    @Test
    void malformedFlowSnapshotStillFailsBothWorkbenchListsAndDirectPermissions() {
        MesProRouteVersionDO brokenFlow = activeRouteVersion(48L, 101L,
                routeStartLeaderSnapshot(999L, "USERS", 3001L));
        when(routeVersionMapper.selectActiveListWithExistingRoutes()).thenReturn(List.of(brokenFlow));
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(brokenFlow);
        when(routeProcessMapper.selectById(1001L)).thenReturn(routeProcess(1001L, 101L, 1L, 1));

        for (org.junit.jupiter.api.function.Executable call : List.<org.junit.jupiter.api.function.Executable>of(
                () -> service.listResponsibleRoutes(3001L), () -> service.listAuthorizedRouteProcesses(3001L),
                () -> service.assertCanJoinRoute(3001L, 101L), () -> service.assertCanMaintainRouteProcess(3001L, 1001L))) {
            ServiceException error = assertThrows(ServiceException.class, call);
            org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("routeVersionId=48"));
        }
    }

    @Test
    void unknownRouteTypeFailsInsteadOfGrantingExecutionScope() {
        when(routeMapper.selectById(101L)).thenReturn(flowRoute(101L).setNodeType(null));

        assertThrows(ServiceException.class, () -> service.assertCanJoinRoute(3001L, 101L));
        verify(routeVersionMapper, never()).selectActiveByRouteId(101L);
    }

    private static MesProRouteDO flowRoute(Long id) {
        return MesProRouteDO.builder().id(id).nodeType("FLOW").code("FLOW-" + id).name("流程" + id).build();
    }

    private static MesProRouteDO groupRoute(Long id) {
        return MesProRouteDO.builder().id(id).nodeType("GROUP").code("GROUP-" + id).name("模板" + id).build();
    }

    private static MesProRouteProcessDO routeProcess(Long id, Long routeId, Long processId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .sort(sort)
                .build();
    }

    private static MesProRouteVersionDO activeRouteVersion(Long id, Long routeId, String routeSnapshotJson) {
        return MesProRouteVersionDO.builder()
                .id(id)
                .routeId(routeId)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionMapper.STATUS_ACTIVE)
                .routeSnapshotJson(routeSnapshotJson)
                .build();
    }

    private static String emptyRouteStartLeaderSnapshot(Long routeId) {
        return """
                {
                  "routeId": %d,
                  "configSnapshots": {
                    "routeStartProductionLeaders": []
                  }
                }
                """.formatted(routeId);
    }

    private static String routeStartLeaderSnapshot(Long routeId, String candidateSourceType,
                                                   Long... candidateSourceIds) {
        String sourceIds = java.util.Arrays.stream(candidateSourceIds)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return """
                {
                  "routeId": %d,
                  "configSnapshots": {
                    "routeStartProductionLeaders": [
                      {
                        "productionLineId": %d,
                        "candidateSourceType": "%s",
                        "candidateSourceIds": [%s]
                      }
                    ]
                  }
                }
                """.formatted(routeId, routeId, candidateSourceType, sourceIds);
    }
}
