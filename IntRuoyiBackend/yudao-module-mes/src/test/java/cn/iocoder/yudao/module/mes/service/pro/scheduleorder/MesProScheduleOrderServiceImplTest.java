package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrderReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrdersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightIssueRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipSettingsReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderOperationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRiskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRouteStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_BATCH_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_BATCH_ADMISSION_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PRIORITY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PROCESS_WIP_CALENDAR_RULE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PROCESS_WIP_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PROMISE_DATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_WORK_ORDER_NOT_CONFIRMED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderServiceImplTest {

    @InjectMocks
    private MesProScheduleOrderServiceImpl scheduleOrderService;

    @Spy
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy = new ScheduleDefaultCompatibilityPolicy();

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteScheduleConfigService routeScheduleConfigService;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Mock
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Mock
    private MesDvMachineryMapper machineryMapper;
    @Mock
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Mock
    private MesProProcessMapper processMapper;

    @org.junit.jupiter.api.BeforeEach
    void setUpRouteFlowContext() {
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(
                        org.mockito.ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> identityMap(invocation.getArgument(0)));
        org.mockito.Mockito.lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0) == null ? invocation.getArgument(2) : invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        org.mockito.Mockito.lenient().when(routeProcessService.resolveFrozenRouteProcess(
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0) == null ? invocation.getArgument(2) : invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        org.mockito.Mockito.lenient().when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.eq(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())))
                .thenAnswer(invocation -> {
                    Long routeId = invocation.getArgument(0);
                    return MesProRouteFlowConfigDO.builder()
                            .id(routeId)
                            .routeId(routeId)
                            .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                            .enabled(Boolean.TRUE)
                            .build();
                });
        org.mockito.Mockito.lenient().when(routeVersionMapper.selectBatchIds(
                        org.mockito.ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> {
                    java.util.Collection<?> routeVersionIds = invocation.getArgument(0);
                    return routeVersionIds.stream()
                            .filter(java.util.Objects::nonNull)
                            .map(id -> MesProRouteVersionDO.builder()
                                    .id(((Number) id).longValue())
                                    .versionNo("V" + id)
                                    .lifecycleStatus("ACTIVE")
                                    .build())
                            .toList();
                });
        org.mockito.Mockito.lenient().when(workstationMapper.selectListByProcessIds(
                        org.mockito.ArgumentMatchers.anyCollection(),
                        org.mockito.ArgumentMatchers.eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(syncRecordMapper.selectByWorkOrderId(
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> {
                    Long workOrderId = invocation.getArgument(0);
                    return MesKingdeeProductionOrderSyncRecordDO.builder()
                            .workOrderId(workOrderId)
                            .sourceFid("FID-" + workOrderId)
                            .sourceBillNo("ERP-MO-" + workOrderId)
                            .build();
                });
    }

    private Map<Long, Long> identityMap(java.util.Collection<Long> processIds) {
        Map<Long, Long> result = new LinkedHashMap<>();
        processIds.stream().filter(java.util.Objects::nonNull).forEach(id -> result.put(id, id));
        return result;
    }

    @Test
    void buildScheduleConfigMap_shouldKeepFrozenRouteProcessId() {
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(600101L)
                .routeId(500101L)
                .build();
        MesProRouteScheduleConfigDO historicalConfig = MesProRouteScheduleConfigDO.builder()
                .id(750101L)
                .routeVersionId(600101L)
                .routeProcessId(710100L)
                .build();
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(710100L)
                .routeId(500101L)
                .processId(700100L)
                .build();
        when(routeScheduleConfigMapper.selectListByRouteVersionId(600101L))
                .thenReturn(List.of(historicalConfig));
        when(routeProcessService.resolveFrozenRouteProcess(710100L, 500101L, null))
                .thenReturn(frozenRouteProcess);

        @SuppressWarnings("unchecked")
        Map<Long, MesProRouteScheduleConfigDO> result = ReflectionTestUtils.invokeMethod(
                scheduleOrderService, "buildScheduleConfigMap", routeVersion);

        assertNotNull(result);
        assertEquals(historicalConfig, result.get(710100L));
        verify(routeProcessService, never()).resolveCurrentRouteProcess(710100L, 500101L, null);
    }

    @Test
    void resolveScheduleRouteFlowConfigMap_shouldKeepFrozenRouteProcessId() {
        MesProRouteFlowProcessConfigDO historicalConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(760101L)
                .routeFlowConfigId(500101L)
                .routeId(500101L)
                .routeProcessId(710100L)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(710100L)
                .routeId(500101L)
                .processId(700100L)
                .build();
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                500101L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(historicalConfig));
        when(routeProcessService.resolveFrozenRouteProcess(710100L, 500101L, null))
                .thenReturn(frozenRouteProcess);

        @SuppressWarnings("unchecked")
        Map<Long, MesProRouteFlowProcessConfigDO> result = ReflectionTestUtils.invokeMethod(
                scheduleOrderService, "resolveScheduleRouteFlowConfigMap", 500101L);

        assertNotNull(result);
        assertEquals(historicalConfig, result.get(710100L));
        verify(routeProcessService, never()).resolveCurrentRouteProcess(710100L, 500101L, null);
    }

    @Test
    void resolveRouteProcessWipKey_shouldKeepFrozenRouteProcessId() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900101L)
                .routeId(500101L)
                .routeVersionId(600101L)
                .build();
        MesProScheduleOrderProcessDO historicalProcess = MesProScheduleOrderProcessDO.builder()
                .id(800101L)
                .scheduleOrderId(900101L)
                .routeVersionId(600101L)
                .routeProcessId(710100L)
                .processId(700100L)
                .build();
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(710100L)
                .routeId(500101L)
                .processId(700100L)
                .build();
        when(routeProcessService.resolveFrozenRouteProcess(710100L, 500101L, 700100L))
                .thenReturn(frozenRouteProcess);

        Object key = ReflectionTestUtils.invokeMethod(scheduleOrderService,
                "resolveRouteProcessWipKey", historicalProcess, scheduleOrder);

        assertNotNull(key);
        assertEquals(710100L, ReflectionTestUtils.getField(key, "routeProcessId"));
        verify(routeProcessService, never()).resolveCurrentRouteProcess(710100L, 500101L, 700100L);
    }

    @Mock
    private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;

    @Test
    void getProcessWipStatistics_shouldExcludeFrozenScheduleOrders() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900331L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .frozen(Boolean.TRUE)
                .manualFinished(Boolean.FALSE)
                .build();
        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(0, result.size());
        verify(scheduleOrderMapper).selectListForProcessWip();
        org.mockito.Mockito.verifyNoInteractions(scheduleOrderProcessMapper, processMapper);
        verify(scheduleOrderMapper, never()).selectListForNightlyReplan();
    }

    @Test
    void getProcessWipStatistics_shouldReturnResolvableRowsWhenHistoricalRouteProcessIsMissing() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900332L)
                .routeId(500332L)
                .routeVersionId(600332L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .frozen(Boolean.FALSE)
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO validProcess = MesProScheduleOrderProcessDO.builder()
                .id(800332L)
                .scheduleOrderId(900332L)
                .routeVersionId(600332L)
                .routeProcessId(710332L)
                .processId(700332L)
                .processCode("VALID-332")
                .processName("有效工序")
                .sort(1)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.ZERO)
                .remainingQuantity(BigDecimal.TEN)
                .build();
        MesProScheduleOrderProcessDO orphanProcess = MesProScheduleOrderProcessDO.builder()
                .id(800333L)
                .scheduleOrderId(900332L)
                .routeVersionId(600332L)
                .routeProcessId(710333L)
                .processId(700333L)
                .processCode("ORPHAN-333")
                .processName("历史孤儿工序")
                .sort(2)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.ZERO)
                .remainingQuantity(BigDecimal.TEN)
                .build();
        MesProRouteProcessDO validRouteProcess = MesProRouteProcessDO.builder()
                .id(710332L)
                .routeId(500332L)
                .processId(700332L)
                .sort(1)
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900332L)))
                .thenReturn(List.of(validProcess, orphanProcess));
        when(routeProcessMapper.selectBatchIds(Set.of(710332L, 710333L)))
                .thenReturn(List.of(validRouteProcess));
        when(processMapper.selectBatchIds(Set.of(700332L))).thenReturn(List.of(
                MesProProcessDO.builder().id(700332L).code("VALID-332").name("有效工序").build()));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600332L, 710332L))
                .thenReturn(routeConfig(750332L, 600332L, 710332L, false));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        assertEquals(710332L, result.get(0).getRouteProcessId());
        assertEquals("VALID-332", result.get(0).getProcessCode());
        verify(routeProcessService, never()).resolveFrozenRouteProcess(710333L, 500332L, 700333L);
    }

    @Test
    void getScheduleOrderPage_shouldMatchWorkbenchWipProcessFilterByAnyUnfinishedProcess() {
        MesProScheduleOrderPageReqVO pageReqVO = new MesProScheduleOrderPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);
        pageReqVO.setCurrentProcessId(922788L);
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900372L)
                .routeId(922046L)
                .routeVersionId(7L)
                .build();
        MesProScheduleOrderProcessDO firstUnfinishedProcess = MesProScheduleOrderProcessDO.builder()
                .id(800371L)
                .scheduleOrderId(900372L)
                .routeProcessId(922339L)
                .processId(922744L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.ZERO)
                .build();
        MesProScheduleOrderProcessDO workbenchWipProcess = MesProScheduleOrderProcessDO.builder()
                .id(800372L)
                .scheduleOrderId(900372L)
                .routeProcessId(922372L)
                .processId(922759L)
                .sort(15)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.ZERO)
                .build();
        when(scheduleOrderMapper.selectPageByProductIds(pageReqVO, List.of()))
                .thenReturn(new PageResult<>(List.of(scheduleOrder), 1L));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900372L)))
                .thenReturn(List.of(firstUnfinishedProcess, workbenchWipProcess));
        org.mockito.Mockito.lenient().when(routeProcessService.resolveFrozenRouteProcess(922339L, 922046L, 922744L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(922339L).routeId(922046L).processId(922789L).build());
        org.mockito.Mockito.lenient().when(routeProcessService.resolveFrozenRouteProcess(922372L, 922046L, 922759L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(922372L).routeId(922046L).processId(922788L).build());

        PageResult<MesProScheduleOrderDO> result = scheduleOrderService.getScheduleOrderPage(pageReqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(scheduleOrder), result.getList());
    }

    @Test
    void getScheduleOrderPage_shouldIgnoreZeroCurrentProcessIdSentinel() {
        MesProScheduleOrderPageReqVO pageReqVO = new MesProScheduleOrderPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);
        pageReqVO.setCurrentProcessId(0L);
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900373L)
                .routeId(null)
                .routeVersionId(null)
                .build();
        MesProScheduleOrderProcessDO invalidSentinelProcess = MesProScheduleOrderProcessDO.builder()
                .id(800373L)
                .scheduleOrderId(900373L)
                .routeProcessId(null)
                .processId(0L)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.ZERO)
                .build();
        when(scheduleOrderMapper.selectPageByProductIds(pageReqVO, List.of()))
                .thenReturn(new PageResult<>(List.of(scheduleOrder), 1L));
        org.mockito.Mockito.lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900373L)))
                .thenReturn(List.of(invalidSentinelProcess));
        org.mockito.Mockito.lenient().doThrow(new IllegalStateException(
                        "无法解析当前工艺路线工序，routeId=null，sourceProcessId=0，routeProcessId=null，processCode=null"))
                .when(routeProcessService).resolveFrozenRouteProcess(null, null, 0L);

        PageResult<MesProScheduleOrderDO> result = scheduleOrderService.getScheduleOrderPage(pageReqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(scheduleOrder), result.getList());
        verify(routeProcessService, never()).resolveFrozenRouteProcess(null, null, 0L);
    }

    @Test
    void getProcessWipStatistics_shouldAnchorEstimatedTimesToLatestPlannedStartTime() {
        LocalDateTime earlierStartTime = LocalDate.now().plusDays(5).atStartOfDay();
        LocalDateTime latestStartTime = LocalDate.now().plusDays(12).atStartOfDay();
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900351L)
                .routeId(500351L)
                .routeVersionId(600351L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900352L)
                .routeId(500351L)
                .routeVersionId(600351L)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO firstProcess = MesProScheduleOrderProcessDO.builder()
                .id(800351L)
                .scheduleOrderId(900351L)
                .routeVersionId(600351L)
                .routeProcessId(710351L)
                .processId(700351L)
                .processCode("P-351")
                .processName("预计时间工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("2500.000000"))
                .shiftCapacityTotal(new BigDecimal("960.000000"))
                .plannedStartTime(earlierStartTime)
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(800352L)
                .scheduleOrderId(900352L)
                .routeVersionId(600351L)
                .routeProcessId(710351L)
                .processId(700351L)
                .processCode("P-351")
                .processName("预计时间工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("10"))
                .remainingQuantity(new BigDecimal("2471.000000"))
                .shiftCapacityTotal(new BigDecimal("960.000000"))
                .plannedStartTime(latestStartTime)
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900351L, 900352L)))
                .thenReturn(List.of(firstProcess, secondProcess));
        stubCurrentRouteProcessDefinitions(firstProcess, secondProcess);
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(
                Set.of(800351L, 800352L), LocalDate.now())).thenReturn(List.of());
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600351L, 710351L))
                .thenReturn(routeConfigWithShiftCapacity(750351L, 600351L, 710351L, "960.000000", false));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(latestStartTime, row.getEstimatedStartTime());
        assertNull(row.getEstimatedCompletionTime());
    }

    @Test
    void getProcessWipStatistics_shouldEstimateCompletionByProcessMaxCapacityWhenCurrentCapacityMissing() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(900361L)
                .routeId(500361L)
                .routeVersionId(600361L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(800361L)
                .scheduleOrderId(900361L)
                .routeVersionId(600361L)
                .routeProcessId(710361L)
                .processId(700361L)
                .processCode("P-361")
                .processName("最大产能估算工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("120.000000"))
                .shiftCapacityTotal(BigDecimal.ZERO)
                .build();
        MesMdWorkstationDO maxCapacityWorkstation = MesMdWorkstationDO.builder()
                .id(510361L)
                .code("WS-361")
                .name("最大产能工作站")
                .processId(700361L)
                .singleStandardHourlyCapacity(new BigDecimal("5.000000"))
                .shiftHours(new BigDecimal("10.000000"))
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(order));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900361L)))
                .thenReturn(List.of(process));
        stubCurrentRouteProcessDefinitions(process);
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(
                Set.of(800361L), LocalDate.now())).thenReturn(List.of());
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600361L, 710361L))
                .thenReturn(routeConfigWithShiftCapacity(750361L, 600361L, 710361L, "0.000000", false));
        org.mockito.Mockito.lenient().when(workstationMapper.selectListByProcessIds(
                        List.of(700361L), CommonStatusEnum.ENABLE.getStatus()))
                .thenReturn(List.of(maxCapacityWorkstation));
        org.mockito.Mockito.lenient().when(workstationMachineMapper.selectListByWorkstationIds(List.of(510361L)))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(workstationWorkerMapper.selectListByWorkstationIds(List.of(510361L)))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(610361L)
                        .workstationId(510361L)
                        .quantity(1)
                        .build()));
        org.mockito.Mockito.lenient().when(machineryProcessMapper.selectListByMachineryIds(Set.of()))
                .thenReturn(List.of());

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(BigDecimal.ZERO.setScale(6), row.getShiftCapacityTotal());
        assertEquals(LocalDate.now().atStartOfDay().plusDays(3), row.getEstimatedCompletionTime());
    }

    @Test
    void getProcessWipStatistics_shouldAggregateAllUnfinishedEnabledProcesses() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900401L)
                .routeId(500401L)
                .routeVersionId(600401L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900402L)
                .routeId(500401L)
                .routeVersionId(600401L)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO firstOrderBlow = MesProScheduleOrderProcessDO.builder()
                .id(800401L)
                .scheduleOrderId(900401L)
                .routeVersionId(600401L)
                .routeProcessId(710401L)
                .processId(700401L)
                .processCode("B010")
                .processName("吹球囊成型")
                .sort(1)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("35"))
                .remainingQuantity(new BigDecimal("65"))
                .build();
        MesProScheduleOrderProcessDO firstOrderAssembly = MesProScheduleOrderProcessDO.builder()
                .id(800402L)
                .scheduleOrderId(900401L)
                .routeVersionId(600401L)
                .routeProcessId(710402L)
                .processId(700402L)
                .processCode("A020")
                .processName("组装")
                .sort(2)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("80"))
                .build();
        MesProScheduleOrderProcessDO secondOrderAssembly = MesProScheduleOrderProcessDO.builder()
                .id(800403L)
                .scheduleOrderId(900402L)
                .routeVersionId(600401L)
                .routeProcessId(710402L)
                .processId(700402L)
                .processCode("A020")
                .processName("组装")
                .sort(2)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("10"))
                .remainingQuantity(new BigDecimal("90"))
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900401L, 900402L)))
                .thenReturn(List.of(firstOrderBlow, firstOrderAssembly, secondOrderAssembly));
        stubCurrentRouteProcessDefinitions(firstOrderBlow, firstOrderAssembly, secondOrderAssembly);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600401L, 710401L))
                .thenReturn(routeConfig(750401L, 600401L, 710401L, false));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600401L, 710402L))
                .thenReturn(routeConfig(750402L, 600401L, 710402L, false));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(2, result.size());
        MesProScheduleOrderProcessWipRespVO assemblyWip = result.stream()
                .filter(row -> row.getProcessId().equals(700402L))
                .findFirst()
                .orElseThrow();
        assertEquals(700402L, assemblyWip.getProcessId());
        assertEquals("A020", assemblyWip.getProcessCode());
        assertEquals("组装", assemblyWip.getProcessName());
        assertEquals(2L, assemblyWip.getWipOrderCount());
        assertEquals(List.of(900401L, 900402L), assemblyWip.getScheduleOrderIds());
        MesProScheduleOrderProcessWipRespVO blowWip = result.stream()
                .filter(row -> row.getProcessId().equals(700401L))
                .findFirst()
                .orElseThrow();
        assertEquals(700401L, blowWip.getProcessId());
        assertEquals("B010", blowWip.getProcessCode());
        assertEquals("吹球囊成型", blowWip.getProcessName());
        assertEquals(1L, blowWip.getWipOrderCount());
        assertEquals(List.of(900401L), blowWip.getScheduleOrderIds());
    }

    @Test
    void getProcessWipStatistics_shouldUseCurrentRouteProcessDefinitionForAllWorkbenchWip() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900451L)
                .routeId(500451L)
                .routeVersionId(600451L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900452L)
                .routeId(500451L)
                .routeVersionId(600451L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO firstCurrentProcess = MesProScheduleOrderProcessDO.builder()
                .id(800451L)
                .scheduleOrderId(900451L)
                .routeVersionId(600451L)
                .routeProcessId(710451L)
                .processId(700451L)
                .processCode("CUR-451")
                .processName("当前吹球囊")
                .sort(1)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("80"))
                .shiftCapacityTotal(new BigDecimal("40"))
                .build();
        MesProScheduleOrderProcessDO firstNextProcess = MesProScheduleOrderProcessDO.builder()
                .id(800452L)
                .scheduleOrderId(900451L)
                .routeVersionId(600451L)
                .routeProcessId(710452L)
                .processId(700452L)
                .processCode("NEXT-452")
                .processName("后续组装")
                .sort(2)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("100"))
                .shiftCapacityTotal(new BigDecimal("50"))
                .build();
        MesProScheduleOrderProcessDO secondCurrentProcess = MesProScheduleOrderProcessDO.builder()
                .id(800453L)
                .scheduleOrderId(900452L)
                .routeVersionId(600451L)
                .routeProcessId(710451L)
                .processId(700451L)
                .processCode("CUR-451")
                .processName("当前吹球囊")
                .sort(1)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("10"))
                .remainingQuantity(new BigDecimal("90"))
                .shiftCapacityTotal(new BigDecimal("45"))
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900451L, 900452L)))
                .thenReturn(List.of(firstCurrentProcess, firstNextProcess, secondCurrentProcess));
        stubCurrentRouteProcessDefinitions(firstCurrentProcess, firstNextProcess, secondCurrentProcess);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600451L, 710451L))
                .thenReturn(routeConfigWithShiftCapacity(750451L, 600451L, 710451L, "40.000000", false));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600451L, 710452L))
                .thenReturn(routeConfigWithShiftCapacity(750452L, 600451L, 710452L, "50.000000", false));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(2, result.size());
        MesProScheduleOrderProcessWipRespVO currentWip = result.stream()
                .filter(row -> row.getProcessId().equals(700451L))
                .findFirst()
                .orElseThrow();
        assertEquals(700451L, currentWip.getProcessId());
        assertEquals("CUR-451", currentWip.getProcessCode());
        assertEquals("当前吹球囊", currentWip.getProcessName());
        assertEquals(2L, currentWip.getWipOrderCount());
        assertEquals(BigDecimal.ZERO.setScale(6), currentWip.getShiftCapacityTotal());
        assertEquals(new BigDecimal("170.000000"), currentWip.getUnfinishedDemandQuantity());
        assertEquals(List.of(900451L, 900452L), currentWip.getScheduleOrderIds());
        MesProScheduleOrderProcessWipRespVO nextWip = result.stream()
                .filter(row -> row.getProcessId().equals(700452L))
                .findFirst()
                .orElseThrow();
        assertEquals("NEXT-452", nextWip.getProcessCode());
        assertEquals("后续组装", nextWip.getProcessName());
        assertEquals(1L, nextWip.getWipOrderCount());
        assertEquals(BigDecimal.ZERO.setScale(6), nextWip.getShiftCapacityTotal());
        assertEquals(new BigDecimal("100.000000"), nextWip.getUnfinishedDemandQuantity());
        assertEquals(List.of(900451L), nextWip.getScheduleOrderIds());
    }

    @Test
    void getProcessWipStatistics_shouldUseCurrentRouteProcessDefinitionInsteadOfStaleSnapshot() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(900461L)
                .routeId(500461L)
                .routeVersionId(600461L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO staleSnapshot = MesProScheduleOrderProcessDO.builder()
                .id(800461L)
                .scheduleOrderId(900461L)
                .routeVersionId(600461L)
                .routeProcessId(710461L)
                .processId(700461L)
                .processCode("OLD-461")
                .processName("历史工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("80"))
                .shiftCapacityTotal(new BigDecimal("40"))
                .build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(710461L)
                .routeId(500461L)
                .processId(700462L)
                .sort(1)
                .build();
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(700462L)
                .code("NEW-462")
                .name("当前路线工序")
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(order));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900461L)))
                .thenReturn(List.of(staleSnapshot));
        when(routeProcessMapper.selectBatchIds(Set.of(710461L))).thenReturn(List.of(currentRouteProcess));
        when(processMapper.selectBatchIds(Set.of(700462L))).thenReturn(List.of(currentProcess));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600461L, 710461L))
                .thenReturn(routeConfig(750461L, 600461L, 710461L, false));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(700462L, row.getProcessId());
        assertEquals("NEW-462", row.getProcessCode());
        assertEquals("当前路线工序", row.getProcessName());
    }

    @Test
    void getProcessWipStatistics_shouldLoadFrozenDeletedRouteProcessIdentity() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(900499L)
                .routeId(500499L)
                .routeVersionId(600499L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO frozenProcess = MesProScheduleOrderProcessDO.builder()
                .id(800499L)
                .scheduleOrderId(900499L)
                .routeVersionId(600499L)
                .routeProcessId(710499L)
                .processId(700499L)
                .processCode("FROZEN-499")
                .processName("历史冻结工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("25"))
                .remainingQuantity(new BigDecimal("75"))
                .shiftCapacityTotal(new BigDecimal("40"))
                .build();
        MesProRouteProcessDO deletedRouteProcess = MesProRouteProcessDO.builder()
                .id(710499L)
                .routeId(500499L)
                .processId(700499L)
                .sort(1)
                .build();
        MesProProcessDO deletedProcess = MesProProcessDO.builder()
                .id(700499L)
                .code("FROZEN-499")
                .name("历史冻结工序")
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(order));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900499L)))
                .thenReturn(List.of(frozenProcess));
        when(routeProcessService.resolveFrozenRouteProcess(710499L, 500499L, 700499L))
                .thenReturn(deletedRouteProcess);
        when(routeProcessMapper.selectBatchIds(Set.of(710499L))).thenReturn(List.of());
        when(routeProcessMapper.selectByIdIgnoreDeleted(710499L)).thenReturn(deletedRouteProcess);
        when(processMapper.selectBatchIds(Set.of(700499L))).thenReturn(List.of());
        when(processMapper.selectListByIdsIgnoreDeleted(Set.of(700499L))).thenReturn(List.of(deletedProcess));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600499L, 710499L))
                .thenReturn(routeConfig(750499L, 600499L, 710499L, false));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(710499L, row.getRouteProcessId());
        assertEquals(700499L, row.getProcessId());
        assertEquals("FROZEN-499", row.getProcessCode());
        assertEquals("历史冻结工序", row.getProcessName());
        assertEquals(1L, row.getWipOrderCount());
    }

    @Test
    void getProcessWipStatistics_shouldExposeListMetricsForWorkbenchTable() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(900501L)
                .routeId(500501L)
                .routeVersionId(600501L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(800501L)
                .scheduleOrderId(900501L)
                .routeVersionId(600501L)
                .routeProcessId(710501L)
                .processId(700501L)
                .processCode("P-501")
                .processName("球囊成型")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("40"))
                .remainingQuantity(new BigDecimal("150.000000"))
                .shiftCapacityTotal(new BigDecimal("50.000000"))
                .nightShiftEnabled(Boolean.TRUE)
                .build();
        MesProFeedbackDO todayFeedback = MesProFeedbackDO.builder()
                .id(910501L)
                .scheduleOrderId(900501L)
                .scheduleOrderProcessId(800501L)
                .processId(700501L)
                .feedbackQuantity(new BigDecimal("12.500000"))
                .feedbackTime(LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0))
                .status(MesProFeedbackStatusEnum.FINISHED.getStatus())
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(order));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900501L)))
                .thenReturn(List.of(process));
        stubCurrentRouteProcessDefinitions(process);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600501L, 710501L))
                .thenReturn(routeConfigWithShiftCapacity(750501L, 600501L, 710501L, "50.000000", true));
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(Set.of(800501L), LocalDate.now()))
                .thenReturn(List.of(todayFeedback));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(BigDecimal.ZERO.setScale(6), row.getShiftCapacityTotal());
        assertEquals("夜班", row.getShiftStatus());
        assertEquals(new BigDecimal("150.000000"), row.getUnfinishedDemandQuantity());
        assertNull(row.getEstimatedCompletionTime());
        assertEquals(new BigDecimal("12.500000"), row.getTodayFeedbackQuantity());
    }

    @Test
    void getProcessWipStatistics_shouldUseLiveRouteCapacityInsteadOfProcessSnapshotCapacity() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(900511L)
                .routeId(500511L)
                .routeVersionId(600511L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO staleSnapshot = MesProScheduleOrderProcessDO.builder()
                .id(800511L)
                .scheduleOrderId(900511L)
                .routeVersionId(600511L)
                .routeProcessId(710511L)
                .processId(700511L)
                .processCode("P-511")
                .processName("实时产能工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("320.000000"))
                .shiftHours(new BigDecimal("10.50"))
                .shiftCapacityTotal(new BigDecimal("40.000000"))
                .build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(710511L)
                .routeId(500511L)
                .processId(700511L)
                .workstationId(510511L)
                .build();
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(700511L)
                .code("P-511")
                .name("实时产能工序")
                .build();
        MesProRouteScheduleConfigDO liveConfig = MesProRouteScheduleConfigDO.builder()
                .id(750511L)
                .routeVersionId(600511L)
                .routeProcessId(710511L)
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacity(new BigDecimal("20.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(510511L)
                .code("WS-511")
                .name("实时产能工位")
                .processId(700511L)
                .singleStandardHourlyCapacity(BigDecimal.ONE)
                .shiftHours(new BigDecimal("8.00"))
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(order));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900511L)))
                .thenReturn(List.of(staleSnapshot));
        when(routeProcessMapper.selectBatchIds(Set.of(710511L))).thenReturn(List.of(currentRouteProcess));
        when(processMapper.selectBatchIds(Set.of(700511L))).thenReturn(List.of(currentProcess));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600511L, 710511L))
                .thenReturn(liveConfig);
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(List.of(700511L)))
                .thenReturn(Map.of(700511L, 700511L));
        org.mockito.Mockito.lenient().when(workstationMapper.selectBatchIds(Set.of(510511L)))
                .thenReturn(List.of(workstation));
        org.mockito.Mockito.lenient().when(workstationMachineMapper.selectListByWorkstationIds(List.of(510511L)))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(workstationWorkerMapper.selectListByWorkstationIds(List.of(510511L)))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(610511L).workstationId(510511L).quantity(1).build()));
        org.mockito.Mockito.lenient().when(machineryProcessMapper.selectListByMachineryIds(Set.of()))
                .thenReturn(List.of());

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(new BigDecimal("160.000000"), row.getShiftCapacityTotal());
        assertEquals(LocalDate.now().atStartOfDay().plusDays(2), row.getEstimatedCompletionTime());
    }

    @Test
    void getProcessWipStatistics_shouldExposeWorkbenchManualCapacityOverrideFromSnapshots() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(900512L)
                .routeId(500512L)
                .routeVersionId(600512L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(800512L)
                .scheduleOrderId(900512L)
                .routeVersionId(600512L)
                .routeProcessId(710512L)
                .processId(700512L)
                .processCode("P-512")
                .processName("工作台覆盖产能工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("2400.000000"))
                .capacitySource("MANUAL_OVERRIDE")
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacityTotal(new BigDecimal("120.000000"))
                .shiftHours(new BigDecimal("10.000000"))
                .shiftCapacityTotal(new BigDecimal("1200.000000"))
                .build();
        MesProRouteScheduleConfigDO routeConfig = MesProRouteScheduleConfigDO.builder()
                .id(750512L)
                .routeVersionId(600512L)
                .routeProcessId(710512L)
                .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                .hourlyCapacity(null)
                .nightShiftEnabled(Boolean.FALSE)
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(order));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900512L)))
                .thenReturn(List.of(process));
        stubCurrentRouteProcessDefinitions(process);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600512L, 710512L))
                .thenReturn(routeConfig);
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(
                Set.of(800512L), LocalDate.now())).thenReturn(List.of());

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(new BigDecimal("1200.000000"), row.getShiftCapacityTotal());
        assertEquals(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode(), row.getCapacityMode());
        assertEquals("MANUAL_OVERRIDE", row.getCapacitySource());
        assertEquals(LocalDate.now().atStartOfDay().plusDays(2), row.getEstimatedCompletionTime());
    }

    @Test
    void getProcessWipStatistics_shouldExposeCapacityModeAndSourceForNavigation() {
        MesProScheduleOrderDO manualOrder = MesProScheduleOrderDO.builder()
                .id(900521L)
                .routeId(500521L)
                .routeVersionId(600521L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO machineOrder = MesProScheduleOrderDO.builder()
                .id(900522L)
                .routeId(500522L)
                .routeVersionId(600522L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO workerOrder = MesProScheduleOrderDO.builder()
                .id(900523L)
                .routeId(500523L)
                .routeVersionId(600523L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO manualProcess = MesProScheduleOrderProcessDO.builder()
                .id(800521L)
                .scheduleOrderId(900521L)
                .routeVersionId(600521L)
                .routeProcessId(710521L)
                .processId(700521L)
                .processCode("P-521")
                .processName("手动覆盖工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("60.000000"))
                .shiftCapacityTotal(new BigDecimal("10.000000"))
                .build();
        MesProScheduleOrderProcessDO machineProcess = MesProScheduleOrderProcessDO.builder()
                .id(800522L)
                .scheduleOrderId(900522L)
                .routeVersionId(600522L)
                .routeProcessId(710522L)
                .processId(700522L)
                .processCode("P-522")
                .processName("设备资源工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("60.000000"))
                .shiftCapacityTotal(new BigDecimal("10.000000"))
                .build();
        MesProScheduleOrderProcessDO workerProcess = MesProScheduleOrderProcessDO.builder()
                .id(800523L)
                .scheduleOrderId(900523L)
                .routeVersionId(600523L)
                .routeProcessId(710523L)
                .processId(700523L)
                .processCode("P-523")
                .processName("人工资源工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("60.000000"))
                .shiftCapacityTotal(new BigDecimal("10.000000"))
                .build();
        MesProRouteProcessDO manualRouteProcess = MesProRouteProcessDO.builder()
                .id(710521L)
                .routeId(500521L)
                .processId(700521L)
                .build();
        MesProRouteProcessDO machineRouteProcess = MesProRouteProcessDO.builder()
                .id(710522L)
                .routeId(500522L)
                .processId(700522L)
                .workstationId(510522L)
                .build();
        MesProRouteProcessDO workerRouteProcess = MesProRouteProcessDO.builder()
                .id(710523L)
                .routeId(500523L)
                .processId(700523L)
                .workstationId(510523L)
                .build();
        MesMdWorkstationDO machineWorkstation = MesMdWorkstationDO.builder()
                .id(510522L)
                .code("WS-M")
                .name("设备工位")
                .processId(700522L)
                .shiftHours(new BigDecimal("8.00"))
                .build();
        MesMdWorkstationDO workerWorkstation = MesMdWorkstationDO.builder()
                .id(510523L)
                .code("WS-W")
                .name("人工工位")
                .processId(700523L)
                .singleStandardHourlyCapacity(new BigDecimal("5.000000"))
                .shiftHours(new BigDecimal("8.00"))
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(manualOrder, machineOrder, workerOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900521L, 900522L, 900523L)))
                .thenReturn(List.of(manualProcess, machineProcess, workerProcess));
        when(routeProcessMapper.selectBatchIds(Set.of(710521L, 710522L, 710523L)))
                .thenReturn(List.of(manualRouteProcess, machineRouteProcess, workerRouteProcess));
        when(processMapper.selectBatchIds(Set.of(700521L, 700522L, 700523L))).thenReturn(List.of(
                MesProProcessDO.builder().id(700521L).code("P-521").name("手动覆盖工序").build(),
                MesProProcessDO.builder().id(700522L).code("P-522").name("设备资源工序").build(),
                MesProProcessDO.builder().id(700523L).code("P-523").name("人工资源工序").build()));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600521L, 710521L))
                .thenReturn(MesProRouteScheduleConfigDO.builder()
                        .id(750521L)
                        .routeVersionId(600521L)
                        .routeProcessId(710521L)
                        .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                        .hourlyCapacity(new BigDecimal("12.000000"))
                        .build());
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600522L, 710522L))
                .thenReturn(MesProRouteScheduleConfigDO.builder()
                        .id(750522L)
                        .routeVersionId(600522L)
                        .routeProcessId(710522L)
                        .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                        .build());
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600523L, 710523L))
                .thenReturn(MesProRouteScheduleConfigDO.builder()
                        .id(750523L)
                        .routeVersionId(600523L)
                        .routeProcessId(710523L)
                        .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                        .build());
        when(routeProcessService.getProcessIdentityMap(List.of(700521L, 700522L, 700523L)))
                .thenReturn(Map.of(700521L, 700521L, 700522L, 700522L, 700523L, 700523L));
        when(workstationMapper.selectBatchIds(Set.of(510522L, 510523L)))
                .thenReturn(List.of(machineWorkstation, workerWorkstation));
        when(workstationMachineMapper.selectListByWorkstationIds(List.of(510522L, 510523L)))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder()
                        .id(610522L)
                        .workstationId(510522L)
                        .machineryId(620522L)
                        .quantity(2)
                        .build()));
        when(workstationWorkerMapper.selectListByWorkstationIds(List.of(510522L, 510523L)))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(610523L)
                        .workstationId(510523L)
                        .quantity(4)
                        .build()));
        when(machineryMapper.selectBatchIds(Set.of(620522L))).thenReturn(List.of(
                MesDvMachineryDO.builder().id(620522L).code("M-522").name("设备522").build()));
        when(machineryProcessMapper.selectListByMachineryIds(Set.of(620522L))).thenReturn(List.of(
                MesDvMachineryProcessDO.builder()
                        .machineryId(620522L)
                        .processId(700522L)
                        .standardHourlyCapacity(new BigDecimal("30.000000"))
                        .build()));
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(
                Set.of(800521L, 800522L, 800523L), LocalDate.now())).thenReturn(List.of());

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        MesProScheduleOrderProcessWipRespVO manualRow = result.stream()
                .filter(row -> row.getRouteProcessId().equals(710521L))
                .findFirst()
                .orElseThrow();
        assertEquals(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode(), manualRow.getCapacityMode());
        assertEquals(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode(), manualRow.getCapacitySource());
        assertEquals("V600521", manualRow.getRouteVersionNo());
        assertEquals("ACTIVE", manualRow.getRouteVersionStatus());
        MesProScheduleOrderProcessWipRespVO machineRow = result.stream()
                .filter(row -> row.getRouteProcessId().equals(710522L))
                .findFirst()
                .orElseThrow();
        assertEquals(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode(), machineRow.getCapacityMode());
        assertEquals("MACHINE", machineRow.getCapacitySource());
        assertEquals("V600522", machineRow.getRouteVersionNo());
        assertEquals("ACTIVE", machineRow.getRouteVersionStatus());
        MesProScheduleOrderProcessWipRespVO workerRow = result.stream()
                .filter(row -> row.getRouteProcessId().equals(710523L))
                .findFirst()
                .orElseThrow();
        assertEquals(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode(), workerRow.getCapacityMode());
        assertEquals("WORKER", workerRow.getCapacitySource());
        assertEquals("V600523", workerRow.getRouteVersionNo());
        assertEquals("ACTIVE", workerRow.getRouteVersionStatus());
    }

    @Test
    void getProcessWipStatistics_shouldUseManualOverrideCapacityWhenWorkerQuantityMissing() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(900467L)
                .routeId(500467L)
                .routeVersionId(600467L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO wipProcess = MesProScheduleOrderProcessDO.builder()
                .id(800467L)
                .scheduleOrderId(900467L)
                .routeVersionId(600467L)
                .routeProcessId(922467L)
                .processId(700467L)
                .processCode("P-467")
                .processName("人工人数缺失工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("120.000000"))
                .build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(922467L)
                .routeId(500467L)
                .processId(700467L)
                .workstationId(922707L)
                .build();
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(922707L)
                .code("WS-922707")
                .name("人工人数缺失工位")
                .processId(700467L)
                .singleStandardHourlyCapacity(new BigDecimal("2.000000"))
                .shiftHours(new BigDecimal("8.00"))
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(order));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900467L)))
                .thenReturn(List.of(wipProcess));
        when(routeMapper.selectBatchIds(Set.of(500467L))).thenReturn(List.of(
                MesProRouteDO.builder().id(500467L).code("R-467").name("人工人数缺失路线").build()));
        when(routeVersionMapper.selectBatchIds(Set.of(600467L))).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(600467L).routeId(500467L)
                        .versionNo("V600467").lifecycleStatus("ACTIVE").build()));
        when(routeProcessMapper.selectBatchIds(Set.of(922467L))).thenReturn(List.of(currentRouteProcess));
        when(processMapper.selectBatchIds(Set.of(700467L))).thenReturn(List.of(
                MesProProcessDO.builder().id(700467L).code("P-467").name("人工人数缺失工序").build()));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600467L, 922467L))
                .thenReturn(MesProRouteScheduleConfigDO.builder()
                        .id(750467L)
                        .routeVersionId(600467L)
                        .routeProcessId(922467L)
                        .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                        .hourlyCapacity(new BigDecimal("2.000000"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build());
        when(routeProcessService.getProcessIdentityMap(List.of(700467L))).thenReturn(Map.of(700467L, 700467L));
        when(workstationMapper.selectBatchIds(Set.of(922707L))).thenReturn(List.of(workstation));
        when(workstationMachineMapper.selectListByWorkstationIds(List.of(922707L))).thenReturn(List.of());
        when(workstationWorkerMapper.selectListByWorkstationIds(List.of(922707L))).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(922807L).workstationId(922707L).quantity(null).build()));
        when(machineryProcessMapper.selectListByMachineryIds(Set.of())).thenReturn(List.of());
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(Set.of(800467L), LocalDate.now()))
                .thenReturn(List.of());

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(922467L, row.getRouteProcessId());
        assertEquals("MANUAL_OVERRIDE", row.getCapacitySource());
        assertEquals("NORMAL", row.getResourceStatus());
        assertEquals("正常", row.getResourceStatusReason());
        assertEquals(new BigDecimal("16.00000000").setScale(6), row.getShiftCapacityTotal().setScale(6));
        assertNotNull(row.getEstimatedCompletionTime());
    }

    @Test
    void getProcessWipStatistics_shouldGroupByRouteProcessAndUseSingleCapacity() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900551L)
                .routeId(500551L)
                .routeVersionId(600551L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900552L)
                .routeId(500551L)
                .routeVersionId(600551L)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO firstProcess = MesProScheduleOrderProcessDO.builder()
                .id(800551L)
                .scheduleOrderId(900551L)
                .routeVersionId(600551L)
                .routeProcessId(700551L)
                .processId(710551L)
                .processCode("P-551")
                .processName("共享路线工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .remainingQuantity(new BigDecimal("50.000000"))
                .shiftCapacityTotal(new BigDecimal("40.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(800552L)
                .scheduleOrderId(900552L)
                .routeVersionId(600551L)
                .routeProcessId(700551L)
                .processId(710551L)
                .processCode("P-551")
                .processName("共享路线工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("30"))
                .remainingQuantity(new BigDecimal("30.000000"))
                .shiftCapacityTotal(new BigDecimal("40.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProRouteDO route = MesProRouteDO.builder()
                .id(500551L)
                .code("ROUTE-551")
                .name("共享产品路线")
                .build();
        MesProRouteScheduleConfigDO config =
                routeConfigWithShiftCapacity(750551L, 600551L, 700551L, "40.000000", true);
        config.setCalendarRuleId(1L);

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900551L, 900552L)))
                .thenReturn(List.of(firstProcess, secondProcess));
        stubCurrentRouteProcessDefinitions(firstProcess, secondProcess);
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(
                Set.of(800551L, 800552L), LocalDate.now())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(routeMapper.selectBatchIds(Set.of(500551L))).thenReturn(List.of(route));
        org.mockito.Mockito.lenient().when(
                routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600551L, 700551L))
                .thenReturn(config);

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(500551L, row.getRouteId());
        assertEquals("ROUTE-551", row.getRouteCode());
        assertEquals("共享产品路线", row.getRouteName());
        assertEquals(600551L, row.getRouteVersionId());
        assertEquals(700551L, row.getRouteProcessId());
        assertEquals(710551L, row.getProcessId());
        assertEquals(BigDecimal.ZERO.setScale(6), row.getShiftCapacityTotal());
        assertEquals(new BigDecimal("80.000000"), row.getUnfinishedDemandQuantity());
        assertEquals(Boolean.TRUE, row.getNightShiftEnabled());
        assertThrows(NoSuchFieldException.class,
                () -> MesProScheduleOrderProcessWipRespVO.class.getDeclaredField("nightShiftMixed"));
    }

    @Test
    void getProcessWipStatistics_shouldSeparateSameProcessAcrossRoutes() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900561L)
                .routeId(500561L)
                .routeVersionId(600561L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900562L)
                .routeId(500562L)
                .routeVersionId(600562L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO firstProcess = MesProScheduleOrderProcessDO.builder()
                .id(800561L)
                .scheduleOrderId(900561L)
                .routeVersionId(600561L)
                .routeProcessId(700561L)
                .processId(710561L)
                .processCode("P-561")
                .processName("跨路线共用工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("10"))
                .remainingQuantity(new BigDecimal("20.000000"))
                .shiftCapacityTotal(new BigDecimal("10.000000"))
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(800562L)
                .scheduleOrderId(900562L)
                .routeVersionId(600562L)
                .routeProcessId(700562L)
                .processId(710561L)
                .processCode("P-561")
                .processName("跨路线共用工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("15"))
                .remainingQuantity(new BigDecimal("30.000000"))
                .shiftCapacityTotal(new BigDecimal("15.000000"))
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900561L, 900562L)))
                .thenReturn(List.of(firstProcess, secondProcess));
        stubCurrentRouteProcessDefinitions(firstProcess, secondProcess);
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(
                Set.of(800561L, 800562L), LocalDate.now())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(routeMapper.selectBatchIds(Set.of(500561L, 500562L)))
                .thenReturn(List.of(
                        MesProRouteDO.builder().id(500561L).code("R-561").name("路线一").build(),
                        MesProRouteDO.builder().id(500562L).code("R-562").name("路线二").build()));
        org.mockito.Mockito.lenient().when(
                routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600561L, 700561L))
                .thenReturn(MesProRouteScheduleConfigDO.builder().id(750561L)
                        .routeVersionId(600561L).routeProcessId(700561L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("10.000000")).nightShiftEnabled(Boolean.FALSE).build());
        org.mockito.Mockito.lenient().when(
                routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600562L, 700562L))
                .thenReturn(MesProRouteScheduleConfigDO.builder().id(750562L)
                        .routeVersionId(600562L).routeProcessId(700562L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("15.000000")).nightShiftEnabled(Boolean.FALSE).build());

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(2, result.size());
        assertEquals(Set.of(700561L, 700562L),
                Set.of(result.get(0).getRouteProcessId(), result.get(1).getRouteProcessId()));
    }

    @Test
    void getProcessWipStatistics_shouldUseCanonicalNightShiftAndExposePlannedStartDateMixedState() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900601L)
                .routeId(500601L)
                .routeVersionId(600601L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900602L)
                .routeId(500601L)
                .routeVersionId(600601L)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO firstProcess = MesProScheduleOrderProcessDO.builder()
                .id(800601L)
                .scheduleOrderId(900601L)
                .routeVersionId(600601L)
                .routeProcessId(710601L)
                .processId(700601L)
                .processCode("P-601")
                .processName("混合工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("25"))
                .remainingQuantity(new BigDecimal("40.000000"))
                .shiftCapacityTotal(new BigDecimal("20.000000"))
                .nightShiftEnabled(Boolean.TRUE)
                .plannedStartTime(LocalDateTime.of(2026, 7, 9, 0, 0))
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(800602L)
                .scheduleOrderId(900602L)
                .routeVersionId(600601L)
                .routeProcessId(710601L)
                .processId(700601L)
                .processCode("P-601")
                .processName("混合工序")
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("15"))
                .remainingQuantity(new BigDecimal("30.000000"))
                .shiftCapacityTotal(new BigDecimal("20.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .plannedStartTime(LocalDateTime.of(2026, 7, 10, 0, 0))
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900601L, 900602L)))
                .thenReturn(List.of(firstProcess, secondProcess));
        stubCurrentRouteProcessDefinitions(firstProcess, secondProcess);
        when(feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(Set.of(800601L, 800602L), LocalDate.now()))
                .thenReturn(List.of());
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600601L, 710601L))
                .thenReturn(routeConfig(750601L, 600601L, 710601L, true));

        List<MesProScheduleOrderProcessWipRespVO> result = scheduleOrderService.getProcessWipStatistics();

        assertEquals(1, result.size());
        MesProScheduleOrderProcessWipRespVO row = result.get(0);
        assertEquals(700601L, row.getProcessId());
        assertEquals(Boolean.TRUE, row.getNightShiftEnabled());
        assertNull(row.getPlannedStartDate());
        assertEquals(Boolean.TRUE, row.getPlannedStartDateMixed());
        assertEquals("夜班", row.getShiftStatus());
        assertEquals(new BigDecimal("70.000000"), row.getUnfinishedDemandQuantity());
    }

    @Test
    void saveProcessWipSettings_shouldUpdateCurrentWipProcessesAndWriteLogs() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900701L)
                .code("SCH-701")
                .routeId(500701L)
                .routeVersionId(600701L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900702L)
                .code("SCH-702")
                .routeId(500701L)
                .routeVersionId(600701L)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO firstProcess = MesProScheduleOrderProcessDO.builder()
                .id(800701L)
                .scheduleOrderId(900701L)
                .routeVersionId(600701L)
                .routeProcessId(710701L)
                .processId(700701L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("30"))
                .calendarRuleId(1001L)
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(800702L)
                .scheduleOrderId(900702L)
                .routeVersionId(600701L)
                .routeProcessId(710701L)
                .processId(700701L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("50"))
                .calendarRuleId(1002L)
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600701L);
        reqVO.setRouteProcessId(710701L);
        reqVO.setNightShiftEnabled(Boolean.TRUE);
        reqVO.setPlannedStartDate(LocalDate.of(2026, 7, 15));
        reqVO.setReason("工作台设置夜班和开排日期");
        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900701L, 900702L)))
                .thenReturn(List.of(firstProcess, secondProcess));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600701L, 710701L))
                .thenReturn(routeConfig(750701L, 600701L, 710701L, false).setCalendarRuleId(1001L));

        scheduleOrderService.saveProcessWipSettings(reqVO);

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper, times(2)).updateById(processCaptor.capture());
        assertEquals(Set.of(800701L, 800702L), Set.of(
                processCaptor.getAllValues().get(0).getId(),
                processCaptor.getAllValues().get(1).getId()));
        assertTrue(processCaptor.getAllValues().stream().allMatch(MesProScheduleOrderProcessDO::getNightShiftEnabled));
        assertTrue(processCaptor.getAllValues().stream().allMatch(process ->
                LocalDateTime.of(2026, 7, 15, 0, 0).equals(process.getPlannedStartTime())));

        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper, times(2)).insert(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream()
                .allMatch(log -> "PROCESS_WIP_SETTINGS".equals(log.getOperationType())));
        assertTrue(logCaptor.getAllValues().stream()
                .allMatch(log -> "工作台设置夜班和开排日期".equals(log.getReason())));
        assertTrue(logCaptor.getAllValues().stream()
                .allMatch(log -> log.getAfterSnapshotJson().contains("\"plannedStartDate\"")));
        assertTrue(logCaptor.getAllValues().stream()
                .allMatch(log -> log.getAfterSnapshotJson().contains("2026")));
        assertTrue(logCaptor.getAllValues().stream()
                .allMatch(log -> log.getAfterSnapshotJson().contains("7")));
        assertTrue(logCaptor.getAllValues().stream()
                .allMatch(log -> log.getAfterSnapshotJson().contains("15")));
    }

    @Test
    void saveProcessWipSettings_shouldPersistManualShiftCapacityAsHourlyOverrideAndSyncWipSnapshots() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900704L)
                .code("SCH-704")
                .routeId(500704L)
                .routeVersionId(600704L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(800704L)
                .scheduleOrderId(900704L)
                .routeVersionId(600704L)
                .routeProcessId(710704L)
                .processId(700704L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("30"))
                .shiftHours(new BigDecimal("10.000000"))
                .shiftCapacityTotal(new BigDecimal("1000.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProRouteScheduleConfigDO routeConfig = MesProRouteScheduleConfigDO.builder()
                .id(750704L)
                .routeVersionId(600704L)
                .routeProcessId(710704L)
                .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                .hourlyCapacity(null)
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600704L);
        reqVO.setRouteProcessId(710704L);
        reqVO.setShiftCapacityTotal(new BigDecimal("1200.000000"));
        reqVO.setReason("工作台调整班次产能");

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900704L)))
                .thenReturn(List.of(process));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600704L, 710704L))
                .thenReturn(routeConfig);

        scheduleOrderService.saveProcessWipSettings(reqVO);

        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).updateById(processCaptor.capture());
        MesProScheduleOrderProcessDO updateObj = processCaptor.getValue();
        assertEquals(800704L, updateObj.getId());
        assertEquals(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode(), updateObj.getCapacityMode());
        assertEquals("MANUAL_OVERRIDE", updateObj.getCapacitySource());
        assertEquals(new BigDecimal("120.000000"), updateObj.getHourlyCapacityTotal());
        assertEquals(new BigDecimal("1200.000000000000"), updateObj.getShiftCapacityTotal());

        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper).insert(logCaptor.capture());
        assertTrue(logCaptor.getValue().getAfterSnapshotJson().contains("\"shiftCapacityTotal\""));
        assertTrue(logCaptor.getValue().getAfterSnapshotJson().contains("1200"));
    }

    @Test
    void refreshProcessWipCapacitySnapshotsForShiftHours_shouldRescaleManualOverrideWipSnapshots() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900705L)
                .code("SCH-705")
                .routeId(500705L)
                .routeVersionId(600705L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(800705L)
                .scheduleOrderId(900705L)
                .routeVersionId(600705L)
                .routeProcessId(710705L)
                .processId(700705L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("30"))
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .capacitySource("MANUAL_OVERRIDE")
                .hourlyCapacityTotal(new BigDecimal("120.000000"))
                .shiftHours(new BigDecimal("10.000000"))
                .shiftCapacityTotal(new BigDecimal("1200.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProRouteScheduleConfigDO routeConfig = MesProRouteScheduleConfigDO.builder()
                .id(750705L)
                .routeVersionId(600705L)
                .routeProcessId(710705L)
                .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                .hourlyCapacity(null)
                .nightShiftEnabled(Boolean.FALSE)
                .build();

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900705L)))
                .thenReturn(List.of(process));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600705L, 710705L))
                .thenReturn(routeConfig);

        scheduleOrderService.refreshProcessWipCapacitySnapshotsForShiftHours(new BigDecimal("9.000000"));

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).updateById(processCaptor.capture());
        MesProScheduleOrderProcessDO updateObj = processCaptor.getValue();
        assertEquals(800705L, updateObj.getId());
        assertEquals(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode(), updateObj.getCapacityMode());
        assertEquals("MANUAL_OVERRIDE", updateObj.getCapacitySource());
        assertEquals(new BigDecimal("120.000000"), updateObj.getHourlyCapacityTotal());
        assertEquals(new BigDecimal("9.000000"), updateObj.getShiftHours());
        assertEquals(new BigDecimal("1080.000000000000"), updateObj.getShiftCapacityTotal());
    }

    @Test
    void saveProcessWipSettings_shouldMatchFrozenRouteProcessSnapshot() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900703L)
                .code("SCH-703")
                .routeId(500703L)
                .routeVersionId(600703L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO historicalProcess = MesProScheduleOrderProcessDO.builder()
                .id(800703L)
                .scheduleOrderId(900703L)
                .routeVersionId(600703L)
                .routeProcessId(710700L)
                .processId(700700L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("30"))
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600703L);
        reqVO.setRouteProcessId(710700L);
        reqVO.setNightShiftEnabled(Boolean.FALSE);
        reqVO.setReason("修复旧工序快照");
        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900703L)))
                .thenReturn(List.of(historicalProcess));
        when(routeProcessService.resolveFrozenRouteProcess(710700L, 500703L, 700700L))
                .thenReturn(MesProRouteProcessDO.builder()
                        .id(710700L).routeId(500703L).processId(700700L).build());
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600703L, 710700L))
                .thenReturn(routeConfig(750703L, 600703L, 710700L, false));

        scheduleOrderService.saveProcessWipSettings(reqVO);

        ArgumentCaptor<MesProScheduleOrderProcessDO> captor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).updateById(captor.capture());
        assertEquals(800703L, captor.getValue().getId());
        assertEquals(710700L, captor.getValue().getRouteProcessId());
        verify(scheduleOrderOperationLogMapper).insert(any(MesProScheduleOrderOperationLogDO.class));
        verify(routeProcessService, never()).resolveCurrentRouteProcess(710700L, 500703L, 700700L);
    }

    @Test
    void saveProcessWipSettings_shouldUpdateOnlyTargetRouteProcess() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(900705L)
                .code("SCH-705")
                .routeId(500705L)
                .routeVersionId(600705L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(900706L)
                .code("SCH-706")
                .routeId(500706L)
                .routeVersionId(600706L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO targetProcess = MesProScheduleOrderProcessDO.builder()
                .id(800705L)
                .scheduleOrderId(900705L)
                .routeVersionId(600705L)
                .routeProcessId(700705L)
                .processId(710705L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .nightShiftEnabled(Boolean.TRUE)
                .build();
        MesProScheduleOrderProcessDO otherRouteProcess = MesProScheduleOrderProcessDO.builder()
                .id(800706L)
                .scheduleOrderId(900706L)
                .routeVersionId(600706L)
                .routeProcessId(700706L)
                .processId(710705L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("25"))
                .nightShiftEnabled(Boolean.TRUE)
                .build();
        MesProRouteScheduleConfigDO targetConfig = MesProRouteScheduleConfigDO.builder()
                .id(750705L)
                .routeVersionId(600705L)
                .routeProcessId(700705L)
                .nightShiftEnabled(Boolean.TRUE)
                .build();
        MesProRouteScheduleConfigDO otherConfig = MesProRouteScheduleConfigDO.builder()
                .id(750706L)
                .routeVersionId(600706L)
                .routeProcessId(700706L)
                .nightShiftEnabled(Boolean.TRUE)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600705L);
        reqVO.setRouteProcessId(700705L);
        reqVO.setNightShiftEnabled(Boolean.FALSE);
        reqVO.setPlannedStartDate(LocalDate.of(2026, 7, 20));
        reqVO.setReason("只修改目标路线工序");

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900705L, 900706L)))
                .thenReturn(List.of(targetProcess, otherRouteProcess));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600705L, 700705L))
                .thenReturn(targetConfig);
        org.mockito.Mockito.lenient().when(
                routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600706L, 700706L))
                .thenReturn(otherConfig);

        scheduleOrderService.saveProcessWipSettings(reqVO);

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).updateById(processCaptor.capture());
        assertEquals(800705L, processCaptor.getValue().getId());
        assertEquals(Boolean.FALSE, processCaptor.getValue().getNightShiftEnabled());
        verify(scheduleOrderOperationLogMapper).insert(any(MesProScheduleOrderOperationLogDO.class));
        verify(routeScheduleConfigMapper).updateById(argThat(
                (MesProRouteScheduleConfigDO config) -> 750705L == config.getId()));
        verify(routeScheduleConfigMapper, never()).updateById(argThat(
                (MesProRouteScheduleConfigDO config) -> 750706L == config.getId()));
    }

    @Test
    void saveProcessWipSettings_shouldBackfillCalendarRuleFromRouteScheduleConfigWhenEnablingNightShift() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900711L)
                .code("SCH-711")
                .routeId(500711L)
                .routeVersionId(600711L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(800711L)
                .scheduleOrderId(900711L)
                .routeVersionId(600711L)
                .routeProcessId(710711L)
                .processId(700711L)
                .routeScheduleConfigId(600711L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .calendarRuleId(null)
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProRouteScheduleConfigDO routeScheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(600711L)
                .routeVersionId(600711L)
                .routeProcessId(710711L)
                .calendarRuleId(300711L)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600711L);
        reqVO.setRouteProcessId(710711L);
        reqVO.setNightShiftEnabled(Boolean.TRUE);
        reqVO.setPlannedStartDate(LocalDate.of(2026, 7, 18));
        reqVO.setReason("工作台设置夜班");
        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900711L)))
                .thenReturn(List.of(process));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600711L, 710711L))
                .thenReturn(routeScheduleConfig);

        scheduleOrderService.saveProcessWipSettings(reqVO);

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).updateById(processCaptor.capture());
        MesProScheduleOrderProcessDO updateObj = processCaptor.getValue();
        assertEquals(800711L, updateObj.getId());
        assertEquals(Boolean.TRUE, updateObj.getNightShiftEnabled());
        assertEquals(300711L, updateObj.getCalendarRuleId());
        assertEquals(LocalDateTime.of(2026, 7, 18, 0, 0), updateObj.getPlannedStartTime());
        verify(scheduleOrderOperationLogMapper).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    @Test
    void saveProcessWipSettings_shouldRefreshStaleSnapshotAndBackfillCalendarRuleWhenEnablingNightShift() {
        MesProScheduleOrderDO firstOrder = MesProScheduleOrderDO.builder()
                .id(9003941L)
                .code(new String(new char[]{'S','C','H','-','3','9','4','-','1'}))
                .routeId(920394L)
                .routeVersionId(910394L)
                .productId(930394L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO secondOrder = MesProScheduleOrderDO.builder()
                .id(9003942L)
                .code(new String(new char[]{'S','C','H','-','3','9','4','-','2'}))
                .routeId(920394L)
                .routeVersionId(910394L)
                .productId(930394L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO staleProcess = MesProScheduleOrderProcessDO.builder()
                .id(8003941L)
                .scheduleOrderId(9003941L)
                .routeProcessId(922483L)
                .routeScheduleConfigId(447L)
                .processId(900394L)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.valueOf(15L))
                .calendarRuleId(null)
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO freshProcess = MesProScheduleOrderProcessDO.builder()
                .id(8003942L)
                .scheduleOrderId(9003942L)
                .routeProcessId(922483L)
                .routeScheduleConfigId(473L)
                .processId(900394L)
                .enabled(Boolean.TRUE)
                .progressPercent(BigDecimal.valueOf(25L))
                .calendarRuleId(1L)
                .nightShiftEnabled(Boolean.TRUE)
                .build();
        MesProRouteScheduleConfigDO freshConfig = MesProRouteScheduleConfigDO.builder()
                .id(473L)
                .routeVersionId(910394L)
                .itemId(null)
                .routeProcessId(922483L)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(BigDecimal.valueOf(48_000_000L, 6))
                .nightShiftEnabled(Boolean.TRUE)
                .calendarRuleId(1L)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(910394L);
        reqVO.setRouteProcessId(922483L);
        reqVO.setNightShiftEnabled(Boolean.TRUE);
        reqVO.setPlannedStartDate(LocalDate.of(2026, 7, 8));
        reqVO.setReason(new String(new char[]{'工','作','台','设','置','夜','班'}));

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(firstOrder, secondOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(9003941L, 9003942L)))
                .thenReturn(List.of(staleProcess, freshProcess));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(910394L, 922483L))
                .thenReturn(freshConfig);

        scheduleOrderService.saveProcessWipSettings(reqVO);

        ArgumentCaptor<MesProRouteScheduleConfigDO> routeConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).updateById(routeConfigCaptor.capture());
        assertEquals(473L, routeConfigCaptor.getValue().getId());
        assertEquals(Boolean.TRUE, routeConfigCaptor.getValue().getNightShiftEnabled());
        assertEquals(1L, routeConfigCaptor.getValue().getCalendarRuleId());

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper, times(2)).updateById(processCaptor.capture());
        List<MesProScheduleOrderProcessDO> processUpdates = processCaptor.getAllValues();
        assertEquals(8003941L, processUpdates.get(0).getId());
        assertEquals(Boolean.TRUE, processUpdates.get(0).getNightShiftEnabled());
        assertEquals(1L, processUpdates.get(0).getCalendarRuleId());
        assertEquals(LocalDateTime.of(2026, 7, 8, 0, 0), processUpdates.get(0).getPlannedStartTime());
        assertEquals(8003942L, processUpdates.get(1).getId());
        assertEquals(Boolean.TRUE, processUpdates.get(1).getNightShiftEnabled());
        assertEquals(1L, processUpdates.get(1).getCalendarRuleId());
        assertEquals(LocalDateTime.of(2026, 7, 8, 0, 0), processUpdates.get(1).getPlannedStartTime());
        verify(scheduleOrderOperationLogMapper, times(2)).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    @Test
    void saveProcessWipSettings_shouldUseCanonicalRouteConfigInsteadOfSnapshotConfigId() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(9003931L)
                .code("SCH-393-1")
                .routeId(900026L)
                .routeVersionId(4L)
                .productId(930393L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(8003931L)
                .scheduleOrderId(9003931L)
                .routeProcessId(922499L)
                .routeScheduleConfigId(463L)
                .processId(900393L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("10.600000"))
                .calendarRuleId(null)
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProRouteScheduleConfigDO staleProductConfig = MesProRouteScheduleConfigDO.builder()
                .id(463L)
                .routeVersionId(4L)
                .itemId(930393L)
                .routeProcessId(922499L)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(new BigDecimal("4799.999998"))
                .nightShiftEnabled(Boolean.FALSE)
                .calendarRuleId(1L)
                .build();
        MesProRouteScheduleConfigDO canonicalRouteConfig = MesProRouteScheduleConfigDO.builder()
                .id(367L)
                .routeVersionId(4L)
                .itemId(null)
                .routeProcessId(922499L)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(new BigDecimal("91.428571"))
                .nightShiftEnabled(Boolean.FALSE)
                .calendarRuleId(1L)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(4L);
        reqVO.setRouteProcessId(922499L);
        reqVO.setNightShiftEnabled(Boolean.TRUE);
        reqVO.setReason("排产员工作台工序在制列表维护");

        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(9003931L)))
                .thenReturn(List.of(process));
        org.mockito.Mockito.lenient().when(routeScheduleConfigMapper.selectById(463L))
                .thenReturn(staleProductConfig);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(4L, 922499L))
                .thenReturn(canonicalRouteConfig);

        scheduleOrderService.saveProcessWipSettings(reqVO);

        ArgumentCaptor<MesProRouteScheduleConfigDO> routeConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).updateById(routeConfigCaptor.capture());
        assertEquals(367L, routeConfigCaptor.getValue().getId());
        assertEquals(Boolean.TRUE, routeConfigCaptor.getValue().getNightShiftEnabled());
        assertEquals(1L, routeConfigCaptor.getValue().getCalendarRuleId());
        verify(routeScheduleConfigMapper, never()).selectById(463L);

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).updateById(processCaptor.capture());
        assertEquals(8003931L, processCaptor.getValue().getId());
        assertEquals(Boolean.TRUE, processCaptor.getValue().getNightShiftEnabled());
        assertEquals(1L, processCaptor.getValue().getCalendarRuleId());
    }

    @Test
    void saveProcessWipSettings_shouldRejectMissingWipProcess() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900801L)
                .routeId(500801L)
                .routeVersionId(600801L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600801L);
        reqVO.setRouteProcessId(710801L);
        reqVO.setNightShiftEnabled(Boolean.FALSE);
        reqVO.setPlannedStartDate(LocalDate.of(2026, 7, 16));
        reqVO.setReason("工作台设置");
        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900801L))).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(800801L)
                        .scheduleOrderId(900801L)
                        .routeVersionId(600801L)
                        .routeProcessId(710802L)
                        .processId(700802L)
                        .enabled(Boolean.TRUE)
                        .progressPercent(new BigDecimal("20"))
                        .build()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.saveProcessWipSettings(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_PROCESS_WIP_NOT_EXISTS.getCode(), exception.getCode());
        verify(scheduleOrderProcessMapper, never()).updateById(any(MesProScheduleOrderProcessDO.class));
        verify(scheduleOrderOperationLogMapper, never()).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    @Test
    void saveProcessWipSettings_shouldRejectNightShiftWithoutCalendarRule() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900901L)
                .routeId(500901L)
                .routeVersionId(600901L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600901L);
        reqVO.setRouteProcessId(710901L);
        reqVO.setNightShiftEnabled(Boolean.TRUE);
        reqVO.setPlannedStartDate(LocalDate.of(2026, 7, 17));
        reqVO.setReason("工作台设置夜班");
        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900901L))).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(800901L)
                        .scheduleOrderId(900901L)
                        .routeVersionId(600901L)
                        .routeProcessId(710901L)
                        .processId(700901L)
                        .enabled(Boolean.TRUE)
                        .progressPercent(new BigDecimal("20"))
                        .calendarRuleId(null)
                        .build()));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600901L, 710901L))
                .thenReturn(routeConfig(750901L, 600901L, 710901L, false));
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(null);

        TenantContextHolder.setTenantId(1L);
        ServiceException exception;
        try {
            exception = assertThrows(ServiceException.class,
                    () -> scheduleOrderService.saveProcessWipSettings(reqVO));
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(PRO_SCHEDULE_ORDER_PROCESS_WIP_CALENDAR_RULE_REQUIRED.getCode(), exception.getCode());
        verify(scheduleOrderProcessMapper, never()).updateById(any(MesProScheduleOrderProcessDO.class));
        verify(scheduleOrderOperationLogMapper, never()).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    @Test
    void saveProcessWipSettings_shouldRejectNightShiftBeforeWritingWhenNightResourcesAreMissing() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900902L)
                .routeId(500902L)
                .routeVersionId(600902L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(800902L)
                .scheduleOrderId(900902L)
                .routeVersionId(600902L)
                .routeProcessId(710902L)
                .processId(700902L)
                .enabled(Boolean.TRUE)
                .progressPercent(new BigDecimal("20"))
                .nightShiftEnabled(Boolean.FALSE)
                .build();
        MesProRouteScheduleConfigDO routeConfig = routeConfig(750902L, 600902L, 710902L, false);
        routeConfig.setCalendarRuleId(300902L);
        MesProScheduleOrderProcessWipSettingsReqVO reqVO = new MesProScheduleOrderProcessWipSettingsReqVO();
        reqVO.setRouteVersionId(600902L);
        reqVO.setRouteProcessId(710902L);
        reqVO.setNightShiftEnabled(Boolean.TRUE);
        reqVO.setReason("工作台设置夜班");
        when(scheduleOrderMapper.selectListForProcessWip()).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(Set.of(900902L))).thenReturn(List.of(process));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(600902L, 710902L))
                .thenReturn(routeConfig);
        org.mockito.Mockito.doThrow(new ServiceException(400,
                        "工序启用夜班失败：工作站[吹球囊成型]所在产线缺少夜班班次或夜班产能"))
                .when(routeScheduleConfigService)
                .validateNightShiftResources(710902L, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> scheduleOrderService.saveProcessWipSettings(reqVO));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("吹球囊成型"));
        assertTrue(ex.getMessage().contains("夜班班次或夜班产能"));
        verify(routeScheduleConfigMapper, never()).updateById(any(MesProRouteScheduleConfigDO.class));
        verify(scheduleOrderProcessMapper, never()).updateById(any(MesProScheduleOrderProcessDO.class));
        verify(scheduleOrderOperationLogMapper, never()).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    private MesProRouteScheduleConfigDO routeConfig(Long id, Long routeVersionId, Long routeProcessId,
                                                     boolean nightShiftEnabled) {
        return MesProRouteScheduleConfigDO.builder()
                .id(id)
                .routeVersionId(routeVersionId)
                .routeProcessId(routeProcessId)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(BigDecimal.ONE)
                .nightShiftEnabled(nightShiftEnabled)
                .build();
    }

    private MesProRouteScheduleConfigDO routeConfigWithShiftCapacity(Long id, Long routeVersionId,
                                                                     Long routeProcessId, String shiftCapacity,
                                                                     boolean nightShiftEnabled) {
        MesProRouteScheduleConfigDO config = routeConfig(id, routeVersionId, routeProcessId, nightShiftEnabled);
        config.setHourlyCapacity(new BigDecimal(shiftCapacity)
                .divide(new BigDecimal("10.5"), 12, java.math.RoundingMode.HALF_UP));
        return config;
    }

    private void stubCurrentRouteProcessDefinitions(MesProScheduleOrderProcessDO... snapshots) {
        Map<Long, MesProRouteProcessDO> routeProcesses = new LinkedHashMap<>();
        Map<Long, MesProProcessDO> processes = new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO snapshot : snapshots) {
            routeProcesses.putIfAbsent(snapshot.getRouteProcessId(), MesProRouteProcessDO.builder()
                    .id(snapshot.getRouteProcessId())
                    .processId(snapshot.getProcessId())
                    .sort(snapshot.getSort())
                    .build());
            processes.putIfAbsent(snapshot.getProcessId(), MesProProcessDO.builder()
                    .id(snapshot.getProcessId())
                    .code(snapshot.getProcessCode())
                    .name(snapshot.getProcessName())
                    .build());
        }
        when(routeProcessMapper.selectBatchIds(routeProcesses.keySet()))
                .thenReturn(new ArrayList<>(routeProcesses.values()));
        when(processMapper.selectBatchIds(processes.keySet()))
                .thenReturn(new ArrayList<>(processes.values()));
    }

    @Test
    void getAdmissionDiff_shouldFilterComputedAdmissionStatusBeforePagination() {
        MesProWorkOrderDO alreadyAdmitted = workOrder(100L, "MO-100", 20L);
        MesProWorkOrderDO missingRoute = workOrder(101L, "MO-101", 21L);
        MesProWorkOrderDO readyToAdmit = workOrder(102L, "MO-102", 22L);
        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(2);
        reqVO.setAdmissionStatus("READY_TO_ADMIT");

        when(workOrderMapper.selectPage(any(MesProWorkOrderPageReqVO.class))).thenAnswer(invocation -> {
            MesProWorkOrderPageReqVO workOrderPageReqVO = invocation.getArgument(0);
            if (workOrderPageReqVO.getPageSize() > reqVO.getPageSize()) {
                return new PageResult<>(List.of(alreadyAdmitted, missingRoute, readyToAdmit), 3L);
            }
            return new PageResult<>(List.of(alreadyAdmitted, missingRoute), 3L);
        });
        when(scheduleOrderMapper.selectListByWorkOrderIds(any()))
                .thenReturn(List.of(MesProScheduleOrderDO.builder().id(900L).workOrderId(100L).build()));
        when(routeProductMapper.selectListByItemId(21L)).thenReturn(List.of());
        when(routeProductMapper.selectListByItemId(22L))
                .thenReturn(List.of(MesProRouteProductDO.builder().routeId(30L).itemId(22L).build()));
        when(routeMapper.selectById(30L))
                .thenReturn(MesProRouteDO.builder().id(30L).status(CommonStatusEnum.ENABLE.getStatus()).build());
        when(routeVersionMapper.selectActiveByRouteId(30L))
                .thenReturn(MesProRouteVersionDO.builder().id(700L).routeId(30L).versionNo("V1").active(true).build());
        when(routeProcessMapper.selectListByRouteId(30L))
                .thenReturn(List.of(MesProRouteProcessDO.builder().id(300L).routeId(30L).processId(40L).sort(1).build()));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(30L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(30L)
                        .routeId(30L)
                        .routeProcessId(300L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(800L)
                        .routeVersionId(700L)
                        .itemId(22L)
                        .routeProcessId(300L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("10.000000"))
                        .build()));
        org.mockito.Mockito.lenient().when(workstationMapper.selectListByProcessIds(List.of(40L))).thenReturn(List.of());

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(102L, result.getList().get(0).getWorkOrderId());
        assertEquals("READY_TO_ADMIT", result.getList().get(0).getAdmissionStatus());
        assertEquals(1, result.getSummary().getReadyCount());
        assertEquals(0, result.getSummary().getAlreadyAdmittedCount());
        assertEquals(0, result.getSummary().getBlockedCount());
    }

    @Test
    void getAdmissionDiff_shouldFilterBlockedAdmissionStatusBeforePagination() {
        MesProWorkOrderDO alreadyAdmittedOne = workOrder(110L, "MO-110", 30L);
        MesProWorkOrderDO alreadyAdmittedTwo = workOrder(111L, "MO-111", 31L);
        MesProWorkOrderDO frozenBlocked = workOrder(112L, "MO-112", 32L);
        frozenBlocked.setTemporaryFrozen(Boolean.TRUE);
        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(2);
        reqVO.setAdmissionStatus("BLOCKED");

        when(workOrderMapper.selectPage(any(MesProWorkOrderPageReqVO.class))).thenAnswer(invocation -> {
            MesProWorkOrderPageReqVO workOrderPageReqVO = invocation.getArgument(0);
            if (workOrderPageReqVO.getPageSize() > reqVO.getPageSize()) {
                return new PageResult<>(List.of(alreadyAdmittedOne, alreadyAdmittedTwo, frozenBlocked), 3L);
            }
            return new PageResult<>(List.of(alreadyAdmittedOne, alreadyAdmittedTwo), 3L);
        });
        when(scheduleOrderMapper.selectListByWorkOrderIds(any()))
                .thenReturn(List.of(
                        MesProScheduleOrderDO.builder().id(910L).workOrderId(110L).build(),
                        MesProScheduleOrderDO.builder().id(911L).workOrderId(111L).build()
                ));

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(112L, result.getList().get(0).getWorkOrderId());
        assertEquals("BLOCKED", result.getList().get(0).getAdmissionStatus());
        assertEquals("BLOCKED_WORK_ORDER_FROZEN", result.getList().get(0).getReasonCode());
        assertEquals(0, result.getSummary().getReadyCount());
        assertEquals(0, result.getSummary().getAlreadyAdmittedCount());
        assertEquals(1, result.getSummary().getBlockedCount());
    }

    @Test
    void preflight_shouldExposeProductIdentityForMissingRouteBlocker() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-MO-100")
                .workOrderId(100L)
                .erpWorkOrderCode("MO-100")
                .productId(20L)
                .autoSchedulable(Boolean.FALSE)
                .build();
        MesProScheduleOrderPreflightReqVO reqVO = new MesProScheduleOrderPreflightReqVO();
        reqVO.setScopeType("SELECTED");
        reqVO.setScheduleOrderIds(List.of(900L));

        when(scheduleOrderMapper.selectListByIds(List.of(900L))).thenReturn(List.of(scheduleOrder));
        when(itemMapper.selectListByIds(List.of(20L)))
                .thenReturn(List.of(MesMdItemDO.builder().id(20L).code("ITEM-BD-001").name("球囊导管").build()));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(900L))).thenReturn(List.of());
        when(syncRecordMapper.selectByWorkOrderId(100L))
                .thenReturn(MesKingdeeProductionOrderSyncRecordDO.builder()
                        .workOrderId(100L)
                        .sourceFid("FID-100")
                        .sourceBillNo("MO-100")
                        .build());

        MesProScheduleOrderPreflightRespVO result = scheduleOrderService.preflight(reqVO);

        assertEquals("BLOCKED", result.getResult());
        assertEquals(1, result.getIssues().size());
        MesProScheduleOrderPreflightIssueRespVO issue = result.getIssues().get(0);
        assertEquals("BLOCKED_MISSING_ROUTE", issue.getReasonCode());
        assertEquals("球囊导管", issue.getProductName());
        assertEquals("ITEM-BD-001", issue.getProductCode());
        assertTrue(issue.getMessage().contains("球囊导管"));
        assertTrue(issue.getMessage().contains("ITEM-BD-001"));
        assertTrue(issue.getMessage().contains("缺少可用工艺路线"));
    }

    @Test
    void updatePriority_shouldPersistPriorityOnExistingScheduleOrder() {
        when(scheduleOrderMapper.selectById(900L))
                .thenReturn(MesProScheduleOrderDO.builder().id(900L).priorityNo(100).build());

        scheduleOrderService.updatePriority(900L, 5);

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).updateById(orderCaptor.capture());
        MesProScheduleOrderDO updated = orderCaptor.getValue();
        assertEquals(900L, updated.getId());
        assertEquals(5, updated.getPriorityNo());
        verify(scheduleOrderOperationLogMapper).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    @Test
    void updatePriority_shouldRejectPriorityLessThanOneBeforeUpdating() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.updatePriority(900L, 0));

        assertEquals(PRO_SCHEDULE_ORDER_PRIORITY_INVALID.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).selectById(900L);
        verify(scheduleOrderMapper, never()).updateById(any(MesProScheduleOrderDO.class));
    }

    @Test
    void updateScheduleOrder_shouldPersistAllowedFieldsAndWriteOperationLog() {
        MesProScheduleOrderDO existing = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .promiseDate(LocalDate.of(2026, 6, 30))
                .plannedStartTime(LocalDateTime.of(2026, 7, 1, 8, 0))
                .priorityNo(10)
                .remark("before")
                .frozen(Boolean.FALSE)
                .build();
        MesProScheduleOrderUpdateReqVO reqVO = new MesProScheduleOrderUpdateReqVO();
        reqVO.setId(900L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 5));
        reqVO.setPlannedStartTime(LocalDateTime.of(2026, 7, 2, 9, 30));
        reqVO.setPriorityNo(3);
        reqVO.setRemark("after");
        reqVO.setReason("计划调整");
        when(scheduleOrderMapper.selectById(900L)).thenReturn(existing);

        scheduleOrderService.updateScheduleOrder(reqVO);

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).updateById(orderCaptor.capture());
        MesProScheduleOrderDO update = orderCaptor.getValue();
        assertEquals(900L, update.getId());
        assertEquals(LocalDate.of(2026, 7, 5), update.getPromiseDate());
        assertEquals(LocalDateTime.of(2026, 7, 2, 9, 30), update.getPlannedStartTime());
        assertEquals(3, update.getPriorityNo());
        assertEquals("after", update.getRemark());

        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper).insert(logCaptor.capture());
        MesProScheduleOrderOperationLogDO log = logCaptor.getValue();
        assertEquals(900L, log.getScheduleOrderId());
        assertEquals("SCH-900", log.getScheduleOrderCode());
        assertEquals("UPDATE", log.getOperationType());
        assertEquals("计划调整", log.getReason());
        assertTrue(log.getBeforeSnapshotJson().contains("before"));
        assertTrue(log.getAfterSnapshotJson().contains("after"));
        assertTrue(log.getAfterSnapshotJson().contains("2026-07-02T09:30"));
    }

    @Test
    void updateScheduleOrder_shouldRejectFrozenScheduleOrder() {
        MesProScheduleOrderDO existing = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .frozen(Boolean.TRUE)
                .build();
        MesProScheduleOrderUpdateReqVO reqVO = new MesProScheduleOrderUpdateReqVO();
        reqVO.setId(900L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 5));
        reqVO.setPriorityNo(3);
        reqVO.setReason("计划调整");
        when(scheduleOrderMapper.selectById(900L)).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.updateScheduleOrder(reqVO));

        assertTrue(exception.getMessage().contains("SCH-900"));
        verify(scheduleOrderMapper, never()).updateById(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderOperationLogMapper, never()).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    @Test
    void freezeScheduleOrders_shouldFreezeAllRowsAndWriteTraceLogs() {
        MesProScheduleOrderDO first = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .frozen(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO second = MesProScheduleOrderDO.builder()
                .id(901L)
                .code("SCH-901")
                .frozen(Boolean.FALSE)
                .build();
        MesProScheduleOrderBatchReqVO reqVO = new MesProScheduleOrderBatchReqVO();
        reqVO.setIds(List.of(900L, 901L));
        reqVO.setReason("插单锁定");
        when(scheduleOrderMapper.selectListByIds(List.of(900L, 901L))).thenReturn(List.of(first, second));

        scheduleOrderService.freezeScheduleOrders(reqVO);

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper, times(2)).updateById(orderCaptor.capture());
        assertTrue(orderCaptor.getAllValues().stream().allMatch(MesProScheduleOrderDO::getFrozen));

        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper, times(2)).insert(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream().allMatch(log -> "FREEZE".equals(log.getOperationType())));
        assertTrue(logCaptor.getAllValues().stream().allMatch(log -> "插单锁定".equals(log.getReason())));
    }

    @Test
    void unfreezeScheduleOrders_shouldClearFrozenFieldsAndWriteTraceLogs() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .frozen(Boolean.TRUE)
                .frozenTime(LocalDateTime.of(2026, 6, 24, 8, 30))
                .frozenBy(100L)
                .freezeReason("插单锁定")
                .build();
        MesProScheduleOrderBatchReqVO reqVO = new MesProScheduleOrderBatchReqVO();
        reqVO.setIds(List.of(900L));
        reqVO.setReason("插单已释放");
        when(scheduleOrderMapper.selectListByIds(List.of(900L))).thenReturn(List.of(scheduleOrder));

        scheduleOrderService.unfreezeScheduleOrders(reqVO);

        verify(scheduleOrderMapper).clearFrozen(900L);

        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper).insert(logCaptor.capture());
        assertEquals("UNFREEZE", logCaptor.getValue().getOperationType());
        assertEquals("插单已释放", logCaptor.getValue().getReason());
        assertTrue(logCaptor.getValue().getBeforeSnapshotJson().contains("\"frozen\":true"));
        assertTrue(logCaptor.getValue().getAfterSnapshotJson().contains("\"frozen\":false"));
    }

    @Test
    void manualFinish_shouldForceFinishedSummaryAndWriteTraceLog() {
        MesProScheduleOrderDO existing = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .frozen(Boolean.FALSE)
                .totalQuantity(new BigDecimal("120.000000"))
                .quantity(new BigDecimal("120.000000"))
                .completedQuantity(new BigDecimal("60.000000"))
                .uncompletedQuantity(new BigDecimal("60.000000"))
                .progressPercent(new BigDecimal("50.000000"))
                .build();
        MesProScheduleOrderActionReqVO reqVO = new MesProScheduleOrderActionReqVO();
        reqVO.setId(900L);
        reqVO.setReason("排产员确认完成");
        when(scheduleOrderMapper.selectById(900L)).thenReturn(existing);

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("scheduler");

            scheduleOrderService.manualFinish(reqVO);
        }

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).updateById(orderCaptor.capture());
        MesProScheduleOrderDO update = orderCaptor.getValue();
        assertEquals(900L, update.getId());
        assertEquals(Boolean.TRUE, update.getManualFinished());
        assertEquals(101L, update.getManualFinishedBy());
        assertEquals("排产员确认完成", update.getManualFinishedReason());
        assertEquals(MesProScheduleOrderStatusEnum.FINISHED.getStatus(), update.getStatus());
        assertEquals(new BigDecimal("120.000000"), update.getCompletedQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), update.getUncompletedQuantity());
        assertEquals(new BigDecimal("100.000000"), update.getProgressPercent());

        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper).insert(logCaptor.capture());
        assertEquals("MANUAL_FINISH", logCaptor.getValue().getOperationType());
        assertEquals("排产员确认完成", logCaptor.getValue().getReason());
        assertTrue(logCaptor.getValue().getAfterSnapshotJson().contains("\"manualFinished\":true"));
    }

    @Test
    void manualFinish_shouldLockAggregateTotalByEnabledProcessCount() {
        MesProScheduleOrderDO existing = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .frozen(Boolean.FALSE)
                .totalQuantity(new BigDecimal("1200.000000"))
                .quantity(new BigDecimal("1000.000000"))
                .completedQuantity(new BigDecimal("490.000000"))
                .uncompletedQuantity(new BigDecimal("710.000000"))
                .progressPercent(new BigDecimal("40.833333"))
                .build();
        when(scheduleOrderMapper.selectById(900L)).thenReturn(existing);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(900L)).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder().id(1001L).scheduleOrderId(900L)
                        .enabled(Boolean.TRUE).build(),
                MesProScheduleOrderProcessDO.builder().id(1002L).scheduleOrderId(900L)
                        .enabled(Boolean.TRUE).build(),
                MesProScheduleOrderProcessDO.builder().id(1003L).scheduleOrderId(900L)
                        .enabled(Boolean.TRUE).build(),
                MesProScheduleOrderProcessDO.builder().id(1004L).scheduleOrderId(900L)
                        .enabled(Boolean.FALSE).build()
        ));
        MesProScheduleOrderActionReqVO reqVO = new MesProScheduleOrderActionReqVO();
        reqVO.setId(900L);
        reqVO.setReason("排产员确认完成");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("scheduler");

            scheduleOrderService.manualFinish(reqVO);
        }

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).updateById(orderCaptor.capture());
        MesProScheduleOrderDO update = orderCaptor.getValue();
        assertEquals(new BigDecimal("3000.000000"), update.getTotalQuantity());
        assertEquals(new BigDecimal("3000.000000"), update.getCompletedQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), update.getUncompletedQuantity());
        assertEquals(new BigDecimal("100.000000"), update.getProgressPercent());
    }

    @Test
    void revokeManualFinish_shouldRecalculateFromRealProgressAndWriteTraceLog() {
        MesProScheduleOrderDO existing = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .status(MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .frozen(Boolean.FALSE)
                .manualFinished(Boolean.TRUE)
                .manualFinishedBy(101L)
                .manualFinishedReason("排产员确认完成")
                .quantity(new BigDecimal("100.000000"))
                .plannedStartTime(LocalDateTime.of(2026, 6, 29, 8, 0))
                .plannedEndTime(LocalDateTime.of(2026, 6, 29, 18, 0))
                .build();
        MesProScheduleOrderActionReqVO reqVO = new MesProScheduleOrderActionReqVO();
        reqVO.setId(900L);
        reqVO.setReason("管理员撤销误操作");
        when(scheduleOrderMapper.selectById(900L)).thenReturn(existing);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(900L)).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(1001L)
                        .scheduleOrderId(900L)
                        .plannedQuantity(new BigDecimal("100.000000"))
                        .reportedQuantity(new BigDecimal("40.000000"))
                        .remainingQuantity(new BigDecimal("60.000000"))
                        .progressPercent(new BigDecimal("40.000000"))
                        .enabled(Boolean.TRUE)
                        .build()
        ));
        when(feedbackMapper.selectProgressListByScheduleOrderId(900L)).thenReturn(List.of(
                feedback(9001L, 900L, 1001L, 11L, "40.000000", LocalDateTime.of(2026, 6, 29, 9, 0))
        ));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("admin");

            scheduleOrderService.revokeManualFinish(reqVO);
        }

        verify(scheduleOrderMapper).clearManualFinishAndUpdateProgress(900L,
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus(),
                new BigDecimal("100.000000"),
                new BigDecimal("40.000000"),
                new BigDecimal("60.000000"),
                new BigDecimal("40.000000"));

        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper, times(2)).insert(logCaptor.capture());
        assertEquals("REVOKE_MANUAL_FINISH", logCaptor.getAllValues().get(0).getOperationType());
    }

    @Test
    void revokeManualFinish_shouldReturnToScheduledWhenNoRealProgressButAlreadyPlanned() {
        MesProScheduleOrderDO existing = MesProScheduleOrderDO.builder()
                .id(901L)
                .code("SCH-901")
                .status(MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .frozen(Boolean.FALSE)
                .manualFinished(Boolean.TRUE)
                .quantity(new BigDecimal("100.000000"))
                .plannedStartTime(LocalDateTime.of(2026, 6, 29, 8, 0))
                .plannedEndTime(LocalDateTime.of(2026, 6, 29, 18, 0))
                .build();
        MesProScheduleOrderActionReqVO reqVO = new MesProScheduleOrderActionReqVO();
        reqVO.setId(901L);
        reqVO.setReason("管理员撤销误操作");
        when(scheduleOrderMapper.selectById(901L)).thenReturn(existing);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(901L)).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(1002L)
                        .scheduleOrderId(901L)
                        .plannedQuantity(new BigDecimal("100.000000"))
                        .reportedQuantity(BigDecimal.ZERO.setScale(6))
                        .remainingQuantity(new BigDecimal("100.000000"))
                        .progressPercent(BigDecimal.ZERO.setScale(6))
                        .enabled(Boolean.TRUE)
                        .plannedStartTime(LocalDateTime.of(2026, 6, 29, 8, 0))
                        .plannedEndTime(LocalDateTime.of(2026, 6, 29, 18, 0))
                        .build()
        ));
        when(feedbackMapper.selectProgressListByScheduleOrderId(901L)).thenReturn(List.of());

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("admin");

            scheduleOrderService.revokeManualFinish(reqVO);
        }

        verify(scheduleOrderMapper).clearManualFinishAndUpdateProgress(901L,
                MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(),
                new BigDecimal("100.000000"),
                BigDecimal.ZERO.setScale(6),
                new BigDecimal("100.000000"),
                BigDecimal.ZERO.setScale(6));
    }

    @Test
    void deleteScheduleOrders_shouldRejectRowsWithReportedProgress() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(900L)
                .code("SCH-900")
                .status(MesProScheduleOrderStatusEnum.PREPARE.getStatus())
                .frozen(Boolean.FALSE)
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(1000L)
                .scheduleOrderId(900L)
                .reportedQuantity(new BigDecimal("1.000000"))
                .build();
        MesProScheduleOrderBatchReqVO reqVO = new MesProScheduleOrderBatchReqVO();
        reqVO.setIds(List.of(900L));
        reqVO.setReason("清理错误入池");
        when(scheduleOrderMapper.selectListByIds(List.of(900L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(900L))).thenReturn(List.of(process));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.deleteScheduleOrders(reqVO));

        assertTrue(exception.getMessage().contains("SCH-900"));
        verify(scheduleOrderMapper, never()).deleteById(900L);
        verify(scheduleOrderOperationLogMapper, never()).insert(any(MesProScheduleOrderOperationLogDO.class));
    }

    @Test
    void preflight_shouldExposeProductIdentityWhenScheduleOrderMissingRoute() {
        Long scheduleOrderId = 901L;
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(scheduleOrderId)
                .code("SCH-MO-001")
                .workOrderId(100L)
                .erpWorkOrderCode("MO-001")
                .productId(20L)
                .autoSchedulable(Boolean.TRUE)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.MISSING.getStatus())
                .build();
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L)
                .code("MO-001")
                .productId(20L)
                .build();
        MesMdItemDO product = MesMdItemDO.builder()
                .id(20L)
                .code("ITEM-001")
                .name("产品A")
                .build();
        MesProScheduleOrderPreflightReqVO reqVO = new MesProScheduleOrderPreflightReqVO();
        reqVO.setScheduleOrderIds(List.of(scheduleOrderId));

        when(scheduleOrderMapper.selectListByIds(List.of(scheduleOrderId))).thenReturn(List.of(scheduleOrder));
        when(itemMapper.selectListByIds(List.of(20L))).thenReturn(List.of(product));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(scheduleOrderId))).thenReturn(List.of());
        when(syncRecordMapper.selectByWorkOrderId(100L)).thenReturn(MesKingdeeProductionOrderSyncRecordDO.builder()
                .id(500L)
                .workOrderId(100L)
                .sourceFid("FID-100")
                .sourceBillNo("MO-001")
                .build());

        MesProScheduleOrderPreflightRespVO result = scheduleOrderService.preflight(reqVO);

        assertEquals("BLOCKED", result.getResult());
        assertEquals(1, result.getIssues().size());
        MesProScheduleOrderPreflightIssueRespVO issue = result.getIssues().get(0);
        assertEquals("BLOCKED_MISSING_ROUTE", issue.getReasonCode());
        assertEquals(20L, issue.getProductId());
        assertEquals("ITEM-001", issue.getProductCode());
        assertEquals("产品A", issue.getProductName());
        assertEquals("SCH-MO-001", issue.getScheduleOrderCode());
        assertTrue(issue.getMessage().contains("产品A"));
        assertTrue(issue.getMessage().contains("ITEM-001"));
        assertTrue(issue.getMessage().contains("缺少可用工艺路线"));
    }

    @Test
    void createFromWorkOrder_shouldCreateUniqueScheduleOrderAndProcessSnapshots() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L)
                .code("ERP-MO-001")
                .name("ERP order 001")
                .productId(20L)
                .quantity(new BigDecimal("120.000000"))
                .requestDate(LocalDateTime.of(2026, 6, 20, 0, 0))
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().id(200L).routeId(30L).itemId(20L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(30L).code("ROUTE-A").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO firstProcess = MesProRouteProcessDO.builder().id(300L).routeId(30L).processId(40L).sort(1)
                .workstationId(500L).keyFlag(Boolean.TRUE).build();
        MesProRouteProcessDO secondProcess = MesProRouteProcessDO.builder().id(301L).routeId(30L).processId(41L).sort(2)
                .workstationId(501L).keyFlag(Boolean.FALSE).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(100L);
        reqVO.setPromiseDate(LocalDate.of(2026, 6, 30));
        reqVO.setPriorityNo(10);
        reqVO.setRemark("first schedule order");

        when(workOrderMapper.selectById(100L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L))).thenReturn(List.of());
        when(scheduleOrderMapper.selectMaxCodeByPrefix(anyString())).thenReturn(null);
        when(routeProductMapper.selectByItemId(20L)).thenReturn(routeProduct);
        when(routeMapper.selectById(30L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(30L)).thenReturn(List.of(firstProcess, secondProcess));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(30L)).thenReturn(List.of(edge(30L, 300L, 301L)));
        when(routeVersionMapper.selectActiveByRouteId(30L)).thenReturn(MesProRouteVersionDO.builder()
                .id(701L)
                .routeId(30L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .build());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(30L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(
                        MesProRouteFlowProcessConfigDO.builder().routeFlowConfigId(30L).routeId(30L).routeProcessId(300L)
                                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE)
                                .productionQuantityFactor(BigDecimal.ONE).build(),
                        MesProRouteFlowProcessConfigDO.builder().routeFlowConfigId(30L).routeId(30L).routeProcessId(301L)
                                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE)
                                .productionQuantityFactor(BigDecimal.ONE).build()
                ));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(701L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(810L)
                        .routeVersionId(701L)
                        .itemId(20L)
                        .routeProcessId(300L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("16.000000"))
                        .build(),
                MesProRouteScheduleConfigDO.builder()
                        .id(811L)
                        .routeVersionId(701L)
                        .itemId(20L)
                        .routeProcessId(301L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("15.000000"))
                        .build()
        ));
        when(processMapper.selectBatchIds(List.of(40L, 41L))).thenReturn(List.of(
                MesProProcessDO.builder().id(40L).code("B010").name("吹球囊成型").build(),
                MesProProcessDO.builder().id(41L).code("B020").name("球囊全检").build()
        ));
        MesMdWorkstationDO machineWorkstation = MesMdWorkstationDO.builder()
                .id(500L).code("WS-M").name("设备工位").processId(40L).shiftHours(new BigDecimal("10.5")).build();
        MesMdWorkstationDO workerWorkstation = MesMdWorkstationDO.builder()
                .id(501L).code("WS-W").name("人工工位").processId(41L)
                .singleStandardHourlyCapacity(new BigDecimal("3.000000"))
                .shiftHours(new BigDecimal("10.5"))
                .build();
        when(workstationMapper.selectBatchIds(Set.of(500L, 501L)))
                .thenReturn(List.of(machineWorkstation, workerWorkstation));
        when(workstationMachineMapper.selectListByWorkstationIds(List.of(500L, 501L)))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder()
                        .id(600L).workstationId(500L).machineryId(700L).quantity(2).build()));
        when(workstationWorkerMapper.selectListByWorkstationIds(List.of(500L, 501L)))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(601L).workstationId(501L).quantity(5).build()));
        when(machineryMapper.selectBatchIds(any()))
                .thenReturn(List.of(MesDvMachineryDO.builder().id(700L).code("A700").name("成型机").build()));
        when(machineryProcessMapper.selectListByMachineryIds(any()))
                .thenReturn(List.of(MesDvMachineryProcessDO.builder()
                        .machineryId(700L).processId(40L).standardHourlyCapacity(new BigDecimal("8.000000")).build()));
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(900L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        Long id = scheduleOrderService.createFromWorkOrder(reqVO);

        assertEquals(900L, id);
        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).insert(orderCaptor.capture());
        MesProScheduleOrderDO inserted = orderCaptor.getValue();
        assertTrue(inserted.getCode().startsWith("SCH-ERP-MO-001-"));
        assertEquals(100L, inserted.getWorkOrderId());
        assertEquals("ERP-MO-001", inserted.getErpWorkOrderCode());
        assertEquals(new BigDecimal("120.000000"), inserted.getQuantity());
        assertEquals(LocalDate.of(2026, 6, 30), inserted.getPromiseDate());
        assertEquals(10, inserted.getPriorityNo());
        assertEquals(MesProScheduleOrderStatusEnum.PREPARE.getStatus(), inserted.getStatus());
        assertEquals(MesProScheduleOrderRiskStatusEnum.NONE.getStatus(), inserted.getRiskStatus());
        assertEquals(30L, inserted.getRouteId());
        assertEquals("V1", inserted.getRouteVersion());
        assertEquals(inserted.getRouteVersion(), inserted.getScheduleConfigVersion());
        assertNotNull(inserted.getSourceSnapshotJson());
        assertTrue(inserted.getRouteSnapshotJson().contains(inserted.getRouteVersion()));
        assertTrue(inserted.getCapacitySnapshotJson().contains("shiftCapacityTotal"));

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper, times(2)).insert(processCaptor.capture());
        List<MesProScheduleOrderProcessDO> processSnapshots = processCaptor.getAllValues();
        assertEquals(2, processSnapshots.size());
        assertEquals(900L, processSnapshots.get(0).getScheduleOrderId());
        assertEquals(300L, processSnapshots.get(0).getRouteProcessId());
        assertEquals("B010", processSnapshots.get(0).getProcessCode());
        assertEquals("吹球囊成型", processSnapshots.get(0).getProcessName());
        assertEquals("MACHINE", processSnapshots.get(0).getCapacitySource());
        assertEquals(new BigDecimal("16.000000"), processSnapshots.get(0).getHourlyCapacityTotal());
        assertEquals(new BigDecimal("10.5"), processSnapshots.get(0).getShiftHours());
        assertEquals(new BigDecimal("168.0000000"), processSnapshots.get(0).getShiftCapacityTotal());
        assertTrue(processSnapshots.get(0).getResourceSnapshotJson().contains("A700"));
        assertEquals(new BigDecimal("120.000000"), processSnapshots.get(0).getPlannedQuantity());
        assertEquals(new BigDecimal("1.000000"), processSnapshots.get(0).getProductionQuantityFactor());
        assertEquals(BigDecimal.ZERO, processSnapshots.get(0).getReportedQuantity());
        assertEquals(Boolean.TRUE, processSnapshots.get(0).getKeyProcessFlag());
        assertEquals("WORKER", processSnapshots.get(1).getCapacitySource());
        assertEquals("B020", processSnapshots.get(1).getProcessCode());
        assertEquals("球囊全检", processSnapshots.get(1).getProcessName());
        assertEquals(new BigDecimal("15.000000"), processSnapshots.get(1).getHourlyCapacityTotal());
        assertEquals(new BigDecimal("157.5000000"), processSnapshots.get(1).getShiftCapacityTotal());
        assertEquals(Boolean.FALSE, processSnapshots.get(1).getKeyProcessFlag());
    }

    @Test
    void createFromWorkOrder_shouldAcceptValidMultipleStartAndMergeFlowGraph() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(120L)
                .code("ERP-MO-MULTI-START")
                .name("多起点排产工单")
                .productId(220L)
                .quantity(new BigDecimal("40.000000"))
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(320L).itemId(220L).build();
        MesProRouteDO route = MesProRouteDO.builder()
                .id(320L).code("ROUTE-MULTI-START").status(CommonStatusEnum.ENABLE.getStatus()).build();
        List<MesProRouteProcessDO> routeProcesses = List.of(
                MesProRouteProcessDO.builder().id(330L).routeId(320L).processId(430L).sort(1).keyFlag(Boolean.TRUE).build(),
                MesProRouteProcessDO.builder().id(331L).routeId(320L).processId(431L).sort(2).keyFlag(Boolean.FALSE).build(),
                MesProRouteProcessDO.builder().id(332L).routeId(320L).processId(432L).sort(3).keyFlag(Boolean.FALSE).build(),
                MesProRouteProcessDO.builder().id(333L).routeId(320L).processId(433L).sort(4).keyFlag(Boolean.FALSE).build());
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(732L).routeId(320L).versionNo("V1").active(Boolean.TRUE).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(120L);
        reqVO.setPromiseDate(LocalDate.of(2026, 8, 30));

        when(workOrderMapper.selectById(120L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(120L))).thenReturn(List.of());
        when(scheduleOrderMapper.selectMaxCodeByPrefix(anyString())).thenReturn(null);
        when(routeProductMapper.selectByItemId(220L)).thenReturn(routeProduct);
        when(routeMapper.selectById(320L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(320L)).thenReturn(routeProcesses);
        when(routeProcessFlowEdgeMapper.selectListByRouteId(320L)).thenReturn(List.of(
                edge(320L, 330L, 332L), edge(320L, 331L, 332L), edge(320L, 331L, 333L)));
        when(routeVersionMapper.selectActiveByRouteId(320L)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                320L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(routeProcesses.stream()
                .map(routeProcess -> MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(320L)
                        .routeId(320L)
                        .routeProcessId(routeProcess.getId())
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE)
                        .build())
                .toList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(732L)).thenReturn(routeProcesses.stream()
                .map(routeProcess -> MesProRouteScheduleConfigDO.builder()
                        .routeVersionId(732L)
                        .routeProcessId(routeProcess.getId())
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("10.000000"))
                        .build())
                .toList());
        when(processMapper.selectBatchIds(List.of(430L, 431L, 432L, 433L))).thenReturn(List.of(
                MesProProcessDO.builder().id(430L).code("P430").name("并行工序一").build(),
                MesProProcessDO.builder().id(431L).code("P431").name("并行工序二").build(),
                MesProProcessDO.builder().id(432L).code("P432").name("后续工序一").build(),
                MesProProcessDO.builder().id(433L).code("P433").name("后续工序二").build()));
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(920L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        Long scheduleOrderId = scheduleOrderService.createFromWorkOrder(reqVO);

        assertEquals(920L, scheduleOrderId);
        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper, times(4)).insert(processCaptor.capture());
        MesProScheduleOrderProcessDO mergeSnapshot = processCaptor.getAllValues().stream()
                .filter(process -> Objects.equals(process.getRouteProcessId(), 332L))
                .findFirst().orElseThrow();
        assertEquals("[330,331]", mergeSnapshot.getPredecessorRouteProcessIdsJson());
        assertNull(mergeSnapshot.getPredecessorRouteProcessId());
    }

    @Test
    void buildRouteProcessPredecessorMap_shouldRejectCycleAfterAllowingMultiplePredecessors() {
        List<MesProRouteProcessDO> routeProcesses = List.of(
                MesProRouteProcessDO.builder().id(340L).routeId(321L).processId(440L).build(),
                MesProRouteProcessDO.builder().id(341L).routeId(321L).processId(441L).build(),
                MesProRouteProcessDO.builder().id(342L).routeId(321L).processId(442L).build());
        when(routeProcessFlowEdgeMapper.selectListByRouteId(321L)).thenReturn(List.of(
                edge(321L, 340L, 341L), edge(321L, 341L, 342L), edge(321L, 342L, 341L)));
        MesProScheduleOrderServiceImpl service = new MesProScheduleOrderServiceImpl();
        ReflectionTestUtils.setField(service, "routeProcessFlowEdgeMapper", routeProcessFlowEdgeMapper);

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "buildRouteProcessPredecessorMap", 321L, routeProcesses));
    }

    @Test
    void createFromWorkOrder_shouldMultiplyProcessPlannedQuantityByProductionFactor() {
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(100L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        stubSimpleSchedulableWorkOrder(100L, "MO-100", 20L, 30L, 300L, 40L, 700L, 800L, "B010", "首道工序");
        MesProWorkOrderDO workOrder = workOrder(100L, "MO-100", 20L);
        workOrder.setQuantity(new BigDecimal("100.000000"));
        when(workOrderMapper.selectById(100L)).thenReturn(workOrder);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(30L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(30L)
                        .routeId(30L)
                        .routeProcessId(300L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .productionQuantityFactor(new BigDecimal("3.000000"))
                        .build()));
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(900L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        Long id = scheduleOrderService.createFromWorkOrder(reqVO);

        assertEquals(900L, id);
        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).insert(processCaptor.capture());
        MesProScheduleOrderProcessDO snapshot = processCaptor.getValue();
        assertEquals(new BigDecimal("3.000000"), snapshot.getProductionQuantityFactor());
        assertEquals(new BigDecimal("300.000000"), snapshot.getPlannedQuantity());
        assertEquals(new BigDecimal("300.000000"), snapshot.getRemainingQuantity());
        assertTrue(snapshot.getResourceSnapshotJson().contains("productionQuantityFactor"));
    }

    @Test
    void createFromWorkOrder_shouldRejectMissingProductionQuantityFactor() {
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(100L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        stubSimpleSchedulableWorkOrder(100L, "MO-100", 20L, 30L, 300L, 40L, 700L, 800L, "B010", "首道工序");
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(30L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(30L)
                        .routeId(30L)
                        .routeProcessId(300L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(900L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID.getCode(), exception.getCode());
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldDeduplicateAndCreateEverySelectedWorkOrderWithSamePromiseDate() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L, 101L, 100L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        stubSimpleSchedulableWorkOrder(100L, "MO-100", 20L, 30L, 300L, 40L, 700L, 800L, "B010", "首道工序");
        stubSimpleSchedulableWorkOrder(101L, "MO-101", 21L, 31L, 301L, 41L, 701L, 801L, "B011", "二道工序");
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L, 101L))).thenReturn(List.of());
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(scheduleOrder.getWorkOrderId() + 800L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        List<Long> ids = scheduleOrderService.createFromWorkOrders(reqVO);

        assertEquals(List.of(900L, 901L), ids);
        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper, times(2)).insert(orderCaptor.capture());
        assertEquals(List.of(100L, 101L), orderCaptor.getAllValues().stream()
                .map(MesProScheduleOrderDO::getWorkOrderId)
                .toList());
        assertTrue(orderCaptor.getAllValues().stream()
                .allMatch(order -> LocalDate.of(2026, 7, 10).equals(order.getPromiseDate())));
        verify(scheduleOrderProcessMapper, times(2)).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldCreateScheduleOrdersWithoutPromiseDate() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L, 101L, 100L));
        stubSimpleSchedulableWorkOrder(100L, "MO-100", 20L, 30L, 300L, 40L, 700L, 800L, "B010", "首道工序");
        stubSimpleSchedulableWorkOrder(101L, "MO-101", 21L, 31L, 301L, 41L, 701L, 801L, "B011", "二道工序");
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L, 101L))).thenReturn(List.of());
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(scheduleOrder.getWorkOrderId() + 800L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        List<Long> ids = scheduleOrderService.createFromWorkOrders(reqVO);

        assertEquals(List.of(900L, 901L), ids);
        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper, times(2)).insert(orderCaptor.capture());
        assertEquals(List.of(100L, 101L), orderCaptor.getAllValues().stream()
                .map(MesProScheduleOrderDO::getWorkOrderId)
                .toList());
        orderCaptor.getAllValues().forEach(order -> assertNull(order.getPromiseDate()));
        verify(scheduleOrderProcessMapper, times(2)).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldFailFastWhenSelectedWorkOrderMissingErpFormalIdentity() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L))).thenReturn(List.of());
        when(workOrderMapper.selectById(100L)).thenReturn(workOrder(100L, "MO-100", 20L));
        when(syncRecordMapper.selectByWorkOrderId(100L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrders(reqVO));

        assertEquals(1_040_270_023, exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldFailFastWhenSelectedWorkOrderNotConfirmed() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L))).thenReturn(List.of());
        MesProWorkOrderDO workOrder = workOrder(100L, "MO-100", 20L);
        workOrder.setStatus(MesProWorkOrderStatusEnum.PREPARE.getStatus());
        when(workOrderMapper.selectById(100L)).thenReturn(workOrder);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrders(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_WORK_ORDER_NOT_CONFIRMED.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldReportEverySelectedWorkOrderCodeAndReason() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L, 101L, 102L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        MesProWorkOrderDO frozen = workOrder(100L, "MO-100", 20L);
        frozen.setTemporaryFrozen(Boolean.TRUE);
        MesProWorkOrderDO notConfirmed = workOrder(101L, "MO-101", 21L);
        notConfirmed.setStatus(MesProWorkOrderStatusEnum.PREPARE.getStatus());
        MesProWorkOrderDO erpMissing = workOrder(102L, "MO-102", 22L);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L, 101L, 102L))).thenReturn(List.of());
        when(workOrderMapper.selectById(100L)).thenReturn(frozen);
        when(workOrderMapper.selectById(101L)).thenReturn(notConfirmed);
        when(workOrderMapper.selectById(102L)).thenReturn(erpMissing);
        when(syncRecordMapper.selectByWorkOrderId(102L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrders(reqVO));

        assertTrue(exception.getMessage().contains("MO-100"));
        assertTrue(exception.getMessage().contains("生产工单已被临时冻结"));
        assertTrue(exception.getMessage().contains("MO-101"));
        assertTrue(exception.getMessage().contains("不是已确认状态"));
        assertTrue(exception.getMessage().contains("MO-102"));
        assertTrue(exception.getMessage().contains("缺少 ERP 正式同步记录"));
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldAggregatePreflightAndRouteIssuesWithWorkOrderCodes() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(103L, 104L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        MesProWorkOrderDO frozen = workOrder(103L, "MO-103", 999L);
        frozen.setTemporaryFrozen(Boolean.TRUE);
        MesProWorkOrderDO routeMissing = workOrder(104L, "MO-104", 998L);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(103L, 104L))).thenReturn(List.of());
        when(workOrderMapper.selectById(103L)).thenReturn(frozen);
        when(workOrderMapper.selectById(104L)).thenReturn(routeMissing);
        when(routeProductMapper.selectByItemId(998L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrders(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_BATCH_ADMISSION_BLOCKED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("MO-103"));
        assertTrue(exception.getMessage().contains("生产工单已被临时冻结"));
        assertTrue(exception.getMessage().contains("MO-104"));
        assertTrue(exception.getMessage().contains("产品缺少启用工艺路线"));
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldRejectEmptySelectionBeforeAnyInsert() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(java.util.Arrays.asList(null, null));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrders(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_BATCH_REQUIRED.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldReportAlreadyAdmittedWorkOrder() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L, 101L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L, 101L)))
                .thenReturn(List.of(MesProScheduleOrderDO.builder().id(901L).workOrderId(101L).build()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrders(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("工单ID 101"));
        assertTrue(exception.getMessage().contains("已在排产工单池中"));
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrders_shouldReportNonDeletedScheduleOrder() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L, 101L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        MesProScheduleOrderDO finishedButNotDeleted = MesProScheduleOrderDO.builder()
                .id(901L)
                .workOrderId(101L)
                .status(MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .build();
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L, 101L)))
                .thenReturn(List.of(finishedButNotDeleted));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrders(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("工单ID 101"));
        assertTrue(exception.getMessage().contains("已在排产工单池中"));
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    private MesProFeedbackDO feedback(Long id, Long scheduleOrderId, Long scheduleOrderProcessId, Long processId,
                                      String quantity, LocalDateTime feedbackTime) {
        return MesProFeedbackDO.builder()
                .id(id)
                .scheduleOrderId(scheduleOrderId)
                .scheduleOrderProcessId(scheduleOrderProcessId)
                .processId(processId)
                .feedbackQuantity(new BigDecimal(quantity))
                .status(cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum.FINISHED.getStatus())
                .feedbackTime(feedbackTime)
                .build();
    }

    @Test
    void createFromWorkOrder_shouldRejectDuplicateEffectiveScheduleOrder() {
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(100L);
        reqVO.setPromiseDate(LocalDate.of(2026, 6, 30));
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L)
                .quantity(BigDecimal.TEN)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO existing = MesProScheduleOrderDO.builder().id(900L).workOrderId(100L).build();
        when(workOrderMapper.selectById(100L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L))).thenReturn(List.of(existing));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
    }

    @Test
    void createFromWorkOrder_shouldRejectMissingPromiseDateBeforeReadingWorkOrder() {
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(100L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_PROMISE_DATE_REQUIRED.getCode(), exception.getCode());
        verify(workOrderMapper, never()).selectById(100L);
    }

    @Test
    void createFromWorkOrder_shouldBindActiveRouteVersionAndCopyScheduleConfigToProcessSnapshot() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L)
                .code("ERP-MO-002")
                .name("ERP order 002")
                .productId(20L)
                .quantity(new BigDecimal("80.000000"))
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(30L).itemId(20L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(30L).code("ROUTE-B").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO finiteProcess = MesProRouteProcessDO.builder().id(300L).routeId(30L).processId(40L).sort(1).build();
        MesProRouteProcessDO infiniteProcess = MesProRouteProcessDO.builder().id(301L).routeId(30L).processId(41L).sort(2).build();
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder()
                .id(700L)
                .routeId(30L)
                .versionNo("V2")
                .active(Boolean.TRUE)
                .build();
        MesProRouteScheduleConfigDO finiteConfig = MesProRouteScheduleConfigDO.builder()
                .id(800L)
                .routeVersionId(700L)
                .itemId(20L)
                .routeProcessId(300L)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(new BigDecimal("22.000000"))
                .nightShiftEnabled(Boolean.TRUE)
                .calendarRuleId(900L)
                .configVersion("CFG-FINITE-1")
                .build();
        MesProRouteScheduleConfigDO infiniteConfig = MesProRouteScheduleConfigDO.builder()
                .id(801L)
                .routeVersionId(700L)
                .itemId(20L)
                .routeProcessId(301L)
                .capacityMode(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode())
                .infiniteDurationQuantityFactor(new BigDecimal("1.500000"))
                .infiniteDurationBaseMinutes(new BigDecimal("12.000000"))
                .nightShiftEnabled(Boolean.FALSE)
                .configVersion("CFG-INF-1")
                .build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(100L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));

        when(workOrderMapper.selectById(100L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L))).thenReturn(List.of());
        when(scheduleOrderMapper.selectMaxCodeByPrefix(anyString())).thenReturn(null);
        when(routeProductMapper.selectByItemId(20L)).thenReturn(routeProduct);
        when(routeMapper.selectById(30L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(30L)).thenReturn(List.of(finiteProcess, infiniteProcess));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(30L)).thenReturn(List.of(edge(30L, 300L, 301L)));
        when(routeVersionMapper.selectActiveByRouteId(30L)).thenReturn(activeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(30L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(
                        MesProRouteFlowProcessConfigDO.builder().routeFlowConfigId(30L).routeId(30L).routeProcessId(300L)
                                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE)
                                .productionQuantityFactor(BigDecimal.ONE).build(),
                        MesProRouteFlowProcessConfigDO.builder().routeFlowConfigId(30L).routeId(30L).routeProcessId(301L)
                                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE)
                                .productionQuantityFactor(BigDecimal.ONE).build()
                ));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(finiteConfig, infiniteConfig));
        when(processMapper.selectBatchIds(List.of(40L, 41L))).thenReturn(List.of(
                MesProProcessDO.builder().id(40L).code("B010").name("吹球囊成型").build(),
                MesProProcessDO.builder().id(41L).code("B020").name("球囊全检").build()
        ));
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(901L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        scheduleOrderService.createFromWorkOrder(reqVO);

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).insert(orderCaptor.capture());
        MesProScheduleOrderDO order = orderCaptor.getValue();
        assertEquals(700L, order.getRouteVersionId());
        assertEquals("V2", order.getRouteVersion());
        assertEquals("V2", order.getScheduleConfigVersion());
        assertTrue(order.getRouteSnapshotJson().contains("\"routeVersionId\":700"));

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper, times(2)).insert(processCaptor.capture());
        List<MesProScheduleOrderProcessDO> snapshots = processCaptor.getAllValues();
        MesProScheduleOrderProcessDO finiteSnapshot = snapshots.get(0);
        assertEquals(700L, finiteSnapshot.getRouteVersionId());
        assertEquals(800L, finiteSnapshot.getRouteScheduleConfigId());
        assertEquals(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), finiteSnapshot.getCapacityMode());
        assertEquals(new BigDecimal("22.000000"), finiteSnapshot.getHourlyCapacityTotal());
        assertNull(finiteSnapshot.getShiftCapacityTotal());
        assertEquals(Boolean.TRUE, finiteSnapshot.getNightShiftEnabled());
        assertEquals(900L, finiteSnapshot.getCalendarRuleId());
        assertTrue(finiteSnapshot.getResourceSnapshotJson().contains("\"capacityMode\":\"FINITE_HOURLY\""));

        MesProScheduleOrderProcessDO infiniteSnapshot = snapshots.get(1);
        assertEquals(700L, infiniteSnapshot.getRouteVersionId());
        assertEquals(801L, infiniteSnapshot.getRouteScheduleConfigId());
        assertEquals(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), infiniteSnapshot.getCapacityMode());
        assertEquals(new BigDecimal("1.500000"), infiniteSnapshot.getInfiniteDurationQuantityFactor());
        assertEquals(new BigDecimal("12.000000"), infiniteSnapshot.getInfiniteDurationBaseMinutes());
        assertEquals(Boolean.FALSE, infiniteSnapshot.getNightShiftEnabled());
    }

    @Test
    void createFromWorkOrder_shouldRejectMissingScheduleUseConfig() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(106L).code("ERP-MO-007").productId(26L).quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).temporaryFrozen(Boolean.FALSE).build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(36L).itemId(26L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(36L).code("ROUTE-G").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(306L).routeId(36L).processId(46L).sort(1).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(106L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 6));

        when(workOrderMapper.selectById(106L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(106L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(26L)).thenReturn(routeProduct);
        when(routeMapper.selectById(36L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(36L)).thenReturn(List.of(routeProcess));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(36L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
    }

    @Test
    void createFromWorkOrder_shouldRejectRouteWithoutActiveVersion() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(126L).code("ERP-MO-027").productId(46L).quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).temporaryFrozen(Boolean.FALSE).build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(56L).itemId(46L).build();
        MesProRouteDO route = MesProRouteDO.builder()
                .id(56L).code("ROUTE-NO-ACTIVE").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(326L).routeId(56L).processId(66L).sort(1).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(126L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 16));

        when(workOrderMapper.selectById(126L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(126L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(46L)).thenReturn(routeProduct);
        when(routeMapper.selectById(56L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(56L)).thenReturn(List.of(routeProcess));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(56L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .id(856L)
                        .routeFlowConfigId(56L)
                        .routeId(56L)
                        .routeProcessId(326L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeVersionMapper.selectActiveByRouteId(56L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
        verify(scheduleOrderMapper, never()).selectMaxRouteVersionByPrefix(anyString());
    }

    @Test
    void createFromWorkOrder_shouldRejectDisabledScheduleFlowEvenWhenChildConfigEnabled() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(116L).code("ERP-MO-017").productId(36L).quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).temporaryFrozen(Boolean.FALSE).build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(46L).itemId(36L).build();
        MesProRouteDO route = MesProRouteDO.builder()
                .id(46L).code("ROUTE-Q").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(316L).routeId(46L).processId(56L).sort(1).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(116L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 16));

        when(workOrderMapper.selectById(116L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(116L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(36L)).thenReturn(routeProduct);
        when(routeMapper.selectById(46L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(46L)).thenReturn(List.of(routeProcess));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(46L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(846L).routeId(46L).useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.FALSE).build());
        org.mockito.Mockito.lenient().when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                        46L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .id(847L).routeFlowConfigId(846L).routeId(46L).routeProcessId(316L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE).build()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
    }

    @Test
    void createFromWorkOrder_shouldRejectMissingRouteScheduleConfig() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(107L).code("ERP-MO-008").productId(27L).quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).temporaryFrozen(Boolean.FALSE).build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(37L).itemId(27L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(37L).code("ROUTE-H").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(307L).routeId(37L).processId(47L).sort(1).build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder().id(707L).routeId(37L).versionNo("V1").active(Boolean.TRUE).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(107L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 7));

        when(workOrderMapper.selectById(107L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(107L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(27L)).thenReturn(routeProduct);
        when(routeMapper.selectById(37L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(37L)).thenReturn(List.of(routeProcess));
        when(routeVersionMapper.selectActiveByRouteId(37L)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(37L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder().routeFlowConfigId(37L)
                        .routeId(37L).routeProcessId(307L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE).build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(707L)).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
    }

    @Test
    void createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(108L).code("ERP-MO-009").productId(28L).quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).temporaryFrozen(Boolean.FALSE).build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(38L).itemId(28L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(38L).code("ROUTE-I").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(308L).routeId(38L).processId(48L).sort(1).workstationId(508L).build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder().id(708L).routeId(38L).versionNo("V1").active(Boolean.TRUE).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(108L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 8));

        when(workOrderMapper.selectById(108L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(108L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(28L)).thenReturn(routeProduct);
        when(routeMapper.selectById(38L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(38L)).thenReturn(List.of(routeProcess));
        when(routeVersionMapper.selectActiveByRouteId(38L)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(38L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder().routeFlowConfigId(38L)
                        .routeId(38L).routeProcessId(308L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE).build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(708L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(812L)
                        .routeVersionId(708L)
                        .itemId(28L)
                        .routeProcessId(308L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("9.000000"))
                        .build()));
        when(processMapper.selectBatchIds(List.of(48L))).thenReturn(List.of(
                MesProProcessDO.builder().id(48L).code("B048").name("设备工序").build()
        ));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(38L)).thenReturn(List.of());
        when(workstationMapper.selectBatchIds(Set.of(508L))).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(508L).code("WS-508").name("设备工位")
                        .processId(48L).singleStandardHourlyCapacity(new BigDecimal("9.000000")).build()
        ));
        when(workstationMapper.selectListByProcessIds(
                List.of(48L), CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of());
        when(workstationMachineMapper.selectListByWorkstationIds(List.of(508L))).thenReturn(List.of());
        when(workstationWorkerMapper.selectListByWorkstationIds(List.of(508L))).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(608L).workstationId(508L).quantity(1).build()
        ));
        when(machineryProcessMapper.selectListByMachineryIds(Set.of())).thenReturn(List.of());
        scheduleOrderService.createFromWorkOrder(reqVO);

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).insert(processCaptor.capture());
        assertEquals(0, processCaptor.getValue().getShiftHours().compareTo(new BigDecimal("10.5")));
        assertEquals(0, processCaptor.getValue().getShiftCapacityTotal().compareTo(new BigDecimal("94.5000000")));
    }

    @Test
    void createFromWorkOrders_shouldUseManualWorkerCapacityWhenWorkerQuantityMissing() {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(109L));
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 10));
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(109L).code("ERP-MO-010").productId(29L).quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).temporaryFrozen(Boolean.FALSE).build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(39L).itemId(29L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(39L).code("ROUTE-J").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(309L).routeId(39L).processId(49L).sort(1).workstationId(509L).build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder().id(709L).routeId(39L).versionNo("V1").active(Boolean.TRUE).build();

        when(workOrderMapper.selectById(109L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(109L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(29L)).thenReturn(routeProduct);
        when(routeMapper.selectById(39L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(39L)).thenReturn(List.of(routeProcess));
        when(routeVersionMapper.selectActiveByRouteId(39L)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(39L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder().routeFlowConfigId(39L)
                        .routeId(39L).routeProcessId(309L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE).build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(709L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(813L)
                        .routeVersionId(709L)
                        .itemId(29L)
                        .routeProcessId(309L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("9.000000"))
                        .build()));
        when(processMapper.selectBatchIds(List.of(49L))).thenReturn(List.of(
                MesProProcessDO.builder().id(49L).code("B049").name("人工工序").build()
        ));
        when(workstationMapper.selectBatchIds(Set.of(509L))).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(509L).code("WS-509").name("人工工位")
                        .processId(49L).shiftHours(new BigDecimal("8.0"))
                        .singleStandardHourlyCapacity(new BigDecimal("2.0")).build()
        ));
        when(workstationMachineMapper.selectListByWorkstationIds(List.of(509L))).thenReturn(List.of());
        when(workstationWorkerMapper.selectListByWorkstationIds(List.of(509L))).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(609L).workstationId(509L).quantity(null).build()
        ));
        when(machineryProcessMapper.selectListByMachineryIds(Set.of())).thenReturn(List.of());

        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(900109L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        List<Long> ids = scheduleOrderService.createFromWorkOrders(reqVO);

        assertEquals(List.of(900109L), ids);
        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).insert(processCaptor.capture());
        MesProScheduleOrderProcessDO snapshot = processCaptor.getValue();
        assertEquals("WORKER", snapshot.getCapacitySource());
        assertEquals(0, snapshot.getHourlyCapacityTotal().compareTo(new BigDecimal("9.000000")));
        assertEquals(0, snapshot.getShiftCapacityTotal().compareTo(new BigDecimal("72.0000000")));
        assertTrue(snapshot.getResourceSnapshotJson().contains("\"workerQuantity\":0"));
    }

    private MesProWorkOrderDO workOrder(Long id, String code, Long productId) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .productId(productId)
                .quantity(BigDecimal.TEN)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
    }

    private void stubSimpleSchedulableWorkOrder(Long workOrderId, String workOrderCode, Long productId,
                                                Long routeId, Long routeProcessId, Long processId,
                                                Long routeVersionId, Long scheduleConfigId,
                                                String processCode, String processName) {
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder()
                .routeId(routeId)
                .itemId(productId)
                .build();
        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .code("ROUTE-" + routeId)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(processId)
                .sort(1)
                .keyFlag(Boolean.TRUE)
                .build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(routeId)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .build();
        when(workOrderMapper.selectById(workOrderId)).thenReturn(workOrder(workOrderId, workOrderCode, productId));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(workOrderId))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(productId)).thenReturn(routeProduct);
        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(routeProcess));
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(routeId)
                        .routeId(routeId)
                        .routeProcessId(routeProcessId)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(scheduleConfigId)
                        .routeVersionId(routeVersionId)
                        .itemId(productId)
                        .routeProcessId(routeProcessId)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("10.000000"))
                        .build()));
        when(processMapper.selectBatchIds(List.of(processId))).thenReturn(List.of(
                MesProProcessDO.builder().id(processId).code(processCode).name(processName).build()));
        org.mockito.Mockito.lenient().when(workstationMapper.selectListByProcessIds(List.of(processId))).thenReturn(List.of());
    }

    private MesProRouteProcessFlowEdgeDO edge(Long routeId, Long sourceRouteProcessId, Long targetRouteProcessId) {
        return MesProRouteProcessFlowEdgeDO.builder()
                .routeId(routeId)
                .sourceRouteProcessId(sourceRouteProcessId)
                .targetRouteProcessId(targetRouteProcessId)
                .relationType("NORMAL")
                .sort(1)
                .build();
    }

}
