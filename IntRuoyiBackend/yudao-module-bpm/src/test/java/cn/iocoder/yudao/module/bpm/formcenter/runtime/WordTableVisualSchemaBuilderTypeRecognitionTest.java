package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.xmlbeans.XmlCursor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordTableVisualSchemaBuilderTypeRecognitionTest {

    @Test
    void buildExpandsNestedTableInsideParentCell() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable parent = document.createTable(1, 2);
            parent.getCTTbl().addNewTblGrid();
            for (int index = 0; index < 7; index++) {
                parent.getCTTbl().getTblGrid().addNewGridCol().setW(1000);
            }
            XWPFTableCell sectionCell = parent.getRow(0).getCell(0);
            sectionCell.setText("生产操作");
            XWPFTableCell nestedHostCell = parent.getRow(0).getCell(1);
            nestedHostCell.getCTTc().addNewTcPr().addNewGridSpan().setVal(BigInteger.valueOf(6));

            XWPFTable nested;
            try (XmlCursor cursor = nestedHostCell.getCTTc().newCursor()) {
                cursor.toEndToken();
                nested = nestedHostCell.insertNewTbl(cursor);
            }
            nested.getCTTbl().addNewTblGrid();
            for (int index = 0; index < 3; index++) {
                nested.getCTTbl().getTblGrid().addNewGridCol().setW(1000);
            }
            appendNestedRow(nested, "物料编码", "物料名称", "批号");
            appendNestedRow(nested, "M-001", "外套", "");

            ByteArrayOutputStream serialized = new ByteArrayOutputStream();
            document.write(serialized);
            String schemaJson;
            try (XWPFDocument reopened = new XWPFDocument(
                    new ByteArrayInputStream(serialized.toByteArray()))) {
                schemaJson = WordTableVisualSchemaBuilder.build(reopened.getTables().get(0));
            }
            Map<String, Object> schema = JsonUtils.parseObject(schemaJson, new TypeReference<>() { });
            Map<String, Object> layout = JsonUtils.parseObject(
                    (String) schema.get("sheetLayoutJson"), new TypeReference<>() { });
            Map<String, Object> rows = castMap(layout.get("rows"));
            assertEquals(2, ((Number) rows.get("len")).intValue());
            assertEquals("生产操作", castMap(cellAt(rows, 0, 0)).get("text"));
            assertEquals(List.of(1, 0), castMap(cellAt(rows, 0, 0)).get("merge"));
            assertEquals("物料编码", castMap(cellAt(rows, 0, 1)).get("text"));
            assertEquals("物料名称", castMap(cellAt(rows, 0, 3)).get("text"));
            assertEquals("批号", castMap(cellAt(rows, 0, 5)).get("text"));
            assertEquals("M-001", castMap(cellAt(rows, 1, 1)).get("text"));
            assertEquals("外套", castMap(cellAt(rows, 1, 3)).get("text"));

            List<Map<String, Object>> rules = castMapList(schema.get("cellRules"));
            assertTrue(rules.stream().anyMatch(rule -> ((Number) rule.get("rowIndex")).intValue() == 1
                    && ((Number) rule.get("columnIndex")).intValue() == 5
                    && "批号".equals(rule.get("label"))));
        }
    }

    @Test
    void buildRecognizesNumericDateCheckboxGroupAndSignatureCells() throws Exception {
        Path fixture = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001", "old", "过程检验记录.docx")
                .toAbsolutePath().normalize();
        try (InputStream input = Files.newInputStream(fixture)) {
            try (XWPFDocument document = new XWPFDocument(input)) {
                Map<String, Object> schema = JsonUtils.parseObject(
                        WordTableVisualSchemaBuilder.build(document.getTables().get(0)),
                        new TypeReference<>() {
                        });
                List<Map<String, Object>> rules = (List<Map<String, Object>>) schema.get("cellRules");
                List<Map<String, Object>> signatures =
                        (List<Map<String, Object>>) schema.get("signatureCellMarkers");

                assertTrue(rules.stream().anyMatch(rule -> "NUMBER".equals(rule.get("valueType"))
                        && "input-number".equals(rule.get("componentFlag"))));
                assertTrue(rules.stream().anyMatch(rule -> "生产批号".equals(rule.get("label"))
                        && "STRING".equals(rule.get("valueType"))));
                assertTrue(rules.stream().anyMatch(rule -> "型号/规格".equals(rule.get("label"))
                        && "STRING".equals(rule.get("valueType"))));
                assertTrue(rules.stream().anyMatch(rule -> "批数量".equals(rule.get("label"))
                        && "NUMBER".equals(rule.get("valueType"))));
                assertTrue(rules.stream().anyMatch(rule -> "合格数量".equals(rule.get("label"))
                        && "NUMBER".equals(rule.get("valueType"))
                        && "input-number".equals(rule.get("componentFlag"))));
                assertTrue(rules.stream().anyMatch(rule -> "不合格数量".equals(rule.get("label"))
                        && "NUMBER".equals(rule.get("valueType"))
                        && "input-number".equals(rule.get("componentFlag"))));
                assertTrue(rules.stream().anyMatch(rule -> "不合格评审报告编号（若有）".equals(rule.get("label"))
                        && "STRING".equals(rule.get("valueType"))
                        && "input-text".equals(rule.get("componentFlag"))));
                assertTrue(rules.stream().anyMatch(rule -> "备注".equals(rule.get("label"))
                        && "STRING".equals(rule.get("valueType"))
                        && "textarea".equals(rule.get("componentFlag"))));
                assertTrue(rules.stream().anyMatch(rule -> "DATE".equals(rule.get("valueType"))
                        && "date".equals(rule.get("componentFlag"))));
                assertTrue(rules.stream().anyMatch(rule -> "STRING".equals(rule.get("valueType"))
                        && "radio-group".equals(rule.get("componentFlag"))
                        && rule.toString().contains("符合要求")
                        && rule.toString().contains("不符合要求")));
                assertTrue(signatures.stream().anyMatch(marker -> Boolean.TRUE.equals(marker.get("enabled"))
                        && "FORM_REVIEW".equals(marker.get("actionType"))));
            }
        }
    }

    @Test
    void recognizeFieldsClassifiesNumberDateCheckboxGroupAndSignature() throws Exception {
        Path fixture = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001", "old", "过程检验记录.docx")
                .toAbsolutePath().normalize();
        FormTemplateImportCommand command = FormTemplateImportCommand.of(
                "按压式压力泵过程检验记录", "V8.0", "过程检验记录.docx", Files.readAllBytes(fixture), "");
        FormTemplateRecognition recognition = new DefaultWordFormTemplateRecognizer().recognize(command);

        assertTrue(recognition.isSuccess());
        assertTrue(recognition.getFields().stream().anyMatch(field -> "number".equals(field.getFieldType())));
        assertTrue(recognition.getFields().stream().anyMatch(field -> "date".equals(field.getFieldType())));
        assertTrue(recognition.getFields().stream().anyMatch(field -> "checkbox-group".equals(field.getFieldType())));
        assertTrue(recognition.getFields().stream().anyMatch(field -> "signature".equals(field.getFieldType())));
    }

    private static void appendNestedRow(XWPFTable table, String... values) {
        XWPFTableRow row = table.createRow();
        while (row.getTableCells().size() < values.length) {
            row.createCell();
        }
        for (int index = 0; index < values.length; index++) {
            row.getCell(index).setText(values[index]);
        }
    }

    private static Object cellAt(Map<String, Object> rows, int rowIndex, int columnIndex) {
        return castMap(castMap(rows.get(String.valueOf(rowIndex))).get("cells"))
                .get(String.valueOf(columnIndex));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castMapList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
