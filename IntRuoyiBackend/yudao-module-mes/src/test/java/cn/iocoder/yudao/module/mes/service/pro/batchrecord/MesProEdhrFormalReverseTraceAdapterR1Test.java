package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionPublishProjectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Fixtures follow snapshot field extraction and buildAfterCellValuesJson in the formal writers. */
class MesProEdhrFormalReverseTraceAdapterR1Test {
    private final MesProBatchRecordExecutionMapper executions = mock(MesProBatchRecordExecutionMapper.class);
    private final MesProEdhrBatchExecutionTaskMapper tasks = mock(MesProEdhrBatchExecutionTaskMapper.class);
    private final FormActionInstanceMapper instances = mock(FormActionInstanceMapper.class);
    private final MesProBatchRecordExecutionFieldAuditItemMapper audits = mock(MesProBatchRecordExecutionFieldAuditItemMapper.class);
    private final MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
            .setId(9001L).setTenantId(1L).setRouteVersionId(2002L);
    private final MesProEdhrFormalReverseTraceAdapter adapter = new MesProEdhrFormalReverseTraceAdapter(
            Category.FIELD, executions, null, null, tasks, instances, audits,
            null, null, null, null, null, null);
    private MesProBatchRecordExecutionDO execution;

    @BeforeEach
    void setUp() {
        execution = new MesProBatchRecordExecutionDO().setId(3001L).setBatchExecutionId(9001L)
                .setBatchRecordVersionId(4001L).setBatchRecordReportId("report-A").setFieldAuditRevision(2L)
                .setExecutionSnapshotJson("{\"fields\":["
                        + field("temperature", "温度", 1, "NUMBER") + ","
                        + field("serial", "序列号", 2, "STRING") + "]}")
                .setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"valueType\":\"NUMBER\",\"value\":32.50},"
                        + "{\"rowIndex\":2,\"columnIndex\":2,\"valueType\":\"STRING\",\"value\":\"007\"}]");
        when(executions.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenAnswer(ignored -> new PageResult<>(List.of(execution), 1L));
        when(tasks.selectListByBatchExecutionId(9001L)).thenReturn(List.of());
        when(audits.selectListByExecutionId(3001L)).thenReturn(List.of());
    }

    @Test
    void realCellsUseFrozenFieldIdentityTypeAndValue() {
        var catalog = adapter.readCatalog(batch);
        assertEquals(2, catalog.items().size());
        var temperature = catalog.items().stream().filter(item -> "温度".equals(item.getLabel())).findFirst().orElseThrow();
        var serial = catalog.items().stream().filter(item -> "序列号".equals(item.getLabel())).findFirst().orElseThrow();
        assertEquals("number", temperature.getValueType());
        assertEquals("string", serial.getValueType());
        assertEquals("007", serial.getSavedValue());
        assertTrue(temperature.getEvidenceKey().contains("4001"));
        assertTrue(temperature.getEvidenceKey().contains("temperature"));
        assertTrue(temperature.getSourceRef().startsWith("execution:3001#"));
        assertTrue(adapter.evaluate(batch, List.of(new Condition().setConditionId("R1")
                .setEvidenceKey(temperature.getEvidenceKey()).setSourceView("SAVED_RECORD")
                .setOperator("GT").setValue(32))).matches().get(0).matched());
    }

    @Test
    void frozenNumericLookingStringRemainsTextAndRejectsNumericComparison() {
        var catalog = adapter.readCatalog(batch);
        var serial = catalog.items().stream().filter(item -> "序列号".equals(item.getLabel())).findFirst().orElseThrow();
        var temperature = catalog.items().stream().filter(item -> "温度".equals(item.getLabel())).findFirst().orElseThrow();
        assertEquals("string", serial.getValueType());
        assertEquals("007", serial.getSavedValue());
        var equal = new Condition().setConditionId("R1-TEXT").setEvidenceKey(serial.getEvidenceKey())
                .setSourceView("SAVED_RECORD").setOperator("EQ").setValue(7);
        assertTrue(adapter.validateCondition(serial, equal).valid());
        assertFalse(adapter.evaluate(batch, List.of(equal)).matches().get(0).matched());
        equal.setValue("007");
        assertTrue(adapter.evaluate(batch, List.of(equal)).matches().get(0).matched());
        equal.setOperator("GT").setValue(7);
        var invalid = adapter.validateCondition(serial, equal);
        assertFalse(invalid.valid());
        assertEquals("CONDITION_INVALID", invalid.reasonCode());
        assertEquals("number", temperature.getValueType());
        assertTrue(adapter.validateCondition(temperature, new Condition().setConditionId("R1-NUMBER")
                .setEvidenceKey(temperature.getEvidenceKey()).setSourceView("SAVED_RECORD")
                .setOperator("GT").setValue(32)).valid());
    }

    @Test
    void emptyArrayIsNoRecordedFactAndDoesNotUseSnapshotDefaults() {
        execution.setCellValuesJson("[]");
        assertEquals("NO_RECORDED_FACT", adapter.readCatalog(batch).status());
        verify(audits).selectListByExecutionId(3001L);
    }

