package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityActualDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacityWindowAllocatorTest {

    private final CapacityWindowAllocator allocator = new CapacityWindowAllocator();

    @Test
    void buildShiftWindows_shouldUseActualCapacityAndClampUsableEndToShiftEnd() {
        MesMdProductionLineDO line = MesMdProductionLineDO.builder().id(10L).calendarPlanId(20L).build();
        MesCalPlanDO plan = MesCalPlanDO.builder()
                .id(20L)
                .startDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .endDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        MesCalPlanShiftDO shift = shift(30L, "DAY", "08:00", "12:00");
        MesProCapacityPlanDO planned = MesProCapacityPlanDO.builder()
                .lineId(10L)
                .calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .shiftId(30L)
                .capacityMinutes(240)
                .build();
        MesProCapacityActualDO actual = MesProCapacityActualDO.builder()
                .lineId(10L)
                .calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .shiftId(30L)
                .capacityMinutes(600)
                .build();

        Map<Long, List<CapacityWindowAllocator.ShiftWindow>> result = allocator.buildShiftWindows(
                "ACTUAL", Map.of(10L, line), Map.of(30L, shift), Map.of(20L, plan), List.of(planned), List.of(actual));

        assertEquals(1, result.get(10L).size());
        CapacityWindowAllocator.ShiftWindow window = result.get(10L).get(0);
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), window.startTime);
        assertEquals(LocalDateTime.of(2026, 5, 13, 12, 0), window.usableEnd);
    }

    @Test
    void buildShiftWindows_shouldNotTreatPlanEndDateAsAutoScheduleHorizon() {
        MesMdProductionLineDO line = MesMdProductionLineDO.builder().id(10L).calendarPlanId(20L).build();
        MesCalPlanDO plan = MesCalPlanDO.builder()
                .id(20L)
                .startDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .endDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        MesCalPlanShiftDO shift = shift(30L, "DAY", "08:00", "12:00");
        MesProCapacityPlanDO futureCapacity = MesProCapacityPlanDO.builder()
                .lineId(10L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(30L)
                .capacityMinutes(240)
                .build();

        Map<Long, List<CapacityWindowAllocator.ShiftWindow>> result = allocator.buildShiftWindows(
                "PLANNED", Map.of(10L, line), Map.of(30L, shift), Map.of(20L, plan), List.of(futureCapacity), List.of());

        assertEquals(1, result.get(10L).size());
        assertEquals(LocalDate.of(2026, 5, 14), result.get(10L).get(0).calendarDate);
    }

    @Test
    void filterWindowsForScheduleProcess_shouldExcludeRestAndSeparateDayNightWindows() {
        List<CapacityWindowAllocator.ShiftWindow> windows = List.of(
                new CapacityWindowAllocator.ShiftWindow(10L, 30L, shift(30L, "DAY", "08:00", "16:00"),
                        LocalDate.of(2026, 5, 13), LocalDateTime.of(2026, 5, 13, 8, 0),
                        LocalDateTime.of(2026, 5, 13, 16, 0)),
                new CapacityWindowAllocator.ShiftWindow(10L, 31L, shift(31L, "NIGHT", "20:00", "04:00"),
                        LocalDate.of(2026, 5, 13), LocalDateTime.of(2026, 5, 13, 20, 0),
                        LocalDateTime.of(2026, 5, 14, 4, 0)));
        MesProScheduleOrderProcessDO dayProcess = process(false);
        MesProScheduleOrderProcessDO nightProcess = process(true);

        List<CapacityWindowAllocator.ShiftWindow> dayWindows = allocator.filterWindowsForScheduleProcess(
                windows, dayProcess, date -> "DAY");
        List<CapacityWindowAllocator.ShiftWindow> nightWindows = allocator.filterWindowsForScheduleProcess(
                windows, nightProcess, date -> "DAY");
        List<CapacityWindowAllocator.ShiftWindow> restWindows = allocator.filterWindowsForScheduleProcess(
                windows, nightProcess, date -> "REST");

        assertEquals(List.of(30L), dayWindows.stream().map(window -> window.shiftId).toList());
        assertEquals(List.of(31L), nightWindows.stream().map(window -> window.shiftId).toList());
        assertTrue(restWindows.isEmpty());
    }

    @Test
    void buildRouteProcessShiftWindows_shouldUseConfiguredShiftHoursWithoutDefaulting() {
        MesProScheduleOrderProcessDO nightProcess = process(true);
        nightProcess.setShiftHours(new BigDecimal("2.0"));
        MesProScheduleOrderProcessDO missingHoursProcess = process(false);

        List<CapacityWindowAllocator.ShiftWindow> windows = allocator.buildRouteProcessShiftWindows(
                nightProcess, LocalDateTime.of(2026, 5, 13, 19, 0), 180,
                date -> MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY);
        List<CapacityWindowAllocator.ShiftWindow> missingWindows = allocator.buildRouteProcessShiftWindows(
                missingHoursProcess, LocalDateTime.of(2026, 5, 13, 8, 0), 60,
                date -> MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY);

        assertEquals(2, windows.size());
        assertEquals(LocalDateTime.of(2026, 5, 13, 20, 0), windows.get(0).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 13, 22, 0), windows.get(0).usableEnd);
        assertTrue(missingWindows.isEmpty());
    }

    @Test
    void buildRouteProcessShiftWindows_shouldSkipRestDatesBeforeConsumingRequiredMinutes() {
        MesProScheduleOrderProcessDO dayProcess = process(false);
        dayProcess.setShiftHours(BigDecimal.ONE);

        List<CapacityWindowAllocator.ShiftWindow> windows = allocator.buildRouteProcessShiftWindows(
                dayProcess, LocalDateTime.of(2026, 5, 14, 8, 0), 60,
                date -> LocalDate.of(2026, 5, 14).equals(date)
                        ? MesProScheduleCalendarRuleSupport.DATE_SHIFT_REST
                        : MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY);

        assertEquals(1, windows.size());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), windows.get(0).startTime);
        assertEquals(LocalDateTime.of(2026, 5, 15, 9, 0), windows.get(0).usableEnd);
    }

    @Test
    void calculateShiftCapacityMinutes_shouldUseTheSameCrossDayRuleAsScheduling() {
        assertEquals(480, allocator.calculateShiftCapacityMinutes(
                shift(31L, "NIGHT", "20:00", "04:00")));
        assertEquals(480, allocator.calculateShiftCapacityMinutes(
                shift(30L, "DAY", "08:00", "16:00")));
    }

    private MesCalPlanShiftDO shift(Long id, String name, String startTime, String endTime) {
        return MesCalPlanShiftDO.builder()
                .id(id)
                .name(name)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    private MesProScheduleOrderProcessDO process(boolean nightShiftEnabled) {
        return MesProScheduleOrderProcessDO.builder()
                .nightShiftEnabled(nightShiftEnabled)
                .build();
    }

}
