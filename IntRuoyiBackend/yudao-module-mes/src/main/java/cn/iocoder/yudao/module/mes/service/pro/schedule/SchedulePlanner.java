package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProReplanExplanationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarWorkOrderAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.MesBizTypeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator.ScheduleWindowResult;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator.ShiftWindow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator.ScheduleWindowResult;

@Component
public class SchedulePlanner {

    private static final String ISSUE_SEVERITY_BLOCKING = "BLOCKING";
    private static final String ISSUE_SEVERITY_WARNING = "WARNING";
    private static final String SCHEDULE_SOURCE_AUTO = "AUTO";
    private static final String SCHEDULE_SOURCE_MANUAL = "MANUAL";
    private static final String RISK_STATUS_NONE = "NONE";
    private static final String CAPACITY_SOURCE_MACHINE = "MACHINE";
    private static final String CAPACITY_SOURCE_WORKER = "WORKER";
    private static final String DAILY_REASON_CAPACITY_WINDOW = "CAPACITY_WINDOW";
    private static final String DAILY_REASON_PROTECTED_TASK = "PROTECTED_TASK";
    private static final String DAILY_REASON_CAPACITY_WINDOW_WITH_PROTECTED = "CAPACITY_WINDOW_WITH_PROTECTED";

    public List<MesProScheduleOrderDO> sortScheduleOrders(Collection<MesProScheduleOrderDO> scheduleOrders,
                                                          Map<Long, List<MesProScheduleOrderProcessDO>> processMap) {
        if (scheduleOrders == null || scheduleOrders.isEmpty()) {
            return Collections.emptyList();
        }
        return scheduleOrders.stream()
                .sorted(scheduleOrderComparator(processMap))
                .toList();
    }

