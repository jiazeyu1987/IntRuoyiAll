package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.*;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProScheduleCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 排程日历")
@RestController
@RequestMapping("/mes/pro/schedule-calendar")
@Validated
public class MesProScheduleCalendarController {

    @Resource
    private MesProScheduleCalendarService scheduleCalendarService;

    @GetMapping("/rules")
    @Operation(summary = "获取排程日历规则")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:query')")
    public CommonResult<MesProScheduleCalendarRulesRespVO> getRules() {
        return success(scheduleCalendarService.getRules());
    }

    @PutMapping("/rules")
    @Operation(summary = "保存排程日历规则")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:create')")
    public CommonResult<MesProScheduleCalendarRulesRespVO> saveRules(@Valid @RequestBody MesProScheduleCalendarRulesSaveReqVO reqVO) {
        return success(scheduleCalendarService.saveRules(reqVO));
    }

    @PostMapping("/simulation/advance-day")
    @Operation(summary = "模拟日期推进一天")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:create')")
    public CommonResult<MesProScheduleCalendarRulesRespVO> advanceSimulationDay() {
        return success(scheduleCalendarService.advanceSimulationDay());
    }

    @PostMapping("/simulation/advance-days")
    @Operation(summary = "模拟日期推进多天")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:create')")
    public CommonResult<MesProScheduleCalendarRulesRespVO> advanceSimulationDays(
            @Valid @RequestBody MesProScheduleCalendarSimulationAdvanceReqVO reqVO) {
        return success(scheduleCalendarService.advanceSimulationDays(reqVO));
    }

    @PostMapping("/simulation/reset")
    @Operation(summary = "重置模拟日期")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:create')")
    public CommonResult<MesProScheduleCalendarRulesRespVO> resetSimulation() {
        return success(scheduleCalendarService.resetSimulation());
    }

    @PostMapping("/capacity/generate")
    @Operation(summary = "按排程规则生成日期班次产能")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:create')")
    public CommonResult<MesProScheduleCalendarCapacityGenerateRespVO> generateCapacityPlans(
            @Valid @RequestBody MesProScheduleCalendarCapacityGenerateReqVO reqVO) {
        return success(scheduleCalendarService.generateCapacityPlans(reqVO));
    }

    @GetMapping("/month")
    @Operation(summary = "获取排程日历月份视图")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:query')")
    public CommonResult<MesProScheduleCalendarMonthRespVO> getMonth(
            @Parameter(name = "month", description = "月份，格式 YYYY-MM", required = true)
            @RequestParam("month") String month) {
        return success(scheduleCalendarService.getMonth(month));
    }

    @GetMapping("/day-detail")
    @Operation(summary = "获取排程日历单日详情")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:query')")
    public CommonResult<MesProScheduleCalendarDayDetailRespVO> getDayDetail(
            @Parameter(name = "date", description = "日期，格式 YYYY-MM-DD", required = true)
            @RequestParam("date") String date) {
        return success(scheduleCalendarService.getDayDetail(date));
    }

    @GetMapping("/work-order-analysis")
    @Operation(summary = "获取正式排程工单产线分析")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:query')")
    public CommonResult<MesProScheduleCalendarWorkOrderAnalysisRespVO> getWorkOrderAnalysis(
            @Parameter(name = "workOrderId", description = "工单编号", required = true)
            @RequestParam("workOrderId") Long workOrderId) {
        return success(scheduleCalendarService.getWorkOrderAnalysis(workOrderId));
    }

}
