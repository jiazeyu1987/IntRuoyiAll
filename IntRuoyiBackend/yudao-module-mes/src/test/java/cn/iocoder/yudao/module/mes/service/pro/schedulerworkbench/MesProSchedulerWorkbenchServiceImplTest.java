package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteResourceCapacityPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchCapacityUnificationAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchPolicySettingsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchShiftHoursRespVO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedulerworkbench.MesProSchedulerWorkbenchMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProScheduleCalendarService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class MesProSchedulerWorkbenchServiceImplTest {

    @InjectMocks
    private MesProSchedulerWorkbenchServiceImpl service;

    @Mock
    private MesProSchedulerWorkbenchMapper schedulerWorkbenchMapper;

    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;

    @Mock
    private ConfigService configService;
    @Mock
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Mock
    private MesProScheduleCalendarService scheduleCalendarService;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteScheduleConfigService routeScheduleConfigService;
    @Mock
    private MesProSchedulerWorkbenchRuntimeStatusService runtimeStatusService;

    @Test
    void getSummary_shouldAggregateTodayStatsAndFixedSchedulerSteps() {
        LocalDate date = LocalDate.of(2026, 6, 10);
        LocalDateTime beginTime = LocalDateTime.of(2026, 6, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 11, 0, 0);
        MesProSchedulerWorkbenchSummaryRespVO.Bottleneck bottleneck = new MesProSchedulerWorkbenchSummaryRespVO.Bottleneck();
        bottleneck.setScheduleOrderProcessId(125L);
        bottleneck.setProcessCode("B060");
        bottleneck.setProcessName("外管与球囊焊接");
        bottleneck.setGapQuantity(new BigDecimal("420"));
        bottleneck.setReason("剩余数量未完成");
        MesProSchedulerWorkbenchSummaryRespVO.ReportedDeviationDetail detail =
                new MesProSchedulerWorkbenchSummaryRespVO.ReportedDeviationDetail();
        detail.setScheduleOrderId(47L);
        detail.setScheduleOrderProcessId(125L);
        detail.setScheduleOrderCode("SCH-001");
        detail.setWorkOrderCode("MO-001");
        detail.setProcessCode("B060");
        detail.setProcessName("外管与球囊焊接");
        detail.setPlannedQuantity(new BigDecimal("432"));
        detail.setReportedQuantity(new BigDecimal("12"));
        detail.setDeviationQuantity(new BigDecimal("-420"));
        detail.setProcessStatus("IN_PROGRESS");
        MesProSchedulerWorkbenchSummaryRespVO.RouteActiveProduct activeProduct =
                new MesProSchedulerWorkbenchSummaryRespVO.RouteActiveProduct();
        activeProduct.setProductId(9001L);
        activeProduct.setProductCode("CP-001");
        activeProduct.setProductName("球囊扩张导管");
        activeProduct.setWipOrderCount(2L);
        MesProSchedulerWorkbenchSummaryRespVO.RouteActiveOrder routeActiveOrder =
                new MesProSchedulerWorkbenchSummaryRespVO.RouteActiveOrder();
        routeActiveOrder.setRouteId(1001L);
        routeActiveOrder.setRouteCode("ROUTE-BALLOON");
        routeActiveOrder.setRouteName("球囊导管工艺路线");
        routeActiveOrder.setWipOrderCount(2L);
        routeActiveOrder.setProducts(List.of(activeProduct));

        when(schedulerWorkbenchMapper.selectPendingScheduleOrderCount()).thenReturn(3L);
        when(schedulerWorkbenchMapper.selectTodayScheduledTaskCount(beginTime, endTime)).thenReturn(24L);
        when(schedulerWorkbenchMapper.selectTodayPlannedCapacity(beginTime, endTime)).thenReturn(new BigDecimal("432"));
        when(schedulerWorkbenchMapper.selectTodayFeedbackCount(beginTime, endTime)).thenReturn(2L);
        when(schedulerWorkbenchMapper.selectTodayFeedbackQuantity(beginTime, endTime)).thenReturn(new BigDecimal("12"));
        when(schedulerWorkbenchMapper.selectPendingApprovalFeedbackCount()).thenReturn(5L);
        when(schedulerWorkbenchMapper.selectCurrentSchedulePlannedQuantity()).thenReturn(new BigDecimal("432"));
        when(schedulerWorkbenchMapper.selectCurrentScheduleReportedQuantity()).thenReturn(new BigDecimal("12"));
        when(schedulerWorkbenchMapper.selectReportedDeviationDetails()).thenReturn(List.of(detail));
        when(schedulerWorkbenchMapper.selectTodayAvailableCapacity(beginTime, endTime)).thenReturn(new BigDecimal("380"));
        when(schedulerWorkbenchMapper.selectRepairingMachineryCount()).thenReturn(1L);
        when(schedulerWorkbenchMapper.selectResourceUnconfiguredCount()).thenReturn(2L);
        when(schedulerWorkbenchMapper.selectMaterialShortageCount(beginTime, endTime)).thenReturn(4L);
        when(schedulerWorkbenchMapper.selectBottlenecks(beginTime, endTime)).thenReturn(List.of(bottleneck));
        when(schedulerWorkbenchMapper.selectRouteActiveOrders()).thenReturn(List.of(routeActiveOrder));

        MesProSchedulerWorkbenchSummaryRespVO summary = service.getSummary(date);

        assertEquals(date, summary.getDate());
        assertEquals(3L, summary.getPendingScheduleOrderCount());
        assertEquals(24L, summary.getTodayScheduledTaskCount());
        assertEquals(new BigDecimal("432"), summary.getTodayPlannedCapacity());
        assertEquals(2L, summary.getTodayFeedbackCount());
        assertEquals(new BigDecimal("12"), summary.getTodayFeedbackQuantity());
        assertEquals(5L, summary.getPendingApprovalFeedbackCount());
        assertEquals(new BigDecimal("432"), summary.getCurrentSchedulePlannedQuantity());
        assertEquals(new BigDecimal("12"), summary.getCurrentScheduleReportedQuantity());
        assertEquals(new BigDecimal("-420"), summary.getReportedDeviationQuantity());
        assertEquals("少报 420", summary.getReportedDeviationText());
        assertEquals(new BigDecimal("380"), summary.getTodayAvailableCapacity());
        assertEquals(1L, summary.getRepairingMachineryCount());
        assertEquals(2L, summary.getResourceUnconfiguredCount());
        assertEquals(7L, summary.getBlockingIssueCount());
        assertEquals(4L, summary.getMaterialShortageCount());
        assertEquals("每晚 02:00 自动重排；已报工、已完成、手工锁定任务保持不动。", summary.getNightlyReplanText());
        assertEquals("全局队列仍有资源未配置 2 项、维修设备 1 台、今日物料短缺 4 项；本次排产是否阻断以预览/发布前检查结果为准。",
                summary.getTodayActionSuggestion());
        assertTrue(summary.getTodayActionSuggestion().contains("预览/发布前检查结果"),
                "今日建议必须提醒工作台摘要风险不等同于本次排产最终阻断。");
        assertEquals("报工偏差按当前有效排产工单（已排产/生产中）的实际报工数量与排产数量计算；不再按当天任务段重复累计。",
                summary.getCurrentScheduleScopeText());
        assertEquals("全局队列治理风险：资源未配置 2 项、维修设备 1 台、今日物料短缺 4 项；用于治理排队，不等同于本次发布阻断。",
                summary.getGlobalRiskScopeText());
        assertEquals(8, summary.getSteps().size());
        assertEquals("生产订单", summary.getSteps().get(0).getName());
        assertEquals("/mes/pro/work-order", summary.getSteps().get(0).getPrimaryPath());
        assertEquals("待确认生产订单", summary.getSteps().get(0).getPrimaryMetricName());
        assertEquals("排产工单池", summary.getSteps().get(1).getName());
        assertEquals("待排产工单", summary.getSteps().get(1).getPrimaryMetricName());
        assertEquals("工艺流程与资源", summary.getSteps().get(2).getName());
        assertEquals("/mes/pro/route?tab=schedule-config", summary.getSteps().get(2).getPrimaryPath());
        assertEquals("今日资源调整", summary.getSteps().get(3).getName());
        assertEquals("/mes/pro/route?tab=schedule-config", summary.getSteps().get(3).getPrimaryPath());
        assertEquals("生产报工", summary.getSteps().get(6).getName());
        assertEquals("/mes/pro/feedback", summary.getSteps().get(6).getPrimaryPath());
        assertEquals("偏差复盘", summary.getSteps().get(7).getName());
        assertEquals("/mes/pro/feedback?tab=feedback&status=2", summary.getSteps().get(7).getPrimaryPath());
        assertEquals("待审批报工", summary.getSteps().get(7).getPrimaryMetricName());
        assertEquals("5", summary.getSteps().get(7).getPrimaryMetricValue());
        assertSchedulerSummaryHasNoEdhrBoundaryLeak(summary);
        assertSchedulerSummaryHasNoLegacyBlockingLabel(summary);
        assertFalse(summary.getBottlenecks().isEmpty());
        assertFalse(summary.getReportedDeviationDetails().isEmpty());
        assertFalse(summary.getRouteActiveOrders().isEmpty());
        assertEquals("球囊导管工艺路线", summary.getRouteActiveOrders().get(0).getRouteName());
        assertEquals(2L, summary.getRouteActiveOrders().get(0).getWipOrderCount());
        assertEquals("球囊扩张导管", summary.getRouteActiveOrders().get(0).getProducts().get(0).getProductName());
        assertEquals(2L, summary.getRouteActiveOrders().get(0).getProducts().get(0).getWipOrderCount());
        assertEquals("SCH-001", summary.getReportedDeviationDetails().get(0).getScheduleOrderCode());
        assertEquals(new BigDecimal("-420"), summary.getReportedDeviationDetails().get(0).getDeviationQuantity());
        assertEquals("B060", summary.getBottlenecks().get(0).getProcessCode());

        verify(schedulerWorkbenchMapper).selectBottlenecks(beginTime, endTime);
        verify(schedulerWorkbenchMapper).selectCurrentSchedulePlannedQuantity();
        verify(schedulerWorkbenchMapper).selectCurrentScheduleReportedQuantity();
        verify(schedulerWorkbenchMapper).selectReportedDeviationDetails();
        verify(schedulerWorkbenchMapper).selectRouteActiveOrders();
    }

    @Test
    void saveShiftHoursSetting_shouldUpdateAllWorkstationsAndReturnUnifiedSetting() {
        BigDecimal shiftHours = new BigDecimal("10.50");
        when(workstationMapper.updateAllShiftHours(shiftHours)).thenReturn(21);
        when(workstationMapper.selectListForShiftHours()).thenReturn(List.of(
                workstation(1L, shiftHours),
                workstation(2L, shiftHours)));

        MesProSchedulerWorkbenchShiftHoursRespVO setting = service.saveShiftHoursSetting(shiftHours);

        assertEquals(shiftHours, setting.getShiftHours());
        assertEquals(2L, setting.getWorkstationCount());
        assertEquals(2L, setting.getConfiguredWorkstationCount());
        assertEquals(0L, setting.getMissingWorkstationCount());
        assertEquals(1L, setting.getDistinctShiftHoursCount());
        assertEquals(21, setting.getUpdatedWorkstationCount());
        verify(workstationMapper).updateAllShiftHours(shiftHours);
        verify(scheduleCalendarService).refreshPlanCapacityForShiftHours(shiftHours);
    }

    @Test
    void getShiftHoursSetting_shouldExposeInconsistentExistingValuesWithoutChoosingFallback() {
        when(workstationMapper.selectListForShiftHours()).thenReturn(List.of(
                workstation(1L, new BigDecimal("8.00")),
                workstation(2L, new BigDecimal("10.50")),
                workstation(3L, null)));

        MesProSchedulerWorkbenchShiftHoursRespVO setting = service.getShiftHoursSetting();

        assertEquals(null, setting.getShiftHours());
        assertEquals(3L, setting.getWorkstationCount());
        assertEquals(2L, setting.getConfiguredWorkstationCount());
        assertEquals(1L, setting.getMissingWorkstationCount());
        assertEquals(2L, setting.getDistinctShiftHoursCount());
        assertEquals(0, setting.getUpdatedWorkstationCount());
    }

    @Test
    void getPolicySettings_shouldReturnCurrentDefaultPolicyWhenConfigMissing() {
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(null);

        MesProSchedulerWorkbenchPolicySettingsRespVO settings = service.getPolicySettings();

        assertEquals("02:00", settings.getErpWorkOrderSyncTime());
        assertEquals("02:00", settings.getNightlyReplanTime());
        assertEquals("PROMISE_DATE", settings.getPriorityRule());
        assertEquals(true, settings.getProtectReportedTasks());
        assertEquals(true, settings.getProtectCompletedTasks());
        assertEquals(true, settings.getProtectLockedTasks());
        assertEquals(true, settings.getDefaultScheduleUseEnabled());
        assertEquals("RESOURCE_CALCULATED", settings.getDefaultScheduleCapacityMode());
        assertEquals(null, settings.getDefaultFiniteHourlyCapacity());
        assertEquals(false, settings.getDefaultNightShiftEnabled());
        assertEquals(5, settings.getDefaultWorkerQuantity());
        assertEquals(new BigDecimal("30"), settings.getDefaultWorkerSingleHourlyCapacity());
        assertEquals("人效h仅影响资源计算模式且产能来源为人工的工序；设备产能、手工覆盖和无限公式不受影响。人数仅作为新配置默认值，不强制重算现有工位。",
                settings.getWorkerCapacityApplicabilityText());
        assertEquals(null, settings.getDefaultInfiniteDurationQuantityFactorHours());
        assertEquals(null, settings.getDefaultInfiniteDurationBaseHours());
    }

    @Test
    void savePolicySettings_shouldCreateInfraConfigWhenMissing() {
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(null);
        when(workstationMapper.selectListByStatus(0)).thenReturn(List.of());
        MesProSchedulerWorkbenchPolicySettingsRespVO reqVO = policySettings("01:30", "03:15",
                "ORDER_PRIORITY", true, false, true,
                true, "FINITE_HOURLY", new BigDecimal("96"), null, null,
                false, 6, new BigDecimal("28.5"));

        MesProSchedulerWorkbenchPolicySettingsRespVO saved = service.savePolicySettings(reqVO);

        assertEquals("01:30", saved.getErpWorkOrderSyncTime());
        assertEquals("03:15", saved.getNightlyReplanTime());
        assertEquals("ORDER_PRIORITY", saved.getPriorityRule());
        assertEquals(false, saved.getProtectCompletedTasks());
        assertEquals("MANUAL_OVERRIDE", saved.getDefaultScheduleCapacityMode());
        assertEquals(new BigDecimal("96"), saved.getDefaultFiniteHourlyCapacity());
        assertEquals(6, saved.getDefaultWorkerQuantity());
        verify(configService).createConfig(argThat((ConfigSaveReqVO config) ->
                "mes.scheduler-workbench.policy-settings".equals(config.getKey())
                        && config.getValue().contains("\"erpWorkOrderSyncTime\":\"01:30\"")
                        && config.getValue().contains("\"defaultScheduleCapacityMode\":\"MANUAL_OVERRIDE\"")
                        && !config.getValue().contains("\"defaultScheduleCapacityMode\":\"FINITE_HOURLY\"")
                        && config.getValue().contains("\"defaultFiniteHourlyCapacity\":96")
                        && config.getValue().contains("\"defaultWorkerQuantity\":6")
                        && Boolean.FALSE.equals(config.getVisible())));
        InOrder persistenceOrder = inOrder(configService, runtimeStatusService);
        persistenceOrder.verify(configService).createConfig(any(ConfigSaveReqVO.class));
        persistenceOrder.verify(runtimeStatusService).updateNightlyReplanTime("03:15");
    }

    @Test
    void savePolicySettings_shouldUpdateInfraConfigWhenExists() {
        ConfigDO config = new ConfigDO();
        config.setId(9001L);
        config.setConfigKey("mes.scheduler-workbench.policy-settings");
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(config);
        when(workstationMapper.selectListByStatus(0)).thenReturn(List.of());

        service.savePolicySettings(policySettings("05:00", "06:00", "CREATED_TIME",
                false, true, true,
                true, "MANUAL_OVERRIDE", new BigDecimal("88"), null, null,
                false, 4, new BigDecimal("32")));

        verify(configService).updateConfig(argThat((ConfigSaveReqVO update) ->
                Long.valueOf(9001L).equals(update.getId())
                        && "mes.scheduler-workbench.policy-settings".equals(update.getKey())
                        && update.getValue().contains("\"priorityRule\":\"CREATED_TIME\"")
                        && update.getValue().contains("\"defaultWorkerSingleHourlyCapacity\":32")));
        verify(runtimeStatusService).updateNightlyReplanTime("06:00");
    }

    @Test
    void savePolicySettings_shouldApplyChangedHumanEfficiencyToEnabledWorkerWorkstationsOnly() {
        ConfigDO config = new ConfigDO();
        config.setId(9001L);
        config.setConfigKey("mes.scheduler-workbench.policy-settings");
        config.setValue("""
                {"erpWorkOrderSyncTime":"02:00","nightlyReplanTime":"02:00","priorityRule":"PROMISE_DATE","protectReportedTasks":true,"protectCompletedTasks":true,"protectLockedTasks":true,"defaultScheduleUseEnabled":true,"defaultScheduleCapacityMode":"RESOURCE_CALCULATED","defaultNightShiftEnabled":false,"defaultWorkerQuantity":5,"defaultWorkerSingleHourlyCapacity":30}
                """);
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(config);
        when(workstationMapper.selectListByStatus(0)).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(101L).status(0).build(),
                MesMdWorkstationDO.builder().id(102L).status(0).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(List.of(101L, 102L)))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder()
                        .id(201L).workstationId(102L).machineryId(301L).build()));

        MesProSchedulerWorkbenchPolicySettingsRespVO saved = service.savePolicySettings(policySettings("02:00", "02:00", "PROMISE_DATE",
                true, true, true,
                true, "RESOURCE_CALCULATED", null, null, null,
                false, 5, new BigDecimal("60")));

        verify(workstationMapper).updateSingleStandardHourlyCapacity(101L, new BigDecimal("60"));
        verify(workstationMapper, never()).updateSingleStandardHourlyCapacity(102L, new BigDecimal("60"));
        assertEquals("人效h仅影响资源计算模式且产能来源为人工的工序；设备产能、手工覆盖和无限公式不受影响。人数仅作为新配置默认值，不强制重算现有工位。",
                saved.getWorkerCapacityApplicabilityText());
        verify(configService).updateConfig(argThat((ConfigSaveReqVO update) ->
                !update.getValue().contains("workerCapacityApplicabilityText")));
    }

    @Test
    void savePolicySettings_shouldNotRewriteWorkerWorkstationsWhenHumanEfficiencyIsUnchanged() {
        ConfigDO config = new ConfigDO();
        config.setId(9001L);
        config.setConfigKey("mes.scheduler-workbench.policy-settings");
        config.setValue("""
                {"erpWorkOrderSyncTime":"02:00","nightlyReplanTime":"02:00","priorityRule":"PROMISE_DATE","protectReportedTasks":true,"protectCompletedTasks":true,"protectLockedTasks":true,"defaultScheduleUseEnabled":true,"defaultScheduleCapacityMode":"RESOURCE_CALCULATED","defaultNightShiftEnabled":false,"defaultWorkerQuantity":5,"defaultWorkerSingleHourlyCapacity":60}
                """);
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(config);

        service.savePolicySettings(policySettings("03:00", "04:00", "ORDER_PRIORITY",
                true, true, true,
                true, "RESOURCE_CALCULATED", null, null, null,
                false, 5, new BigDecimal("60.000")));

        verifyNoInteractions(workstationMapper, workstationMachineService);
        verify(runtimeStatusService).updateNightlyReplanTime("04:00");
    }

    @Test
    void savePolicySettings_shouldAcceptDecimalManualOverrideHourlyCapacity() {
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(null);
        when(workstationMapper.selectListByStatus(0)).thenReturn(List.of());

        MesProSchedulerWorkbenchPolicySettingsRespVO saved = service.savePolicySettings(policySettings("05:00", "06:00",
                "CREATED_TIME", false, true, true,
                true, "MANUAL_OVERRIDE", new BigDecimal("12.5"), null, null,
                false, 4, new BigDecimal("32")));

        assertEquals("MANUAL_OVERRIDE", saved.getDefaultScheduleCapacityMode());
        assertEquals(new BigDecimal("12.5"), saved.getDefaultFiniteHourlyCapacity());
        verify(configService).createConfig(argThat((ConfigSaveReqVO create) ->
                create.getValue().contains("\"defaultFiniteHourlyCapacity\":12.5")));
    }

    @Test
    void getPolicySettings_shouldMergeDefaultSchedulingFieldsWhenInfraConfigStillUsesLegacyJson() {
        ConfigDO config = new ConfigDO();
        config.setId(9002L);
        config.setValue("""
                {"erpWorkOrderSyncTime":"02:00","nightlyReplanTime":"03:00","priorityRule":"PROMISE_DATE","protectReportedTasks":true,"protectCompletedTasks":true,"protectLockedTasks":true}
                """);
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(config);

        MesProSchedulerWorkbenchPolicySettingsRespVO settings = service.getPolicySettings();

        assertEquals("02:00", settings.getErpWorkOrderSyncTime());
        assertEquals("03:00", settings.getNightlyReplanTime());
        assertEquals(true, settings.getDefaultScheduleUseEnabled());
        assertEquals("RESOURCE_CALCULATED", settings.getDefaultScheduleCapacityMode());
        assertEquals(null, settings.getDefaultFiniteHourlyCapacity());
        assertEquals(false, settings.getDefaultNightShiftEnabled());
        assertEquals(5, settings.getDefaultWorkerQuantity());
        assertEquals(new BigDecimal("30"), settings.getDefaultWorkerSingleHourlyCapacity());
    }

    @Test
    void getPolicySettings_shouldNormalizeLegacyFiniteHourlyDefaultToManualOverride() {
        ConfigDO config = new ConfigDO();
        config.setId(9004L);
        config.setValue("""
                {"erpWorkOrderSyncTime":"02:00","nightlyReplanTime":"03:00","priorityRule":"PROMISE_DATE","protectReportedTasks":true,"protectCompletedTasks":true,"protectLockedTasks":true,"defaultScheduleUseEnabled":true,"defaultScheduleCapacityMode":"FINITE_HOURLY","defaultFiniteHourlyCapacity":55,"defaultNightShiftEnabled":false,"defaultWorkerQuantity":5,"defaultWorkerSingleHourlyCapacity":30}
                """);
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(config);

        MesProSchedulerWorkbenchPolicySettingsRespVO settings = service.getPolicySettings();

        assertEquals("MANUAL_OVERRIDE", settings.getDefaultScheduleCapacityMode());
        assertEquals(new BigDecimal("55"), settings.getDefaultFiniteHourlyCapacity());
        assertEquals(null, settings.getDefaultInfiniteDurationQuantityFactorHours());
        assertEquals(null, settings.getDefaultInfiniteDurationBaseHours());
    }

    @Test
    void savePolicySettings_shouldAcceptResourceCalculatedWithoutManualOrFormulaDefaults() {
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(null);
        when(workstationMapper.selectListByStatus(0)).thenReturn(List.of());

        MesProSchedulerWorkbenchPolicySettingsRespVO saved = service.savePolicySettings(policySettings("01:30", "03:15",
                "ORDER_PRIORITY", true, false, true,
                true, "RESOURCE_CALCULATED", new BigDecimal("96"), new BigDecimal("0.5"), new BigDecimal("1"),
                false, 6, new BigDecimal("28.5")));

        assertEquals("RESOURCE_CALCULATED", saved.getDefaultScheduleCapacityMode());
        assertEquals(null, saved.getDefaultFiniteHourlyCapacity());
        assertEquals(null, saved.getDefaultInfiniteDurationQuantityFactorHours());
        assertEquals(null, saved.getDefaultInfiniteDurationBaseHours());
        verify(configService).createConfig(argThat((ConfigSaveReqVO config) ->
                config.getValue().contains("\"defaultScheduleCapacityMode\":\"RESOURCE_CALCULATED\"")
                        && config.getValue().contains("\"defaultFiniteHourlyCapacity\":null")
                        && config.getValue().contains("\"defaultInfiniteDurationQuantityFactorHours\":null")));
    }

    @Test
    void getPolicySettings_shouldReturnBusinessErrorWhenInfraConfigJsonIsInvalid() {
        ConfigDO config = new ConfigDO();
        config.setId(9003L);
        config.setValue("{broken-json");
        when(configService.getConfigByKey("mes.scheduler-workbench.policy-settings")).thenReturn(config);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.getPolicySettings());

        assertEquals("排产员工作台策略配置无效，请重新保存工作台默认值。", ex.getMessage());
    }

    @Test
    void getCapacityUnificationAudit_shouldReportLegacyManualDiffAndResourceBlockers() {
        when(routeScheduleConfigMapper.selectListForCapacityUnificationAudit()).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(700L)
                        .routeProcessId(200L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("10"))
                        .build(),
                MesProRouteScheduleConfigDO.builder()
                        .id(701L)
                        .routeProcessId(201L)
                        .capacityMode("MANUAL_OVERRIDE")
                        .hourlyCapacity(new BigDecimal("12"))
                        .build(),
                MesProRouteScheduleConfigDO.builder()
                        .id(702L)
                        .routeProcessId(202L)
                        .capacityMode("RESOURCE_CALCULATED")
                        .build()));
        MesProRouteResourceCapacityPreviewRespVO manualPreview = new MesProRouteResourceCapacityPreviewRespVO();
        manualPreview.setRouteProcessId(201L);
        manualPreview.setResourceCapacityHourly(new BigDecimal("15"));
        manualPreview.setCapacitySource("WORKER");
        when(routeScheduleConfigService.getResourcePreview(201L)).thenReturn(manualPreview);
        MesProRouteResourceCapacityPreviewRespVO resourcePreview = new MesProRouteResourceCapacityPreviewRespVO();
        resourcePreview.setRouteProcessId(202L);
        resourcePreview.setResourceCapacityHourly(BigDecimal.ZERO);
        resourcePreview.setCapacitySource("MACHINE");
        MesProRouteResourceCapacityPreviewRespVO.BlockingIssue blocker =
                new MesProRouteResourceCapacityPreviewRespVO.BlockingIssue();
        blocker.setCode("BLOCKED_NO_MACHINERY_PROCESS_CAPACITY");
        blocker.setMessage("设备工序产能缺失");
        blocker.setRouteProcessId(202L);
        blocker.setWorkstationId(800L);
        blocker.setMachineryId(900L);
        resourcePreview.setBlockingIssues(List.of(blocker));
        when(routeScheduleConfigService.getResourcePreview(202L)).thenReturn(resourcePreview);

        MesProSchedulerWorkbenchCapacityUnificationAuditRespVO audit = service.getCapacityUnificationAudit();

        assertEquals(true, audit.getEnabled());
        assertEquals(1L, audit.getLegacyFiniteHourlyConfigCount());
        assertEquals(1L, audit.getManualOverrideDiffCount());
        assertEquals(1L, audit.getResourceMissingCount());
        assertEquals(1L, audit.getMachineryProcessCapacityMissingCount());
        assertEquals(3L, audit.getTotalIssueCount());
        MesProSchedulerWorkbenchCapacityUnificationAuditRespVO.Issue legacyIssue = audit.getIssues().stream()
                .filter(issue -> "LEGACY_FINITE_HOURLY".equals(issue.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("存在历史小时产能配置，请确认产能覆盖口径。", legacyIssue.getMessage());
        assertFalse(legacyIssue.getMessage().contains("Legacy"));
        assertFalse(legacyIssue.getMessage().contains("migrated"));
        assertTrue(audit.getIssues().stream().anyMatch(issue -> "MANUAL_OVERRIDE_RESOURCE_DIFF".equals(issue.getCode())
                && issue.getManualHourlyCapacity().compareTo(new BigDecimal("12")) == 0
                && issue.getResourceCapacityHourly().compareTo(new BigDecimal("15")) == 0));
        assertTrue(audit.getIssues().stream().anyMatch(issue -> "BLOCKED_NO_MACHINERY_PROCESS_CAPACITY".equals(issue.getCode())
                && Long.valueOf(900L).equals(issue.getMachineryId())));
    }

    @Test
    void capacityUnificationAuditSchema_shouldUseNeutralHistoricalCapacityDescription() throws NoSuchFieldException {
        Schema schema = MesProSchedulerWorkbenchCapacityUnificationAuditRespVO.class
                .getDeclaredField("legacyFiniteHourlyConfigCount")
                .getAnnotation(Schema.class);

        assertEquals("历史小时产能配置数量", schema.description());
        assertFalse(schema.description().contains("旧"));
        assertFalse(schema.description().contains("FINITE_HOURLY"));
    }

    private MesProSchedulerWorkbenchPolicySettingsRespVO policySettings(String erpSyncTime,
                                                                        String replanTime,
                                                                        String priorityRule,
                                                                        Boolean protectReported,
                                                                        Boolean protectCompleted,
                                                                        Boolean protectLocked,
                                                                        Boolean defaultScheduleUseEnabled,
                                                                        String defaultScheduleCapacityMode,
                                                                        BigDecimal defaultFiniteHourlyCapacity,
                                                                        BigDecimal defaultInfiniteDurationQuantityFactorHours,
                                                                        BigDecimal defaultInfiniteDurationBaseHours,
                                                                        Boolean defaultNightShiftEnabled,
                                                                        Integer defaultWorkerQuantity,
                                                                        BigDecimal defaultWorkerSingleHourlyCapacity) {
        MesProSchedulerWorkbenchPolicySettingsRespVO reqVO = new MesProSchedulerWorkbenchPolicySettingsRespVO();
        reqVO.setErpWorkOrderSyncTime(erpSyncTime);
        reqVO.setNightlyReplanTime(replanTime);
        reqVO.setPriorityRule(priorityRule);
        reqVO.setProtectReportedTasks(protectReported);
        reqVO.setProtectCompletedTasks(protectCompleted);
        reqVO.setProtectLockedTasks(protectLocked);
        reqVO.setDefaultScheduleUseEnabled(defaultScheduleUseEnabled);
        reqVO.setDefaultScheduleCapacityMode(defaultScheduleCapacityMode);
        reqVO.setDefaultFiniteHourlyCapacity(defaultFiniteHourlyCapacity);
        reqVO.setDefaultInfiniteDurationQuantityFactorHours(defaultInfiniteDurationQuantityFactorHours);
        reqVO.setDefaultInfiniteDurationBaseHours(defaultInfiniteDurationBaseHours);
        reqVO.setDefaultNightShiftEnabled(defaultNightShiftEnabled);
        reqVO.setDefaultWorkerQuantity(defaultWorkerQuantity);
        reqVO.setDefaultWorkerSingleHourlyCapacity(defaultWorkerSingleHourlyCapacity);
        return reqVO;
    }

    private MesMdWorkstationDO workstation(Long id, BigDecimal shiftHours) {
        MesMdWorkstationDO workstation = new MesMdWorkstationDO();
        workstation.setId(id);
        workstation.setShiftHours(shiftHours);
        return workstation;
    }

    private void assertSchedulerSummaryHasNoLegacyBlockingLabel(MesProSchedulerWorkbenchSummaryRespVO summary) {
        String allWorkbenchText = Stream.concat(
                Stream.of(summary.getNightlyReplanText(), summary.getTodayActionSuggestion(),
                        summary.getCurrentScheduleScopeText(), summary.getGlobalRiskScopeText()),
                summary.getSteps().stream()
                        .flatMap(step -> Stream.of(step.getName(), step.getDescription(),
                                step.getPrimaryMetricName(), step.getPrimaryMetricValue()))
        ).reduce("", (left, right) -> left + "\n" + right);
        assertFalse(allWorkbenchText.contains("阻塞项"), "工作台不得再把全局治理风险或 A5 复盘称为阻塞项");
    }

    private void assertSchedulerSummaryHasNoEdhrBoundaryLeak(MesProSchedulerWorkbenchSummaryRespVO summary) {
        String allWorkbenchText = Stream.concat(
                Stream.of(summary.getNightlyReplanText(), summary.getTodayActionSuggestion(),
                        summary.getCurrentScheduleScopeText(), summary.getGlobalRiskScopeText()),
                summary.getSteps().stream()
                        .flatMap(step -> Stream.of(step.getName(), step.getDescription(), step.getPrimaryPath()))
        ).reduce("", (left, right) -> left + "\n" + right);
        assertFalse(allWorkbenchText.contains("EDHR"), "排产工作台摘要不得展示 EDHR 内容");
        assertFalse(allWorkbenchText.contains("eDHR"), "排产工作台摘要不得展示 eDHR 内容");
        assertFalse(allWorkbenchText.contains("电子批记录"), "排产工作台摘要不得展示电子批记录内容");
        assertFalse(allWorkbenchText.contains("批记录"), "排产工作台摘要不得展示批记录内容");
        assertFalse(allWorkbenchText.contains("/edhr"), "排产工作台步骤不得跳转到 EDHR 路由");
    }

}
