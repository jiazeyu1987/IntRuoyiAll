package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultWordFormTemplateRecognizerTest {

    @Test
    void recognizeProcessInspectionDocxPreservesSourceGridAndCreatesEquipmentTextInputs() throws Exception {
        Path sample = findRepoResource("按压式球囊扩充压力泵IDI-001", "过程检验记录.docx");
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