    public List<MesProWorkOrderDO> orderWorkOrdersByScheduleOrder(List<MesProWorkOrderDO> workOrders,
                                                                  List<MesProScheduleOrderDO> scheduleOrders) {
        if (workOrders == null || workOrders.isEmpty()
                || scheduleOrders == null || scheduleOrders.isEmpty()) {
            return workOrders;
        }
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrders.stream()
                .filter(workOrder -> workOrder.getId() != null)
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, workOrder -> workOrder,
                        (left, right) -> left, LinkedHashMap::new));
        List<MesProWorkOrderDO> ordered = new ArrayList<>();
        for (MesProScheduleOrderDO scheduleOrder : scheduleOrders) {
            MesProWorkOrderDO workOrder = workOrderMap.remove(scheduleOrder.getWorkOrderId());
            if (workOrder != null) {
                ordered.add(workOrder);
            }
        }
        ordered.addAll(workOrderMap.values());
        return ordered;
    }

    public ProcessLineCandidate selectBestProcessLineCandidate(ScheduleComputation computation,
                                                                MesProRouteProcessDO routeProcess,
                                                                Long requiredLineId,
                                                                Function<Long, ProcessLineCandidate> lineCandidateSimulator,
                                                                Supplier<ProcessLineCandidate> routeProcessCandidateSupplier) {
        Objects.requireNonNull(computation, "schedule computation must not be null");
        Objects.requireNonNull(routeProcess, "route process must not be null");
        Objects.requireNonNull(lineCandidateSimulator, "line candidate simulator must not be null");
        Objects.requireNonNull(routeProcessCandidateSupplier, "route process candidate supplier must not be null");
        List<Long> candidateLineIds = requiredLineId != null
                ? List.of(requiredLineId)
                : computation.workstationsByRouteProcessId.getOrDefault(routeProcess.getId(), Collections.emptyList()).stream()
                .map(MesMdWorkstationDO::getProductionLineId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (candidateLineIds.isEmpty()) {
            ProcessLineCandidate routeProcessCandidate = routeProcessCandidateSupplier.get();
            if (hasFailureIssues(routeProcessCandidate)) {
                computation.issues.addAll(routeProcessCandidate.failureIssues);
                return null;
            }
            return routeProcessCandidate;
        }

        ProcessLineCandidate selected = null;
        List<ScheduleIssueDraft> firstFailure = null;
        for (Long lineId : candidateLineIds) {
            ProcessLineCandidate candidate = lineCandidateSimulator.apply(lineId);
            if (hasFailureIssues(candidate)) {
                if (firstFailure == null) {
                    firstFailure = candidate.failureIssues;
                }
                continue;
            }
            if (selected == null
                    || selected.endTime.isAfter(candidate.endTime)
                    || (selected.endTime.isEqual(candidate.endTime) && selected.lineId > candidate.lineId)) {
                selected = candidate;
            }
        }
        if (selected == null && firstFailure != null) {
            ProcessLineCandidate routeProcessCandidate = routeProcessCandidateSupplier.get();
            if (!hasFailureIssues(routeProcessCandidate)) {
                return routeProcessCandidate;
            }
            computation.issues.addAll(firstFailure);
        }
        return selected;
    }

    public List<PlannedTask> allocateFiniteProcessPlans(LocalDateTime availableFrom, int requiredMinutes,
                                                        List<ShiftWindow> windows,
                                                        MesProWorkOrderDO workOrder,
                                                        MesProRouteProductDO routeProduct,
                                                        MesProRouteProcessDO routeProcess,
                                                        ProcessResourcePool pool,
                                                        Long lineId,
                                                        String lineName,
                                                        MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                        BigDecimal targetQuantity) {
        return allocateFiniteProcessPlans(availableFrom, requiredMinutes, windows, workOrder, routeProduct,
                routeProcess, pool, lineId, lineName, scheduleOrderProcess, targetQuantity,
                new DailyProcessCapacityLedger());
    }

    public List<PlannedTask> allocateFiniteProcessPlans(LocalDateTime availableFrom, int requiredMinutes,
                                                        List<ShiftWindow> windows,
                                                        MesProWorkOrderDO workOrder,
                                                        MesProRouteProductDO routeProduct,
                                                        MesProRouteProcessDO routeProcess,
                                                        ProcessResourcePool pool,
                                                        Long lineId,
                                                        String lineName,
                                                        MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                        BigDecimal targetQuantity,
                                                        DailyProcessCapacityLedger capacityLedger) {
        Objects.requireNonNull(availableFrom, "available from must not be null");
        Objects.requireNonNull(workOrder, "work order must not be null");
        Objects.requireNonNull(routeProduct, "route product must not be null");
        Objects.requireNonNull(routeProcess, "route process must not be null");
        Objects.requireNonNull(pool, "process resource pool must not be null");
        List<ShiftWindow> filteredWindows = safeList(windows).stream()
                .filter(window -> window.usableEnd.isAfter(availableFrom))
                .toList();
        if (filteredWindows.isEmpty()) {
            return Collections.emptyList();
        }
        return allocateSegmentedProcessPlans(availableFrom, requiredMinutes, filteredWindows, workOrder,
                routeProduct, routeProcess, scheduleOrderProcess, pool, lineId, lineName, targetQuantity,
                capacityLedger);
    }

    public List<PlannedTask> allocateInfiniteProcessPlans(LocalDateTime availableFrom, int requiredMinutes,
                                                          List<ShiftWindow> windows,
                                                          MesProWorkOrderDO workOrder,
                                                          MesProRouteProductDO routeProduct,
                                                          MesProRouteProcessDO routeProcess,
                                                          ProcessResourcePool pool,
                                                          Long lineId,
                                                           String lineName,
                                                           MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                           BigDecimal targetQuantity,
                                                           CapacityWindowAllocator capacityWindowAllocator) {
        return allocateInfiniteProcessPlans(availableFrom, requiredMinutes, windows, workOrder, routeProduct,
                routeProcess, pool, lineId, lineName, scheduleOrderProcess, targetQuantity,
                capacityWindowAllocator, new DailyProcessCapacityLedger());
    }

    public List<PlannedTask> allocateInfiniteProcessPlans(LocalDateTime availableFrom, int requiredMinutes,
                                                          List<ShiftWindow> windows,
                                                          MesProWorkOrderDO workOrder,
                                                          MesProRouteProductDO routeProduct,
                                                          MesProRouteProcessDO routeProcess,
                                                          ProcessResourcePool pool,
                                                          Long lineId,
                                                          String lineName,
                                                          MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                          BigDecimal targetQuantity,
                                                          CapacityWindowAllocator capacityWindowAllocator,
                                                          DailyProcessCapacityLedger capacityLedger) {
        Objects.requireNonNull(capacityWindowAllocator, "capacity window allocator must not be null");
        if (scheduleOrderProcess != null && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled())) {
            return allocateFiniteProcessPlans(availableFrom, requiredMinutes, windows, workOrder, routeProduct,
                    routeProcess, pool, lineId, lineName, scheduleOrderProcess, targetQuantity, capacityLedger);
        }
        if (capacityLedger != null && capacityLedger.isCapacityLimited(scheduleOrderProcess)) {
            return allocateFiniteProcessPlans(availableFrom, requiredMinutes, windows, workOrder, routeProduct,
                    routeProcess, pool, lineId, lineName, scheduleOrderProcess, targetQuantity, capacityLedger);
        }
        ScheduleWindowResult windowResult = capacityWindowAllocator.allocateInfiniteWindow(availableFrom, requiredMinutes, windows);
        if (windowResult == null) {
            return Collections.emptyList();
        }
        PlannedTask plan = buildPlannedTask(workOrder, routeProduct, routeProcess, scheduleOrderProcess, pool,
                lineId, lineName, windowResult.startTime, windowResult.endTime, windowResult.minutes,
                targetQuantity);
        plan.segmentIndex = 1;
        return List.of(plan);
    }

    public List<LinkPlan> buildLinkPlans(ScheduleComputation computation) {
        Objects.requireNonNull(computation, "schedule computation must not be null");
        computation.linkPlans.clear();
        for (MesProScheduleOrderDO scheduleOrder : computation.scheduleOrders) {
            List<MesProScheduleOrderProcessDO> processes = computation.scheduleOrderProcessesByOrderId
                    .getOrDefault(scheduleOrder.getId(), Collections.emptyList());
            processes = activeTopologyScheduleOrderProcesses(processes);
            Map<Long, MesProRouteProcessDO> routeProcessById = computation.routeProcessesByWorkOrderId
                    .getOrDefault(scheduleOrder.getWorkOrderId(), Collections.emptyList())
                    .stream()
                    .filter(routeProcess -> routeProcess != null && routeProcess.getId() != null)
                    .collect(Collectors.toMap(MesProRouteProcessDO::getId, routeProcess -> routeProcess,
                            (left, right) -> left, LinkedHashMap::new));
            Map<Integer, MesProRouteProcessDO> routeProcessBySort = computation.routeProcessesByWorkOrderId
                    .getOrDefault(scheduleOrder.getWorkOrderId(), Collections.emptyList())
                    .stream()
                    .filter(routeProcess -> routeProcess != null && routeProcess.getSort() != null)
                    .collect(Collectors.toMap(MesProRouteProcessDO::getSort, routeProcess -> routeProcess,
                            (left, right) -> left, LinkedHashMap::new));
            if (!hasRouteProcessTopologySnapshot(processes) || hasInactiveTopologyPredecessor(processes)) {
                continue;
            }
            Map<Long, MesProScheduleOrderProcessDO> processByRouteProcessId = processes.stream()
                    .collect(Collectors.toMap(MesProScheduleOrderProcessDO::getRouteProcessId,
                            item -> item, (left, right) -> left, LinkedHashMap::new));
            for (MesProScheduleOrderProcessDO process : processByRouteProcessId.values()) {
                if (process.getPredecessorRouteProcessId() == null) {
                    continue;
                }
                MesProScheduleOrderProcessDO predecessor =
                        processByRouteProcessId.get(process.getPredecessorRouteProcessId());
                if (predecessor == null) {
                    throw new IllegalStateException("排产工序直接前置快照不存在，scheduleOrderId="
                            + scheduleOrder.getId() + ", routeProcessId=" + process.getRouteProcessId());
                }
                Long predecessorProcessId = resolveRuntimeProcessId(routeProcessById, routeProcessBySort, predecessor);
                Long processId = resolveRuntimeProcessId(routeProcessById, routeProcessBySort, process);
                if (predecessorProcessId == null || processId == null) {
                    continue;
                }
                computation.linkPlans.add(new LinkPlan(
                        scheduleOrder.getWorkOrderId(), predecessorProcessId, processId));
            }
        }
        return computation.linkPlans;
    }

    private Comparator<MesProScheduleOrderDO> scheduleOrderComparator(
            Map<Long, List<MesProScheduleOrderProcessDO>> processMap) {
        return Comparator
                .comparing((MesProScheduleOrderDO order) -> !hasNightShiftScheduleProcess(order, processMap))
                .thenComparing(MesProScheduleOrderDO::getPromiseDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(MesProScheduleOrderDO::getPriorityNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MesProScheduleOrderDO::getId, Comparator.nullsLast(Long::compareTo));
    }

    private boolean hasNightShiftScheduleProcess(MesProScheduleOrderDO scheduleOrder,
                                                 Map<Long, List<MesProScheduleOrderProcessDO>> processMap) {
        if (scheduleOrder == null || processMap == null) {
            return false;
        }
        return processMap.getOrDefault(scheduleOrder.getId(), Collections.emptyList()).stream()
                .anyMatch(process -> process != null && Boolean.TRUE.equals(process.getNightShiftEnabled()));
    }

    public boolean violatesLatestStartConstraint(ScheduleComputation computation,
                                                 MesProScheduleOrderDO scheduleOrder,
                                                 List<PreviewStep> steps,
                                                 CapacityWindowAllocator capacityWindowAllocator,
                                                 LatestStartCalendarShiftModeResolver shiftModeResolver) {
        if (scheduleOrder == null || steps == null || steps.isEmpty()) {
            return false;
        }
        LocalDateTime latestStartTime = calculateLatestStartTime(
                computation, scheduleOrder, steps, capacityWindowAllocator, shiftModeResolver);
        if (latestStartTime == null) {
            return false;
        }
        LocalDateTime plannedStartTime = steps.stream()
                .map(step -> step.startTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return plannedStartTime != null && plannedStartTime.isAfter(latestStartTime);
    }

    public LocalDateTime calculateLatestStartTime(ScheduleComputation computation,
                                                  MesProScheduleOrderDO scheduleOrder,
                                                  List<PreviewStep> steps,
                                                  CapacityWindowAllocator capacityWindowAllocator,
                                                  LatestStartCalendarShiftModeResolver shiftModeResolver) {
        Objects.requireNonNull(computation, "schedule computation must not be null");
        Objects.requireNonNull(scheduleOrder, "schedule order must not be null");
        Objects.requireNonNull(capacityWindowAllocator, "capacity window allocator must not be null");
        Objects.requireNonNull(shiftModeResolver, "latest start shift mode resolver must not be null");
        if (scheduleOrder.getPromiseDate() == null || steps == null || steps.isEmpty()) {
            return null;
        }
        LocalDateTime cursor = scheduleOrder.getPromiseDate().plusDays(1).atStartOfDay();
        List<PreviewStep> orderedSteps = steps.stream()
                .filter(step -> step.startTime != null && step.endTime != null)
                .sorted(Comparator.comparing((PreviewStep step) -> step.startTime)
                        .thenComparing(step -> step.endTime))
                .toList();
        for (int i = orderedSteps.size() - 1; i >= 0; i--) {
            PreviewStep step = orderedSteps.get(i);
            int plannedMinutes = (int) Duration.between(step.startTime, step.endTime).toMinutes();
            if (plannedMinutes <= 0) {
                continue;
            }
            MesMdWorkstationDO workstation = computation.workstationMap.get(step.workstationId);
            MesProScheduleOrderProcessDO scheduleOrderProcess = findScheduleOrderProcessByProcessId(
                    computation, scheduleOrder.getWorkOrderId(), step.processId);
            if (workstation == null || workstation.getProductionLineId() == null) {
                int routeWindowMinutes = capacityWindowAllocator.resolveRouteProcessDailyWindowMinutes(scheduleOrderProcess);
                if (routeWindowMinutes <= 0) {
                    return cursor.minusMinutes(plannedMinutes);
                }
                LocalDateTime routeWindowStart = computation.requestStartTime == null
                        ? step.startTime : computation.requestStartTime;
                List<ShiftWindow> routeWindows = capacityWindowAllocator.buildRouteProcessShiftWindows(
                        scheduleOrderProcess, routeWindowStart, plannedMinutes,
                        date -> shiftModeResolver.resolve(date, scheduleOrderProcess));
                cursor = capacityWindowAllocator.consumeBackwardWindows(cursor, plannedMinutes, routeWindows);
                continue;
            }
            List<ShiftWindow> windows = capacityWindowAllocator.filterWindowsForScheduleProcess(
                    computation.shiftWindowsByLineId.getOrDefault(workstation.getProductionLineId(), Collections.emptyList()),
                    scheduleOrderProcess, date -> shiftModeResolver.resolve(date, scheduleOrderProcess));
            cursor = capacityWindowAllocator.consumeBackwardWindows(cursor, plannedMinutes, windows);
        }
        return cursor;
    }

    public List<MesProReplanExplanationRespVO.DailyExplanationItem> buildDailyExplanations(
            ScheduleComputation computation, DailyAvailableWindowMinutesResolver windowMinutesResolver) {
        Map<DailyExplanationKey, DailyExplanationAccumulator> accumulators = new LinkedHashMap<>();
        for (PlannedTask plan : computation.generatedTasks) {
            if (plan.startTime == null) {
                continue;
            }
            DailyExplanationKey key = new DailyExplanationKey(plan.startTime.toLocalDate(),
                    plan.workOrderId, plan.scheduleOrderProcessId, plan.processId);
            DailyExplanationAccumulator accumulator = accumulators.computeIfAbsent(key, DailyExplanationAccumulator::new);
            accumulator.lineId = plan.lineId;
            accumulator.plannedQuantity = accumulator.plannedQuantity.add(ObjUtil.defaultIfNull(plan.quantity, BigDecimal.ZERO));
            accumulator.generatedTaskCount++;
            accumulator.usedWindowMinutes += positiveMinutes(plan.startTime, plan.endTime);
            accumulator.dependencyReleasedAt = minTime(accumulator.dependencyReleasedAt, plan.dependencyReleasedAt);
        }
        for (List<PreviewStep> steps : computation.finalSteps.values()) {
            for (PreviewStep step : steps) {
                if (step == null || step.generated || step.startTime == null) {
                    continue;
                }
                MesProScheduleOrderProcessDO scheduleOrderProcess = findScheduleOrderProcessForDailyExplanation(
                        computation, step.workOrderId, null, step.processId);
                DailyExplanationKey key = new DailyExplanationKey(step.startTime.toLocalDate(),
                        step.workOrderId, scheduleOrderProcess == null ? null : scheduleOrderProcess.getId(),
                        step.processId);
                DailyExplanationAccumulator accumulator = accumulators.computeIfAbsent(key, DailyExplanationAccumulator::new);
                MesMdWorkstationDO workstation = computation.workstationMap.get(step.workstationId);
                accumulator.lineId = workstation == null ? accumulator.lineId : workstation.getProductionLineId();
                accumulator.plannedQuantity = accumulator.plannedQuantity.add(ObjUtil.defaultIfNull(step.quantity, BigDecimal.ZERO));
                accumulator.protectedOccupiedMinutes += positiveMinutes(step.startTime, step.endTime);
            }
        }
        return accumulators.values().stream()
                .map(accumulator -> buildDailyExplanationItem(computation, accumulator, windowMinutesResolver))
                .sorted(Comparator
                        .comparing(MesProReplanExplanationRespVO.DailyExplanationItem::getPlanDate,
                                Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(MesProReplanExplanationRespVO.DailyExplanationItem::getWorkOrderId,
                                Comparator.nullsLast(Long::compareTo))
                        .thenComparing(MesProReplanExplanationRespVO.DailyExplanationItem::getProcessId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private MesProReplanExplanationRespVO.DailyExplanationItem buildDailyExplanationItem(
            ScheduleComputation computation, DailyExplanationAccumulator accumulator,
            DailyAvailableWindowMinutesResolver windowMinutesResolver) {
        MesProScheduleOrderProcessDO scheduleOrderProcess = findScheduleOrderProcessForDailyExplanation(
                computation, accumulator.key.workOrderId(), accumulator.key.scheduleOrderProcessId(),
                accumulator.key.processId());
        MesProWorkOrderDO workOrder = computation.workOrderMap.get(accumulator.key.workOrderId());
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO process =
                computation.processMap.get(accumulator.key.processId());
        MesProReplanExplanationRespVO.DailyExplanationItem item =
                new MesProReplanExplanationRespVO.DailyExplanationItem();
        item.setPlanDate(accumulator.key.planDate());
        item.setWorkOrderId(accumulator.key.workOrderId());
        item.setWorkOrderCode(workOrder == null ? null : workOrder.getCode());
        item.setScheduleOrderProcessId(scheduleOrderProcess == null
                ? accumulator.key.scheduleOrderProcessId() : scheduleOrderProcess.getId());
        item.setProcessId(accumulator.key.processId());
        item.setProcessName(resolveProcessName(process == null ? null : process.getName(), accumulator.key.processId()));
        item.setPlannedQuantity(accumulator.plannedQuantity);
        item.setGeneratedTaskCount(accumulator.generatedTaskCount);
        item.setAvailableWindowMinutes(windowMinutesResolver.resolve(
                accumulator.lineId, accumulator.key.planDate(), scheduleOrderProcess));
        item.setUsedWindowMinutes(accumulator.usedWindowMinutes);
        item.setProtectedOccupiedMinutes(accumulator.protectedOccupiedMinutes);
        item.setDependencyReleasedAt(accumulator.dependencyReleasedAt);
        item.setRemainingQuantityBefore(scheduleOrderProcess == null ? null : scheduleOrderProcess.getRemainingQuantity());
        item.setRemainingQuantityAfter(calculateRemainingQuantityAfter(scheduleOrderProcess, accumulator.plannedQuantity));
        item.setReasonCode(resolveDailyReasonCode(accumulator));
        return item;
    }

    private MesProScheduleOrderProcessDO findScheduleOrderProcessForDailyExplanation(
            ScheduleComputation computation, Long workOrderId, Long scheduleOrderProcessId, Long processId) {
        MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrderId);
        if (scheduleOrder == null) {
            return null;
        }
        return computation.scheduleOrderProcessesByOrderId
                .getOrDefault(scheduleOrder.getId(), Collections.emptyList())
                .stream()
                .filter(process -> scheduleOrderProcessId == null || ObjUtil.equal(process.getId(), scheduleOrderProcessId))
                .filter(process -> processId == null || ObjUtil.equal(process.getProcessId(), processId))
                .findFirst()
                .orElse(null);
    }

    private MesProScheduleOrderDO findScheduleOrderByWorkOrderId(ScheduleComputation computation, Long workOrderId) {
        return computation.scheduleOrders.stream()
                .filter(order -> ObjUtil.equal(order.getWorkOrderId(), workOrderId))
                .findFirst()
                .orElse(null);
    }

    private MesProScheduleOrderProcessDO findScheduleOrderProcessByProcessId(ScheduleComputation computation,
                                                                             Long workOrderId, Long processId) {
        MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrderId);
        if (scheduleOrder == null) {
            return null;
        }
        return computation.scheduleOrderProcessesByOrderId.getOrDefault(scheduleOrder.getId(), Collections.emptyList()).stream()
                .filter(process -> ObjUtil.equal(process.getProcessId(), processId))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal calculateRemainingQuantityAfter(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                       BigDecimal plannedQuantity) {
        if (scheduleOrderProcess == null || scheduleOrderProcess.getRemainingQuantity() == null) {
            return null;
        }
        return scheduleOrderProcess.getRemainingQuantity()
                .subtract(ObjUtil.defaultIfNull(plannedQuantity, BigDecimal.ZERO))
                .max(BigDecimal.ZERO);
    }

    private String resolveDailyReasonCode(DailyExplanationAccumulator accumulator) {
        if (accumulator.generatedTaskCount > 0 && accumulator.protectedOccupiedMinutes > 0) {
            return DAILY_REASON_CAPACITY_WINDOW_WITH_PROTECTED;
        }
        if (accumulator.generatedTaskCount > 0) {
            return DAILY_REASON_CAPACITY_WINDOW;
        }
        return DAILY_REASON_PROTECTED_TASK;
    }

    private String resolveProcessName(String processName, Long processId) {
        if (StrUtil.isNotBlank(processName)) {
            return processName;
        }
        return processId == null ? null : "工序" + processId;
    }

    private int positiveMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return 0;
        }
        return Math.toIntExact(Duration.between(startTime, endTime).toMinutes());
    }

    private LocalDateTime minTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private Long resolveRuntimeProcessId(Map<Long, MesProRouteProcessDO> routeProcessById,
                                         Map<Integer, MesProRouteProcessDO> routeProcessBySort,
                                          MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess == null) {
            return null;
        }
        MesProRouteProcessDO runtimeRouteProcess = routeProcessById.get(scheduleOrderProcess.getRouteProcessId());
        if (runtimeRouteProcess == null && scheduleOrderProcess.getSort() != null) {
            runtimeRouteProcess = routeProcessBySort.get(scheduleOrderProcess.getSort());
        }
        Long runtimeProcessId = runtimeRouteProcess == null ? null : positiveIdOrNull(runtimeRouteProcess.getProcessId());
        return runtimeProcessId != null ? runtimeProcessId : positiveIdOrNull(scheduleOrderProcess.getProcessId());
    }

    private Long positiveIdOrNull(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    private boolean hasFailureIssues(ProcessLineCandidate candidate) {
        return candidate != null && candidate.failureIssues != null && !candidate.failureIssues.isEmpty();
    }

    private List<PlannedTask> allocateSegmentedProcessPlans(LocalDateTime availableFrom, int requiredMinutes,
                                                            List<ShiftWindow> filteredWindows,
                                                            MesProWorkOrderDO workOrder,
                                                            MesProRouteProductDO routeProduct,
                                                            MesProRouteProcessDO routeProcess,
                                                            MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                            ProcessResourcePool pool,
                                                            Long lineId,
                                                            String lineName,
                                                            BigDecimal targetQuantity,
                                                            DailyProcessCapacityLedger capacityLedger) {
        if (capacityLedger != null && capacityLedger.isCapacityLimited(scheduleOrderProcess)
                && targetQuantity != null && targetQuantity.compareTo(BigDecimal.ZERO) > 0
                && requiredMinutes > 0) {
            return allocateCapacityLimitedSegmentedProcessPlans(availableFrom, requiredMinutes, filteredWindows,
                    workOrder, routeProduct, routeProcess, scheduleOrderProcess, pool, lineId, lineName,
                    targetQuantity, capacityLedger);
        }
        List<PlannedTask> plans = new ArrayList<>();
        LocalDateTime cursor = availableFrom;
        int remainingMinutes = requiredMinutes;
        for (ShiftWindow window : filteredWindows) {
            if (!window.usableEnd.isAfter(cursor)) {
                continue;
            }
            LocalDateTime segmentStart = window.startTime.isAfter(cursor) ? window.startTime : cursor;
            if (!window.usableEnd.isAfter(segmentStart)) {
                continue;
            }
            long usableMinutes = Duration.between(segmentStart, window.usableEnd).toMinutes();
            int segmentMinutes = (int) Math.min(usableMinutes, remainingMinutes);
            if (segmentMinutes <= 0) {
                continue;
            }
            LocalDateTime segmentEnd = segmentStart.plusMinutes(segmentMinutes);
            BigDecimal segmentQuantity = allocateSegmentQuantity(targetQuantity, segmentMinutes, requiredMinutes);
            plans.add(buildPlannedTask(workOrder, routeProduct, routeProcess, scheduleOrderProcess, pool, lineId, lineName,
                    segmentStart, segmentEnd, segmentMinutes, segmentQuantity));
            remainingMinutes -= segmentMinutes;
            cursor = segmentEnd;
            if (remainingMinutes <= 0) {
                break;
            }
        }
        if (remainingMinutes > 0) {
            return Collections.emptyList();
        }
        BigDecimal quantityDiff = targetQuantity == null ? BigDecimal.ZERO : targetQuantity.subtract(plans.stream()
                .map(plan -> plan.quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (quantityDiff.compareTo(BigDecimal.ZERO) != 0 && !plans.isEmpty()) {
            PlannedTask lastPlan = plans.get(plans.size() - 1);
            lastPlan.quantity = lastPlan.quantity.add(quantityDiff);
        }
        for (int i = 0; i < plans.size(); i++) {
            plans.get(i).segmentIndex = i + 1;
        }
        return plans;
    }

    private List<PlannedTask> allocateCapacityLimitedSegmentedProcessPlans(LocalDateTime availableFrom,
                                                                           int requiredMinutes,
                                                                           List<ShiftWindow> filteredWindows,
                                                                           MesProWorkOrderDO workOrder,
                                                                           MesProRouteProductDO routeProduct,
                                                                           MesProRouteProcessDO routeProcess,
                                                                           MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                                           ProcessResourcePool pool,
                                                                           Long lineId,
                                                                           String lineName,
                                                                           BigDecimal targetQuantity,
                                                                           DailyProcessCapacityLedger capacityLedger) {
        List<PlannedTask> plans = new ArrayList<>();
        LocalDateTime cursor = availableFrom;
        int remainingMinutes = requiredMinutes;
        BigDecimal totalQuantity = normalizeTaskQuantity(targetQuantity);
        BigDecimal remainingQuantity = totalQuantity;
        for (ShiftWindow window : filteredWindows) {
            if (!window.usableEnd.isAfter(cursor)) {
                continue;
            }
            LocalDateTime segmentStart = window.startTime.isAfter(cursor) ? window.startTime : cursor;
            if (!window.usableEnd.isAfter(segmentStart)) {
                continue;
            }
            int usableMinutes = Math.toIntExact(Duration.between(segmentStart, window.usableEnd).toMinutes());
            int availableMinutes = usableMinutes;
            if (availableMinutes <= 0) {
                continue;
            }
            BigDecimal remainingDailyCapacity = capacityLedger.remaining(lineId, routeProcess.getProcessId(),
                    window.calendarDate, scheduleOrderProcess);
            if (remainingDailyCapacity == null) {
                remainingDailyCapacity = remainingQuantity;
            } else {
                remainingDailyCapacity = remainingDailyCapacity.setScale(0, RoundingMode.FLOOR);
            }
            BigDecimal maxQuantityByWindow = maxCapacityLimitedQuantityForWindow(scheduleOrderProcess, pool,
                    totalQuantity, availableMinutes, requiredMinutes);
            BigDecimal segmentQuantity = minQuantity(remainingQuantity, remainingDailyCapacity, maxQuantityByWindow);
            segmentQuantity = fitCapacityLimitedQuantityWithinWindow(scheduleOrderProcess, pool, totalQuantity,
                    segmentQuantity, availableMinutes, requiredMinutes);
            if (segmentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                cursor = window.usableEnd;
                continue;
            }
            int segmentMinutes = minutesForCapacityLimitedQuantity(scheduleOrderProcess, pool, totalQuantity,
                    segmentQuantity, requiredMinutes);
            if (segmentMinutes <= 0 || segmentMinutes > availableMinutes) {
                return Collections.emptyList();
            }
            LocalDateTime segmentEnd = segmentStart.plusMinutes(segmentMinutes);
            plans.add(buildPlannedTask(workOrder, routeProduct, routeProcess, scheduleOrderProcess, pool,
                    lineId, lineName, segmentStart, segmentEnd, segmentMinutes, segmentQuantity));
            capacityLedger.reserve(lineId, routeProcess.getProcessId(), window.calendarDate,
                    scheduleOrderProcess, segmentQuantity);
            remainingQuantity = remainingQuantity.subtract(segmentQuantity);
            remainingMinutes = Math.max(0, remainingMinutes - segmentMinutes);
            cursor = segmentEnd;
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return Collections.emptyList();
        }
        for (int i = 0; i < plans.size(); i++) {
            plans.get(i).segmentIndex = i + 1;
        }
        return plans;
    }

    private BigDecimal minQuantity(BigDecimal... quantities) {
        BigDecimal result = null;
        for (BigDecimal quantity : quantities) {
            if (quantity == null) {
                continue;
            }
            if (result == null || quantity.compareTo(result) < 0) {
                result = quantity;
            }
        }
        return result == null ? BigDecimal.ZERO : result;
    }

    private BigDecimal fitCapacityLimitedQuantityWithinWindow(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                              ProcessResourcePool pool,
                                                              BigDecimal totalQuantity,
                                                              BigDecimal quantity,
                                                              int availableMinutes,
                                                              int requiredMinutes) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0 || availableMinutes <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal fittedQuantity = quantity.setScale(0, RoundingMode.FLOOR);
        while (fittedQuantity.compareTo(BigDecimal.ZERO) > 0
                && minutesForCapacityLimitedQuantity(scheduleOrderProcess, pool, totalQuantity,
                fittedQuantity, requiredMinutes) > availableMinutes) {
            fittedQuantity = fittedQuantity.subtract(BigDecimal.ONE);
        }
        return fittedQuantity;
    }

    private BigDecimal maxCapacityLimitedQuantityForWindow(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                           ProcessResourcePool pool,
                                                           BigDecimal totalQuantity,
                                                           int availableMinutes,
                                                           int requiredMinutes) {
        if (availableMinutes <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal hourlyCapacity = resolveFiniteHourlyCapacity(scheduleOrderProcess, pool);
        if (hourlyCapacity != null && hourlyCapacity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal quantity = hourlyCapacity
                    .multiply(BigDecimal.valueOf(availableMinutes))
                    .divide(BigDecimal.valueOf(60), 0, RoundingMode.FLOOR);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0
                    && minutesForHourlyCapacityQuantity(hourlyCapacity, BigDecimal.ONE) <= availableMinutes) {
                return BigDecimal.ONE;
            }
            return totalQuantity == null ? quantity : quantity.min(totalQuantity);
        }
        BigDecimal dailyCapacity = resolveWholeUnitDailyCapacity(scheduleOrderProcess);
        int dailyWindowMinutes = resolveDailyWindowMinutes(scheduleOrderProcess);
        if (dailyCapacity != null && dailyCapacity.compareTo(BigDecimal.ZERO) > 0 && dailyWindowMinutes > 0) {
            BigDecimal quantity = dailyCapacity
                    .multiply(BigDecimal.valueOf(availableMinutes))
                    .divide(BigDecimal.valueOf(dailyWindowMinutes), 0, RoundingMode.FLOOR);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0
                    && minutesForDailyCapacityQuantity(dailyCapacity, dailyWindowMinutes, BigDecimal.ONE) <= availableMinutes) {
                return BigDecimal.ONE;
            }
            return totalQuantity == null ? quantity : quantity.min(totalQuantity);
        }
        return maxQuantityForMinutes(totalQuantity, availableMinutes, requiredMinutes);
    }

    private int minutesForCapacityLimitedQuantity(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                  ProcessResourcePool pool,
                                                  BigDecimal totalQuantity,
                                                  BigDecimal quantity,
                                                  int requiredMinutes) {
        if (isInfiniteFormulaCapacity(scheduleOrderProcess)) {
            return minutesForQuantity(totalQuantity, quantity, requiredMinutes);
        }
        BigDecimal hourlyCapacity = resolveFiniteHourlyCapacity(scheduleOrderProcess, pool);
        if (hourlyCapacity != null && hourlyCapacity.compareTo(BigDecimal.ZERO) > 0) {
            return minutesForHourlyCapacityQuantity(hourlyCapacity, quantity);
        }
        BigDecimal dailyCapacity = resolveWholeUnitDailyCapacity(scheduleOrderProcess);
        int dailyWindowMinutes = resolveDailyWindowMinutes(scheduleOrderProcess);
        if (dailyCapacity != null && dailyCapacity.compareTo(BigDecimal.ZERO) > 0 && dailyWindowMinutes > 0) {
            return minutesForDailyCapacityQuantity(dailyCapacity, dailyWindowMinutes, quantity);
        }
        return minutesForQuantity(totalQuantity, quantity, requiredMinutes);
    }

    private BigDecimal resolveFiniteHourlyCapacity(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                   ProcessResourcePool pool) {
        if (isInfiniteFormulaCapacity(scheduleOrderProcess)) {
            return null;
        }
        if (scheduleOrderProcess != null
                && scheduleOrderProcess.getHourlyCapacityTotal() != null
                && scheduleOrderProcess.getHourlyCapacityTotal().compareTo(BigDecimal.ZERO) > 0) {
            return scheduleOrderProcess.getHourlyCapacityTotal();
        }
        if (pool == null || pool.effectiveHourlyCapacity == null
                || pool.effectiveHourlyCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return pool.effectiveHourlyCapacity;
    }

    private boolean isInfiniteFormulaCapacity(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        return scheduleOrderProcess != null
                && MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(scheduleOrderProcess.getCapacityMode());
    }

    private BigDecimal resolveWholeUnitDailyCapacity(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess == null || scheduleOrderProcess.getShiftCapacityTotal() == null
                || scheduleOrderProcess.getShiftCapacityTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal wholeUnitCapacity = scheduleOrderProcess.getShiftCapacityTotal()
                .setScale(0, RoundingMode.FLOOR);
        return wholeUnitCapacity.compareTo(BigDecimal.ZERO) > 0 ? wholeUnitCapacity : null;
    }

    private int resolveDailyWindowMinutes(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess == null || scheduleOrderProcess.getShiftHours() == null
                || scheduleOrderProcess.getShiftHours().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return scheduleOrderProcess.getShiftHours()
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int minutesForHourlyCapacityQuantity(BigDecimal hourlyCapacity, BigDecimal quantity) {
        if (hourlyCapacity == null || hourlyCapacity.compareTo(BigDecimal.ZERO) <= 0
                || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return Math.max(1, quantity
                .multiply(BigDecimal.valueOf(60))
                .divide(hourlyCapacity, 0, RoundingMode.CEILING)
                .intValue());
    }

    private int minutesForDailyCapacityQuantity(BigDecimal dailyCapacity, int dailyWindowMinutes,
                                                BigDecimal quantity) {
        if (dailyCapacity == null || dailyCapacity.compareTo(BigDecimal.ZERO) <= 0
                || dailyWindowMinutes <= 0
                || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return Math.max(1, quantity
                .multiply(BigDecimal.valueOf(dailyWindowMinutes))
                .divide(dailyCapacity, 0, RoundingMode.CEILING)
                .intValue());
    }

    private BigDecimal maxQuantityForMinutes(BigDecimal totalQuantity, int availableMinutes, int requiredMinutes) {
        if (availableMinutes <= 0 || requiredMinutes <= 0 || totalQuantity == null
                || totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal quantity = totalQuantity
                .multiply(BigDecimal.valueOf(availableMinutes))
                .divide(BigDecimal.valueOf(requiredMinutes), 0, RoundingMode.FLOOR);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0
                && minutesForQuantity(totalQuantity, BigDecimal.ONE, requiredMinutes) <= availableMinutes) {
            return BigDecimal.ONE;
        }
        return quantity.min(totalQuantity);
    }

    private int minutesForQuantity(BigDecimal totalQuantity, BigDecimal quantity, int requiredMinutes) {
        if (requiredMinutes <= 0 || totalQuantity == null || totalQuantity.compareTo(BigDecimal.ZERO) <= 0
                || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return Math.max(1, quantity
                .multiply(BigDecimal.valueOf(requiredMinutes))
                .divide(totalQuantity, 0, RoundingMode.CEILING)
                .intValue());
    }

    private BigDecimal allocateSegmentQuantity(BigDecimal targetQuantity, int segmentMinutes, int requiredMinutes) {
        if (targetQuantity == null) {
            return null;
        }
        return targetQuantity
                .multiply(BigDecimal.valueOf(segmentMinutes))
                .divide(BigDecimal.valueOf(requiredMinutes), 0, RoundingMode.HALF_UP);
    }

    private PlannedTask buildPlannedTask(MesProWorkOrderDO workOrder, MesProRouteProductDO routeProduct,
                                         MesProRouteProcessDO routeProcess,
                                         MesProScheduleOrderProcessDO scheduleOrderProcess, ProcessResourcePool pool,
                                         Long lineId, String lineName, LocalDateTime startTime, LocalDateTime endTime,
                                         int plannedDurationMinutes, BigDecimal quantity) {
        PlannedTask plan = new PlannedTask();
        plan.workOrderId = workOrder.getId();
        plan.routeId = routeProduct.getRouteId();
        plan.processId = routeProcess.getProcessId();
        plan.scheduleOrderProcessId = scheduleOrderProcess == null ? null : scheduleOrderProcess.getId();
        plan.itemId = workOrder.getProductId();
        plan.clientId = workOrder.getClientId();
        plan.workstationId = pool.primaryWorkstationId;
        plan.lineId = lineId;
        plan.lineName = lineName;
        plan.quantity = normalizeTaskQuantity(quantity == null ? workOrder.getQuantity() : quantity);
        plan.startTime = startTime;
        plan.endTime = endTime;
        plan.durationBlocks = Math.max(1, (int) Math.ceil(plannedDurationMinutes / 480D));
        plan.colorCode = routeProcess.getColorCode();
        plan.plannedDurationMinutes = plannedDurationMinutes;
        plan.segmentIndex = 1;
        return plan;
    }

    private BigDecimal normalizeTaskQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return null;
        }
        return quantity.setScale(0, RoundingMode.UP);
    }

    private List<ShiftWindow> safeList(List<ShiftWindow> windows) {
        return windows == null ? Collections.emptyList() : windows;
    }

    private boolean hasRouteProcessTopologySnapshot(Collection<MesProScheduleOrderProcessDO> snapshotProcesses) {
        return snapshotProcesses != null && !snapshotProcesses.isEmpty()
                && snapshotProcesses.stream().anyMatch(item -> item != null
                && (item.getPredecessorRouteProcessId() != null || item.getRootProcessFlag() != null));
    }

    private boolean hasInactiveTopologyPredecessor(Collection<MesProScheduleOrderProcessDO> snapshotProcesses) {
        if (snapshotProcesses == null || snapshotProcesses.isEmpty()) {
            return false;
        }
        Set<Long> activeRouteProcessIds = snapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return snapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getPredecessorRouteProcessId)
                .filter(Objects::nonNull)
                .anyMatch(predecessorRouteProcessId -> !activeRouteProcessIds.contains(predecessorRouteProcessId));
    }

    private List<MesProScheduleOrderProcessDO> activeTopologyScheduleOrderProcesses(
            Collection<MesProScheduleOrderProcessDO> processes) {
        if (processes == null || processes.isEmpty()) {
            return Collections.emptyList();
        }
        return processes.stream()
                .filter(this::isActiveTopologyScheduleOrderProcess)
                .toList();
    }

    private boolean isActiveTopologyScheduleOrderProcess(MesProScheduleOrderProcessDO process) {
        return process != null
                && !Boolean.FALSE.equals(process.getEnabled())
                && process.getRouteProcessId() != null;
    }

    @FunctionalInterface
    public interface DailyAvailableWindowMinutesResolver {
        int resolve(Long lineId, LocalDate planDate, MesProScheduleOrderProcessDO scheduleOrderProcess);
    }

    @FunctionalInterface
    public interface LatestStartCalendarShiftModeResolver {
        String resolve(LocalDate date, MesProScheduleOrderProcessDO scheduleOrderProcess);
    }

    public static final class ScheduleComputation {
        boolean replanMode;
        String capacityMode;
        LocalDateTime requestStartTime;
        Boolean preserveManualLockedTasks;
        MesProScheduleCalendarRulesRespVO calendarSummary;
        AutoScheduleCalendarContext calendarContext;
        Map<Long, MesProScheduleCalendarRulesRespVO> processCalendarSummariesByRuleId = new LinkedHashMap<>();
        Map<Long, AutoScheduleCalendarContext> processCalendarContextByRuleId = new LinkedHashMap<>();
        List<MesProWorkOrderDO> workOrders = new ArrayList<>();
        Map<Long, MesProWorkOrderDO> workOrderMap = new LinkedHashMap<>();
        List<MesProScheduleOrderDO> scheduleOrders = new ArrayList<>();
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = new LinkedHashMap<>();
        Map<Long, List<MesProScheduleOrderProcessDO>> scheduleOrderProcessesByOrderId = new LinkedHashMap<>();
        Map<Long, MesProRouteProductDO> routeProductByWorkOrderId = new LinkedHashMap<>();
        Map<Long, MesProRouteDO> routeMap = new LinkedHashMap<>();
        Map<Long, List<MesProRouteProcessDO>> routeProcessesByWorkOrderId = new LinkedHashMap<>();
        Map<Long, Map<Long, BigDecimal>> materialDemandByWorkOrderId = new LinkedHashMap<>();
        Set<Long> workOrderIdsWithProductionMaterialList = new LinkedHashSet<>();
        Set<Long> workOrderIdsWithUnmappedProductionMaterialList = new LinkedHashSet<>();
        Map<Long, MesMdItemDO> itemMap = new LinkedHashMap<>();
        Map<Long, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO> processMap = new LinkedHashMap<>();
        Map<Long, List<MesMdWorkstationDO>> workstationsByRouteProcessId = new LinkedHashMap<>();
        Map<Long, Set<Long>> workstationProcessAliasesByRouteProcessId = new LinkedHashMap<>();
        Map<Long, MesMdWorkstationDO> workstationMap = new LinkedHashMap<>();
        Map<Long, MesMdProductionLineDO> lineMap = new LinkedHashMap<>();
        Map<String, ProcessResourcePool> processResourcePoolByLineProcessKey = new LinkedHashMap<>();
        Map<Long, MesCalPlanDO> planMap = new LinkedHashMap<>();
        Map<Long, List<MesCalPlanShiftDO>> shiftListByPlanId = new LinkedHashMap<>();
        Map<Long, MesCalPlanShiftDO> shiftMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> availableStockByItemId = new LinkedHashMap<>();
        Map<Long, List<ShiftWindow>> shiftWindowsByLineId = new LinkedHashMap<>();
        List<MesProTaskDO> scopeTasks = new ArrayList<>();
        Map<Long, MesProTaskScheduleExtDO> taskExtMap = new LinkedHashMap<>();
        Map<Long, List<MesProFeedbackDO>> feedbackByTaskId = new LinkedHashMap<>();
        List<MesProTaskDO> preservedTasks = new ArrayList<>();
        List<MesProTaskDO> replaceableScopeTasks = new ArrayList<>();
        Set<Long> preservedTaskIds = new LinkedHashSet<>();
        Set<Long> nonBlockingSkippedWorkOrderIds = new LinkedHashSet<>();
        Map<String, List<MesProTaskDO>> preservedTaskByWorkOrderProcess = new LinkedHashMap<>();
        Map<Long, String> protectionReasonByTaskId = new LinkedHashMap<>();
        Map<Long, LocalDateTime> lineAvailableFrom = new LinkedHashMap<>();
        Map<String, LocalDateTime> lineProcessAvailableFrom = new LinkedHashMap<>();
        DailyProcessCapacityLedger processCapacityLedger = new DailyProcessCapacityLedger();
        List<PlannedTask> generatedTasks = new ArrayList<>();
        List<ScheduleIssueDraft> issues = new ArrayList<>();
        Map<Long, List<PreviewStep>> finalSteps = new LinkedHashMap<>();
        Map<Long, RejectedLatestStartPlan> latestStartRejectedPlans = new LinkedHashMap<>();
        List<LinkPlan> linkPlans = new ArrayList<>();
        List<cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO> previewTasks = new ArrayList<>();
        List<cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttLinkRespVO> previewLinks = new ArrayList<>();
        List<MesProScheduleCalendarWorkOrderAnalysisRespVO> workOrderAnalyses = new ArrayList<>();
    }

    public static final class AutoScheduleCalendarContext {
        final LocalDate horizonStartDate;
        final LocalDate horizonEndDate;
        final Set<String> holidayDateSet;
        final Map<String, String> effectiveShiftModeByDate;
        final String token;

        AutoScheduleCalendarContext(LocalDate horizonStartDate,
                                    LocalDate horizonEndDate,
                                    Set<String> holidayDateSet,
                                    Map<String, String> effectiveShiftModeByDate,
                                    String token) {
            this.horizonStartDate = horizonStartDate;
            this.horizonEndDate = horizonEndDate;
            this.holidayDateSet = holidayDateSet;
            this.effectiveShiftModeByDate = effectiveShiftModeByDate;
            this.token = token;
        }
    }

    public static final class PlannedTask {
        Long workOrderId;
        Long routeId;
        Long processId;
        Long scheduleOrderProcessId;
        Long itemId;
        Long clientId;
        Long workstationId;
        Long lineId;
        String lineName;
        BigDecimal quantity;
        LocalDateTime startTime;
        LocalDateTime endTime;
        LocalDateTime dependencyReleasedAt;
        Integer durationBlocks;
        String colorCode;
        Integer plannedDurationMinutes;
        Integer segmentIndex;
    }

    public static final class LinkPlan {
        final Long workOrderId;
        final Long sourceProcessId;
        final Long targetProcessId;

        LinkPlan(Long workOrderId, Long sourceProcessId, Long targetProcessId) {
            this.workOrderId = workOrderId;
            this.sourceProcessId = sourceProcessId;
            this.targetProcessId = targetProcessId;
        }
    }

    public static final class RejectedLatestStartPlan {
        LocalDateTime plannedStartTime;
        LocalDateTime plannedEndTime;
        LocalDateTime latestStartTime;

        static RejectedLatestStartPlan from(CandidateLinePlan selectedPlan, LocalDateTime latestStartTime) {
            RejectedLatestStartPlan plan = new RejectedLatestStartPlan();
            plan.plannedStartTime = selectedPlan.startTime;
            plan.plannedEndTime = selectedPlan.endTime;
            plan.latestStartTime = latestStartTime;
            return plan;
        }
    }

    public static final class ScheduleIssueDraft {
        private static long sequence = -1L;

        Long id;
        String issueType;
        String severity;
        Long workOrderId;
        Long processId;
        Long workstationId;
        Long materialId;
        LocalDateTime calendarDate;
        Long shiftId;
        BigDecimal requiredQty;
        BigDecimal availableQty;
        BigDecimal shortageQty;
        String message;

        static ScheduleIssueDraft blocking(String issueType, Long workOrderId, Long processId,
                                           Long workstationId, Long materialId, String message) {
            ScheduleIssueDraft draft = new ScheduleIssueDraft();
            draft.id = sequence--;
            draft.issueType = issueType;
            draft.severity = ISSUE_SEVERITY_BLOCKING;
            draft.workOrderId = workOrderId;
            draft.processId = processId;
            draft.workstationId = workstationId;
            draft.materialId = materialId;
            draft.message = message;
            return draft;
        }

        static ScheduleIssueDraft warning(String issueType, Long workOrderId, Long processId,
                                          Long workstationId, Long materialId, String message) {
            ScheduleIssueDraft draft = new ScheduleIssueDraft();
            draft.id = sequence--;
            draft.issueType = issueType;
            draft.severity = ISSUE_SEVERITY_WARNING;
            draft.workOrderId = workOrderId;
            draft.processId = processId;
            draft.workstationId = workstationId;
            draft.materialId = materialId;
            draft.message = message;
            return draft;
        }

        ScheduleIssueDraft withQty(BigDecimal requiredQty, BigDecimal availableQty, BigDecimal shortageQty) {
            this.requiredQty = requiredQty;
            this.availableQty = availableQty;
            this.shortageQty = shortageQty;
            return this;
        }

        ScheduleIssueDraft withCalendarShift(LocalDate date, Long shiftId) {
            this.calendarDate = date == null ? null : date.atStartOfDay();
            this.shiftId = shiftId;
            return this;
        }

        ScheduleIssueDraft copyWithSeverity(String severity) {
            ScheduleIssueDraft draft = new ScheduleIssueDraft();
            draft.id = this.id;
            draft.issueType = this.issueType;
            draft.severity = severity;
            draft.workOrderId = this.workOrderId;
            draft.processId = this.processId;
            draft.workstationId = this.workstationId;
            draft.materialId = this.materialId;
            draft.calendarDate = this.calendarDate;
            draft.shiftId = this.shiftId;
            draft.requiredQty = this.requiredQty;
            draft.availableQty = this.availableQty;
            draft.shortageQty = this.shortageQty;
            draft.message = this.message;
            return draft;
        }

        MesProScheduleIssueDO toDO(Long taskId) {
            return MesProScheduleIssueDO.builder()
                    .id(id != null && id > 0 ? id : null)
                    .issueType(issueType)
                    .severity(severity)
                    .workOrderId(workOrderId)
                    .taskId(taskId)
                    .processId(processId)
                    .workstationId(workstationId)
                    .materialId(materialId)
                    .calendarDate(calendarDate)
                    .shiftId(shiftId)
                    .requiredQty(requiredQty)
                    .availableQty(availableQty)
                    .shortageQty(shortageQty)
                    .message(message)
                    .resolved(Boolean.FALSE)
                    .build();
        }
    }

    public static final class CandidateLinePlan {
        LocalDateTime startTime;
        LocalDateTime endTime;
        List<ScheduleIssueDraft> failureIssues;
        MesProScheduleCalendarWorkOrderAnalysisRespVO analysis;
        final List<PlannedTask> plans = new ArrayList<>();
        final List<PreviewStep> steps = new ArrayList<>();
        final Map<String, LocalDateTime> processAvailableUntilByKey = new LinkedHashMap<>();
        DailyProcessCapacityLedger capacityLedgerAfterPlan;

        CandidateLinePlan() {
        }

        static CandidateLinePlan success() {
            return new CandidateLinePlan();
        }

        static CandidateLinePlan failed(ScheduleIssueDraft issue) {
            return failed(List.of(issue));
        }

        static CandidateLinePlan failed(List<ScheduleIssueDraft> issues) {
            CandidateLinePlan plan = new CandidateLinePlan();
            plan.failureIssues = issues;
            return plan;
        }
    }

    public static final class ProcessLineCandidate {
        final Long lineId;
        final String lineCode;
        final String lineName;
        final String availabilityKey;
        final ProcessResourcePool pool;
        final List<PlannedTask> plans;
        final int requiredMinutes;
        final LocalDateTime endTime;
        final List<ScheduleIssueDraft> failureIssues;
        final DailyProcessCapacityLedger capacityLedgerAfterPlan;

        ProcessLineCandidate(Long lineId, String lineCode, String lineName, String availabilityKey, ProcessResourcePool pool,
                              List<PlannedTask> plans, int requiredMinutes, LocalDateTime endTime,
                              List<ScheduleIssueDraft> failureIssues,
                              DailyProcessCapacityLedger capacityLedgerAfterPlan) {
            this.lineId = lineId;
            this.lineCode = lineCode;
            this.lineName = lineName;
            this.availabilityKey = availabilityKey;
            this.pool = pool;
            this.plans = plans;
            this.requiredMinutes = requiredMinutes;
            this.endTime = endTime;
            this.failureIssues = failureIssues;
            this.capacityLedgerAfterPlan = capacityLedgerAfterPlan;
        }

        static ProcessLineCandidate success(Long lineId, String lineCode, String lineName,
                                            ProcessResourcePool pool, List<PlannedTask> plans,
                                            int requiredMinutes) {
            return success(lineId, lineCode, lineName, null, pool, plans, requiredMinutes);
        }

        static ProcessLineCandidate success(Long lineId, String lineCode, String lineName,
                                             String availabilityKey, ProcessResourcePool pool, List<PlannedTask> plans,
                                             int requiredMinutes) {
            return success(lineId, lineCode, lineName, availabilityKey, pool, plans, requiredMinutes, null);
        }

        static ProcessLineCandidate success(Long lineId, String lineCode, String lineName,
                                            String availabilityKey, ProcessResourcePool pool, List<PlannedTask> plans,
                                            int requiredMinutes, DailyProcessCapacityLedger capacityLedgerAfterPlan) {
            LocalDateTime endTime = plans.stream()
                    .map(plan -> plan.endTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            return new ProcessLineCandidate(lineId, lineCode, lineName, availabilityKey, pool, plans, requiredMinutes,
                    endTime, null, capacityLedgerAfterPlan);
        }

        static ProcessLineCandidate failed(ScheduleIssueDraft issue) {
            return failed(List.of(issue));
        }

        static ProcessLineCandidate failed(List<ScheduleIssueDraft> issues) {
            return new ProcessLineCandidate(null, null, null, null, null, Collections.emptyList(), 0, null,
                    issues, null);
        }

        String lineLabel() {
            return StrUtil.blankToDefault(lineName, "--");
        }
    }

    public static final class ProcessResourcePool {
        final Long lineId;
        final Long processId;
        final String processName;
        final List<Long> workstationIds = new ArrayList<>();
        final List<String> workstationNames = new ArrayList<>();
        int workstationCount;
        int machineCount;
        int configuredWorkerCount;
        int currentWorkerCount;
        boolean workerHourlyCapacityConfigured;
        BigDecimal effectiveHourlyCapacity = BigDecimal.ZERO;
        String capacitySource = CAPACITY_SOURCE_WORKER;
        Long primaryWorkstationId;

        ProcessResourcePool(Long lineId, Long processId, String processName) {
            this.lineId = lineId;
            this.processId = processId;
            this.processName = processName;
        }

        void addWorkstation(MesMdWorkstationDO workstation,
                            MesMdWorkstationCapacityMetrics metrics,
                            List<MesMdWorkstationMachineDO> machines) {
            this.workstationCount += 1;
            this.workstationIds.add(workstation.getId());
            this.workstationNames.add(workstation.getName());
            this.machineCount += machines.stream()
                    .map(MesMdWorkstationMachineDO::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);
            this.configuredWorkerCount += ObjUtil.defaultIfNull(metrics.getConfiguredWorkerCount(), 0);
            this.currentWorkerCount += ObjUtil.defaultIfNull(metrics.getCurrentWorkerCount(), 0);
            this.workerHourlyCapacityConfigured = this.workerHourlyCapacityConfigured
                    || workstation.getSingleStandardHourlyCapacity() != null;
            this.effectiveHourlyCapacity = this.effectiveHourlyCapacity.add(ObjUtil.defaultIfNull(metrics.getTodayCapacity(), BigDecimal.ZERO));
            if (this.machineCount > 0) {
                this.capacitySource = CAPACITY_SOURCE_MACHINE;
            }
            if (this.primaryWorkstationId == null || this.primaryWorkstationId > workstation.getId()) {
                this.primaryWorkstationId = workstation.getId();
            }
        }
    }

    public static final class PreviewStep {
        Long taskId;
        Long workOrderId;
        Long processId;
        Long workstationId;
        Long itemId;
        Long originalId;
        BigDecimal quantity;
        LocalDateTime startTime;
        LocalDateTime endTime;
        Long duration;
        String colorCode;
        String lineName;
        String scheduleSource;
        Boolean locked;
        String riskStatus;
        Integer segmentIndex;
        boolean generated;

        static PreviewStep fromExisting(MesProTaskDO task) {
            PreviewStep step = new PreviewStep();
            step.taskId = task.getId();
            step.workOrderId = task.getWorkOrderId();
            step.processId = task.getProcessId();
            step.workstationId = task.getWorkstationId();
            step.itemId = task.getItemId();
            step.originalId = task.getId();
            step.quantity = task.getQuantity();
            step.startTime = task.getStartTime();
            step.endTime = task.getEndTime();
            step.duration = task.getDuration() == null ? null : task.getDuration().longValue();
            step.colorCode = task.getColorCode();
            step.scheduleSource = SCHEDULE_SOURCE_MANUAL;
            step.locked = Boolean.TRUE;
            step.riskStatus = RISK_STATUS_NONE;
            step.segmentIndex = 1;
            step.generated = false;
            return step;
        }

        static PreviewStep fromPlan(PlannedTask plan) {
            PreviewStep step = new PreviewStep();
            step.taskId = null;
            step.workOrderId = plan.workOrderId;
            step.processId = plan.processId;
            step.workstationId = plan.workstationId;
            step.itemId = plan.itemId;
            step.originalId = null;
            step.quantity = plan.quantity;
            step.startTime = plan.startTime;
            step.endTime = plan.endTime;
            step.duration = plan.durationBlocks.longValue();
            step.colorCode = plan.colorCode;
            step.lineName = plan.lineName;
            step.scheduleSource = SCHEDULE_SOURCE_AUTO;
            step.locked = Boolean.FALSE;
            step.riskStatus = RISK_STATUS_NONE;
            step.segmentIndex = plan.segmentIndex;
            step.generated = true;
            return step;
        }

        String ganttNodeId() {
            String prefix = String.valueOf(MesBizTypeConstants.PRO_TASK);
            if (originalId != null) {
                return prefix + "_" + originalId;
            }
            return prefix + "_preview_" + workOrderId + "_" + processId + "_" + ObjUtil.defaultIfNull(segmentIndex, 1);
        }

        GanttDataRespVO toGanttDataRespVO(Long previewOriginalId, MesMdWorkstationDO workstation,
                                          cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO process,
                                          MesMdItemDO item,
                                          String workOrderCode) {
            Long resolvedOriginalId = originalId != null ? originalId : previewOriginalId;
            String resolvedLineName = lineName;
            if (resolvedLineName == null && workstation != null) {
                resolvedLineName = null;
            }
            return new GanttDataRespVO()
                    .setId(originalId != null
                            ? MesBizTypeConstants.PRO_TASK + "_" + originalId
                            : MesBizTypeConstants.PRO_TASK + "_preview_" + workOrderId + "_" + processId + "_" + ObjUtil.defaultIfNull(segmentIndex, 1))
                    .setOriginalId(resolvedOriginalId)
                    .setType(MesBizTypeConstants.PRO_TASK)
                    .setParent(MesBizTypeConstants.PRO_WORKORDER + "_" + workOrderId)
                    .setText((item != null ? item.getName() : "") + (quantity != null ? quantity.stripTrailingZeros().toPlainString() : ""))
                    .setWorkOrderCode(workOrderCode)
                    .setWorkstation(workstation != null ? workstation.getName() : null)
                    .setProcess(process != null ? process.getName() : null)
                    .setLine(resolvedLineName)
                    .setColor(colorCode)
                    .setQuantity(quantity)
                    .setScheduleSource(scheduleSource)
                    .setLocked(locked)
                    .setRiskStatus(riskStatus)
                    .setStartDate(startTime)
                    .setEndDate(endTime)
                    .setDuration(duration)
                    .setProgress(0F);
        }
    }

    private record DailyExplanationKey(LocalDate planDate, Long workOrderId, Long scheduleOrderProcessId,
                                       Long processId) {
    }

    public static final class DailyProcessCapacityLedger {
        private final Map<DailyProcessCapacityKey, BigDecimal> usedQuantityByKey = new LinkedHashMap<>();

        public DailyProcessCapacityLedger copy() {
            DailyProcessCapacityLedger copy = new DailyProcessCapacityLedger();
            copy.usedQuantityByKey.putAll(this.usedQuantityByKey);
            return copy;
        }

        boolean isCapacityLimited(MesProScheduleOrderProcessDO scheduleOrderProcess) {
            return resolveDailyMaxCapacity(scheduleOrderProcess) != null;
        }

        BigDecimal remaining(Long lineId, Long processId, LocalDate planDate,
                             MesProScheduleOrderProcessDO scheduleOrderProcess) {
            BigDecimal maxCapacity = resolveDailyMaxCapacity(scheduleOrderProcess);
            if (maxCapacity == null || planDate == null || processId == null) {
                return null;
            }
            BigDecimal used = usedQuantityByKey.getOrDefault(
                    new DailyProcessCapacityKey(lineId, processId, planDate), BigDecimal.ZERO);
            BigDecimal remaining = maxCapacity.subtract(used);
            return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
        }

        void reserve(Long lineId, Long processId, LocalDate planDate,
                     MesProScheduleOrderProcessDO scheduleOrderProcess, BigDecimal quantity) {
            if (scheduleOrderProcess != null && resolveDailyMaxCapacity(scheduleOrderProcess) == null) {
                return;
            }
            if (planDate == null || processId == null
                    || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            DailyProcessCapacityKey key = new DailyProcessCapacityKey(lineId, processId, planDate);
            usedQuantityByKey.merge(key, quantity, BigDecimal::add);
        }

        private BigDecimal resolveDailyMaxCapacity(MesProScheduleOrderProcessDO scheduleOrderProcess) {
            if (scheduleOrderProcess == null || scheduleOrderProcess.getShiftCapacityTotal() == null
                    || scheduleOrderProcess.getShiftCapacityTotal().compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            BigDecimal wholeUnitCapacity = scheduleOrderProcess.getShiftCapacityTotal()
                    .setScale(0, RoundingMode.FLOOR);
            return wholeUnitCapacity.compareTo(BigDecimal.ZERO) > 0 ? wholeUnitCapacity : BigDecimal.ZERO;
        }
    }

    private record DailyProcessCapacityKey(Long lineId, Long processId, LocalDate planDate) {
    }

    private static final class DailyExplanationAccumulator {
        private final DailyExplanationKey key;
        private Long lineId;
        private BigDecimal plannedQuantity = BigDecimal.ZERO;
        private int generatedTaskCount;
        private int usedWindowMinutes;
        private int protectedOccupiedMinutes;
        private LocalDateTime dependencyReleasedAt;

        private DailyExplanationAccumulator(DailyExplanationKey key) {
            this.key = key;
        }
    }

}
