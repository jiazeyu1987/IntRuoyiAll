package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderLossReasonRouteScopeTest {
    @Mock private MesRouteStartProductionLeaderAuthorizationService authorization;
    @Mock private MesProRouteProcessMapper processes;
    @InjectMocks private MesTeamLeaderLossReasonServiceImpl service;

    @Test
    void exactRouteMustBeAuthorizedBeforeReadingItsProcesses() {
        when(processes.selectListByRouteId(9001L)).thenReturn(List.of());
        assertTrue(service.listLossReasonRowsForRoute(507L, 9001L).isEmpty());
        var order = inOrder(authorization, processes);
        order.verify(authorization).assertCanJoinRoute(507L, 9001L);
        order.verify(processes).selectListByRouteId(9001L);
        verify(authorization, never()).listAuthorizedRouteProcesses(any());
    }

    @Test
    void unauthorizedRouteMustNotReadAnyProcessData() {
        doThrow(new IllegalStateException("denied")).when(authorization).assertCanJoinRoute(507L, 9001L);
        assertThrows(IllegalStateException.class, () -> service.listLossReasonRowsForRoute(507L, 9001L));
        verifyNoInteractions(processes);
    }
}
