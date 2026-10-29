package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class MesProRouteLegacySummaryUsageTest {

    @Test
    void executionConsumers_shouldRejectArchiveBeforeCurrentMasterConfigurationCanBeUsed() {
        var batch = mock(MesProEdhrBatchExecutionServiceImpl.class, CALLS_REAL_METHODS);
        var schedule = mock(MesProScheduleOrderServiceImpl.class, CALLS_REAL_METHODS);
        var activeOrder = mock(MesTeamLeaderActiveOrderServiceImpl.class, CALLS_REAL_METHODS);
        MesProRouteVersionDO version = MesProRouteVersionDO.builder().id(5L).routeId(922022L)
                .active(false).lifecycleStatus(MesRouteLegacySummaryIdentity.STATUS)
                .routeSnapshotJson("{\"routeId\":922022}")
                .routeSnapshotFormatVersion(MesRouteLegacySummaryIdentity.FORMAT_VERSION).build();
        var route = MesProRouteDO.builder().id(922022L).build();
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(batch,
                "buildBatchTaskConfigs", route, List.of(), version));
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(batch,
                "requireFrozenRouteIdentity", version, route.getId()));
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(schedule,
                "buildScheduleConfigMap", version));
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(activeOrder,
                "toRouteProcessSources", null, version));
    }

    @Test
    void markerAlone_shouldNeverAuthorizeSourceFreeAuthoring() {
        MesProRouteVersionDO arbitrary = MesProRouteVersionDO.builder().id(100L).routeId(922022L)
                .active(false).lifecycleStatus(MesRouteLegacySummaryIdentity.STATUS)
                .routeSnapshotFormatVersion(MesRouteLegacySummaryIdentity.FORMAT_VERSION).build();
        assertThrows(IllegalStateException.class,
                () -> MesProRouteVersionUsagePolicy.knownArchivedSummary(List.of(arbitrary)));
    }
}
