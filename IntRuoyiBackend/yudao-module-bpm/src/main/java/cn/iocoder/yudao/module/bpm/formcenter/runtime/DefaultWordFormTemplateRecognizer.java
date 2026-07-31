package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class DefaultWordFormTemplateRecognizer implements FormTemplateRecognizer {

    private static final int MAX_FIELDS = 300;

    @Override
    public FormTemplateRecognition recognize(FormTemplateImportCommand command) {
        String fileName = command.getSourceFileName().toLowerCase(Locale.ROOT);
        try {
            List<String> labels = fileName.endsWith(".docx")
                    ? extractDocxLabels(command.getSourceBytes())
                    : extractDocLabels(command.getSourceBytes());
            List<FormRecognizedField> fields = toFields(labels);
            if (fields.isEmpty()) {
                return FormTemplateRecognition.failure("no recognizable text field labels");
            }
            return FormTemplateRecognition.success(fields);
        } catch (Exception ex) {
            return FormTemplateRecognition.failure(ex.getMessage());
        }
    }

    private List<String> extractDocxLabels(byte[] bytes) throws Exception {
        Set<String> labels = new LinkedHashSet<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            document.getParagraphs().forEach(paragraph -> addLabel(labels, paragraph.getText()));
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        addLabel(labels, cell.getText());
                    }
                }
            }
        }
        return new ArrayList<>(labels);
    }

    private List<String> extractDocLabels(byte[] bytes) throws Exception {
        Set<String> labels = new LinkedHashSet<>();
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            for (String paragraph : extractor.getParagraphText()) {
                addLabel(labels, paragraph);
            }
        }
        return new ArrayList<>(labels);
    }

    private void addLabel(Set<String> labels, String text) {
        String normalized = StrUtil.cleanBlank(text);
        if (StrUtil.isBlank(normalized) || normalized.length() > 80) {
            return;
        }
        labels.add(normalized);
    }

    private List<FormRecognizedField> toFields(List<String> labels) {
        List<FormRecognizedField> fields = new ArrayList<>();
        int index = 1;
        for (String label : labels) {
            if (fields.size() >= MAX_FIELDS) {
                break;
            }
            fields.add(FormRecognizedField.of(toFieldCode(label, index), label, guessFieldType(label),
                    label.contains("*") || label.contains("必填")));
            index++;
        }
        return fields;
    }

    private String toFieldCode(String label, int index) {
        String code = label.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return StrUtil.isBlank(code) ? "field" + index : code;
    }

    private String guessFieldType(String label) {
        if (label.contains("日期") || label.toLowerCase(Locale.ROOT).contains("date")) {
            return "date";
        }
        if (label.contains("是否") || label.contains("合格") || label.contains("□")) {
            return "checkbox";
        }
        if (label.length() > 20 || label.contains("原因") || label.contains("说明")) {
            return "textarea";
        }
        return "input";
    }

}
