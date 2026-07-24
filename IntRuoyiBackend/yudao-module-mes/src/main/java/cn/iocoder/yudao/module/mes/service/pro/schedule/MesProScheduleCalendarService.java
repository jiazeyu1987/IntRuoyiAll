package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Collection;

public interface MesProScheduleCalendarService {

    MesProScheduleCalendarRulesRespVO getRules();

    MesProScheduleCalendarRulesRespVO saveRules(@Valid MesProScheduleCalendarRulesSaveReqVO reqVO);

    MesProScheduleCalendarRulesRespVO advanceSimulationDay();

    MesProScheduleCalendarRulesRespVO advanceSimulationDays(@Valid MesProScheduleCalendarSimulationAdvanceReqVO reqVO);

    MesProScheduleCalendarRulesRespVO resetSimulation();

    MesProScheduleCalendarCapacityGenerateRespVO generateCapacityPlans(@Valid MesProScheduleCalendarCapacityGenerateReqVO reqVO);

    void ensureCapacityPlanCoverage(Collection<Long> lineIds, LocalDate startDate, LocalDate endDate);

    void refreshPlanCapacityForShiftHours(BigDecimal shiftHours);

    MesProScheduleCalendarMonthRespVO getMonth(String monthText);

    MesProScheduleCalendarDayDetailRespVO getDayDetail(String dateText);

    MesProScheduleCalendarWorkOrderAnalysisRespVO getWorkOrderAnalysis(Long workOrderId);

}
