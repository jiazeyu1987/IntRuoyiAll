package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProReplanExplanationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.holiday.MesCalHolidayDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarSimulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanExplanationSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdProductionLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.cal.plan.MesCalPlanShiftMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanExplanationSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarSimulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.iocoder.yudao.module.mes.enums.cal.MesCalHolidayTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.service.cal.holiday.MesCalHolidayService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MesProScheduleCalendarServiceImpl implements MesProScheduleCalendarService {

    private static final String DATE_SHIFT_DAY = "DAY";
    private static final String DATE_SHIFT_NIGHT = "NIGHT";
    private static final String CAPACITY_SOURCE_ROUTE_PROCESS = "ROUTE_PROCESS";
    private static final String ISSUE_STATUS_OPEN = "OPEN";
    private static final String ISSUE_SEVERITY_BLOCKING = "BLOCKING";

    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesProScheduleCalendarSimulationMapper scheduleCalendarSimulationMapper;
    @Resource
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Resource
    private MesCalHolidayService holidayService;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;
    @Resource
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Resource
    @Lazy
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesMdProductionLineMapper productionLineMapper;
    @Resource
    private MesWmMaterialStockMapper materialStockMapper;
    @Resource
    private MesMdProductionLineService productionLineService;
    @Resource
    private MesMdWorkshopService workshopService;
    @Resource
    private MesProProcessService processService;
    @Resource
    @Lazy
    private MesProRouteService routeService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesCalPlanService planService;
    @Resource
    private MesCalPlanShiftService planShiftService;
    @Resource
    private MesCalPlanShiftMapper planShiftMapper;
    @Resource
    private MesMdWorkstationCapacityService workstationCapacityService;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy;

    @Override
    public MesProScheduleCalendarRulesRespVO getRules() {
        MesProScheduleCalendarRuleDO rule = getOrCreateRule();
        MesProScheduleCalendarSimulationDO simulation = getOrCreateSimulation();
        return toRulesResp(rule, simulation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProScheduleCalendarRulesRespVO saveRules(MesProScheduleCalendarRulesSaveReqVO reqVO) {
        MesProScheduleCalendarRuleDO rule = getOrCreateRule();
        rule.setSkipStatutoryHolidays(reqVO.getSkipStatutoryHolidays());
        rule.setWeekendRestMode(normalizeWeekendRestMode(reqVO.getWeekendRestMode()));
        rule.setDateShiftModeByDateJson(JsonUtils.toJsonString(normalizeDateShiftModeByDate(reqVO.getDateShiftModeByDate())));
        scheduleCalendarRuleMapper.updateById(rule);
        return getRules();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProScheduleCalendarRulesRespVO advanceSimulationDay() {
        MesProScheduleCalendarSimulationDO simulation = getOrCreateSimulation();
        simulation.setCurrentDate(simulation.getCurrentDate().plusDays(1));
        scheduleCalendarSimulationMapper.updateById(simulation);
        return getRules();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProScheduleCalendarRulesRespVO advanceSimulationDays(MesProScheduleCalendarSimulationAdvanceReqVO reqVO) {
        MesProScheduleCalendarSimulationDO simulation = getOrCreateSimulation();
        simulation.setCurrentDate(simulation.getCurrentDate().plusDays(reqVO.getDays()));
        scheduleCalendarSimulationMapper.updateById(simulation);
        return getRules();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProScheduleCalendarRulesRespVO resetSimulation() {
        MesProScheduleCalendarSimulationDO simulation = getOrCreateSimulation();
        simulation.setCurrentDate(LocalDate.now().atStartOfDay());
        scheduleCalendarSimulationMapper.updateById(simulation);
        return getRules();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProScheduleCalendarCapacityGenerateRespVO generateCapacityPlans(MesProScheduleCalendarCapacityGenerateReqVO reqVO) {
        LocalDate startDate = parseDate(reqVO.getStartDate());
        LocalDate endDate = startDate.plusDays(reqVO.getDays() - 1L);
        CapacityPlanGenerationResult result = generateCapacityPlans(loadCapacityGenerationLines(reqVO), startDate, endDate);
        return MesProScheduleCalendarCapacityGenerateRespVO.builder()
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .lineCount(result.lines().size())
                .generatedCount(result.generatedCount())
                .skippedExistingCount(result.skippedExistingCount())
                .skippedRestCount(result.skippedRestCount())
                .skippedNoShiftCount(result.skippedNoShiftCount())
                .skippedDetails(result.skippedDetails())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureCapacityPlanCoverage(Collection<Long> lineIds, LocalDate startDate, LocalDate endDate) {
        if (CollUtil.isEmpty(lineIds) || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return;
        }
        MesProScheduleCalendarCapacityGenerateReqVO reqVO = new MesProScheduleCalendarCapacityGenerateReqVO();
        reqVO.setLineIds(new ArrayList<>(new LinkedHashSet<>(lineIds)));
        List<MesMdProductionLineDO> lines = loadCapacityGenerationLines(reqVO);
        if (CollUtil.isEmpty(lines)) {
            return;
        }
        generateCapacityPlans(lines, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPlanCapacityForShiftHours(BigDecimal shiftHours) {
        if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception0(400, "班次小时必须大于 0");
        }
        Integer capacityMinutes = shiftHours
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValueExact();
        if (capacityMinutes <= 0) {
            throw exception0(400, "班次分钟必须大于 0");
        }
        List<MesMdProductionLineDO> lines = loadCapacityGenerationLines(new MesProScheduleCalendarCapacityGenerateReqVO());
        if (CollUtil.isEmpty(lines)) {
            throw exception0(400, "未找到已启用且绑定排班计划的产线，无法同步班时");
        }
        LocalDateTime refreshStartDate = getOrCreateSimulation().getCurrentDate().toLocalDate().atStartOfDay();
        Map<Long, List<MesCalPlanShiftDO>> shiftsByPlanId = loadPlanShifts(lines);
        Set<Long> refreshedShiftIds = new LinkedHashSet<>();
        for (MesMdProductionLineDO line : lines) {
            MesCalPlanShiftDO shift = firstShiftForLine(line, shiftsByPlanId);
            String refreshedEndTime = calculateShiftEndTime(shift.getStartTime(), capacityMinutes);
            if (refreshedShiftIds.add(shift.getId())) {
                planShiftMapper.updateEndTimeById(shift.getId(), refreshedEndTime);
            }
            capacityPlanMapper.updateCapacityMinutesByLineAndShiftFromDate(
                    line.getId(), shift.getId(), refreshStartDate, capacityMinutes);
        }
    }

    private MesProScheduleCalendarCapacityGenerateRespVO.SkippedDetail buildCapacitySkippedDetail(
            LocalDate date, MesMdProductionLineDO line, MesCalPlanShiftDO shift, String reasonCode, String reasonText) {
        return MesProScheduleCalendarCapacityGenerateRespVO.SkippedDetail.builder()
                .date(date == null ? null : date.toString())
                .lineCode(line == null ? null : line.getCode())
                .lineName(line == null ? null : line.getName())
                .shiftName(shift == null ? null : shift.getName())
                .reasonCode(reasonCode)
                .reasonText(reasonText)
                .build();
    }

    private CapacityPlanGenerationResult generateCapacityPlans(List<MesMdProductionLineDO> lines,
                                                               LocalDate startDate,
                                                               LocalDate endDate) {
        if (CollUtil.isEmpty(lines) || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return new CapacityPlanGenerationResult(Collections.emptyList(), 0, 0, 0, 0, Collections.emptyList());
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        MesProScheduleCalendarRuleDO rule = getOrCreateRule();
        Map<String, String> ruleMap = parseDateShiftModeByDate(rule.getDateShiftModeByDateJson());
        Set<String> holidayDateSet = holidayService.getHolidayList(startDateTime, endExclusive).stream()
                .filter(holiday -> ObjUtil.equal(holiday.getType(), MesCalHolidayTypeEnum.HOLIDAY.getType()))
                .map(holiday -> holiday.getDay().toLocalDate().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> lineIds = lines.stream()
                .map(MesMdProductionLineDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<MesCalPlanShiftDO>> shiftsByPlanId = loadPlanShifts(lines);
        Set<String> existingCapacityKeys = capacityPlanMapper
                .selectListByLineIdsAndDateRange(lineIds, startDateTime, endExclusive)
                .stream()
                .filter(capacity -> capacity.getCalendarDate() != null)
                .map(capacity -> buildLineDateShiftKey(
                        capacity.getLineId(), capacity.getCalendarDate().toLocalDate(), capacity.getShiftId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int generatedCount = 0;
        int skippedExistingCount = 0;
        int skippedRestCount = 0;
        int skippedNoShiftCount = 0;
        List<MesProScheduleCalendarCapacityGenerateRespVO.SkippedDetail> skippedDetails = new ArrayList<>();
        for (MesMdProductionLineDO line : lines) {
            List<MesCalPlanShiftDO> shifts = shiftsByPlanId.getOrDefault(line.getCalendarPlanId(), Collections.emptyList());
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateShiftMode = resolveDateShiftMode(date, rule, ruleMap, holidayDateSet);
                if (MesProScheduleCalendarRuleSupport.DATE_SHIFT_REST.equals(dateShiftMode)) {
                    skippedRestCount++;
                    skippedDetails.add(buildCapacitySkippedDetail(date, line, null,
                            "REST_DATE", "排程日历规则标记为休息日"));
                    continue;
                }
                List<MesCalPlanShiftDO> availableShifts = shifts.stream()
                        .filter(shift -> MesProScheduleCalendarRuleSupport.isShiftAvailable(dateShiftMode, shift))
                        .toList();
                if (availableShifts.isEmpty()) {
                    skippedNoShiftCount++;
                    skippedDetails.add(buildCapacitySkippedDetail(date, line, null,
                            "NO_SHIFT", "产线未配置可用班次"));
                    continue;
                }
                for (MesCalPlanShiftDO shift : availableShifts) {
                    String capacityKey = buildLineDateShiftKey(line.getId(), date, shift.getId());
                    if (existingCapacityKeys.contains(capacityKey)) {
                        skippedExistingCount++;
                        skippedDetails.add(buildCapacitySkippedDetail(date, line, shift,
                                "EXISTING_CAPACITY", "该日期班次产能已存在"));
                        continue;
                    }
                    Integer capacityMinutes = calculateShiftCapacityMinutes(date, shift);
                    if (capacityMinutes == null || capacityMinutes <= 0) {
                        skippedNoShiftCount++;
                        skippedDetails.add(buildCapacitySkippedDetail(date, line, shift,
                                "INVALID_SHIFT_CAPACITY", "班次时间无法换算为有效产能"));
                        continue;
                    }
                    MesProCapacityPlanDO capacityPlan = MesProCapacityPlanDO.builder()
                            .lineId(line.getId())
                            .calendarDate(date.atStartOfDay())
                            .shiftId(shift.getId())
                            .capacityMinutes(capacityMinutes)
                            .enabled(Boolean.TRUE)
                            .remark("按排程日历规则生成")
                            .build();
                    capacityPlanMapper.insert(capacityPlan);
                    existingCapacityKeys.add(capacityKey);
                    generatedCount++;
                }
            }
        }
        return new CapacityPlanGenerationResult(lines, generatedCount, skippedExistingCount,
                skippedRestCount, skippedNoShiftCount, skippedDetails);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProScheduleCalendarMonthRespVO getMonth(String monthText) {
        YearMonth month = parseMonth(monthText);
        CalendarContext context = buildContext(month.atDay(1), month.atEndOfMonth());

        List<MesProScheduleCalendarMonthRespVO.DaySummaryItem> days = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate currentDate = month.atDay(day);
            String dateText = currentDate.toString();
            List<TaskCalendarRow> taskRows = context.tasksByDate.getOrDefault(dateText, Collections.emptyList());
            List<DailyMaterialSummaryRow> materialRows = context.materialRowsByDate.getOrDefault(dateText, Collections.emptyList());
            days.add(MesProScheduleCalendarMonthRespVO.DaySummaryItem.builder()
                    .date(dateText)
                    .holiday(context.holidayDateSet.contains(dateText))
                    .dateShiftMode(resolveDateShiftMode(currentDate, context.rule, context.ruleMap, context.holidayDateSet))
                    .totalTaskCount(taskRows.size())
                    .totalOrderCount((int) taskRows.stream().map(TaskCalendarRow::getWorkOrderId).distinct().count())
                    .dayShiftTaskCount((int) taskRows.stream().filter(row -> MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY.equals(row.shiftCode)).count())
                    .nightShiftTaskCount((int) taskRows.stream().filter(row -> MesProScheduleCalendarRuleSupport.DATE_SHIFT_NIGHT.equals(row.shiftCode)).count())
                    .shortageCount((int) materialRows.stream().filter(row -> hasPositive(row.shortageQty)).count())
                    .build());
        }

        MesProScheduleCalendarMonthRespVO response = new MesProScheduleCalendarMonthRespVO();
        response.setMonth(month.toString());
        response.setSimulationCurrentDate(context.simulation.getCurrentDate().toLocalDate().toString());
        response.setCurrentScheduleStatus(MesProScheduleCalendarMonthRespVO.CurrentScheduleStatus.builder()
                .hasCurrentSchedule(context.hasCurrentSchedule)
                .updatedAt(formatDateTime(context.latestUpdatedAt))
                .totalTaskCount(context.totalCurrentTaskCount)
                .build());
        response.setDays(days);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProScheduleCalendarDayDetailRespVO getDayDetail(String dateText) {
        LocalDate selectedDate = parseDate(dateText);
        CalendarContext context = buildContext(selectedDate, selectedDate);
        List<TaskCalendarRow> rows = context.tasksByDate.getOrDefault(dateText, Collections.emptyList());
        List<DailyMaterialSummaryRow> dailyMaterialRows = context.materialRowsByDate.getOrDefault(dateText, Collections.emptyList());
        List<MesProScheduleIssueDO> dailyIssues = context.issuesByDate.getOrDefault(dateText, Collections.emptyList());

        Map<Optional<Long>, List<TaskCalendarRow>> rowsByWorkshop = rows.stream()
                .collect(Collectors.groupingBy(row -> Optional.ofNullable(row.getWorkshopId()), LinkedHashMap::new, Collectors.toList()));

        List<MesProScheduleCalendarDayDetailRespVO.WorkshopDetailItem> workshops = new ArrayList<>();
        for (Map.Entry<Optional<Long>, List<TaskCalendarRow>> workshopEntry : rowsByWorkshop.entrySet()) {
            Long workshopId = workshopEntry.getKey().orElse(null);
            MesMdWorkshopDO workshop = workshopId != null ? context.workshopMap.get(workshopId) : null;
            TaskCalendarRow workshopSource = workshopEntry.getValue().get(0);
            Map<Optional<Long>, List<TaskCalendarRow>> rowsByLine = workshopEntry.getValue().stream()
                    .collect(Collectors.groupingBy(row -> Optional.ofNullable(row.getLineId()), LinkedHashMap::new, Collectors.toList()));

            List<MesProScheduleCalendarDayDetailRespVO.LineDetailItem> lines = new ArrayList<>();
            for (Map.Entry<Optional<Long>, List<TaskCalendarRow>> lineEntry : rowsByLine.entrySet()) {
                Long lineId = lineEntry.getKey().orElse(null);
                TaskCalendarRow lineSource = lineEntry.getValue().get(0);
                List<MesProScheduleCalendarDayDetailRespVO.TaskDetailItem> tasks = lineEntry.getValue().stream()
                        .map(row -> MesProScheduleCalendarDayDetailRespVO.TaskDetailItem.builder()
                                .taskId(row.taskId)
                                .taskCode(row.taskCode)
                                .workOrderId(row.workOrderId)
                                .workOrderCode(row.workOrderCode)
                                .routeId(row.routeId)
                                .routeName(row.routeName)
                                .processName(row.processName)
                                .itemCode(row.itemCode)
                                .itemName(row.itemName)
                                .shiftCode(row.shiftCode)
                                .quantity(row.quantity)
                                .dailyQuantity(calculateDailyQuantity(row, selectedDate))
                                .reportedQuantity(row.reportedQuantity)
                                .pendingInspectionQuantity(row.pendingInspectionQuantity)
                                .executionStatus(row.executionStatus)
                                .startTime(formatDateTime(row.startTime))
                                .endTime(formatDateTime(row.endTime))
                                .scheduleSource(row.scheduleSource)
                                .locked(row.locked)
                                .riskStatus(row.riskStatus)
                                .scheduleOrderFrozen(row.scheduleOrderFrozen)
                                .scheduleOrderFreezeReason(row.scheduleOrderFreezeReason)
                                .build())
                        .toList();
                lines.add(MesProScheduleCalendarDayDetailRespVO.LineDetailItem.builder()
                        .lineId(lineId)
                        .lineCode(StrUtil.blankToDefault(lineSource.lineCode, ""))
                        .lineName(StrUtil.blankToDefault(lineSource.lineName, "未绑定工艺路线"))
                        .taskCount(tasks.size())
                        .orderCount((int) lineEntry.getValue().stream().map(TaskCalendarRow::getWorkOrderId).distinct().count())
                        .tasks(tasks)
                        .build());
            }

            workshops.add(MesProScheduleCalendarDayDetailRespVO.WorkshopDetailItem.builder()
                    .workshopId(workshopId)
                    .workshopCode(workshop != null ? workshop.getCode() : "")
                    .workshopName(workshop != null ? workshop.getName() : workshopSource.workshopName)
                    .taskCount(workshopEntry.getValue().size())
                    .orderCount((int) workshopEntry.getValue().stream().map(TaskCalendarRow::getWorkOrderId).distinct().count())
                    .busyLineCount((int) lines.stream().filter(line -> line.getTaskCount() > 0).count())
                    .lines(lines)
                    .build());
        }

        List<MesProScheduleCalendarDayDetailRespVO.MaterialShortageItem> materialIssues = dailyMaterialRows.stream()
                .map(row -> {
                    MesMdItemDO material = context.itemMap.get(row.materialId);
                    MesProWorkOrderDO workOrder = row.singleWorkOrderId != null ? context.workOrderMap.get(row.singleWorkOrderId) : null;
                    return MesProScheduleCalendarDayDetailRespVO.MaterialShortageItem.builder()
                            .issueId(null)
                            .severity(hasPositive(row.shortageQty) ? "WARNING" : "INFO")
                            .workOrderId(row.singleWorkOrderId)
                            .workOrderCode(workOrder != null ? workOrder.getCode() : "")
                            .materialId(row.materialId)
                            .materialCode(material != null ? material.getCode() : "")
                            .materialName(material != null ? material.getName() : "物料不存在")
                            .scheduledUsageQty(row.scheduledUsageQty)
                            .remainingAvailableQty(row.remainingAvailableQty)
                            .affectedWorkOrderCount(row.affectedWorkOrderCount)
                            .requiredQty(row.cumulativeRequiredQty)
                            .availableQty(row.totalAvailableQty)
                            .shortageQty(row.shortageQty)
                            .message(buildMaterialSummaryMessage(row))
                            .build();
                })
                .filter(item -> hasPositive(item.getShortageQty()))
                .toList();

        MesProScheduleCalendarDayDetailRespVO response = new MesProScheduleCalendarDayDetailRespVO();
        response.setDate(dateText);
        response.setSimulationCurrentDate(context.simulation.getCurrentDate().toLocalDate().toString());
        response.setHoliday(context.holidayDateSet.contains(dateText));
        response.setDateShiftMode(resolveDateShiftMode(selectedDate, context.rule, context.ruleMap, context.holidayDateSet));
        response.setDayShiftTaskCount((int) rows.stream().filter(row -> MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY.equals(row.shiftCode)).count());
        response.setNightShiftTaskCount((int) rows.stream().filter(row -> MesProScheduleCalendarRuleSupport.DATE_SHIFT_NIGHT.equals(row.shiftCode)).count());
        response.setWorkshops(workshops);
        response.setMaterialShortageSummary(MesProScheduleCalendarDayDetailRespVO.MaterialShortageSummary.builder()
                .shortageCount((int) materialIssues.stream().filter(item -> hasPositive(item.getShortageQty())).count())
                .totalShortageQty(materialIssues.stream()
                        .map(MesProScheduleCalendarDayDetailRespVO.MaterialShortageItem::getShortageQty)
                        .filter(this::hasPositive)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .items(materialIssues)
                .build());
        response.setMaterialDemandSummary(buildMaterialDemandSummary(
                selectedDate, context.workOrderMap, context.itemMap, context.materialDemandByWorkOrderId,
                context.availableStockByItemId, context.firstStartDateByWorkOrderId));
        response.setScheduleIssueSummary(buildScheduleIssueSummary(dailyIssues, context.workOrderMap));
        response.setProcessCapacitySummary(buildProcessCapacitySummary(rows, selectedDate));
        return response;
    }

    private MesProScheduleCalendarDayDetailRespVO.ProcessCapacitySummary buildProcessCapacitySummary(List<TaskCalendarRow> rows,
                                                                                                      LocalDate selectedDate) {
        if (CollUtil.isEmpty(rows)) {
            return emptyProcessCapacitySummary();
        }
        Map<String, ProcessCapacityAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (TaskCalendarRow row : rows) {
            String processKey = buildProcessCapacityProcessKey(row);
            ProcessCapacityAccumulator accumulator = accumulatorMap.computeIfAbsent(processKey,
                    ignored -> new ProcessCapacityAccumulator(row.processId, buildProcessCapacityProcessName(row)));
            accumulator.taskCount++;
            if (row.workOrderId != null) {
                accumulator.workOrderIds.add(row.workOrderId);
            }
            BigDecimal dailyQuantity = ObjUtil.defaultIfNull(calculateDailyQuantity(row, selectedDate), BigDecimal.ZERO);
            accumulator.scheduledQuantity = accumulator.scheduledQuantity.add(dailyQuantity);
            if (hasPositive(row.shiftCapacityTotal)) {
                accumulator.maxCapacityByLine.merge(row.resourceLineId, row.shiftCapacityTotal,
                        (left, right) -> left.compareTo(right) >= 0 ? left : right);
            }
        }

        List<MesProScheduleCalendarDayDetailRespVO.ProcessCapacityItem> items = accumulatorMap.values().stream()
                .map(accumulator -> {
                    BigDecimal maxCapacity = accumulator.maxCapacityByLine.values().stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal remainingCapacity = maxCapacity.subtract(accumulator.scheduledQuantity);
                    if (remainingCapacity.compareTo(BigDecimal.ZERO) < 0) {
                        remainingCapacity = BigDecimal.ZERO;
                    }
                    BigDecimal overCapacity = hasPositive(maxCapacity)
                            && accumulator.scheduledQuantity.compareTo(maxCapacity) > 0
                            ? accumulator.scheduledQuantity.subtract(maxCapacity)
                            : BigDecimal.ZERO;
                    BigDecimal utilizationRate = hasPositive(maxCapacity)
                            ? accumulator.scheduledQuantity
                            .multiply(BigDecimal.valueOf(100))
                            .divide(maxCapacity, 6, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return MesProScheduleCalendarDayDetailRespVO.ProcessCapacityItem.builder()
                            .processId(accumulator.processId)
                            .processName(accumulator.processName)
                            .taskCount(accumulator.taskCount)
                            .workOrderCount(accumulator.workOrderIds.size())
                            .maxCapacity(maxCapacity)
                            .scheduledQuantity(accumulator.scheduledQuantity)
                            .remainingCapacity(remainingCapacity)
                            .overCapacity(overCapacity)
                            .utilizationRate(utilizationRate)
                            .build();
                })
                .toList();
        return MesProScheduleCalendarDayDetailRespVO.ProcessCapacitySummary.builder()
                .processCount(items.size())
                .totalMaxCapacity(items.stream()
                        .map(MesProScheduleCalendarDayDetailRespVO.ProcessCapacityItem::getMaxCapacity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalScheduledQuantity(items.stream()
                        .map(MesProScheduleCalendarDayDetailRespVO.ProcessCapacityItem::getScheduledQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalRemainingCapacity(items.stream()
                        .map(MesProScheduleCalendarDayDetailRespVO.ProcessCapacityItem::getRemainingCapacity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .items(items)
                .build();
    }

    private MesProScheduleCalendarDayDetailRespVO.ProcessCapacitySummary emptyProcessCapacitySummary() {
        return MesProScheduleCalendarDayDetailRespVO.ProcessCapacitySummary.builder()
                .processCount(0)
                .totalMaxCapacity(BigDecimal.ZERO)
                .totalScheduledQuantity(BigDecimal.ZERO)
                .totalRemainingCapacity(BigDecimal.ZERO)
                .items(Collections.emptyList())
                .build();
    }

    private String buildProcessCapacityProcessKey(TaskCalendarRow row) {
        if (row.processId != null) {
            return "PROCESS:" + row.processId;
        }
        return "NAME:" + StrUtil.blankToDefault(row.processName, "未绑定工序");
    }

    private String buildProcessCapacityProcessName(TaskCalendarRow row) {
        if (StrUtil.isNotBlank(row.processName)) {
            return row.processName;
        }
        return row.processId != null ? "工序不存在" : "未绑定工序";
    }

    private MesProScheduleCalendarDayDetailRespVO.MaterialDemandSummary buildMaterialDemandSummary(
            LocalDate selectedDate,
            Map<Long, MesProWorkOrderDO> workOrderMap,
            Map<Long, MesMdItemDO> itemMap,
            Map<Long, Map<Long, BigDecimal>> materialDemandByWorkOrderId,
            Map<Long, BigDecimal> availableStockByItemId,
            Map<Long, LocalDate> firstStartDateByWorkOrderId) {
        if (selectedDate == null || CollUtil.isEmpty(materialDemandByWorkOrderId)) {
            return emptyMaterialDemandSummary();
        }
        List<MesProScheduleCalendarDayDetailRespVO.MaterialDemandWorkOrderItem> workOrderItems = new ArrayList<>();
        Map<Long, BigDecimal> totalRequiredByMaterialId = new LinkedHashMap<>();
        Map<Long, Set<Long>> workOrderIdsByMaterialId = new LinkedHashMap<>();
        for (Map.Entry<Long, Map<Long, BigDecimal>> workOrderEntry : materialDemandByWorkOrderId.entrySet()) {
            Long workOrderId = workOrderEntry.getKey();
            LocalDate firstStartDate = firstStartDateByWorkOrderId.get(workOrderId);
            if (firstStartDate == null || firstStartDate.isAfter(selectedDate)) {
                continue;
            }
            MesProWorkOrderDO workOrder = workOrderMap.get(workOrderId);
            for (Map.Entry<Long, BigDecimal> demandEntry : workOrderEntry.getValue().entrySet()) {
                Long materialId = demandEntry.getKey();
                BigDecimal requiredQty = ObjUtil.defaultIfNull(demandEntry.getValue(), BigDecimal.ZERO);
                totalRequiredByMaterialId.merge(materialId, requiredQty, BigDecimal::add);
                workOrderIdsByMaterialId.computeIfAbsent(materialId, ignored -> new LinkedHashSet<>()).add(workOrderId);
                BigDecimal availableQty = ObjUtil.defaultIfNull(availableStockByItemId.get(materialId), BigDecimal.ZERO);
                BigDecimal shortageQty = requiredQty.subtract(availableQty);
                if (shortageQty.compareTo(BigDecimal.ZERO) < 0) {
                    shortageQty = BigDecimal.ZERO;
                }
                MesMdItemDO material = itemMap.get(materialId);
                workOrderItems.add(MesProScheduleCalendarDayDetailRespVO.MaterialDemandWorkOrderItem.builder()
                        .workOrderId(workOrderId)
                        .workOrderCode(workOrder != null ? workOrder.getCode() : "")
                        .materialId(materialId)
                        .materialCode(material != null ? material.getCode() : "")
                        .materialName(material != null ? material.getName() : "物料不存在")
                        .requiredQty(requiredQty)
                        .availableQty(availableQty)
                        .shortageQty(shortageQty)
                        .build());
            }
        }
        List<MesProScheduleCalendarDayDetailRespVO.MaterialDemandTotalItem> totalItems = totalRequiredByMaterialId.entrySet()
                .stream()
                .map(entry -> {
                    Long materialId = entry.getKey();
                    BigDecimal requiredQty = entry.getValue();
                    BigDecimal availableQty = ObjUtil.defaultIfNull(availableStockByItemId.get(materialId), BigDecimal.ZERO);
                    BigDecimal shortageQty = requiredQty.subtract(availableQty);
                    if (shortageQty.compareTo(BigDecimal.ZERO) < 0) {
                        shortageQty = BigDecimal.ZERO;
                    }
                    MesMdItemDO material = itemMap.get(materialId);
                    return MesProScheduleCalendarDayDetailRespVO.MaterialDemandTotalItem.builder()
                            .materialId(materialId)
                            .materialCode(material != null ? material.getCode() : "")
                            .materialName(material != null ? material.getName() : "物料不存在")
                            .requiredQty(requiredQty)
                            .availableQty(availableQty)
                            .shortageQty(shortageQty)
                            .affectedWorkOrderCount(workOrderIdsByMaterialId.getOrDefault(materialId, Collections.emptySet()).size())
                            .build();
                })
                .toList();
        return MesProScheduleCalendarDayDetailRespVO.MaterialDemandSummary.builder()
                .materialCount(totalItems.size())
                .workOrderCount((int) workOrderItems.stream()
                        .map(MesProScheduleCalendarDayDetailRespVO.MaterialDemandWorkOrderItem::getWorkOrderId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count())
                .totalItems(totalItems)
                .workOrderItems(workOrderItems)
                .build();
    }

    private MesProScheduleCalendarDayDetailRespVO.MaterialDemandSummary emptyMaterialDemandSummary() {
        return MesProScheduleCalendarDayDetailRespVO.MaterialDemandSummary.builder()
                .materialCount(0)
                .workOrderCount(0)
                .totalItems(Collections.emptyList())
                .workOrderItems(Collections.emptyList())
                .build();
    }

    private MesProScheduleCalendarDayDetailRespVO.ScheduleIssueSummary buildScheduleIssueSummary(
            List<MesProScheduleIssueDO> issues, Map<Long, MesProWorkOrderDO> workOrderMap) {
        List<MesProScheduleCalendarDayDetailRespVO.ScheduleIssueItem> items = ObjUtil.defaultIfNull(issues, Collections.<MesProScheduleIssueDO>emptyList())
                .stream()
                .filter(issue -> !ObjUtil.equal(issue.getResolved(), Boolean.TRUE))
                .filter(issue -> issue.getStatus() == null || ISSUE_STATUS_OPEN.equals(issue.getStatus()))
                .map(issue -> {
                    MesProWorkOrderDO workOrder = issue.getWorkOrderId() == null ? null : workOrderMap.get(issue.getWorkOrderId());
                    return MesProScheduleCalendarDayDetailRespVO.ScheduleIssueItem.builder()
                            .issueId(issue.getId())
                            .issueType(issue.getIssueType())
                            .severity(issue.getSeverity())
                            .workOrderId(issue.getWorkOrderId())
                            .workOrderCode(workOrder != null ? workOrder.getCode() : "")
                            .taskId(issue.getTaskId())
                            .message(issue.getMessage())
                            .status(issue.getStatus())
                            .sourceType(issue.getSourceType())
                            .sourceId(issue.getSourceId())
                            .build();
                })
                .toList();
        return MesProScheduleCalendarDayDetailRespVO.ScheduleIssueSummary.builder()
                .openIssueCount(items.size())
                .blockingIssueCount((int) items.stream()
                        .filter(item -> ISSUE_SEVERITY_BLOCKING.equals(item.getSeverity()))
                        .count())
                .items(items)
                .build();
    }

    @Override
    public MesProScheduleCalendarWorkOrderAnalysisRespVO getWorkOrderAnalysis(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderService.getWorkOrder(workOrderId);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        List<MesProTaskDO> tasks = ObjUtil.defaultIfNull(
                        taskMapper.selectListByWorkOrderIds(Collections.singleton(workOrderId)),
                        Collections.<MesProTaskDO>emptyList())
                .stream()
                .filter(task -> !ObjUtil.equal(task.getStatus(), cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum.CANCELED.getStatus()))
                .sorted(Comparator.comparing(MesProTaskDO::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(MesProTaskDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (tasks.isEmpty()) {
            throw exception(PRO_TASK_NOT_EXISTS);
        }

        MesMdItemDO product = itemService.getItemMap(Collections.singleton(workOrder.getProductId())).get(workOrder.getProductId());
        Set<Long> workstationIds = tasks.stream().map(MesProTaskDO::getWorkstationId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdWorkstationDO> workstationMap = workstationIds.isEmpty()
                ? Collections.emptyMap()
                : ObjUtil.defaultIfNull(workstationMapper.selectByIds(workstationIds), Collections.<MesMdWorkstationDO>emptyList()).stream()
                .collect(Collectors.toMap(MesMdWorkstationDO::getId, Function.identity()));
        validateIdsFound(workstationIds, workstationMap, MD_WORKSTATION_NOT_EXISTS);

        Set<Long> lineIds = workstationMap.values().stream().map(MesMdWorkstationDO::getProductionLineId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdProductionLineDO> lineMap = lineIds.isEmpty()
                ? Collections.emptyMap()
                : ObjUtil.defaultIfNull(productionLineService.getProductionLineMap(lineIds), Collections.emptyMap());
        validateIdsFound(lineIds, lineMap, MD_PRODUCTION_LINE_NOT_EXISTS);
        Set<Long> routeIds = tasks.stream().map(MesProTaskDO::getRouteId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProRouteDO> routeMap = routeIds.isEmpty() ? Collections.emptyMap()
                : ObjUtil.defaultIfNull(scheduleDefaultCompatibilityPolicy.historicalReadRouteMapIgnoreDeleted(
                () -> routeService.getRouteMapIgnoreDeleted(routeIds)), Collections.emptyMap());
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(tasks.stream()
                .map(MesProTaskDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, Long> processIdentityMap = routeProcessService.getProcessIdentityMap(processMap.keySet());
        List<MesMdWorkstationDO> lineWorkstations = workstationMapper.selectListByProcessIds(
                processIdentityMap.keySet(),
                cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE.getStatus());
        lineWorkstations.forEach(workstation -> {
            Long currentProcessId = processIdentityMap.get(workstation.getProcessId());
            if (currentProcessId != null) {
                workstation.setProcessId(currentProcessId);
            }
        });
        Map<String, CalendarProcessResourcePool> resourcePoolMap = buildCalendarProcessResourcePools(lineWorkstations, processMap);
        Map<Long, MesProTaskScheduleExtDO> extMap = ObjUtil.defaultIfNull(
                        taskScheduleExtMapper.selectListByTaskIds(tasks.stream()
                                .map(MesProTaskDO::getId)
                                .filter(Objects::nonNull)
                                .toList()),
                        Collections.<MesProTaskScheduleExtDO>emptyList())
                .stream()
                .collect(Collectors.toMap(MesProTaskScheduleExtDO::getTaskId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Set<Long> scheduleOrderProcessIds = extMap.values().stream()
                .map(MesProTaskScheduleExtDO::getScheduleOrderProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProScheduleOrderProcessDO> scheduleOrderProcessMap = scheduleOrderProcessIds.isEmpty()
                ? Collections.emptyMap()
                : ObjUtil.defaultIfNull(scheduleOrderProcessMapper.selectByIds(scheduleOrderProcessIds),
                        Collections.<MesProScheduleOrderProcessDO>emptyList()).stream()
                .collect(Collectors.toMap(MesProScheduleOrderProcessDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<MesProTaskDO>> tasksByProcess = tasks.stream()
                .collect(Collectors.groupingBy(MesProTaskDO::getProcessId, LinkedHashMap::new, Collectors.toList()));

        List<MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem> processItems = new ArrayList<>();
        int processSort = 1;
        for (Map.Entry<Long, List<MesProTaskDO>> entry : tasksByProcess.entrySet()) {
            Long processId = entry.getKey();
            MesProProcessDO process = processMap.get(processId);
            List<MesProTaskDO> processTasks = entry.getValue();
            Set<Long> processLineIds = processTasks.stream()
                    .map(MesProTaskDO::getWorkstationId)
                    .map(workstationMap::get)
                    .filter(Objects::nonNull)
                    .map(MesMdWorkstationDO::getProductionLineId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            CalendarProcessResourcePool pool = selectCalendarProcessPool(resourcePoolMap, processLineIds, processId);
            MesProScheduleOrderProcessDO routeProcessSnapshot = resolveRouteProcessSnapshot(processTasks, extMap, scheduleOrderProcessMap);
            BigDecimal effectiveHourlyCapacity = pool != null ? pool.effectiveHourlyCapacity : resolveRouteProcessEffectiveHourlyCapacity(routeProcessSnapshot);
            LocalDateTime startTime = processTasks.stream().map(MesProTaskDO::getStartTime).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime endTime = processTasks.stream().map(MesProTaskDO::getEndTime).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
            processItems.add(MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem.builder()
                    .processId(processId)
                    .processName(resolveCalendarProcessName(process, routeProcessSnapshot, processId))
                    .processSort(processSort++)
                    .scheduledQuantity(processTasks.stream().map(MesProTaskDO::getQuantity).filter(Objects::nonNull)
                            .max(BigDecimal::compareTo).orElse(workOrder.getQuantity()))
                    .capacitySource(pool != null ? pool.capacitySource : routeProcessSnapshot != null ? CAPACITY_SOURCE_ROUTE_PROCESS : "WORKER")
                    .workstationCount(pool != null ? pool.workstationCount : 0)
                    .workstationNames(pool != null ? pool.workstationNames : Collections.emptyList())
                    .machineCount(pool != null ? pool.machineCount : 0)
                    .configuredWorkerCount(pool != null ? pool.configuredWorkerCount : 0)
                    .currentWorkerCount(pool != null ? pool.currentWorkerCount : 0)
                    .effectiveHourlyCapacity(effectiveHourlyCapacity)
                    .plannedDurationMinutes(effectiveHourlyCapacity != null
                            ? calculateCalendarProcessMinutes(workOrder.getQuantity(), effectiveHourlyCapacity)
                            : null)
                    .startTime(startTime)
                    .endTime(endTime)
                    .bottleneck(Boolean.FALSE)
                    .build());
        }
        markCalendarBottleneck(processItems);
        MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem bottleneck = processItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getBottleneck()))
                .findFirst()
                .orElse(null);
        Long lineId = routeIds.size() == 1 ? routeIds.iterator().next() : null;
        MesProRouteDO route = lineId == null ? null : routeMap.get(lineId);
        return MesProScheduleCalendarWorkOrderAnalysisRespVO.builder()
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .productId(workOrder.getProductId())
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .quantity(workOrder.getQuantity())
                .conflict(Boolean.FALSE)
                .conflictMessage(null)
                .lineId(lineId)
                .lineCode(lineId == null ? null : buildScheduleRouteLineCode(lineId, route))
                .lineName(buildScheduleRouteLineSummary(routeIds, routeMap))
                .startTime(tasks.stream().map(MesProTaskDO::getStartTime).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null))
                .endTime(tasks.stream().map(MesProTaskDO::getEndTime).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null))
                .bottleneckProcessId(bottleneck != null ? bottleneck.getProcessId() : null)
                .bottleneckProcessName(bottleneck != null ? bottleneck.getProcessName() : null)
                .bottleneckHourlyCapacity(bottleneck != null ? bottleneck.getEffectiveHourlyCapacity() : null)
                .processes(processItems)
                .build();
    }

    private CalendarProcessResourcePool selectCalendarProcessPool(Map<String, CalendarProcessResourcePool> resourcePoolMap,
                                                                  Set<Long> processLineIds,
                                                                  Long processId) {
        if (processId == null || CollUtil.isEmpty(processLineIds) || resourcePoolMap.isEmpty()) {
            return null;
        }
        return processLineIds.stream()
                .map(lineId -> resourcePoolMap.get(calendarLineProcessKey(lineId, processId)))
                .filter(Objects::nonNull)
                .max(Comparator.comparing((CalendarProcessResourcePool pool) -> pool.effectiveHourlyCapacity)
                        .thenComparing(pool -> ObjUtil.defaultIfNull(pool.lineId, Long.MAX_VALUE), Comparator.reverseOrder()))
                .orElse(null);
    }

    private MesProScheduleOrderProcessDO resolveRouteProcessSnapshot(List<MesProTaskDO> processTasks,
                                                                     Map<Long, MesProTaskScheduleExtDO> extMap,
                                                                     Map<Long, MesProScheduleOrderProcessDO> scheduleOrderProcessMap) {
        if (CollUtil.isEmpty(processTasks) || extMap.isEmpty() || scheduleOrderProcessMap.isEmpty()) {
            return null;
        }
        return processTasks.stream()
                .map(task -> extMap.get(task.getId()))
                .filter(Objects::nonNull)
                .map(MesProTaskScheduleExtDO::getScheduleOrderProcessId)
                .filter(Objects::nonNull)
                .map(scheduleOrderProcessMap::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal resolveRouteProcessEffectiveHourlyCapacity(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess == null || scheduleOrderProcess.getHourlyCapacityTotal() == null) {
            return BigDecimal.ZERO;
        }
        return scheduleOrderProcess.getHourlyCapacityTotal();
    }

    private String resolveCalendarProcessName(MesProProcessDO process,
                                              MesProScheduleOrderProcessDO scheduleOrderProcess,
                                              Long processId) {
        if (process != null && StrUtil.isNotBlank(process.getName())) {
            return process.getName();
        }
        if (scheduleOrderProcess != null && StrUtil.isNotBlank(scheduleOrderProcess.getProcessName())) {
            return scheduleOrderProcess.getProcessName();
        }
        return processId != null ? "工序不存在" : "";
    }

    private CalendarContext buildContext(LocalDate startDate, LocalDate endDate) {
        MesProScheduleCalendarRuleDO rule = getOrCreateRule();
        MesProScheduleCalendarSimulationDO simulation = getOrCreateSimulation();
        Map<String, String> ruleMap = parseDateShiftModeByDate(rule.getDateShiftModeByDateJson());

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        Set<String> holidayDateSet = holidayService.getHolidayList(startDateTime, endDateTime).stream()
                .filter(holiday -> ObjUtil.equal(holiday.getType(), MesCalHolidayTypeEnum.HOLIDAY.getType()))
                .map(holiday -> holiday.getDay().toLocalDate().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<MesProTaskDO> tasksBeforeEnd = taskMapper.selectListByStartTimeRange(null, endDateTime);
        Set<Long> historicalWorkOrderIds = tasksBeforeEnd.stream()
                .map(MesProTaskDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Long currentTaskCount = ObjUtil.defaultIfNull(taskMapper.selectCurrentScheduleCount(), 0L);
        MesProTaskDO latestUpdatedTask = taskMapper.selectLatestUpdatedTask();
        if (tasksBeforeEnd.isEmpty() && currentTaskCount <= 0) {
            return buildEmptyCalendarContext(rule, simulation, ruleMap, holidayDateSet);
        }
        if (tasksBeforeEnd.isEmpty()) {
            return new CalendarContext(rule, simulation, ruleMap, holidayDateSet, Collections.emptyList(),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                    currentTaskCount.intValue(), latestUpdatedTask != null ? latestUpdatedTask.getUpdateTime() : null, true);
        }
        List<MesProScheduleOrderDO> activeScheduleOrders = scheduleOrderMapper.selectEffectiveListByWorkOrderIds(historicalWorkOrderIds);
        activeScheduleOrders = activeScheduleOrders.stream()
                .filter(item -> !ObjUtil.equal(item.getStatus(), cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum.FINISHED.getStatus()))
                .filter(item -> !ObjUtil.equal(item.getStatus(), cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum.CANCELED.getStatus()))
                .toList();
        Set<Long> latestReplanScheduleOrderIds = resolveLatestAppliedReplanScheduleOrderIds();
        if (CollUtil.isNotEmpty(latestReplanScheduleOrderIds)) {
            activeScheduleOrders = activeScheduleOrders.stream()
                    .filter(item -> latestReplanScheduleOrderIds.contains(item.getId()))
                    .toList();
        }
        Map<Long, MesProScheduleOrderDO> scheduleOrderMapByWorkOrderId = activeScheduleOrders.stream()
                .filter(item -> item.getWorkOrderId() != null)
                .collect(Collectors.toMap(MesProScheduleOrderDO::getWorkOrderId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Set<Long> activeScheduleOrderIds = activeScheduleOrders.stream()
                .map(MesProScheduleOrderDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> activeWorkOrderIds = activeScheduleOrders.stream()
                .map(item -> item.getWorkOrderId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProTaskScheduleExtDO> taskExtMapBeforeEnd = CollUtil.isEmpty(latestReplanScheduleOrderIds)
                ? Collections.emptyMap()
                : taskScheduleExtMapper.selectListByTaskIds(tasksBeforeEnd.stream().map(MesProTaskDO::getId).toList()).stream()
                .collect(Collectors.toMap(MesProTaskScheduleExtDO::getTaskId, item -> item, (first, ignored) -> first));
        List<MesProTaskDO> activeTasksBeforeEnd = tasksBeforeEnd.stream()
                .filter(task -> task.getWorkOrderId() != null && activeWorkOrderIds.contains(task.getWorkOrderId()))
                .filter(task -> CollUtil.isEmpty(latestReplanScheduleOrderIds)
                        || taskBelongsToScheduleOrderScope(task, taskExtMapBeforeEnd, activeScheduleOrderIds))
                .toList();
        if (activeTasksBeforeEnd.isEmpty()) {
            return buildEmptyCalendarContext(rule, simulation, ruleMap, holidayDateSet);
        }
        currentTaskCount = (long) activeTasksBeforeEnd.size();
        latestUpdatedTask = activeTasksBeforeEnd.stream()
                .filter(task -> task.getUpdateTime() != null)
                .max(Comparator.comparing(MesProTaskDO::getUpdateTime).thenComparing(MesProTaskDO::getId))
                .orElse(activeTasksBeforeEnd.stream().max(Comparator.comparing(MesProTaskDO::getId)).orElse(null));
        List<MesProTaskDO> visibleTasks = activeTasksBeforeEnd.stream()
                .filter(task -> overlapsRange(task, startDateTime, endDateTime))
                .toList();
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderService.getWorkOrderMap(activeWorkOrderIds);
        validateIdsFound(activeWorkOrderIds, workOrderMap, PRO_WORK_ORDER_NOT_EXISTS);

        Map<Long, Map<Long, BigDecimal>> materialDemandByWorkOrderId = buildProductionMaterialDemandMap(workOrderMap);
        Set<Long> materialIds = materialDemandByWorkOrderId.values().stream()
                .flatMap(materialDemand -> materialDemand.keySet().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, BigDecimal> availableStockByItemId = ObjUtil.defaultIfNull(
                        materialStockMapper.selectListByItemIds(materialIds),
                        Collections.<MesWmMaterialStockDO>emptyList())
                .stream()
                .collect(Collectors.groupingBy(MesWmMaterialStockDO::getItemId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, MesWmMaterialStockDO::getQuantity, BigDecimal::add)));
        Map<Long, LocalDate> firstStartDateByWorkOrderId = buildFirstStartDateByWorkOrder(activeTasksBeforeEnd);
        Map<String, List<DailyMaterialSummaryRow>> materialRowsByDate = buildMaterialRowsByDate(
                startDate, endDate, activeTasksBeforeEnd, workOrderMap, materialDemandByWorkOrderId, availableStockByItemId);

        if (visibleTasks.isEmpty()) {
            Map<Long, MesMdItemDO> itemMap = materialIds.isEmpty()
                    ? Collections.emptyMap()
                    : itemService.getItemMap(materialIds);
            return new CalendarContext(rule, simulation, ruleMap, holidayDateSet, Collections.emptyList(),
                    Collections.emptyMap(), Collections.emptyMap(), workOrderMap,
                    Collections.emptyMap(), Collections.emptyMap(), itemMap, materialRowsByDate, scheduleOrderMapByWorkOrderId,
                    materialDemandByWorkOrderId, availableStockByItemId, firstStartDateByWorkOrderId,
                    currentTaskCount.intValue(), latestUpdatedTask != null ? latestUpdatedTask.getUpdateTime() : null, currentTaskCount > 0);
        }

        Set<Long> workstationIds = visibleTasks.stream().map(MesProTaskDO::getWorkstationId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdWorkstationDO> workstationMap = workstationIds.isEmpty()
                ? Collections.emptyMap()
                : workstationMapper.selectByIds(workstationIds).stream()
                .collect(Collectors.toMap(MesMdWorkstationDO::getId, Function.identity()));

        for (MesMdWorkstationDO workstation : workstationMap.values()) {
            if (workstation.getWorkshopId() == null) {
                throw exception(MD_WORKSHOP_NOT_EXISTS);
            }
        }

        Set<Long> lineIds = workstationMap.values().stream().map(MesMdWorkstationDO::getProductionLineId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdProductionLineDO> lineMap = lineIds.isEmpty()
                ? Collections.emptyMap()
                : productionLineService.getProductionLineMap(lineIds);
        validateIdsFound(lineIds, lineMap, MD_PRODUCTION_LINE_NOT_EXISTS);
        for (MesMdProductionLineDO line : lineMap.values()) {
            if (line.getCalendarPlanId() == null) {
                throw exception(PRO_AUTO_SCHEDULE_CALENDAR_REQUIRED);
            }
        }

        Set<Long> workshopIds = workstationMap.values().stream().map(MesMdWorkstationDO::getWorkshopId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdWorkshopDO> workshopMap = workshopIds.isEmpty()
                ? Collections.emptyMap()
                : workshopService.getWorkshopMap(workshopIds);
        validateIdsFound(workshopIds, workshopMap, MD_WORKSHOP_NOT_EXISTS);

        Set<Long> processIds = visibleTasks.stream().map(MesProTaskDO::getProcessId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);

        Set<Long> routeIds = visibleTasks.stream().map(MesProTaskDO::getRouteId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProRouteDO> routeMap = routeIds.isEmpty()
                ? Collections.emptyMap()
                : ObjUtil.defaultIfNull(scheduleDefaultCompatibilityPolicy.historicalReadRouteMapIgnoreDeleted(
                () -> routeService.getRouteMapIgnoreDeleted(routeIds)), Collections.emptyMap());

        Map<Long, MesProTaskScheduleExtDO> extMap = taskScheduleExtMapper.selectListByTaskIds(
                visibleTasks.stream().map(MesProTaskDO::getId).toList()).stream()
                .collect(Collectors.toMap(MesProTaskScheduleExtDO::getTaskId, item -> item));
        Set<Long> scheduleOrderProcessIds = extMap.values().stream()
                .map(MesProTaskScheduleExtDO::getScheduleOrderProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProScheduleOrderProcessDO> scheduleOrderProcessMap = scheduleOrderProcessIds.isEmpty()
                ? Collections.emptyMap()
                : ObjUtil.defaultIfNull(scheduleOrderProcessMapper.selectByIds(scheduleOrderProcessIds),
                        Collections.<MesProScheduleOrderProcessDO>emptyList()).stream()
                .collect(Collectors.toMap(MesProScheduleOrderProcessDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, TaskExecutionSummary> executionSummaryByTaskId = buildExecutionSummaryByTaskId(visibleTasks);

        Set<Long> planIds = lineMap.values().stream().map(MesMdProductionLineDO::getCalendarPlanId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, List<MesCalPlanShiftDO>> shiftsByPlanId = new LinkedHashMap<>();
        for (Long planId : planIds) {
            shiftsByPlanId.put(planId, planShiftService.getPlanShiftListByPlanId(planId).stream()
                    .sorted(Comparator.comparing(MesCalPlanShiftDO::getSort, Comparator.nullsLast(Integer::compareTo)))
                    .toList());
        }

        List<MesProScheduleIssueDO> issues = ObjUtil.defaultIfNull(
                scheduleIssueMapper.selectListByWorkOrderIds(workOrderMap.keySet()),
                Collections.emptyList());
        Set<Long> itemIds = new LinkedHashSet<>();
        visibleTasks.stream().map(MesProTaskDO::getItemId).filter(Objects::nonNull).forEach(itemIds::add);
        materialIds.forEach(itemIds::add);
        Set<Long> issueMaterialIds = issues.stream().map(MesProScheduleIssueDO::getMaterialId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        issueMaterialIds.forEach(itemIds::add);
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(itemIds);
        Map<String, List<MesProScheduleIssueDO>> issuesByDate = new LinkedHashMap<>();
        for (MesProScheduleIssueDO issue : issues) {
            String issueDate = resolveIssueDate(issue, workOrderMap, activeTasksBeforeEnd);
            if (issueDate == null) {
                continue;
            }
            issuesByDate.computeIfAbsent(issueDate, key -> new ArrayList<>()).add(issue);
        }

        List<TaskCalendarRow> taskRows = visibleTasks.stream()
                .map(task -> toTaskRow(task, workOrderMap, workstationMap, lineMap, workshopMap, processMap,
                        routeMap, itemMap, extMap, scheduleOrderProcessMap, shiftsByPlanId,
                        scheduleOrderMapByWorkOrderId, executionSummaryByTaskId))
                .toList();
        validateCapacityPlanCoverage(startDateTime, endDateTime, taskRows);
        Map<String, List<TaskCalendarRow>> tasksByDate = buildTasksByDate(
                startDate, endDate, taskRows, rule, ruleMap, holidayDateSet);

        return new CalendarContext(rule, simulation, ruleMap, holidayDateSet, taskRows, tasksByDate, issuesByDate,
                workOrderMap, lineMap, workshopMap, itemMap, materialRowsByDate, scheduleOrderMapByWorkOrderId,
                materialDemandByWorkOrderId, availableStockByItemId, firstStartDateByWorkOrderId, currentTaskCount.intValue(),
                latestUpdatedTask != null ? latestUpdatedTask.getUpdateTime() : null, currentTaskCount > 0);
    }

    private CalendarContext buildEmptyCalendarContext(MesProScheduleCalendarRuleDO rule,
                                                      MesProScheduleCalendarSimulationDO simulation,
                                                      Map<String, String> ruleMap,
                                                      Set<String> holidayDateSet) {
        return new CalendarContext(rule, simulation, ruleMap, holidayDateSet, Collections.emptyList(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                0, null, false);
    }

    private Set<Long> resolveLatestAppliedReplanScheduleOrderIds() {
        MesProReplanExplanationSnapshotDO snapshot = replanExplanationSnapshotMapper.selectLatest();
        if (snapshot == null || StrUtil.isBlank(snapshot.getSnapshotJson())) {
            return Collections.emptySet();
        }
        MesProReplanExplanationRespVO explanation = JsonUtils.parseObject(
                snapshot.getSnapshotJson(), MesProReplanExplanationRespVO.class);
        if (explanation == null || CollUtil.isEmpty(explanation.getOrders())) {
            return Collections.emptySet();
        }
        return explanation.getOrders().stream()
                .map(MesProReplanExplanationRespVO.OrderItem::getScheduleOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean taskBelongsToScheduleOrderScope(MesProTaskDO task,
                                                    Map<Long, MesProTaskScheduleExtDO> taskExtMap,
                                                    Set<Long> activeScheduleOrderIds) {
        MesProTaskScheduleExtDO ext = taskExtMap.get(task.getId());
        return ext != null
                && ext.getScheduleOrderId() != null
                && activeScheduleOrderIds.contains(ext.getScheduleOrderId());
    }

    private Map<Long, LocalDate> buildFirstStartDateByWorkOrder(List<MesProTaskDO> tasksBeforeEnd) {
        Map<Long, LocalDate> firstStartDateByWorkOrder = new LinkedHashMap<>();
        for (MesProTaskDO task : ObjUtil.defaultIfNull(tasksBeforeEnd, Collections.<MesProTaskDO>emptyList())) {
            if (task.getWorkOrderId() == null || task.getStartTime() == null) {
                continue;
            }
            LocalDate taskDate = task.getStartTime().toLocalDate();
            firstStartDateByWorkOrder.merge(task.getWorkOrderId(), taskDate,
                    (oldDate, newDate) -> oldDate.isAfter(newDate) ? newDate : oldDate);
        }
        return firstStartDateByWorkOrder;
    }

    private TaskCalendarRow toTaskRow(MesProTaskDO task,
                                      Map<Long, MesProWorkOrderDO> workOrderMap,
                                      Map<Long, MesMdWorkstationDO> workstationMap,
                                      Map<Long, MesMdProductionLineDO> lineMap,
                                      Map<Long, MesMdWorkshopDO> workshopMap,
                                      Map<Long, MesProProcessDO> processMap,
                                      Map<Long, MesProRouteDO> routeMap,
                                      Map<Long, MesMdItemDO> itemMap,
                                      Map<Long, MesProTaskScheduleExtDO> extMap,
                                      Map<Long, MesProScheduleOrderProcessDO> scheduleOrderProcessMap,
                                      Map<Long, List<MesCalPlanShiftDO>> shiftsByPlanId,
                                      Map<Long, MesProScheduleOrderDO> scheduleOrderMapByWorkOrderId,
                                      Map<Long, TaskExecutionSummary> executionSummaryByTaskId) {
        MesProWorkOrderDO workOrder = workOrderMap.get(task.getWorkOrderId());
        MesMdWorkstationDO workstation = workstationMap.get(task.getWorkstationId());
        MesMdProductionLineDO line = workstation != null ? lineMap.get(workstation.getProductionLineId()) : null;
        MesMdWorkshopDO workshop = workstation != null ? workshopMap.get(workstation.getWorkshopId()) : null;
        boolean workstationMissing = task.getWorkstationId() != null && workstation == null;
        MesProProcessDO process = processMap.get(task.getProcessId());
        MesProRouteDO route = routeMap.get(task.getRouteId());
        MesMdItemDO item = itemMap.get(task.getItemId());
        MesProTaskScheduleExtDO ext = extMap.get(task.getId());
        MesProScheduleOrderProcessDO scheduleOrderProcess = ext != null && ext.getScheduleOrderProcessId() != null
                ? scheduleOrderProcessMap.get(ext.getScheduleOrderProcessId()) : null;
        MesProScheduleOrderDO scheduleOrder = scheduleOrderMapByWorkOrderId.get(task.getWorkOrderId());
        TaskExecutionSummary executionSummary = executionSummaryByTaskId.getOrDefault(task.getId(), TaskExecutionSummary.empty());
        String shiftCode = resolveShiftCode(task, line, shiftsByPlanId);
        Long processId = task.getProcessId() != null ? task.getProcessId()
                : (scheduleOrderProcess != null ? scheduleOrderProcess.getProcessId() : null);
        String processName = resolveCalendarProcessName(process, scheduleOrderProcess, processId);
        boolean workshopMissing = workstation != null && workstation.getWorkshopId() != null && workshop == null;

        return TaskCalendarRow.builder()
                .taskId(task.getId())
                .taskCode(task.getCode())
                .workOrderId(task.getWorkOrderId())
                .workOrderCode(workOrder != null ? workOrder.getCode() : "")
                .routeId(task.getRouteId())
                .routeName(task.getRouteId() == null ? "" : buildScheduleRouteLineLabel(task.getRouteId(), route))
                .workshopId(workshop != null ? workshop.getId() : null)
                .workshopCode(workshop != null ? workshop.getCode() : "")
                .workshopName(workshop != null ? workshop.getName() : (workstationMissing ? "工作站不存在" : (workshopMissing ? "车间不存在" : "未绑定车间")))
                .resourceLineId(line != null ? line.getId() : null)
                .lineId(task.getRouteId())
                .lineCode(buildScheduleRouteLineCode(task.getRouteId(), route))
                .lineName(task.getRouteId() == null ? "未绑定工艺路线" : buildScheduleRouteLineLabel(task.getRouteId(), route))
                .processId(processId)
                .processName(processName)
                .scheduleOrderProcessId(ext != null ? ext.getScheduleOrderProcessId() : null)
                .shiftCapacityTotal(scheduleOrderProcess != null ? scheduleOrderProcess.getShiftCapacityTotal() : null)
                .capacitySource(scheduleOrderProcess != null ? scheduleOrderProcess.getCapacitySource() : null)
                .itemCode(item != null ? item.getCode() : "")
                .itemName(item != null ? item.getName() : (task.getItemId() != null ? "物料不存在" : ""))
                .quantity(task.getQuantity())
                .reportedQuantity(executionSummary.reportedQuantity)
                .pendingInspectionQuantity(executionSummary.pendingInspectionQuantity)
                .executionStatus(resolveExecutionStatus(task.getQuantity(), executionSummary, scheduleOrder))
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .updateTime(task.getUpdateTime())
                .dateText(task.getStartTime() != null ? task.getStartTime().toLocalDate().toString() : "")
                .shiftCode(shiftCode)
                .scheduleSource(ext != null ? ObjUtil.defaultIfNull(ext.getScheduleSource(), "MANUAL") : "MANUAL")
                .locked(ext != null ? ObjUtil.defaultIfNull(ext.getLocked(), Boolean.FALSE) : Boolean.FALSE)
                .riskStatus(ext != null ? ObjUtil.defaultIfNull(ext.getRiskStatus(), "") : "")
                .scheduleOrderFrozen(scheduleOrder != null ? ObjUtil.defaultIfNull(scheduleOrder.getFrozen(), Boolean.FALSE) : Boolean.FALSE)
                .scheduleOrderFreezeReason(scheduleOrder != null ? ObjUtil.defaultIfNull(scheduleOrder.getFreezeReason(), "") : "")
                .build();
    }

    private Map<Long, TaskExecutionSummary> buildExecutionSummaryByTaskId(List<MesProTaskDO> visibleTasks) {
        Set<Long> taskIds = visibleTasks.stream().map(MesProTaskDO::getId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, TaskExecutionSummary> result = new LinkedHashMap<>();
        for (MesProFeedbackDO feedback : feedbackMapper.selectListByTaskIds(taskIds)) {
            if (feedback.getTaskId() == null || !isCountedFeedbackStatus(feedback.getStatus())) {
                continue;
            }
            TaskExecutionSummary summary = result.computeIfAbsent(feedback.getTaskId(), ignored -> TaskExecutionSummary.empty());
            summary.reportedQuantity = summary.reportedQuantity.add(ObjUtil.defaultIfNull(feedback.getFeedbackQuantity(), BigDecimal.ZERO));
            if (ObjUtil.equal(feedback.getStatus(), MesProFeedbackStatusEnum.UNCHECK.getStatus())) {
                summary.pendingInspectionQuantity = summary.pendingInspectionQuantity.add(ObjUtil.defaultIfNull(feedback.getUncheckQuantity(), BigDecimal.ZERO));
            }
        }
        return result;
    }

    private boolean isCountedFeedbackStatus(Integer status) {
        return ObjUtil.equal(status, MesProFeedbackStatusEnum.APPROVING.getStatus())
                || ObjUtil.equal(status, MesProFeedbackStatusEnum.UNCHECK.getStatus())
                || ObjUtil.equal(status, MesProFeedbackStatusEnum.FINISHED.getStatus());
    }

    private String resolveExecutionStatus(BigDecimal plannedQuantity, TaskExecutionSummary executionSummary, MesProScheduleOrderDO scheduleOrder) {
        if (scheduleOrder != null && ObjUtil.equal(scheduleOrder.getFrozen(), Boolean.TRUE)) {
            return "FROZEN";
        }
        if (hasPositive(executionSummary.pendingInspectionQuantity)) {
            return "PENDING_INSPECTION";
        }
        if (!hasPositive(executionSummary.reportedQuantity)) {
            return "NOT_STARTED";
        }
        if (plannedQuantity != null && executionSummary.reportedQuantity.compareTo(plannedQuantity) >= 0) {
            return "COMPLETED";
        }
        return "IN_PROGRESS";
    }

    private String resolveShiftCode(MesProTaskDO task,
                                    MesMdProductionLineDO line,
                                    Map<Long, List<MesCalPlanShiftDO>> shiftsByPlanId) {
        if (task.getStartTime() == null) {
            return MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY;
        }
        if (line != null && line.getCalendarPlanId() != null) {
            List<MesCalPlanShiftDO> shifts = shiftsByPlanId.getOrDefault(line.getCalendarPlanId(), Collections.emptyList());
            LocalDate taskDate = task.getStartTime().toLocalDate();
            for (MesCalPlanShiftDO shift : shifts) {
                LocalDateTime shiftStart = buildShiftDateTime(taskDate, shift.getStartTime());
                LocalDateTime shiftEnd = buildShiftDateTime(taskDate, shift.getEndTime());
                if (!shiftEnd.isAfter(shiftStart)) {
                    shiftEnd = shiftEnd.plusDays(1);
                }
                if ((task.getStartTime().isEqual(shiftStart) || task.getStartTime().isAfter(shiftStart))
                        && task.getStartTime().isBefore(shiftEnd)) {
                    if (shifts.size() == 1 && isWideDayShiftCoveringNight(shiftStart, shiftEnd, shift)) {
                        return resolveShiftCodeByStartHour(task.getStartTime());
                    }
                    return normalizeShiftCode(shift);
                }
            }
            if (shifts.size() == 1) {
                return MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY;
            }
        }
        return resolveShiftCodeByStartHour(task.getStartTime());
    }

    private boolean isWideDayShiftCoveringNight(LocalDateTime shiftStart, LocalDateTime shiftEnd, MesCalPlanShiftDO shift) {
        return MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY.equals(normalizeShiftCode(shift))
                && !shiftStart.toLocalTime().isAfter(LocalTime.of(8, 0))
                && shiftEnd.toLocalTime().isAfter(LocalTime.of(18, 0));
    }

    private String resolveShiftCodeByStartHour(LocalDateTime startTime) {
        int hour = startTime.getHour();
        return hour >= 18 || hour < 6
                ? MesProScheduleCalendarRuleSupport.DATE_SHIFT_NIGHT
                : MesProScheduleCalendarRuleSupport.DATE_SHIFT_DAY;
    }

    private String normalizeShiftCode(MesCalPlanShiftDO shift) {
        return MesProScheduleCalendarRuleSupport.resolveShiftCode(shift);
    }

    private String resolveIssueDate(MesProScheduleIssueDO issue,
                                    Map<Long, MesProWorkOrderDO> workOrderMap,
                                    List<MesProTaskDO> tasks) {
        if (issue.getCalendarDate() != null) {
            return issue.getCalendarDate().toLocalDate().toString();
        }
        if (issue.getTaskId() != null) {
            return tasks.stream()
                    .filter(task -> ObjUtil.equal(task.getId(), issue.getTaskId()))
                    .map(task -> task.getStartTime() != null ? task.getStartTime().toLocalDate().toString() : null)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        MesProWorkOrderDO workOrder = workOrderMap.get(issue.getWorkOrderId());
        if (workOrder != null && workOrder.getRequestDate() != null) {
            return workOrder.getRequestDate().toLocalDate().toString();
        }
        return null;
    }

    private MesProScheduleCalendarRulesRespVO toRulesResp(MesProScheduleCalendarRuleDO rule,
                                                          MesProScheduleCalendarSimulationDO simulation) {
        MesProScheduleCalendarRulesRespVO response = new MesProScheduleCalendarRulesRespVO();
        response.setId(rule.getId());
        response.setSkipStatutoryHolidays(rule.getSkipStatutoryHolidays());
        response.setWeekendRestMode(rule.getWeekendRestMode());
        response.setDateShiftModeByDate(parseDateShiftModeByDate(rule.getDateShiftModeByDateJson()));
        response.setSimulationCurrentDate(simulation.getCurrentDate().toLocalDate().toString());
        response.setTemporaryFreezeEnabled(rule.getTemporaryFreezeEnabled());
        response.setCalendarContextToken(MesProScheduleCalendarRuleSupport.buildCalendarContextToken(response));
        return response;
    }

    private MesProScheduleCalendarRuleDO getOrCreateRule() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProScheduleCalendarRuleDO rule = scheduleCalendarRuleMapper.selectByTenantId(tenantId);
        if (rule != null) {
            return rule;
        }
        MesProScheduleCalendarRuleDO create = MesProScheduleCalendarRuleDO.builder()
                .skipStatutoryHolidays(Boolean.FALSE)
                .weekendRestMode(MesProScheduleCalendarRuleSupport.WEEKEND_MODE_SINGLE)
                .dateShiftModeByDateJson("{}")
                .temporaryFreezeEnabled(Boolean.FALSE)
                .remark("INIT")
                .build();
        create.setTenantId(tenantId);
        scheduleCalendarRuleMapper.insert(create);
        return create;
    }

    private MesProScheduleCalendarSimulationDO getOrCreateSimulation() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProScheduleCalendarSimulationDO simulation = scheduleCalendarSimulationMapper.selectByTenantId(tenantId);
        if (simulation != null) {
            LocalDate today = LocalDate.now();
            if (simulation.getCurrentDate() == null || simulation.getCurrentDate().toLocalDate().isBefore(today)) {
                simulation.setCurrentDate(today.atStartOfDay());
                scheduleCalendarSimulationMapper.updateById(simulation);
            }
            return simulation;
        }
        MesProScheduleCalendarSimulationDO create = MesProScheduleCalendarSimulationDO.builder()
                .currentDate(LocalDate.now().atStartOfDay())
                .remark("INIT")
                .build();
        create.setTenantId(tenantId);
        scheduleCalendarSimulationMapper.insert(create);
        return create;
    }

    private YearMonth parseMonth(String monthText) {
        try {
            return YearMonth.parse(monthText);
        } catch (Exception e) {
            throw exception(PRO_SCHEDULE_CALENDAR_INVALID_MONTH);
        }
    }

    private LocalDate parseDate(String dateText) {
        try {
            return LocalDate.parse(dateText);
        } catch (Exception e) {
            throw exception(PRO_SCHEDULE_CALENDAR_INVALID_DATE);
        }
    }

    private String normalizeWeekendRestMode(String mode) {
        return MesProScheduleCalendarRuleSupport.normalizeWeekendRestMode(mode);
    }

    private Map<String, String> normalizeDateShiftModeByDate(Map<String, String> input) {
        return MesProScheduleCalendarRuleSupport.normalizeDateShiftModeByDate(input);
    }

    private Map<String, String> parseDateShiftModeByDate(String json) {
        return MesProScheduleCalendarRuleSupport.parseDateShiftModeByDate(json);
    }

    private String resolveDateShiftMode(LocalDate date,
                                        MesProScheduleCalendarRuleDO rule,
                                        Map<String, String> dateShiftModeByDate,
                                        Set<String> holidayDateSet) {
        return MesProScheduleCalendarRuleSupport.resolveDateShiftMode(
                date,
                rule.getSkipStatutoryHolidays(),
                rule.getWeekendRestMode(),
                dateShiftModeByDate,
                holidayDateSet);
    }

    private LocalDateTime buildShiftDateTime(LocalDate date, String hhmm) {
        LocalTime time = LocalTime.parse(hhmm != null && hhmm.length() == 5 ? hhmm : "00:00");
        return LocalDateTime.of(date, time);
    }

    private List<MesMdProductionLineDO> loadCapacityGenerationLines(MesProScheduleCalendarCapacityGenerateReqVO reqVO) {
        if (CollUtil.isNotEmpty(reqVO.getLineIds())) {
            Set<Long> lineIds = new LinkedHashSet<>(reqVO.getLineIds());
            Map<Long, MesMdProductionLineDO> lineMap = productionLineMapper.selectListByIds(lineIds).stream()
                    .collect(Collectors.toMap(MesMdProductionLineDO::getId, Function.identity()));
            validateIdsFound(lineIds, lineMap, MD_PRODUCTION_LINE_NOT_EXISTS);
            return lineMap.values().stream()
                    .sorted(Comparator.comparing(MesMdProductionLineDO::getId))
                    .toList();
        }
        return productionLineMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .filter(line -> line.getCalendarPlanId() != null)
                .sorted(Comparator.comparing(MesMdProductionLineDO::getId))
                .toList();
    }

    private Map<Long, List<MesCalPlanShiftDO>> loadPlanShifts(List<MesMdProductionLineDO> lines) {
        Set<Long> planIds = lines.stream()
                .map(MesMdProductionLineDO::getCalendarPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<MesCalPlanShiftDO>> shiftsByPlanId = new LinkedHashMap<>();
        for (Long planId : planIds) {
            shiftsByPlanId.put(planId, planShiftService.getPlanShiftListByPlanId(planId).stream()
                    .filter(shift -> shift.getId() != null)
                    .sorted(Comparator
                            .comparing(MesCalPlanShiftDO::getSort, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(shift -> ObjUtil.defaultIfNull(shift.getStartTime(), "00:00"))
                            .thenComparing(MesCalPlanShiftDO::getId))
                    .toList());
        }
        return shiftsByPlanId;
    }

    private MesCalPlanShiftDO firstShiftForLine(MesMdProductionLineDO line,
                                                Map<Long, List<MesCalPlanShiftDO>> shiftsByPlanId) {
        List<MesCalPlanShiftDO> shifts = shiftsByPlanId.get(line.getCalendarPlanId());
        if (CollUtil.isEmpty(shifts)) {
            throw exception0(400, "产线 " + line.getName() + " 未配置排班班次，无法同步班时");
        }
        return shifts.get(0);
    }

    private String calculateShiftEndTime(String startTime, Integer capacityMinutes) {
        if (startTime == null || startTime.length() != 5) {
            throw exception0(400, "排班班次开始时间必须为 HH:mm 格式");
        }
        return LocalTime.parse(startTime).plusMinutes(capacityMinutes).toString();
    }

    private Integer calculateShiftCapacityMinutes(LocalDate date, MesCalPlanShiftDO shift) {
        LocalDateTime shiftStart = buildShiftDateTime(date, shift.getStartTime());
        LocalDateTime shiftEnd = buildShiftDateTime(date, shift.getEndTime());
        if (!shiftEnd.isAfter(shiftStart)) {
            shiftEnd = shiftEnd.plusDays(1);
        }
        long minutes = Duration.between(shiftStart, shiftEnd).toMinutes();
        return minutes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) minutes;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : LocalDateTimeUtil.format(value, DatePattern.NORM_DATETIME_PATTERN);
    }

    private boolean overlapsRange(MesProTaskDO task, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return task != null
                && task.getStartTime() != null
                && task.getEndTime() != null
                && task.getStartTime().isBefore(rangeEnd)
                && task.getEndTime().isAfter(rangeStart);
    }

    private Map<Long, Map<Long, BigDecimal>> buildProductionMaterialDemandMap(
            Map<Long, MesProWorkOrderDO> workOrderMap) {
        List<MesKingdeeProductionMaterialListDO> rows = ObjUtil.defaultIfNull(
                productionMaterialListMapper.selectListByWorkOrderIds(workOrderMap.keySet()),
                Collections.<MesKingdeeProductionMaterialListDO>emptyList());
        Map<Long, List<MesKingdeeProductionMaterialListDO>> rowsByWorkOrderId = rows.stream()
                .filter(row -> row.getWorkOrderId() != null)
                .collect(Collectors.groupingBy(MesKingdeeProductionMaterialListDO::getWorkOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, Map<Long, BigDecimal>> demandByWorkOrderId = new LinkedHashMap<>();
        for (MesProWorkOrderDO workOrder : workOrderMap.values()) {
            List<MesKingdeeProductionMaterialListDO> workOrderRows = rowsByWorkOrderId
                    .getOrDefault(workOrder.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(workOrderRows)) {
                throw exception0(PRO_SCHEDULE_CALENDAR_PRODUCTION_MATERIAL_REQUIRED.getCode(),
                        "排程工单缺少生产用料清单: {}", buildWorkOrderLabel(workOrder));
            }
            Map<Long, BigDecimal> demandByItemId = new LinkedHashMap<>();
            for (MesKingdeeProductionMaterialListDO row : workOrderRows) {
                if (row.getChildMaterialId() == null) {
                    throw exception0(PRO_SCHEDULE_CALENDAR_PRODUCTION_MATERIAL_REQUIRED.getCode(),
                            "排程工单生产用料清单子项未映射本地物料: {}",
                            buildProductionMaterialRowLabel(workOrder, row));
                }
                if (row.getRequiredQuantity() == null) {
                    throw exception0(PRO_SCHEDULE_CALENDAR_PRODUCTION_MATERIAL_REQUIRED.getCode(),
                            "排程工单生产用料清单缺少应发数量: {}",
                            buildProductionMaterialRowLabel(workOrder, row));
                }
                demandByItemId.merge(row.getChildMaterialId(), row.getRequiredQuantity(), BigDecimal::add);
            }
            demandByWorkOrderId.put(workOrder.getId(), demandByItemId);
        }
        return demandByWorkOrderId;
    }

    private String buildWorkOrderLabel(MesProWorkOrderDO workOrder) {
        if (workOrder == null) {
            return "未知工单";
        }
        if (workOrder.getCode() != null && !workOrder.getCode().isBlank()) {
            return workOrder.getCode();
        }
        if (workOrder.getId() != null) {
            return "工单#" + workOrder.getId();
        }
        return "未知工单";
    }

    private String buildProductionMaterialRowLabel(MesProWorkOrderDO workOrder,
                                                   MesKingdeeProductionMaterialListDO row) {
        StringBuilder label = new StringBuilder(buildWorkOrderLabel(workOrder));
        if (row == null) {
            return label.toString();
        }
        String materialCode = row.getChildMaterialCode();
        if (materialCode != null && !materialCode.isBlank()) {
            label.append(" / ").append(materialCode);
            return label.toString();
        }
        if (row.getChildMaterialId() != null) {
            label.append(" / 物料#").append(row.getChildMaterialId());
        }
        return label.toString();
    }

    private Map<String, List<DailyMaterialSummaryRow>> buildMaterialRowsByDate(LocalDate rangeStart,
                                                                               LocalDate rangeEnd,
                                                                               List<MesProTaskDO> tasksBeforeEnd,
                                                                               Map<Long, MesProWorkOrderDO> workOrderMap,
                                                                               Map<Long, Map<Long, BigDecimal>> materialDemandByWorkOrderId,
                                                                               Map<Long, BigDecimal> availableStockByItemId) {
        if (CollUtil.isEmpty(tasksBeforeEnd) || CollUtil.isEmpty(materialDemandByWorkOrderId)) {
            return Collections.emptyMap();
        }

        Map<Long, LocalDate> firstStartDateByWorkOrder = new LinkedHashMap<>();
        for (MesProTaskDO task : tasksBeforeEnd) {
            if (task.getWorkOrderId() == null || task.getStartTime() == null) {
                continue;
            }
            LocalDate taskDate = task.getStartTime().toLocalDate();
            firstStartDateByWorkOrder.merge(task.getWorkOrderId(), taskDate,
                    (oldDate, newDate) -> oldDate.isAfter(newDate) ? newDate : oldDate);
        }
        if (firstStartDateByWorkOrder.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<LocalDate, Map<Long, DailyMaterialAccumulator>> accumulatorsByDate = new TreeMap<>();
        for (Map.Entry<Long, LocalDate> entry : firstStartDateByWorkOrder.entrySet()) {
            Map<Long, BigDecimal> materialDemandByItemId = materialDemandByWorkOrderId.get(entry.getKey());
            if (materialDemandByItemId == null || materialDemandByItemId.isEmpty()) {
                continue;
            }
            Map<Long, DailyMaterialAccumulator> dayAccumulators = accumulatorsByDate.computeIfAbsent(entry.getValue(), key -> new LinkedHashMap<>());
            for (Map.Entry<Long, BigDecimal> demandEntry : materialDemandByItemId.entrySet()) {
                if (demandEntry.getKey() == null || demandEntry.getValue() == null) {
                    continue;
                }
                DailyMaterialAccumulator accumulator = dayAccumulators.computeIfAbsent(demandEntry.getKey(),
                        key -> new DailyMaterialAccumulator(demandEntry.getKey()));
                accumulator.scheduledUsageQty = accumulator.scheduledUsageQty.add(demandEntry.getValue());
                accumulator.workOrderIds.add(entry.getKey());
            }
        }
        if (accumulatorsByDate.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDate firstDemandDate = accumulatorsByDate.keySet().stream().min(LocalDate::compareTo).orElse(rangeStart);
        if (firstDemandDate.isAfter(rangeEnd)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> cumulativeRequiredByMaterialId = new LinkedHashMap<>();
        Map<Long, Set<Long>> cumulativeWorkOrderIdsByMaterialId = new LinkedHashMap<>();
        Map<String, List<DailyMaterialSummaryRow>> materialRowsByDate = new LinkedHashMap<>();
        for (LocalDate date = firstDemandDate; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            Map<Long, DailyMaterialAccumulator> todayAccumulators = accumulatorsByDate.getOrDefault(date, Collections.emptyMap());
            for (DailyMaterialAccumulator accumulator : todayAccumulators.values()) {
                cumulativeRequiredByMaterialId.merge(accumulator.materialId, accumulator.scheduledUsageQty, BigDecimal::add);
                cumulativeWorkOrderIdsByMaterialId
                        .computeIfAbsent(accumulator.materialId, ignored -> new LinkedHashSet<>())
                        .addAll(accumulator.workOrderIds);
            }
            if (date.isBefore(rangeStart)) {
                continue;
            }
            for (Map.Entry<Long, BigDecimal> cumulativeEntry : cumulativeRequiredByMaterialId.entrySet()) {
                Long materialId = cumulativeEntry.getKey();
                DailyMaterialAccumulator todayAccumulator = todayAccumulators.get(materialId);
                BigDecimal todayScheduledUsageQty = todayAccumulator != null ? todayAccumulator.scheduledUsageQty : BigDecimal.ZERO;
                BigDecimal totalStock = availableStockByItemId.getOrDefault(materialId, BigDecimal.ZERO);
                BigDecimal cumulativeRequiredQty = cumulativeEntry.getValue();
                BigDecimal requiredBeforeToday = cumulativeRequiredQty.subtract(todayScheduledUsageQty);
                BigDecimal remainingAvailableQty = totalStock.subtract(requiredBeforeToday);
                if (remainingAvailableQty.compareTo(BigDecimal.ZERO) < 0) {
                    remainingAvailableQty = BigDecimal.ZERO;
                }
                BigDecimal shortageQty = cumulativeRequiredQty.subtract(totalStock);
                if (shortageQty.compareTo(BigDecimal.ZERO) < 0) {
                    shortageQty = BigDecimal.ZERO;
                }
                if (!hasPositive(shortageQty)) {
                    continue;
                }
                Set<Long> cumulativeWorkOrderIds = cumulativeWorkOrderIdsByMaterialId.getOrDefault(materialId, Collections.emptySet());
                DailyMaterialSummaryRow.DailyMaterialSummaryRowBuilder rowBuilder = DailyMaterialSummaryRow.builder()
                        .date(date.toString())
                        .materialId(materialId)
                        .scheduledUsageQty(todayScheduledUsageQty)
                        .remainingAvailableQty(remainingAvailableQty)
                        .cumulativeRequiredQty(cumulativeRequiredQty)
                        .totalAvailableQty(totalStock)
                        .shortageQty(shortageQty)
                        .affectedWorkOrderCount(cumulativeWorkOrderIds.size());
                if (todayAccumulator != null && todayAccumulator.workOrderIds.size() == 1) {
                    Long singleWorkOrderId = todayAccumulator.workOrderIds.iterator().next();
                    rowBuilder.singleWorkOrderId(singleWorkOrderId);
                }
                materialRowsByDate.computeIfAbsent(date.toString(), key -> new ArrayList<>()).add(rowBuilder.build());
            }
        }
        return materialRowsByDate;
    }

    private boolean hasPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String buildMaterialSummaryMessage(DailyMaterialSummaryRow row) {
        if (row == null) {
            return "";
        }
        if (hasPositive(row.shortageQty)) {
            return "物料库存不足";
        }
        return "物料库存充足";
    }

    private void validateCapacityPlanCoverage(LocalDateTime startDateTime, LocalDateTime endDateTime, List<TaskCalendarRow> taskRows) {
        if (taskRows.isEmpty()) {
            return;
        }
        List<TaskCalendarRow> rowsWithLine = taskRows.stream()
                .filter(row -> row.getResourceLineId() != null)
                .toList();
        if (rowsWithLine.isEmpty()) {
            return;
        }
        Set<Long> lineIds = rowsWithLine.stream().map(TaskCalendarRow::getResourceLineId).collect(Collectors.toCollection(LinkedHashSet::new));
        LocalDate requiredStartDate = rowsWithLine.stream()
                .map(row -> row.startTime.toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(startDateTime.toLocalDate());
        LocalDate requiredEndDate = rowsWithLine.stream()
                .map(this::resolveTaskEndDate)
                .max(LocalDate::compareTo)
                .orElse(startDateTime.toLocalDate());
        LocalDateTime requiredStartDateTime = requiredStartDate.atStartOfDay();
        LocalDateTime requiredEndExclusive = requiredEndDate.plusDays(1).atStartOfDay();
        MesProScheduleCalendarRuleDO rule = getOrCreateRule();
        Map<String, String> ruleMap = parseDateShiftModeByDate(rule.getDateShiftModeByDateJson());
        Set<String> holidayDateSet = holidayService.getHolidayList(requiredStartDateTime, requiredEndExclusive).stream()
                .filter(holiday -> ObjUtil.equal(holiday.getType(), MesCalHolidayTypeEnum.HOLIDAY.getType()))
                .map(holiday -> holiday.getDay().toLocalDate().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<LocalDate> workingDates = new LinkedHashSet<>();
        for (LocalDate cursor = requiredStartDate; !cursor.isAfter(requiredEndDate); cursor = cursor.plusDays(1)) {
            if (!MesProScheduleCalendarRuleSupport.DATE_SHIFT_REST.equals(resolveDateShiftMode(cursor, rule, ruleMap, holidayDateSet))) {
                workingDates.add(cursor);
            }
        }
        ensureCapacityPlanCoverage(lineIds, requiredStartDate, requiredEndDate);
        List<MesProCapacityPlanDO> capacities = capacityPlanMapper.selectListByLineIdsAndDate(lineIds, requiredStartDateTime);
        Set<String> availableKeys = capacities.stream()
                .filter(capacity -> capacity.getCalendarDate() != null && capacity.getCalendarDate().isBefore(requiredEndExclusive))
                .map(capacity -> buildLineDateKey(capacity.getLineId(), capacity.getCalendarDate().toLocalDate()))
                .collect(Collectors.toSet());
        for (TaskCalendarRow row : rowsWithLine) {
            LocalDate cursor = row.startTime.toLocalDate();
            LocalDate occupancyEnd = resolveCapacityCoverageEndDate(row);
            while (!cursor.isAfter(occupancyEnd)) {
                if (workingDates.contains(cursor) && !availableKeys.contains(buildLineDateKey(row.getResourceLineId(), cursor))) {
                    throw exception(PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED);
                }
                cursor = cursor.plusDays(1);
            }
        }
    }

    private LocalDate resolveCapacityCoverageEndDate(TaskCalendarRow row) {
        if ("NIGHT".equals(row.getShiftCode()) && row.endTime != null
                && row.endTime.toLocalDate().isAfter(row.startTime.toLocalDate())) {
            return row.startTime.toLocalDate();
        }
        return resolveTaskEndDate(row);
    }

    private Map<String, List<TaskCalendarRow>> buildTasksByDate(LocalDate rangeStart,
                                                                LocalDate rangeEnd,
                                                                List<TaskCalendarRow> taskRows,
                                                                MesProScheduleCalendarRuleDO rule,
                                                                Map<String, String> ruleMap,
                                                                Set<String> holidayDateSet) {
        Map<String, List<TaskCalendarRow>> tasksByDate = new LinkedHashMap<>();
        for (TaskCalendarRow row : taskRows) {
            LocalDate cursor = row.startTime.toLocalDate();
            if (cursor.isBefore(rangeStart)) {
                cursor = rangeStart;
            }
            LocalDate occupancyEnd = resolveTaskEndDate(row);
            if (occupancyEnd.isAfter(rangeEnd)) {
                occupancyEnd = rangeEnd;
            }
            while (!cursor.isAfter(occupancyEnd)) {
                if (!MesProScheduleCalendarRuleSupport.DATE_SHIFT_REST.equals(
                        resolveDateShiftMode(cursor, rule, ruleMap, holidayDateSet))) {
                    tasksByDate.computeIfAbsent(cursor.toString(), key -> new ArrayList<>()).add(row);
                }
                cursor = cursor.plusDays(1);
            }
        }
        return tasksByDate;
    }

    private LocalDate resolveTaskEndDate(TaskCalendarRow row) {
        if (row.endTime == null || !row.endTime.isAfter(row.startTime)) {
            return row.startTime.toLocalDate();
        }
        return row.endTime.minusNanos(1).toLocalDate();
    }

    private BigDecimal calculateDailyQuantity(TaskCalendarRow row, LocalDate selectedDate) {
        if (row.quantity == null || row.startTime == null) {
            return row.quantity;
        }
        if (row.endTime == null || !row.endTime.isAfter(row.startTime)) {
            return selectedDate.equals(row.startTime.toLocalDate()) ? row.quantity : BigDecimal.ZERO;
        }
        long totalMinutes = Duration.between(row.startTime, row.endTime).toMinutes();
        if (totalMinutes <= 0) {
            return row.quantity;
        }
        LocalDateTime dayStart = selectedDate.atStartOfDay();
        LocalDateTime dayEnd = selectedDate.plusDays(1).atStartOfDay();
        LocalDateTime overlapStart = row.startTime.isAfter(dayStart) ? row.startTime : dayStart;
        LocalDateTime overlapEnd = row.endTime.isBefore(dayEnd) ? row.endTime : dayEnd;
        if (!overlapEnd.isAfter(overlapStart)) {
            return BigDecimal.ZERO;
        }
        long overlapMinutes = Duration.between(overlapStart, overlapEnd).toMinutes();
        if (overlapMinutes <= 0) {
            return BigDecimal.ZERO;
        }
        if (overlapMinutes >= totalMinutes) {
            return row.quantity;
        }
        return row.quantity
                .multiply(BigDecimal.valueOf(overlapMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private record CapacityPlanGenerationResult(
            List<MesMdProductionLineDO> lines,
            int generatedCount,
            int skippedExistingCount,
            int skippedRestCount,
            int skippedNoShiftCount,
            List<MesProScheduleCalendarCapacityGenerateRespVO.SkippedDetail> skippedDetails) {
    }

    private Map<String, CalendarProcessResourcePool> buildCalendarProcessResourcePools(List<MesMdWorkstationDO> workstations,
                                                                                       Map<Long, MesProProcessDO> processMap) {
        if (CollUtil.isEmpty(workstations)) {
            return Collections.emptyMap();
        }
        List<Long> workstationIds = workstations.stream().map(MesMdWorkstationDO::getId).toList();
        Map<Long, MesMdWorkstationCapacityMetrics> capacityMetrics = workstationCapacityService.getCapacityMetrics(workstations, BigDecimal.ONE);
        Map<Long, List<cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO>> machinesByWorkstationId = ObjUtil.defaultIfNull(
                        workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds),
                        Collections.<cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO>emptyList())
                .stream()
                .collect(Collectors.groupingBy(cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO::getWorkstationId,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, CalendarProcessResourcePool> poolMap = new LinkedHashMap<>();
        for (MesMdWorkstationDO workstation : workstations) {
            String key = calendarLineProcessKey(workstation.getProductionLineId(), workstation.getProcessId());
            CalendarProcessResourcePool pool = poolMap.computeIfAbsent(key, ignored ->
                    new CalendarProcessResourcePool(workstation.getProductionLineId(), workstation.getProcessId(),
                            processMap.get(workstation.getProcessId()) != null ? processMap.get(workstation.getProcessId()).getName() : ""));
            MesMdWorkstationCapacityMetrics metrics = capacityMetrics.getOrDefault(workstation.getId(),
                    MesMdWorkstationCapacityMetrics.builder()
                            .configuredWorkerCount(0)
                            .currentWorkerCount(0)
                            .machineryStandardHourlyCapacity(BigDecimal.ZERO)
                            .todayCapacity(BigDecimal.ZERO)
                            .build());
            pool.addWorkstation(workstation, metrics, machinesByWorkstationId.getOrDefault(workstation.getId(), Collections.emptyList()));
        }
        return poolMap;
    }

    private Integer calculateCalendarProcessMinutes(BigDecimal workOrderQuantity, BigDecimal effectiveHourlyCapacity) {
        if (workOrderQuantity == null || effectiveHourlyCapacity == null || effectiveHourlyCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return workOrderQuantity.divide(effectiveHourlyCapacity, 8, java.math.RoundingMode.UP)
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, java.math.RoundingMode.UP)
                .intValue();
    }

    private void markCalendarBottleneck(List<MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem> processItems) {
        MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem bottleneck = processItems.stream()
                .filter(item -> item.getEffectiveHourlyCapacity() != null)
                .min(Comparator.comparing(MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem::getEffectiveHourlyCapacity)
                        .thenComparing(item -> ObjUtil.defaultIfNull(item.getProcessSort(), Integer.MAX_VALUE)))
                .orElse(null);
        if (bottleneck != null) {
            bottleneck.setBottleneck(Boolean.TRUE);
        }
    }

    private String buildScheduleRouteLineSummary(Set<Long> routeIds, Map<Long, MesProRouteDO> routeMap) {
        if (CollUtil.isEmpty(routeIds)) {
            return "未绑定工艺路线";
        }
        return routeIds.stream()
                .map(routeId -> buildScheduleRouteLineLabel(routeId, routeMap.get(routeId)))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(" / "));
    }

    private String buildScheduleRouteLineLabel(Long routeId, MesProRouteDO route) {
        if (routeId == null) {
            return "未绑定工艺路线";
        }
        if (route == null) {
            return "工艺路线不存在";
        }
        return StrUtil.blankToDefault(route.getName(), StrUtil.blankToDefault(route.getCode(), "工艺路线#" + routeId));
    }

    private String buildScheduleRouteLineCode(Long routeId, MesProRouteDO route) {
        if (routeId == null || route == null) {
            return "";
        }
        return StrUtil.blankToDefault(route.getCode(), String.valueOf(routeId));
    }

    private String calendarLineProcessKey(Long lineId, Long processId) {
        return lineId + "_" + processId;
    }

    private String buildLineDateKey(Long lineId, LocalDate date) {
        return lineId + "@" + date;
    }

    private String buildLineDateShiftKey(Long lineId, LocalDate date, Long shiftId) {
        return lineId + "@" + date + "@" + shiftId;
    }

    private void validateIdsFound(Set<Long> expectedIds, Map<Long, ?> actualMap, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (expectedIds.isEmpty()) {
            return;
        }
        if (actualMap == null || !actualMap.keySet().containsAll(expectedIds)) {
            throw exception(errorCode);
        }
    }

    private static class CalendarContext {
        private final MesProScheduleCalendarRuleDO rule;
        private final MesProScheduleCalendarSimulationDO simulation;
        private final Map<String, String> ruleMap;
        private final Set<String> holidayDateSet;
        private final List<TaskCalendarRow> taskRows;
        private final Map<String, List<TaskCalendarRow>> tasksByDate;
        private final Map<String, List<MesProScheduleIssueDO>> issuesByDate;
        private final Map<Long, MesProWorkOrderDO> workOrderMap;
        private final Map<Long, MesMdProductionLineDO> lineMap;
        private final Map<Long, MesMdWorkshopDO> workshopMap;
        private final Map<Long, MesMdItemDO> itemMap;
        private final Map<String, List<DailyMaterialSummaryRow>> materialRowsByDate;
        private final Map<Long, MesProScheduleOrderDO> scheduleOrderMapByWorkOrderId;
        private final Map<Long, Map<Long, BigDecimal>> materialDemandByWorkOrderId;
        private final Map<Long, BigDecimal> availableStockByItemId;
        private final Map<Long, LocalDate> firstStartDateByWorkOrderId;
        private final Integer totalCurrentTaskCount;
        private final LocalDateTime latestUpdatedAt;
        private final Boolean hasCurrentSchedule;

        private CalendarContext(MesProScheduleCalendarRuleDO rule,
                                MesProScheduleCalendarSimulationDO simulation,
                                Map<String, String> ruleMap,
                                Set<String> holidayDateSet,
                                List<TaskCalendarRow> taskRows,
                                Map<String, List<TaskCalendarRow>> tasksByDate,
                                Map<String, List<MesProScheduleIssueDO>> issuesByDate,
                                Map<Long, MesProWorkOrderDO> workOrderMap,
                                Map<Long, MesMdProductionLineDO> lineMap,
                                Map<Long, MesMdWorkshopDO> workshopMap,
                                Map<Long, MesMdItemDO> itemMap,
                                Map<String, List<DailyMaterialSummaryRow>> materialRowsByDate,
                                Map<Long, MesProScheduleOrderDO> scheduleOrderMapByWorkOrderId,
                                Map<Long, Map<Long, BigDecimal>> materialDemandByWorkOrderId,
                                Map<Long, BigDecimal> availableStockByItemId,
                                Map<Long, LocalDate> firstStartDateByWorkOrderId,
                                Integer totalCurrentTaskCount,
                                LocalDateTime latestUpdatedAt,
                                Boolean hasCurrentSchedule) {
            this.rule = rule;
            this.simulation = simulation;
            this.ruleMap = ruleMap;
            this.holidayDateSet = holidayDateSet;
            this.taskRows = taskRows;
            this.tasksByDate = tasksByDate;
            this.issuesByDate = issuesByDate;
            this.workOrderMap = workOrderMap;
            this.lineMap = lineMap;
            this.workshopMap = workshopMap;
            this.itemMap = itemMap;
            this.materialRowsByDate = materialRowsByDate;
            this.scheduleOrderMapByWorkOrderId = scheduleOrderMapByWorkOrderId;
            this.materialDemandByWorkOrderId = materialDemandByWorkOrderId;
            this.availableStockByItemId = availableStockByItemId;
            this.firstStartDateByWorkOrderId = firstStartDateByWorkOrderId;
            this.totalCurrentTaskCount = totalCurrentTaskCount;
            this.latestUpdatedAt = latestUpdatedAt;
            this.hasCurrentSchedule = hasCurrentSchedule;
        }
    }

    private static class TaskExecutionSummary {
        private BigDecimal reportedQuantity = BigDecimal.ZERO;
        private BigDecimal pendingInspectionQuantity = BigDecimal.ZERO;

        private static TaskExecutionSummary empty() {
            return new TaskExecutionSummary();
        }
    }

    @Getter
    @Builder
    private static class DailyMaterialSummaryRow {
        private String date;
        private Long materialId;
        private Long singleWorkOrderId;
        private BigDecimal scheduledUsageQty;
        private BigDecimal remainingAvailableQty;
        private BigDecimal cumulativeRequiredQty;
        private BigDecimal totalAvailableQty;
        private BigDecimal shortageQty;
        private Integer affectedWorkOrderCount;
    }

    private static class DailyMaterialAccumulator {
        private final Long materialId;
        private BigDecimal scheduledUsageQty = BigDecimal.ZERO;
        private final Set<Long> workOrderIds = new LinkedHashSet<>();

        private DailyMaterialAccumulator(Long materialId) {
            this.materialId = materialId;
        }
    }

    private static class ProcessCapacityAccumulator {
        private final Long processId;
        private final String processName;
        private int taskCount;
        private BigDecimal scheduledQuantity = BigDecimal.ZERO;
        private final Set<Long> workOrderIds = new LinkedHashSet<>();
        private final Map<Long, BigDecimal> maxCapacityByLine = new LinkedHashMap<>();

        private ProcessCapacityAccumulator(Long processId, String processName) {
            this.processId = processId;
            this.processName = processName;
        }
    }

    @Getter
    @Builder
    private static class TaskCalendarRow {
        private Long taskId;
        private String taskCode;
        private Long workOrderId;
        private String workOrderCode;
        private Long routeId;
        private String routeName;
        private Long workshopId;
        private String workshopCode;
        private String workshopName;
        private Long resourceLineId;
        private Long lineId;
        private String lineCode;
        private String lineName;
        private Long processId;
        private String processName;
        private Long scheduleOrderProcessId;
        private BigDecimal shiftCapacityTotal;
        private String capacitySource;
        private String itemCode;
        private String itemName;
        private BigDecimal quantity;
        private BigDecimal reportedQuantity;
        private BigDecimal pendingInspectionQuantity;
        private String executionStatus;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime updateTime;
        private String dateText;
        private String shiftCode;
        private String scheduleSource;
        private Boolean locked;
        private String riskStatus;
        private Boolean scheduleOrderFrozen;
        private String scheduleOrderFreezeReason;
    }

    private static class CalendarProcessResourcePool {
        private static final String CAPACITY_SOURCE_MACHINE = "MACHINE";
        private static final String CAPACITY_SOURCE_WORKER = "WORKER";

        private final Long lineId;
        private final Long processId;
        private final String processName;
        private int workstationCount;
        private int machineCount;
        private int configuredWorkerCount;
        private int currentWorkerCount;
        private BigDecimal effectiveHourlyCapacity = BigDecimal.ZERO;
        private String capacitySource = CAPACITY_SOURCE_WORKER;
        private final List<String> workstationNames = new ArrayList<>();

        private CalendarProcessResourcePool(Long lineId, Long processId, String processName) {
            this.lineId = lineId;
            this.processId = processId;
            this.processName = processName;
        }

        private void addWorkstation(MesMdWorkstationDO workstation,
                                    MesMdWorkstationCapacityMetrics metrics,
                                    List<cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO> machines) {
            this.workstationCount += 1;
            this.workstationNames.add(workstation.getName());
            this.machineCount += machines.stream()
                    .map(cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);
            this.configuredWorkerCount += ObjUtil.defaultIfNull(metrics.getConfiguredWorkerCount(), 0);
            this.currentWorkerCount += ObjUtil.defaultIfNull(metrics.getCurrentWorkerCount(), 0);
            this.effectiveHourlyCapacity = this.effectiveHourlyCapacity.add(ObjUtil.defaultIfNull(metrics.getTodayCapacity(), BigDecimal.ZERO));
            if (this.machineCount > 0) {
                this.capacitySource = CAPACITY_SOURCE_MACHINE;
            }
        }
    }

}

