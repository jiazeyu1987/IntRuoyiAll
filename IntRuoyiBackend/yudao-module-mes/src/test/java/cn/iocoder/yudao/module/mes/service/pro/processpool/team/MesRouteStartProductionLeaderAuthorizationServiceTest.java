package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesRouteStartProductionLeaderAuthorizationServiceTest {

    private static final Long ADMIN_USER_ID = 1L;
    private static final String TEAM_LEADER_MAINTAIN_PERMISSION = "mes:pro-process-pool-team-leader:maintain";

    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private PermissionApi permissionApi;

    private MesRouteStartProductionLeaderAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new MesRouteStartProductionLeaderAuthorizationServiceImpl(routeProcessMapper, routeVersionMapper,
                permissionApi);
    }

    @Test
    void listAuthorizedRouteProcesses_allowsTeamLeaderMaintainerToMaintainActiveRouteProcesses() {
        when(routeVersionMapper.selectList(any())).thenReturn(List.of(
                activeRouteVersion(9001L, 101L, emptyRouteStartLeaderSnapshot(101L)),
                activeRouteVersion(9002L, 102L, emptyRouteStartLeaderSnapshot(102L))));
        when(permissionApi.hasAnyPermissions(ADMIN_USER_ID, TEAM_LEADER_MAINTAIN_PERMISSION))
                .thenReturn(true);
        when(routeProcessMapper.selectListByRouteIds(Set.of(101L, 102L))).thenReturn(List.of(
                routeProcess(1002L, 101L, 202L, 20),
                routeProcess(1001L, 101L, 201L, 10),
                routeProcess(1003L, 102L, 203L, 30)));

        List<MesProRouteProcessDO> result = service.listAuthorizedRouteProcesses(ADMIN_USER_ID);

        assertEquals(List.of(1001L, 1002L, 1003L), result.stream().map(MesProRouteProcessDO::getId).toList());
        verify(permissionApi, never()).getUserRoleIdListByUserId(ADMIN_USER_ID);
    }

    @Test
    void assertCanMaintainRouteProcess_allowsTeamLeaderMaintainerWithoutRouteStartSnapshotMembership() {
        when(routeProcessMapper.selectById(1001L)).thenReturn(routeProcess(1001L, 101L, 201L, 10));
        when(routeVersionMapper.selectActiveByRouteId(101L)).thenReturn(
                activeRouteVersion(9001L, 101L, emptyRouteStartLeaderSnapshot(101L)));
        when(permissionApi.hasAnyPermissions(ADMIN_USER_ID, TEAM_LEADER_MAINTAIN_PERMISSION))
                .thenReturn(true);

        assertDoesNotThrow(() -> service.assertCanMaintainRouteProcess(ADMIN_USER_ID, 1001L));

        verify(permissionApi, never()).getUserRoleIdListByUserId(ADMIN_USER_ID);
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
}
