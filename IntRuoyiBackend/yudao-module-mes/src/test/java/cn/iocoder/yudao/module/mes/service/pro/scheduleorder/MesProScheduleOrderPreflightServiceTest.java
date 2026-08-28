package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRouteStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderPreflightServiceTest {

    @InjectMocks
    private MesProScheduleOrderServiceImpl scheduleOrderService;
    @Spy
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy = new ScheduleDefaultCompatibilityPolicy();

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Mock
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Mock
    private MesDvMachineryMapper machineryMapper;
    @Mock
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Mock
    private MesProProcessMapper processMapper;

    @Test
    void preflight_shouldBlockMissingRouteAndWarnMissingErpSyncEvidence() {
        MesProScheduleOrderDO missingRoute = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SCH-MISSING")
                .workOrderId(100L)
                .erpWorkOrderCode("MO-MISSING")
                .productId(200L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.MISSING.getStatus())
                .autoSchedulable(Boolean.FALSE)
                .build();
        MesProScheduleOrderDO readyButNoSyncRecord = MesProScheduleOrderDO.builder()
                .id(11L)
                .code("SCH-WARN")
                .workOrderId(101L)
                .erpWorkOrderCode("MO-WARN")
                .productId(201L)
                .routeId(301L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE)
                .build();

        when(scheduleOrderMapper.selectListByIds(List.of(10L, 11L))).thenReturn(List.of(missingRoute, readyButNoSyncRecord));
        when(itemMapper.selectListByIds(List.of(200L, 201L))).thenReturn(List.of(
                MesMdItemDO.builder().id(200L).code("ITEM-200").name("缺路线产品").build(),
                MesMdItemDO.builder().id(201L).code("ITEM-201").name("已建路线产品").build()
        ));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(10L, 11L))).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .scheduleOrderId(11L)
                        .processId(401L)
                        .routeProcessId(4010L)
                        .enabled(Boolean.TRUE)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacityTotal(new BigDecimal("12.000000"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()
        ));
        when(syncRecordMapper.selectByWorkOrderId(100L)).thenReturn(MesKingdeeProductionOrderSyncRecordDO.builder()
                .id(900L)
                .workOrderId(100L)
                .build());
        when(syncRecordMapper.selectByWorkOrderId(101L)).thenReturn(null);

        MesProScheduleOrderPreflightReqVO reqVO = new MesProScheduleOrderPreflightReqVO();
        reqVO.setScopeType("SELECTED");
        reqVO.setScheduleOrderIds(List.of(10L, 11L));

        MesProScheduleOrderPreflightRespVO result = scheduleOrderService.preflight(reqVO);

        assertEquals("BLOCKED", result.getResult());
        assertEquals(0, result.getSummary().getPassCount());
        assertEquals(1, result.getSummary().getWarnCount());
        assertEquals(1, result.getSummary().getBlockedCount());
        assertTrue(result.getIssues().stream()
                .anyMatch(issue -> "BLOCKED_MISSING_ROUTE".equals(issue.getReasonCode())
                        && "维护路线".equals(issue.getAction().getActionLabel())
                        && "MesProRoute".equals(issue.getAction().getTargetRouteName())
                        && "mes:pro-route:update".equals(issue.getAction().getRequiredPermission())));
        assertTrue(result.getIssues().stream()
                .anyMatch(issue -> "WARN_ERP_SYNC_RECORD_MISSING".equals(issue.getReasonCode())
                        && "WARN".equals(issue.getSeverity())));
    }

    @Test
    void preflight_shouldNotBlockMissingBatchCodeWhenBatchRouteEnabled() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(21L)
                .code("SCH-BATCH")
                .workOrderId(201L)
                .erpWorkOrderCode("MO-BATCH")
                .productId(301L)
                .routeId(401L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE)
                .build();

        when(scheduleOrderMapper.selectListByIds(List.of(21L))).thenReturn(List.of(scheduleOrder));
        when(itemMapper.selectListByIds(List.of(301L))).thenReturn(List.of(
                MesMdItemDO.builder().id(301L).code("ITEM-301").name("批记录产品").build()
        ));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(21L))).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .scheduleOrderId(21L)
                        .processId(501L)
                        .processName("第一工序")
                        .routeProcessId(5101L)
                        .enabled(Boolean.TRUE)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacityTotal(new BigDecimal("10.000000"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));
        when(syncRecordMapper.selectByWorkOrderId(201L)).thenReturn(MesKingdeeProductionOrderSyncRecordDO.builder()
                .id(901L)
                .workOrderId(201L)
                .sourceFid("FID-201")
                .sourceBillNo("BILL-201")
                .build());

        MesProScheduleOrderPreflightReqVO reqVO = new MesProScheduleOrderPreflightReqVO();
        reqVO.setScopeType("SELECTED");
        reqVO.setScheduleOrderIds(List.of(21L));

        MesProScheduleOrderPreflightRespVO result = scheduleOrderService.preflight(reqVO);

        assertEquals("PASS", result.getResult());
        assertEquals(1, result.getSummary().getPassCount());
        assertEquals(0, result.getSummary().getBlockedCount());
        assertTrue(result.getIssues().stream()
                .noneMatch(issue -> "BLOCKED_BATCH_CODE_REQUIRED".equals(issue.getReasonCode())));
    }

    @Test
    void preflight_shouldNotBlockInvalidBatchRouteConfigWhenDefaultReportMissing() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(22L)
                .code("SCH-BATCH-CONFIG")
                .workOrderId(202L)
                .erpWorkOrderCode("MO-BATCH-CONFIG")
                .productId(302L)
                .routeId(402L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE)
                .build();

        when(scheduleOrderMapper.selectListByIds(List.of(22L))).thenReturn(List.of(scheduleOrder));
        when(itemMapper.selectListByIds(List.of(302L))).thenReturn(List.of(
                MesMdItemDO.builder().id(302L).code("ITEM-302").name("批记录配置产品").build()
        ));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(22L))).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .scheduleOrderId(22L)
                        .processId(502L)
                        .processName("第二工序")
                        .routeProcessId(5201L)
                        .enabled(Boolean.TRUE)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacityTotal(new BigDecimal("10.000000"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));
        when(syncRecordMapper.selectByWorkOrderId(202L)).thenReturn(MesKingdeeProductionOrderSyncRecordDO.builder()
                .id(902L)
                .workOrderId(202L)
                .sourceFid("FID-202")
                .sourceBillNo("BILL-202")
                .build());

        MesProScheduleOrderPreflightReqVO reqVO = new MesProScheduleOrderPreflightReqVO();
        reqVO.setScopeType("SELECTED");
        reqVO.setScheduleOrderIds(List.of(22L));

        MesProScheduleOrderPreflightRespVO result = scheduleOrderService.preflight(reqVO);

        assertEquals("PASS", result.getResult());
        assertEquals(1, result.getSummary().getPassCount());
        assertEquals(0, result.getSummary().getBlockedCount());
        assertTrue(result.getIssues().stream()
                .noneMatch(issue -> "BLOCKED_BATCH_ROUTE_CONFIG_INVALID".equals(issue.getReasonCode())));
    }

    @Test
    void preflight_shouldWarnForDefaultScheduleConfigWithoutBlocking() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(23L)
                .code("SCH-DEFAULT-CONFIG")
                .workOrderId(203L)
                .erpWorkOrderCode("MO-DEFAULT-CONFIG")
                .productId(303L)
                .routeId(403L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE)
                .build();

        when(scheduleOrderMapper.selectListByIds(List.of(23L))).thenReturn(List.of(scheduleOrder));
        when(itemMapper.selectListByIds(List.of(303L))).thenReturn(List.of(
                MesMdItemDO.builder().id(303L).code("ITEM-303").name("默认策略产品").build()
        ));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(23L))).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .scheduleOrderId(23L)
                        .processId(503L)
                        .processName("默认工序")
                        .routeProcessId(5301L)
                        .routeScheduleConfigId(6301L)
                        .enabled(Boolean.TRUE)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacityTotal(new BigDecimal("1.000000"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .resourceSnapshotJson(JsonUtils.toJsonString(java.util.Map.of(
                                "configVersion", MesProRouteServiceImpl.DEFAULT_SCHEDULE_CONFIG_VERSION)))
                        .build()));
        when(syncRecordMapper.selectByWorkOrderId(203L)).thenReturn(MesKingdeeProductionOrderSyncRecordDO.builder()
                .id(903L)
                .workOrderId(203L)
                .build());

        MesProScheduleOrderPreflightReqVO reqVO = new MesProScheduleOrderPreflightReqVO();
        reqVO.setScopeType("SELECTED");
        reqVO.setScheduleOrderIds(List.of(23L));

        MesProScheduleOrderPreflightRespVO result = scheduleOrderService.preflight(reqVO);

        assertEquals("WARN", result.getResult());
        assertEquals(0, result.getSummary().getPassCount());
        assertEquals(1, result.getSummary().getWarnCount());
        assertEquals(0, result.getSummary().getBlockedCount());
        assertTrue(result.getIssues().stream()
                .anyMatch(issue -> "WARN_DEFAULT_ROUTE_SCHEDULE_CONFIG".equals(issue.getReasonCode())
                        && "WARN".equals(issue.getSeverity())
                        && "维护排产策略".equals(issue.getAction().getActionLabel())
                        && "MesProRouteEdit".equals(issue.getAction().getTargetRouteName())
                        && "schedule-config".equals(issue.getAction().getTargetQuery().get("tab"))
                        && Long.valueOf(403L).equals(issue.getAction().getTargetQuery().get("routeId"))
                        && Long.valueOf(5301L).equals(issue.getAction().getTargetQuery().get("routeProcessId"))
                        && "mes:pro-route:schedule-config:update".equals(issue.getAction().getRequiredPermission())));
    }

}
