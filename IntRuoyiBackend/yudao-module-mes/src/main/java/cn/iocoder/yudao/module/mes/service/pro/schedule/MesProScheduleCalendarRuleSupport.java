package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_CALENDAR_INVALID_DATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_CALENDAR_INVALID_WEEKEND_MODE;

final class MesProScheduleCalendarRuleSupport {

    static final String WEEKEND_MODE_DOUBLE = "DOUBLE";
    static final String WEEKEND_MODE_SINGLE = "SINGLE";
    static final String WEEKEND_MODE_NONE = "NONE";

    static final String DATE_SHIFT_REST = "REST";
    static final String DATE_SHIFT_DAY = "DAY";
    static final String DATE_SHIFT_NIGHT = "NIGHT";

    private static final Set<String> WEEKEND_MODES = Set.of(
            WEEKEND_MODE_DOUBLE, WEEKEND_MODE_SINGLE, WEEKEND_MODE_NONE);
    private static final Set<String> DATE_SHIFT_MODES = Set.of(
            DATE_SHIFT_REST, DATE_SHIFT_DAY);
    private static final TypeReference<Map<String, String>> DATE_SHIFT_MODE_MAP_TYPE = new TypeReference<>() {};

    private MesProScheduleCalendarRuleSupport() {
    }

    static String normalizeWeekendRestMode(String mode) {
        String normalized = String.valueOf(mode).trim().toUpperCase(Locale.ROOT);
        if (!WEEKEND_MODES.contains(normalized)) {
            throw exception(PRO_SCHEDULE_CALENDAR_INVALID_WEEKEND_MODE);
        }
        return normalized;
    }

    static Map<String, String> normalizeDateShiftModeByDate(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String dateText = String.valueOf(entry.getKey()).trim();
            if (!dateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw exception(PRO_SCHEDULE_CALENDAR_INVALID_DATE);
            }
            parseDate(dateText);
            String mode = String.valueOf(entry.getValue()).trim().toUpperCase(Locale.ROOT);
            if (!DATE_SHIFT_MODES.contains(mode)) {
                throw exception(PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE);
            }
            normalized.put(dateText, mode);
        }
        return normalized;
    }

    static Map<String, String> parseDateShiftModeByDate(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> parsed = JsonUtils.parseObject(json, DATE_SHIFT_MODE_MAP_TYPE);
        return normalizeDateShiftModeByDate(parsed);
    }

    static String resolveDateShiftMode(LocalDate date,
                                       MesProScheduleCalendarRulesRespVO rules,
                                       Set<String> holidayDateSet) {
        return resolveDateShiftMode(date,
                rules != null ? rules.getSkipStatutoryHolidays() : Boolean.FALSE,
                rules != null ? rules.getWeekendRestMode() : WEEKEND_MODE_SINGLE,
                rules != null ? rules.getDateShiftModeByDate() : Collections.emptyMap(),
                holidayDateSet);
    }

    static String resolveDateShiftMode(LocalDate date,
                                       Boolean skipStatutoryHolidays,
                                       String weekendRestMode,
                                       Map<String, String> dateShiftModeByDate,
                                       Set<String> holidayDateSet) {
        String dateText = date.toString();
        String manualMode = dateShiftModeByDate.get(dateText);
        if (manualMode != null) {
            return manualMode;
        }
        if (Boolean.TRUE.equals(skipStatutoryHolidays) && holidayDateSet.contains(dateText)) {
            return DATE_SHIFT_REST;
        }
        String normalizedWeekendMode = normalizeWeekendRestMode(weekendRestMode);
        if (WEEKEND_MODE_NONE.equals(normalizedWeekendMode)) {
            return DATE_SHIFT_DAY;
        }
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (WEEKEND_MODE_SINGLE.equals(normalizedWeekendMode)) {
            return dayOfWeek == 7 ? DATE_SHIFT_REST : DATE_SHIFT_DAY;
        }
        return dayOfWeek == 6 || dayOfWeek == 7 ? DATE_SHIFT_REST : DATE_SHIFT_DAY;
    }

    static boolean isShiftAvailable(String dateShiftMode, MesCalPlanShiftDO shift) {
        return !DATE_SHIFT_REST.equals(dateShiftMode) && shift != null;
    }

    static String resolveShiftCode(MesCalPlanShiftDO shift) {
        if (shift == null) {
            return DATE_SHIFT_DAY;
        }
        String shiftName = String.valueOf(shift.getName()).trim().toUpperCase(Locale.ROOT);
        if (shiftName.contains("NIGHT") || shiftName.contains("夜")) {
            return DATE_SHIFT_NIGHT;
        }
        if (shift.getSort() != null) {
            if (shift.getSort() == 1) {
                return DATE_SHIFT_DAY;
            }
            if (shift.getSort() == 3) {
                return DATE_SHIFT_NIGHT;
            }
        }
        if (isCrossDayShift(shift)) {
            return DATE_SHIFT_NIGHT;
        }
        return DATE_SHIFT_DAY;
    }

    private static boolean isCrossDayShift(MesCalPlanShiftDO shift) {
        if (shift.getStartTime() == null || shift.getEndTime() == null
                || shift.getStartTime().length() != 5 || shift.getEndTime().length() != 5) {
            return false;
        }
        try {
            LocalTime startTime = LocalTime.parse(shift.getStartTime());
            LocalTime endTime = LocalTime.parse(shift.getEndTime());
            return !endTime.isAfter(startTime);
        } catch (DateTimeParseException parseFailure) {
            return false;
        }
    }

    static String buildCalendarContextToken(MesProScheduleCalendarRulesRespVO rules) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("skipStatutoryHolidays", rules != null ? rules.getSkipStatutoryHolidays() : Boolean.FALSE);
        payload.put("weekendRestMode", normalizeWeekendRestMode(
                rules != null ? rules.getWeekendRestMode() : WEEKEND_MODE_SINGLE));
        payload.put("dateShiftModeByDate", new TreeMap<>(rules != null && rules.getDateShiftModeByDate() != null
                ? rules.getDateShiftModeByDate() : Collections.emptyMap()));
        payload.put("simulationCurrentDate", rules != null ? rules.getSimulationCurrentDate() : "");
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(payload));
    }

    private static LocalDate parseDate(String dateText) {
        try {
            return LocalDate.parse(dateText);
        } catch (Exception e) {
            throw exception(PRO_SCHEDULE_CALENDAR_INVALID_DATE);
        }
    }
}
