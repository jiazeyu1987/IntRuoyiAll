package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormCellsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRuleSaveItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupMappingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordRepeatRowGroupDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordRepeatRowGroupMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordCellLinkServiceImplTest {

    @Mock
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    @Mock
    private MesProBatchRecordReportMapper reportMapper;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordReportService reportService;
    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private FormTemplateVersionMapper templateVersionMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper deviceParameterRuleMapper;
    @Mock
    private MesProBatchRecordRepeatRowGroupMapper repeatRowGroupMapper;
    @Mock
    private MesProductionPickListSourceService productionPickListSourceService;
    @Mock
    private MesQaInspectionRegulationMapper qaRegulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper qaRegulationVersionMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper qaRegulationItemMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Mock
    private MesQaInspectionRegulationProcessMapper qaRegulationProcessMapper;

    @InjectMocks
    private MesProBatchRecordCellLinkServiceImpl service;

    @BeforeEach
    void setUpPickListCatalog() {
        lenient().when(productionPickListSourceService.listSourceFields(any())).thenReturn(List.of());
    }

    @Test
    void getWorkbenchContext_exposesProductionPickListFieldsForCurrentRouteProcess() {
        MesProBatchRecordReportDO targetReport = report("target-report", "粗洗工序生产记录", 2001L, 3001L);
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L)).thenReturn(List.of(targetReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIds(any())).thenReturn(List.of(
                routeBinding(5001L, 3001L, "target-report")));
        when(productionPickListSourceService.listSourceFields(9001L)).thenReturn(List.of(
                new MesProductionPickListSourceService.SourceField(
                        "material.3201.lotNumber", "手柄（MAT-001）- 物料批次号", "STRING", 5001L)));

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, 2001L, 3001L, null, null, null, null);

        assertEquals(1, result.getSourceFields().stream()
                .filter(field -> "PRODUCTION_PICK_LIST".equals(field.getSourceType()))
                .filter(field -> "material.3201.lotNumber".equals(field.getFieldCode()))
                .filter(field -> Long.valueOf(5001L).equals(field.getRouteProcessId()))
                .count());
    }

    @Test
    void getWorkbenchContext_exposesPqcAggregateFieldsForRequestedSharedProcessInspectionForm() {
        MesProBatchRecordReportDO sharedProcessInspectionReport =
                report("process-inspection-report", "球囊扩张压力泵过程检验记录", 2001L, 3001L);
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L))
                .thenReturn(List.of(sharedProcessInspectionReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        stubPublishedQaForRouteProcess(5001L, 8001L, "FIRST", "LEAK", "泄漏测试");

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, 2001L, 3001L,
                        null, null, null, 5001L);

        assertPqcSourceField(result, "FIRST|LEAK|1|measuredValue",
                "首检 / 泄漏测试 / 第1件 / 实测值", "STRING", 5001L);
        assertPqcSourceField(result, "FIRST|LEAK|1|selectedEquipmentName",
                "首检 / 泄漏测试 / 第1件 / 检验设备名称", "STRING", 5001L);
        assertPqcSourceField(result, "FIRST|DCC|dccProjectCode",
                "首检 / DCC项目代码", "STRING", 5001L);
    }

    @Test
    void getWorkbenchContext_acceptsPqcAggregateDetailAsVirtualSourceReportId() {
        MesProBatchRecordReportDO sharedProcessInspectionReport =
                report("process-inspection-report", "球囊扩张压力泵过程检验记录", 2001L, 3001L);
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L))
                .thenReturn(List.of(sharedProcessInspectionReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        stubPublishedQaForRouteProcess(5001L, 8001L, "FIRST", "LEAK", "泄漏测试");

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, 2001L, 3001L,
                        "PQC_AGGREGATE_DETAIL", null, null, 5001L);

        assertEquals("PQC_AGGREGATE_DETAIL", result.getDefaultSourceReportId());
        assertEquals("process-inspection-report", result.getDefaultTargetReportId());
        assertPqcSourceField(result, "FIRST|LEAK|1|measuredValue",
                "首检 / 泄漏测试 / 第1件 / 实测值", "STRING", 5001L);
    }

    @Test
    void getWorkbenchContext_resolvesPqcAggregateDetailFromRouteOnlyVirtualSource() {
        MesProBatchRecordReportDO sharedProcessInspectionReport =
                report("process-inspection-report", "球囊扩张压力泵过程检验记录", 2001L, 3001L);
        MesProRouteFlowProcessBatchRecordDO routeBinding =
                routeBinding(5001L, 3001L, "process-inspection-report")
                        .setBatchRecordDefinitionId(null)
                        .setBatchRecordVersionId(null);
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(9001L, "BATCH"))
                .thenReturn(List.of(routeBinding));
        when(reportMapper.selectListByReportIds(any())).thenReturn(List.of(sharedProcessInspectionReport));
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L))
                .thenReturn(List.of(sharedProcessInspectionReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        stubPublishedQaForRouteProcess(5001L, 8001L, "FIRST", "LEAK", "泄漏测试");

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, null, null,
                        "PQC_AGGREGATE_DETAIL", null, null, 5001L);

        assertEquals("ROUTE_VERSION", result.getScopeType());
        assertEquals(3001L, result.getScopeId());
        assertEquals("PQC_AGGREGATE_DETAIL", result.getDefaultSourceReportId());
        assertEquals("process-inspection-report", result.getDefaultTargetReportId());
        assertPqcSourceField(result, "FIRST|LEAK|1|measuredValue",
                "首检 / 泄漏测试 / 第1件 / 实测值", "STRING", 5001L);
    }

    @Test
    void getWorkbenchContext_usesActiveRouteVersionForPqcInsteadOfBatchRecordVersion() {
        MesProBatchRecordReportDO sharedProcessInspectionReport =
                report("process-inspection-report", "球囊扩张压力泵过程检验记录", 2001L, 3001L);
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L))
                .thenReturn(List.of(sharedProcessInspectionReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        stubPublishedQaForRouteProcess(5001L, 9101L, 8001L, "FIRST", "LEAK", "泄漏测试");

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, 2001L, 3001L,
                        "PQC_AGGREGATE_DETAIL", null, null, 5001L);

        assertEquals("ROUTE_VERSION", result.getScopeType());
        assertEquals(3001L, result.getScopeId());
        assertPqcSourceField(result, "FIRST|LEAK|1|selectedEquipmentNumber",
                "首检 / 泄漏测试 / 第1件 / 设备编号", "STRING", 5001L);
    }

    @Test
    void getWorkbenchContext_exposesProcessPoolReportFieldsFromRouteParameters() {
        MesProBatchRecordReportDO targetReport = report("target-report", "粗洗工序生产记录", 2001L, 3001L);
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L)).thenReturn(List.of(targetReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIds(any())).thenReturn(List.of(
                routeBinding(5001L, 3001L, "target-report"),
                routeBinding(5002L, 3001L, "target-report")));
        lenient().when(deviceParameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(11L, 5001L, "pressure", "扩张压力",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL),
                parameterRule(12L, 5002L, "holdTime", "保压时间",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER)));

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, 2001L, 3001L, null, null, null, null);

        assertProcessPoolSourceField(result, "allocatedQuantity", "放行分配数量", "NUMBER", null);
        assertProcessPoolSourceField(result, "actualEmployeeId", "实际操作员工", "NUMBER", null);
        assertProcessPoolSourceField(result, "serverSubmitTime", "提交时间", "STRING", null);
        assertProcessPoolSourceField(result, "selectedDevice.deviceName", "选用设备名称", "STRING", null);
        assertProcessPoolSourceField(result, "deviceMeteringValidity.inMeteringValidityPeriod",
                "选用设备计量有效期内", "BOOLEAN", null);
        assertProcessPoolSourceField(result, "clearanceConfirmations.workplace.confirmed", "清场确认", "BOOLEAN", null);
        assertProcessPoolSourceField(result, "deviceParameterReadings.pressure.value",
                "扩张压力实际值", "NUMBER", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.pressure.unit", "扩张压力单位", "STRING", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.pressure.lowerLimit",
                "扩张压力下限", "NUMBER", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.pressure.upperLimit",
                "扩张压力上限", "NUMBER", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.pressure.parameterStatus",
                "扩张压力状态", "STRING", 5001L);
        assertProcessPoolSourceField(result, "equipmentParameterRules.pressure.standardText",
                "扩张压力参考标准", "STRING", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.holdTime.value",
                "保压时间实际值", "NUMBER", 5002L);
    }

    @Test
    void getWorkbenchContext_resolvesMainSlotBlankRecordCategoryAsFormalBatchRecordProcess() {
        MesProBatchRecordReportDO targetReport = report("rough-wash-report", "粗洗工序生产记录", 2001L, 3001L);
        MesProRouteFlowProcessBatchRecordDO roughWashBinding = routeBinding(5001L, 3001L, "rough-wash-report")
                .setBatchRecordVersionId(null)
                .setRecordCategory(null)
                .setFormSlotType("MAIN");
        MesProRouteFlowProcessBatchRecordDO otherRouteBinding = routeBinding(6001L, 3001L, "rough-wash-report")
                .setRouteId(9002L)
                .setBatchRecordVersionId(null);
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L)).thenReturn(List.of(targetReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIds(any()))
                .thenReturn(List.of(roughWashBinding, otherRouteBinding));
        lenient().when(deviceParameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(11L, 5001L, "cleaningCount", "清洗次数",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER),
                parameterRule(12L, 5001L, "cleaningMedium", "清洗介质",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_SELECT),
                parameterRule(13L, 5001L, "cleaningPower", "清洗功率",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL),
                parameterRule(14L, 5001L, "roomTemperature", "室温",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL),
                parameterRule(15L, 5001L, "cleaningDuration", "清洗时间",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER)));

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, 2001L, 3001L, null, null, null, null);

        assertEquals(5001L, result.getForms().get(0).getRouteProcessId());
        assertProcessPoolSourceField(result, "outputQuantity", "本次报工产出数量", "NUMBER", null);
        assertProcessPoolSourceField(result, "actualEmployeeId", "实际操作员工", "NUMBER", null);
        assertProcessPoolSourceField(result, "serverSubmitTime", "提交时间", "STRING", null);
        assertProcessPoolSourceField(result, "selectedDevice.deviceName", "选用设备名称", "STRING", null);
        assertProcessPoolSourceField(result, "deviceMeteringValidity.inMeteringValidityPeriod",
                "选用设备计量有效期内", "BOOLEAN", null);
        assertProcessPoolSourceField(result, "clearanceConfirmations.workplace.confirmed", "清场确认", "BOOLEAN", null);
        assertProcessPoolSourceField(result, "deviceParameterReadings.cleaningCount.value",
                "清洗次数实际值", "NUMBER", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.cleaningMedium.value",
                "清洗介质实际值", "STRING", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.cleaningPower.value",
                "清洗功率实际值", "NUMBER", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.roomTemperature.value",
                "室温实际值", "NUMBER", 5001L);
        assertProcessPoolSourceField(result, "deviceParameterReadings.cleaningDuration.value",
                "清洗时间实际值", "NUMBER", 5001L);
    }

    @Test
    void saveRules_acceptsProcessPoolReportFieldWithExplicitAggregation() {
        stubProcessPoolSaveContext();
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());

        service.saveRules(processPoolRuleSaveRequest("deviceParameterReadings.pressure.value", "SUM"));

        ArgumentCaptor<List<MesProBatchRecordCellLinkRuleDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(ruleMapper).insertBatch(captor.capture());
        MesProBatchRecordCellLinkRuleDO row = captor.getValue().get(0);
        assertEquals("PROCESS_POOL_REPORT", row.getSourceType());
        assertEquals("PROCESS_POOL_REPORT", row.getSourceReportId());
        assertEquals("deviceParameterReadings.pressure.value", row.getSourceFieldCode());
        assertEquals("扩张压力实际值", row.getSourceFieldName());
        assertEquals("SUM", row.getAggregationStrategy());
        assertEquals("1:2", row.getTargetCellKey());
    }

    @Test
    void saveRules_acceptsPqcAggregateFieldForSharedProcessInspectionTargetRouteProcess() {
        stubSharedProcessInspectionSaveContext();
        stubPublishedQaForRouteProcess(5001L, 8001L, "FIRST", "LEAK", "泄漏测试");
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());

        service.saveRules(new BatchRecordCellLinkRulesSaveReqVO()
                .setScopeType("ROUTE_VERSION")
                .setScopeId(3001L)
                .setRouteId(9001L)
                .setRouteProcessId(5001L)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L)
                .setRules(List.of(new BatchRecordCellLinkRuleSaveItemReqVO()
                        .setSourceType("PQC_AGGREGATE_DETAIL")
                        .setSourceReportId("PQC_AGGREGATE_DETAIL")
                        .setSourceRowIndex(-1)
                        .setSourceColumnIndex(-1)
                        .setSourceFieldCode("FIRST|LEAK|1|measuredValue")
                        .setTargetReportId("process-inspection-report")
                        .setTargetRowIndex(1)
                        .setTargetColumnIndex(2))));

        ArgumentCaptor<List<MesProBatchRecordCellLinkRuleDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(ruleMapper).insertBatch(captor.capture());
        MesProBatchRecordCellLinkRuleDO row = captor.getValue().get(0);
        assertEquals(5001L, row.getRouteProcessId());
        assertEquals("PQC_AGGREGATE_DETAIL", row.getSourceType());
        assertEquals("PQC_AGGREGATE_DETAIL", row.getSourceReportId());
        assertEquals("FIRST|LEAK|1|measuredValue", row.getSourceFieldCode());
        assertEquals("首检 / 泄漏测试 / 第1件 / 实测值", row.getSourceFieldName());
        assertEquals("process-inspection-report", row.getTargetReportId());
        assertEquals("1:2", row.getTargetCellKey());
    }

    @Test
    void saveRules_rejectsPqcAggregateFieldFromAnotherRouteProcess() {
        stubSharedProcessInspectionSaveContext();
        stubPublishedQaForRouteProcess(5001L, 8001L, "FIRST", "LEAK", "泄漏测试");

        ServiceException error = assertThrows(ServiceException.class, () -> service.saveRules(
                new BatchRecordCellLinkRulesSaveReqVO()
                        .setScopeType("ROUTE_VERSION")
                        .setScopeId(3001L)
                        .setRouteId(9001L)
                        .setRouteProcessId(5002L)
                        .setBatchRecordDefinitionId(2001L)
                        .setBatchRecordVersionId(3001L)
                        .setRules(List.of(new BatchRecordCellLinkRuleSaveItemReqVO()
                                .setSourceType("PQC_AGGREGATE_DETAIL")
                                .setSourceReportId("PQC_AGGREGATE_DETAIL")
                                .setSourceRowIndex(-1)
                                .setSourceColumnIndex(-1)
                                .setSourceFieldCode("FIRST|LEAK|1|measuredValue")
                                .setTargetReportId("process-inspection-report")
                                .setTargetRowIndex(1)
                                .setTargetColumnIndex(2)))));

        assertEquals(
                MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED.getCode(),
                error.getCode());
        verify(ruleMapper, never()).deleteByScope("ROUTE_VERSION", 3001L);
    }

    @Test
    void saveRules_resolvesProcessParameterFromRequestedRouteWhenReportIsSharedAcrossRoutes() {
        stubProcessPoolSaveContext();
        MesProRouteFlowProcessBatchRecordDO currentRouteBinding =
                routeBinding(5001L, 3001L, "target-report");
        MesProRouteFlowProcessBatchRecordDO otherRouteBinding =
                routeBinding(6001L, 3001L, "target-report").setRouteId(9002L);
        when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIds(any()))
                .thenReturn(List.of(currentRouteBinding, otherRouteBinding));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());

        service.saveRules(processPoolRuleSaveRequest("deviceParameterReadings.pressure.value", "SUM"));

        ArgumentCaptor<List<MesProBatchRecordCellLinkRuleDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(ruleMapper).insertBatch(captor.capture());
        MesProBatchRecordCellLinkRuleDO row = captor.getValue().get(0);
        assertEquals(9001L, row.getRouteId());
        assertEquals("deviceParameterReadings.pressure.value", row.getSourceFieldCode());
        assertEquals("扩张压力实际值", row.getSourceFieldName());
    }

    @Test
    void saveRules_rejectsProcessPoolReportFieldWithoutAggregation() {
        stubProcessPoolSaveContext();

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.saveRules(processPoolRuleSaveRequest("deviceParameterReadings.pressure.value", null)));

        assertEquals(
                MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED.getCode(),
                error.getCode());
        verify(ruleMapper, never()).deleteByScope("ROUTE_VERSION", 3001L);
    }

    @Test
    void saveRules_rejectsUnknownProcessPoolReportField() {
        stubProcessPoolSaveContext();

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.saveRules(processPoolRuleSaveRequest("unknownPressure", "SUM")));

        assertEquals(
                MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED.getCode(),
                error.getCode());
        verify(ruleMapper, never()).deleteByScope("ROUTE_VERSION", 3001L);
    }

    private BatchRecordCellLinkRulesSaveReqVO processPoolRuleSaveRequest(String fieldCode, String aggregationStrategy) {
        return new BatchRecordCellLinkRulesSaveReqVO()
                .setScopeType("ROUTE_VERSION")
                .setScopeId(3001L)
                .setRouteId(9001L)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L)
                .setRules(List.of(new BatchRecordCellLinkRuleSaveItemReqVO()
                        .setSourceType("PROCESS_POOL_REPORT")
                        .setSourceReportId("PROCESS_POOL_REPORT")
                        .setSourceRowIndex(-1)
                        .setSourceColumnIndex(-1)
                        .setSourceFieldCode(fieldCode)
                        .setSourceFieldName("扩张压力")
                        .setTargetReportId("target-report")
                        .setTargetRowIndex(1)
                        .setTargetColumnIndex(2)
                        .setAggregationStrategy(aggregationStrategy)));
    }

    private static void assertProcessPoolSourceField(BatchRecordCellLinkWorkbenchContextRespVO result,
                                                     String fieldCode,
                                                     String fieldName,
                                                     String valueType,
                                                     Long routeProcessId) {
        assertEquals(1, result.getSourceFields().stream()
                .filter(field -> "PROCESS_POOL_REPORT".equals(field.getSourceType()))
                .filter(field -> fieldCode.equals(field.getFieldCode()))
                .filter(field -> fieldName.equals(field.getFieldName()))
                .filter(field -> valueType.equals(field.getValueType()))
                .filter(field -> Objects.equals(routeProcessId, field.getRouteProcessId()))
                .count());
    }

    private static void assertPqcSourceField(BatchRecordCellLinkWorkbenchContextRespVO result,
                                             String fieldCode,
                                             String fieldName,
                                             String valueType,
                                             Long routeProcessId) {
        assertEquals(1, result.getSourceFields().stream()
                .filter(field -> "PQC_AGGREGATE_DETAIL".equals(field.getSourceType()))
                .filter(field -> fieldCode.equals(field.getFieldCode()))
                .filter(field -> fieldName.equals(field.getFieldName()))
                .filter(field -> valueType.equals(field.getValueType()))
                .filter(field -> Objects.equals(routeProcessId, field.getRouteProcessId()))
                .count());
    }

    @Test
    void getWorkbenchContext_resolvesFormTemplateVersionScope() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(1001L, "V3.0")).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 7001L)).thenReturn(List.of());

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(null, null, null, null, 1001L, "V3.0", null);

        assertEquals("FORM_TEMPLATE_VERSION", result.getScopeType());
        assertEquals(7001L, result.getScopeId());
        assertEquals("PRODUCTION_WORK_ORDER", result.getDefaultSourceReportId());
        assertEquals("FORMTPL:7001", result.getDefaultTargetReportId());
        assertEquals("FORMTPL:7001", result.getForms().get(0).getReportId());
        assertEquals("过程检验记录 V3.0", result.getForms().get(0).getReportName());
    }

    @Test
    void getWorkbenchContext_acceptsPqcAggregateSourceForFormTemplateRouteProcess() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(1001L, "V3.0")).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 7001L)).thenReturn(List.of());
        stubPublishedQaForRouteProcess(5001L, 8001L, "FIRST", "LEAK", "泄漏测试");

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, null, null,
                        "PQC_AGGREGATE_DETAIL", 1001L, "V3.0", 5001L);

        assertEquals("FORM_TEMPLATE_VERSION", result.getScopeType());
        assertEquals("PQC_AGGREGATE_DETAIL", result.getDefaultSourceReportId());
        assertEquals("FORMTPL:7001", result.getDefaultTargetReportId());
        assertPqcSourceField(result, "FIRST|LEAK|1|measuredValue",
                "首检 / 泄漏测试 / 第1件 / 实测值", "STRING", 5001L);
    }

    @Test
    void getWorkbenchContext_resolvesDccPqcFieldsWhenRouteProcessNameHasProcessSuffix() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(1001L, "V3.0")).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 7001L)).thenReturn(List.of());
        stubDccPublishedQaForRouteProcess(5002L, "ROUTE-P002", "精洗工序",
                8102L, "ID-QA-002", "精洗", 8001L,
                "FIRST", "FINE-CLEAN", "精洗外观");

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, null, null,
                        "PQC_AGGREGATE_DETAIL", 1001L, "V3.0", 5002L);

        assertPqcSourceField(result, "FIRST|FINE-CLEAN|1|measuredValue",
                "首检 / 精洗外观 / 第1件 / 实测值", "STRING", 5002L);
    }

    @Test
    void getWorkbenchContext_resolvesDccPqcFieldsWhenRomanNumeralStyleDiffers() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(1001L, "V3.0")).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 7001L)).thenReturn(List.of());
        stubDccPublishedQaForRouteProcess(5006L, "ROUTE-P006", "光固Ⅰ工序",
                8106L, "PQC-ID-001-P005", "光固I", 8001L,
                "FIRST", "LIGHT-CURE", "光固强度");

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, null, null,
                        "PQC_AGGREGATE_DETAIL", 1001L, "V3.0", 5006L);

        assertPqcSourceField(result, "FIRST|LIGHT-CURE|1|measuredValue",
                "首检 / 光固强度 / 第1件 / 实测值", "STRING", 5006L);
    }

    @Test
    void getFormCells_resolvesFormTemplateCellsFromJimuSchema() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectById(7001L)).thenReturn(templateVersion);

        BatchRecordCellLinkFormCellsRespVO result = service.getFormCells("FORMTPL:7001", null);

        assertEquals("FORMTPL:7001", result.getReportId());
        assertEquals("过程检验记录 V3.0", result.getReportName());
        assertEquals(1, result.getCells().stream()
                .filter(cell -> "3:1".equals(cell.getCellKey()))
                .filter(cell -> Boolean.TRUE.equals(cell.getLinkableAsTarget()))
                .count());
    }

    @Test
    void getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema() {
        FormTemplateVersionDO templateVersion = formTemplateVersion()
                .setId(32L)
                .setJimuSchemaJson("""
                        {
                          "cols":{"0":{"width":140},"1":{"width":220}},
                          "rows":{"3":{"height":36,"cells":{"0":{"text":"生产批号"},"1":{"text":""}}}},
                          "cellRules":[{"rowIndex":3,"columnIndex":1,"label":"生产批号","valueType":"STRING","componentFlag":"input-text","required":true,"reviewed":true,"source":"MANUAL"}]
                        }
                        """);
        when(templateVersionMapper.selectById(32L)).thenReturn(templateVersion);

        BatchRecordCellLinkFormCellsRespVO result = service.getFormCells("FORMTPL:32", null);

        assertEquals("FORMTPL:32", result.getReportId());
        assertEquals(1, result.getCells().stream()
                .filter(cell -> "3:1".equals(cell.getCellKey()))
                .filter(cell -> Boolean.TRUE.equals(cell.getLinkableAsTarget()))
                .count());
    }

    @Test
    void getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing() {
        FormTemplateVersionDO templateVersion = formTemplateVersion()
                .setId(32L)
                .setJimuSchemaJson(null)
                .setRecognizedSchemaJson("""
                        [
                          {"fieldCode":"no","label":"NO.","fieldType":"input","required":false},
                          {"fieldCode":"field3","label":"生产批号","fieldType":"input","required":false}
                        ]
                        """);
        when(templateVersionMapper.selectById(32L)).thenReturn(templateVersion);

        BatchRecordCellLinkFormCellsRespVO result = service.getFormCells("FORMTPL:32", null);

        assertEquals("FORMTPL:32", result.getReportId());
        assertEquals(1, result.getCells().stream()
                .filter(cell -> "3:3".equals(cell.getCellKey()))
                .filter(cell -> "生产批号".equals(cell.getLabel()))
                .filter(cell -> Boolean.TRUE.equals(cell.getLinkableAsTarget()))
                .count());
    }

    @Test
    void getFormCells_fallsBackToRecognizedFieldsWhenJimuSchemaHasNoSheetLayout() {
        FormTemplateVersionDO templateVersion = formTemplateVersion()
                .setId(32L)
                .setJimuSchemaJson("""
                        {
                          "cellRules":[{"rowIndex":3,"columnIndex":1,"label":"旧模板元信息","valueType":"STRING"}]
                        }
                        """)
                .setRecognizedSchemaJson("""
                        [
                          {"fieldCode":"no","label":"NO.","fieldType":"input","required":false},
                          {"fieldCode":"field3","label":"生产批号","fieldType":"input","required":false}
                        ]
                        """);
        when(templateVersionMapper.selectById(32L)).thenReturn(templateVersion);

        BatchRecordCellLinkFormCellsRespVO result = service.getFormCells("FORMTPL:32", null);

        assertEquals("FORMTPL:32", result.getReportId());
        assertEquals(1, result.getCells().stream()
                .filter(cell -> "3:3".equals(cell.getCellKey()))
                .filter(cell -> "生产批号".equals(cell.getLabel()))
                .filter(cell -> Boolean.TRUE.equals(cell.getLinkableAsTarget()))
                .count());
    }

    @Test
    void getFormCells_usesRecognizedFieldsWhenJimuLayoutHasNoCellRules() {
        FormTemplateVersionDO templateVersion = formTemplateVersion()
                .setId(32L)
                .setJimuSchemaJson("""
                        {
                          "sheetLayoutJson":"{\\\"cols\\\":{\\\"0\\\":{\\\"width\\\":140},\\\"1\\\":{\\\"width\\\":220}},\\\"rows\\\":{\\\"3\\\":{\\\"height\\\":36,\\\"cells\\\":{\\\"0\\\":{\\\"text\\\":\\\"生产批号\\\"},\\\"1\\\":{\\\"text\\\":\\\"\\\"}}}}}"
                        }
                        """)
                .setRecognizedSchemaJson("""
                        [
                          {"fieldCode":"no","label":"NO.","fieldType":"input","required":false},
                          {"fieldCode":"field3","label":"生产批号","fieldType":"input","required":false}
                        ]
                        """);
        when(templateVersionMapper.selectById(32L)).thenReturn(templateVersion);

        BatchRecordCellLinkFormCellsRespVO result = service.getFormCells("FORMTPL:32", null);

        assertEquals("FORMTPL:32", result.getReportId());
        assertEquals(1, result.getCells().stream()
                .filter(cell -> "3:3".equals(cell.getCellKey()))
                .filter(cell -> "生产批号".equals(cell.getLabel()))
                .filter(cell -> Boolean.TRUE.equals(cell.getLinkableAsTarget()))
                .count());
    }

    @Test
    void saveRules_acceptsPqcAggregateSourceForFormTemplateVersionRouteProcess() {
        FormTemplateVersionDO templateVersion = formTemplateVersion().setId(32L);
        when(templateVersionMapper.selectById(32L)).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 32L)).thenReturn(List.of());
        stubPublishedQaForRouteProcess(5001L, 8001L, "FIRST", "LEAK", "泄漏测试");

        service.saveRules(new BatchRecordCellLinkRulesSaveReqVO()
                .setScopeType("FORM_TEMPLATE_VERSION")
                .setScopeId(32L)
                .setRouteId(9001L)
                .setRouteProcessId(5001L)
                .setRules(List.of(new BatchRecordCellLinkRuleSaveItemReqVO()
                        .setSourceType("PQC_AGGREGATE_DETAIL")
                        .setSourceReportId("PQC_AGGREGATE_DETAIL")
                        .setSourceRowIndex(-1)
                        .setSourceColumnIndex(-1)
                        .setSourceFieldCode("FIRST|LEAK|1|measuredValue")
                        .setTargetReportId("FORMTPL:32")
                        .setTargetRowIndex(3)
                        .setTargetColumnIndex(1))));

        ArgumentCaptor<List<MesProBatchRecordCellLinkRuleDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(ruleMapper).insertBatch(captor.capture());
        MesProBatchRecordCellLinkRuleDO row = captor.getValue().get(0);
        assertEquals("FORM_TEMPLATE_VERSION", row.getScopeType());
        assertEquals(32L, row.getScopeId());
        assertEquals(9001L, row.getRouteId());
        assertEquals(5001L, row.getRouteProcessId());
        assertEquals("PQC_AGGREGATE_DETAIL", row.getSourceType());
        assertEquals("PQC_AGGREGATE_DETAIL", row.getSourceReportId());
        assertEquals("FIRST|LEAK|1|measuredValue", row.getSourceCellKey());
        assertEquals("FIRST|LEAK|1|measuredValue", row.getSourceFieldCode());
        assertEquals("首检 / 泄漏测试 / 第1件 / 实测值", row.getSourceFieldName());
        assertEquals("FORMTPL:32", row.getTargetReportId());
        assertEquals("3:1", row.getTargetCellKey());
    }

    @Test
    void saveRules_acceptsProductionLossSourceForFormTemplateVersionScope() {
        FormTemplateVersionDO templateVersion = formTemplateVersion().setId(27L);
        when(templateVersionMapper.selectById(27L)).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 27L)).thenReturn(List.of());

        service.saveRules(new BatchRecordCellLinkRulesSaveReqVO()
                .setScopeType("FORM_TEMPLATE_VERSION")
                .setScopeId(27L)
                .setRules(List.of(new BatchRecordCellLinkRuleSaveItemReqVO()
                        .setSourceType("PRODUCTION_LOSS")
                        .setSourceReportId("PRODUCTION_LOSS")
                        .setSourceRowIndex(-1)
                        .setSourceColumnIndex(-1)
                        .setSourceFieldCode("lossSummary")
                        .setSourceFieldName("损耗汇总")
                        .setTargetReportId("FORMTPL:27")
                        .setTargetRowIndex(3)
                        .setTargetColumnIndex(1))));

        ArgumentCaptor<List<MesProBatchRecordCellLinkRuleDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(ruleMapper).insertBatch(captor.capture());
        MesProBatchRecordCellLinkRuleDO row = captor.getValue().get(0);
        assertEquals("FORM_TEMPLATE_VERSION", row.getScopeType());
        assertEquals(27L, row.getScopeId());
        assertEquals("PRODUCTION_LOSS", row.getSourceType());
        assertEquals("PRODUCTION_LOSS", row.getSourceReportId());
        assertEquals("SUMMARY|lossSummary", row.getSourceCellKey());
        assertEquals("lossSummary", row.getSourceFieldCode());
        assertEquals("损耗汇总", row.getSourceFieldName());
        assertEquals("FORMTPL:27", row.getTargetReportId());
        assertEquals("3:1", row.getTargetCellKey());
    }

    @Test
    void getPrefill_resolvesProductionWorkOrderFieldWithoutSourceExecution() {
        MesProBatchRecordExecutionDO targetExecution = new MesProBatchRecordExecutionDO()
                .setId(9001L)
                .setWorkOrderId(1001L)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L)
                .setBatchRecordReportId("target-report")
                .setBatchCode("BATCH-001")
                .setCellValuesJson("[]");
        MesProBatchRecordReportDO targetReport = new MesProBatchRecordReportDO()
                .setReportId("target-report")
                .setReportName("批次执行记录")
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L);
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO()
                .setId(11L)
                .setScopeType("ROUTE_VERSION")
                .setScopeId(3001L)
                .setSourceType("PRODUCTION_WORK_ORDER")
                .setSourceReportId("PRODUCTION_WORK_ORDER")
                .setSourceReportName("生产工单")
                .setSourceFieldCode("quantity")
                .setSourceFieldName("生产数量")
                .setSourceCellKey("quantity")
                .setSourceLabel("生产数量")
                .setSourceValueType("NUMBER")
                .setTargetReportId("target-report")
                .setTargetReportName("批次执行记录")
                .setTargetRowIndex(1)
                .setTargetColumnIndex(2)
                .setTargetCellKey("1:2")
                .setOverwritePolicy("ONLY_WHEN_EMPTY")
                .setRuleVersion(7L);
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(1001L)
                .code("MO-001")
                .quantity(new BigDecimal("128.5"))
                .build();

        when(executionMapper.selectById(9001L)).thenReturn(targetExecution);
        when(reportMapper.selectByReportId("target-report")).thenReturn(targetReport);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 3001L, "target-report"))
                .thenReturn(List.of(rule));
        when(workOrderMapper.selectById(1001L)).thenReturn(workOrder);

        BatchRecordCellLinkPrefillRespVO result = service.getPrefill(9001L, null);

        assertEquals(1, result.getPrefills().size());
        assertEquals(new BigDecimal("128.5"), result.getPrefills().get(0).getValue());
        assertEquals("PRODUCTION_WORK_ORDER", result.getPrefills().get(0).getSourceType());
        assertEquals("quantity", result.getPrefills().get(0).getSourceFieldCode());
        assertEquals("生产数量", result.getPrefills().get(0).getSourceFieldName());
        assertEquals(0, result.getConflicts().size());
    }

    @Test
    void getPrefill_skipsProcessPoolReportRulesBecauseTeamLeaderBackfillOwnsThem() {
        MesProBatchRecordExecutionDO targetExecution = new MesProBatchRecordExecutionDO()
                .setId(9011L)
                .setWorkOrderId(1001L)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L)
                .setBatchRecordReportId("target-report")
                .setBatchCode("BATCH-001")
                .setCellValuesJson("[]");
        MesProBatchRecordReportDO targetReport = new MesProBatchRecordReportDO()
                .setReportId("target-report")
                .setReportName("批次执行记录")
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L);
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO()
                .setId(31L)
                .setScopeType("ROUTE_VERSION")
                .setScopeId(3001L)
                .setSourceType("PROCESS_POOL_REPORT")
                .setSourceFieldCode("TLW-20260731-PRESSURE")
                .setSourceFieldName("压力")
                .setTargetReportId("target-report")
                .setTargetReportName("批次执行记录")
                .setTargetRowIndex(11)
                .setTargetColumnIndex(14)
                .setTargetCellKey("11:14")
                .setOverwritePolicy("ONLY_WHEN_EMPTY")
                .setRuleVersion(7L);

        when(executionMapper.selectById(9011L)).thenReturn(targetExecution);
        when(reportMapper.selectByReportId("target-report")).thenReturn(targetReport);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 3001L, "target-report"))
                .thenReturn(List.of(rule));

        BatchRecordCellLinkPrefillRespVO result = service.getPrefill(9011L, null);

        assertEquals(0, result.getPrefills().size());
        assertEquals(0, result.getConflicts().size());
    }

    @Test
    void getPrefill_resolvesProductionBatchCodeFromExecutionContextWhenWorkOrderBatchCodeEmpty() {
        MesProBatchRecordExecutionDO targetExecution = new MesProBatchRecordExecutionDO()
                .setId(9002L)
                .setWorkOrderId(1002L)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L)
                .setBatchRecordReportId("target-report")
                .setBatchCode("BATCH-FROM-EXECUTION")
                .setCellValuesJson("[]");
        MesProBatchRecordReportDO targetReport = new MesProBatchRecordReportDO()
                .setReportId("target-report")
                .setReportName("批次执行记录")
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L);
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO()
                .setId(16L)
                .setScopeType("ROUTE_VERSION")
                .setScopeId(3001L)
                .setSourceType("PRODUCTION_WORK_ORDER")
                .setSourceReportId("PRODUCTION_WORK_ORDER")
                .setSourceReportName("生产工单")
                .setSourceFieldCode("batchCode")
                .setSourceFieldName("生产批号")
                .setSourceCellKey("batchCode")
                .setSourceLabel("生产批号")
                .setSourceValueType("STRING")
                .setTargetReportId("target-report")
                .setTargetReportName("批次执行记录")
                .setTargetRowIndex(4)
                .setTargetColumnIndex(1)
                .setTargetCellKey("4:1")
                .setOverwritePolicy("ONLY_WHEN_EMPTY")
                .setRuleVersion(7L);
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(1002L)
                .code("MO-002")
                .batchCode(null)
                .build();

        when(executionMapper.selectById(9002L)).thenReturn(targetExecution);
        when(reportMapper.selectByReportId("target-report")).thenReturn(targetReport);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 3001L, "target-report"))
                .thenReturn(List.of(rule));
        when(workOrderMapper.selectById(1002L)).thenReturn(workOrder);

        BatchRecordCellLinkPrefillRespVO result = service.getPrefill(9002L, null);

        assertEquals(1, result.getPrefills().size());
        assertEquals("BATCH-FROM-EXECUTION", result.getPrefills().get(0).getValue());
        assertEquals("batchCode", result.getPrefills().get(0).getSourceFieldCode());
        assertEquals("4:1", result.getPrefills().get(0).getTargetCellKey());
        assertEquals(0, result.getConflicts().size());
    }

    @Test
    void buildFormTemplateVersionPrefillData_resolvesProductionBatchCodeFromExecutionContext() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO()
                .setId(21L)
                .setScopeType("FORM_TEMPLATE_VERSION")
                .setScopeId(7001L)
                .setSourceType("PRODUCTION_WORK_ORDER")
                .setSourceReportId("PRODUCTION_WORK_ORDER")
                .setSourceReportName("生产工单")
                .setSourceFieldCode("batchCode")
                .setSourceFieldName("生产批号")
                .setSourceCellKey("batchCode")
                .setTargetReportId("FORMTPL:7001")
                .setTargetReportName("过程检验记录 V3.0")
                .setTargetRowIndex(3)
                .setTargetColumnIndex(1)
                .setTargetCellKey("3:1")
                .setOverwritePolicy("ONLY_WHEN_EMPTY");
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(1002L)
                .code("MO-002")
                .batchCode(null)
                .build();
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("batchTaskId", 3002L);

        when(templateVersionMapper.selectById(7001L)).thenReturn(templateVersion);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("FORM_TEMPLATE_VERSION", 7001L, "FORMTPL:7001"))
                .thenReturn(List.of(rule));
        when(workOrderMapper.selectById(1002L)).thenReturn(workOrder);

        Map<String, Object> result = service.buildFormTemplateVersionPrefillData(
                7001L, 1002L, "BATCH-FROM-EXECUTION", formData);

        assertEquals("BATCH-FROM-EXECUTION", result.get("productionBatchCode"));
        assertFalse(result.containsKey("3:1"));
        assertEquals(3002L, result.get("batchTaskId"));
    }

    @Test
    void buildFormTemplateVersionPrefillData_appliesWorkOrderRuleAndSkipsReleaseWriterRules() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        MesProBatchRecordCellLinkRuleDO workOrderRule = formTemplateRule(
                31L, "PRODUCTION_WORK_ORDER", "batchCode", 3, 1);
        MesProBatchRecordCellLinkRuleDO processInspectionRule = formTemplateRule(
                32L, "PQC_AGGREGATE_DETAIL", "dccProjectCode", 3, 3);
        MesProBatchRecordCellLinkRuleDO lossRule = formTemplateRule(
                33L, "PRODUCTION_LOSS", "productLabel", 4, 1);
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(1002L)
                .code("MO-002")
                .build();

        when(templateVersionMapper.selectById(7001L)).thenReturn(templateVersion);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("FORM_TEMPLATE_VERSION", 7001L, "FORMTPL:7001"))
                .thenReturn(List.of(workOrderRule, processInspectionRule, lossRule));
        when(workOrderMapper.selectById(1002L)).thenReturn(workOrder);

        Map<String, Object> result = service.buildFormTemplateVersionPrefillData(
                7001L, 1002L, "BATCH-FROM-EXECUTION", Map.of("existing", "kept"));

        assertEquals("BATCH-FROM-EXECUTION", result.get("productionBatchCode"));
        assertEquals("kept", result.get("existing"));
        assertEquals(2, result.size());
    }

    @Test
    void buildFormTemplateVersionPrefillData_skipsKnownReleaseWriterRulesWithoutWorkOrderContext() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectById(7001L)).thenReturn(templateVersion);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("FORM_TEMPLATE_VERSION", 7001L, "FORMTPL:7001"))
                .thenReturn(List.of(
                        formTemplateRule(41L, "PQC_AGGREGATE_DETAIL", "dccProjectCode", 3, 3),
                        formTemplateRule(42L, "PRODUCTION_LOSS", "productLabel", 4, 1)));

        Map<String, Object> result = service.buildFormTemplateVersionPrefillData(
                7001L, null, null, Map.of("existing", "kept"));

        assertEquals(Map.of("existing", "kept"), result);
        verify(workOrderMapper, never()).selectById(any());
    }

    @Test
    void buildFormTemplateVersionPrefillData_failsFastForUnknownSourceType() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectById(7001L)).thenReturn(templateVersion);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("FORM_TEMPLATE_VERSION", 7001L, "FORMTPL:7001"))
                .thenReturn(List.of(formTemplateRule(51L, "UNSUPPORTED_DYNAMIC", "field", 3, 3)));

        assertThrows(ServiceException.class, () -> service.buildFormTemplateVersionPrefillData(
                7001L, null, null, Map.of()));
        verify(workOrderMapper, never()).selectById(any());
    }


    @Test
    void saveRepeatRowGroup_persistsConfirmedProcessPoolMappingsWithoutExecutionSideEffects() {
        stubProcessPoolSaveContext();

        BatchRecordRepeatRowGroupSaveRespVO result = service.saveRepeatRowGroup(new BatchRecordRepeatRowGroupSaveReqVO()
                .setScopeType("ROUTE_VERSION")
                .setScopeId(3001L)
                .setRouteId(9001L)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L)
                .setRouteProcessId(5001L)
                .setTargetReportId("target-report")
                .setTemplateStartRowIndex(10)
                .setTemplateEndRowIndex(10)
                .setRepeatAreaStartRowIndex(10)
                .setRepeatAreaEndRowIndex(13)
                .setRecords(List.of(
                        new BatchRecordRepeatRowGroupRecordSaveReqVO().setRecordSequence(1).setStartRowIndex(10).setEndRowIndex(10),
                        new BatchRecordRepeatRowGroupRecordSaveReqVO().setRecordSequence(2).setStartRowIndex(11).setEndRowIndex(11),
                        new BatchRecordRepeatRowGroupRecordSaveReqVO().setRecordSequence(3).setStartRowIndex(12).setEndRowIndex(12)))
                .setMappings(List.of(new BatchRecordRepeatRowGroupMappingSaveReqVO()
                        .setSourceType("PROCESS_POOL_REPORT")
                        .setSourceFieldCode("deviceParameterReadings.pressure.value")
                        .setSourceFieldName("扩张压力实际值")
                        .setSourceValueType("NUMBER")
                        .setTemplateTargetRowIndex(10)
                        .setTemplateTargetColumnIndex(2)
                        .setTemplateTargetCellKey("10:2")
                        .setTargetValueType("NUMBER"))));

        ArgumentCaptor<MesProBatchRecordRepeatRowGroupDO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordRepeatRowGroupDO.class);
        verify(repeatRowGroupMapper).deleteEnabledByScopeAndTargetReport("ROUTE_VERSION", 3001L, "target-report");
        verify(repeatRowGroupMapper).insert((MesProBatchRecordRepeatRowGroupDO) captor.capture());
        verify(executionMapper, never()).insert(any(MesProBatchRecordExecutionDO.class));
        assertEquals(3, result.getRecords().size());
        assertEquals(1, result.getMappings().size());
        assertEquals(5001L, captor.getValue().getRouteProcessId());
        assertEquals("PROCESS_POOL_REPORT", captor.getValue().getSourceType());
        assertEquals(10, captor.getValue().getTemplateStartRowIndex());
        assertEquals(13, captor.getValue().getRepeatAreaEndRowIndex());
    }
    private MesProBatchRecordCellLinkRuleDO formTemplateRule(long id, String sourceType, String fieldCode,
                                                             int rowIndex, int columnIndex) {
        boolean workOrder = "PRODUCTION_WORK_ORDER".equals(sourceType);
        return new MesProBatchRecordCellLinkRuleDO()
                .setId(id)
                .setScopeType("FORM_TEMPLATE_VERSION")
                .setScopeId(7001L)
                .setSourceType(sourceType)
                .setSourceReportId(sourceType)
                .setSourceReportName(sourceType)
                .setSourceFieldCode(fieldCode)
                .setSourceFieldName(fieldCode)
                .setSourceCellKey(workOrder ? fieldCode : "SUMMARY|" + fieldCode)
                .setTargetReportId("FORMTPL:7001")
                .setTargetReportName("过程检验记录 V3.0")
                .setTargetRowIndex(rowIndex)
                .setTargetColumnIndex(columnIndex)
                .setTargetCellKey(rowIndex + ":" + columnIndex)
                .setOverwritePolicy("ONLY_WHEN_EMPTY");
    }

    private FormTemplateVersionDO formTemplateVersion() {
        return FormTemplateVersionDO.builder()
                .id(7001L)
                .templateId(1001L)
                .templateName("过程检验记录")
                .versionNo("V3.0")
                .recognizedSchemaJson("""
                        [{"fieldCode":"productionBatchCode","label":"生产批号","fieldType":"input","required":true}]
                        """)
                .jimuSchemaJson("""
                        {
                          "sheetLayoutJson":"{\\\"cols\\\":{\\\"0\\\":{\\\"width\\\":140},\\\"1\\\":{\\\"width\\\":220}},\\\"rows\\\":{\\\"3\\\":{\\\"height\\\":36,\\\"cells\\\":{\\\"0\\\":{\\\"text\\\":\\\"生产批号\\\"},\\\"1\\\":{\\\"text\\\":\\\"\\\"}}}}}",
                          "cellRules":[{"rowIndex":3,"columnIndex":1,"label":"生产批号","valueType":"STRING","componentFlag":"input-text","required":true,"reviewed":true,"source":"MANUAL"}]
                        }
                        """)
                .build();
    }

    private MesProBatchRecordReportDO report(String reportId, String reportName, Long definitionId, Long versionId) {
        return new MesProBatchRecordReportDO()
                .setId(801L)
                .setReportId(reportId)
                .setReportName(reportName)
                .setBatchRecordDefinitionId(definitionId)
                .setBatchRecordVersionId(versionId);
    }

    private BatchRecordReportCellRulesRespVO targetCellRules() {
        return new BatchRecordReportCellRulesRespVO()
                .setReportId("target-report")
                .setSheetLayoutJson("""
                        {"cols":{"0":{"width":120},"1":{"width":120},"2":{"width":120}},
                         "rows":{
                           "1":{"height":32,"cells":{"0":{"text":"压力"},"2":{"text":"","fillForm":{"field":"pressure"}}}},
                           "10":{"height":32,"cells":{"0":{"text":"第1次"},"2":{"text":"","fillForm":{"field":"pressure"}}}},
                           "11":{"height":32,"cells":{"0":{"text":"第2次"},"2":{"text":"","fillForm":{"field":"pressure"}}}},
                           "12":{"height":32,"cells":{"0":{"text":"第3次"},"2":{"text":"","fillForm":{"field":"pressure"}}}}
                         }}
                        """)
                .setRules(List.of(
                        repeatRowCellRule(1),
                        repeatRowCellRule(10),
                        repeatRowCellRule(11),
                        repeatRowCellRule(12)));
    }

    private BatchRecordReportCellRuleVO repeatRowCellRule(int rowIndex) {
        return new BatchRecordReportCellRuleVO()
                .setRowIndex(rowIndex)
                .setColumnIndex(2)
                .setLabel("扩张压力")
                .setValueType("NUMBER")
                .setComponentFlag("input-number")
                .setReviewed(true);
    }

    private MesProRouteFlowProcessBatchRecordDO routeBinding(Long routeProcessId, Long versionId, String reportId) {
        return new MesProRouteFlowProcessBatchRecordDO()
                .setId(routeProcessId + 100L)
                .setRouteId(9001L)
                .setRouteProcessId(routeProcessId)
                .setRecordCategory("BATCH_RECORD")
                .setFormSlotType("MAIN")
                .setBatchRecordVersionId(versionId)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordReportId(reportId)
                .setUseType("BATCH");
    }

    private void stubProcessPoolSaveContext() {
        MesProBatchRecordReportDO targetReport = report("target-report", "粗洗工序生产记录", 2001L, 3001L);
        lenient().when(reportMapper.selectByReportId("target-report")).thenReturn(targetReport);
        lenient().when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L))
                .thenReturn(List.of(targetReport));
        lenient().when(reportService.getCellRules("target-report")).thenReturn(targetCellRules());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIds(any())).thenReturn(
                List.of(routeBinding(5001L, 3001L, "target-report")));
        lenient().when(deviceParameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(11L, 5001L, "pressure", "扩张压力",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL)));
    }

    private void stubSharedProcessInspectionSaveContext() {
        MesProBatchRecordReportDO targetReport =
                report("process-inspection-report", "球囊扩张压力泵过程检验记录", 2001L, 3001L);
        lenient().when(reportMapper.selectByReportId("process-inspection-report")).thenReturn(targetReport);
        lenient().when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L))
                .thenReturn(List.of(targetReport));
        lenient().when(reportService.getCellRules("process-inspection-report")).thenReturn(targetCellRules());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIds(any()))
                .thenReturn(List.of());
    }

    private void stubPublishedQaForRouteProcess(Long routeProcessId, Long versionId,
                                                String inspectionType, String itemCode, String itemName) {
        stubPublishedQaForRouteProcess(routeProcessId, 3001L, versionId, inspectionType, itemCode, itemName);
    }

    private void stubPublishedQaForRouteProcess(Long routeProcessId, Long routeVersionId, Long versionId,
                                                String inspectionType, String itemCode, String itemName) {
        lenient().when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(
                MesProRouteVersionDO.builder()
                        .id(routeVersionId)
                        .routeId(9001L)
                        .active(true)
                        .lifecycleStatus("ACTIVE")
                        .build());
        lenient().when(qaRegulationMapper.selectList(any())).thenReturn(List.of(
                MesQaInspectionRegulationDO.builder()
                        .id(7001L)
                        .routeId(9001L)
                        .routeVersionId(routeVersionId)
                        .routeProcessId(routeProcessId)
                        .lifecycleStatus("PUBLISHED")
                        .currentVersionId(versionId)
                        .build()));
        lenient().when(qaRegulationVersionMapper.selectById(versionId)).thenReturn(
                MesQaInspectionRegulationVersionDO.builder()
                        .id(versionId)
                        .regulationId(7001L)
                        .versionNo("V1")
                        .lifecycleStatus("PUBLISHED")
                        .build());
        lenient().when(qaRegulationItemMapper.selectListByVersionId(versionId)).thenReturn(List.of(
                MesQaInspectionRegulationItemDO.builder()
                        .id(9001L)
                        .regulationVersionId(versionId)
                        .inspectionType(inspectionType)
                        .itemCode(itemCode)
                        .itemName(itemName)
                        .inspectionMethod("目视")
                        .standardText("符合要求")
                        .resultType("TEXT")
                        .firstInspectionQuantity(1)
                        .build()));
    }

    private void stubDccPublishedQaForRouteProcess(Long routeProcessId, String routeProcessCode, String routeProcessName,
                                                   Long qaProcessId, String qaProcessCode, String qaProcessName,
                                                   Long versionId, String inspectionType, String itemCode, String itemName) {
        Long processId = routeProcessId + 10000L;
        lenient().when(routeDccProjectBindingMapper.selectCurrentByRouteId(9001L)).thenReturn(
                MesRouteDccProjectBindingDO.builder()
                        .id(6101L)
                        .routeId(9001L)
                        .dccProjectCodeId(147L)
                        .build());
        lenient().when(qaRegulationMapper.selectByDccProjectCodeId(147L)).thenReturn(
                MesQaInspectionRegulationDO.builder()
                        .id(7001L)
                        .currentVersionId(versionId)
                        .lifecycleStatus("PUBLISHED")
                        .build());
        lenient().when(qaRegulationVersionMapper.selectById(versionId)).thenReturn(
                MesQaInspectionRegulationVersionDO.builder()
                        .id(versionId)
                        .regulationId(7001L)
                        .versionNo("B/1")
                        .lifecycleStatus("PUBLISHED")
                        .build());
        lenient().when(routeProcessMapper.selectByIdIgnoreDeleted(routeProcessId)).thenReturn(
                MesProRouteProcessDO.builder()
                        .id(routeProcessId)
                        .routeId(9001L)
                        .processId(processId)
                        .build());
        lenient().when(processMapper.selectByIdIgnoreDeleted(processId)).thenReturn(
                MesProProcessDO.builder()
                        .id(processId)
                        .code(routeProcessCode)
                        .name(routeProcessName)
                        .build());
        lenient().when(qaRegulationProcessMapper.selectListByVersionId(versionId)).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(qaProcessId)
                        .regulationVersionId(versionId)
                        .processCode(qaProcessCode)
                        .processName(qaProcessName)
                        .build()));
        lenient().when(qaRegulationItemMapper.selectListByVersionId(versionId)).thenReturn(List.of(
                MesQaInspectionRegulationItemDO.builder()
                        .id(9001L)
                        .regulationVersionId(versionId)
                        .qaProcessId(qaProcessId)
                        .inspectionType(inspectionType)
                        .itemCode(itemCode)
                        .itemName(itemName)
                        .inspectionMethod("目视")
                        .standardText("符合要求")
                        .resultType("TEXT")
                        .firstInspectionQuantity(1)
                        .build()));
    }

    private MesProcessPoolDeviceParameterRuleDO parameterRule(Long id, Long routeProcessId,
                                                              String parameterCode, String parameterName,
                                                              String valueType) {
        return new MesProcessPoolDeviceParameterRuleDO()
                .setId(id)
                .setRouteProcessId(routeProcessId)
                .setParameterCode(parameterCode)
                .setParameterName(parameterName)
                .setValueType(valueType)
                .setEnabled(true);
    }
}
