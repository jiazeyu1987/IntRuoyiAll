package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE;

@Service
public class MesTeamLeaderOrderProcessCompletionService {

    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    private final MesProScheduleOrderMapper scheduleOrderMapper;
    private final MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;

    public MesTeamLeaderOrderProcessCompletionService(MesProcessPoolReportAllocationMapper allocationMapper,
                                                      MesProWorkOrderMapper workOrderMapper,
                                                      MesProcessPoolOrderProcessCompletionMapper completionMapper,
                                                      MesTeamLeaderOrderProcessTargetService orderProcessTargetService,
                                                      MesProScheduleOrderMapper scheduleOrderMapper,
                                                      MesProScheduleOrderProcessMapper scheduleOrderProcessMapper) {
        this.allocationMapper = allocationMapper;
        this.workOrderMapper = workOrderMapper;
        this.completionMapper = completionMapper;
        this.orderProcessTargetService = orderProcessTargetService;
        this.scheduleOrderMapper = scheduleOrderMapper;
        this.scheduleOrderProcessMapper = scheduleOrderProcessMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyConfirmedAllocations(MesProProcessPoolEventDO event,
                                          Collection<MesProcessPoolReportAllocationDO> confirmedAllocations) {
        reconcileAffectedAllocations(event, confirmedAllocations, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reconcileAffectedAllocations(MesProProcessPoolEventDO event,
                                             Collection<MesProcessPoolReportAllocationDO> affectedAllocations) {
        reconcileAffectedAllocations(event, affectedAllocations, true);
    }

    private void reconcileAffectedAllocations(MesProProcessPoolEventDO event,
                                              Collection<MesProcessPoolReportAllocationDO> affectedAllocations,
                                              boolean allowAdjustableOverage) {
        if (event == null || event.getId() == null || event.getRouteProcessId() == null || event.getProcessId() == null
                || affectedAllocations == null || affectedAllocations.isEmpty()
                || affectedAllocations.stream().anyMatch(allocation -> allocation == null
                || allocation.getWorkOrderId() == null || allocation.getActiveOrderId() == null
                || allocation.getRouteProcessId() == null || allocation.getProcessId() == null)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "orderProcessCompletion");
        }
        List<Long> workOrderIds = affectedAllocations.stream()
                .map(MesProcessPoolReportAllocationDO::getWorkOrderId)
                .distinct()
                .toList();
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderMapper.selectListByIdsForUpdate(workOrderIds)
                .stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        Map<TargetKey, MesProcessPoolReportAllocationDO> representatives = affectedAllocations.stream()
                .collect(Collectors.toMap(this::targetKey, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        for (Map.Entry<TargetKey, MesProcessPoolReportAllocationDO> entry : representatives.entrySet()) {
            TargetKey key = entry.getKey();
            Long workOrderId = key.workOrderId();
            MesProWorkOrderDO workOrder = requireWorkOrder(workOrderId, workOrderMap);
            MesProcessPoolReportAllocationDO representativeAllocation = entry.getValue();
            List<MesProcessPoolReportAllocationDO> sourceAllocations = ordered(allocationMapper
                    .selectListByWorkOrderIdsAndProcessForUpdate(List.of(workOrderId), key.routeProcessId(),
                            key.processId()));
            MesProcessPoolReportAllocationDO currentRepresentative = sourceAllocations.isEmpty()
                    ? representativeAllocation : sourceAllocations.get(sourceAllocations.size() - 1);
            BigDecimal confirmedQuantity = sourceAllocations.stream().map(this::requireAllocatedQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            MesTeamLeaderOrderProcessTarget target = orderProcessTargetService.requireTarget(
                    currentRepresentative.getActiveOrderId(), workOrderId, key.routeProcessId(), key.processId());
            boolean quantityConflict = confirmedQuantity.compareTo(target.plannedQuantity()) > 0;
            if (quantityConflict && !allowAdjustableOverage) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH, workOrderId);
            }
            BigDecimal scheduleProgressQuantity = confirmedQuantity.min(target.plannedQuantity());
            syncFormalScheduleProgress(workOrderId, key.routeProcessId(), key.processId(),
                    scheduleProgressQuantity, target.plannedQuantity());

            MesProcessPoolOrderProcessCompletionDO completion =
                    completionMapper.selectByWorkOrderAndProcessForUpdate(workOrderId, key.routeProcessId(),
                            key.processId());
            if (completion == null) {
                completion = new MesProcessPoolOrderProcessCompletionDO();
            }
            completion.setWorkOrderId(workOrderId)
                    .setRouteProcessId(key.routeProcessId())
                    .setProcessId(key.processId())
                    .setTargetQuantity(target.plannedQuantity())
                    .setConfirmedQuantity(confirmedQuantity)
                    .setLastEventId(event.getId())
                    .setLastReviewId(currentRepresentative.getReviewId());
            if (confirmedQuantity.compareTo(target.plannedQuantity()) >= 0) {
                // Reaching a process target is only a progress projection. The active-order
                // completion command owned by flow 4 is the sole caller allowed to backfill.
                if (!isCompletedAndBackfilled(completion)) {
                    requireSourceAllocations(sourceAllocations);
                    applyPendingSourceTrace(key, completion, sourceAllocations);
                    completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                            .setCompletedAt(LocalDateTime.now())
                            .setBackfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED)
                            .setBackfillExecutionId(null)
                            .setBackfillError(null);
                } else {
                    completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED);
                }
            } else {
                applyPendingSourceTrace(key, completion, sourceAllocations);
                completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_IN_PROGRESS)
                        .setCompletedAt(null)
                        .setBackfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED)
                        .setBackfillExecutionId(null)
                        .setBackfillError(null);
            }
            if (completion.getId() == null) {
                completionMapper.insert(completion);
            } else {
                completionMapper.updateById(completion);
            }
        }
    }

    private TargetKey targetKey(MesProcessPoolReportAllocationDO allocation) {
        return new TargetKey(allocation.getWorkOrderId(), allocation.getRouteProcessId(), allocation.getProcessId());
    }

    private List<MesProcessPoolReportAllocationDO> ordered(List<MesProcessPoolReportAllocationDO> allocations) {
        return allocations.stream()
                .sorted(Comparator
                        .comparing(MesProcessPoolReportAllocationDO::getConfirmedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesProcessPoolReportAllocationDO::getEventId,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesProcessPoolReportAllocationDO::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private BigDecimal requireAllocatedQuantity(MesProcessPoolReportAllocationDO allocation) {
        if (allocation == null || allocation.getAllocatedQuantity() == null) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED,
                    allocation == null ? null : allocation.getWorkOrderId());
        }
        return allocation.getAllocatedQuantity();
    }

    private void requireSourceAllocations(List<MesProcessPoolReportAllocationDO> sourceAllocations) {
        if (sourceAllocations.isEmpty() || sourceAllocations.stream().anyMatch(allocation -> allocation.getId() == null
                || allocation.getEventId() == null || allocation.getAllocatedQuantity() == null)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "orderProcessCompletionSources");
        }
    }

    private boolean isCompletedAndBackfilled(MesProcessPoolOrderProcessCompletionDO completion) {
        return completion.getId() != null
                && MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())
                && MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS.equals(completion.getBackfillStatus())
                && completion.getBackfillExecutionId() != null;
    }

    private void applyPendingSourceTrace(TargetKey targetKey,
                                         MesProcessPoolOrderProcessCompletionDO completion,
                                         List<MesProcessPoolReportAllocationDO> sourceAllocations) {
        String aggregateHash = aggregateHashFromAllocations(targetKey, sourceAllocations);
        completion.setSourceEventIdsJson(toJsonIdArray(sourceAllocations.stream()
                        .map(MesProcessPoolReportAllocationDO::getEventId)
                        .distinct()
                        .toList()))
                .setSourceAllocationIdsJson(toJsonIdArray(sourceAllocations.stream()
                        .map(MesProcessPoolReportAllocationDO::getId)
                        .toList()))
                .setAggregateHash(aggregateHash)
                .setBackfillIdempotencyKey(idempotencyKey(targetKey, aggregateHash));
    }

    private void syncFormalScheduleProgress(Long workOrderId, Long routeProcessId, Long processId,
                                            BigDecimal confirmedQuantity, BigDecimal targetQuantity) {
        Optional<MesProScheduleOrderDO> scheduleOrderOptional = findSingleScheduleOrder(workOrderId);
        if (scheduleOrderOptional.isEmpty()) {
            // Active-order process snapshots remain authoritative after the source schedule order exits.
            return;
        }
        MesProScheduleOrderDO scheduleOrder = scheduleOrderOptional.get();
        List<MesProScheduleOrderProcessDO> processes = scheduleOrderProcessMapper
                .selectListByScheduleOrderId(scheduleOrder.getId());
        MesProScheduleOrderProcessDO currentProcess = processes.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .filter(process -> Objects.equals(process.getRouteProcessId(), routeProcessId))
                .filter(process -> Objects.equals(process.getProcessId(), processId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, workOrderId));

        BigDecimal plannedQuantity = requirePositive(currentProcess.getPlannedQuantity(), workOrderId);
        BigDecimal normalizedTargetQuantity = normalizeQuantity(targetQuantity);
        if (plannedQuantity.compareTo(normalizedTargetQuantity) != 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, workOrderId);
        }
        BigDecimal reportedQuantity = normalizeQuantity(confirmedQuantity).min(plannedQuantity);
        BigDecimal remainingQuantity = plannedQuantity.subtract(reportedQuantity).setScale(6, RoundingMode.HALF_UP);
        BigDecimal progressPercent = calculateProgressPercent(reportedQuantity, plannedQuantity);
        scheduleOrderProcessMapper.updateProgress(currentProcess.getId(), reportedQuantity, remainingQuantity,
                progressPercent);

        ProgressSummary summary = calculateFormalScheduleSummary(processes, currentProcess.getId(), reportedQuantity);
        scheduleOrderMapper.updateProgressSummary(scheduleOrder.getId(), summary.totalQuantity(),
                summary.completedQuantity(), summary.uncompletedQuantity(), summary.progressPercent(),
                resolveScheduleStatus(scheduleOrder, summary));
    }

    private Optional<MesProScheduleOrderDO> findSingleScheduleOrder(Long workOrderId) {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListByWorkOrderIds(List.of(workOrderId));
        if (scheduleOrders == null || scheduleOrders.isEmpty()) {
            return Optional.empty();
        }
        if (scheduleOrders.size() > 1) {
            throw exception(PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE);
        }
        return Optional.of(scheduleOrders.get(0));
    }

    private ProgressSummary calculateFormalScheduleSummary(List<MesProScheduleOrderProcessDO> processes,
                                                           Long currentProcessId,
                                                           BigDecimal currentReportedQuantity) {
        BigDecimal totalQuantity = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        BigDecimal completedQuantity = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        BigDecimal uncompletedQuantity = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        for (MesProScheduleOrderProcessDO process : processes) {
            if (!Boolean.TRUE.equals(process.getEnabled())) {
                continue;
            }
            BigDecimal plannedQuantity = requirePositive(process.getPlannedQuantity(), process.getId());
            BigDecimal reportedQuantity = Objects.equals(process.getId(), currentProcessId)
                    ? currentReportedQuantity : normalizeQuantity(process.getReportedQuantity()).min(plannedQuantity);
            reportedQuantity = reportedQuantity.min(plannedQuantity);
            totalQuantity = totalQuantity.add(plannedQuantity).setScale(6, RoundingMode.HALF_UP);
            completedQuantity = completedQuantity.add(reportedQuantity).setScale(6, RoundingMode.HALF_UP);
            uncompletedQuantity = uncompletedQuantity.add(plannedQuantity.subtract(reportedQuantity))
                    .setScale(6, RoundingMode.HALF_UP);
        }
        return new ProgressSummary(totalQuantity, completedQuantity, uncompletedQuantity,
                calculateProgressPercent(completedQuantity, totalQuantity));
    }

    private Integer resolveScheduleStatus(MesProScheduleOrderDO scheduleOrder, ProgressSummary summary) {
        if (summary.totalQuantity().compareTo(BigDecimal.ZERO) > 0
                && summary.completedQuantity().compareTo(summary.totalQuantity()) >= 0) {
            return MesProScheduleOrderStatusEnum.FINISHED.getStatus();
        }
        if (summary.completedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            return MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus();
        }
        return scheduleOrder.getStatus();
    }

    private BigDecimal calculateProgressPercent(BigDecimal completedQuantity, BigDecimal totalQuantity) {
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, "scheduleProgressTotal");
        }
        return completedQuantity.multiply(BigDecimal.valueOf(100))
                .divide(totalQuantity, 6, RoundingMode.HALF_UP)
                .min(new BigDecimal("100.000000"))
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : value.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal requirePositive(BigDecimal value, Object contextId) {
        BigDecimal normalized = normalizeQuantity(value);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, contextId);
        }
        return normalized;
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId, Map<Long, MesProWorkOrderDO> workOrderMap) {
        MesProWorkOrderDO workOrder = workOrderMap.get(workOrderId);
        if (workOrder == null || workOrder.getQuantity() == null
                || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, workOrderId);
        }
        return workOrder;
    }

    private String aggregateHashFromAllocations(TargetKey targetKey,
                                                List<MesProcessPoolReportAllocationDO> sourceAllocations) {
        StringBuilder canonical = new StringBuilder();
        canonical.append("routeProcess:").append(targetKey.routeProcessId()).append('\n')
                .append("process:").append(targetKey.processId()).append('\n');
        for (MesProcessPoolReportAllocationDO sourceAllocation : sourceAllocations) {
            canonical.append("allocation:")
                    .append(sourceAllocation.getId()).append('|')
                    .append(sourceAllocation.getEventId()).append('|')
                    .append(sourceAllocation.getAllocatedQuantity()).append('|')
                    .append(sourceAllocation.getConfirmedAt()).append('\n');
        }
        return sha256(canonical.toString());
    }

    private String idempotencyKey(TargetKey targetKey, String aggregateHash) {
        return "PROCESS_POOL_REPORT_BACKFILL_AGG:" + targetKey.workOrderId()
                + ":" + targetKey.routeProcessId()
                + ":" + targetKey.processId()
                + ":" + aggregateHash;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private String toJsonIdArray(List<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private record ProgressSummary(BigDecimal totalQuantity, BigDecimal completedQuantity,
                                   BigDecimal uncompletedQuantity, BigDecimal progressPercent) {
    }

    private record TargetKey(Long workOrderId, Long routeProcessId, Long processId) {
    }
}
