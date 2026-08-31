package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WordTableVisualSchemaBuilderTypeRecognitionTest {

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
}
