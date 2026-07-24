package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDailyCompareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRouteStatusEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleSchemaTest {

    @Test
    void scheduleOrderSchema_shouldHoldRouteRiskAndProgressContract() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(1L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.MISSING.getStatus())
                .autoSchedulable(Boolean.FALSE)
                .latestStartTime(LocalDateTime.of(2026, 6, 19, 8, 0))
                .plannedStartTime(LocalDateTime.of(2026, 6, 20, 8, 0))
                .plannedEndTime(LocalDateTime.of(2026, 6, 21, 18, 30))
                .startRiskFlag(Boolean.TRUE)
                .delayRiskFlag(Boolean.TRUE)
                .totalQuantity(new BigDecimal("120.000000"))
                .completedQuantity(new BigDecimal("45.000000"))
                .uncompletedQuantity(new BigDecimal("75.000000"))
                .progressPercent(new BigDecimal("37.500000"))
                .build();

        assertEquals(MesProScheduleOrderRouteStatusEnum.MISSING.getStatus(), scheduleOrder.getRouteStatus());
        assertFalse(scheduleOrder.getAutoSchedulable());
        assertTrue(scheduleOrder.getStartRiskFlag());
        assertTrue(scheduleOrder.getDelayRiskFlag());
        assertEquals(new BigDecimal("37.500000"), scheduleOrder.getProgressPercent());
    }

    @Test
    void scheduleProcessSchema_shouldHoldCapacityCalendarAndProgressContract() {
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(2L)
                .routeVersionId(10L)
                .routeScheduleConfigId(20L)
                .capacityMode(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode())
                .infiniteDurationQuantityFactor(new BigDecimal("0.500000"))
                .infiniteDurationBaseMinutes(new BigDecimal("15.000000"))
                .nightShiftEnabled(Boolean.TRUE)
                .calendarRuleId(30L)
                .planDate(LocalDate.of(2026, 6, 20))
                .actualStartTime(LocalDateTime.of(2026, 6, 20, 8, 10))
                .actualEndTime(LocalDateTime.of(2026, 6, 20, 12, 30))
                .progressPercent(new BigDecimal("40.000000"))
                .build();

        assertEquals(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), process.getCapacityMode());
        assertEquals(new BigDecimal("0.500000"), process.getInfiniteDurationQuantityFactor());
        assertTrue(process.getNightShiftEnabled());
        assertEquals(LocalDate.of(2026, 6, 20), process.getPlanDate());
        assertEquals(new BigDecimal("40.000000"), process.getProgressPercent());
    }

    @Test
    void routeAndDailyCompareSchema_shouldHoldVersionConfigAndVarianceContract() {
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(10L)
                .routeId(100L)
                .versionNo("R-100-V002")
                .active(Boolean.TRUE)
                .routeSnapshotJson("{\"routeId\":100}")
                .build();
        MesProRouteScheduleConfigDO config = MesProRouteScheduleConfigDO.builder()
                .id(20L)
                .routeVersionId(10L)
                .routeProcessId(200L)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(new BigDecimal("12.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProScheduleOrderDailyCompareDO compare = MesProScheduleOrderDailyCompareDO.builder()
                .id(30L)
                .scheduleOrderId(1L)
                .scheduleOrderProcessId(2L)
                .planDate(LocalDate.of(2026, 6, 20))
                .plannedQuantity(new BigDecimal("100.000000"))
                .actualQuantity(new BigDecimal("80.000000"))
                .diffQuantity(new BigDecimal("-20.000000"))
                .status(MesProScheduleDailyCompareStatusEnum.BEHIND.getStatus())
                .build();

        assertTrue(routeVersion.getActive());
        assertNotNull(routeVersion.getRouteSnapshotJson());
        assertEquals(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), config.getCapacityMode());
        assertEquals(MesProScheduleDailyCompareStatusEnum.BEHIND.getStatus(), compare.getStatus());
    }

    @Test
    void scheduleIssueSchema_shouldDeclareLifecycleColumnsInMigration() throws Exception {
        MesProScheduleIssueDO issue = MesProScheduleIssueDO.builder()
                .status("OPEN")
                .sourceType("AUTO_SCHEDULE")
                .sourceId(10L)
                .resolutionReason("manual review")
                .resolvedBy(20L)
                .resolvedAt(LocalDateTime.of(2026, 6, 26, 8, 0))
                .build();
        assertEquals("OPEN", issue.getStatus());
        assertEquals("AUTO_SCHEDULE", issue.getSourceType());
        assertEquals(10L, issue.getSourceId());
        assertEquals("manual review", issue.getResolutionReason());
        assertEquals(20L, issue.getResolvedBy());
        assertEquals(LocalDateTime.of(2026, 6, 26, 8, 0), issue.getResolvedAt());

        String migration = Files.readString(Path.of("..", "sql", "mysql", "20260626_mes_schedule_issue_lifecycle.sql"), StandardCharsets.UTF_8);
        for (String column : List.of("status", "source_type", "source_id", "resolution_reason", "resolved_by", "resolved_at")) {
            assertTrue(migration.contains("column_name = '" + column + "'"), "migration must guard column " + column);
            assertTrue(migration.contains("ADD COLUMN `" + column + "`"), "migration must add column " + column);
        }
    }
}
