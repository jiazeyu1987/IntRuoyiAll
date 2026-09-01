package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultWordFormTemplateRecognizerTest {

    private static final String CLEANING_PRODUCTION_RECORD_FIXTURE =
            "/formcenter/pressure-pump-cleaning-production-record.docx";

    @Test
    void recognizeMultiFormProductionRecordSelectsCleaningSegmentAndPreservesVisualGrid() throws Exception {
        byte[] sample = readCleaningProductionRecordFixture();
        DefaultWordFormTemplateRecognizer recognizer = new DefaultWordFormTemplateRecognizer();

        FormTemplateRecognition recognition = recognizer.recognize(FormTemplateImportCommand.of(
                "清洗工序", "V1.0", "pressure-pump-cleaning-production-record.docx",
                sample, null));

        assertTrue(recognition.isSuccess());
        assertTrue(recognition.getFields().stream()
                .anyMatch(field -> field.getLabel().contains("清洗工序生产记录")));
        assertFalse(recognition.getFields().stream()
                .anyMatch(field -> field.getLabel().contains("粗洗工序生产记录")),
                "recognized fields must be scoped to the selected logical form");

        Map<String, Object> schema = parseMap(recognition.getJimuSchemaJson());
        Map<String, Object> layout = parseMap((String) schema.get("sheetLayoutJson"));
        Map<String, Object> cols = castMap(layout.get("cols"));
        Map<String, Object> rows = castMap(layout.get("rows"));
        assertEquals(45, number(cols.get("len")), "the source cleaning form uses a 45-column visual grid");
        assertEquals(44, number(rows.get("len")), "only rows 41-84 of the shared source table belong to cleaning");
        assertFalse(((List<?>) layout.get("styles")).isEmpty(), "source typography and borders must produce styles");
        List<?> merges = (List<?>) layout.get("merges");
        assertTrue(merges.contains("A1:AS1"), "the title must span the full 45-column grid");
        assertTrue(merges.contains("A6:A41"), "the cleaning operation band must span its complete source section");
        assertEquals(List.of(0, 44), castMap(cellAt(rows, 0, 0)).get("merge"));
        assertEquals(List.of(2, 0), castMap(cellAt(rows, 2, 0)).get("merge"));
        assertEquals(List.of(35, 0), castMap(cellAt(rows, 5, 0)).get("merge"));
        assertEquals(List.of(2, 1), castMap(cellAt(rows, 8, 2)).get("merge"));
        String titleText = String.valueOf(castMap(cellAt(rows, 0, 0)).get("text"));
        assertTrue(titleText.contains("清洗工序生产记录"));
        assertTrue(titleText.contains("\n"), "separate Word paragraphs in the title cell must remain on separate lines");
        assertTrue(titleText.contains("☑关键/特殊工序") && titleText.contains("□非关键/特殊工序"));
        assertTrue(hasRowContaining(rows, List.of("清洗生产操作及自检记录", "设备编码")));
        assertTrue(cellStream(rows)
                .map(cell -> String.valueOf(cell.getOrDefault("text", "")))
                .anyMatch(text -> text.contains("超声波清洗机：B09353")
                        && text.contains("箱型干燥机：B04091") && text.contains("\n")));
        assertTrue(hasRowContainingFragments(rows, List.of("操作日期", "物料编码", "物料名称", "批号",
                "清洗次数", "清洗介质", "清洗功率", "清洗温度", "清洗时间",
                "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人")));
        assertTrue(hasAllCellTexts(rows, List.of("胶塞环", "杠杆架", "螺杆", "后盖",
                "外套", "按钮", "活塞", "螺纹块", "生产自检", "生产批量汇总", "生产后清场记录")));
        assertFalse(hasAnyCellTextContaining(rows, "粗洗工序生产记录"));
        assertEquals(96, countCellsWithFlag(rows, "edhrDiagonalSlash"),
                "all diagonal slash cells from the source Word table must remain forbidden visual cells");
        assertEquals(96, countCellsWithValue(rows, "edhrDiagonalSlashDirection", "TR2BL"),
                "the source slash direction must remain top-right to bottom-left");
        assertTrue(hasRowContaining(rows, List.of("清洗次数", "清洗介质", "清洗功率", "清洗时间", "清洗次数")),
                "the recognizer must preserve the source header anomaly instead of silently rewriting it");

        List<Map<String, Object>> cellRules = castMapList(schema.get("cellRules"));
        assertFalse(cellRules.stream().anyMatch(rule -> number(rule.get("rowIndex")) == 0
                        && number(rule.get("columnIndex")) == 0),
                "the full-width form title and its key-process markers must remain static text");
        assertTrue(cellRules.stream().anyMatch(rule -> "操作日期".equals(rule.get("label"))
                        && "DATE".equals(rule.get("valueType"))),
                "operation-date cells must be independently fillable");
        assertTrue(cellRules.stream().anyMatch(rule -> "生产数量/pcs".equals(rule.get("label"))
                        && "NUMBER".equals(rule.get("valueType"))),
                "production quantity cells must be numeric inputs");
        assertTrue(cellRules.stream().anyMatch(rule -> "复核人".equals(rule.get("label"))
                        && "SIGNATURE".equals(rule.get("valueType"))),
                "reviewer cells must remain electronic signature inputs");
    }

    @Test
    void recognizeMultiFormProductionRecordFailsWhenTemplateNameDoesNotSelectOneCandidate() throws Exception {
        byte[] sample = readCleaningProductionRecordFixture();
        DefaultWordFormTemplateRecognizer recognizer = new DefaultWordFormTemplateRecognizer();

        FormTemplateRecognition recognition = recognizer.recognize(FormTemplateImportCommand.of(
                "生产记录", "V1.0", "pressure-pump-cleaning-production-record.docx",
                sample, null));

        assertFalse(recognition.isSuccess());
        assertTrue(recognition.getFailureReason().contains("multiple Word form candidates found"));
    }

    @Test
    void recognizeLightCuringProcessPreservesNestedMaterialTable() throws Exception {
        Path sample = findRepoResource("按压式球囊扩充压力泵IDI-001",
                "RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.docx");
        assertTrue(Files.exists(sample), "pressure pump IDI production record DOCX fixture is required");
        DefaultWordFormTemplateRecognizer recognizer = new DefaultWordFormTemplateRecognizer();

        FormTemplateRecognition recognition = recognizer.recognize(FormTemplateImportCommand.of(
                "光固工序", "V1.0", sample.getFileName().toString(), Files.readAllBytes(sample), null));

        assertTrue(recognition.isSuccess());
        Map<String, Object> schema = parseMap(recognition.getJimuSchemaJson());
        Map<String, Object> layout = parseMap((String) schema.get("sheetLayoutJson"));
        Map<String, Object> rows = castMap(layout.get("rows"));
        assertEquals(81, number(castMap(layout.get("cols")).get("len")));
        assertEquals(20, number(rows.get("len")),
                "the four-row nested material table must expand its one parent row by three visual rows");
        assertEquals(List.of(11, 0), castMap(cellAt(rows, 5, 0)).get("merge"),
                "the light-curing operation band must expand across the nested rows and following operation rows");
        assertTrue(hasRowContaining(rows,
                List.of("物料编码", "物料名称", "批号")),
                "the nested material header must remain in the light-curing layout");
        assertEquals("物料编码", castMap(cellAt(rows, 5, 1)).get("text"));
        assertEquals("物料名称", castMap(cellAt(rows, 5, 11)).get("text"));
        assertEquals("批号", castMap(cellAt(rows, 5, 25)).get("text"));
        assertEquals("物料编码", castMap(cellAt(rows, 5, 38)).get("text"));
        assertEquals("物料名称", castMap(cellAt(rows, 5, 54)).get("text"));
        assertEquals("批号", castMap(cellAt(rows, 5, 69)).get("text"));
        assertEquals("A003.017.02.002.5001", castMap(cellAt(rows, 6, 1)).get("text"));
        assertEquals("外套", castMap(cellAt(rows, 6, 11)).get("text"));
        assertEquals("A001.02.033.106", castMap(cellAt(rows, 6, 38)).get("text"));
        assertEquals("□30atm压力表", castMap(cellAt(rows, 6, 54)).get("text"));
        assertTrue(hasRowContaining(rows,
                List.of("A003.017.02.002.5001", "外套", "A001.02.033.106", "□30atm压力表")));
        assertTrue(hasRowContainingFragments(rows,
                List.of("A004.002.04.1008", "延长管（尼龙编织管）", "A001.02.033.107", "□40atm压力表")));
        assertTrue(hasRowContaining(rows,
                List.of("A006.001.1001", "旋转接头", "A001.03.003.1004", "光固胶")));

        List<Map<String, Object>> batchRules = castMapList(schema.get("cellRules")).stream()
                .filter(rule -> "批号".equals(rule.get("label")))
                .toList();
        assertEquals(6, batchRules.size(), "each nested material must keep one fillable batch-number cell");
        assertTrue(batchRules.stream().allMatch(rule -> "STRING".equals(rule.get("valueType"))
                && "input-text".equals(rule.get("componentFlag"))));
        assertEquals(List.of("6:25", "6:69", "7:25", "7:69", "8:25", "8:69"), batchRules.stream()
                .map(rule -> rule.get("rowIndex") + ":" + rule.get("columnIndex"))
                .sorted()
                .toList());
    }

    @Test
    void recognizeProcessInspectionDocxPreservesSourceGridAndCreatesEquipmentTextInputs() throws Exception {
        Path sample = findRepoResource("按压式球囊扩充压力泵IDI-001", "old/过程检验记录.docx");
        assertTrue(Files.exists(sample), "pressure pump process inspection DOCX fixture is required");
        DefaultWordFormTemplateRecognizer recognizer = new DefaultWordFormTemplateRecognizer();

        FormTemplateRecognition recognition = recognizer.recognize(FormTemplateImportCommand.of(
                "按压式压力泵过程检验记录", "V8.0", sample.getFileName().toString(),
                Files.readAllBytes(sample), null));

        assertTrue(recognition.isSuccess());
        assertFalse(recognition.getFields().isEmpty());
        assertNotNull(recognition.getJimuSchemaJson(),
                "table-based Word imports must persist a visual layout instead of using the recognized-field fallback");

        Map<String, Object> schema = parseMap(recognition.getJimuSchemaJson());
        Map<String, Object> layout = parseMap((String) schema.get("sheetLayoutJson"));
        Map<String, Object> cols = castMap(layout.get("cols"));
        Map<String, Object> rows = castMap(layout.get("rows"));
        assertEquals(18, number(cols.get("len")));
        assertTrue(number(rows.get("len")) >= 47);
        assertTrue(hasRowContaining(rows, List.of("序号", "检验日期", "检验项目", "检验设备")),
                "the original process-inspection header must remain in one horizontal table");

        List<Map<String, Object>> cellRules = castMapList(schema.get("cellRules"));
        List<Map<String, Object>> equipmentRules = cellRules.stream()
                .filter(rule -> "气密性检测工装：".equals(rule.get("label")))
                .toList();
        assertEquals(3, equipmentRules.size());
        for (Map<String, Object> rule : equipmentRules) {
            assertEquals("STRING", rule.get("valueType"));
            assertEquals("input-text", rule.get("componentFlag"));
            int rowIndex = number(rule.get("rowIndex"));
            int columnIndex = number(rule.get("columnIndex"));
            Map<String, Object> row = castMap(rows.get(String.valueOf(rowIndex)));
            Map<String, Object> cells = castMap(row.get("cells"));
            Map<String, Object> inputCell = castMap(cells.get(String.valueOf(columnIndex)));
            Map<String, Object> labelCell = castMap(cells.get(String.valueOf(columnIndex - 1)));
            assertEquals("气密性检测工装：", labelCell.get("text"));
            assertEquals("", inputCell.get("text"));
        }
    }

    private static Path findRepoResource(String directoryName, String fileName) {
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; cursor != null && depth < 8; depth++) {
            Path candidate = cursor.resolve("resource").resolve(directoryName).resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }
        return Path.of("resource").resolve(directoryName).resolve(fileName);
    }

    private static byte[] readCleaningProductionRecordFixture() throws Exception {
        try (InputStream input = DefaultWordFormTemplateRecognizerTest.class
                .getResourceAsStream(CLEANING_PRODUCTION_RECORD_FIXTURE)) {
            assertNotNull(input, "pressure pump production-record DOCX fixture is required");
            return input.readAllBytes();
        }
    }

    private static boolean hasRowContaining(Map<String, Object> rows, List<String> expectedTexts) {
        for (Map.Entry<String, Object> entry : rows.entrySet()) {
            if (!entry.getKey().chars().allMatch(Character::isDigit) || !(entry.getValue() instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> cells = castMap(castMap(entry.getValue()).get("cells"));
            List<String> texts = cells.values().stream()
                    .filter(Map.class::isInstance)
                    .map(DefaultWordFormTemplateRecognizerTest::castMap)
                    .map(cell -> String.valueOf(cell.getOrDefault("text", "")))
                    .toList();
            if (texts.containsAll(expectedTexts)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRowContainingFragments(Map<String, Object> rows, List<String> expectedTexts) {
        for (Map.Entry<String, Object> entry : rows.entrySet()) {
            if (!entry.getKey().chars().allMatch(Character::isDigit) || !(entry.getValue() instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> cells = castMap(castMap(entry.getValue()).get("cells"));
            List<String> texts = cells.values().stream()
                    .filter(Map.class::isInstance)
                    .map(DefaultWordFormTemplateRecognizerTest::castMap)
                    .map(cell -> String.valueOf(cell.getOrDefault("text", "")).replaceAll("\\s+", ""))
                    .toList();
            boolean containsAll = expectedTexts.stream()
                    .map(text -> text.replaceAll("\\s+", ""))
                    .allMatch(expected -> texts.stream().anyMatch(text -> text.contains(expected)));
            if (containsAll) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAllCellTexts(Map<String, Object> rows, List<String> expectedTexts) {
        List<String> texts = cellStream(rows)
                .map(cell -> String.valueOf(cell.getOrDefault("text", "")))
                .toList();
        return texts.containsAll(expectedTexts);
    }

    private static boolean hasAnyCellTextContaining(Map<String, Object> rows, String expectedText) {
        return cellStream(rows)
                .map(cell -> String.valueOf(cell.getOrDefault("text", "")))
                .anyMatch(text -> text.contains(expectedText));
    }

    private static long countCellsWithFlag(Map<String, Object> rows, String flag) {
        return cellStream(rows)
                .filter(cell -> Boolean.TRUE.equals(cell.get(flag)))
                .count();
    }

    private static long countCellsWithValue(Map<String, Object> rows, String key, Object value) {
        return cellStream(rows)
                .filter(cell -> value.equals(cell.get(key)))
                .count();
    }

    private static Stream<Map<String, Object>> cellStream(Map<String, Object> rows) {
        return rows.entrySet().stream()
                .filter(entry -> entry.getKey().chars().allMatch(Character::isDigit))
                .filter(entry -> entry.getValue() instanceof Map<?, ?>)
                .map(entry -> castMap(castMap(entry.getValue()).get("cells")))
                .flatMap(cells -> cells.values().stream())
                .filter(Map.class::isInstance)
                .map(DefaultWordFormTemplateRecognizerTest::castMap);
    }

    private static Object cellAt(Map<String, Object> rows, int rowIndex, int columnIndex) {
        Map<String, Object> row = castMap(rows.get(String.valueOf(rowIndex)));
        return castMap(row.get("cells")).get(String.valueOf(columnIndex));
    }

    private static Map<String, Object> parseMap(String json) {
        return JsonUtils.parseObject(json, new TypeReference<Map<String, Object>>() { });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castMapList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private static int number(Object value) {
        return ((Number) value).intValue();
    }

}
