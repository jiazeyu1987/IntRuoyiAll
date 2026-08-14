package cn.iocoder.yudao.module.mes.service.pro.frontline;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesFrontlineProductionSubmitCandidateTest {

    @Test
    void shouldCreateCandidateOnlyWhenProductionEventBelongsToActiveOrderProcessSnapshot() {
        MesFrontlineProductionSubmitCandidate candidate =
                MesFrontlineProductionSubmitCandidate.requireActiveOrderProcessSnapshot(
                        7001L, LocalDateTime.of(2026, 8, 12, 10, 0),
                        5001L, 3001L, 4001L,
                        List.of(new MesFrontlineProductionSubmitCandidate.ActiveOrderProcessSnapshot(
                                5001L, 3001L, 4001L)));

        assertEquals(7001L, candidate.eventId());
        assertEquals(5001L, candidate.activeOrderId());
        assertEquals(3001L, candidate.routeProcessId());
        assertEquals(4001L, candidate.processId());
    }

    @Test
    void shouldRejectCandidateWhenRouteProcessIsNotInActiveOrderProcessSnapshot() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MesFrontlineProductionSubmitCandidate.requireActiveOrderProcessSnapshot(
                        7001L, LocalDateTime.of(2026, 8, 12, 10, 0),
                        5001L, 3999L, 4001L,
                        List.of(new MesFrontlineProductionSubmitCandidate.ActiveOrderProcessSnapshot(
                                5001L, 3001L, 4001L))));

        assertEquals("production submit event is not backed by active-order process snapshot: eventId=7001, activeOrderId=5001, routeProcessId=3999, processId=4001",
                error.getMessage());
    }
}
