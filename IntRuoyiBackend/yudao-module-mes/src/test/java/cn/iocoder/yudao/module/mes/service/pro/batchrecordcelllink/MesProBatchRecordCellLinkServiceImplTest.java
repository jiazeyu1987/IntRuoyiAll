package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormCellsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @InjectMocks
    private MesProBatchRecordCellLinkServiceImpl service;

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
}
