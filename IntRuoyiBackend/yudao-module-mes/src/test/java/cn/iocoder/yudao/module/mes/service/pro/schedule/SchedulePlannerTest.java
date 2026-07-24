package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProReplanExplanationRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchedulePlannerTest {

    @Test
    void planner_shouldExposePlanValueObjectsWithoutPersistenceDependencies() {
        assertNotNull(SchedulePlanner.ScheduleComputation.class);
        assertNotNull(SchedulePlanner.PlannedTask.class);
        assertNotNull(SchedulePlanner.PreviewStep.class);
        assertNotNull(SchedulePlanner.LinkPlan.class);
        assertNotNull(SchedulePlanner.ScheduleIssueDraft.class);

        assertFalse(Arrays.stream(SchedulePlanner.class.getDeclaredFields())
                .anyMatch(this::isPersistenceOrInjectedDependency));
    }

    @Test
    void buildDailyExplanations_shouldAggregateGeneratedAndProtectedPlanFacts() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = new SchedulePlanner.ScheduleComputation();
        computation.workOrderMap.put(10L, MesProWorkOrderTestData.workOrder(10L, "MO-10"));
        computation.scheduleOrders.add(MesProScheduleOrderDO.builder()
                .id(20L)
                .workOrderId(10L)
                .build());
        computation.scheduleOrderProcessesByOrderId.put(20L, List.of(MesProScheduleOrderProcessDO.builder()
                .id(30L)
                .processId(40L)
                .remainingQuantity(new BigDecimal("10"))
                .build()));
        computation.processMap.put(40L, MesProProcessDO.builder()
                .id(40L)
                .name("组装")
                .build());
        computation.workstationMap.put(50L, MesMdWorkstationDO.builder()
                .id(50L)
                .productionLineId(60L)
                .build());

        SchedulePlanner.PlannedTask generated = new SchedulePlanner.PlannedTask();
        generated.workOrderId = 10L;
        generated.processId = 40L;
        generated.scheduleOrderProcessId = 30L;
        generated.lineId = 60L;
        generated.quantity = new BigDecimal("3");
        generated.startTime = LocalDateTime.of(2026, 5, 15, 8, 0);
        generated.endTime = LocalDateTime.of(2026, 5, 15, 9, 0);
        generated.dependencyReleasedAt = LocalDateTime.of(2026, 5, 15, 7, 30);
        computation.generatedTasks.add(generated);

        SchedulePlanner.PreviewStep protectedStep = new SchedulePlanner.PreviewStep();
        protectedStep.workOrderId = 10L;
        protectedStep.processId = 40L;
        protectedStep.workstationId = 50L;
        protectedStep.quantity = new BigDecimal("2");
        protectedStep.startTime = LocalDateTime.of(2026, 5, 15, 9, 0);
        protectedStep.endTime = LocalDateTime.of(2026, 5, 15, 9, 30);
        protectedStep.generated = false;
        computation.finalSteps.put(10L, List.of(protectedStep));

        List<MesProReplanExplanationRespVO.DailyExplanationItem> explanations =
                planner.buildDailyExplanations(computation, (lineId, planDate, scheduleOrderProcess) -> 480);

        assertEquals(1, explanations.size());
        MesProReplanExplanationRespVO.DailyExplanationItem item = explanations.get(0);
        assertEquals(LocalDate.of(2026, 5, 15), item.getPlanDate());
        assertEquals(10L, item.getWorkOrderId());
        assertEquals("MO-10", item.getWorkOrderCode());
        assertEquals(30L, item.getScheduleOrderProcessId());
        assertEquals(40L, item.getProcessId());
        assertEquals("组装", item.getProcessName());
        assertEquals(new BigDecimal("5"), item.getPlannedQuantity());
        assertEquals(1, item.getGeneratedTaskCount());
        assertEquals(480, item.getAvailableWindowMinutes());
        assertEquals(60, item.getUsedWindowMinutes());
        assertEquals(30, item.getProtectedOccupiedMinutes());
        assertEquals(LocalDateTime.of(2026, 5, 15, 7, 30), item.getDependencyReleasedAt());
        assertEquals(new BigDecimal("10"), item.getRemainingQuantityBefore());
        assertEquals(new BigDecimal("5"), item.getRemainingQuantityAfter());
        assertEquals("CAPACITY_WINDOW_WITH_PROTECTED", item.getReasonCode());
    }

    @Test
    void buildLinkPlans_shouldUseExplicitTopologySnapshot() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = new SchedulePlanner.ScheduleComputation();
        computation.scheduleOrders.add(MesProScheduleOrderDO.builder()
                .id(20L)
                .workOrderId(10L)
                .build());
        computation.routeProcessesByWorkOrderId.put(10L, List.of(
                MesProRouteProcessDO.builder()
                        .id(1L)
                        .processId(101L)
                        .sort(1)
                        .build(),
                MesProRouteProcessDO.builder()
                        .id(2L)
                        .processId(102L)
                        .sort(2)
                        .build()));
        computation.scheduleOrderProcessesByOrderId.put(20L, List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(30L)
                        .routeProcessId(1L)
                        .processId(1001L)
                        .rootProcessFlag(Boolean.TRUE)
                        .enabled(Boolean.TRUE)
                        .build(),
                MesProScheduleOrderProcessDO.builder()
                        .id(31L)
                        .routeProcessId(2L)
                        .predecessorRouteProcessId(1L)
                        .processId(1002L)
                        .enabled(Boolean.TRUE)
                        .build()));

        List<SchedulePlanner.LinkPlan> linkPlans = planner.buildLinkPlans(computation);

        assertEquals(1, linkPlans.size());
        SchedulePlanner.LinkPlan linkPlan = linkPlans.get(0);
        assertEquals(10L, linkPlan.workOrderId);
        assertEquals(101L, linkPlan.sourceProcessId);
        assertEquals(102L, linkPlan.targetProcessId);
        assertEquals(linkPlans, computation.linkPlans);
    }

    @Test
    void buildLinkPlans_shouldNotInferDependencyWhenTopologySnapshotMissing() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = new SchedulePlanner.ScheduleComputation();
        computation.scheduleOrders.add(MesProScheduleOrderDO.builder()
                .id(20L)
                .workOrderId(10L)
                .build());
        computation.routeProcessesByWorkOrderId.put(10L, List.of(
                MesProRouteProcessDO.builder()
                        .id(1L)
                        .processId(101L)
                        .sort(1)
                        .build(),
                MesProRouteProcessDO.builder()
                        .id(2L)
                        .processId(102L)
                        .sort(2)
                        .build()));
        computation.scheduleOrderProcessesByOrderId.put(20L, List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(30L)
                        .routeProcessId(1L)
                        .processId(101L)
                        .enabled(Boolean.TRUE)
                        .build(),
                MesProScheduleOrderProcessDO.builder()
                        .id(31L)
                        .routeProcessId(2L)
                        .processId(102L)
                        .enabled(Boolean.TRUE)
                        .build()));

        List<SchedulePlanner.LinkPlan> linkPlans = planner.buildLinkPlans(computation);

        assertEquals(List.of(), linkPlans);
        assertEquals(List.of(), computation.linkPlans);
    }

    @Test
    void sortScheduleOrders_shouldPrioritizeNightShiftThenPromiseDatePriorityAndId() {
        SchedulePlanner planner = new SchedulePlanner();
        MesProScheduleOrderDO normalEarlier = scheduleOrder(10L, 100L,
                LocalDate.of(2026, 5, 14), 1);
        MesProScheduleOrderDO nightLaterLowerPriority = scheduleOrder(11L, 101L,
                LocalDate.of(2026, 5, 16), 9);
        MesProScheduleOrderDO nightEarlierHigherPriority = scheduleOrder(12L, 102L,
                LocalDate.of(2026, 5, 15), 5);
        MesProScheduleOrderDO normalSameDateLowerId = scheduleOrder(13L, 103L,
                LocalDate.of(2026, 5, 17), 2);
        MesProScheduleOrderDO normalSameDateHigherId = scheduleOrder(14L, 104L,
                LocalDate.of(2026, 5, 17), 2);
        Map<Long, List<MesProScheduleOrderProcessDO>> processMap = Map.of(
                10L, List.of(scheduleProcess(false)),
                11L, List.of(scheduleProcess(true)),
                12L, List.of(scheduleProcess(true)),
                13L, List.of(scheduleProcess(false)),
                14L, List.of(scheduleProcess(false)));

        List<MesProScheduleOrderDO> sorted = planner.sortScheduleOrders(
                List.of(normalSameDateHigherId, normalEarlier, nightLaterLowerPriority,
                        normalSameDateLowerId, nightEarlierHigherPriority),
                processMap);

        assertEquals(List.of(12L, 11L, 10L, 13L, 14L),
                sorted.stream().map(MesProScheduleOrderDO::getId).toList());
    }

    @Test
    void allocateFiniteProcessPlans_shouldContinueAfterPartialDailyCapacityForLargeRouteProcessOrder() {
        SchedulePlanner planner = new SchedulePlanner();
        CapacityWindowAllocator allocator = new CapacityWindowAllocator();
        MesProScheduleOrderProcessDO scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(30L)
                .processId(922919L)
                .shiftHours(new BigDecimal("10.50"))
                .shiftCapacityTotal(new BigDecimal("270.000003"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        LocalDateTime availableFrom = LocalDateTime.of(2026, 7, 23, 8, 30);
        int requiredMinutes = 2334;
        List<CapacityWindowAllocator.ShiftWindow> windows = allocator.buildRouteProcessShiftWindows(
                scheduleOrderProcess, availableFrom, 2520,
                date -> date.getDayOfWeek().getValue() == 7
                        ? MesProScheduleCalendarRuleSupport.DATE_SHIFT_REST
                        : MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY);
        SchedulePlanner.DailyProcessCapacityLedger ledger = new SchedulePlanner.DailyProcessCapacityLedger();
        ledger.reserve(null, 922919L, LocalDate.of(2026, 7, 23),
                scheduleOrderProcess, new BigDecimal("112"));

        List<SchedulePlanner.PlannedTask> plans = planner.allocateFiniteProcessPlans(
                availableFrom, requiredMinutes, windows,
                MesProWorkOrderTestData.workOrder(10L, "881MO093613"),
                MesProRouteProductDO.builder().routeId(20L).build(),
                MesProRouteProcessDO.builder().id(926634L).routeId(20L).processId(922919L).build(),
                routeProcessPool(922919L, "外管拉伸2", new BigDecimal("25.714286")),
                null, "棘突球囊扩张导管", scheduleOrderProcess,
                new BigDecimal("1000"), ledger);

        assertFalse(plans.isEmpty());
        assertEquals(new BigDecimal("1000"), plans.stream()
                .map(plan -> plan.quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("158"), plans.get(0).quantity);
        assertEquals(LocalDate.of(2026, 7, 28), plans.get(plans.size() - 1).endTime.toLocalDate());
    }

    @Test
    void orderWorkOrdersByScheduleOrder_shouldFollowSortedScheduleOrderScope() {
        SchedulePlanner planner = new SchedulePlanner();
        MesProWorkOrderDO first = MesProWorkOrderTestData.workOrder(100L, "MO-100");
        MesProWorkOrderDO second = MesProWorkOrderTestData.workOrder(101L, "MO-101");
        MesProWorkOrderDO third = MesProWorkOrderTestData.workOrder(102L, "MO-102");

        List<MesProWorkOrderDO> ordered = planner.orderWorkOrdersByScheduleOrder(
                List.of(third, first, second),
                List.of(scheduleOrder(1L, 101L, null, null),
                        scheduleOrder(2L, 100L, null, null),
                        scheduleOrder(3L, 102L, null, null)));

        assertEquals(List.of(101L, 100L, 102L),
                ordered.stream().map(MesProWorkOrderDO::getId).toList());
    }

    @Test
    void selectBestProcessLineCandidate_shouldUseRequiredLineOnly() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = candidateComputation(200L, 11L, 12L);
        List<Long> attemptedLineIds = new ArrayList<>();
        AtomicInteger routeFallbackCalls = new AtomicInteger();

        SchedulePlanner.ProcessLineCandidate selected = planner.selectBestProcessLineCandidate(
                computation, routeProcess(200L), 99L,
                lineId -> {
                    attemptedLineIds.add(lineId);
                    return lineCandidate(lineId, LocalDateTime.of(2026, 5, 15, 10, 0));
                },
                () -> {
                    routeFallbackCalls.incrementAndGet();
                    return routeCandidate(LocalDateTime.of(2026, 5, 15, 9, 0));
                });

        assertEquals(99L, selected.lineId);
        assertEquals(List.of(99L), attemptedLineIds);
        assertEquals(0, routeFallbackCalls.get());
        assertEquals(List.of(), computation.issues);
    }

    @Test
    void selectBestProcessLineCandidate_shouldPickEarliestEndTimeThenLowestLineId() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = candidateComputation(200L, 12L, 11L, 11L);

        SchedulePlanner.ProcessLineCandidate selected = planner.selectBestProcessLineCandidate(
                computation, routeProcess(200L), null,
                lineId -> lineCandidate(lineId, LocalDateTime.of(2026, 5, 15, 10, 0)),
                () -> routeCandidate(LocalDateTime.of(2026, 5, 15, 8, 0)));

        assertEquals(11L, selected.lineId);
        assertEquals(List.of(), computation.issues);
    }

    @Test
    void selectBestProcessLineCandidate_shouldFallbackToRouteProcessWhenAllLinesFail() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = candidateComputation(200L, 11L, 12L);
        AtomicInteger routeFallbackCalls = new AtomicInteger();

        SchedulePlanner.ProcessLineCandidate selected = planner.selectBestProcessLineCandidate(
                computation, routeProcess(200L), null,
                lineId -> SchedulePlanner.ProcessLineCandidate.failed(
                        SchedulePlanner.ScheduleIssueDraft.blocking("LINE", 1L, 2L, null, null,
                                "line " + lineId + " failure")),
                () -> {
                    routeFallbackCalls.incrementAndGet();
                    return routeCandidate(LocalDateTime.of(2026, 5, 15, 9, 0));
                });

        assertEquals(null, selected.lineId);
        assertEquals(1, routeFallbackCalls.get());
        assertEquals(List.of(), computation.issues);
    }

    @Test
    void selectBestProcessLineCandidate_shouldKeepFirstLineFailureWhenFallbackAlsoFails() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = candidateComputation(200L, 11L, 12L);

        SchedulePlanner.ProcessLineCandidate selected = planner.selectBestProcessLineCandidate(
                computation, routeProcess(200L), null,
                lineId -> SchedulePlanner.ProcessLineCandidate.failed(
                        SchedulePlanner.ScheduleIssueDraft.blocking("LINE", 1L, 2L, null, null,
                                "line " + lineId + " failure")),
                () -> SchedulePlanner.ProcessLineCandidate.failed(
                        SchedulePlanner.ScheduleIssueDraft.blocking("CAPACITY", 1L, 2L, null, null,
                                "route process failure")));

        assertEquals(null, selected);
        assertEquals(1, computation.issues.size());
        assertEquals("line 11 failure", computation.issues.get(0).message);
    }

    @Test
    void calculateLatestStartTime_shouldConsumeLineWindowsBackwardFromPromiseDate() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = latestStartComputation();
        MesProScheduleOrderDO scheduleOrder = computation.scheduleOrders.get(0);
        SchedulePlanner.PreviewStep step = latestStartStep(
                LocalDateTime.of(2026, 5, 16, 9, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));

        LocalDateTime latestStartTime = planner.calculateLatestStartTime(
                computation, scheduleOrder, List.of(step), new CapacityWindowAllocator(), (date, process) -> "DAY");

        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), latestStartTime);
    }

    @Test
    void violatesLatestStartConstraint_shouldDetectPlannedStartAfterLatestStart() {
        SchedulePlanner planner = new SchedulePlanner();
        SchedulePlanner.ScheduleComputation computation = latestStartComputation();
        MesProScheduleOrderDO scheduleOrder = computation.scheduleOrders.get(0);
        SchedulePlanner.PreviewStep step = latestStartStep(
                LocalDateTime.of(2026, 5, 16, 9, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));

        boolean violates = planner.violatesLatestStartConstraint(
                computation, scheduleOrder, List.of(step), new CapacityWindowAllocator(), (date, process) -> "DAY");

        assertEquals(true, violates);
    }

    @Test
    void allocateFiniteProcessPlans_shouldSplitAcrossWindowsAndBalanceQuantityOnLastSegment() {
        SchedulePlanner planner = new SchedulePlanner();
        List<CapacityWindowAllocator.ShiftWindow> windows = List.of(
                window(60L, 70L, "DAY-A", LocalDateTime.of(2026, 5, 15, 8, 0),
                        LocalDateTime.of(2026, 5, 15, 12, 0)),
                window(60L, 71L, "DAY-B", LocalDateTime.of(2026, 5, 15, 13, 0),
                        LocalDateTime.of(2026, 5, 15, 19, 0)));

        List<SchedulePlanner.PlannedTask> plans = planner.allocateFiniteProcessPlans(
                LocalDateTime.of(2026, 5, 15, 8, 0), 600, windows,
                planningWorkOrder(), routeProduct(), routeProcess(200L), planningPool(),
                60L, "一号线", scheduleProcess(30L, false), new BigDecimal("101"));

        assertEquals(2, plans.size());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), plans.get(0).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 15, 12, 0), plans.get(0).endTime);
        assertEquals(new BigDecimal("40"), plans.get(0).quantity);
        assertEquals(1, plans.get(0).segmentIndex);
        assertEquals(LocalDateTime.of(2026, 5, 15, 13, 0), plans.get(1).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 15, 19, 0), plans.get(1).endTime);
        assertEquals(new BigDecimal("61"), plans.get(1).quantity);
        assertEquals(2, plans.get(1).segmentIndex);
        assertEquals(100L, plans.get(0).workOrderId);
        assertEquals(500L, plans.get(0).routeId);
        assertEquals(300L, plans.get(0).processId);
        assertEquals(30L, plans.get(0).scheduleOrderProcessId);
        assertEquals(50L, plans.get(0).workstationId);
        assertEquals(60L, plans.get(0).lineId);
        assertEquals("一号线", plans.get(0).lineName);
        assertEquals("BLUE", plans.get(0).colorCode);
    }

    @Test
    void allocateFiniteProcessPlans_shouldReturnEmptyWhenWindowsCannotCoverRequiredMinutes() {
        SchedulePlanner planner = new SchedulePlanner();

        List<SchedulePlanner.PlannedTask> plans = planner.allocateFiniteProcessPlans(
                LocalDateTime.of(2026, 5, 15, 8, 0), 600,
                List.of(window(60L, 70L, "DAY", LocalDateTime.of(2026, 5, 15, 8, 0),
                        LocalDateTime.of(2026, 5, 15, 12, 0))),
                planningWorkOrder(), routeProduct(), routeProcess(200L), planningPool(),
                60L, "一号线", scheduleProcess(30L, false), new BigDecimal("100"));

        assertEquals(List.of(), plans);
    }

    @Test
    void allocateInfiniteProcessPlans_shouldUseInfiniteWindowForNonNightProcess() {
        SchedulePlanner planner = new SchedulePlanner();

        List<SchedulePlanner.PlannedTask> plans = planner.allocateInfiniteProcessPlans(
                LocalDateTime.of(2026, 5, 15, 9, 0), 90,
                List.of(window(60L, 70L, "DAY", LocalDateTime.of(2026, 5, 15, 8, 0),
                        LocalDateTime.of(2026, 5, 15, 16, 0))),
                planningWorkOrder(), routeProduct(), routeProcess(200L), planningPool(),
                60L, "一号线", scheduleProcess(30L, false), new BigDecimal("10"),
                new CapacityWindowAllocator());

        assertEquals(1, plans.size());
        assertEquals(LocalDateTime.of(2026, 5, 15, 9, 0), plans.get(0).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 15, 10, 30), plans.get(0).endTime);
        assertEquals(90, plans.get(0).plannedDurationMinutes);
        assertEquals(new BigDecimal("10"), plans.get(0).quantity);
        assertEquals(1, plans.get(0).segmentIndex);
    }

    @Test
    void allocateInfiniteProcessPlans_shouldSplitWhenDailyCapacityWouldBeExceeded() {
        SchedulePlanner planner = new SchedulePlanner();

        List<SchedulePlanner.PlannedTask> plans = planner.allocateInfiniteProcessPlans(
                LocalDateTime.of(2026, 5, 15, 8, 0), 480,
                List.of(
                        window(60L, 70L, "DAY", LocalDateTime.of(2026, 5, 15, 8, 0),
                                LocalDateTime.of(2026, 5, 15, 16, 0)),
                        window(60L, 71L, "DAY", LocalDateTime.of(2026, 5, 16, 8, 0),
                                LocalDateTime.of(2026, 5, 16, 16, 0))),
                planningWorkOrder(), routeProduct(), routeProcess(200L), planningPool(),
                60L, "一号线", scheduleProcess(30L, false, "100"), new BigDecimal("120"),
                new CapacityWindowAllocator());

        assertEquals(2, plans.size());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), plans.get(0).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 15, 14, 40), plans.get(0).endTime);
        assertEquals(new BigDecimal("100"), plans.get(0).quantity);
        assertEquals(LocalDateTime.of(2026, 5, 16, 8, 0), plans.get(1).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 16, 9, 20), plans.get(1).endTime);
        assertEquals(new BigDecimal("20"), plans.get(1).quantity);
    }

    @Test
    void allocateFiniteProcessPlans_shouldRespectExistingDailyCapacityReservation() {
        SchedulePlanner planner = new SchedulePlanner();
        MesProScheduleOrderProcessDO scheduleProcess = scheduleProcess(30L, false, "100");
        SchedulePlanner.DailyProcessCapacityLedger ledger = new SchedulePlanner.DailyProcessCapacityLedger();
        ledger.reserve(60L, 300L, LocalDate.of(2026, 5, 15), scheduleProcess, new BigDecimal("80"));

        List<SchedulePlanner.PlannedTask> plans = planner.allocateFiniteProcessPlans(
                LocalDateTime.of(2026, 5, 15, 8, 0), 300,
                List.of(
                        window(60L, 70L, "DAY", LocalDateTime.of(2026, 5, 15, 8, 0),
                                LocalDateTime.of(2026, 5, 15, 16, 0)),
                        window(60L, 71L, "DAY", LocalDateTime.of(2026, 5, 16, 8, 0),
                                LocalDateTime.of(2026, 5, 16, 16, 0))),
                planningWorkOrder(), routeProduct(), routeProcess(200L), planningPool(),
                60L, "一号线", scheduleProcess, new BigDecimal("50"), ledger);

        assertEquals(2, plans.size());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), plans.get(0).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 15, 10, 0), plans.get(0).endTime);
        assertEquals(new BigDecimal("20"), plans.get(0).quantity);
        assertEquals(LocalDateTime.of(2026, 5, 16, 8, 0), plans.get(1).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 16, 11, 0), plans.get(1).endTime);
        assertEquals(new BigDecimal("30"), plans.get(1).quantity);
    }

    @Test
    void allocateFiniteProcessPlans_shouldReturnEmptyWhenDailyCapacityIsExhausted() {
        SchedulePlanner planner = new SchedulePlanner();
        MesProScheduleOrderProcessDO scheduleProcess = scheduleProcess(30L, false, "100");
        SchedulePlanner.DailyProcessCapacityLedger ledger = new SchedulePlanner.DailyProcessCapacityLedger();
        ledger.reserve(60L, 300L, LocalDate.of(2026, 5, 15), scheduleProcess, new BigDecimal("100"));
        ledger.reserve(60L, 300L, LocalDate.of(2026, 5, 16), scheduleProcess, new BigDecimal("100"));

        List<SchedulePlanner.PlannedTask> plans = planner.allocateFiniteProcessPlans(
                LocalDateTime.of(2026, 5, 15, 8, 0), 60,
                List.of(
                        window(60L, 70L, "DAY", LocalDateTime.of(2026, 5, 15, 8, 0),
                                LocalDateTime.of(2026, 5, 15, 16, 0)),
                        window(60L, 71L, "DAY", LocalDateTime.of(2026, 5, 16, 8, 0),
                                LocalDateTime.of(2026, 5, 16, 16, 0))),
                planningWorkOrder(), routeProduct(), routeProcess(200L), planningPool(),
                60L, "一号线", scheduleProcess, new BigDecimal("10"), ledger);

        assertEquals(List.of(), plans);
    }

    @Test
    void allocateInfiniteProcessPlans_shouldUseFiniteSegmentationForNightProcess() {
        SchedulePlanner planner = new SchedulePlanner();
        List<CapacityWindowAllocator.ShiftWindow> windows = List.of(
                window(60L, 80L, "NIGHT-A", LocalDateTime.of(2026, 5, 15, 20, 0),
                        LocalDateTime.of(2026, 5, 16, 0, 0)),
                window(60L, 81L, "NIGHT-B", LocalDateTime.of(2026, 5, 16, 1, 0),
                        LocalDateTime.of(2026, 5, 16, 7, 0)));

        List<SchedulePlanner.PlannedTask> plans = planner.allocateInfiniteProcessPlans(
                LocalDateTime.of(2026, 5, 15, 20, 0), 600, windows,
                planningWorkOrder(), routeProduct(), routeProcess(200L), planningPool(),
                60L, "夜班线", scheduleProcess(30L, true), new BigDecimal("10"),
                new CapacityWindowAllocator());

        assertEquals(2, plans.size());
        assertEquals(LocalDateTime.of(2026, 5, 15, 20, 0), plans.get(0).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 16, 0, 0), plans.get(0).endTime);
        assertEquals(new BigDecimal("4"), plans.get(0).quantity);
        assertEquals(1, plans.get(0).segmentIndex);
        assertEquals(LocalDateTime.of(2026, 5, 16, 1, 0), plans.get(1).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 16, 7, 0), plans.get(1).endTime);
        assertEquals(new BigDecimal("6"), plans.get(1).quantity);
        assertEquals(2, plans.get(1).segmentIndex);
    }

    private boolean isPersistenceOrInjectedDependency(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && (field.isAnnotationPresent(Resource.class)
                || field.getType().getSimpleName().endsWith("Mapper")
                || field.getType().getSimpleName().endsWith("Service"));
    }

    private SchedulePlanner.ScheduleComputation latestStartComputation() {
        SchedulePlanner.ScheduleComputation computation = new SchedulePlanner.ScheduleComputation();
        computation.workstationMap.put(50L, MesMdWorkstationDO.builder()
                .id(50L)
                .productionLineId(60L)
                .build());
        computation.scheduleOrders.add(MesProScheduleOrderDO.builder()
                .id(20L)
                .workOrderId(10L)
                .promiseDate(LocalDate.of(2026, 5, 15))
                .build());
        computation.scheduleOrderProcessesByOrderId.put(20L, List.of(MesProScheduleOrderProcessDO.builder()
                .id(30L)
                .processId(40L)
                .nightShiftEnabled(Boolean.FALSE)
                .build()));
        computation.shiftWindowsByLineId.put(60L, List.of(new CapacityWindowAllocator.ShiftWindow(
                60L, 70L, shift(70L, "DAY", "08:00", "16:00"),
                LocalDate.of(2026, 5, 15),
                LocalDateTime.of(2026, 5, 15, 8, 0),
                LocalDateTime.of(2026, 5, 15, 16, 0))));
        return computation;
    }

    private SchedulePlanner.PreviewStep latestStartStep(LocalDateTime startTime, LocalDateTime endTime) {
        SchedulePlanner.PreviewStep step = new SchedulePlanner.PreviewStep();
        step.workOrderId = 10L;
        step.processId = 40L;
        step.workstationId = 50L;
        step.startTime = startTime;
        step.endTime = endTime;
        return step;
    }

    private MesProScheduleOrderDO scheduleOrder(Long id, Long workOrderId, LocalDate promiseDate, Integer priorityNo) {
        return MesProScheduleOrderDO.builder()
                .id(id)
                .workOrderId(workOrderId)
                .promiseDate(promiseDate)
                .priorityNo(priorityNo)
                .build();
    }

    private MesProScheduleOrderProcessDO scheduleProcess(boolean nightShiftEnabled) {
        return MesProScheduleOrderProcessDO.builder()
                .nightShiftEnabled(nightShiftEnabled)
                .build();
    }

    private MesProScheduleOrderProcessDO scheduleProcess(Long id, boolean nightShiftEnabled) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id)
                .processId(300L)
                .nightShiftEnabled(nightShiftEnabled)
                .build();
    }

    private MesProScheduleOrderProcessDO scheduleProcess(Long id, boolean nightShiftEnabled, String shiftCapacity) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id)
                .processId(300L)
                .shiftCapacityTotal(new BigDecimal(shiftCapacity))
                .nightShiftEnabled(nightShiftEnabled)
                .build();
    }

    private MesProWorkOrderDO planningWorkOrder() {
        return MesProWorkOrderDO.builder()
                .id(100L)
                .productId(400L)
                .clientId(900L)
                .quantity(new BigDecimal("101"))
                .build();
    }

    private MesProRouteProductDO routeProduct() {
        return MesProRouteProductDO.builder()
                .routeId(500L)
                .build();
    }

    private SchedulePlanner.ProcessResourcePool planningPool() {
        SchedulePlanner.ProcessResourcePool pool = new SchedulePlanner.ProcessResourcePool(60L, 300L, "组装");
        pool.primaryWorkstationId = 50L;
        return pool;
    }

    private SchedulePlanner.ProcessResourcePool routeProcessPool(Long processId, String processName,
                                                                 BigDecimal effectiveHourlyCapacity) {
        SchedulePlanner.ProcessResourcePool pool =
                new SchedulePlanner.ProcessResourcePool(null, processId, processName);
        pool.effectiveHourlyCapacity = effectiveHourlyCapacity;
        return pool;
    }

    private CapacityWindowAllocator.ShiftWindow window(Long lineId, Long shiftId, String shiftName,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        return new CapacityWindowAllocator.ShiftWindow(lineId, shiftId,
                shift(shiftId, shiftName, startTime.toLocalTime().toString(), endTime.toLocalTime().toString()),
                startTime.toLocalDate(), startTime, endTime);
    }

    private SchedulePlanner.ScheduleComputation candidateComputation(Long routeProcessId, Long... lineIds) {
        SchedulePlanner.ScheduleComputation computation = new SchedulePlanner.ScheduleComputation();
        List<MesMdWorkstationDO> workstations = Arrays.stream(lineIds)
                .map(lineId -> MesMdWorkstationDO.builder()
                        .id(lineId + 1000)
                        .productionLineId(lineId)
                        .build())
                .toList();
        computation.workstationsByRouteProcessId.put(routeProcessId, workstations);
        return computation;
    }

    private MesProRouteProcessDO routeProcess(Long routeProcessId) {
        return MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .processId(300L)
                .colorCode("BLUE")
                .build();
    }

    private SchedulePlanner.ProcessLineCandidate lineCandidate(Long lineId, LocalDateTime endTime) {
        SchedulePlanner.PlannedTask plan = new SchedulePlanner.PlannedTask();
        plan.lineId = lineId;
        plan.endTime = endTime;
        return SchedulePlanner.ProcessLineCandidate.success(lineId, "LINE-" + lineId, "产线" + lineId,
                null, List.of(plan), 60);
    }

    private SchedulePlanner.ProcessLineCandidate routeCandidate(LocalDateTime endTime) {
        SchedulePlanner.PlannedTask plan = new SchedulePlanner.PlannedTask();
        plan.endTime = endTime;
        return SchedulePlanner.ProcessLineCandidate.success(null, "ROUTE", "工序资源",
                "ROUTE_PROCESS:200", null, List.of(plan), 60);
    }

    private MesCalPlanShiftDO shift(Long id, String name, String startTime, String endTime) {
        return MesCalPlanShiftDO.builder()
                .id(id)
                .name(name)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    private static final class MesProWorkOrderTestData {

        private static cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO workOrder(
                Long id, String code) {
            return cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder()
                    .id(id)
                    .code(code)
                    .build();
        }
    }

}
