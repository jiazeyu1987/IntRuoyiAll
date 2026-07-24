package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityActualDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CapacityWindowAllocator {

    private static final String CAPACITY_MODE_ACTUAL = "ACTUAL";
    private static final int ROUTE_PROCESS_WINDOW_SEARCH_DAY_LIMIT = 3660;

    public Map<Long, List<ShiftWindow>> buildShiftWindows(String capacityMode,
                                                          Map<Long, MesMdProductionLineDO> lineMap,
                                                          Map<Long, MesCalPlanShiftDO> shiftMap,
                                                          Map<Long, MesCalPlanDO> planMap,
                                                          List<MesProCapacityPlanDO> capacityPlanList,
                                                          List<MesProCapacityActualDO> capacityActualList) {
        Map<Long, List<ShiftWindow>> windowsByLineId = new LinkedHashMap<>();
        if (CAPACITY_MODE_ACTUAL.equals(capacityMode)) {
            safeList(capacityActualList).forEach(capacity -> addShiftWindow(lineMap, shiftMap, planMap, windowsByLineId,
                    capacity.getLineId(), capacity.getCalendarDate(), capacity.getShiftId(), capacity.getCapacityMinutes()));
        } else {
            safeList(capacityPlanList).forEach(capacity -> addShiftWindow(lineMap, shiftMap, planMap, windowsByLineId,
                    capacity.getLineId(), capacity.getCalendarDate(), capacity.getShiftId(), capacity.getCapacityMinutes()));
        }
        windowsByLineId.values().forEach(list -> list.sort(shiftWindowComparator()));
        return windowsByLineId;
    }

    public List<ShiftWindow> buildRouteProcessShiftWindows(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                           LocalDateTime availableFrom,
                                                           int requiredMinutes,
                                                           Function<LocalDate, String> dateShiftModeResolver) {
        if (availableFrom == null || requiredMinutes <= 0) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(dateShiftModeResolver, "dateShiftModeResolver is required");
        int windowMinutes = resolveRouteProcessDailyWindowMinutes(scheduleOrderProcess);
        if (windowMinutes <= 0) {
            return Collections.emptyList();
        }
        boolean nightShiftEnabled = scheduleOrderProcess != null && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled());
        MesCalPlanShiftDO routeProcessShift = buildRouteProcessShift(scheduleOrderProcess, windowMinutes);
        List<ShiftWindow> windows = new ArrayList<>();
        LocalDate startDate = availableFrom.toLocalDate();
        int remainingMinutes = requiredMinutes;
        for (int dayOffset = 0;
             (remainingMinutes > 0 || windows.isEmpty()) && dayOffset < ROUTE_PROCESS_WINDOW_SEARCH_DAY_LIMIT;
             dayOffset++) {
            LocalDate date = startDate.plusDays(dayOffset);
            if (!isWindowAllowedByCalendarMode(dateShiftModeResolver.apply(date), routeProcessShift, nightShiftEnabled)) {
                continue;
            }
            LocalDateTime windowStart = buildShiftDateTime(date, routeProcessShift.getStartTime());
            LocalDateTime windowEnd = windowStart.plusMinutes(windowMinutes);
            if (!windowEnd.isAfter(windowStart) || !windowEnd.isAfter(availableFrom)) {
                continue;
            }
            windows.add(new ShiftWindow(null, null, routeProcessShift, date, windowStart, windowEnd));
            LocalDateTime segmentStart = windowStart.isAfter(availableFrom) ? windowStart : availableFrom;
            remainingMinutes -= Math.max(0, (int) Duration.between(segmentStart, windowEnd).toMinutes());
        }
        if (remainingMinutes > 0) {
            return Collections.emptyList();
        }
        windows.sort(shiftWindowComparator());
        return windows;
    }

    public int resolveRouteProcessDailyWindowMinutes(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess != null && scheduleOrderProcess.getShiftHours() != null) {
            return Math.max(scheduleOrderProcess.getShiftHours()
                    .multiply(BigDecimal.valueOf(60))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue(), 0);
        }
        return 0;
    }

    public List<ShiftWindow> filterWindowsForScheduleProcess(List<ShiftWindow> windows,
                                                             MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                             Function<LocalDate, String> dateShiftModeResolver) {
        boolean nightShiftEnabled = scheduleOrderProcess != null && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled());
        return safeList(windows).stream()
                .filter(window -> isWindowAllowedByCalendarMode(
                        dateShiftModeResolver.apply(window.calendarDate), window.shift, nightShiftEnabled))
                .sorted(shiftWindowComparator())
                .toList();
    }

    public List<ShiftWindow> appendUnboundNightWindowsFromLineCapacity(Long lineId,
                                                                       List<ShiftWindow> windows,
                                                                       MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                                       Map<Long, MesMdProductionLineDO> lineMap,
                                                                       Map<Long, List<MesCalPlanShiftDO>> shiftListByPlanId) {
        if (lineId == null
                || CollUtil.isEmpty(windows)
                || scheduleOrderProcess == null
                || !Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled())) {
            return windows;
        }
        MesMdProductionLineDO line = lineMap.get(lineId);
        Long planId = line == null ? null : line.getCalendarPlanId();
        List<MesCalPlanShiftDO> nightShifts = planId == null
                ? Collections.emptyList()
                : safeList(shiftListByPlanId.get(planId)).stream()
                .filter(this::isNightShift)
                .toList();
        if (CollUtil.isEmpty(nightShifts)) {
            return windows;
        }
        Set<String> existingKeys = windows.stream()
                .map(window -> buildShiftCapacityKey(window.calendarDate, window.shiftId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<LocalDate, Integer> availableMinutesByDate = new LinkedHashMap<>();
        for (ShiftWindow window : windows) {
            if (window.calendarDate == null || isNightShift(window.shift)) {
                continue;
            }
            int minutes = Math.toIntExact(Math.max(0, Duration.between(window.startTime, window.usableEnd).toMinutes()));
            availableMinutesByDate.merge(window.calendarDate, minutes, Math::max);
        }
        if (availableMinutesByDate.isEmpty()) {
            return windows;
        }
        List<ShiftWindow> result = new ArrayList<>(windows);
        for (Map.Entry<LocalDate, Integer> entry : availableMinutesByDate.entrySet()) {
            for (MesCalPlanShiftDO nightShift : nightShifts) {
                String key = buildShiftCapacityKey(entry.getKey(), nightShift.getId());
                if (existingKeys.contains(key)) {
                    continue;
                }
                LocalDateTime shiftStart = buildShiftDateTime(entry.getKey(), nightShift.getStartTime());
                LocalDateTime shiftEnd = buildShiftDateTime(entry.getKey(), nightShift.getEndTime());
                if (!shiftEnd.isAfter(shiftStart)) {
                    shiftEnd = shiftEnd.plusDays(1);
                }
                LocalDateTime usableEnd = shiftStart.plusMinutes(entry.getValue());
                if (usableEnd.isAfter(shiftEnd)) {
                    usableEnd = shiftEnd;
                }
                if (!usableEnd.isAfter(shiftStart)) {
                    continue;
                }
                result.add(new ShiftWindow(lineId, nightShift.getId(), nightShift, entry.getKey(), shiftStart, usableEnd));
                existingKeys.add(key);
            }
        }
        result.sort(shiftWindowComparator());
        return result;
    }

    public ScheduleWindowResult allocateInfiniteWindow(LocalDateTime availableFrom, int requiredMinutes, List<ShiftWindow> windows) {
        for (ShiftWindow window : safeList(windows)) {
            if (!window.usableEnd.isAfter(availableFrom)) {
                continue;
            }
            LocalDateTime startTime = window.startTime.isAfter(availableFrom) ? window.startTime : availableFrom;
            return new ScheduleWindowResult(startTime, startTime.plusMinutes(requiredMinutes), requiredMinutes);
        }
        return null;
    }

    public LocalDateTime consumeBackwardWindows(LocalDateTime latestEndTime, int requiredMinutes, List<ShiftWindow> windows) {
        int remainingMinutes = requiredMinutes;
        LocalDateTime cursor = latestEndTime;
        List<ShiftWindow> orderedWindows = safeList(windows).stream()
                .filter(window -> window.startTime.isBefore(latestEndTime))
                .sorted(Comparator.comparing((ShiftWindow window) -> window.startTime).reversed())
                .toList();
        for (ShiftWindow window : orderedWindows) {
            LocalDateTime segmentEnd = window.usableEnd.isBefore(cursor) ? window.usableEnd : cursor;
            if (!segmentEnd.isAfter(window.startTime)) {
                continue;
            }
            long usableMinutes = Duration.between(window.startTime, segmentEnd).toMinutes();
            if (usableMinutes >= remainingMinutes) {
                return segmentEnd.minusMinutes(remainingMinutes);
            }
            remainingMinutes -= (int) usableMinutes;
            cursor = window.startTime;
        }
        return cursor.minusMinutes(remainingMinutes);
    }

    public boolean isWindowAllowedByCalendarMode(String dateShiftMode, MesCalPlanShiftDO shift,
                                                 boolean nightShiftEnabled) {
        if (MesProScheduleCalendarRuleSupport.DATE_SHIFT_REST.equals(dateShiftMode)) {
            return false;
        }
        return nightShiftEnabled == isNightShift(shift);
    }

    public boolean isNightShift(MesCalPlanShiftDO shift) {
        return MesProScheduleCalendarRuleSupport.DATE_SHIFT_NIGHT.equals(
                MesProScheduleCalendarRuleSupport.resolveShiftCode(shift));
    }

    public String buildShiftCapacityKey(LocalDate date, Long shiftId) {
        return (date == null ? "" : date.toString()) + "|" + (shiftId == null ? "" : shiftId);
    }

    public LocalDateTime buildShiftDateTime(LocalDate date, String hhmm) {
        LocalTime time = LocalTime.parse(ObjUtil.defaultIfBlank(hhmm, "00:00"));
        return LocalDateTime.of(date, time);
    }

    public Comparator<ShiftWindow> shiftWindowComparator() {
        return Comparator
                .comparing((ShiftWindow window) -> window.startTime, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(window -> window.usableEnd, Comparator.nullsLast(LocalDateTime::compareTo));
    }

    private void addShiftWindow(Map<Long, MesMdProductionLineDO> lineMap,
                                Map<Long, MesCalPlanShiftDO> shiftMap,
                                Map<Long, MesCalPlanDO> planMap,
                                Map<Long, List<ShiftWindow>> windowsByLineId,
                                Long lineId,
                                LocalDateTime calendarDate,
                                Long shiftId,
                                Integer capacityMinutes) {
        if (lineId == null || calendarDate == null || shiftId == null || capacityMinutes == null || capacityMinutes <= 0) {
            return;
        }
        MesMdProductionLineDO line = lineMap.get(lineId);
        MesCalPlanShiftDO shift = shiftMap.get(shiftId);
        MesCalPlanDO plan = line == null ? null : planMap.get(line.getCalendarPlanId());
        if (line == null || shift == null || plan == null) {
            return;
        }
        LocalDate date = calendarDate.toLocalDate();
        if (plan.getStartDate() != null && date.isBefore(plan.getStartDate().toLocalDate())) {
            return;
        }
        // Auto scheduling uses generated capacity rows as the horizon; the plan end date must not truncate future rows.
        LocalDateTime shiftStart = buildShiftDateTime(date, shift.getStartTime());
        LocalDateTime shiftEnd = buildShiftDateTime(date, shift.getEndTime());
        if (!shiftEnd.isAfter(shiftStart)) {
            shiftEnd = shiftEnd.plusDays(1);
        }
        LocalDateTime usableEnd = shiftStart.plusMinutes(capacityMinutes);
        if (usableEnd.isAfter(shiftEnd)) {
            usableEnd = shiftEnd;
        }
        if (!usableEnd.isAfter(shiftStart)) {
            return;
        }
        windowsByLineId.computeIfAbsent(lineId, key -> new ArrayList<>())
                .add(new ShiftWindow(lineId, shiftId, shift, date, shiftStart, usableEnd));
    }

    private MesCalPlanShiftDO buildRouteProcessShift(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                     int windowMinutes) {
        boolean nightShiftEnabled = scheduleOrderProcess != null && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled());
        LocalTime startTime = nightShiftEnabled ? LocalTime.of(20, 0) : LocalTime.of(8, 0);
        LocalTime endTime = startTime.plusMinutes(windowMinutes);
        return MesCalPlanShiftDO.builder()
                .id(null)
                .name(nightShiftEnabled ? "NIGHT" : "DAY")
                .startTime(startTime.toString())
                .endTime(endTime.toString())
                .build();
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    public static final class ShiftWindow {
        public final Long lineId;
        public final Long shiftId;
        public final MesCalPlanShiftDO shift;
        public final LocalDate calendarDate;
        public final LocalDateTime startTime;
        public final LocalDateTime usableEnd;

        public ShiftWindow(Long lineId, Long shiftId, MesCalPlanShiftDO shift, LocalDate calendarDate,
                           LocalDateTime startTime, LocalDateTime usableEnd) {
            this.lineId = lineId;
            this.shiftId = shiftId;
            this.shift = shift;
            this.calendarDate = calendarDate;
            this.startTime = startTime;
            this.usableEnd = usableEnd;
        }
    }

    public static final class ScheduleWindowResult {
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;
        public final int minutes;

        public ScheduleWindowResult(LocalDateTime startTime, LocalDateTime endTime, int minutes) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.minutes = minutes;
        }
    }

}
