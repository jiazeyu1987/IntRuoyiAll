package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class MesProductionReleaseFormalFactSnapshots {

    private static final String ACTIVE_ORDER_FACTS_PREFIX = "ACTIVE_ORDER_FACTS:";

    private MesProductionReleaseFormalFactSnapshots() {
    }

    public static boolean isActiveOrderFactsSnapshot(String snapshotHash) {
        return StrUtil.startWith(snapshotHash, ACTIVE_ORDER_FACTS_PREFIX);
    }

    public static String activeOrderFactsSnapshotHash(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String pqcDecision,
            Long pqcDecidedBy,
            Object pqcDecidedAt) {
        requireApplicationFacts(application, pqcDecision, pqcDecidedBy, pqcDecidedAt);
        return ACTIVE_ORDER_FACTS_PREFIX + MesReleaseFlowIdempotency.payloadHash(
                value(application.getId()),
                value(application.getActiveOrderId()),
                value(application.getWorkOrderId()),
                value(application.getWorkOrderCode()),
                value(application.getBatchCode()),
                value(application.getBatchExecutionId()),
                value(application.getPqcReleaseWorkTaskId()),
                value(application.getSourceSnapshotHash()),
                StrUtil.trim(pqcDecision),
                value(pqcDecidedBy),
                value(pqcDecidedAt));
    }

    public static String recomputeActiveOrderFactsSnapshot(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        return activeOrderFactsSnapshotHash(application,
                application == null ? null : application.getPqcDecision(),
                application == null ? null : application.getPqcDecidedBy(),
                application == null ? null : application.getPqcDecidedAt());
    }

    private static void requireApplicationFacts(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String pqcDecision,
            Long pqcDecidedBy,
            Object pqcDecidedAt) {
        if (application == null
                || application.getId() == null
                || application.getActiveOrderId() == null
                || application.getWorkOrderId() == null
                || application.getBatchExecutionId() == null
                || application.getPqcReleaseWorkTaskId() == null
                || pqcDecidedBy == null
                || pqcDecidedAt == null
                || StrUtil.hasBlank(application.getWorkOrderCode(), application.getBatchCode(),
                application.getSourceSnapshotHash(), pqcDecision)) {
            throw new IllegalStateException("active order formal facts are incomplete");
        }
    }

    private static String value(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.plusNanos(500_000_000L).truncatedTo(ChronoUnit.SECONDS).toString();
        }
        return Objects.toString(value, "");
    }
}
