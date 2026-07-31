package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchExecutionLegacyProcessTest {

    private static final Long ROUTE_ID = 20L;
    private static final Long CURRENT_ROUTE_PROCESS_ID = 100L;
    private static final Long HISTORICAL_ROUTE_PROCESS_ID = 99L;
    private static final Long PROCESS_ID = 1000L;
    private static final Long BATCH_RECORD_DEFINITION_ID = 6001L;
    private static final Long BATCH_RECORD_VERSION_ID = 6002L;

    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProBatchRecordReportMapper reportMapper;
    @Mock
    private MesProBatchRecordVersionMapper batchRecordVersionMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;

    private MesProEdhrBatchExecutionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesProEdhrBatchExecutionServiceImpl();
        ReflectionTestUtils.setField(service, "routeFlowConfigMapper", routeFlowConfigMapper);
        ReflectionTestUtils.setField(service, "routeFlowProcessConfigMapper", routeFlowProcessConfigMapper);
        ReflectionTestUtils.setField(service, "routeFlowProcessBatchRecordMapper", routeFlowProcessBatchRecordMapper);
        ReflectionTestUtils.setField(service, "reportMapper", reportMapper);
        ReflectionTestUtils.setField(service, "batchRecordVersionMapper", batchRecordVersionMapper);
        ReflectionTestUtils.setField(service, "processMapper", processMapper);
        ReflectionTestUtils.setField(service, "routeProcessFlowEdgeMapper", routeProcessFlowEdgeMapper);
        ReflectionTestUtils.setField(service, "routeProcessService", routeProcessService);
    }

    @Test
    void resolveBatchTaskConfigs_shouldKeepFrozenHistoricalRouteProcessBinding() {
        MesProRouteDO route = MesProRouteDO.builder().id(ROUTE_ID).build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(CURRENT_ROUTE_PROCESS_ID)
                .routeId(ROUTE_ID)
                .processId(PROCESS_ID)
                .sort(10)
                .build();
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(HISTORICAL_ROUTE_PROCESS_ID)
                .routeId(ROUTE_ID)
                .processId(PROCESS_ID)
                .sort(10)
                .build();
        MesProRouteFlowProcessConfigDO historicalConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(200L)
                .routeFlowConfigId(300L)
                .routeId(ROUTE_ID)
                .routeProcessId(HISTORICAL_ROUTE_PROCESS_ID)
                .useType("BATCH")
                .enabled(true)
                .executionMode("SEQUENTIAL")
                .build();
        MesProRouteFlowProcessBatchRecordDO historicalRecord = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(400L)
                .routeFlowProcessConfigId(historicalConfig.getId())
                .routeId(ROUTE_ID)
                .routeProcessId(HISTORICAL_ROUTE_PROCESS_ID)
                .useType("BATCH")
                .batchRecordReportId("REPORT-1")
                .batchRecordDefinitionId(BATCH_RECORD_DEFINITION_ID)
                .batchRecordVersionId(BATCH_RECORD_VERSION_ID)
                .formSlotType("MAIN")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .permissionScopeId(5001L)
                .requiredPolicy("REQUIRED")
                .archiveVisibility("FINAL_DHR")
                .slotConfigSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                .reportSort(1)
                .build();
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setReportId("REPORT-1");
        report.setBatchRecordDefinitionId(BATCH_RECORD_DEFINITION_ID);
        report.setBatchRecordVersionId(BATCH_RECORD_VERSION_ID);
        report.setFormSlotType("MAIN");
        report.setSourceTableIndex(1);

        when(routeFlowConfigMapper.selectByRouteIdAndUseType(ROUTE_ID, "BATCH"))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(300L)
                        .routeId(ROUTE_ID)
                        .useType("BATCH")
                        .enabled(true)
                        .build());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(ROUTE_ID, "BATCH"))
                .thenReturn(List.of(historicalConfig));
        when(routeProcessService.resolveFrozenRouteProcess(HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, null))
                .thenReturn(frozenRouteProcess);
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                anyCollection(), eq("BATCH"))).thenReturn(List.of(historicalRecord));
        when(reportMapper.selectListByReportIds(anyCollection())).thenReturn(List.of(report));
        when(batchRecordVersionMapper.selectLatestApprovedByDefinitionId(BATCH_RECORD_DEFINITION_ID))
                .thenReturn(MesProBatchRecordVersionDO.builder()
                        .id(BATCH_RECORD_VERSION_ID)
                        .definitionId(BATCH_RECORD_DEFINITION_ID)
                        .versionNo("V1.0")
                        .status("APPROVED")
                        .build());
        when(reportMapper.selectListByDefinitionIdAndVersionId(
                BATCH_RECORD_DEFINITION_ID, BATCH_RECORD_VERSION_ID)).thenReturn(List.of(report));
        when(processMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(MesProProcessDO.builder().id(PROCESS_ID).name("历史工序").build()));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of());

        List<?> result = ReflectionTestUtils.invokeMethod(
                service, "resolveBatchTaskConfigs", route, List.of(currentRouteProcess));

        assertNotNull(result);
        assertEquals(1, result.size());
        Object taskConfig = result.get(0);
        MesProRouteProcessDO resolvedRouteProcess =
                (MesProRouteProcessDO) ReflectionTestUtils.getField(taskConfig, "routeProcess");
        assertNotNull(resolvedRouteProcess);
        assertEquals(HISTORICAL_ROUTE_PROCESS_ID, resolvedRouteProcess.getId());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, null);
    }
}
