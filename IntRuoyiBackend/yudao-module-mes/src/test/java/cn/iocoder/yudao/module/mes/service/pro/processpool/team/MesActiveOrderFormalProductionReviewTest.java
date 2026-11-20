package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesActiveOrderFormalProductionReviewTest {
    private final MesProcessPoolActiveOrderDO order = new MesProcessPoolActiveOrderDO().setWorkOrderId(1L);
    private final MesProcessPoolActiveOrderProcessSnapshotDO first =
            new MesProcessPoolActiveOrderProcessSnapshotDO().setRouteProcessId(2L).setProcessId(3L);
    private final MesProcessPoolActiveOrderProcessSnapshotDO second =
            new MesProcessPoolActiveOrderProcessSnapshotDO().setRouteProcessId(4L).setProcessId(5L);

    @Test
    void everyFrozenProcessRequiresAUniqueCompletedReview() {
        assertFalse(complete(List.of()));
        assertFalse(complete(List.of(review(2L, 3L))));
        assertFalse(complete(List.of(review(2L, 3L), review(2L, 3L), review(4L, 5L))));
        assertFalse(complete(List.of(review(2L, 3L).setWorkOrderId(6L), review(4L, 5L))));
        assertTrue(complete(List.of(review(2L, 3L), review(4L, 5L))));
    }

    @Test
    void submittedQuantityWithoutFormalReviewEvidenceDoesNotEnableCompletion() {
        var valid = review(4L, 5L);
        assertFalse(complete(List.of(review(2L, 3L).setCompletionStatus("IN_PROGRESS"), valid)));
        assertFalse(complete(List.of(review(2L, 3L).setConfirmedQuantity(BigDecimal.ZERO), valid)));
        assertFalse(complete(List.of(review(2L, 3L).setLastReviewId(null), valid)));
        assertFalse(complete(List.of(review(2L, 3L).setLastEventId(null), valid)));
        assertFalse(complete(List.of(review(2L, 3L).setSourceAllocationIdsJson(null), valid)));
        assertFalse(complete(List.of(review(2L, 3L).setAggregateHash(null), valid)));
    }

    private boolean complete(List<MesProcessPoolOrderProcessCompletionDO> reviews) {
        return MesTeamLeaderActiveOrderServiceImpl.hasFormalProductionReview(order, List.of(first, second), reviews);
    }

    private MesProcessPoolOrderProcessCompletionDO review(Long routeProcessId, Long processId) {
        return new MesProcessPoolOrderProcessCompletionDO().setId(routeProcessId).setWorkOrderId(1L)
                .setRouteProcessId(routeProcessId).setProcessId(processId).setCompletionStatus("COMPLETED")
                .setTargetQuantity(BigDecimal.TEN).setConfirmedQuantity(BigDecimal.TEN)
                .setLastReviewId(7L).setLastEventId(8L).setSourceEventIdsJson("[8]")
                .setSourceAllocationIdsJson("[9]").setAggregateHash("hash");
    }
}
