package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordReportJsonBuilderTest {

    private static final String REPORT_CODE = "EBR_TEST_T01";
    private static final Path FIXED_SAMPLE = Path.of(
            "D:\\ProjectPackage\\Int\\IntRuoyi\\resource\\批记录模板.doc");
    private static final Path PRESSURE_PUMP_SAMPLE = Path.of(
            "C:\\Users\\BJB110\\Desktop\\文档\\批记录压力泵.doc");
    private static final Path PROCESS_INSPECTION_DOCX_SAMPLE = Path.of(
            "C:\\Users\\BJB110\\Desktop\\文档\\过程检验记录.docx");
    private static final List<String> REPEATED_EQUIPMENT_MATRIX_LABELS = List.of(
            "封口热合机", "热合机", "自动热合机", "□是", "□否");

    private final MesProBatchRecordReportJsonBuilder builder = new MesProBatchRecordReportJsonBuilder();
    private final MesProBatchRecordReportLayoutCalibrator calibrator = new MesProBatchRecordReportLayoutCalibrator();
    private final MesProBatchRecordDocParser parser = new MesProBatchRecordDocParser();

    @Test
    void build_shouldNotKeepRouteSpecificDocumentHeaderContentDetector() {
        boolean hasRouteSpecificDetector = java.util.Arrays.stream(MesProBatchRecordReportJsonBuilder.class.getDeclaredMethods())
                .anyMatch(method -> "isDocumentHeaderContentStart".equals(method.getName()));

        assertFalse(hasRouteSpecificDetector,
                "document header detection must stay structure-based and cannot keep source-document content detectors");
    }

    @Test
    void build_shouldProvideJimuHiddenCollectionsAndPreserveCellText() {
        MesProBatchRecordParsedTable table = parsedTable(8, "物料编码");

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);

        JSONObject hidden = root.getJSONObject("hidden");
        assertNotNull(hidden);
        assertTrue(hidden.get("rows") instanceof JSONArray);
        assertTrue(hidden.get("cols") instanceof JSONArray);
        JSONObject conditions = hidden.getJSONObject("conditions");
        assertNotNull(conditions);
        assertNotNull(conditions.getJSONObject("rows"));
        assertNotNull(conditions.getJSONObject("cols"));

        JSONObject recordSubTableOrCollection = root.getJSONObject("recordSubTableOrCollection");
        assertNotNull(recordSubTableOrCollection);
        assertTrue(recordSubTableOrCollection.get("record") instanceof JSONArray);
        assertTrue(recordSubTableOrCollection.get("range") instanceof JSONArray);
        assertTrue(recordSubTableOrCollection.get("group") instanceof JSONArray);

        JSONObject area = root.getJSONObject("area");
        assertNotNull(area);
        assertEquals(0, area.getIntValue("sri"));
        assertEquals(0, area.getIntValue("sci"));

        JSONObject queryFormSetting = root.getJSONObject("queryFormSetting");
        assertNotNull(queryFormSetting);
        assertEquals("", queryFormSetting.getString("idField"));
        assertEquals(100, root.getJSONObject("rows").getIntValue("len"));
        assertEquals(1, root.getJSONObject("cols").getIntValue("len"));
        assertEquals(9, root.getJSONArray("styles").getJSONObject(0).getJSONObject("font").getIntValue("size"));
        assertEquals(false, root.getJSONObject("rpbar").getBoolean("show"));
        assertEquals(false, root.getJSONObject("fillFormToolbar").getBoolean("show"));

        JSONObject rows = root.getJSONObject("rows");
        assertEquals("物料编码", rows.getJSONObject("0")
                .getJSONObject("cells")
                .getJSONObject("0")
                .getString("text"));
    }

    @Test
    void build_shouldNotExposeDefaultRightSideBlankGridColumns() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("清洗工序生产记录")
                .rowCount(4)
                .columnCount(20)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("清洗工序生产记录")
                                .rowSpan(1)
                                .colSpan(20)
                                .widthPx(1040)
                                .bold(true)
                                .build()),
                        row("操作日期", "物料编码", "物料名称", "批号", "清洗次数", "清洗介质", "清洗功率",
                                "清洗温度", "清洗时间", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs",
                                "操作人", "复核人"),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("烘干温度℃").colSpan(10).widthPx(520).build(),
                                MesProBatchRecordParsedCell.builder().text("烘干时间").colSpan(10).widthPx(520).build()),
                        row("2", "", "/", "股塞", "2", "纯化水", "100%", "室温", "30min", "", "", "", "", "")
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        JSONObject cols = root.getJSONObject("cols");
        assertEquals(20, cols.getIntValue("len"));
        assertFalse(cols.containsKey("20"), "right-side default grid columns must not be exposed after the real table");
        assertEquals(1040, root.getIntValue("dataRectWidth"));
    }

    @Test
    void build_shouldAllowDenseTemplateColumnsBelowLegacySixtyPixelFloor() {
        List<MesProBatchRecordParsedCell> headerCells = new ArrayList<>();
        for (int index = 0; index < 19; index++) {
            headerCells.add(MesProBatchRecordParsedCell.builder()
                    .text("C" + index)
                    .widthPx(24)
                    .build());
        }
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("dense")
                .rowCount(1)
                .columnCount(19)
                .rows(List.of(headerCells))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);

        assertEquals(24, root.getJSONObject("cols").getJSONObject("0").getIntValue("width"));
        assertEquals(24, root.getJSONObject("cols").getJSONObject("18").getIntValue("width"));
    }

    @Test
    void build_shouldCapColumnWidthAtSinglePageBudget() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("wide")
                .rowCount(1)
                .columnCount(1)
                .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                        .text("wide-cell")
                        .widthPx(500)
                        .build())))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);

        assertEquals(72, root.getJSONObject("cols").getJSONObject("0").getIntValue("width"));
    }

    @Test
    void build_shouldRespectFixedColumnWidthsFromCalibratedTemplate() {
        List<Integer> columnWidths = List.of(34, 62, 72, 45, 65, 40, 40, 45, 45, 42,
                42, 42, 42, 45, 45, 65, 65, 65, 65, 65);
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("rough")
                .rowCount(1)
                .columnCount(columnWidths.size())
                .columnWidths(columnWidths)
                .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                        .text("粗洗工序生产记录")
                        .colSpan(columnWidths.size())
                        .widthPx(columnWidths.stream().reduce(0, Integer::sum))
                        .build())))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);

        assertEquals(columnWidths.stream().reduce(0, Integer::sum), root.getIntValue("dataRectWidth"));
        assertEquals(columnWidths.stream().reduce(0, Integer::sum), root.getJSONObject("area").getIntValue("width"));
        assertEquals(34, root.getJSONObject("cols").getJSONObject("0").getIntValue("width"));
        assertEquals(72, root.getJSONObject("cols").getJSONObject("2").getIntValue("width"));
        assertEquals(65, root.getJSONObject("cols").getJSONObject("19").getIntValue("width"));
    }

    @Test
    void build_shouldNotExposeInferredColumnsBeyondFixedColumnWidths() {
        List<Integer> columnWidths = new ArrayList<>();
        for (int index = 0; index < 28; index++) {
            columnWidths.add(index < 27 ? 52 : 73);
        }
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(3)
                .tableTitle("精洗工序生产记录")
                .rowCount(2)
                .columnCount(28)
                .columnWidths(columnWidths)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder()
                                        .text("跨行侧栏")
                                        .rowSpan(2)
                                        .colSpan(13)
                                        .widthPx(676)
                                        .build(),
                                MesProBatchRecordParsedCell.builder()
                                        .text("精洗工序生产记录")
                                        .colSpan(15)
                                        .widthPx(801)
                                        .build()),
                        row("操作日期", "物料编码", "物料名称", "批号", "清洗次数", "清洗介质", "清洗功率",
                                "清洗温度", "清洗时间", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs",
                                "操作人", "复核人", "备注", "", "", "", "", "", "", "", "", "", "", "", "", "")
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        JSONObject cols = root.getJSONObject("cols");
        assertEquals(28, cols.getIntValue("len"));
        assertEquals(28, countNumericKeys(cols));
        assertEquals(1477, root.getIntValue("dataRectWidth"));
        assertEquals(1477, root.getJSONObject("area").getIntValue("width"));
        assertFalse(cols.containsKey("28"), "inferred columns beyond fixed source widths must not be exposed");
    }

    @Test
    void build_shouldRenderMetadataLabelValuePairsAsGroupedMergesAfterCalibration() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(5)
                .columnCount(21)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("组装Ⅰ工序生产记录")
                                .rowSpan(1)
                                .colSpan(21)
                                .bold(true)
                                .fontSize(10)
                                .horizontalAlign("center")
                                .verticalAlign("middle")
                                .widthPx(1260)
                                .heightPx(26)
                                .build()),
                        List.of(
                                sourceBackedCell("生产批号", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("", 1, 1, 60, 24, true, false, false),
                                sourceBackedCell("产品规格", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("", 1, 1, 60, 24, true, false, false),
                                sourceBackedCell("生产依据", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("PP-ID-1-08（  /  ）", 1, 16, 960, 24, false, false, false)
                        ),
                        List.of(
                                sourceBackedCell("生产前检查记录", 1, 7, 420, 24, false, false, true),
                                sourceBackedCell("检查要求", 1, 11, 660, 24, false, false, true),
                                sourceBackedCell("结果", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("操作人/日期", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("复核人/日期", 1, 1, 60, 24, false, false, true)
                        ),
                        List.of(
                                sourceBackedCell("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；", 1, 18, 1080, 36, false, false, false),
                                sourceBackedCell("□符合要求\n□不符合要求", 1, 1, 60, 36, false, false, false),
                                sourceBackedCell("", 1, 1, 60, 36, true, false, false),
                                sourceBackedCell("", 1, 1, 60, 36, true, false, false)
                        ),
                        List.of(sourceBackedCell("备注：检查结果符合要求后进行以下生产操作。", 1, 21, 1260, 24, false, false, false))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(source);
        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

        int metadataRowIndex = findRenderedRowIndexByText(root, "生产批号");
        JSONObject metadataCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(metadataRowIndex))
                .getJSONObject("cells");

        assertEquals(List.of(0, 2), metadataCells.getJSONObject("0").getJSONArray("merge").toJavaList(Integer.class));
        assertEquals(List.of(0, 3), metadataCells.getJSONObject("3").getJSONArray("merge").toJavaList(Integer.class));
        assertEquals(List.of(0, 2), metadataCells.getJSONObject("7").getJSONArray("merge").toJavaList(Integer.class));
        assertEquals(List.of(0, 3), metadataCells.getJSONObject("10").getJSONArray("merge").toJavaList(Integer.class));
        assertEquals(List.of(0, 2), metadataCells.getJSONObject("14").getJSONArray("merge").toJavaList(Integer.class));
        assertEquals(List.of(0, 3), metadataCells.getJSONObject("17").getJSONArray("merge").toJavaList(Integer.class));
        assertEquals("PP-ID-1-08（  /  ）", metadataCells.getJSONObject("17").getString("text"));
    }

    @Test
    void build_shouldPreserveChecklistNarrativeBandHeightFromCalibratedSource() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(5)
                .columnCount(21)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("组装Ⅰ工序生产记录")
                                .rowSpan(1)
                                .colSpan(21)
                                .bold(true)
                                .fontSize(10)
                                .horizontalAlign("center")
                                .verticalAlign("middle")
                                .widthPx(1260)
                                .heightPx(26)
                                .build()),
                        List.of(
                                sourceBackedCell("生产批号", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("", 1, 1, 60, 24, true, false, false),
                                sourceBackedCell("产品规格", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("", 1, 1, 60, 24, true, false, false),
                                sourceBackedCell("生产依据", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("PP-ID-1-08（  /  ）", 1, 16, 960, 24, false, false, false)
                        ),
                        List.of(
                                sourceBackedCell("生产前检查记录", 1, 7, 420, 24, false, false, true),
                                sourceBackedCell("检查要求", 1, 11, 660, 24, false, false, true),
                                sourceBackedCell("结果", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("操作人/日期", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("复核人/日期", 1, 1, 60, 24, false, false, true)
                        ),
                        List.of(
                                sourceBackedCell("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；\n"
                                                + "2、墙面、地面、天花板、灯具等环境清洁应符合《INT/PD/6.4工作环境控制程序》。\n"
                                                + "具体清场标准及内容依据《INT/GL/7.5.8-03清场管理制度》执行。", 1, 18, 1080, 52, false, false, false),
                                sourceBackedCell("□符合要求\n□不符合要求", 1, 1, 60, 52, false, false, false),
                                sourceBackedCell("", 1, 1, 60, 52, true, false, false),
                                sourceBackedCell("", 1, 1, 60, 52, true, false, false)
                        ),
                        List.of(sourceBackedCell("备注：检查结果符合要求后进行以下生产操作。", 1, 21, 1260, 28, false, false, false))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(source);
        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

        int headerRowIndex = findRenderedRowIndexByText(root, "检查要求");
        int bodyRowIndex = findRenderedRowIndexByText(root, "□符合要求\n□不符合要求");
        int remarkRowIndex = findRenderedRowIndexByText(root, "备注：检查结果符合要求后进行以下生产操作。");

        assertTrue(root.getJSONObject("rows").getJSONObject(String.valueOf(headerRowIndex)).getIntValue("height") >= 28);
        assertTrue(root.getJSONObject("rows").getJSONObject(String.valueOf(bodyRowIndex)).getIntValue("height") >= 52);
        assertTrue(root.getJSONObject("rows").getJSONObject(String.valueOf(remarkRowIndex)).getIntValue("height") >= 28);
    }

    @Test
    void build_shouldKeepInlineCheckboxChoicesInsideSingleVisualCell() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("通用工序生产记录")
                .rowCount(5)
                .columnCount(21)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("通用工序生产记录")
                                .rowSpan(1)
                                .colSpan(21)
                                .bold(true)
                                .fontSize(10)
                                .horizontalAlign("center")
                                .verticalAlign("middle")
                                .widthPx(1260)
                                .heightPx(26)
                                .build()),
                        List.of(
                                sourceBackedCell("生产前检查记录", 1, 7, 420, 24, false, false, true),
                                sourceBackedCell("检查要求", 1, 11, 660, 24, false, false, true),
                                sourceBackedCell("结果", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("操作人/日期", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("复核人/日期", 1, 1, 60, 24, false, false, true)
                        ),
                        List.of(
                                sourceBackedCell("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；", 1, 18, 1080, 52, false, false, false),
                                sourceBackedCell("□符合要求\n□不符合要求", 1, 1, 60, 52, false, false, false),
                                sourceBackedCell("", 1, 1, 60, 52, true, false, false),
                                sourceBackedCell("", 1, 1, 60, 52, true, false, false)
                        ),
                        List.of(sourceBackedCell("备注：检查结果符合要求后进行以下生产操作。", 1, 21, 1260, 28, false, false, false)),
                        List.of(sourceBackedCell("封口热合机：□A05199", 1, 21, 1260, 28, false, false, false))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(source);
        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

        int bodyRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("符合要求"))
                .stream()
                .findFirst()
                .orElseThrow();
        JSONObject bodyCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(bodyRowIndex))
                .getJSONObject("cells");
        JSONObject checklistCell = findCellByText(bodyCells, "□符合要求");
        JSONObject fillForm = checklistCell.getJSONObject("fillForm");
        JSONObject rule = checklistCell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
        assertNotNull(fillForm);
        assertEquals("checkbox", fillForm.getString("componentFlag"));
        assertEquals(Boolean.FALSE, fillForm.get("defaultValue"));
        assertEquals(2, fillForm.getJSONArray("options").size());
        assertTrue(hasCheckboxFillFormOption(fillForm, "符合要求"));
        assertTrue(hasCheckboxFillFormOption(fillForm, "不符合要求"));
        assertNotNull(rule);
        assertEquals("BOOLEAN", rule.getString("valueType"));
        assertEquals("checkbox", rule.getString("componentFlag"));
        assertEquals(1, countCheckboxFillForms(bodyCells));
        assertFalse(hasCheckboxFillFormLabel(bodyCells, "不符合要求"),
                "inline checkbox choices without trailing fillable text must not be split into a narrow table cell");

        int equipmentRowIndex = findRenderedRowIndexByText(root, "封口热合机：□A05199");
        JSONObject equipmentCell = findCellByText(root.getJSONObject("rows")
                .getJSONObject(String.valueOf(equipmentRowIndex))
                .getJSONObject("cells"), "封口热合机");
        assertTrue(!equipmentCell.containsKey("fillForm"));
    }

    @Test
    void build_shouldNotPromoteCheckboxFragmentsUnderSignatureDateHeaders() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("通用签署日期列识别")
                .rowCount(2)
                .columnCount(5)
                .rows(List.of(
                        List.of(
                                sourceBackedCell("检查要求", 1, 2, 240, 28, false, false, true),
                                sourceBackedCell("结果", 1, 1, 80, 28, false, false, true),
                                sourceBackedCell("操作人/日期", 1, 1, 90, 28, false, false, true),
                                sourceBackedCell("复核人/日期", 1, 1, 90, 28, false, false, true)
                        ),
                        List.of(
                                sourceBackedCell("工作场所检查", 1, 2, 240, 36, false, false, false),
                                sourceBackedCell("□符合要求", 1, 1, 80, 36, false, false, false),
                                sourceBackedCell("□不符合要求", 1, 1, 90, 36, false, false, false),
                                sourceBackedCell("", 1, 1, 90, 36, true, false, false)
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(source, REPORT_CODE));
        JSONObject bodyCells = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject resultCell = bodyCells.getJSONObject("2");
        JSONObject operatorDateCell = bodyCells.getJSONObject("3");

        assertEquals("checkbox", resultCell.getJSONObject("fillForm").getString("componentFlag"));
        assertEquals(2, resultCell.getJSONObject("fillForm").getJSONArray("options").size());
        assertTrue(hasCheckboxFillFormOption(resultCell.getJSONObject("fillForm"), "符合要求"));
        assertTrue(hasCheckboxFillFormOption(resultCell.getJSONObject("fillForm"), "不符合要求"));
        assertEquals(1, countCheckboxFillForms(bodyCells),
                "checkbox fragments under signature/date headers must not become additional checkbox controls");
        assertEquals("", operatorDateCell.getString("text"));
        assertEquals("input-text", operatorDateCell.getJSONObject("fillForm").getString("componentFlag"));
    }

    @Test
    void build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("粗洗工序签署日期列偏移识别")
                .rowCount(2)
                .columnCount(8)
                .rows(List.of(
                        List.of(
                                sourceBackedCellAt("检查要求", 0, 1, 2, 240, 28, false, false, true),
                                sourceBackedCellAt("结果", 2, 1, 1, 80, 28, false, false, true),
                                sourceBackedCellAt("操作人/日期", 4, 1, 1, 90, 28, false, false, true),
                                sourceBackedCellAt("复核人/日期", 6, 1, 1, 90, 28, false, false, true)
                        ),
                        List.of(
                                sourceBackedCellAt("工作场所检查", 0, 1, 2, 240, 36, false, false, false),
                                sourceBackedCellAt("□符合要求", 2, 1, 1, 80, 36, false, false, false),
                                sourceBackedCellAt("□不符合要求", 5, 1, 1, 90, 36, false, false, false),
                                sourceBackedCellAt("", 6, 1, 1, 90, 36, true, false, false)
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(source, REPORT_CODE));
        JSONObject bodyCells = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject resultCell = bodyCells.getJSONObject("2");
        JSONObject shiftedSignatureDateCell = bodyCells.getJSONObject("5");

        assertEquals("checkbox", resultCell.getJSONObject("fillForm").getString("componentFlag"));
        JSONArray resultOptions = resultCell.getJSONObject("fillForm").getJSONArray("options");
        assertNotNull(resultOptions, "signature/date checkbox fragments must be merged as result options");
        assertEquals(2, resultOptions.size());
        assertTrue(hasCheckboxFillFormOption(resultCell.getJSONObject("fillForm"), "符合要求"));
        assertTrue(hasCheckboxFillFormOption(resultCell.getJSONObject("fillForm"), "不符合要求"));
        assertEquals(1, countCheckboxFillForms(bodyCells),
                "misaligned checkbox fragments inside signature/date tail must be folded into the result cell");
        assertEquals("", shiftedSignatureDateCell.getString("text"));
        assertEquals("input-text", shiftedSignatureDateCell.getJSONObject("fillForm").getString("componentFlag"));
    }

    @Test
    void build_shouldKeepYesNoCheckboxChoicesInsideSingleVisualCell() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic yes no checkbox")
                .rowCount(2)
                .columnCount(4)
                .rows(List.of(
                        row("项目", "结果", "操作人/日期", "复核人/日期"),
                        row("清洁卫生", "□是 □否", "", "")
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject cells = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject checkboxCell = cells.getJSONObject("1");
        JSONObject fillForm = checkboxCell.getJSONObject("fillForm");

        assertNotNull(fillForm);
        assertEquals("checkbox", fillForm.getString("componentFlag"));
        assertEquals(2, fillForm.getJSONArray("options").size());
        assertTrue(hasCheckboxFillFormOption(fillForm, "是"));
        assertTrue(hasCheckboxFillFormOption(fillForm, "否"));
        assertEquals(1, countCheckboxFillForms(cells));
    }

    @Test
    void build_shouldKeepRealAssemblyInlineCheckboxChoicesInsideSingleVisualCell() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();
        MesProBatchRecordParsedTable parsedTable = recognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(item -> "组装Ⅰ工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int checklistRowIndex = findRenderedRowIndexes(root, row -> {
                    String text = renderedRowText(row);
                    return text.contains("工作场所") && text.contains("符合要求");
                })
                .stream()
                .findFirst()
                .orElseThrow();
        JSONObject cells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(checklistRowIndex))
                .getJSONObject("cells");
        JSONObject checkboxCell = findCellByText(cells, "□符合要求");
        JSONObject fillForm = checkboxCell.getJSONObject("fillForm");

        assertNotNull(fillForm);
        assertEquals("checkbox", fillForm.getString("componentFlag"));
        assertEquals(2, fillForm.getJSONArray("options").size());
        assertTrue(hasCheckboxFillFormOption(fillForm, "符合要求"));
        assertTrue(hasCheckboxFillFormOption(fillForm, "不符合要求"));
        assertEquals(1, countCheckboxFillForms(cells),
                "real assembly checklist choices must stay inside one visual result cell");
        assertFalse(hasCheckboxFillFormLabel(cells, "不符合要求"),
                "real assembly checklist must not render the second choice in a narrow table cell");
    }

    @Test
    void build_shouldSplitInlineChecklistChoiceCellWithTrailingUnderlineIntoIndependentFillForms() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(16)
                .tableTitle("通用过程检验记录")
                .rowCount(2)
                .columnCount(5)
                .rows(List.of(
                        List.of(
                                sourceBackedCell("序号", 1, 1, 60, 28, false, false, true),
                                sourceBackedCell("检测结果", 1, 3, 300, 28, false, false, true),
                                sourceBackedCell("检验设备", 1, 1, 80, 28, false, false, true)
                        ),
                        List.of(
                                sourceBackedCell("1", 1, 1, 60, 36, false, false, false),
                                sourceBackedCell("□符合要求  □不符合要求____", 1, 3, 300, 36, false, false, false),
                                sourceBackedCell("目测", 1, 1, 80, 36, false, false, false)
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(source, REPORT_CODE));
        int bodyRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("符合要求"))
                .stream()
                .findFirst()
                .orElseThrow();
        JSONObject bodyCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(bodyRowIndex))
                .getJSONObject("cells");

        assertEquals(2, countCheckboxFillForms(bodyCells));
        assertTrue(hasCheckboxFillFormLabel(bodyCells, "符合要求"));
        assertTrue(hasCheckboxFillFormLabel(bodyCells, "不符合要求"));
        assertTrue(hasInputTextFillFormWithBlankPlaceholder(bodyCells),
                "the trailing underline must remain a writable text area after the second checkbox");
    }

    @Test
    void build_shouldExpandInlineUnderlineFillablePromptsIntoTextInputs() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(17)
                .tableTitle("过程检验记录")
                .rowCount(2)
                .columnCount(16)
                .rows(List.of(
                        List.of(sourceBackedCell("检测结果", 1, 16, 960, 28, false, false, true)),
                        List.of(sourceBackedCell(
                                "合格数量：________；不合格数量：________；不合格评审报告编号（若有）：",
                                1, 16, 960, 36, false, false, false))
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(source, REPORT_CODE));
        int bodyRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("合格数量"))
                .stream()
                .findFirst()
                .orElseThrow();
        JSONObject bodyCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(bodyRowIndex))
                .getJSONObject("cells");

        assertEquals(3, countInputTextFillForms(bodyCells),
                "every inline writable blank, including the trailing label blank, should become an input");
        assertTrue(renderedRowText(root.getJSONObject("rows").getJSONObject(String.valueOf(bodyRowIndex)))
                .contains("不合格评审报告编号"));
    }

    @Test
    void build_shouldRenderNarrativePromptBlankAreaAsTextarea() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(18)
                .tableTitle("过程检验记录")
                .rowCount(2)
                .columnCount(16)
                .rows(List.of(
                        List.of(sourceBackedCell("过程检验记录", 1, 16, 960, 28, false, false, true)),
                        List.of(sourceBackedCell("备注：特殊内容（如不合格描述）可填写进下面空白处。",
                                1, 16, 960, 72, false, false, false))
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(source, REPORT_CODE));
        int bodyRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("特殊内容"))
                .stream()
                .findFirst()
                .orElseThrow();
        JSONObject bodyCell = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(bodyRowIndex))
                .getJSONObject("cells")
                .getJSONObject("0");

        assertEquals("input-textarea", bodyCell.getJSONObject("fillForm").getString("componentFlag"));
        assertTrue(bodyCell.getString("text").contains("备注"));
    }

    @Test
    void build_shouldRecognizeRealProcessInspectionInlineBlankAreasAsTextInputs() throws Exception {
        Assumptions.assumeTrue(Files.exists(PROCESS_INSPECTION_DOCX_SAMPLE),
                "process inspection docx sample fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PROCESS_INSPECTION_DOCX_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = calibrator.calibrate(parser.parseDocx(bytes).get(0));

        JSONObject root = JSON.parseObject(builder.build(parsedTable, REPORT_CODE));
        String renderedText = renderedSheetText(root);
        int summaryRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("合格数量")
                        && renderedRowText(row).contains("不合格数量")
                        && renderedRowText(row).contains("不合格评审报告编号"))
                .stream()
                .findFirst()
                .orElseThrow();
        JSONObject summaryCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(summaryRowIndex))
                .getJSONObject("cells");
        int remarkRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("特殊内容")
                        && renderedRowText(row).contains("空白处"))
                .stream()
                .findFirst()
                .orElseThrow();
        JSONObject remarkCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(remarkRowIndex))
                .getJSONObject("cells");

        assertTrue(renderedText.contains("1 of 5"));
        assertFalse(renderedText.contains("1 of 8"));
        assertTrue(countInputTextFillForms(summaryCells) >= 3,
                "real process inspection footer summary blanks should be writable text inputs");
        assertTrue(hasTextareaFillForm(remarkCells),
                "real process inspection remark blank area should be a writable textarea");
    }

    @Test
    void build_shouldKeepSequenceNumberColumnAsStaticNumbers() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(17)
                .tableTitle("过程检验记录")
                .rowCount(5)
                .columnCount(6)
                .rows(List.of(
                        List.of(
                                sourceBackedCell("序号", 1, 1, 54, 28, false, false, true),
                                sourceBackedCell("检验日期", 1, 1, 78, 28, false, false, true),
                                sourceBackedCell("检验项目", 1, 1, 86, 28, false, false, true),
                                sourceBackedCell("检测数量pcs", 1, 1, 92, 28, false, false, true),
                                sourceBackedCell("检测结果", 1, 1, 180, 28, false, false, true),
                                sourceBackedCell("检验设备", 1, 1, 86, 28, false, false, true)
                        ),
                        List.of(
                                sourceBackedCell("", 1, 1, 54, 36, false, false, false),
                                sourceBackedCell("", 1, 1, 78, 36, true, false, false),
                                sourceBackedCell("清洗", 1, 1, 86, 36, false, false, false),
                                sourceBackedCell("抽检", 1, 1, 92, 36, false, false, true),
                                sourceBackedCell("□符合要求  □不符合要求____", 1, 1, 180, 36, false, false, false),
                                sourceBackedCell("目测", 1, 1, 86, 36, false, false, false)
                        ),
                        List.of(
                                sourceBackedCell("", 1, 1, 54, 36, false, false, false),
                                sourceBackedCell("", 1, 1, 78, 36, true, false, false),
                                sourceBackedCell("精洗", 1, 1, 86, 36, false, false, false),
                                sourceBackedCell("抽检", 1, 1, 92, 36, false, false, true),
                                sourceBackedCell("□符合要求  □不符合要求____", 1, 1, 180, 36, false, false, false),
                                sourceBackedCell("目测", 1, 1, 86, 36, false, false, false)
                        ),
                        List.of(
                                sourceBackedCell("", 2, 1, 54, 72, false, false, false),
                                sourceBackedCell("", 2, 1, 78, 72, true, false, false),
                                sourceBackedCell("组装I", 2, 1, 86, 72, false, false, true),
                                sourceBackedCell("首检", 1, 1, 92, 36, false, false, true),
                                sourceBackedCell("□符合要求  □不符合要求____", 1, 1, 180, 36, false, false, false),
                                sourceBackedCell("目测", 2, 1, 86, 72, false, false, false)
                        ),
                        List.of(
                                sourceBackedCell("抽检", 1, 1, 92, 36, false, false, true),
                                sourceBackedCell("□符合要求  □不符合要求____", 1, 1, 180, 36, false, false, false)
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(source, REPORT_CODE));
        JSONObject row1Cells = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject row2Cells = root.getJSONObject("rows").getJSONObject("2").getJSONObject("cells");
        JSONObject row3Cells = root.getJSONObject("rows").getJSONObject("3").getJSONObject("cells");

        JSONObject sequence1 = row1Cells.getJSONObject("0");
        JSONObject sequence2 = row2Cells.getJSONObject("0");
        JSONObject sequence3 = row3Cells.getJSONObject("0");

        assertEquals("1", sequence1.getString("text"));
        assertEquals("2", sequence2.getString("text"));
        assertEquals("3", sequence3.getString("text"));
        assertFalse(sequence1.containsKey("fillForm"));
        assertFalse(sequence2.containsKey("fillForm"));
        assertFalse(sequence3.containsKey("fillForm"));
        assertEquals(1, sequence3.getJSONArray("merge").getIntValue(0));
        assertNotNull(row1Cells.getJSONObject("1").getJSONObject("fillForm"),
                "ordinary blank cells outside the sequence column must remain fillable");
    }

    @Test
    void build_shouldPreserveNonUniformChecklistColumnWidthsForAssemblyChecklist() {
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(5)
                .columnCount(21)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("组装Ⅰ工序生产记录")
                                .rowSpan(1)
                                .colSpan(21)
                                .bold(true)
                                .fontSize(10)
                                .horizontalAlign("center")
                                .verticalAlign("middle")
                                .widthPx(1260)
                                .heightPx(26)
                                .build()),
                        List.of(
                                sourceBackedCell("生产批号", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("", 1, 1, 60, 24, true, false, false),
                                sourceBackedCell("产品规格", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("", 1, 1, 60, 24, true, false, false),
                                sourceBackedCell("生产依据", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("PP-ID-1-08（  /  ）", 1, 16, 960, 24, false, false, false)
                        ),
                        List.of(
                                sourceBackedCell("生产前检查记录", 1, 7, 420, 24, false, false, true),
                                sourceBackedCell("检查要求", 1, 11, 660, 24, false, false, true),
                                sourceBackedCell("结果", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("操作人/日期", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("复核人/日期", 1, 1, 60, 24, false, false, true)
                        ),
                        List.of(
                                sourceBackedCell("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；\n"
                                                + "2、墙面、地面、天花板、灯具等环境清洁应符合《INT/PD/6.4工作环境控制程序》。", 1, 18, 1080, 52, false, false, false),
                                sourceBackedCell("□符合要求\n□不符合要求", 1, 1, 60, 52, false, false, false),
                                sourceBackedCell("", 1, 1, 60, 52, true, false, false),
                                sourceBackedCell("", 1, 1, 60, 52, true, false, false)
                        ),
                        List.of(sourceBackedCell("备注：检查结果符合要求后进行以下生产操作。", 1, 21, 1260, 28, false, false, false))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(source);
        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        JSONObject cols = root.getJSONObject("cols");

        int sideWidth = cols.getJSONObject("0").getIntValue("width");
        int narrativeWidth = cols.getJSONObject("1").getIntValue("width")
                + cols.getJSONObject("2").getIntValue("width")
                + cols.getJSONObject("3").getIntValue("width")
                + cols.getJSONObject("4").getIntValue("width")
                + cols.getJSONObject("5").getIntValue("width")
                + cols.getJSONObject("6").getIntValue("width")
                + cols.getJSONObject("7").getIntValue("width")
                + cols.getJSONObject("8").getIntValue("width")
                + cols.getJSONObject("9").getIntValue("width")
                + cols.getJSONObject("10").getIntValue("width")
                + cols.getJSONObject("11").getIntValue("width");
        int resultWidth = cols.getJSONObject("12").getIntValue("width")
                + cols.getJSONObject("13").getIntValue("width")
                + cols.getJSONObject("14").getIntValue("width");
        int operatorWidth = cols.getJSONObject("15").getIntValue("width")
                + cols.getJSONObject("16").getIntValue("width")
                + cols.getJSONObject("17").getIntValue("width");
        int reviewerWidth = cols.getJSONObject("18").getIntValue("width")
                + cols.getJSONObject("19").getIntValue("width")
                + cols.getJSONObject("20").getIntValue("width");

        assertTrue(sideWidth < resultWidth, "side strip should stay narrower than a tail block");
        assertTrue(narrativeWidth > resultWidth, "narrative band should stay wider than tail blocks");
        assertTrue(Math.abs(resultWidth - operatorWidth) <= 1,
                "tail blocks should stay balanced: result=" + resultWidth + ", operator=" + operatorWidth
                        + ", reviewer=" + reviewerWidth);
        assertTrue(Math.abs(resultWidth - reviewerWidth) <= 1,
                "tail blocks should stay balanced: result=" + resultWidth + ", operator=" + operatorWidth
                        + ", reviewer=" + reviewerWidth);
    }

    @Test
    void build_shouldKeepVerticalMergesForAssemblyOperationBandFromRealRouteBSample() throws Exception {
        Path source = Path.of("C:\\Users\\BJB110\\Desktop\\2\\2\\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc");
        Assumptions.assumeTrue(Files.exists(source), "real Route B sample is required for this regression");

        byte[] bytes = Files.readAllBytes(source);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();
        MesProBatchRecordParsedTable sourceTable = recognizer.recognize(source, bytes, source.getFileName().toString()).stream()
                .filter(table -> "组装Ⅰ工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedCell sourceSelfInspectionCell = findParsedCellByText(sourceTable, "生产自检");
        MesProBatchRecordParsedCell sourceNarrativeCell = findParsedCellByText(sourceTable, "合格标准：");

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(sourceTable);
        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

        int operationRowIndex = findRenderedRowIndexByText(root, "操作日期");
        JSONObject operationCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(operationRowIndex))
                .getJSONObject("cells");
        JSONObject selfInspectionCell = findCellByText(operationCells, "生产自检");
        JSONObject narrativeCell = findCellByText(operationCells, "合格标准：");

        assertEquals(List.of(5, Math.max(1, sourceSelfInspectionCell.getColSpan()) - 1),
                selfInspectionCell.getJSONArray("merge").toJavaList(Integer.class),
                "生产自检 must restore vertical merge while keeping source column span");
        assertEquals(List.of(5, Math.max(1, sourceNarrativeCell.getColSpan()) - 1),
                narrativeCell.getJSONArray("merge").toJavaList(Integer.class),
                "合格标准 must restore vertical merge while keeping source column span");
    }

    @Test
    void build_shouldUseLandscapeA4ForWideCalibratedTables() {
        List<Integer> columnWidths = List.of(34, 62, 72, 45, 65, 40, 40, 45, 45, 42,
                42, 42, 42, 45, 45, 65, 65, 65, 65, 65);
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("wide-process")
                .rowCount(1)
                .columnCount(columnWidths.size())
                .columnWidths(columnWidths)
                .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                        .text("通用宽表")
                        .colSpan(columnWidths.size())
                        .widthPx(columnWidths.stream().reduce(0, Integer::sum))
                        .build())))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject printConfig = root.getJSONObject("printConfig");

        assertEquals("landscape", printConfig.getString("layout"));
        assertEquals(297, printConfig.getIntValue("width"));
        assertEquals(210, printConfig.getIntValue("height"));
    }

    @Test
    void build_shouldUseSourceHeaderRowsAndFixedPrintHeaderForDocLikeHeaderBlocks() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("doc-header")
                .rowCount(3)
                .columnCount(10)
                .rows(List.of(
                        List.of(
                                docHeaderCell("文档标题", 2, 5, true),
                                docHeaderCell("记录编号", 1, 2, true),
                                docHeaderCell("RE-01", 1, 3, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 2, true),
                                docHeaderCell("A/1", 1, 3, true)
                        ),
                        List.of(docHeaderCell("主体内容", 1, 10, false))
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(MesProBatchRecordReportShapeRules.DOC_HEADER_PRINT_MARGIN_Y,
                root.getJSONObject("printConfig").getIntValue("marginY"));
        JSONObject fixedHeadRange = root.getJSONArray("fixedPrintHeadRows").getJSONObject(0);
        assertEquals(0, fixedHeadRange.getIntValue("sri"));
        assertEquals(0, fixedHeadRange.getIntValue("sci"));
        assertEquals(1, fixedHeadRange.getIntValue("eri"));
        assertEquals(9, fixedHeadRange.getIntValue("eci"));
        assertEquals("文档标题", root.getJSONObject("rows").getJSONObject("0")
                .getJSONObject("cells").getJSONObject("0").getString("text"));
    }

    @Test
    void build_shouldRegisterDocLikeFooterAsFixedPrintTailRows() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("doc-footer")
                .rowCount(4)
                .columnCount(10)
                .rows(List.of(
                        List.of(
                                docHeaderCell("文档标题", 2, 5, true),
                                docHeaderCell("记录编号", 1, 2, true),
                                docHeaderCell("RE-01", 1, 3, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 2, true),
                                docHeaderCell("A/1", 1, 3, true)
                        ),
                        List.of(docHeaderCell("主体内容", 1, 10, false)),
                        row("生效日期：2026年02月02日")
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONArray tailRanges = root.getJSONArray("fixedPrintTailRows");

        assertEquals(1, tailRanges.size());
        JSONObject tailRange = tailRanges.getJSONObject(0);
        assertEquals(3, tailRange.getIntValue("sri"));
        assertEquals(0, tailRange.getIntValue("sci"));
        assertEquals(3, tailRange.getIntValue("eri"));
        assertEquals(9, tailRange.getIntValue("eci"));
        assertEquals("生效日期：2026年02月02日", root.getJSONObject("rows").getJSONObject("3")
                .getJSONObject("cells").getJSONObject("0").getString("text"));
    }

    @Test
    void build_shouldNotRegisterFooterLikeRowsAsFixedTailWithoutDocHeader() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("plain-footer")
                .rowCount(2)
                .columnCount(1)
                .rows(List.of(
                        row("主体内容"),
                        row("生效日期：2026年02月02日")
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertTrue(root.getJSONArray("fixedPrintTailRows").isEmpty());
    }

    @Test
    void build_shouldKeepRepeatedDocLikeHeaderBlocksAsSourceRows() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("doc-header-repeat")
                .rowCount(6)
                .columnCount(10)
                .rows(List.of(
                        List.of(
                                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                                docHeaderCell("记录编号", 1, 2, true),
                                docHeaderCell("RE-PP-ID-01", 1, 3, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 2, true),
                                docHeaderCell("A/1", 1, 3, true)
                        ),
                        List.of(docHeaderCell("主体内容", 1, 10, false)),
                        List.of(
                                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                                docHeaderCell("记录编号", 1, 2, true),
                                docHeaderCell("RE-PP-ID-01", 1, 3, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 2, true),
                                docHeaderCell("A/1", 1, 3, true)
                        ),
                        List.of(docHeaderCell("续页主体", 1, 10, false))
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertEquals("球囊扩张压力泵生产记录", root.getJSONObject("rows").getJSONObject("3")
                .getJSONObject("cells").getJSONObject("0").getString("text"));
    }

    @Test
    void build_shouldUseSourceDocumentHeaderRowsWithoutSyntheticFirstPageSpacer() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("doc-header-repeat-rhythm")
                .rowCount(6)
                .columnCount(10)
                .rows(List.of(
                        List.of(
                                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                                docHeaderCell("记录编号", 1, 2, true),
                                docHeaderCell("RE-PP-ID-01", 1, 3, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 2, true),
                                docHeaderCell("A/1", 1, 3, true)
                        ),
                        List.of(docHeaderCell("主体内容", 1, 10, false)),
                        List.of(
                                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                                docHeaderCell("记录编号", 1, 2, true),
                                docHeaderCell("RE-PP-ID-01", 1, 3, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 2, true),
                                docHeaderCell("A/1", 1, 3, true)
                        ),
                        List.of(docHeaderCell("续页主体", 1, 10, false))
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject firstRenderedRow = root.getJSONObject("rows").getJSONObject("0");
        JSONObject fixedHeadRange = root.getJSONArray("fixedPrintHeadRows").getJSONObject(0);

        assertEquals("球囊扩张压力泵生产记录", firstRenderedRow.getJSONObject("cells")
                .getJSONObject("0").getString("text"));
        assertFalse(firstRenderedRow.getBooleanValue("pagingRow"),
                "source document header row should be the first rendered row, not a synthetic paging spacer");
        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1),
                "source-authored repeated document headers should not gain synthetic spacer rows");
        assertEquals(0, fixedHeadRange.getIntValue("sri"));
        assertEquals(1, fixedHeadRange.getIntValue("eri"));
    }

    @Test
    void build_shouldKeepShiftedRepeatedDocHeaderBlocksAsSourceRows() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("doc-header-shifted-repeat")
                .rowCount(6)
                .columnCount(19)
                .rows(List.of(
                        List.of(
                                docHeaderCell("球囊扩张压力泵生产记录", 2, 10, true),
                                docHeaderCell("记录编号", 1, 4, true),
                                docHeaderCell("RE-PP-ID-01", 1, 5, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 4, true),
                                docHeaderCell("A/1", 1, 5, true)
                        ),
                        List.of(docHeaderCell("主体内容", 1, 19, false)),
                        List.of(
                                docHeaderCell("球囊扩张压力泵生产记录", 2, 10, true),
                                docHeaderCell("记录编号", 1, 1, true),
                                docHeaderCell("RE-PP-ID-01", 1, 8, true)
                        ),
                        List.of(
                                docHeaderCell("版本", 1, 1, true),
                                docHeaderCell("A/1", 1, 5, true)
                        ),
                        List.of(docHeaderCell("续页主体", 1, 19, false))
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertEquals("球囊扩张压力泵生产记录", root.getJSONObject("rows").getJSONObject("3")
                .getJSONObject("cells").getJSONObject("0").getString("text"));
    }

    @Test
    void build_shouldPreserveBudgetSizedWideColumnsForGenericOverviewPages() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("overview")
                .rowCount(2)
                .columnCount(6)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("装配及包装信息")
                                .colSpan(6)
                                .widthPx(960)
                                .bold(true)
                                .build()),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("工序名称").widthPx(160).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("操作人员").widthPx(160).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("装配日期").widthPx(160).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("工序名称").widthPx(160).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("操作人员").widthPx(160).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("装配日期").widthPx(160).bold(true).build()
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(1120, root.getIntValue("dataRectWidth"));
        assertEquals(1120, root.getJSONObject("area").getIntValue("width"));
        assertTrue(root.getJSONObject("cols").getJSONObject("0").getIntValue("width") >= 186);
        assertTrue(root.getJSONObject("cols").getJSONObject("5").getIntValue("width") >= 186);
    }

    @Test
    void build_shouldGenerateInputFillFormForRecognizedBlankCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("align")
                .rowCount(1)
                .columnCount(3)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("标签").widthPx(80).build(),
                        MesProBatchRecordParsedCell.builder().text("").widthPx(80).build(),
                        MesProBatchRecordParsedCell.builder().text("备注：这里是一段较长的说明文字，用于验证叙述型内容左对齐。").widthPx(100).build()
                )))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);
        JSONObject row0 = root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells");
        JSONArray styles = root.getJSONArray("styles");

        JSONObject shortStyle = styles.getJSONObject(row0.getJSONObject("0").getIntValue("style"));
        JSONObject blankStyle = styles.getJSONObject(row0.getJSONObject("1").getIntValue("style"));
        JSONObject narrativeStyle = styles.getJSONObject(row0.getJSONObject("2").getIntValue("style"));
        JSONObject blankCell = row0.getJSONObject("1");
        JSONObject fillForm = blankCell.getJSONObject("fillForm");

        assertEquals("center", shortStyle.getString("align"));
        assertEquals("middle", shortStyle.getString("valign"));
        assertEquals("center", blankStyle.getString("align"));
        assertEquals("middle", blankStyle.getString("valign"));
        assertEquals("left", narrativeStyle.getString("align"));
        assertEquals("middle", narrativeStyle.getString("valign"));
        assertEquals("", blankCell.getString("text"));
        assertEquals("Input", fillForm.getString("component"));
        assertEquals("input-text", fillForm.getString("componentFlag"));
        assertEquals("\u8bf7\u586b\u5199", fillForm.getString("placeholder"));
        assertEquals("ebr_" + REPORT_CODE + "_r0_c1", fillForm.getString("field"));
        assertEquals(false, fillForm.getBoolean("required"));
    }

    @Test
    void build_shouldGenerateCheckboxFillFormForRecognizedCheckboxChoiceCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("checkbox")
                .rowCount(2)
                .columnCount(4)
                .rows(List.of(
                        row("操作日期", "物料编码", "物料名称", "批号"),
                        row("", "/", "□30atm压力表", "")
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject checkboxCell = root.getJSONObject("rows")
                .getJSONObject("1")
                .getJSONObject("cells")
                .getJSONObject("2");
        JSONObject fillForm = checkboxCell.getJSONObject("fillForm");
        JSONObject cellRule = checkboxCell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);

        assertNotNull(fillForm);
        assertNotNull(cellRule);
        assertEquals("□30atm压力表", checkboxCell.getString("text"));
        assertEquals("checkbox", fillForm.getString("componentFlag"));
        assertEquals(Boolean.FALSE, fillForm.get("value"));
        assertEquals(Boolean.FALSE, fillForm.get("defaultValue"));
        assertEquals("", fillForm.getString("placeholder"));
        assertEquals("30atm压力表", fillForm.getString("labelText"));
        assertEquals("BOOLEAN", cellRule.getString("valueType"));
        assertEquals("checkbox", cellRule.getString("componentFlag"));
        assertEquals("30atm压力表", cellRule.getString("label"));
        assertEquals(false, cellRule.getBoolean("reviewed"));
    }

    @Test
    void build_shouldGenerateCheckboxFillFormForAlphanumericEquipmentCodeChoiceGroups() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("light cure equipment checkbox")
                .rowCount(2)
                .columnCount(10)
                .rows(List.of(
                        row("操作日期", "光固机设备编码", "是否在计量效期内", "紫外灯数量", "参数",
                                "能量范围mJ/cm2", "参数", "传送带速度", "运行次数", "生产数量/pcs"),
                        row("", "□A05075\n(A05059", "□是 □否", "1", "", "800-1000", "", "10-30", "1", "")
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject cells = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject equipmentCodeCell = cells.getJSONObject("1");
        JSONObject effectiveCell = cells.getJSONObject("2");

        assertEquals("checkbox", equipmentCodeCell
                .getJSONObject("fillForm")
                .getString("componentFlag"));
        assertEquals("A05075 (A05059", equipmentCodeCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getString("label"));
        assertEquals(false, equipmentCodeCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getBoolean("reviewed"));
        assertEquals("checkbox", effectiveCell
                .getJSONObject("fillForm")
                .getString("componentFlag"));
        assertEquals(false, effectiveCell
                .getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY)
                .getBoolean("reviewed"));
    }

    @Test
    void build_shouldShowVisibleSingleLineInputsForGenericMaterialBatchBlankCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic process material matrix")
                .rowCount(1)
                .columnCount(10)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder()
                                .text("生产操作及自检记录")
                                .rowSpan(4)
                                .colSpan(1)
                                .widthPx(60)
                                .heightPx(24)
                                .bold(true)
                                .build(),
                        MesProBatchRecordParsedCell.builder()
                                .text("""
                                        物料编码
                                        物料名称
                                        批号
                                        物料编码
                                        物料名称
                                        批号
                                        /
                                        三通旋塞-I
                                        /
                                        吸塑盒
                                        /
                                        顶头袋
                                        /
                                        面纸
                                        /
                                        说明书
                                        /
                                        彩盒
                                        /
                                        中盒
                                        /
                                        大箱
                                        """)
                                .rowSpan(1)
                                .colSpan(9)
                                .widthPx(900)
                                .heightPx(96)
                                .build()
                )))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(table);
        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        List<String> materialNames = List.of("三通旋塞-I", "吸塑盒", "顶头袋", "面纸", "说明书", "彩盒", "中盒", "大箱");
        List<JSONObject> materialBatchBlankCells = new ArrayList<>();
        findRenderedRowIndexes(root,
                row -> materialNames.stream().anyMatch(name -> renderedRowText(row).contains(name)))
                .forEach(rowIndex -> {
                    JSONObject cells = root.getJSONObject("rows")
                            .getJSONObject(String.valueOf(rowIndex))
                            .getJSONObject("cells");
                    for (String columnKey : cells.keySet()) {
                        JSONObject cell = cells.getJSONObject(columnKey);
                        if (cell != null && "".equals(cell.getString("text"))) {
                            materialBatchBlankCells.add(cell);
                        }
                    }
                });

        assertEquals(8, materialBatchBlankCells.size());
        for (JSONObject cell : materialBatchBlankCells) {
            JSONObject fillForm = cell.getJSONObject("fillForm");
            assertNotNull(fillForm);
            assertEquals("input-text", fillForm.getString("componentFlag"));
            assertEquals("请填写", fillForm.getString("placeholder"));
        }
    }

    @Test
    void build_pressurePumpSinglePackaging_shouldShowVisibleBatchInputsInMaterialMatrix() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE), "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable singlePackaging = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(singlePackaging);
        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        List<String> materialNames = List.of("三通旋塞", "顶头袋", "面纸", "吸塑盒", "条形码");
        List<JSONObject> visibleBatchInputs = new ArrayList<>();
        findRenderedRowIndexes(root, row -> materialNames.stream().anyMatch(name -> renderedRowText(row).contains(name)))
                .forEach(rowIndex -> {
                    JSONObject cells = root.getJSONObject("rows")
                            .getJSONObject(String.valueOf(rowIndex))
                            .getJSONObject("cells");
                    for (String columnKey : cells.keySet()) {
                        JSONObject cell = cells.getJSONObject(columnKey);
                        JSONObject fillForm = cell == null ? null : cell.getJSONObject("fillForm");
                        if (cell != null && "".equals(cell.getString("text"))
                                && fillForm != null
                                && "input-text".equals(fillForm.getString("componentFlag"))
                                && "请填写".equals(fillForm.getString("placeholder"))) {
                            visibleBatchInputs.add(cell);
                        }
                    }
                });

        assertTrue(visibleBatchInputs.size() >= 5,
                "single packaging material matrix batch cells should be visible fillable inputs");
    }

    @Test
    void build_shouldNotSynthesizeBlankCellsOutsideSourceCoverageBounds() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic-source-coverage")
                .rowCount(1)
                .columnCount(6)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("标签").widthPx(80).build(),
                        MesProBatchRecordParsedCell.builder().text("").widthPx(80).build()
                )))
                .build();

        JSONObject row0 = JSON.parseObject(builder.build(table, REPORT_CODE))
                .getJSONObject("rows")
                .getJSONObject("0")
                .getJSONObject("cells");

        assertTrue(row0.getJSONObject("1").containsKey("fillForm"));
        assertFalse(row0.containsKey("2"));
        assertFalse(row0.containsKey("5"));
    }

    @Test
    void build_shouldRenderWhitespaceForSuppressedBlankFillableCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("summary")
                .rowCount(1)
                .columnCount(3)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("生产批量汇总").widthPx(120).bold(true).build(),
                        MesProBatchRecordParsedCell.builder()
                                .text("")
                                .widthPx(120)
                                .fontSize(10)
                                .horizontalAlign("center")
                                .verticalAlign("middle")
                                .fillable(true)
                                .placeholder("")
                                .inputType(MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT)
                                .build(),
                        MesProBatchRecordParsedCell.builder().text("2").widthPx(120).build()
                )))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject blankCell = root.getJSONObject("rows")
                .getJSONObject("0")
                .getJSONObject("cells")
                .getJSONObject("1");

        assertEquals(" ", blankCell.getString("text"));
        assertFalse(blankCell.containsKey("fillForm"));
    }

    @Test
    void build_shouldRespectExplicitCenterAlignForMultilineProcessHeader() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("rough")
                .rowCount(1)
                .columnCount(20)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder()
                                .text("粗洗工序生产记录\n□关键/特殊工序   ☑非关键/特殊工序")
                                .colSpan(20)
                                .widthPx(1040)
                                .heightPx(36)
                                .fontSize(10)
                                .bold(true)
                                .horizontalAlign("center")
                                .verticalAlign("middle")
                                .build()
                )))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);
        JSONObject headerCell = root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells").getJSONObject("0");
        JSONObject headerStyle = root.getJSONArray("styles").getJSONObject(headerCell.getIntValue("style"));

        assertEquals("center", headerStyle.getString("align"));
        assertEquals("middle", headerStyle.getString("valign"));
    }

    @Test
    void build_shouldGenerateFillFormsForLiveRouteARoughWashProcessPage() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteARecognizer recognizer = new MesProBatchRecordRouteARecognizer(parser);
        MesProBatchRecordParsedTable parsedTable = recognizer.recognize(
                        FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(item -> "粗洗工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int fillFormCount = countFillFormControls(root);

        assertTrue(fillFormCount >= 20,
                "live A-route rough wash process page must expose enough editable cells for eDHR execution, fillFormCount="
                        + fillFormCount);
    }

    @Test
    void build_shouldReducePlaceholderDensityForAutoFilledStructuralBlankCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("placeholder")
                .rowCount(1)
                .columnCount(3)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("标签").widthPx(80).build(),
                        MesProBatchRecordParsedCell.builder().text("").widthPx(80).build()
                )))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject row0 = JSON.parseObject(json).getJSONObject("rows").getJSONObject("0").getJSONObject("cells");

        assertEquals("", row0.getJSONObject("1").getString("text"));
        assertEquals("\u8bf7\u586b\u5199", row0.getJSONObject("1").getJSONObject("fillForm").getString("placeholder"));
        assertFalse(row0.containsKey("2"));
    }

    @Test
    void build_shouldCompactDenseFillGridControlsAndHideCompactPlaceholders() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("overview-fill-grid")
                .rowCount(4)
                .columnCount(6)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("工序名称").widthPx(176).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("操作人员").widthPx(176).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("装配日期").widthPx(176).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("工序名称").widthPx(176).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("操作人员").widthPx(176).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("装配日期").widthPx(176).bold(true).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("粗洗").widthPx(176).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("硅化II").widthPx(176).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("精洗").widthPx(176).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("检测").widthPx(176).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("记录人/日期").widthPx(176).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("复核人/日期").widthPx(176).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(176).heightPx(24).build()
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject fillFormLayout = root.getJSONObject("fillFormInfo").getJSONObject("layout");
        JSONObject row1 = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject row3 = root.getJSONObject("rows").getJSONObject("3").getJSONObject("cells");

        assertTrue(fillFormLayout.getIntValue("width") <= 96,
                "layout width should compact for dense fill grids");
        assertTrue(fillFormLayout.getIntValue("height") <= 24,
                "layout height should compact for dense fill grids");
        assertEquals("", row1.getJSONObject("1").getJSONObject("fillForm").getString("placeholder"));
        assertEquals("", row1.getJSONObject("2").getJSONObject("fillForm").getString("placeholder"));
        assertEquals("", row3.getJSONObject("1").getJSONObject("fillForm").getString("placeholder"));
        assertEquals("", row3.getJSONObject("2").getJSONObject("fillForm").getString("placeholder"));
        assertEquals(false, row1.getJSONObject("1").getJSONObject("fillForm")
                .getJSONObject("props").getBooleanValue("border"));
        assertEquals("small", row1.getJSONObject("1").getJSONObject("fillForm")
                .getJSONObject("props").getString("size"));
        assertTrue(root.getJSONObject("rows").getJSONObject("1").getIntValue("height")
                        >= fillFormLayout.getIntValue("height"),
                "row height should not be shorter than dense fill control height");
        JSONObject submitSignature = row3.getJSONObject("1").getJSONObject("edhrSignature");
        JSONObject approveSignature = row3.getJSONObject("4").getJSONObject("edhrSignature");
        assertNotNull(submitSignature, "记录人/日期填写格必须携带签名位，避免只读展示退化为普通日期。");
        assertNotNull(approveSignature, "复核人/日期填写格必须携带签名位，避免只读展示退化为普通日期。");
        assertEquals(true, submitSignature.getBoolean("enabled"));
        assertEquals("SUBMIT", submitSignature.getString("actionType"));
        assertEquals("记录人/日期", submitSignature.getString("label"));
        assertEquals("ACTOR_SIGNED_AT", submitSignature.getString("displayFormat"));
        assertEquals(true, approveSignature.getBoolean("enabled"));
        assertEquals("APPROVE", approveSignature.getString("actionType"));
        assertEquals("复核人/日期", approveSignature.getString("label"));
        assertEquals("ACTOR_SIGNED_AT", approveSignature.getString("displayFormat"));
    }

    @Test
    void build_shouldGrowVisibleEditablePlaceholderRowsToInputControlHeight() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("visible-input-grid")
                .rowCount(2)
                .columnCount(3)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("图号").widthPx(90).heightPx(22).build(),
                                MesProBatchRecordParsedCell.builder()
                                        .text("")
                                        .widthPx(120)
                                        .heightPx(22)
                                        .placeholder("请填写")
                                        .build(),
                                MesProBatchRecordParsedCell.builder().text("生产周期").widthPx(90).heightPx(22).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("记录人").widthPx(90).heightPx(22).build(),
                                MesProBatchRecordParsedCell.builder()
                                        .text("")
                                        .widthPx(120)
                                        .heightPx(22)
                                        .placeholder("请填写")
                                        .build(),
                                MesProBatchRecordParsedCell.builder().text("复核人").widthPx(90).heightPx(22).build()
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject fillFormLayout = root.getJSONObject("fillFormInfo").getJSONObject("layout");

        assertEquals(36, fillFormLayout.getIntValue("height"),
                "visible fill controls must use an input-sized layout height");
        assertEquals("请填写", root.getJSONObject("rows").getJSONObject("0")
                .getJSONObject("cells").getJSONObject("1")
                .getJSONObject("fillForm").getString("placeholder"));
        assertTrue(root.getJSONObject("rows").getJSONObject("0").getIntValue("height") >= 36,
                "row height must fit the visible 请填写 input control");
        assertTrue(root.getJSONObject("rows").getJSONObject("1").getIntValue("height") >= 36,
                "all visible editable rows must fit the input control");
    }

    @Test
    void build_shouldNotCreateFillFormControlsForTrailingPaddingColumns() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("rectangular-padding")
                .rowCount(3)
                .columnCount(6)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("列1").widthPx(120).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("列2").widthPx(120).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("列3").widthPx(120).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("列4").widthPx(120).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("列5").widthPx(120).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("列6").widthPx(120).bold(true).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("项目").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("结果").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(24).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("备注").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(24).build()
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject row1 = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");

        assertTrue(row1.getJSONObject("1").containsKey("fillForm"));
        assertTrue(row1.getJSONObject("3").containsKey("fillForm"));
        assertFalse(row1.containsKey("4"),
                "trailing padding column 4 should not synthesize a blank cell");
        assertFalse(row1.containsKey("5"),
                "trailing padding column 5 should not synthesize a blank cell");
    }

    @Test
    void build_shouldNotAutoFillSummaryPaddingCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("summary-row")
                .rowCount(2)
                .columnCount(6)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("操作日期").widthPx(120).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("数量").widthPx(120).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("结果").widthPx(120).bold(true).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("生产批量汇总").widthPx(120).colSpan(2).bold(true).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(24).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(24).build()
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject row1 = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");

        assertTrue(!row1.getJSONObject("2").containsKey("fillForm"));
        assertTrue(!row1.getJSONObject("3").containsKey("fillForm"));
    }

    @Test
    void build_shouldPromoteWideBlankNarrativeAreasToTextareaWithMoreWhitespace() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("narrative-area")
                .rowCount(1)
                .columnCount(4)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("\u8bf4\u660e").widthPx(80).build(),
                        MesProBatchRecordParsedCell.builder()
                                .text("")
                                .colSpan(3)
                                .widthPx(360)
                                .heightPx(36)
                                .build()
                )))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject narrativeCell = root.getJSONObject("rows")
                .getJSONObject("0")
                .getJSONObject("cells")
                .getJSONObject("1");

        assertEquals("input-textarea", narrativeCell.getJSONObject("fillForm").getString("componentFlag"));
        assertEquals(40, root.getJSONObject("rows").getJSONObject("0").getIntValue("height"));
    }

    @Test
    void build_shouldUseTextareaFillFormForTallOrMergedBlankCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("textarea")
                .rowCount(1)
                .columnCount(2)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder()
                                .text("")
                                .rowSpan(2)
                                .widthPx(120)
                                .heightPx(60)
                                .build(),
                        MesProBatchRecordParsedCell.builder().text("标签").widthPx(80).build()
                )))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject cell = JSON.parseObject(json)
                .getJSONObject("rows")
                .getJSONObject("0")
                .getJSONObject("cells")
                .getJSONObject("0");

        assertEquals("", cell.getString("text"));
        assertEquals("Input", cell.getJSONObject("fillForm").getString("component"));
        assertEquals("input-textarea", cell.getJSONObject("fillForm").getString("componentFlag"));
        assertEquals("\u8bf7\u586b\u5199", cell.getJSONObject("fillForm").getString("placeholder"));
    }

    @Test
    void build_shouldKeepDetailDataBlankInputsVisuallyQuietWithoutDroppingFillForm() {
        MesProBatchRecordParsedCell mergedBlankBatchCell = MesProBatchRecordParsedCell.builder()
                .text("")
                .rowSpan(2)
                .widthPx(120)
                .heightPx(60)
                .build();
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic detail")
                .rowCount(3)
                .columnCount(9)
                .rows(List.of(
                        row("操作日期", "物料编码", "物料名称", "批号", "生产数量/pcs",
                                "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("2026-05-17").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("/").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("压力表").widthPx(120).build(),
                                mergedBlankBatchCell,
                                MesProBatchRecordParsedCell.builder().text("120").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("118").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("2").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("2026-05-18").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("/").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("压力表").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("120").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("118").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("2").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).build()
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject detailRow = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject slashCell = detailRow.getJSONObject("1");
        JSONObject batchCell = detailRow.getJSONObject("3");
        JSONObject operatorCell = detailRow.getJSONObject("7");

        assertEquals("/", slashCell.getString("text"));
        assertFalse(slashCell.containsKey("fillForm"), "slash-only marker cells should remain visual text");
        assertNotNull(batchCell.getJSONObject("fillForm"), "blank detail cells should still be fillable");
        assertEquals("input-textarea", batchCell.getJSONObject("fillForm").getString("componentFlag"));
        assertEquals("", batchCell.getJSONObject("fillForm").getString("placeholder"));
        assertEquals(false, batchCell.getJSONObject("fillForm").getJSONObject("props").getBooleanValue("border"));
        assertEquals("small", batchCell.getJSONObject("fillForm").getJSONObject("props").getString("size"));
        assertNotNull(operatorCell.getJSONObject("fillForm"), "operator/date blank should still be fillable");
        assertEquals("", operatorCell.getJSONObject("fillForm").getString("placeholder"));
        assertEquals(false, operatorCell.getJSONObject("fillForm").getJSONObject("props").getBooleanValue("border"));
        assertEquals("small", operatorCell.getJSONObject("fillForm").getJSONObject("props").getString("size"));
    }

    @Test
    void build_shouldGenerateFillFormForPackedMaterialMatrixBlankBatchCells() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("generic packed material matrix")
                .rowCount(3)
                .columnCount(7)
                .rows(List.of(
                        List.of(sourceBackedCell("通用工序生产记录", 1, 7, 420, 24, false, false, true)),
                        List.of(
                                sourceBackedCell("生产操作及自检记录", 1, 1, 60, 24, false, false, true),
                                sourceBackedCell("""
                                                物料编码
                                                物料名称
                                                批号
                                                物料编码
                                                物料名称
                                                批号
                                                /
                                                三通旋塞-I
                                                /
                                                吸塑盒
                                                 /
                                                 顶头袋
                                                 /
                                                 条形码
                                                 /
                                                 说明书
                                                 """,
                                         1, 6, 360, 96, false, false, false)
                         )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(calibrator.calibrate(parsedTable), REPORT_CODE));
        int firstMaterialRowIndex = findRenderedRowIndexByText(root, "三通旋塞-I");
        JSONObject cells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(firstMaterialRowIndex))
                .getJSONObject("cells");
        JSONObject leftBatchCell = cells.getJSONObject("3");
        JSONObject rightBatchCell = cells.getJSONObject("6");

        assertEquals("/", cells.getJSONObject("1").getString("text"));
        assertEquals("三通旋塞-I", cells.getJSONObject("2").getString("text"));
        assertEquals("/", cells.getJSONObject("4").getString("text"));
        assertEquals("吸塑盒", cells.getJSONObject("5").getString("text"));
        assertNotNull(leftBatchCell.getJSONObject("fillForm"),
                "blank batch cells expanded from a packed material matrix must be editable");
        assertNotNull(rightBatchCell.getJSONObject("fillForm"),
                "both repeated material groups must keep the blank batch input");
        assertEquals("input-text", leftBatchCell.getJSONObject("fillForm").getString("componentFlag"));
        assertEquals("请填写", leftBatchCell.getJSONObject("fillForm").getString("placeholder"));

        int oddMaterialRowIndex = findRenderedRowIndexByText(root, "说明书");
        JSONObject oddMaterialCells = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(oddMaterialRowIndex))
                .getJSONObject("cells");
        assertNotNull(oddMaterialCells.getJSONObject("3").getJSONObject("fillForm"),
                "odd packed material row must keep the left batch blank editable");
        assertNotNull(oddMaterialCells.getJSONObject("5").getJSONObject("fillForm"),
                "odd packed material row must keep the empty right material slot editable");
        assertNotNull(oddMaterialCells.getJSONObject("6").getJSONObject("fillForm"),
                "odd packed material row must keep the empty right batch slot editable");
    }

    @Test
    void build_shouldTreatDiagonalSlashCellsAsForbiddenWithoutDroppingNormalInputs() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("cleaning forbidden cells")
                .rowCount(2)
                .columnCount(3)
                .rows(List.of(
                        row("清洗功率", "清洗温度", "清洗时间"),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("100%").widthPx(120).heightPx(28).build(),
                                MesProBatchRecordParsedCell.builder()
                                        .text("")
                                        .widthPx(120)
                                        .heightPx(28)
                                        .fillable(true)
                                        .diagonalSlash(true)
                                        .build(),
                                MesProBatchRecordParsedCell.builder()
                                        .text("")
                                        .widthPx(120)
                                        .heightPx(28)
                                        .fillable(true)
                                        .build()
                        )
                ))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject cells = root.getJSONObject("rows").getJSONObject("1").getJSONObject("cells");
        JSONObject forbiddenCell = cells.getJSONObject("1");
        JSONObject normalBlankCell = cells.getJSONObject("2");

        assertEquals(true, forbiddenCell.getBoolean("edhrDiagonalSlash"));
        assertFalse(forbiddenCell.containsKey("fillForm"),
                "diagonal slash cells are forbidden and must not become fillable controls");
        assertNotNull(normalBlankCell.getJSONObject("fillForm"),
                "ordinary blank fillable cells in the same row must stay editable");
    }

    @Test
    void build_shouldKeepRepeatedEquipmentMatrixBlankInputsVisuallyQuietWithoutDroppingFillForm() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic repeated equipment")
                .rowCount(1)
                .columnCount(10)
                .rows(List.of(repeatedEquipmentMatrixRow()))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject cells = root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells");
        JSONObject firstBlankCell = cells.getJSONObject("0");
        JSONObject middleBlankCell = cells.getJSONObject("5");
        JSONObject equipmentCell = cells.getJSONObject("2");

        assertTrue(equipmentCell.getString("text").contains("封口热合机"));
        assertNotNull(firstBlankCell.getJSONObject("fillForm"), "blank equipment matrix cells should still be fillable");
        assertEquals("", firstBlankCell.getJSONObject("fillForm").getString("placeholder"));
        assertEquals(false, firstBlankCell.getJSONObject("fillForm").getJSONObject("props").getBooleanValue("border"));
        assertEquals("small", firstBlankCell.getJSONObject("fillForm").getJSONObject("props").getString("size"));
        assertNotNull(middleBlankCell.getJSONObject("fillForm"), "all blank cells in the matrix row should stay fillable");
        assertEquals("", middleBlankCell.getJSONObject("fillForm").getString("placeholder"));
        assertEquals(false, middleBlankCell.getJSONObject("fillForm").getJSONObject("props").getBooleanValue("border"));
        assertEquals("small", middleBlankCell.getJSONObject("fillForm").getJSONObject("props").getString("size"));
    }

    @Test
    void build_shouldAutoFillStructuredHeaderBlankCellsWhenRowCarriesEntryCues() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("structured header blanks")
                .rowCount(1)
                .columnCount(10)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("设备编码").widthPx(96).build(),
                        MesProBatchRecordParsedCell.builder().text("设备名称").widthPx(120).build(),
                        MesProBatchRecordParsedCell.builder().text("设备型号").widthPx(120).build(),
                        MesProBatchRecordParsedCell.builder().text("设备编号").widthPx(96).build(),
                        MesProBatchRecordParsedCell.builder().text("是否在计量效期内").widthPx(132).build(),
                        MesProBatchRecordParsedCell.builder().text("").widthPx(72).build(),
                        MesProBatchRecordParsedCell.builder().text("操作人").widthPx(84).build(),
                        MesProBatchRecordParsedCell.builder().text("").widthPx(72).build(),
                        MesProBatchRecordParsedCell.builder().text("复核人").widthPx(84).build(),
                        MesProBatchRecordParsedCell.builder().text("").widthPx(72).build()
                )))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject cells = root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells");

        assertNotNull(cells.getJSONObject("5").getJSONObject("fillForm"),
                "structured header blank after entry cue should stay fillable");
        assertNotNull(cells.getJSONObject("7").getJSONObject("fillForm"),
                "operator blank in structured header should stay fillable");
        assertNotNull(cells.getJSONObject("9").getJSONObject("fillForm"),
                "reviewer blank in structured header should stay fillable");
    }

    @Test
    void build_shouldKeepPureSpacerRowsBlank() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("spacer")
                .rowCount(2)
                .columnCount(2)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder().text("标签").widthPx(80).build()),
                        List.of()
                ))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject row1 = JSON.parseObject(json).getJSONObject("rows").getJSONObject("1").getJSONObject("cells");

        assertEquals(" ", row1.getJSONObject("0").getString("text"));
        assertEquals(" ", row1.getJSONObject("1").getString("text"));
        assertTrue(!row1.getJSONObject("0").containsKey("fillForm"));
        assertTrue(!row1.getJSONObject("1").containsKey("fillForm"));
    }

    @Test
    void build_shouldConstrainTallRowsAndLargeFontForSingleBrowserPage() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (int index = 0; index < 28; index++) {
            rows.add(List.of(MesProBatchRecordParsedCell.builder()
                    .text("需要填写的长文本内容-" + index)
                    .fontSize(16)
                    .widthPx(180)
                    .heightPx(80)
                    .build()));
        }
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("single-page")
                .rowCount(rows.size())
                .columnCount(1)
                .rows(rows)
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);

        int totalHeight = 0;
        for (String key : root.getJSONObject("rows").keySet()) {
            if (!key.chars().allMatch(Character::isDigit)) {
                continue;
            }
            totalHeight += root.getJSONObject("rows").getJSONObject(key).getIntValue("height");
        }
        int styleIndex = root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells")
                .getJSONObject("0").getIntValue("style");
        int fontSize = root.getJSONArray("styles").getJSONObject(styleIndex).getJSONObject("font").getIntValue("size");

        assertTrue(totalHeight <= MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX);
        assertTrue(fontSize <= MesProBatchRecordReportShapeRules.MAX_RENDER_FONT_SIZE);
        assertTrue(root.getJSONObject("rows").getJSONObject("0").getIntValue("height")
                <= MesProBatchRecordReportShapeRules.MAX_ROW_HEIGHT_PX);
    }

    @Test
    void build_shouldExposeFillToolbarAndNoSubmitHandlers() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("fill")
                .rowCount(1)
                .columnCount(1)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("").widthPx(80).build()
                )))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);

        assertEquals(false, root.getJSONObject("fillFormToolbar").getBooleanValue("show"));
        assertNotNull(root.getJSONObject("fillFormInfo"));
        assertTrue(root.getJSONArray("submitHandlers").isEmpty());
        assertTrue(root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells")
                .getJSONObject("0").containsKey("fillForm"));
    }

    @Test
    void build_shouldRenderBorderlessEffectiveDateFooterWithoutBoxBorder() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("rough-footer")
                .rowCount(1)
                .columnCount(20)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder()
                                .text("生效日期：2026年02月02日")
                                .colSpan(20)
                                .widthPx(640)
                                .horizontalAlign("left")
                                .fontSize(8)
                                .borderless(true)
                                .build()
                )))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);
        JSONObject footerCell = root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells").getJSONObject("0");
        JSONObject footerStyle = root.getJSONArray("styles").getJSONObject(footerCell.getIntValue("style"));

        assertEquals("生效日期：2026年02月02日", footerCell.getString("text"));
        assertEquals("left", footerStyle.getString("align"));
        assertTrue(!footerStyle.containsKey("border"));
    }

    @Test
    void build_shouldApplyExplicitBackgroundColorFromParsedCell() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("rough-header")
                .rowCount(1)
                .columnCount(2)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder()
                                .text("生产批号")
                                .widthPx(120)
                                .bold(true)
                                .backgroundColor("#d9d9d9")
                                .build(),
                        MesProBatchRecordParsedCell.builder()
                                .text("")
                                .widthPx(120)
                                .build()
                )))
                .build();

        String json = builder.build(table, REPORT_CODE);
        JSONObject root = JSON.parseObject(json);
        JSONObject headerStyle = root.getJSONArray("styles").getJSONObject(
                root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells").getJSONObject("0").getIntValue("style"));

        assertEquals("#d9d9d9", headerStyle.getString("bgcolor"));
    }

    @Test
    void build_shouldAccentPcsTextGenericallyWithoutTemplateBranching() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(3)
                .tableTitle("generic-pcs")
                .rowCount(1)
                .columnCount(3)
                .rows(List.of(List.of(
                        MesProBatchRecordParsedCell.builder().text("生产数量/pcs").widthPx(80).bold(true).build(),
                        MesProBatchRecordParsedCell.builder().text("自检合格数量/pcs").widthPx(80).bold(true).build(),
                        MesProBatchRecordParsedCell.builder().text("操作人").widthPx(80).bold(true).build()
                )))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject pcsStyleA = styleOf(root, 0, 0);
        JSONObject pcsStyleB = styleOf(root, 0, 1);
        JSONObject regularStyle = styleOf(root, 0, 2);

        assertEquals("#c00000", pcsStyleA.getString("color"));
        assertEquals("#c00000", pcsStyleB.getString("color"));
        assertTrue(!regularStyle.containsKey("color"));
    }

    @Test
    void build_shouldHonorExplicitSourceBorderStylesWhenPresent() {
        MesProBatchRecordParsedCell bordered = MesProBatchRecordParsedCell.builder()
                .text("源边框")
                .widthPx(120)
                .topBorderStyle("")
                .bottomBorderStyle("medium")
                .leftBorderStyle("thin")
                .rightBorderStyle("")
                .build();
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("explicit-border")
                .rowCount(1)
                .columnCount(1)
                .rows(List.of(List.of(bordered)))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject style = styleOf(root, 0, 0);
        JSONObject border = style.getJSONObject("border");

        assertEquals("", border.getJSONArray("top").getString(0));
        assertEquals("medium", border.getJSONArray("bottom").getString(0));
        assertEquals("thin", border.getJSONArray("left").getString(0));
        assertEquals("", border.getJSONArray("right").getString(0));
    }

    @Test
    void build_shouldDifferentiateOuterFrameSectionAndGridBordersFromCellShape() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(3)
                .tableTitle("generic-border")
                .rowCount(3)
                .columnCount(3)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("Section")
                                .colSpan(3)
                                .widthPx(240)
                                .bold(true)
                                .horizontalAlign("center")
                                .build()),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("H1").widthPx(80).bold(true).backgroundColor("#d9d9d9").build(),
                                MesProBatchRecordParsedCell.builder().text("H2").widthPx(80).bold(true).backgroundColor("#d9d9d9").build(),
                                MesProBatchRecordParsedCell.builder().text("H3").widthPx(80).bold(true).backgroundColor("#d9d9d9").build()
                        ),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("A").widthPx(80).build(),
                                MesProBatchRecordParsedCell.builder().text("B").widthPx(80).build(),
                                MesProBatchRecordParsedCell.builder().text("C").widthPx(80).build()
                        )))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        JSONObject sectionStyle = styleOf(root, 0, 0);
        JSONObject headerStyle = styleOf(root, 1, 1);
        JSONObject gridStyle = styleOf(root, 2, 1);

        assertEquals("thick", borderWeight(sectionStyle, "top"));
        assertEquals("medium", borderWeight(sectionStyle, "bottom"));
        assertEquals("thick", borderWeight(sectionStyle, "left"));
        assertEquals("thick", borderWeight(sectionStyle, "right"));

        assertEquals("medium", borderWeight(headerStyle, "top"));
        assertEquals("medium", borderWeight(headerStyle, "bottom"));
        assertEquals("thin", borderWeight(headerStyle, "left"));
        assertEquals("thin", borderWeight(headerStyle, "right"));

        assertEquals("thin", borderWeight(gridStyle, "top"));
        assertEquals("thick", borderWeight(gridStyle, "bottom"));
        assertEquals("thin", borderWeight(gridStyle, "left"));
        assertEquals("thin", borderWeight(gridStyle, "right"));
    }

    @Test
    void build_shouldKeepOuterFrameHierarchyForSourceBackedBlankCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(4)
                .tableTitle("generic-fill")
                .rowCount(2)
                .columnCount(3)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("Section")
                                .colSpan(3)
                                .widthPx(240)
                                .bold(true)
                                .horizontalAlign("center")
                                .build()),
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("Label").widthPx(80).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(80).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(80).build()
                        )))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        JSONObject autoFilledBlankStyle = styleOf(root, 1, 2);

        assertEquals("thin", borderWeight(autoFilledBlankStyle, "top"));
        assertEquals("thick", borderWeight(autoFilledBlankStyle, "right"));
        assertEquals("thick", borderWeight(autoFilledBlankStyle, "bottom"));
    }

    @Test
    void build_shouldPreserveCheckedStateInChecklistRow() {
        for (String sourceText : List.of(
                "☑关键/特殊工序   □非关键/特殊工序",
                "□关键/特殊工序   ☑非关键/特殊工序")) {
            MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                    .tableTitle("generic-checklist")
                    .rowCount(1)
                    .columnCount(20)
                    .rows(List.of(List.of(
                            MesProBatchRecordParsedCell.builder()
                                    .text(sourceText)
                                    .colSpan(20)
                                    .widthPx(640)
                                    .build()
                    )))
                    .build();

            String json = builder.build(table, REPORT_CODE);
            JSONObject root = JSON.parseObject(json);
            String text = root.getJSONObject("rows").getJSONObject("0").getJSONObject("cells")
                    .getJSONObject("0").getString("text");

            assertEquals(sourceText, text);
            assertFalse(sourceText.replace('☑', '□').equals(text));
        }
    }

    @Test
    void build_shouldPreserveMoreHeightForSummaryRowsThanDetailRowsWhenFittingSinglePage() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(List.of(MesProBatchRecordParsedCell.builder()
                .text("\u901a\u7528\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")
                .colSpan(20)
                .widthPx(1040)
                .heightPx(30)
                .bold(true)
                .build()));
        rows.add(List.of(
                MesProBatchRecordParsedCell.builder().text("\u751f\u4ea7\u6279\u53f7").widthPx(120).heightPx(26).build(),
                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(26).build(),
                MesProBatchRecordParsedCell.builder().text("\u4ea7\u54c1\u89c4\u683c").widthPx(120).heightPx(26).build(),
                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(26).build(),
                MesProBatchRecordParsedCell.builder().text("\u751f\u4ea7\u4f9d\u636e").widthPx(120).heightPx(26).build(),
                MesProBatchRecordParsedCell.builder().text("").widthPx(120).heightPx(26).build()
        ));
        rows.add(List.of(
                MesProBatchRecordParsedCell.builder().text("\u64cd\u4f5c\u65e5\u671f").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u7269\u6599\u7f16\u7801").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u7269\u6599\u540d\u79f0").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u6279\u53f7").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u751f\u4ea7\u6570\u91cf/pcs").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u81ea\u68c0\u5408\u683c\u6570\u91cf/pcs").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u4e0d\u5408\u683c\u6570\u91cf/pcs").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u64cd\u4f5c\u4eba").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u590d\u6838\u4eba").widthPx(120).heightPx(26).bold(true).build()
        ));
        for (int index = 0; index < 30; index++) {
            rows.add(List.of(
                    MesProBatchRecordParsedCell.builder().text("2026-05-17").widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("A00" + index).widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("\u5f39\u7c27").widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("B-" + index).widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("120").widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("118").widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("2").widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("\u5f20\u4e09").widthPx(120).heightPx(26).build(),
                    MesProBatchRecordParsedCell.builder().text("\u674e\u56db").widthPx(120).heightPx(26).build()
            ));
        }
        rows.add(List.of(
                MesProBatchRecordParsedCell.builder().text("\u751f\u4ea7\u6279\u91cf\u6c47\u603b").widthPx(180).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("\u5408\u683c\u6570\u91cf").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("118").widthPx(120).heightPx(26).build(),
                MesProBatchRecordParsedCell.builder().text("\u4e0d\u5408\u683c\u6570\u91cf").widthPx(120).heightPx(26).bold(true).build(),
                MesProBatchRecordParsedCell.builder().text("2").widthPx(120).heightPx(26).build()
        ));
        rows.add(List.of(MesProBatchRecordParsedCell.builder()
                .text("\u751f\u6548\u65e5\u671f\uff1a2026\u5e7402\u670802\u65e5")
                .colSpan(20)
                .widthPx(1040)
                .heightPx(22)
                .horizontalAlign("left")
                .borderless(true)
                .build()));

        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("\u901a\u7528\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")
                .rowCount(rows.size())
                .columnCount(20)
                .rows(rows)
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        int detailHeight = root.getJSONObject("rows").getJSONObject("3").getIntValue("height");
        int summaryHeight = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(rows.size() - 2))
                .getIntValue("height");

        assertTrue(detailHeight <= 18);
        assertTrue(summaryHeight >= 22);
        assertTrue(summaryHeight > detailHeight);
    }

    @Test
    void build_shouldAllowLiveLikeDenseProcessPagesToExceedTheGeneric650pxJsonCap() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-B source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();
        MesProBatchRecordParsedTable parsedTable = recognizer.recognize(FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(item -> "精洗工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable table = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        int totalHeight = root.getJSONObject("rows").keySet().stream()
                .filter(key -> !"len".equals(key))
                .map(root.getJSONObject("rows")::getJSONObject)
                .mapToInt(row -> row.getIntValue("height"))
                .sum();

        int sourceTotalHeight = table.getRows().stream()
                .mapToInt(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0))
                .sum();

        assertEquals(sourceTotalHeight, totalHeight,
                "source-grid process pages should keep Word source row-height rhythm instead of synthetic expansion");
        assertTrue(totalHeight > MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX,
                "totalHeight=" + totalHeight);
    }

    @Test
    void build_shouldFollowSourceHeightForLiveLikeMediumProcessPages() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-B source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();
        MesProBatchRecordParsedTable parsedTable = recognizer.recognize(FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(item -> "清洁工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable table = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));
        int totalHeight = root.getJSONObject("rows").keySet().stream()
                .filter(key -> !"len".equals(key))
                .map(root.getJSONObject("rows")::getJSONObject)
                .mapToInt(row -> row.getIntValue("height"))
                .sum();
        int sourceTotalHeight = table.getRows().stream()
                .mapToInt(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0))
                .sum();

        assertEquals(sourceTotalHeight, totalHeight,
                "live-like medium process pages should keep source row-height rhythm");
        assertTrue(totalHeight <= Math.max(670, sourceTotalHeight), "totalHeight=" + totalHeight);
    }

    @Test
    void build_shouldKeepSourceHeightForFixedRouteADetectionPage() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "检测工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int totalHeight = root.getJSONObject("rows").keySet().stream()
                .filter(key -> !"len".equals(key))
                .map(root.getJSONObject("rows")::getJSONObject)
                .mapToInt(row -> row.getIntValue("height"))
                .sum();
        int sourceTotalHeight = 0;
        for (int rowIndex = 0; rowIndex < calibrated.getRowCount(); rowIndex++) {
            sourceTotalHeight += rowHeightAt(calibrated, rowIndex);
        }

        assertEquals(sourceTotalHeight, totalHeight,
                "detection page should keep source Word row-height rhythm");
        assertTrue(totalHeight >= 660, "totalHeight=" + totalHeight);
    }

    @Test
    void build_routeAT13_shouldKeepSingleSyntheticHeaderAfterRemoval() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        JSONObject rows = root.getJSONObject("rows");
        long headerRowCount = rows.keySet().stream()
                .filter(key -> !"len".equals(key))
                .map(rows::getJSONObject)
                .filter(Objects::nonNull)
                .map(row -> row.getJSONObject("cells"))
                .filter(Objects::nonNull)
                .filter(cells -> cells.keySet().stream()
                        .map(cells::getJSONObject)
                        .filter(Objects::nonNull)
                        .map(cell -> cell.getString("text"))
                        .filter(Objects::nonNull)
                    .anyMatch(text -> text.contains("球囊扩张压力泵生产记录")))
                .count();

        assertEquals(1L, headerRowCount);
    }

    @Test
    void build_routeAT13_shouldUseLandscapeA4ForFixedWideTemplate() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        JSONObject printConfig = root.getJSONObject("printConfig");

        assertEquals("landscape", printConfig.getString("layout"));
        assertEquals(297, printConfig.getIntValue("width"));
        assertEquals(210, printConfig.getIntValue("height"));
        assertTrue(root.getIntValue("dataRectWidth") > MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_DENSE_BUDGET_PX,
                "landscape wide template should expand render width budget beyond portrait dense budget");
    }

    @Test
    void build_routeAT13_shouldKeepSourceHeightForEquipmentMatrixRows() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int renderedRowIndex = findRenderedRowIndexByText(root, "封口热合机：□A05199\n封口热合机：(A05203\n热合机：□A05048\n自动热合机：(A03274");
        int rowHeight = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(renderedRowIndex))
                .getIntValue("height");
        int sourceHeight = rowHeightAt(calibrated, renderedRowIndex);

        assertEquals(sourceHeight, rowHeight,
                "equipment matrix row height should follow the source Word row height");
    }

    @Test
    void build_routeAT13_shouldKeepRepeatedEquipmentMatrixRowsMultiLineInLandscapeWidePages() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        assertEquals("landscape", root.getJSONObject("printConfig").getString("layout"));

        List<Integer> oneLineMatrixRows = findRenderedRowIndexes(root, row ->
                isRepeatedEquipmentMatrixRenderedRow(row) && !renderedRowText(row).contains("\n"));
        assertTrue(oneLineMatrixRows.isEmpty(),
                "expected repeated equipment matrix rows to stay multi-line in fixed route-A sample");
    }

    @Test
    void build_routeAT13_shouldNotLetVerticalMergeCrossPagingRow() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int pagingRowIndex = findPagingRowIndexOrDefault(root, -1);
        if (pagingRowIndex < 0) {
            return;
        }
        JSONObject rows = root.getJSONObject("rows");

        for (String key : rows.keySet()) {
            if (!key.chars().allMatch(Character::isDigit)) {
                continue;
            }
            int rowIndex = Integer.parseInt(key);
            JSONObject row = rows.getJSONObject(key);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String cellKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(cellKey);
                if (cell == null || !cell.containsKey("merge")) {
                    continue;
                }
                List<Integer> merge = cell.getJSONArray("merge").toJavaList(Integer.class);
                int rowSpan = merge.get(0) + 1;
                int endRowIndex = rowIndex + rowSpan - 1;
                assertFalse(rowIndex < pagingRowIndex && endRowIndex > pagingRowIndex,
                        "merge crosses paging row: startRow=" + rowIndex + ", endRow=" + endRowIndex
                                + ", pagingRow=" + pagingRowIndex + ", text=" + cell.getString("text"));
            }
        }
    }

    @Test
    void build_shouldSplitOversizedRepeatedEquipmentMatrixBandBeforeStructuredTail() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        assertEquals("landscape", root.getJSONObject("printConfig").getString("layout"));

        List<Integer> matrixRowIndexes = findRenderedRowIndexes(root,
                MesProBatchRecordReportJsonBuilderTest::isRepeatedEquipmentMatrixRenderedRow)
                .stream()
                .sorted()
                .toList();

        assertTrue(matrixRowIndexes.size() >= 8, "matrixRowIndexes=" + matrixRowIndexes);
        assertEquals(calibrated.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1),
                "source-backed repeated equipment matrix bands should not gain synthetic paging rows");
    }

    @Test
    void build_routeAT13_shouldAvoidDetachingCompactStructuredTailFromRepeatedMatrix() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(item -> "单包装工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int summaryRowIndex = findRenderedRowIndexByText(root, "生产批量汇总");
        List<Integer> matrixRowIndexes = findRenderedRowIndexes(root,
                MesProBatchRecordReportJsonBuilderTest::isRepeatedEquipmentMatrixRenderedRow)
                .stream()
                .sorted()
                .toList();

        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertTrue(matrixRowIndexes.stream().allMatch(rowIndex -> rowIndex < summaryRowIndex),
                "compact structured tail should stay after the source-backed repeated matrix band");
    }

    @Test
    void build_shouldKeepSourceRowsWhenDetailBandContinuesWithoutSourcePagingRow() {
        MesProBatchRecordParsedTable table = buildRepeatedEquipmentMatrixTable(8, false, true);
        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertEquals("操作日期|设备编码|参数|实际|生产数量/pcs|自检合格数量/pcs|不合格数量/pcs|操作人|复核人",
                renderedRowText(root.getJSONObject("rows").getJSONObject("2")));
        assertTrue(isRepeatedEquipmentMatrixRenderedRow(root.getJSONObject("rows").getJSONObject("3")));
    }

    @Test
    void build_shouldKeepSecondBandHeaderWithinSourceRows() {
        MesProBatchRecordParsedTable table = buildTwoBandRepeatedEquipmentMatrixTable(true);
        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertFalse(findRenderedRowIndexes(root, row -> renderedRowText(row)
                .equals("复核日期|模具编号|项目|目标|首件数量/pcs|巡检数量/pcs|异常数量/pcs|记录人|确认人")).isEmpty());
    }

    @Test
    void build_shouldKeepNonReusableHeaderAsSourceRowWithoutFallbackPaging() {
        MesProBatchRecordParsedTable table = buildTwoBandRepeatedEquipmentMatrixTable(false);
        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertFalse(findRenderedRowIndexes(root, row -> renderedRowText(row)
                .equals("阶段|检查日期|设备|项目|目标|数量/pcs|结果|记录人|确认人")).isEmpty());
    }

    @Test
    void build_shouldKeepPlainTailAsSourceRowWithoutSyntheticPaging() {
        MesProBatchRecordParsedTable table = buildRepeatedEquipmentMatrixTableWithPlainTail();
        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertTrue(findRenderedRowIndexByText(root, "批后确认") >= 0);
    }

    @Test
    void build_shouldKeepVerticalMergeWhenNoSourcePagingRowExists() {
        MesProBatchRecordParsedTable table = buildRepeatedEquipmentMatrixTableWithCrossPagingMerge();
        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        JSONObject sourceMergeCell = root.getJSONObject("rows").getJSONObject("6")
                .getJSONObject("cells").getJSONObject("0");

        assertEquals(table.getRowCount().intValue(), countRenderedDataRows(root));
        assertEquals(-1, findPagingRowIndexOrDefault(root, -1));
        assertNotNull(sourceMergeCell, "source vertical merge should remain on its original row");
        assertEquals("跨页合并批号", sourceMergeCell.getString("text"));
        assertEquals(2, sourceMergeCell.getJSONArray("merge").getIntValue(0),
                "source vertical merge should not be split without a source paging row");
        assertEquals(countSourceMergeRanges(table), root.getJSONArray("merges").size());
        assertEquals(0, root.getJSONArray("fixedPrintHeadRows").getJSONObject(0).getIntValue("sri"));
        assertEquals(1, root.getJSONArray("fixedPrintHeadRows").getJSONObject(0).getIntValue("eri"));
        int footerRowIndex = findRenderedRowIndexByText(root, "生效日期：2026年02月02日");
        assertEquals(footerRowIndex, root.getJSONArray("fixedPrintTailRows").getJSONObject(0).getIntValue("sri"));
        assertEquals(footerRowIndex, root.getJSONArray("fixedPrintTailRows").getJSONObject(0).getIntValue("eri"));
    }

    @Test
    void build_shouldNotSplitShortOrDispersedRepeatedEquipmentMatrixRows() {
        JSONObject shortBandRoot = JSON.parseObject(builder.build(buildRepeatedEquipmentMatrixTable(7, false), REPORT_CODE));
        JSONObject dispersedBandRoot = JSON.parseObject(builder.build(buildRepeatedEquipmentMatrixTable(8, true), REPORT_CODE));

        assertEquals(-1, findPagingRowIndexOrDefault(shortBandRoot, -1),
                "short repeated equipment matrix bands should stay on the same page");
        assertEquals(-1, findPagingRowIndexOrDefault(dispersedBandRoot, -1),
                "dispersed equipment matrix rows should not be treated as one oversized band");
    }

    @Test
    void build_shouldPreserveCompactedOneLineDetailHeightsOnStructuredTailPages() {
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(buildStructuredTailAfterSummaryTable());
        int detailRowIndex = findRowIndex(calibrated.getRows(), "2026-05-17");
        int tailRowIndex = findRowIndex(calibrated.getRows(), "1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；");
        int calibratedDetailHeight = rowHeightAt(calibrated, detailRowIndex);
        int calibratedTailHeight = rowHeightAt(calibrated, tailRowIndex);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int builtDetailRowIndex = findRenderedRowIndexByText(root, "2026-05-17");
        int builtTailRowIndex = findRenderedRowIndexByText(root, "1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；");
        int builtDetailHeight = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(builtDetailRowIndex))
                .getIntValue("height");
        int builtTailHeight = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(builtTailRowIndex))
                .getIntValue("height");

        assertTrue(calibratedDetailHeight <= 18, "calibratedDetailHeight=" + calibratedDetailHeight);
        assertTrue(builtDetailHeight <= calibratedDetailHeight,
                "builtDetailHeight=" + builtDetailHeight + ", calibratedDetailHeight=" + calibratedDetailHeight);
        assertTrue(builtTailHeight >= calibratedTailHeight,
                "builtTailHeight=" + builtTailHeight + ", calibratedTailHeight=" + calibratedTailHeight);
    }

    @Test
    void build_shouldPreserveLiveRepeatedChecklistSourceRowsOnFixedCleanProcessPage() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-B source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();
        MesProBatchRecordParsedTable parsedTable = recognizer.recognize(FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(item -> "清洁工序生产记录".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        int sourceDetailRowIndex = findRowIndexContaining(parsedTable.getRows(), "□30atm压力表");
        int sourceDetailHeight = rowHeightAt(parsedTable, sourceDetailRowIndex);
        int detailRowIndex = findRowIndexContaining(calibrated.getRows(), "□30atm压力表");
        int calibratedDetailHeight = rowHeightAt(calibrated, detailRowIndex);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int builtDetailHeight = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(detailRowIndex))
                .getIntValue("height");

        assertEquals(sourceDetailHeight, calibratedDetailHeight);
        assertEquals(calibratedDetailHeight, builtDetailHeight);
    }

    @Test
    void build_shouldUse1120WidthForFixedOverviewPage() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-B source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();
        MesProBatchRecordParsedTable parsedTable = recognizer.recognize(FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(item -> "产品信息".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

        assertEquals(1120, root.getIntValue("dataRectWidth"));
        assertEquals(1120, root.getJSONObject("area").getIntValue("width"));
    }

    @Test
    void build_shouldKeepActualPressurePumpSummaryMaterialRowsAtVisibleInputHeight() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();
        MesProBatchRecordParsedTable parsedTable = recognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(item -> "产品信息".equals(item.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        JSONObject root = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        int fillFormHeight = root.getJSONObject("fillFormInfo").getJSONObject("layout").getIntValue("height");
        int materialRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("手柄"))
                .stream()
                .findFirst()
                .orElseThrow();
        int materialRowHeight = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(materialRowIndex))
                .getIntValue("height");

        assertTrue(materialRowHeight >= fillFormHeight,
                "summary material rows must not render lower than visible input controls; rowText="
                        + renderedRowText(root.getJSONObject("rows").getJSONObject(String.valueOf(materialRowIndex))));
    }

    @Test
    void build_shouldKeepFullPageWidthForDocParsedLowColumnOverviewPages() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteARecognizer recognizer = new MesProBatchRecordRouteARecognizer(parser);
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(
                FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString());
        MesProBatchRecordParsedTable parsedTable = parsedTables
                .stream()
                .filter(MesProBatchRecordReportJsonBuilderTest::isLowColumnOverviewLikePage)
                .findFirst()
                .orElseThrow(() -> new AssertionError(describeLowColumnPageCandidates(parsedTables)));

        JSONObject root = JSON.parseObject(builder.build(parsedTable, REPORT_CODE));

        assertEquals(MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_NARROW_BUDGET_PX,
                root.getIntValue("dataRectWidth"));
        assertEquals(MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_NARROW_BUDGET_PX,
                root.getJSONObject("area").getIntValue("width"));
        assertEquals("landscape", root.getJSONObject("printConfig").getString("layout"));
    }

    @Test
    void build_shouldPreserveAuthoritativeSourceColumnWidthsForDocParsedProcessPages() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteARecognizer recognizer = new MesProBatchRecordRouteARecognizer(parser);
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(
                FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString());
        List<MesProBatchRecordParsedTable> processPages = parsedTables
                .stream()
                .map(calibrator::calibrate)
                .filter(MesProBatchRecordReportJsonBuilderTest::isSourceBackedProcessLikeFullPage)
                .toList();

        assertTrue(processPages.size() >= 5, describeSourceBackedProcessPageCandidates(parsedTables));
        for (MesProBatchRecordParsedTable parsedTable : processPages) {
            JSONObject root = JSON.parseObject(builder.build(parsedTable, REPORT_CODE));
            assertNotNull(parsedTable.getColumnWidths(),
                    "sourceTableIndex=" + parsedTable.getSourceTableIndex());
            assertEquals(parsedTable.getColumnCount(), parsedTable.getColumnWidths().size(),
                    "sourceTableIndex=" + parsedTable.getSourceTableIndex());
            List<Integer> expectedColumnWidths = expectedRenderedColumnWidths(parsedTable);
            int expectedWidth = expectedColumnWidths.stream().mapToInt(Integer::intValue).sum();

            assertEquals(expectedWidth, root.getIntValue("dataRectWidth"),
                    "sourceTableIndex=" + parsedTable.getSourceTableIndex());
            assertEquals(expectedWidth, root.getJSONObject("area").getIntValue("width"),
                    "sourceTableIndex=" + parsedTable.getSourceTableIndex());
            for (int columnIndex = 0; columnIndex < parsedTable.getColumnCount(); columnIndex++) {
                assertEquals(expectedColumnWidths.get(columnIndex).intValue(),
                        root.getJSONObject("cols").getJSONObject(String.valueOf(columnIndex)).getIntValue("width"),
                        "sourceTableIndex=" + parsedTable.getSourceTableIndex() + ", columnIndex=" + columnIndex);
            }
            assertEquals("landscape", root.getJSONObject("printConfig").getString("layout"),
                    "sourceTableIndex=" + parsedTable.getSourceTableIndex());
        }
    }

    @Test
    void build_shouldKeepEffectiveDateRowsCompactForFixedRouteAProcessPages() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteARecognizer recognizer = new MesProBatchRecordRouteARecognizer(parser);
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(
                FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString());

        List<MesProBatchRecordParsedTable> processPages = parsedTables.stream()
                .map(calibrator::calibrate)
                .filter(table -> table.getTableTitle() != null && table.getTableTitle().contains("工序生产记录"))
                .toList();

        assertFalse(processPages.isEmpty(), "fixed route-A should include process pages");
        for (MesProBatchRecordParsedTable parsedTable : processPages) {
            JSONObject root = JSON.parseObject(builder.build(parsedTable, REPORT_CODE));
            Integer footerRowIndex = findRenderedRowIndexes(root, row -> renderedRowText(row).contains("生效日期："))
                    .stream()
                    .findFirst()
                    .orElseThrow();
            int height = root.getJSONObject("rows")
                    .getJSONObject(String.valueOf(footerRowIndex))
                    .getIntValue("height");

            assertTrue(height <= MesProBatchRecordReportShapeRules.FOOTER_ROW_HEIGHT_FLOOR_PX,
                    "tableTitle=" + parsedTable.getTableTitle() + ", footerHeight=" + height);
        }
    }

    @Test
    void build_shouldKeepDocumentHeaderTopSpacerCompactForFixedRouteAProcessPages() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteARecognizer recognizer = new MesProBatchRecordRouteARecognizer(parser);
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(
                FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString());

        List<MesProBatchRecordParsedTable> processPages = parsedTables.stream()
                .map(calibrator::calibrate)
                .filter(table -> table.getTableTitle() != null && table.getTableTitle().contains("工序生产记录"))
                .toList();

        assertFalse(processPages.isEmpty(), "fixed route-A should include process pages");
        for (MesProBatchRecordParsedTable parsedTable : processPages) {
            JSONObject root = JSON.parseObject(builder.build(parsedTable, REPORT_CODE));
            JSONObject firstRenderedRow = root.getJSONObject("rows").getJSONObject("0");
            JSONObject fixedHeadRange = root.getJSONArray("fixedPrintHeadRows").getJSONObject(0);

            assertFalse(" ".equals(firstRenderedRow.getJSONObject("cells").getJSONObject("0").getString("text")),
                    "tableTitle=" + parsedTable.getTableTitle() + " should render source header row first");
            assertFalse(firstRenderedRow.getBooleanValue("pagingRow"),
                    "tableTitle=" + parsedTable.getTableTitle() + " should not synthesize first-page paging row");
            assertEquals(0, fixedHeadRange.getIntValue("sri"),
                    "tableTitle=" + parsedTable.getTableTitle());
            assertEquals(1, fixedHeadRange.getIntValue("eri"),
                    "tableTitle=" + parsedTable.getTableTitle());
        }
    }

    @Test
    void build_shouldPreferSourceRowHeightsForFixedRouteANonPagedProcessRows() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteARecognizer recognizer = new MesProBatchRecordRouteARecognizer(parser);
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(
                FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString());

        List<String> inflatedRows = new ArrayList<>();
        int assertedRows = 0;
        for (MesProBatchRecordParsedTable parsedTable : parsedTables.stream()
                .map(calibrator::calibrate)
                .filter(MesProBatchRecordReportJsonBuilderTest::isProcessRecordPage)
                .toList()) {
            JSONObject root = JSON.parseObject(builder.build(parsedTable, REPORT_CODE));
            if (findPagingRowIndexOrDefault(root, -1) >= 0) {
                continue;
            }

            assertEquals(parsedTable.getRowCount().intValue(), countRenderedDataRows(root),
                    "sourceTableIndex=" + parsedTable.getSourceTableIndex());
            for (int rowIndex = 0; rowIndex < parsedTable.getRowCount(); rowIndex++) {
                List<MesProBatchRecordParsedCell> sourceRow = parsedTable.getRows().get(rowIndex);
                int sourceHeight = rowHeightAt(parsedTable, rowIndex);
                if (!hasCredibleSingleLineSourceHeight(sourceRow, sourceHeight)) {
                    continue;
                }
                int renderedHeight = root.getJSONObject("rows")
                        .getJSONObject(String.valueOf(rowIndex))
                        .getIntValue("height");
                if (renderedHeight > sourceHeight) {
                    inflatedRows.add("sourceTableIndex=" + parsedTable.getSourceTableIndex()
                            + ", rowIndex=" + rowIndex
                            + ", sourceHeight=" + sourceHeight
                            + ", renderedHeight=" + renderedHeight
                            + ", text=" + renderedRowText(root.getJSONObject("rows")
                            .getJSONObject(String.valueOf(rowIndex))));
                }
                assertedRows++;
            }
        }

        assertTrue(assertedRows >= 20, "expected enough source-backed one-line rows, actual=" + assertedRows);
        assertTrue(inflatedRows.isEmpty(),
                () -> "source-backed single-line rows should keep source height; examples="
                        + inflatedRows.stream().limit(8).toList());
    }

    @Test
    void build_shouldNotMaterializeSyntheticPagingRowsForSourceBackedRouteAGrids() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-A source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordRouteARecognizer recognizer = new MesProBatchRecordRouteARecognizer(parser);
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(
                FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString());

        List<String> deltas = new ArrayList<>();
        for (MesProBatchRecordParsedTable parsedTable : parsedTables.stream().map(calibrator::calibrate).toList()) {
            JSONObject root = JSON.parseObject(builder.build(parsedTable, REPORT_CODE));
            int renderedRowCount = countRenderedDataRows(root);
            int sourceMergeCount = countSourceMergeRanges(parsedTable);
            int renderedMergeCount = root.getJSONArray("merges").size();
            int pagingRowIndex = findPagingRowIndexOrDefault(root, -1);
            if (renderedRowCount != parsedTable.getRowCount()
                    || renderedMergeCount != sourceMergeCount
                    || pagingRowIndex >= 0) {
                deltas.add("sourceTableIndex=" + parsedTable.getSourceTableIndex()
                        + ", rows=" + parsedTable.getRowCount() + "/" + renderedRowCount
                        + ", merges=" + sourceMergeCount + "/" + renderedMergeCount
                        + ", pagingRowIndex=" + pagingRowIndex
                        + ", title=" + parsedTable.getTableTitle());
            }
        }

        assertTrue(deltas.isEmpty(),
                () -> "source-backed grids should not gain synthetic paging rows or spacer merges; examples="
                        + deltas.stream().limit(8).toList());
    }

    @Test
    void build_shouldPreferSourceRowHeightsForDenseRowsWithVisualBlanksAndShortWrappedCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic dense source grid")
                .rowCount(5)
                .columnCount(6)
                .columnWidths(List.of(46, 46, 46, 46, 46, 46))
                .rows(List.of(
                        List.of(sourceBackedCell("通用工序生产记录", 1, 6, 276, 18, false, false, true)),
                        List.of(
                                sourceBackedCell("操作日期", 1, 1, 46, 20, false, false, false),
                                sourceBackedCell("物料名称", 1, 1, 46, 20, false, false, false),
                                sourceBackedCell("清洗介质", 1, 1, 46, 20, false, false, false),
                                sourceBackedCell("清洗功率", 1, 1, 46, 20, false, false, false),
                                sourceBackedCell("操作人", 1, 1, 46, 20, false, false, false),
                                sourceBackedCell("复核人", 1, 1, 46, 20, false, false, false)),
                        List.of(
                                sourceBackedCell("/", 1, 1, 46, 18, false, true, false),
                                sourceBackedCell("弹簧", 1, 1, 46, 18, false, false, false),
                                sourceBackedCell("自来\n水", 1, 1, 46, 18, false, false, false),
                                sourceBackedCell("20-30\n%", 1, 1, 46, 18, false, false, false),
                                sourceBackedCell("", 1, 1, 46, 18, true, false, false),
                                sourceBackedCell("30min", 1, 1, 46, 18, false, false, false)),
                        List.of(sourceBackedCell("生产批量汇总", 1, 6, 276, 22, false, false, false)),
                        List.of(sourceBackedCell("生效日期：2026年02月02日", 1, 6, 276, 20, false, false, false))))
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(18, root.getJSONObject("rows").getJSONObject("2").getIntValue("height"),
                "source-backed dense detail rows should keep Word row height even with visual blanks and short wrapping");
    }

    @Test
    void build_shouldKeepSourceRowHeightForWideSourceBackedProcessRowsWithShortWrappedCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(11)
                .tableTitle("generic source-backed process grid")
                .rowCount(3)
                .columnCount(20)
                .columnWidths(List.of(34, 62, 72, 45, 65, 40, 40, 45, 45, 42,
                        42, 42, 42, 45, 45, 65, 65, 65, 65, 65))
                .rows(List.of(
                        List.of(sourceBackedCell("通用工序生产记录", 1, 20, 1042, 20, false, false, true)),
                        List.of(
                                sourceBackedCell("操作日期", 1, 1, 34, 20, false, false, true),
                                sourceBackedCell("物料编码", 1, 1, 62, 20, false, false, true),
                                sourceBackedCell("物料名称", 1, 1, 72, 20, false, false, true),
                                sourceBackedCell("批号", 1, 1, 45, 20, false, false, true),
                                sourceBackedCell("清洗次数", 1, 2, 105, 20, false, false, true),
                                sourceBackedCell("清洗介质", 1, 2, 85, 20, false, false, true),
                                sourceBackedCell("清洗功率", 1, 2, 87, 20, false, false, true),
                                sourceBackedCell("清洗温度", 1, 2, 84, 20, false, false, true),
                                sourceBackedCell("清洗时间", 1, 2, 87, 20, false, false, true),
                                sourceBackedCell("生产数量/pcs", 1, 1, 65, 20, false, false, true),
                                sourceBackedCell("自检合格数量/pcs", 1, 1, 65, 20, false, false, true),
                                sourceBackedCell("不合格数量/pcs", 1, 1, 65, 20, false, false, true),
                                sourceBackedCell("操作人", 1, 1, 65, 20, false, false, true),
                                sourceBackedCell("复核人", 1, 1, 65, 20, false, false, true)),
                        List.of(
                                sourceBackedCell("/", 1, 1, 34, 18, false, true, false),
                                sourceBackedCell("PP-001", 1, 1, 62, 18, false, false, false),
                                sourceBackedCell("弹簧", 1, 1, 72, 18, false, false, false),
                                sourceBackedCell("", 1, 1, 45, 18, true, false, false),
                                sourceBackedCell("2", 1, 1, 65, 18, false, false, false),
                                sourceBackedCell("", 1, 1, 40, 18, true, false, false),
                                sourceBackedCell("自来\n水", 1, 1, 40, 18, false, false, false),
                                sourceBackedCell("", 1, 1, 45, 18, true, false, false),
                                sourceBackedCell("20-30\n%", 1, 1, 45, 18, false, false, false),
                                sourceBackedCell("", 1, 1, 42, 18, true, false, false),
                                sourceBackedCell("室温", 1, 1, 42, 18, false, false, false),
                                sourceBackedCell("", 1, 1, 42, 18, true, false, false),
                                sourceBackedCell("30min", 1, 1, 42, 18, false, false, false),
                                sourceBackedCell("", 1, 1, 45, 18, true, false, false),
                                sourceBackedCell("", 1, 1, 45, 18, true, false, false),
                                sourceBackedCell("", 1, 1, 65, 18, true, false, false),
                                sourceBackedCell("", 1, 1, 65, 18, true, false, false),
                                sourceBackedCell("", 1, 1, 65, 18, true, false, false),
                                sourceBackedCell("", 1, 1, 65, 18, true, false, false),
                                sourceBackedCell("", 1, 1, 65, 18, true, false, false))))
                .preserveSourceGrid(true)
                .build();

        JSONObject root = JSON.parseObject(builder.build(table, REPORT_CODE));

        assertEquals(18, root.getJSONObject("rows").getJSONObject("2").getIntValue("height"),
                "source-backed process detail rows should keep Word row height and avoid synthetic JSON inflation");
    }

    private static MesProBatchRecordParsedTable parsedTable(int index, String title) {
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(index)
                .tableTitle(title)
                .rowCount(1)
                .columnCount(1)
                .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                        .text(title)
                        .rowSpan(1)
                        .colSpan(1)
                        .build())))
                .build();
    }

    private static boolean isLowColumnOverviewLikePage(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return false;
        }
        int columnCount = Math.max(table.getColumnCount(), 0);
        if (columnCount <= 1 || columnCount > MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT) {
            return false;
        }
        if (table.getRowCount() == null || table.getRowCount() < 20) {
            return false;
        }
        int sharedBudget = MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(columnCount);
        int maxSourceRowWidth = 0;
        int fullWidthSectionRows = 0;
        int gridRows = 0;
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            int rowColSpan = 0;
            int rowWidth = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                rowColSpan += Math.max(cell.getColSpan(), 1);
                rowWidth += Math.max(cell.getWidthPx(), 0);
            }
            maxSourceRowWidth = Math.max(maxSourceRowWidth, rowWidth);
            if (row.size() == 1 && rowColSpan >= columnCount) {
                fullWidthSectionRows++;
            }
            if (row.size() >= 2 && rowColSpan >= columnCount) {
                gridRows++;
            }
        }
        return maxSourceRowWidth >= Math.min(1000, Math.round(sharedBudget * 0.9f))
                && (fullWidthSectionRows >= 2 || gridRows >= 4);
    }

    private static String describeLowColumnPageCandidates(List<MesProBatchRecordParsedTable> tables) {
        List<String> descriptions = new ArrayList<>();
        for (MesProBatchRecordParsedTable table : tables) {
            if (table == null || table.getRows() == null) {
                continue;
            }
            int columnCount = Math.max(table.getColumnCount(), 0);
            int maxSourceRowWidth = 0;
            int fullWidthSectionRows = 0;
            int gridRows = 0;
            for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
                int rowColSpan = 0;
                int rowWidth = 0;
                for (MesProBatchRecordParsedCell cell : row) {
                    rowColSpan += Math.max(cell.getColSpan(), 1);
                    rowWidth += Math.max(cell.getWidthPx(), 0);
                }
                maxSourceRowWidth = Math.max(maxSourceRowWidth, rowWidth);
                if (row.size() == 1 && rowColSpan >= columnCount) {
                    fullWidthSectionRows++;
                }
                if (row.size() >= 2 && rowColSpan >= columnCount) {
                    gridRows++;
                }
            }
            descriptions.add("index=" + table.getSourceTableIndex()
                    + ", cols=" + columnCount
                    + ", rows=" + table.getRowCount()
                    + ", maxWidth=" + maxSourceRowWidth
                    + ", fullRows=" + fullWidthSectionRows
                    + ", gridRows=" + gridRows);
        }
        return "low-column overview-like page not found; candidates=" + descriptions;
    }

    private static boolean isSourceBackedProcessLikeFullPage(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return false;
        }
        int columnCount = Math.max(table.getColumnCount(), 0);
        if (!Boolean.TRUE.equals(table.getPreserveSourceGrid())) {
            return false;
        }
        if (columnCount <= 1 || table.getColumnWidths() == null || table.getColumnWidths().size() != columnCount) {
            return false;
        }
        if (table.getRowCount() == null || table.getRowCount() < 17) {
            return false;
        }
        int sharedBudget = MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(columnCount);
        int maxSourceRowWidth = 0;
        boolean processTitle = false;
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            if (MesProBatchRecordSharedPageTitleRules.detectTitleType(row)
                    == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD) {
                processTitle = true;
            }
            int rowWidth = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                rowWidth += Math.max(cell.getWidthPx(), 0);
            }
            maxSourceRowWidth = Math.max(maxSourceRowWidth, rowWidth);
        }
        return processTitle
                && maxSourceRowWidth >= Math.min(1000, Math.round(sharedBudget * 0.9f));
    }

    private static String describeSourceBackedProcessPageCandidates(List<MesProBatchRecordParsedTable> tables) {
        List<String> descriptions = new ArrayList<>();
        for (MesProBatchRecordParsedTable table : tables) {
            if (table == null || table.getRows() == null) {
                continue;
            }
            int columnCount = Math.max(table.getColumnCount(), 0);
            int maxSourceRowWidth = 0;
            int gridRows = 0;
            boolean processTitle = false;
            for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
                if (MesProBatchRecordSharedPageTitleRules.detectTitleType(row)
                        == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD) {
                    processTitle = true;
                }
                int rowColSpan = 0;
                int rowWidth = 0;
                for (MesProBatchRecordParsedCell cell : row) {
                    rowColSpan += Math.max(cell.getColSpan(), 1);
                    rowWidth += Math.max(cell.getWidthPx(), 0);
                }
                maxSourceRowWidth = Math.max(maxSourceRowWidth, rowWidth);
                if (row.size() >= 2 && rowColSpan >= columnCount) {
                    gridRows++;
                }
            }
            descriptions.add("index=" + table.getSourceTableIndex()
                    + ", cols=" + columnCount
                    + ", rows=" + table.getRowCount()
                    + ", widths=" + (table.getColumnWidths() == null ? 0 : table.getColumnWidths().size())
                    + ", maxWidth=" + maxSourceRowWidth
                    + ", processTitle=" + processTitle
                    + ", gridRows=" + gridRows);
        }
        return "source-backed process full-page candidates not found; candidates=" + descriptions;
    }

    private static JSONObject styleOf(JSONObject root, int rowIndex, int columnIndex) {
        int styleIndex = root.getJSONObject("rows")
                .getJSONObject(String.valueOf(rowIndex))
                .getJSONObject("cells")
                .getJSONObject(String.valueOf(columnIndex))
                .getIntValue("style");
        return root.getJSONArray("styles").getJSONObject(styleIndex);
    }

    private static String borderWeight(JSONObject style, String side) {
        return style.getJSONObject("border").getJSONArray(side).getString(0);
    }

    private static MesProBatchRecordParsedTable buildStructuredTailAfterSummaryTable() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(row("通用工序生产记录"));
        rows.add(row("生产批号", "", "产品规格", "", "生产依据", ""));
        rows.add(row("设备编码", "超声波清洗机：T01", "是否在计量效期内", "□是", "□否"));
        rows.add(row("检查要求", "结果", "操作人/日期", "复核人/日期"));
        rows.add(row("备注：检查结果符合要求后进行以下生产操作"));
        rows.add(row("物料批号", "", "清洗介质", "", "清洗时间", ""));
        rows.add(row("清洗温度", "", "操作人员", "", "复核人员", ""));
        rows.add(row("操作日期", "物料编码", "物料名称", "批号",
                "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"));
        for (int index = 0; index < 30; index++) {
            rows.add(row("2026-05-17", "A00" + index, "弹簧", "B-" + index, "120", "118", "2", "张三", "李四"));
        }
        rows.add(row("生产批量汇总", "合格数量", "118", "不合格数量", "2"));
        rows.add(row("生产后清场记录", "结果", "操作人/日期", "复核人/日期"));
        rows.add(row("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；",
                "□符合要求\n□不符合要求", "", ""));
        rows.add(row("2、墙面、地面、天花板、灯具等环境清洁应符合要求。",
                "□符合要求\n□不符合要求", "", ""));
        rows.add(row("生效日期：2026年02月02日"));
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("通用工序生产记录")
                .rowCount(rows.size())
                .columnCount(20)
                .rows(rows)
                .build();
    }

    private static int findRowIndex(List<List<MesProBatchRecordParsedCell>> rows, String firstCellText) {
        for (int index = 0; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            if (!row.isEmpty() && firstCellText.equals(textOf(row.get(0)))) {
                return index;
            }
        }
        throw new IllegalStateException("row not found: " + firstCellText);
    }

    private static int findRowIndexContaining(List<List<MesProBatchRecordParsedCell>> rows, String text) {
        for (int index = 0; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            boolean found = row.stream().anyMatch(cell -> text.equals(textOf(cell)));
            if (found) {
                return index;
            }
        }
        throw new IllegalStateException("row not found: " + text);
    }

    private static int rowHeightAt(MesProBatchRecordParsedTable table, int rowIndex) {
        return table.getRows().get(rowIndex).stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0);
    }

    private static boolean isProcessRecordPage(MesProBatchRecordParsedTable table) {
        return table != null && table.getRows() != null && table.getRows().stream()
                .anyMatch(row -> MesProBatchRecordSharedPageTitleRules.detectTitleType(row)
                        == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD);
    }

    private static boolean hasCredibleSingleLineSourceHeight(List<MesProBatchRecordParsedCell> sourceRow,
                                                             int sourceHeight) {
        if (sourceHeight < MesProBatchRecordReportShapeRules.MIN_ROW_HEIGHT_PX
                || sourceHeight > MesProBatchRecordReportShapeRules.MAX_ROW_HEIGHT_PX) {
            return false;
        }
        for (MesProBatchRecordParsedCell cell : sourceRow) {
            String text = textOf(cell);
            if (text.contains("\n") || cell.isFillable() || cell.isVisualBlank()) {
                return false;
            }
        }
        return sourceRow.stream().anyMatch(cell -> !textOf(cell).isBlank());
    }

    private static int countRenderedDataRows(JSONObject root) {
        return (int) root.getJSONObject("rows").keySet().stream()
                .filter(key -> key.chars().allMatch(Character::isDigit))
                .count();
    }

    private static int countFillFormControls(JSONObject root) {
        int count = 0;
        JSONObject rows = root.getJSONObject("rows");
        for (String rowKey : rows.keySet()) {
            if (!rowKey.chars().allMatch(Character::isDigit)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(rowKey);
            if (row == null || row.getJSONObject("cells") == null) {
                continue;
            }
            JSONObject cells = row.getJSONObject("cells");
            for (String cellKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(cellKey);
                if (cell != null && cell.getJSONObject("fillForm") != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countCheckboxFillForms(JSONObject cells) {
        int count = 0;
        for (String cellKey : cells.keySet()) {
            if ("len".equals(cellKey)) {
                continue;
            }
            JSONObject fillForm = cells.getJSONObject(cellKey).getJSONObject("fillForm");
            if (fillForm != null && "checkbox".equals(fillForm.getString("componentFlag"))) {
                count++;
            }
        }
        return count;
    }

    private static int countInputTextFillForms(JSONObject cells) {
        int count = 0;
        for (String cellKey : cells.keySet()) {
            if ("len".equals(cellKey)) {
                continue;
            }
            JSONObject fillForm = cells.getJSONObject(cellKey).getJSONObject("fillForm");
            if (fillForm != null && "input-text".equals(fillForm.getString("componentFlag"))) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasTextareaFillForm(JSONObject cells) {
        for (String cellKey : cells.keySet()) {
            if ("len".equals(cellKey)) {
                continue;
            }
            JSONObject fillForm = cells.getJSONObject(cellKey).getJSONObject("fillForm");
            if (fillForm != null && "input-textarea".equals(fillForm.getString("componentFlag"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCheckboxFillFormLabel(JSONObject cells, String labelText) {
        for (String cellKey : cells.keySet()) {
            if ("len".equals(cellKey)) {
                continue;
            }
            JSONObject fillForm = cells.getJSONObject(cellKey).getJSONObject("fillForm");
            if (fillForm != null
                    && "checkbox".equals(fillForm.getString("componentFlag"))
                    && labelText.equals(fillForm.getString("labelText"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCheckboxFillFormOption(JSONObject fillForm, String labelText) {
        JSONArray options = fillForm.getJSONArray("options");
        if (options == null) {
            return false;
        }
        for (int index = 0; index < options.size(); index++) {
            JSONObject option = options.getJSONObject(index);
            if (option != null && labelText.equals(option.getString("label"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInputTextFillFormWithBlankPlaceholder(JSONObject cells) {
        for (String cellKey : cells.keySet()) {
            if ("len".equals(cellKey)) {
                continue;
            }
            JSONObject fillForm = cells.getJSONObject(cellKey).getJSONObject("fillForm");
            if (fillForm != null
                    && "input-text".equals(fillForm.getString("componentFlag"))
                    && "".equals(fillForm.getString("placeholder"))) {
                return true;
            }
        }
        return false;
    }

    private static int countSourceMergeRanges(MesProBatchRecordParsedTable table) {
        int mergeCount = 0;
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell.getRowSpan() > 1 || cell.getColSpan() > 1) {
                    mergeCount++;
                }
            }
        }
        return mergeCount;
    }

    private static int findRenderedRowIndexByText(JSONObject root, String text) {
        JSONObject rows = root.getJSONObject("rows");
        for (String key : rows.keySet()) {
            if (!key.chars().allMatch(Character::isDigit)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(key);
            if (row == null) {
                continue;
            }
            JSONObject cells = row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String cellKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(cellKey);
                if (cell != null && text.equals(cell.getString("text"))) {
                    return Integer.parseInt(key);
                }
            }
        }
        throw new IllegalStateException("rendered row not found: " + text);
    }

    private static JSONObject findCellByText(JSONObject cells, String textFragment) {
        for (String cellKey : cells.keySet()) {
            if ("len".equals(cellKey)) {
                continue;
            }
            JSONObject cell = cells.getJSONObject(cellKey);
            if (cell == null) {
                continue;
            }
            String text = cell.getString("text");
            if (text != null && text.contains(textFragment)) {
                return cell;
            }
        }
        throw new IllegalStateException("rendered cell not found: " + textFragment);
    }

    private static MesProBatchRecordParsedCell findParsedCellByText(MesProBatchRecordParsedTable table,
                                                                    String textFragment) {
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            for (MesProBatchRecordParsedCell cell : row) {
                String text = textOf(cell);
                if (text.contains(textFragment)) {
                    return cell;
                }
            }
        }
        throw new IllegalStateException("parsed cell not found: " + textFragment);
    }

    private static int findPagingRowIndex(JSONObject root) {
        JSONObject rows = root.getJSONObject("rows");
        for (String key : rows.keySet()) {
            if (!key.chars().allMatch(Character::isDigit)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(key);
            if (row != null && row.getBooleanValue("pagingRow")) {
                return Integer.parseInt(key);
            }
        }
        throw new IllegalStateException("paging row not found");
    }

    private static int findPagingRowIndexOrDefault(JSONObject root, int defaultValue) {
        JSONObject rows = root.getJSONObject("rows");
        for (String key : rows.keySet()) {
            if (!key.chars().allMatch(Character::isDigit)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(key);
            if (row != null && row.getBooleanValue("pagingRow")) {
                return Integer.parseInt(key);
            }
        }
        return defaultValue;
    }

    private static List<Integer> findRenderedRowIndexes(JSONObject root,
                                                        java.util.function.Predicate<JSONObject> predicate) {
        List<Integer> indexes = new ArrayList<>();
        JSONObject rows = root.getJSONObject("rows");
        for (String key : rows.keySet()) {
            if (!key.chars().allMatch(Character::isDigit)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(key);
            if (row != null && predicate.test(row)) {
                indexes.add(Integer.parseInt(key));
            }
        }
        return indexes;
    }

    private static boolean isRepeatedEquipmentMatrixRenderedRow(JSONObject row) {
        String text = renderedRowText(row).replace(" ", "");
        int matchedLabelCount = 0;
        for (String label : REPEATED_EQUIPMENT_MATRIX_LABELS) {
            if (text.contains(label.replace(" ", ""))) {
                matchedLabelCount++;
            }
        }
        return matchedLabelCount >= 3;
    }

    private static String describeRenderedRows(JSONObject root, List<Integer> rowIndexes) {
        List<String> descriptions = new ArrayList<>();
        JSONObject rows = root.getJSONObject("rows");
        for (Integer rowIndex : rowIndexes) {
            JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
            descriptions.add(rowIndex + ":" + renderedRowText(row).replace("\n", " / "));
        }
        return descriptions.toString();
    }

    private static String renderedRowText(JSONObject row) {
        if (row == null) {
            return "";
        }
        JSONObject cells = row.getJSONObject("cells");
        if (cells == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String cellKey : cells.keySet()) {
            JSONObject cell = cells.getJSONObject(cellKey);
            if (cell == null) {
                continue;
            }
            String text = cell.getString("text");
            if (text == null || text.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('|');
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private static String renderedSheetText(JSONObject root) {
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        if (rows == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String rowKey : rows.keySet()) {
            if (!rowKey.chars().allMatch(Character::isDigit)) {
                continue;
            }
            String text = renderedRowText(rows.getJSONObject(rowKey));
            if (text.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private static int countNumericKeys(JSONObject object) {
        int count = 0;
        for (String key : object.keySet()) {
            if (key.chars().allMatch(Character::isDigit)) {
                count++;
            }
        }
        return count;
    }

    private static boolean mergeRangesSpanRow(JSONArray ranges, int rowIndex) {
        if (ranges == null) {
            return false;
        }
        for (Object item : ranges) {
            String range = String.valueOf(item);
            String[] refs = range.split(":");
            if (refs.length != 2) {
                continue;
            }
            int startRowIndex = rowIndexFromCellRef(refs[0]);
            int endRowIndex = rowIndexFromCellRef(refs[1]);
            if (startRowIndex < rowIndex && endRowIndex > rowIndex) {
                return true;
            }
        }
        return false;
    }

    private static int rowIndexFromCellRef(String ref) {
        int splitIndex = 0;
        while (splitIndex < ref.length() && Character.isLetter(ref.charAt(splitIndex))) {
            splitIndex++;
        }
        return Integer.parseInt(ref.substring(splitIndex)) - 1;
    }

    private static String textOf(MesProBatchRecordParsedCell cell) {
        return cell == null || cell.getText() == null ? "" : cell.getText().trim();
    }

    private static List<Integer> expectedRenderedColumnWidths(MesProBatchRecordParsedTable parsedTable) {
        List<Integer> widths = new ArrayList<>(parsedTable.getColumnWidths());
        normalizeExpectedVisibleVerticalSectionColumnWidth(widths, parsedTable);
        return widths;
    }

    private static void normalizeExpectedVisibleVerticalSectionColumnWidth(List<Integer> widths,
                                                                           MesProBatchRecordParsedTable parsedTable) {
        if (widths == null || widths.isEmpty() || parsedTable == null || parsedTable.getRows() == null
                || parsedTable.getColumnCount() <= 1 || !hasLeadingVerticalSectionColumn(parsedTable)) {
            return;
        }
        int currentWidth = Math.max(0, widths.get(0));
        int visibleWidth = Math.max(24, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
        if (currentWidth >= visibleWidth) {
            return;
        }
        int deficit = visibleWidth - currentWidth;
        widths.set(0, visibleWidth);
        while (deficit > 0) {
            int donor = findBestExpectedColumnWidthDonor(widths);
            if (donor <= 0) {
                break;
            }
            widths.set(donor, widths.get(donor) - 1);
            deficit--;
        }
    }

    private static boolean hasLeadingVerticalSectionColumn(MesProBatchRecordParsedTable parsedTable) {
        for (List<MesProBatchRecordParsedCell> row : parsedTable.getRows()) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            MesProBatchRecordParsedCell firstCell = row.get(0);
            if (firstCell == null || Math.max(1, firstCell.getColSpan()) != 1
                    || Math.max(1, firstCell.getRowSpan()) < 3) {
                continue;
            }
            if (isStructurallyNarrowLeadingSectionCell(firstCell)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStructurallyNarrowLeadingSectionCell(MesProBatchRecordParsedCell firstCell) {
        String text = textOf(firstCell).replace("\n", "");
        if (text.isBlank()) {
            return false;
        }
        int width = Math.max(0, firstCell.getWidthPx());
        int height = Math.max(0, firstCell.getHeightPx()) * Math.max(1, firstCell.getRowSpan());
        if (width <= 0 || height <= 0) {
            return true;
        }
        return height >= width * 2;
    }

    private static int findBestExpectedColumnWidthDonor(List<Integer> widths) {
        int bestColumn = -1;
        int bestSlack = 0;
        for (int columnIndex = 1; columnIndex < widths.size(); columnIndex++) {
            int width = widths.get(columnIndex) == null ? 0 : widths.get(columnIndex);
            int slack = width - MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
            if (slack > bestSlack) {
                bestSlack = slack;
                bestColumn = columnIndex;
            }
        }
        return bestColumn;
    }

    private static List<MesProBatchRecordParsedCell> row(String... texts) {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (String text : texts) {
            row.add(MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .rowSpan(1)
                    .colSpan(1)
                    .bold(false)
                    .fontSize(10)
                    .horizontalAlign("center")
                    .verticalAlign("middle")
                    .widthPx(120)
                    .heightPx(24)
                    .build());
        }
        return row;
    }

    private static MesProBatchRecordParsedCell sourceBackedCell(String text, int rowSpan, int colSpan,
                                                                int widthPx, int heightPx,
                                                                boolean fillable, boolean visualBlank,
                                                                boolean bold) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(rowSpan)
                .colSpan(colSpan)
                .bold(bold)
                .fontSize(bold ? 10 : 9)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .widthPx(widthPx)
                .heightPx(heightPx)
                .fillable(fillable)
                .visualBlank(visualBlank)
                .placeholder(fillable ? "" : "请填写")
                .build();
    }

    private static MesProBatchRecordParsedCell sourceBackedCellAt(String text, int columnIndex, int rowSpan, int colSpan,
                                                                  int widthPx, int heightPx,
                                                                  boolean fillable, boolean visualBlank,
                                                                  boolean bold) {
        MesProBatchRecordParsedCell cell = sourceBackedCell(text, rowSpan, colSpan, widthPx, heightPx,
                fillable, visualBlank, bold);
        cell.setColumnIndex(columnIndex);
        return cell;
    }

    private static MesProBatchRecordParsedTable buildRepeatedEquipmentMatrixTable(int matrixRows, boolean dispersed) {
        return buildRepeatedEquipmentMatrixTable(matrixRows, dispersed, false);
    }

    private static MesProBatchRecordParsedTable buildRepeatedEquipmentMatrixTable(int matrixRows, boolean dispersed,
                                                                                  boolean includeTableHeader) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(List.of(
                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                docHeaderCell("记录编号", 1, 2, true),
                docHeaderCell("RE-PP-ID-01", 1, 3, true)
        ));
        rows.add(List.of(
                docHeaderCell("版本", 1, 2, true),
                docHeaderCell("A/1", 1, 3, true)
        ));
        if (includeTableHeader) {
            rows.add(row("操作日期", "设备编码", "参数", "实际", "生产数量/pcs",
                    "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人", ""));
        }
        int insertedMatrixRows = 0;
        while (insertedMatrixRows < matrixRows) {
            if (dispersed && insertedMatrixRows == matrixRows / 2) {
                rows.add(row("操作日期", "设备编码", "参数", "实际", "生产数量/pcs",
                        "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人", ""));
            }
            rows.add(repeatedEquipmentMatrixRow());
            insertedMatrixRows++;
        }
        rows.add(row("生产批量汇总", "", "", "", "", "", "", "", "", ""));
        rows.add(row("生产后清场记录", "项目", "要求", "结果", "操作人/日期", "复核人/日期", "", "", "", ""));
        rows.add(row("生效日期：2026年02月02日", "", "", "", "", "", "", "", "", ""));
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic repeated matrix")
                .rowCount(rows.size())
                .columnCount(10)
                .rows(rows)
                .build();
    }

    private static MesProBatchRecordParsedTable buildTwoBandRepeatedEquipmentMatrixTable(boolean reusableSecondHeader) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(List.of(
                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                docHeaderCell("记录编号", 1, 2, true),
                docHeaderCell("RE-PP-ID-01", 1, 3, true)
        ));
        rows.add(List.of(
                docHeaderCell("版本", 1, 2, true),
                docHeaderCell("A/1", 1, 3, true)
        ));
        rows.add(row("操作日期", "设备编码", "参数", "实际", "生产数量/pcs",
                "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人", ""));
        for (int index = 0; index < 4; index++) {
            rows.add(repeatedEquipmentMatrixRow());
        }
        rows.add(reusableSecondHeader ? row("复核日期", "模具编号", "项目", "目标", "首件数量/pcs",
                "巡检数量/pcs", "异常数量/pcs", "记录人", "确认人", "") : nonReusableTableHeaderRow());
        for (int index = 0; index < 8; index++) {
            rows.add(repeatedEquipmentMatrixRow());
        }
        rows.add(row("生产批量汇总", "", "", "", "", "", "", "", "", ""));
        rows.add(row("生产后清场记录", "项目", "要求", "结果", "操作人/日期", "复核人/日期", "", "", "", ""));
        rows.add(row("生效日期：2026年02月02日", "", "", "", "", "", "", "", "", ""));
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic two-band repeated matrix")
                .rowCount(rows.size())
                .columnCount(10)
                .rows(rows)
                .build();
    }

    private static MesProBatchRecordParsedTable buildRepeatedEquipmentMatrixTableWithPlainTail() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(List.of(
                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                docHeaderCell("记录编号", 1, 2, true),
                docHeaderCell("RE-PP-ID-01", 1, 3, true)
        ));
        rows.add(List.of(
                docHeaderCell("版本", 1, 2, true),
                docHeaderCell("A/1", 1, 3, true)
        ));
        rows.add(row("操作日期", "设备编码", "参数", "实际", "生产数量/pcs",
                "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人", ""));
        for (int index = 0; index < 8; index++) {
            rows.add(repeatedEquipmentMatrixRow());
        }
        rows.add(row("批后确认", "", "", "", "", "", "", "", "", ""));
        rows.add(row("生效日期：2026年02月02日", "", "", "", "", "", "", "", "", ""));
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic repeated matrix with plain tail")
                .rowCount(rows.size())
                .columnCount(10)
                .rows(rows)
                .build();
    }

    private static List<MesProBatchRecordParsedCell> repeatedEquipmentMatrixRow() {
        return row("", "", "封口热合机：□A05199\n热合机：□A05048\n自动热合机：(A03274",
                "", "", "", "", "□是 □否\n□是 □否", "(135±10", "(1.8±0.5");
    }

    private static List<MesProBatchRecordParsedCell> nonReusableTableHeaderRow() {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        row.add(MesProBatchRecordParsedCell.builder()
                .text("阶段")
                .rowSpan(1)
                .colSpan(2)
                .bold(false)
                .fontSize(10)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .widthPx(120)
                .heightPx(24)
                .build());
        row.addAll(row("检查日期", "设备", "项目", "目标", "数量/pcs", "结果", "记录人", "确认人"));
        return row;
    }

    private static MesProBatchRecordParsedTable buildRepeatedEquipmentMatrixTableWithCrossPagingMerge() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(List.of(
                docHeaderCell("球囊扩张压力泵生产记录", 2, 5, true),
                docHeaderCell("记录编号", 1, 2, true),
                docHeaderCell("RE-PP-ID-01", 1, 3, true)
        ));
        rows.add(List.of(
                docHeaderCell("版本", 1, 2, true),
                docHeaderCell("A/1", 1, 3, true)
        ));
        rows.add(row("操作日期", "设备编码", "参数", "实际", "生产数量/pcs",
                "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人", ""));
        for (int index = 0; index < 8; index++) {
            if (index == 3) {
                rows.add(repeatedEquipmentMatrixRowWithLeadingMerge());
            } else if (index == 4 || index == 5) {
                rows.add(repeatedEquipmentMatrixRowUnderLeadingMerge());
            } else {
                rows.add(repeatedEquipmentMatrixRow());
            }
        }
        rows.add(row("生产批量汇总", "", "", "", "", "", "", "", "", ""));
        rows.add(row("生产后清场记录", "项目", "要求", "结果", "操作人/日期", "复核人/日期", "", "", "", ""));
        rows.add(row("生效日期：2026年02月02日", "", "", "", "", "", "", "", "", ""));
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("generic repeated matrix with merge")
                .rowCount(rows.size())
                .columnCount(10)
                .rows(rows)
                .build();
    }

    private static List<MesProBatchRecordParsedCell> repeatedEquipmentMatrixRowWithLeadingMerge() {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        row.add(MesProBatchRecordParsedCell.builder()
                .text("跨页合并批号")
                .rowSpan(3)
                .colSpan(1)
                .bold(false)
                .fontSize(10)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .widthPx(120)
                .heightPx(24)
                .build());
        row.addAll(row("", "封口热合机：□A05199\n热合机：□A05048\n自动热合机：(A03274",
                "", "", "", "", "□是 □否\n□是 □否", "(135±10", "(1.8±0.5"));
        return row;
    }

    private static List<MesProBatchRecordParsedCell> repeatedEquipmentMatrixRowUnderLeadingMerge() {
        return row("", "封口热合机：□A05199\n热合机：□A05048\n自动热合机：(A03274",
                "", "", "", "", "□是 □否\n□是 □否", "(135±10", "(1.8±0.5");
    }

    private static MesProBatchRecordParsedCell docHeaderCell(String text, int rowSpan, int colSpan, boolean bold) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(rowSpan)
                .colSpan(colSpan)
                .bold(bold)
                .fontSize(bold ? 10 : 9)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .widthPx(colSpan * 80)
                .heightPx(26)
                .borderless(false)
                .build();
    }
}
