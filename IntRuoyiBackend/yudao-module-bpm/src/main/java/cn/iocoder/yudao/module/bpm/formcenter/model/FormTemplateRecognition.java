package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.List;

public class FormTemplateRecognition {

    private final boolean success;
    private final List<FormRecognizedField> fields;
    private final String failureReason;

    private FormTemplateRecognition(boolean success, List<FormRecognizedField> fields, String failureReason) {
        this.success = success;
        this.fields = List.copyOf(fields);
        this.failureReason = failureReason;
    }

    public static FormTemplateRecognition success(List<FormRecognizedField> fields) {
        return new FormTemplateRecognition(true, fields, null);
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

}
