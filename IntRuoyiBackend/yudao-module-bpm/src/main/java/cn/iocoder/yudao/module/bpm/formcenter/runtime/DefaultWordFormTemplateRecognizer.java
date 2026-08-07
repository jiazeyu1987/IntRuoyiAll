package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import cn.iocoder.yudao.module.wordparser.SharedWordDocumentParser;
import cn.iocoder.yudao.module.wordparser.WordCell;
import cn.iocoder.yudao.module.wordparser.WordParseCommand;
import cn.iocoder.yudao.module.wordparser.WordParseException;
import cn.iocoder.yudao.module.wordparser.WordParseFailureCode;
import cn.iocoder.yudao.module.wordparser.WordParseProfile;
import cn.iocoder.yudao.module.wordparser.WordParseResult;
import cn.iocoder.yudao.module.wordparser.WordTable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DefaultWordFormTemplateRecognizer implements FormTemplateRecognizer {

    private static final int MAX_FIELDS = 300;
    private static final String NO_RECOGNIZABLE_FIELD_LABELS = "NO_RECOGNIZABLE_FIELD_LABELS";

    private final SharedWordDocumentParser sharedParser;

    public DefaultWordFormTemplateRecognizer(SharedWordDocumentParser sharedParser) {
        this.sharedParser = sharedParser;
    }

    @Override
    public FormTemplateRecognition recognize(FormTemplateImportCommand command) {
        String fileName = command.getSourceFileName().toLowerCase(Locale.ROOT);
        try {
            String extension = fileName.endsWith(".docx") ? ".docx" : ".doc";
            WordParseResult parsed = sharedParser.parse(new WordParseCommand(
                    command.getSourceBytes(), extension, command.getSourceFileName(),
                    WordParseProfile.STRUCTURAL_CANONICAL));
            List<String> labels = extractLabels(parsed);
            List<FormRecognizedField> fields = toFields(labels);
            if (fields.isEmpty()) {
                return FormTemplateRecognition.failure(NO_RECOGNIZABLE_FIELD_LABELS);
            }
            return FormTemplateRecognition.success(fields);
        } catch (WordParseException ex) {
            return FormTemplateRecognition.failure(toBusinessErrorCode(ex.code()), ex.code().name());
        }
    }

    private FormCenterErrorCode toBusinessErrorCode(WordParseFailureCode failureCode) {
        return switch (failureCode) {
            case EMPTY_SOURCE, CORRUPT_SOURCE -> FormCenterErrorCode.TEMPLATE_SOURCE_INVALID;
            case UNSUPPORTED_SOURCE_TYPE -> FormCenterErrorCode.TEMPLATE_SOURCE_TYPE_UNSUPPORTED;
            case INVALID_TABLE_STRUCTURE, NO_PARSEABLE_CONTENT ->
                    FormCenterErrorCode.TEMPLATE_RECOGNITION_FAILED;
        };
    }

    private List<String> extractLabels(WordParseResult parsed) {
        Set<String> labels = new LinkedHashSet<>();
        parsed.paragraphs().forEach(paragraph -> addLabel(labels, paragraph));
        for (WordTable table : parsed.tables()) {
            for (List<WordCell> row : table.rows()) {
                for (WordCell cell : row) {
                    if (cell != null) {
                        addLabel(labels, cell.text());
                    }
                }
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
