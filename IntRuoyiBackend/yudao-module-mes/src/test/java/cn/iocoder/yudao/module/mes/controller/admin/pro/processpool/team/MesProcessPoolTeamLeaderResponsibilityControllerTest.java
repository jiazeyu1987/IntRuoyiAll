package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesRouteStartProductionLeaderAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolTeamLeaderResponsibilityControllerTest {

    @Mock
    private MesRouteStartProductionLeaderAuthorizationService authorizationService;

    @InjectMocks
    private MesProcessPoolTeamLeaderResponsibilityController controller;

    @Test
    void getResponsibleRoutes_returnsOnlyCurrentUsersFormalRouteStartResponsibilities() {
        when(authorizationService.listResponsibleRoutes(3001L)).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).code("R-PUMP").name("球囊扩张压力泵").build(),
                MesProRouteDO.builder().id(102L).code("R-PRESS").name("按压式球囊扩充压力泵").build()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            var response = controller.getResponsibleRoutes().getData();

            assertEquals(List.of(101L, 102L), response.stream().map(item -> item.getRouteId()).toList());
            assertEquals(List.of("R-PUMP", "R-PRESS"),
                    response.stream().map(item -> item.getRouteCode()).toList());
            assertEquals(List.of("球囊扩张压力泵", "按压式球囊扩充压力泵"),
                    response.stream().map(item -> item.getRouteName()).toList());
        }
        verify(authorizationService).listResponsibleRoutes(3001L);
    }

    @Test
    void mappingsAndPermissions_matchResponsibilityContract() throws Exception {
        RequestMapping requestMapping = MesProcessPoolTeamLeaderResponsibilityController.class
                .getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/mes/pro/process-pool/team-leader"}, requestMapping.value());

        var method = MesProcessPoolTeamLeaderResponsibilityController.class
                .getDeclaredMethod("getResponsibleRoutes");
        assertArrayEquals(new String[]{"/responsible-routes"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-process-pool-team-leader:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }

}
