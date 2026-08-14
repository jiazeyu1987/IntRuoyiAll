package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteResourceCapacityPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchCapacityUnificationAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchPolicySettingsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchShiftHoursRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedulerworkbench.MesProSchedulerWorkbenchMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProScheduleCalendarService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID;

@Service
public class MesProSchedulerWorkbenchServiceImpl implements MesProSchedulerWorkbenchService {

    private static final String POLICY_SETTINGS_CONFIG_KEY = "mes.scheduler-workbench.policy-settings";
    private static final Set<String> PRIORITY_RULES = Set.of("PROMISE_DATE", "ORDER_PRIORITY", "CREATED_TIME");
    private static final BigDecimal CAPACITY_DIFF_EPSILON = new BigDecimal("0.000001");
    private static final String WORKER_CAPACITY_APPLICABILITY_TEXT =
            "人效h仅影响资源计算模式且产能来源为人工的工序；设备产能、手工覆盖和无限公式不受影响。人数仅作为新配置默认值，不强制重算现有工位。";
    private static final Set<String> DEFAULT_SCHEDULE_CAPACITY_MODES = Set.of(
            MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode(),
            MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode(),
            MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode());

    @Resource
    private MesProSchedulerWorkbenchMapper schedulerWorkbenchMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private ConfigService configService;
    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesProScheduleCalendarService scheduleCalendarService;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProRouteScheduleConfigService routeScheduleConfigService;
    @Resource
    private MesProSchedulerWorkbenchRuntimeStatusService runtimeStatusService;
    @Value("${mes.schedule.default-route-capacity-mode:RESOURCE_CALCULATED}")
    private String defaultRouteCapacityMode = MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode();
    @Value("${mes.schedule.capacity-audit-enabled:true}")
    private boolean capacityAuditEnabled = true;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public MesProSchedulerWorkbenchSummaryRespVO getSummary(LocalDate date) {
        LocalDateTime beginTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        MesProSchedulerWorkbenchSummaryRespVO summary = new MesProSchedulerWorkbenchSummaryRespVO();
        summary.setDate(date);
        summary.setPendingScheduleOrderCount(nvl(schedulerWorkbenchMapper.selectPendingScheduleOrderCount()));
        summary.setTodayScheduledTaskCount(nvl(schedulerWorkbenchMapper.selectTodayScheduledTaskCount(beginTime, endTime)));
        summary.setTodayPlannedCapacity(nvl(schedulerWorkbenchMapper.selectTodayPlannedCapacity(beginTime, endTime)));
        summary.setTodayFeedbackCount(nvl(schedulerWorkbenchMapper.selectTodayFeedbackCount(beginTime, endTime)));
        summary.setTodayFeedbackQuantity(nvl(schedulerWorkbenchMapper.selectTodayFeedbackQuantity(beginTime, endTime)));
        summary.setPendingApprovalFeedbackCount(nvl(schedulerWorkbenchMapper.selectPendingApprovalFeedbackCount()));
        summary.setCurrentSchedulePlannedQuantity(nvl(schedulerWorkbenchMapper.selectCurrentSchedulePlannedQuantity()));
        summary.setCurrentScheduleReportedQuantity(nvl(schedulerWorkbenchMapper.selectCurrentScheduleReportedQuantity()));
        summary.setReportedDeviationQuantity(summary.getCurrentScheduleReportedQuantity()
                .subtract(summary.getCurrentSchedulePlannedQuantity()));
        summary.setReportedDeviationText(buildDeviationText(summary.getReportedDeviationQuantity()));
        summary.setTodayAvailableCapacity(nvl(schedulerWorkbenchMapper.selectTodayAvailableCapacity(beginTime, endTime)));
        summary.setRepairingMachineryCount(nvl(schedulerWorkbenchMapper.selectRepairingMachineryCount()));
        summary.setResourceUnconfiguredCount(nvl(schedulerWorkbenchMapper.selectResourceUnconfiguredCount()));
        summary.setMaterialShortageCount(nvl(schedulerWorkbenchMapper.selectMaterialShortageCount(beginTime, endTime)));
        summary.setBlockingIssueCount(summary.getResourceUnconfiguredCount()
                + summary.getRepairingMachineryCount()
                + summary.getMaterialShortageCount());
        summary.setNightlyReplanText("每晚 02:00 自动重排；已报工、已完成、手工锁定任务保持不动。");
        summary.setTodayActionSuggestion(buildActionSuggestion(summary));
        summary.setCurrentScheduleScopeText("报工偏差按当前有效排产工单（已排产/生产中）的实际报工数量与排产数量计算；不再按当天任务段重复累计。");
        summary.setGlobalRiskScopeText(buildGlobalRiskScopeText(summary));
        summary.setSteps(buildSteps(summary));
        summary.setBottlenecks(schedulerWorkbenchMapper.selectBottlenecks(beginTime, endTime));
        summary.setReportedDeviationDetails(schedulerWorkbenchMapper.selectReportedDeviationDetails());
        summary.setRouteActiveOrders(schedulerWorkbenchMapper.selectRouteActiveOrders());
        return summary;
    }

