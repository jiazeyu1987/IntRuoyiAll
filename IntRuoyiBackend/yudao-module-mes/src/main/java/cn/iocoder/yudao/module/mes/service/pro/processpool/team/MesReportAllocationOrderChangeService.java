package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;

@Service
public class MesReportAllocationOrderChangeService {

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolReportAllocationStateMapper stateMapper;
    private final MesProcessPoolReportAllocationAdjustmentAuditMapper auditMapper;
    private final MesReportAllocationReleaseStateService releaseStateService;
    private final MesTeamLeaderOrderProcessTargetService targetService;
    private final MesReportAllocationQuantityFragmentService fragmentService;
    private final MesTeamLeaderOrderProcessCompletionService completionService;
    private final MesProductionReportManagementSummaryService reportManagementSummaryService;

    public MesReportAllocationOrderChangeService(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolReportAllocationStateMapper stateMapper,
            MesProcessPoolReportAllocationAdjustmentAuditMapper auditMapper,
            MesReportAllocationReleaseStateService releaseStateService,
            MesTeamLeaderOrderProcessTargetService targetService,
            MesReportAllocationQuantityFragmentService fragmentService,
            MesTeamLeaderOrderProcessCompletionService completionService,
            MesProductionReportManagementSummaryService reportManagementSummaryService) {
        this.activeOrderMapper = activeOrderMapper;
        this.eventMapper = eventMapper;
        this.allocationMapper = allocationMapper;
        this.stateMapper = stateMapper;
        this.auditMapper = auditMapper;
        this.releaseStateService = releaseStateService;
        this.targetService = targetService;
        this.fragmentService = fragmentService;
        this.completionService = completionService;
        this.reportManagementSummaryService = reportManagementSummaryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void invalidateActiveOrder(Long activeOrderId, Long actorUserId, String reason) {
        adjustActiveOrder(activeOrderId, null, actorUserId, reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void invalidateWorkOrder(Long workOrderId, Long actorUserId, String reason) {
        for (MesProcessPoolActiveOrderDO activeOrder : activeOrderMapper.selectListByWorkOrderIdForUpdate(workOrderId)) {
            adjustActiveOrder(activeOrder.getId(), null, actorUserId, reason);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reduceWorkOrderAllocations(Long workOrderId, BigDecimal newErpQuantity,
                                           Long actorUserId, String reason) {
        if (newErpQuantity == null || newErpQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "workOrder.newQuantity");
        }
        for (MesProcessPoolActiveOrderDO activeOrder : activeOrderMapper.selectListByWorkOrderIdForUpdate(workOrderId)) {
            adjustActiveOrder(activeOrder.getId(), newErpQuantity, actorUserId, reason);
        }
    }

    private void adjustActiveOrder(Long activeOrderId, BigDecimal newErpQuantity,
                                   Long actorUserId, String reason) {
        List<MesProcessPoolReportAllocationDO> current = allocationMapper
                .selectListByActiveOrderIdForUpdate(activeOrderId);
        if (current.isEmpty()) {
            return;
        }
        if (releaseStateService.findReleasedActiveOrderIdsForUpdate(Set.of(activeOrderId))
                .contains(activeOrderId)) {
            return;
        }
        Map<Long, BigDecimal> desiredByAllocationId = newErpQuantity == null
                ? current.stream().collect(Collectors.toMap(MesProcessPoolReportAllocationDO::getId,
                        ignored -> BigDecimal.ZERO, (a, b) -> a, LinkedHashMap::new))
                : calculateReducedQuantities(current, newErpQuantity);
        Map<Long, List<MesProcessPoolReportAllocationDO>> byEvent = current.stream().collect(Collectors.groupingBy(
                MesProcessPoolReportAllocationDO::getEventId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<MesProcessPoolReportAllocationDO>> entry : byEvent.entrySet()) {
            boolean changed = entry.getValue().stream().anyMatch(row -> row.getAllocatedQuantity()
                    .compareTo(desiredByAllocationId.getOrDefault(row.getId(), BigDecimal.ZERO)) != 0);
            if (changed) {
                applyEventChange(entry.getKey(), activeOrderId, actorUserId, reason, desiredByAllocationId);
            }
        }
    }

    private Map<Long, BigDecimal> calculateReducedQuantities(
            List<MesProcessPoolReportAllocationDO> current, BigDecimal newErpQuantity) {
        Map<Long, BigDecimal> desired = new LinkedHashMap<>();
        Map<TargetKey, List<MesProcessPoolReportAllocationDO>> byTarget = current.stream()
                .collect(Collectors.groupingBy(this::targetKey, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<TargetKey, List<MesProcessPoolReportAllocationDO>> entry : byTarget.entrySet()) {
            TargetKey key = entry.getKey();
            MesTeamLeaderOrderProcessTarget target = targetService.requireTarget(key.activeOrderId(),
                    key.workOrderId(), key.routeProcessId(), key.processId());
            BigDecimal remaining = newErpQuantity.multiply(target.productionQuantityFactor());
            for (MesProcessPoolReportAllocationDO row : entry.getValue().stream()
                    .sorted(Comparator.comparing(MesProcessPoolReportAllocationDO::getId)).toList()) {
                BigDecimal kept = remaining.max(BigDecimal.ZERO).min(row.getAllocatedQuantity());
                desired.put(row.getId(), kept);
                remaining = remaining.subtract(kept);
            }
        }
        return desired;
    }

    private void applyEventChange(Long eventId, Long activeOrderId, Long actorUserId, String reason,
                                  Map<Long, BigDecimal> desiredByAllocationId) {
        if (actorUserId == null || StrUtil.isBlank(reason)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "orderChange.actor/reason");
        }
        MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(eventId);
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, eventId);
        }
        List<MesProcessPoolReportAllocationDO> allCurrent = allocationMapper.selectListByEventIdForUpdate(eventId);
        List<MesProcessPoolReportAllocationDO> changedOld = allCurrent.stream()
                .filter(row -> Objects.equals(row.getActiveOrderId(), activeOrderId))
                .filter(row -> row.getAllocatedQuantity().compareTo(
                        desiredByAllocationId.getOrDefault(row.getId(), BigDecimal.ZERO)) != 0)
                .toList();
        if (changedOld.isEmpty()) {
            return;
        }
        MesProcessPoolReportAllocationStateDO state = requireState(eventId, actorUserId);
        int version = (state.getCurrentVersion() == null ? 0 : state.getCurrentVersion()) + 1;
        List<Long> changedIds = changedOld.stream().map(MesProcessPoolReportAllocationDO::getId).toList();
        if (allocationMapper.supersedeCurrentRows(changedIds, version) != changedIds.size()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    eventId, version - 1, state.getCurrentVersion());
        }
        LocalDateTime now = LocalDateTime.now();
        List<MesProcessPoolReportAllocationDO> inserted = changedOld.stream()
                .filter(row -> desiredByAllocationId.getOrDefault(row.getId(), BigDecimal.ZERO)
                        .compareTo(BigDecimal.ZERO) > 0)
                .map(row -> copyForVersion(row, desiredByAllocationId.get(row.getId()), version, now)).toList();
        if (!inserted.isEmpty() && !Boolean.TRUE.equals(allocationMapper.insertBatch(inserted))) {
            throw new IllegalStateException("Failed to insert order-change report allocations");
        }
        List<MesProcessPoolReportAllocationDO> next = new ArrayList<>(allCurrent.stream()
                .filter(row -> !changedIds.contains(row.getId())).toList());
        next.addAll(inserted);
        fragmentService.rebuildForVersion(event, version, next);
        List<MesProcessPoolReportAllocationDO> affected = new ArrayList<>(changedOld);
        affected.addAll(inserted);
        completionService.reconcileAffectedAllocations(event, affected);
        insertAudit(eventId, version, activeOrderId, actorUserId, reason, changedOld, inserted, now);
        state.setCurrentVersion(version).setLastIdempotencyKey(null).setLastRequestHash(null)
                .setLastChangedBy(actorUserId).setLastChangedAt(now);
        if (stateMapper.updateById(state) != 1) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    eventId, version - 1, state.getCurrentVersion());
        }
        reportManagementSummaryService.refreshProductionEvent(event);
    }

