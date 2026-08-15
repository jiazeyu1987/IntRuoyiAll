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
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordRepeatRowGroupMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import org.junit.jupiter.api.Test;
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
    private MesProcessPoolDeviceParameterRuleMapper deviceParameterRuleMapper;
    @Mock
    private MesProBatchRecordRepeatRowGroupMapper repeatRowGroupMapper;

    @InjectMocks
    private MesProBatchRecordCellLinkServiceImpl service;

    @Test
    void getWorkbenchContext_exposesProcessPoolReportFieldsFromRouteParameters() {
        MesProBatchRecordReportDO targetReport = report("target-report", "粗洗工序生产记录", 2001L, 3001L);
        when(reportMapper.selectListByDefinitionIdAndVersionId(2001L, 3001L)).thenReturn(List.of(targetReport));
        when(ruleMapper.selectListByScope("ROUTE_VERSION", 3001L)).thenReturn(List.of());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordVersionId(3001L)).thenReturn(List.of(
                routeBinding(5001L, 3001L, "target-report"),
                routeBinding(5002L, 3001L, "target-report")));
        lenient().when(deviceParameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(11L, 5001L, "pressure", "扩张压力",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL),
                parameterRule(12L, 5002L, "holdTime", "保压时间",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER)));

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(9001L, 2001L, 3001L, null, null, null);

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

    @Test
    void getWorkbenchContext_resolvesFormTemplateVersionScope() {
        FormTemplateVersionDO templateVersion = formTemplateVersion();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(1001L, "V3.0")).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 7001L)).thenReturn(List.of());

        BatchRecordCellLinkWorkbenchContextRespVO result =
                service.getWorkbenchContext(null, null, null, null, 1001L, "V3.0");

        assertEquals("FORM_TEMPLATE_VERSION", result.getScopeType());
        assertEquals(7001L, result.getScopeId());
        assertEquals("PRODUCTION_WORK_ORDER", result.getDefaultSourceReportId());
        assertEquals("FORMTPL:7001", result.getDefaultTargetReportId());
        assertEquals("FORMTPL:7001", result.getForms().get(0).getReportId());
        assertEquals("过程检验记录 V3.0", result.getForms().get(0).getReportName());
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
    void saveRules_acceptsFormalDynamicSourceForFormTemplateVersionScope() {
        FormTemplateVersionDO templateVersion = formTemplateVersion().setId(32L);
        when(templateVersionMapper.selectById(32L)).thenReturn(templateVersion);
        when(ruleMapper.selectListByScope("FORM_TEMPLATE_VERSION", 32L)).thenReturn(List.of());

        service.saveRules(new BatchRecordCellLinkRulesSaveReqVO()
                .setScopeType("FORM_TEMPLATE_VERSION")
                .setScopeId(32L)
                .setRules(List.of(new BatchRecordCellLinkRuleSaveItemReqVO()
                        .setSourceType("PQC_AGGREGATE_DETAIL")
                        .setSourceReportId("PQC_AGGREGATE_DETAIL")
                        .setSourceRowIndex(-1)
                        .setSourceColumnIndex(-1)
                        .setSourceFieldCode("inspectionSummary")
                        .setSourceFieldName("过程检验汇总")
                        .setTargetReportId("FORMTPL:32")
                        .setTargetRowIndex(3)
                        .setTargetColumnIndex(1))));

        ArgumentCaptor<List<MesProBatchRecordCellLinkRuleDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(ruleMapper).insertBatch(captor.capture());
        MesProBatchRecordCellLinkRuleDO row = captor.getValue().get(0);
        assertEquals("FORM_TEMPLATE_VERSION", row.getScopeType());
        assertEquals(32L, row.getScopeId());
        assertEquals("PQC_AGGREGATE_DETAIL", row.getSourceType());
        assertEquals("PQC_AGGREGATE_DETAIL", row.getSourceReportId());
        assertEquals("SUMMARY|inspectionSummary", row.getSourceCellKey());
        assertEquals("inspectionSummary", row.getSourceFieldCode());
        assertEquals("过程检验汇总", row.getSourceFieldName());
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
                .setBatchRecordReportId(reportId)
                .setUseType("BATCH");
    }

    private void stubProcessPoolSaveContext() {
        MesProBatchRecordReportDO targetReport = report("target-report", "粗洗工序生产记录", 2001L, 3001L);
        lenient().when(reportMapper.selectByReportId("target-report")).thenReturn(targetReport);
        lenient().when(reportService.getCellRules("target-report")).thenReturn(targetCellRules());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByBatchRecordVersionId(3001L)).thenReturn(
                List.of(routeBinding(5001L, 3001L, "target-report")));
        lenient().when(deviceParameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(11L, 5001L, "pressure", "扩张压力",
                        MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL)));
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