    @Override
    public MesProSchedulerWorkbenchShiftHoursRespVO getShiftHoursSetting() {
        return buildShiftHoursSetting(workstationMapper.selectListForShiftHours(), 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProSchedulerWorkbenchShiftHoursRespVO saveShiftHoursSetting(BigDecimal shiftHours) {
        int updatedCount = workstationMapper.updateAllShiftHours(shiftHours);
        scheduleCalendarService.refreshPlanCapacityForShiftHours(shiftHours);
        return buildShiftHoursSetting(workstationMapper.selectListForShiftHours(), updatedCount);
    }

    @Override
    public MesProSchedulerWorkbenchPolicySettingsRespVO getPolicySettings() {
        ConfigDO config = configService.getConfigByKey(POLICY_SETTINGS_CONFIG_KEY);
        if (config == null) {
            return defaultPolicySettings();
        }
        return parsePolicySettings(config.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProSchedulerWorkbenchPolicySettingsRespVO savePolicySettings(
            MesProSchedulerWorkbenchPolicySettingsRespVO reqVO) {
        MesProSchedulerWorkbenchPolicySettingsRespVO normalizedReqVO = normalizePolicySettings(reqVO);
        validatePolicySettings(normalizedReqVO);
        String value = serializePolicySettings(normalizedReqVO);
        ConfigDO config = configService.getConfigByKey(POLICY_SETTINGS_CONFIG_KEY);
        MesProSchedulerWorkbenchPolicySettingsRespVO previousSettings = config == null
                || config.getValue() == null || config.getValue().isBlank()
                ? null : parsePolicySettings(config.getValue());
        ConfigSaveReqVO saveReqVO = new ConfigSaveReqVO();
        saveReqVO.setId(config == null ? null : config.getId());
        saveReqVO.setCategory("mes");
        saveReqVO.setName("MES 排产员工作台策略设置");
        saveReqVO.setKey(POLICY_SETTINGS_CONFIG_KEY);
        saveReqVO.setValue(value);
        saveReqVO.setVisible(false);
        saveReqVO.setRemark("ERP工单同步时间、自动重排时间、排产优先级规则、发布/重排保护规则、默认排产补齐规则");
        if (config == null) {
            configService.createConfig(saveReqVO);
        } else {
            configService.updateConfig(saveReqVO);
        }
        applyWorkerHumanEfficiencyIfChanged(previousSettings, normalizedReqVO);
        if (previousSettings == null || !Objects.equals(previousSettings.getNightlyReplanTime(),
                normalizedReqVO.getNightlyReplanTime())) {
            runtimeStatusService.updateNightlyReplanTime(normalizedReqVO.getNightlyReplanTime());
        }
        normalizedReqVO.setWorkerCapacityApplicabilityText(WORKER_CAPACITY_APPLICABILITY_TEXT);
        return normalizedReqVO;
    }

    private void applyWorkerHumanEfficiencyIfChanged(
            MesProSchedulerWorkbenchPolicySettingsRespVO previousSettings,
            MesProSchedulerWorkbenchPolicySettingsRespVO currentSettings) {
        BigDecimal currentHumanEfficiency = currentSettings.getDefaultWorkerSingleHourlyCapacity();
        BigDecimal previousHumanEfficiency = previousSettings == null
                ? null : previousSettings.getDefaultWorkerSingleHourlyCapacity();
        if (previousHumanEfficiency != null && previousHumanEfficiency.compareTo(currentHumanEfficiency) == 0) {
            return;
        }
        List<MesMdWorkstationDO> workstations = Objects.requireNonNull(
                workstationMapper.selectListByStatus(ENABLE.getStatus()),
                "enabled workstations must not be null");
        if (workstations.isEmpty()) {
            return;
        }
        List<Long> workstationIds = workstations.stream()
                .map(MesMdWorkstationDO::getId)
                .filter(Objects::nonNull)
                .toList();
        if (workstationIds.isEmpty()) {
            return;
        }
        List<MesMdWorkstationMachineDO> machineBindings = Objects.requireNonNull(
                workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds),
                "workstation machine bindings must not be null");
        Set<Long> machineWorkstationIds = machineBindings.stream()
                .map(MesMdWorkstationMachineDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        workstations.stream()
                .map(MesMdWorkstationDO::getId)
                .filter(Objects::nonNull)
                .filter(workstationId -> !machineWorkstationIds.contains(workstationId))
                .forEach(workstationId -> workstationMapper.updateSingleStandardHourlyCapacity(
                        workstationId, currentHumanEfficiency));
    }

    @Override
    public MesProSchedulerWorkbenchCapacityUnificationAuditRespVO getCapacityUnificationAudit() {
        MesProSchedulerWorkbenchCapacityUnificationAuditRespVO audit = new MesProSchedulerWorkbenchCapacityUnificationAuditRespVO();
        audit.setEnabled(capacityAuditEnabled);
        if (!capacityAuditEnabled) {
            return audit;
        }
        List<MesProSchedulerWorkbenchCapacityUnificationAuditRespVO.Issue> issues = new ArrayList<>();
        long legacyCount = 0L;
        long manualDiffCount = 0L;
        long resourceMissingCount = 0L;
        long machineryProcessCapacityMissingCount = 0L;
        for (MesProRouteScheduleConfigDO config : routeScheduleConfigMapper.selectListForCapacityUnificationAudit()) {
            if (MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode().equals(config.getCapacityMode())) {
                legacyCount++;
                issues.add(buildAuditIssue(config, "LEGACY_FINITE_HOURLY",
                        "存在历史小时产能配置，请确认产能覆盖口径。", null, null));
                continue;
            }
            if (config.getRouteProcessId() == null
                    || MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(config.getCapacityMode())) {
                continue;
            }
            MesProRouteResourceCapacityPreviewRespVO preview = routeScheduleConfigService
                    .getResourcePreview(config.getRouteProcessId());
            for (MesProRouteResourceCapacityPreviewRespVO.BlockingIssue blocker : preview.getBlockingIssues()) {
                resourceMissingCount++;
                if ("BLOCKED_NO_MACHINERY_PROCESS_CAPACITY".equals(blocker.getCode())) {
                    machineryProcessCapacityMissingCount++;
                }
                issues.add(buildAuditIssue(config, blocker.getCode(), blocker.getMessage(), preview, blocker));
            }
            if (MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode().equals(config.getCapacityMode())
                    && positive(config.getHourlyCapacity())
                    && positive(preview.getResourceCapacityHourly())
                    && config.getHourlyCapacity().subtract(preview.getResourceCapacityHourly()).abs()
                    .compareTo(CAPACITY_DIFF_EPSILON) > 0) {
                manualDiffCount++;
                issues.add(buildAuditIssue(config, "MANUAL_OVERRIDE_RESOURCE_DIFF",
                        "Manual override differs from resource capacity", preview, null));
            }
        }
        audit.setLegacyFiniteHourlyConfigCount(legacyCount);
        audit.setManualOverrideDiffCount(manualDiffCount);
        audit.setResourceMissingCount(resourceMissingCount);
        audit.setMachineryProcessCapacityMissingCount(machineryProcessCapacityMissingCount);
        audit.setTotalIssueCount((long) issues.size());
        audit.setIssues(issues);
        return audit;
    }

    private MesProSchedulerWorkbenchCapacityUnificationAuditRespVO.Issue buildAuditIssue(
            MesProRouteScheduleConfigDO config,
            String code,
            String message,
            MesProRouteResourceCapacityPreviewRespVO preview,
            MesProRouteResourceCapacityPreviewRespVO.BlockingIssue blocker) {
        MesProSchedulerWorkbenchCapacityUnificationAuditRespVO.Issue issue =
                new MesProSchedulerWorkbenchCapacityUnificationAuditRespVO.Issue();
        issue.setCode(code);
        issue.setMessage(message);
        issue.setRouteScheduleConfigId(config.getId());
        issue.setRouteProcessId(config.getRouteProcessId());
        issue.setCapacityMode(config.getCapacityMode());
        issue.setManualHourlyCapacity(config.getHourlyCapacity());
        if (preview != null) {
            issue.setResourceCapacityHourly(preview.getResourceCapacityHourly());
            issue.setCapacitySource(preview.getCapacitySource());
        }
        if (blocker != null) {
            issue.setRouteProcessId(blocker.getRouteProcessId());
            issue.setWorkstationId(blocker.getWorkstationId());
            issue.setWorkstationCode(blocker.getWorkstationCode());
            issue.setMachineryId(blocker.getMachineryId());
            issue.setMachineryCode(blocker.getMachineryCode());
        }
        return issue;
    }
    private String buildDeviationText(BigDecimal deviationQuantity) {
        if (deviationQuantity == null || deviationQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return "无偏差";
        }
        BigDecimal absValue = deviationQuantity.abs().stripTrailingZeros();
        return deviationQuantity.compareTo(BigDecimal.ZERO) > 0
                ? "多报 " + absValue.toPlainString()
                : "少报 " + absValue.toPlainString();
    }

    private String buildActionSuggestion(MesProSchedulerWorkbenchSummaryRespVO summary) {
        if (summary.getBlockingIssueCount() == null || summary.getBlockingIssueCount() == 0) {
            return "今天暂无全局治理风险，按操作顺序确认排程和报工即可。";
        }
        return String.format("全局队列仍有资源未配置 %d 项、维修设备 %d 台、今日物料短缺 %d 项；本次排产是否阻断以预览/发布前检查结果为准。",
                summary.getResourceUnconfiguredCount(),
                summary.getRepairingMachineryCount(),
                summary.getMaterialShortageCount());
    }

    private String buildGlobalRiskScopeText(MesProSchedulerWorkbenchSummaryRespVO summary) {
        return String.format("全局队列治理风险：资源未配置 %d 项、维修设备 %d 台、今日物料短缺 %d 项；用于治理排队，不等同于本次发布阻断。",
                summary.getResourceUnconfiguredCount(),
                summary.getRepairingMachineryCount(),
                summary.getMaterialShortageCount());
    }

    private List<MesProSchedulerWorkbenchSummaryRespVO.Step> buildSteps(MesProSchedulerWorkbenchSummaryRespVO summary) {
        return List.of(
                step(1, "生产订单", "查看 ERP 同步来的生产订单", "/mes/pro/work-order", "待确认生产订单", summary.getPendingScheduleOrderCount()),
                step(2, "排产工单池", "确认承诺交期和排产优先级", "/mes/pro/schedule-order", "待排产工单", summary.getPendingScheduleOrderCount()),
                step(3, "工艺流程与资源", "核对工艺流程、设备和人工产能", "/mes/pro/route?tab=schedule-config", "资源未配置", summary.getResourceUnconfiguredCount()),
                step(4, "今日资源调整", "处理维修、设备和人员临时变化", "/mes/pro/route?tab=schedule-config", "维修设备", summary.getRepairingMachineryCount()),
                step(5, "生成排程日历", "预览并发布当天排程", "/mes/pro/schedule-calendar", "今日任务", summary.getTodayScheduledTaskCount()),
                step(6, "生产任务", "查看任务甘特和锁定原因", "/mes/pro/task", "今日任务", summary.getTodayScheduledTaskCount()),
                step(7, "生产报工", "导入报工并完成待归属", "/mes/pro/feedback", "报工数量", summary.getTodayFeedbackQuantity()),
                step(8, "偏差复盘", "查看待审批报工、报工与计划偏差和瓶颈原因", "/mes/pro/feedback?tab=feedback&status=2", "待审批报工", summary.getPendingApprovalFeedbackCount())
        );
    }

    private MesProSchedulerWorkbenchSummaryRespVO.Step step(Integer sort, String name, String description,
                                                           String path, String metricName, Object metricValue) {
        MesProSchedulerWorkbenchSummaryRespVO.Step step = new MesProSchedulerWorkbenchSummaryRespVO.Step();
        step.setSort(sort);
        step.setName(name);
        step.setDescription(description);
        step.setPrimaryPath(path);
        step.setPrimaryMetricName(metricName);
        step.setPrimaryMetricValue(String.valueOf(metricValue));
        return step;
    }

    private MesProSchedulerWorkbenchShiftHoursRespVO buildShiftHoursSetting(List<MesMdWorkstationDO> workstations,
                                                                            int updatedCount) {
        long workstationCount = workstations.size();
        List<BigDecimal> configuredShiftHours = workstations.stream()
                .map(MesMdWorkstationDO::getShiftHours)
                .filter(value -> value != null)
                .toList();
        Set<BigDecimal> distinctShiftHours = configuredShiftHours.stream()
                .map(BigDecimal::stripTrailingZeros)
                .collect(Collectors.toSet());

        MesProSchedulerWorkbenchShiftHoursRespVO respVO = new MesProSchedulerWorkbenchShiftHoursRespVO();
        respVO.setWorkstationCount(workstationCount);
        respVO.setConfiguredWorkstationCount((long) configuredShiftHours.size());
        respVO.setMissingWorkstationCount(workstationCount - configuredShiftHours.size());
        respVO.setDistinctShiftHoursCount((long) distinctShiftHours.size());
        respVO.setUpdatedWorkstationCount(updatedCount);
        if (distinctShiftHours.size() == 1) {
            respVO.setShiftHours(configuredShiftHours.get(0));
        }
        return respVO;
    }

    private MesProSchedulerWorkbenchPolicySettingsRespVO defaultPolicySettings() {
        MesProSchedulerWorkbenchPolicySettingsRespVO settings = normalizePolicySettings(null);
        settings.setWorkerCapacityApplicabilityText(WORKER_CAPACITY_APPLICABILITY_TEXT);
        return settings;
    }

    private MesProSchedulerWorkbenchPolicySettingsRespVO parsePolicySettings(String value) {
        try {
            MesProSchedulerWorkbenchPolicySettingsRespVO settings = objectMapper.readValue(value,
                    MesProSchedulerWorkbenchPolicySettingsRespVO.class);
            settings = normalizePolicySettings(settings);
            validatePolicySettings(settings);
            settings.setWorkerCapacityApplicabilityText(WORKER_CAPACITY_APPLICABILITY_TEXT);
            return settings;
        } catch (JsonProcessingException ex) {
            throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
        }
    }

    private String serializePolicySettings(MesProSchedulerWorkbenchPolicySettingsRespVO reqVO) {
        try {
            return objectMapper.writeValueAsString(reqVO);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize scheduler workbench policy settings", ex);
        }
    }

    private void validatePolicySettings(MesProSchedulerWorkbenchPolicySettingsRespVO reqVO) {
        if (!PRIORITY_RULES.contains(reqVO.getPriorityRule())) {
            throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
        }
        if (!DEFAULT_SCHEDULE_CAPACITY_MODES.contains(reqVO.getDefaultScheduleCapacityMode())) {
            throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
        }
        if (MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode().equals(reqVO.getDefaultScheduleCapacityMode())) {
            if (!positive(reqVO.getDefaultFiniteHourlyCapacity())) {
                throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
            }
        } else if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(reqVO.getDefaultScheduleCapacityMode())
                && (!positive(reqVO.getDefaultInfiniteDurationQuantityFactorHours())
                || reqVO.getDefaultInfiniteDurationBaseHours() == null
                || reqVO.getDefaultInfiniteDurationBaseHours().compareTo(BigDecimal.ZERO) < 0)) {
            throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
        }
        if (reqVO.getDefaultWorkerQuantity() == null || reqVO.getDefaultWorkerQuantity() <= 0) {
            throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
        }
        if (!positive(reqVO.getDefaultWorkerSingleHourlyCapacity())) {
            throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
        }
        if (Boolean.TRUE.equals(reqVO.getDefaultNightShiftEnabled())) {
            resolveCurrentTenantCalendarRule();
        }
    }

    private MesProSchedulerWorkbenchPolicySettingsRespVO normalizePolicySettings(
            MesProSchedulerWorkbenchPolicySettingsRespVO source) {
        MesProSchedulerWorkbenchPolicySettingsRespVO defaults = buildPolicySettingsDefaults();
        MesProSchedulerWorkbenchPolicySettingsRespVO target = new MesProSchedulerWorkbenchPolicySettingsRespVO();
        target.setErpWorkOrderSyncTime(defaultIfBlank(source == null ? null : source.getErpWorkOrderSyncTime(),
                defaults.getErpWorkOrderSyncTime()));
        target.setNightlyReplanTime(defaultIfBlank(source == null ? null : source.getNightlyReplanTime(),
                defaults.getNightlyReplanTime()));
        target.setPriorityRule(defaultIfBlank(source == null ? null : source.getPriorityRule(),
                defaults.getPriorityRule()));
        target.setProtectReportedTasks(source == null || source.getProtectReportedTasks() == null
                ? defaults.getProtectReportedTasks() : source.getProtectReportedTasks());
        target.setProtectCompletedTasks(source == null || source.getProtectCompletedTasks() == null
                ? defaults.getProtectCompletedTasks() : source.getProtectCompletedTasks());
        target.setProtectLockedTasks(source == null || source.getProtectLockedTasks() == null
                ? defaults.getProtectLockedTasks() : source.getProtectLockedTasks());
        target.setDefaultScheduleUseEnabled(source == null || source.getDefaultScheduleUseEnabled() == null
                ? defaults.getDefaultScheduleUseEnabled() : source.getDefaultScheduleUseEnabled());
        target.setDefaultScheduleCapacityMode(defaultIfBlank(
                source == null ? null : source.getDefaultScheduleCapacityMode(),
                defaults.getDefaultScheduleCapacityMode()));
        if (MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode().equals(target.getDefaultScheduleCapacityMode())) {
            target.setDefaultScheduleCapacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode());
        }
        target.setDefaultFiniteHourlyCapacity(source == null || source.getDefaultFiniteHourlyCapacity() == null
                ? defaults.getDefaultFiniteHourlyCapacity() : source.getDefaultFiniteHourlyCapacity());
        target.setDefaultInfiniteDurationQuantityFactorHours(
                source == null ? null : source.getDefaultInfiniteDurationQuantityFactorHours());
        target.setDefaultInfiniteDurationBaseHours(source == null ? null : source.getDefaultInfiniteDurationBaseHours());
        target.setDefaultNightShiftEnabled(source == null || source.getDefaultNightShiftEnabled() == null
                ? defaults.getDefaultNightShiftEnabled() : source.getDefaultNightShiftEnabled());
        target.setDefaultWorkerQuantity(source == null || source.getDefaultWorkerQuantity() == null
                ? defaults.getDefaultWorkerQuantity() : source.getDefaultWorkerQuantity());
        target.setDefaultWorkerSingleHourlyCapacity(
                source == null || source.getDefaultWorkerSingleHourlyCapacity() == null
                        ? defaults.getDefaultWorkerSingleHourlyCapacity()
                        : source.getDefaultWorkerSingleHourlyCapacity());
        if (MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode().equals(target.getDefaultScheduleCapacityMode())) {
            target.setDefaultInfiniteDurationQuantityFactorHours(null);
            target.setDefaultInfiniteDurationBaseHours(null);
        } else if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(target.getDefaultScheduleCapacityMode())) {
            target.setDefaultFiniteHourlyCapacity(null);
        } else {
            target.setDefaultFiniteHourlyCapacity(null);
            target.setDefaultInfiniteDurationQuantityFactorHours(null);
            target.setDefaultInfiniteDurationBaseHours(null);
        }
        return target;
    }

    private MesProSchedulerWorkbenchPolicySettingsRespVO buildPolicySettingsDefaults() {
        MesProSchedulerWorkbenchPolicySettingsRespVO respVO = new MesProSchedulerWorkbenchPolicySettingsRespVO();
        respVO.setErpWorkOrderSyncTime("02:00");
        respVO.setNightlyReplanTime("02:00");
        respVO.setPriorityRule("PROMISE_DATE");
        respVO.setProtectReportedTasks(true);
        respVO.setProtectCompletedTasks(true);
        respVO.setProtectLockedTasks(true);
        respVO.setDefaultScheduleUseEnabled(true);
        respVO.setDefaultScheduleCapacityMode(resolveConfiguredDefaultRouteCapacityMode());
        respVO.setDefaultFiniteHourlyCapacity(new BigDecimal("30"));
        respVO.setDefaultNightShiftEnabled(false);
        respVO.setDefaultWorkerQuantity(5);
        respVO.setDefaultWorkerSingleHourlyCapacity(new BigDecimal("30"));
        return respVO;
    }

    private String resolveConfiguredDefaultRouteCapacityMode() {
        if (MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode().equals(defaultRouteCapacityMode)) {
            return MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode();
        }
        if (!DEFAULT_SCHEDULE_CAPACITY_MODES.contains(defaultRouteCapacityMode)) {
            throw exception(PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID);
        }
        return defaultRouteCapacityMode;
    }

    private MesProScheduleCalendarRuleDO resolveCurrentTenantCalendarRule() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProScheduleCalendarRuleDO rule = scheduleCalendarRuleMapper.selectByTenantId(tenantId);
        if (rule == null || rule.getId() == null) {
            throw new IllegalStateException("默认夜班已开启，但当前租户缺少排程日历规则");
        }
        return rule;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean positiveInteger(BigDecimal value) {
        return positive(value) && value.stripTrailingZeros().scale() <= 0;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
