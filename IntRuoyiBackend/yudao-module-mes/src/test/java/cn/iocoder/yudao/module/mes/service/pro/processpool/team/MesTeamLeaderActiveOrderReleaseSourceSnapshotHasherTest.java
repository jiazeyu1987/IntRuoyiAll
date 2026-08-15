package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest {

    private final MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher hasher =
            new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher();

    @Test
    void hashIsStableAcrossAuthoritativeListOrder() {
        MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input first = input(
                List.of(snapshot(2L, 20L), snapshot(1L, 10L)),
                List.of(completion(2L, 20L, "hash-2"), completion(1L, 10L, "hash-1")),
                List.of(task(2L, 20L), task(1L, 10L)),
                List.of(detail(2L, 20L), detail(1L, 10L)));
        MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input reordered = input(
                List.of(snapshot(1L, 10L), snapshot(2L, 20L)),
                List.of(completion(1L, 10L, "hash-1"), completion(2L, 20L, "hash-2")),
                List.of(task(1L, 10L), task(2L, 20L)),
                List.of(detail(1L, 10L), detail(2L, 20L)));

        assertEquals(hasher.hash(first), hasher.hash(reordered));
    }

    @Test
    void productionOrInspectionEvidenceChangeChangesHash() {
        String baseline = hasher.hash(input(
                List.of(snapshot(1L, 10L)),
                List.of(completion(1L, 10L, "hash-1")),
                List.of(task(1L, 10L)),
                List.of(detail(1L, 10L))));
        String productionChanged = hasher.hash(input(
                List.of(snapshot(1L, 10L)),
                List.of(completion(1L, 10L, "hash-changed")),
                List.of(task(1L, 10L)),
                List.of(detail(1L, 10L))));
        MesPqcInspectionTaskDO changedTask = task(1L, 10L).setTaskStatus("REJECTED");
        String inspectionChanged = hasher.hash(input(
                List.of(snapshot(1L, 10L)),
                List.of(completion(1L, 10L, "hash-1")),
                List.of(changedTask),
                List.of(detail(1L, 10L))));

        assertNotEquals(baseline, productionChanged);
        assertNotEquals(baseline, inspectionChanged);
    }

    private static MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input input(
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
            List<MesProcessPoolOrderProcessCompletionDO> completions,
            List<MesPqcInspectionTaskDO> tasks,
            List<MesPqcProcessInspectionAggregateDetailDO> details) {
        return new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input(
                1L,
                MesProcessPoolActiveOrderDO.builder().id(2001L).leaderUserId(1001L).workOrderId(3001L)
                        .routeId(4001L).routeVersionId(4002L).activeStatus("ACTIVE")
                        .businessStatus("PRODUCING").erpFixedQuantitySnapshot(BigDecimal.TEN).build(),
                MesProWorkOrderDO.builder().id(3001L).code("WO-001").batchCode("BATCH-001")
                        .productId(3101L).quantity(BigDecimal.TEN).build(),
                snapshots, completions, tasks, details);
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot(Long routeProcessId, Long processId) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(4000L + routeProcessId).activeOrderId(2001L).workOrderId(3001L)
                .routeId(4001L).routeVersionId(4002L)
                .routeProcessId(routeProcessId).processId(processId).build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completion(
            Long routeProcessId, Long processId, String aggregateHash) {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(5000L + routeProcessId).workOrderId(3001L)
                .routeProcessId(routeProcessId).processId(processId)
                .targetQuantity(BigDecimal.TEN).confirmedQuantity(BigDecimal.TEN)
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .completedAt(LocalDateTime.of(2026, 8, 14, 10, 0))
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(5200L + routeProcessId).lastEventId(5300L + routeProcessId)
                .lastReviewId(5400L + routeProcessId).sourceEventIdsJson("[5301]")
                .sourceAllocationIdsJson("[5501]").aggregateHash(aggregateHash)
                .backfillIdempotencyKey("backfill-" + routeProcessId).build();
    }

    private static MesPqcInspectionTaskDO task(Long routeProcessId, Long processId) {
        return MesPqcInspectionTaskDO.builder().id(6000L + routeProcessId)
                .activeOrderId(2001L).workOrderId(3001L).routeId(4001L).routeVersionId(4002L)
                .routeProcessId(routeProcessId).processId(processId)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build();
    }

    private static MesPqcProcessInspectionAggregateDetailDO detail(Long routeProcessId, Long processId) {
        return MesPqcProcessInspectionAggregateDetailDO.builder().id(7000L + routeProcessId)
                .pqcTaskId(6000L + routeProcessId).activeOrderId(2001L).workOrderId(3001L)
                .routeId(4001L).routeVersionId(4002L)
                .routeProcessId(routeProcessId).processId(processId).build();
    }
}