    private void insertAudit(Long eventId, int version, Long activeOrderId, Long actorUserId, String reason,
                             List<MesProcessPoolReportAllocationDO> beforeRows,
                             List<MesProcessPoolReportAllocationDO> afterRows, LocalDateTime occurredAt) {
        MesProcessPoolReportAllocationDO source = beforeRows.get(0);
        BigDecimal before = sum(beforeRows);
        BigDecimal after = sum(afterRows);
        MesProcessPoolReportAllocationAdjustmentAuditDO audit =
                MesProcessPoolReportAllocationAdjustmentAuditDO.builder()
                        .eventId(eventId).allocationVersion(version).sourceAllocationId(source.getId())
                        .activeOrderId(activeOrderId).workOrderId(source.getWorkOrderId())
                        .routeProcessId(source.getRouteProcessId()).processId(source.getProcessId())
                        .beforeQuantity(before).afterQuantity(after).deltaQuantity(after.subtract(before))
                        .actorUserId(actorUserId).adjustmentReason(reason)
                        .allocationMode(MesProcessPoolReportAllocationDO.MODE_SYSTEM)
                        .changeSource(MesProcessPoolReportAllocationAdjustmentAuditDO.SOURCE_ORDER_CHANGE)
                        .occurredAt(occurredAt).build();
        if (!Boolean.TRUE.equals(auditMapper.insertBatch(List.of(audit)))) {
            throw new IllegalStateException("Failed to insert order-change allocation audit");
        }
    }

    private MesProcessPoolReportAllocationDO copyForVersion(MesProcessPoolReportAllocationDO source,
                                                            BigDecimal quantity, int version,
                                                            LocalDateTime confirmedAt) {
        return MesProcessPoolReportAllocationDO.builder().eventId(source.getEventId())
                .reviewId(source.getReviewId()).leaderUserId(source.getLeaderUserId())
                .activeOrderId(source.getActiveOrderId()).workOrderId(source.getWorkOrderId())
                .routeProcessId(source.getRouteProcessId()).processId(source.getProcessId())
                .allocatedQuantity(quantity).allocationMode(MesProcessPoolReportAllocationDO.MODE_SYSTEM)
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .createdVersion(version).confirmedAt(confirmedAt).build();
    }

    private MesProcessPoolReportAllocationStateDO requireState(Long eventId, Long actorUserId) {
        MesProcessPoolReportAllocationStateDO state = stateMapper.selectByEventIdForUpdate(eventId);
        if (state != null) {
            return state;
        }
        state = MesProcessPoolReportAllocationStateDO.builder().eventId(eventId).currentVersion(0)
                .lastChangedBy(actorUserId).lastChangedAt(LocalDateTime.now()).build();
        stateMapper.insert(state);
        return state;
    }

    private TargetKey targetKey(MesProcessPoolReportAllocationDO row) {
        return new TargetKey(row.getActiveOrderId(), row.getWorkOrderId(), row.getRouteProcessId(),
                row.getProcessId());
    }

    private BigDecimal sum(Collection<MesProcessPoolReportAllocationDO> rows) {
        return rows.stream().map(MesProcessPoolReportAllocationDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record TargetKey(Long activeOrderId, Long workOrderId, Long routeProcessId, Long processId) {
    }
}
