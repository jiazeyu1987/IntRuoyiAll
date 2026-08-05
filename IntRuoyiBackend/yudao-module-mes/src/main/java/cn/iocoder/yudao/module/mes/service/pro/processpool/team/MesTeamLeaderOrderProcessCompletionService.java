package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE;

@Service
public class MesTeamLeaderOrderProcessCompletionService {

    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    private final MesProScheduleOrderMapper scheduleOrderMapper;
    private final MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    private final MesTeamLeaderBatchRecordBackfillService backfillService;

    public MesTeamLeaderOrderProcessCompletionService(MesProcessPoolReportAllocationMapper allocationMapper,
                                                      MesProProcessPoolEventMapper eventMapper,
                                                      MesProWorkOrderMapper workOrderMapper,
                                                      MesProcessPoolOrderProcessCompletionMapper completionMapper,
                                                      MesTeamLeaderOrderProcessTargetService orderProcessTargetService,
                                                      MesProScheduleOrderMapper scheduleOrderMapper,
                                                      MesProScheduleOrderProcessMapper scheduleOrderProcessMapper,
                                                      MesTeamLeaderBatchRecordBackfillService backfillService) {
        this.allocationMapper = allocationMapper;
        this.eventMapper = eventMapper;
        this.workOrderMapper = workOrderMapper;
        this.completionMapper = completionMapper;
        this.orderProcessTargetService = orderProcessTargetService;
        this.scheduleOrderMapper = scheduleOrderMapper;
        this.scheduleOrderProcessMapper = scheduleOrderProcessMapper;
        this.backfillService = backfillService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyConfirmedAllocations(MesProProcessPoolEventDO event,
                                          Collection<MesProcessPoolReportAllocationDO> confirmedAllocations) {
        if (event == null || event.getId() == null || event.getRouteProcessId() == null || event.getProcessId() == null
                || confirmedAllocations == null || confirmedAllocations.isEmpty()
                || confirmedAllocations.stream().anyMatch(allocation -> allocation == null
                || allocation.getWorkOrderId() == null || allocation.getActiveOrderId() == null)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "orderProcessCompletion");
        }
        List<Long> workOrderIds = confirmedAllocations.stream()
                .map(MesProcessPoolReportAllocationDO::getWorkOrderId)
                .distinct()
                .toList();
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderMapper.selectListByIdsForUpdate(workOrderIds)
                .stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        Map<Long, MesProcessPoolReportAllocationDO> representativeAllocations = confirmedAllocations.stream()
                .collect(Collectors.toMap(MesProcessPoolReportAllocationDO::getWorkOrderId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        List<MesProcessPoolReportAllocationDO> lockedAllocations = allocationMapper
                .selectListByWorkOrderIdsAndProcessForUpdate(workOrderIds, event.getRouteProcessId(),
                        event.getProcessId());
        Map<Long, List<MesProcessPoolReportAllocationDO>> allocationsByWorkOrder = lockedAllocations.stream()
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getWorkOrderId,
                        LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), this::ordered)));
        Map<Long, BigDecimal> confirmedByWorkOrder = lockedAllocations.stream()
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getWorkOrderId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, this::requireAllocatedQuantity, BigDecimal::add)));

        for (Long workOrderId : workOrderIds) {
            MesProWorkOrderDO workOrder = requireWorkOrder(workOrderId, workOrderMap);
            MesProcessPoolReportAllocationDO representativeAllocation = representativeAllocations.get(workOrderId);
            MesTeamLeaderOrderProcessTarget target = orderProcessTargetService.requireTarget(
                    representativeAllocation.getActiveOrderId(), workOrderId, event.getRouteProcessId(),
                    event.getProcessId());
            BigDecimal confirmedQuantity = confirmedByWorkOrder.getOrDefault(workOrderId, BigDecimal.ZERO);
            if (confirmedQuantity.compareTo(target.plannedQuantity()) > 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH, workOrderId);
            }
            List<MesProcessPoolReportAllocationDO> sourceAllocations =
                    allocationsByWorkOrder.getOrDefault(workOrderId, List.of());
            requireSourceAllocations(sourceAllocations);
            syncFormalScheduleProgress(workOrderId, event.getRouteProcessId(), event.getProcessId(),
                    confirmedQuantity, target.plannedQuantity());

            MesProcessPoolOrderProcessCompletionDO completion =
                    completionMapper.selectByWorkOrderAndProcessForUpdate(workOrderId, event.getRouteProcessId(),
                            event.getProcessId());
            if (completion == null) {
                completion = new MesProcessPoolOrderProcessCompletionDO();
            }
            completion.setWorkOrderId(workOrderId)
                    .setRouteProcessId(event.getRouteProcessId())
                    .setProcessId(event.getProcessId())
                    .setTargetQuantity(target.plannedQuantity())
                    .setConfirmedQuantity(confirmedQuantity)
                    .setLastEventId(event.getId())
                    .setLastReviewId(representativeAllocation.getReviewId());
            if (confirmedQuantity.compareTo(target.plannedQuantity()) >= 0) {
                if (isCompletedAndBackfilled(completion)) {
                    completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED);
                } else {
                    completeAndBackfill(event, representativeAllocation, sourceAllocations, workOrder, completion);
                }
            } else {
                applyPendingSourceTrace(event, completion, sourceAllocations);
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

    private void completeAndBackfill(MesProProcessPoolEventDO event,
                                     MesProcessPoolReportAllocationDO allocation,
                                     List<MesProcessPoolReportAllocationDO> sourceAllocations,
                                     MesProWorkOrderDO workOrder,
                                     MesProcessPoolOrderProcessCompletionDO completion) {
        List<MesProProcessPoolEventDO> sourceEvents = loadSourceEvents(sourceAllocations);
        String aggregateHash = aggregateHash(event, sourceEvents, sourceAllocations);
        String idempotencyKey = idempotencyKey(allocation.getWorkOrderId(), event, aggregateHash);
        MesTeamLeaderBatchRecordBackfillResult backfill = backfillService.backfillCompletedProcess(
                new MesTeamLeaderBatchRecordBackfillCommand()
                        .setEvent(event)
                        .setAllocation(allocation)
                        .setSourceEvents(sourceEvents)
                        .setAllocations(sourceAllocations)
                        .setAggregateHash(aggregateHash)
                        .setIdempotencyKey(idempotencyKey)
                        .setWorkOrder(workOrder));
        completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .setCompletedAt(LocalDateTime.now())
                .setBackfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .setBackfillExecutionId(backfill.getExecutionId())
                .setBackfillError(null)
                .setSourceEventIdsJson(toJsonIdArray(sourceEvents.stream()
                        .map(MesProProcessPoolEventDO::getId).toList()))
                .setSourceAllocationIdsJson(toJsonIdArray(sourceAllocations.stream()
                        .map(MesProcessPoolReportAllocationDO::getId).toList()))
                .setAggregateHash(aggregateHash)
                .setBackfillIdempotencyKey(idempotencyKey);
    }

    private List<MesProProcessPoolEventDO> loadSourceEvents(
            List<MesProcessPoolReportAllocationDO> sourceAllocations) {
        List<Long> eventIds = sourceAllocations.stream()
                .map(MesProcessPoolReportAllocationDO::getEventId)
                .distinct()
                .toList();
        Map<Long, MesProProcessPoolEventDO> loaded = eventMapper.selectBatchIds(eventIds).stream()
                .collect(Collectors.toMap(MesProProcessPoolEventDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        for (Long eventId : eventIds) {
            if (!loaded.containsKey(eventId)) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "orderProcessCompletionSourceEvent");
            }
        }
        return eventIds.stream().map(loaded::get).toList();
    }

    private void applyPendingSourceTrace(MesProProcessPoolEventDO event,
                                         MesProcessPoolOrderProcessCompletionDO completion,
                                         List<MesProcessPoolReportAllocationDO> sourceAllocations) {
        String aggregateHash = aggregateHashFromAllocations(event, sourceAllocations);
        completion.setSourceEventIdsJson(toJsonIdArray(sourceAllocations.stream()
                        .map(MesProcessPoolReportAllocationDO::getEventId)
                        .distinct()
                        .toList()))
                .setSourceAllocationIdsJson(toJsonIdArray(sourceAllocations.stream()
                        .map(MesProcessPoolReportAllocationDO::getId)
                        .toList()))
                .setAggregateHash(aggregateHash)
                .setBackfillIdempotencyKey(idempotencyKey(sourceAllocations.get(0).getWorkOrderId(), event,
                        aggregateHash));
    }

    private void syncFormalScheduleProgress(Long workOrderId, Long routeProcessId, Long processId,
                                            BigDecimal confirmedQuantity, BigDecimal targetQuantity) {
        MesProScheduleOrderDO scheduleOrder = requireSingleScheduleOrder(workOrderId);
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
        BigDecimal reportedQuantity = normalizeQuantity(confirmedQuantity);
        if (reportedQuantity.compareTo(plannedQuantity) > 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH, workOrderId);
        }
        BigDecimal remainingQuantity = plannedQuantity.subtract(reportedQuantity).setScale(6, RoundingMode.HALF_UP);
        BigDecimal progressPercent = calculateProgressPercent(reportedQuantity, plannedQuantity);
        scheduleOrderProcessMapper.updateProgress(currentProcess.getId(), reportedQuantity, remainingQuantity,
                progressPercent);

        ProgressSummary summary = calculateFormalScheduleSummary(processes, currentProcess.getId(), reportedQuantity);
        scheduleOrderMapper.updateProgressSummary(scheduleOrder.getId(), summary.totalQuantity(),
                summary.completedQuantity(), summary.uncompletedQuantity(), summary.progressPercent(),
                resolveScheduleStatus(scheduleOrder, summary));
    }

    private MesProScheduleOrderDO requireSingleScheduleOrder(Long workOrderId) {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListByWorkOrderIds(List.of(workOrderId));
        if (scheduleOrders == null || scheduleOrders.isEmpty()) {
            throw exception(PRO_SCHEDULE_ORDER_NOT_EXISTS);
        }
        if (scheduleOrders.size() > 1) {
            throw exception(PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE);
        }
        return scheduleOrders.get(0);
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
                    ? currentReportedQuantity : normalizeQuantity(process.getReportedQuantity());
            if (reportedQuantity.compareTo(plannedQuantity) > 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH, process.getId());
            }
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

    private String aggregateHash(MesProProcessPoolEventDO event,
                                 List<MesProProcessPoolEventDO> sourceEvents,
                                 List<MesProcessPoolReportAllocationDO> sourceAllocations) {
        StringBuilder canonical = new StringBuilder();
        canonical.append("workOrder:").append(event.getWorkOrderId()).append('\n')
                .append("routeProcess:").append(event.getRouteProcessId()).append('\n')
                .append("process:").append(event.getProcessId()).append('\n');
        for (MesProProcessPoolEventDO sourceEvent : sourceEvents) {
            canonical.append("event:")
                    .append(sourceEvent.getId()).append('|')
                    .append(sourceEvent.getRawPayload()).append('|')
                    .append(sourceEvent.getServerSubmitTime()).append('\n');
        }
        for (MesProcessPoolReportAllocationDO sourceAllocation : sourceAllocations) {
            canonical.append("allocation:")
                    .append(sourceAllocation.getId()).append('|')
                    .append(sourceAllocation.getEventId()).append('|')
                    .append(sourceAllocation.getAllocatedQuantity()).append('|')
                    .append(sourceAllocation.getConfirmedAt()).append('\n');
        }
        return sha256(canonical.toString());
    }

    private String aggregateHashFromAllocations(MesProProcessPoolEventDO event,
                                                List<MesProcessPoolReportAllocationDO> sourceAllocations) {
        StringBuilder canonical = new StringBuilder();
        canonical.append("routeProcess:").append(event.getRouteProcessId()).append('\n')
                .append("process:").append(event.getProcessId()).append('\n');
        for (MesProcessPoolReportAllocationDO sourceAllocation : sourceAllocations) {
            canonical.append("allocation:")
                    .append(sourceAllocation.getId()).append('|')
                    .append(sourceAllocation.getEventId()).append('|')
                    .append(sourceAllocation.getAllocatedQuantity()).append('|')
                    .append(sourceAllocation.getConfirmedAt()).append('\n');
        }
        return sha256(canonical.toString());
    }

    private String idempotencyKey(Long workOrderId, MesProProcessPoolEventDO event, String aggregateHash) {
        return "PROCESS_POOL_REPORT_BACKFILL_AGG:" + workOrderId
                + ":" + event.getRouteProcessId()
                + ":" + event.getProcessId()
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
}
