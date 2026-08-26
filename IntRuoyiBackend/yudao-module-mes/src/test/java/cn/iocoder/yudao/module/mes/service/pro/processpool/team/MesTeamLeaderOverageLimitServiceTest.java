package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessOverageLimitDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessOverageLimitMapper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderOverageLimitServiceTest {
    @Mock
    private MesProcessPoolTeamProcessOverageLimitMapper mapper;

    @Test
    void acceptsConfiguredLimitAndRejectsOnlyTheExcess() {
        when(mapper.selectByLeaderAndRouteProcess(1L, 2L, 3L))
                .thenReturn(MesProcessPoolTeamProcessOverageLimitDO.builder()
                        .leaderUserId(1L).routeProcessId(2L).processId(3L)
                        .overagePercent(new BigDecimal("10")).enabled(Boolean.TRUE).build());
        MesTeamLeaderOverageLimitService service = new MesTeamLeaderOverageLimitServiceImpl(mapper);

        assertDoesNotThrow(() -> service.assertWithinLimit(1L, 2L, 3L,
                new BigDecimal("1000"), new BigDecimal("1100")));
        assertThrows(ServiceException.class, () -> service.assertWithinLimit(1L, 2L, 3L,
                new BigDecimal("1000"), new BigDecimal("1100.01")));
    }

    @Test
    void listLookupUsesTenPercentWhenLimitIsMissing() {
        when(mapper.selectByLeaderAndRouteProcess(1L, 2L, 3L)).thenReturn(null);
        MesTeamLeaderOverageLimitService service = new MesTeamLeaderOverageLimitServiceImpl(mapper);

        assertEquals(new BigDecimal("10"), service.findPercent(1L, 2L, 3L));
    }

    @Test
    void requiredLimitUsesTenPercentWhenLimitIsMissing() {
        when(mapper.selectByLeaderAndRouteProcess(1L, 2L, 3L)).thenReturn(null);
        MesTeamLeaderOverageLimitService service = new MesTeamLeaderOverageLimitServiceImpl(mapper);

        assertEquals(new BigDecimal("10"), service.requirePercent(1L, 2L, 3L));
    }

    @Test
    void nullStoredPercentAlsoUsesTenPercentDefault() {
        when(mapper.selectByLeaderAndRouteProcess(1L, 2L, 3L))
                .thenReturn(MesProcessPoolTeamProcessOverageLimitDO.builder()
                        .leaderUserId(1L).routeProcessId(2L).processId(3L)
                        .overagePercent(null).enabled(Boolean.TRUE).build());
        MesTeamLeaderOverageLimitService service = new MesTeamLeaderOverageLimitServiceImpl(mapper);

        assertEquals(new BigDecimal("10"), service.findPercent(1L, 2L, 3L));
        assertEquals(new BigDecimal("10"), service.requirePercent(1L, 2L, 3L));
    }
}
