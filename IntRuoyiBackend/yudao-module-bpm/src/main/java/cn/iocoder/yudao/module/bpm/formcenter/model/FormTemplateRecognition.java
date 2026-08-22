package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.List;

public class FormTemplateRecognition {

    private final boolean success;
    private final List<FormRecognizedField> fields;
    private final String failureReason;
    private final String jimuSchemaJson;

    private FormTemplateRecognition(boolean success, List<FormRecognizedField> fields, String failureReason) {
        this(success, fields, failureReason, null);
    }

    private FormTemplateRecognition(boolean success, List<FormRecognizedField> fields, String failureReason,
                                    String jimuSchemaJson) {
        this.success = success;
        this.fields = List.copyOf(fields);
        this.failureReason = failureReason;
        this.jimuSchemaJson = jimuSchemaJson;
    }

    public static FormTemplateRecognition success(List<FormRecognizedField> fields) {
        return new FormTemplateRecognition(true, fields, null);
    }

    public static FormTemplateRecognition success(List<FormRecognizedField> fields, String jimuSchemaJson) {
        return new FormTemplateRecognition(true, fields, null, jimuSchemaJson);
    }

    public static FormTemplateRecognition failure(String failureReason) {
        return new FormTemplateRecognition(false, List.of(), failureReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<FormRecognizedField> getFields() {
        return fields;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getJimuSchemaJson() {
        return jimuSchemaJson;
    }

}
