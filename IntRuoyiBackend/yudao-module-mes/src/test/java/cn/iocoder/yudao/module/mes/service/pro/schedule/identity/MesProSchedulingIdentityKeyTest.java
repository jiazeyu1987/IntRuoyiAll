package cn.iocoder.yudao.module.mes.service.pro.schedule.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MesProSchedulingIdentityKeyTest {

    @Test
    void routeProcessIdentity_shouldKeepRouteProcessSeparateFromBaseProcess() {
        RouteProcessIdentity firstRouteProcess = RouteProcessIdentity.of(10L, 100L, 1001L);
        RouteProcessIdentity secondRouteProcess = RouteProcessIdentity.of(11L, 101L, 1001L);

        assertNotEquals(firstRouteProcess, secondRouteProcess);
        assertEquals("ROUTE_PROCESS_1001", firstRouteProcess.availabilityKey());
        assertEquals("ROUTE_PROCESS_10_2001", RouteProcessIdentity.legacyAvailabilityKey(10L, 2001L));
    }

    @Test
    void lineProcessIdentity_shouldRemainOnlyLineAndBaseProcessAvailabilityKey() {
        LineProcessIdentity identity = LineProcessIdentity.of(300L, 2001L);

        assertEquals("300_2001", identity.availabilityKey());
        assertEquals("300_2001", LineProcessIdentity.availabilityKey(300L, 2001L));
    }

    @Test
    void scheduleOrderProcessIdentity_shouldKeepHistoricalTaskRelinkKeysCentralized() {
        ScheduleOrderProcessIdentity identity = ScheduleOrderProcessIdentity.of(400L, 1001L, 5001L);

        assertEquals(400L, identity.scheduleOrderId());
        assertEquals(1001L, identity.routeProcessId());
        assertEquals(5001L, identity.scheduleOrderProcessId());
        assertEquals("600_2001", ScheduleOrderProcessIdentity.workOrderProcessKey(600L, 2001L));
        assertEquals("600_SOP_5001", ScheduleOrderProcessIdentity.scheduleOrderProcessTaskKey(600L, 5001L));
    }

    @Test
    void taskAttributionIdentity_shouldExposeCompleteFeedbackAttributionChain() {
        TaskAttributionIdentity identity = TaskAttributionIdentity.of(7001L, 5001L, 400L, 600L);

        assertEquals(7001L, identity.taskId());
        assertEquals(5001L, identity.scheduleOrderProcessId());
        assertEquals(400L, identity.scheduleOrderId());
        assertEquals(600L, identity.workOrderId());
    }

}
