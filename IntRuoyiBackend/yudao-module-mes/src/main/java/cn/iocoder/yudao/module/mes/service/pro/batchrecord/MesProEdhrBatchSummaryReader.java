package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Page-scoped facts only: reading a list never provisions or repairs execution tasks. */
@Service
@RequiredArgsConstructor
public class MesProEdhrBatchSummaryReader {
    private final MesProEdhrReleaseTransactionMapper releases;
    private final MesProEdhrBatchExecutionOriginMapper origins;
    private final MesProEdhrOperationAuditEventMapper audits;
    private final MesProEdhrRecordChangeEventMapper changes;
    private final MesProEdhrNonconformanceReviewMapper reviews;
    private final MesMarketReleaseAccess access;

    public record Facts(MesProEdhrReleaseTransactionDO release, Long activeOrderId,
                        MesProEdhrRecordChangeEventDO pendingVoid, boolean frozen,
                        MesMarketReleaseAccess.Access marketAccess) {}

    public Map<Long, Facts> read(List<MesProEdhrBatchExecutionDO> batches, Long actor,
                               Function<MesProEdhrOperationAuditEventDO, Long> auditSource) {
        if (batches.isEmpty()) return Map.of();
        List<Long> ids = batches.stream().map(MesProEdhrBatchExecutionDO::getId).toList();
        Map<Long, MesProEdhrReleaseTransactionDO> latestReleases = new HashMap<>();
        for (var release : releases.selectLatestByBatchExecutionIds(ids)) {
            latestReleases.putIfAbsent(release.getBatchExecutionId(), release);
        }
        var originRows = origins.selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .in(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId, ids));
        Map<Long, Set<Long>> originIds = new HashMap<>();
        for (var origin : originRows) {
            if (origin.getActiveOrderId() != null) originIds
                    .computeIfAbsent(origin.getBatchExecutionId(), ignored -> new LinkedHashSet<>())
                    .add(origin.getActiveOrderId());
        }
        Map<Long, Long> activeOrderIds = new HashMap<>();
        for (Long id : ids) {
            Set<Long> sources = originIds.getOrDefault(id, Set.of());
            if (sources.size() > 1) throw new IllegalStateException("批次执行存在多个不同的活跃订单来源：" + id);
            if (!sources.isEmpty()) activeOrderIds.put(id, sources.iterator().next());
        }
        List<Long> auditIds = ids.stream().filter(id -> !activeOrderIds.containsKey(id)).toList();
        if (!auditIds.isEmpty()) {
            Map<Long, Set<Long>> auditSources = new HashMap<>();
            for (var audit : audits.selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                    .in(MesProEdhrOperationAuditEventDO::getBatchExecutionId, auditIds)
                    .eq(MesProEdhrOperationAuditEventDO::getOperationType, "OPEN")
                    .eq(MesProEdhrOperationAuditEventDO::getResultStatus, "SUCCESS"))) {
                Long source = auditSource.apply(audit);
                if (source != null) auditSources.computeIfAbsent(audit.getBatchExecutionId(), ignored -> new LinkedHashSet<>()).add(source);
            }
            for (Long id : auditIds) {
                Set<Long> sources = auditSources.getOrDefault(id, Set.of());
                if (sources.size() > 1) throw new IllegalStateException("批次执行存在多个不同的活跃订单审计来源：" + id);
                if (!sources.isEmpty()) activeOrderIds.put(id, sources.iterator().next());
            }
        }
        Map<Long, MesProEdhrRecordChangeEventDO> pendingVoids = new HashMap<>();
        // Existing single-row reader uses LIMIT 1 without ordering; do not invent a new status priority.
        for (var change : changes.selectList(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .in(MesProEdhrRecordChangeEventDO::getBatchExecutionId, ids)
                .eq(MesProEdhrRecordChangeEventDO::getTargetScope, "BATCH")
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, "VOID")
                .eq(MesProEdhrRecordChangeEventDO::getChangeStatus, "SUBMITTED"))) {
            pendingVoids.putIfAbsent(change.getBatchExecutionId(), change);
        }
        Set<Long> frozen = readFrozen(batches, originIds);
        var accesses = access.getAll(ids, actor);
        Map<Long, Facts> result = new LinkedHashMap<>();
        for (Long id : ids) result.put(id, new Facts(latestReleases.get(id), activeOrderIds.get(id),
                pendingVoids.get(id), frozen.contains(id), accesses.get(id)));
        return result;
    }

    private Set<Long> readFrozen(List<MesProEdhrBatchExecutionDO> batches, Map<Long, Set<Long>> origins) {
        List<Long> ids = batches.stream().map(MesProEdhrBatchExecutionDO::getId).toList();
        Set<Long> workOrderIds = batches.stream().map(MesProEdhrBatchExecutionDO::getWorkOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> activeOrderIds = origins.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        Map<Long, Long> pendingCounts = new HashMap<>();
        Set<Long> blockedWorkOrders = new HashSet<>();
        Set<Long> blockedActiveOrders = new HashSet<>();
        for (var count : reviews.selectFreezeCounts(ids, workOrderIds, activeOrderIds)) {
            switch (count.getScope()) {
                case "BATCH" -> pendingCounts.put(count.getScopeId(), count.getReviewCount());
                case "WORK_ORDER" -> blockedWorkOrders.add(count.getScopeId());
                case "ACTIVE_ORDER" -> blockedActiveOrders.add(count.getScopeId());
                default -> throw new IllegalStateException("Unknown freeze scope: " + count.getScope());
            }
        }
        Set<Long> frozen = new HashSet<>();
        for (var batch : batches) {
            long count = pendingCounts.getOrDefault(batch.getId(), 0L);
            // Preserve selectPendingByBatchExecutionId/selectOne's duplicate-evidence failure.
            if (count > 1) throw new TooManyResultsException("Expected one pending review for batch " + batch.getId() + " but found " + count);
            if (count == 1 || blockedWorkOrders.contains(batch.getWorkOrderId())
                    || origins.getOrDefault(batch.getId(), Set.of()).stream().anyMatch(blockedActiveOrders::contains)) {
                frozen.add(batch.getId());
            }
        }
        return frozen;
    }
}