    @Test
    void emptyArrayStillReadsFormalAuditBeforeAndAfter() {
        execution.setCellValuesJson("[]");
        when(audits.selectListByExecutionId(3001L)).thenReturn(List.of(new MesProBatchRecordExecutionFieldAuditItemDO()
                .setId(5001L).setExecutionId(3001L).setFieldAuditRevision(2L)
                .setFieldPath("sheet[0].rows[1].cells[2].temperature").setFieldKey("temperature").setFieldLabel("温度")
                .setRowIndex(1).setColumnIndex(2).setValueType("NUMBER")
                .setOldValueJson("30").setNewValueJson("32.50")));
        var items = adapter.readCatalog(batch).items();
        assertEquals(2, items.size());
        assertTrue(items.stream().allMatch(item -> "FIELD_CHANGE".equals(item.getSourceView())));
        assertTrue(items.stream().anyMatch(item -> "field-audit:5001:before".equals(item.getSourceRef())
                && "30".equals(item.getSavedValue())));
        assertTrue(items.stream().anyMatch(item -> "field-audit:5001:after".equals(item.getSourceRef())));
    }

    @Test
    void realTraditionalAndDynamicFactsRemainIndependentInMixedBatch() {
        when(tasks.selectListByBatchExecutionId(9001L)).thenReturn(List.of(new MesProEdhrBatchExecutionTaskDO()
                .setId(9100L).setBatchExecutionId(9001L).setFormTemplateId(4200L).setFormTemplateVersionId(4201L)
                .setFormBindingKey("MAIN").setFormCenterInstanceId(9101L)));
        when(instances.selectById(9101L)).thenReturn(FormActionInstanceDO.builder()
                .id(9101L).tenantId(1L).dataDomain("MES").systemCode("MES").objectType("EDHR_ROUTE_FORM")
                .objectId("9100").objectVersion("2002")
                .actionCode(MesProRouteVersionPublishProjectionServiceImpl.routeFormActionCode(2002L, "MAIN"))
                .formDataJson("{\"batchExecutionId\":9001,\"batchTaskId\":9100,\"formTemplateId\":4200,"
                        + "\"formTemplateVersionId\":4201,\"temperature\":18}").build());
        var items = adapter.readCatalog(batch).items();
        assertEquals(3, items.size());
        assertEquals(1, items.stream().filter(item -> "FIELD_DYNAMIC:4201:temperature".equals(item.getEvidenceKey())).count());
        assertEquals(2, items.stream().filter(item -> item.getSourceRef().startsWith("execution:3001#")).count());
    }

    @Test
    void repeatedFieldPositionsHaveDistinctIdentitiesBoundToSavedRevision() {
        execution.setExecutionSnapshotJson("{\"fields\":[" + field("reading", "读数", 1, "NUMBER")
                + "," + field("reading", "读数", 2, "NUMBER") + "]}")
                .setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"value\":10},"
                        + "{\"rowIndex\":2,\"columnIndex\":2,\"value\":20}]");
        var original = adapter.readCatalog(batch).items().stream().map(item -> item.getEvidenceKey()).toList();
        assertEquals(2, original.stream().distinct().count());
        execution.setFieldAuditRevision(3L);
        assertTrue(adapter.readCatalog(batch).items().stream().noneMatch(item -> original.contains(item.getEvidenceKey())),
                "Even equal saved values must not equate different repeated-row revisions");
        execution.setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"value\":20},"
                + "{\"rowIndex\":2,\"columnIndex\":2,\"value\":10}]");
        assertTrue(adapter.readCatalog(batch).items().stream().noneMatch(item -> original.contains(item.getEvidenceKey())));
    }

    @Test
    void objectShapeIsRejectedRatherThanSupportedAsCompatibilityInput() {
        execution.setCellValuesJson("{\"temperature\":32}");
        assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch));
    }

    @Test
    void savedCoordinatesWithoutFrozenDeclarationAreRejected() {
        execution.setExecutionSnapshotJson("{\"fields\":[]}");
        assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch));
    }

    @Test
    void valueContradictingFrozenNumberTypeIsRejected() {
        execution.setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"value\":\"not-a-number\"}]");
        assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch));
    }

    @Test
    void unknownFrozenTypeCannotHideBehindAnExplicitNull() {
        execution.setExecutionSnapshotJson("{\"fields\":[" + field("reading", "读数", 1, "UNKNOWN") + "]}")
                .setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"value\":null}]");
        assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch));
    }

    private static String field(String key, String label, int row, String type) {
        return "{\"fieldKey\":\"" + key + "\",\"fieldPath\":\"sheet[0].rows[" + row + "].cells[2]." + key
                + "\",\"label\":\"" + label + "\",\"rowIndex\":" + row
                + ",\"columnIndex\":2,\"valueType\":\"" + type + "\",\"defaultValue\":99}";
    }
}
