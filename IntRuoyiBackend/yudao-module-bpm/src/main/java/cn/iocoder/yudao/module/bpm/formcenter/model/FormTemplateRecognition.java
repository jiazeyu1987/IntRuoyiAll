package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.List;
import java.util.Objects;

public class FormTemplateRecognition {

    private final boolean success;
    private final List<FormRecognizedField> fields;
    private final FormCenterErrorCode failureCode;
    private final String failureReason;

    private FormTemplateRecognition(boolean success, List<FormRecognizedField> fields,
                                    FormCenterErrorCode failureCode, String failureReason) {
        this.success = success;
        this.fields = List.copyOf(fields);
        this.failureCode = failureCode;
        this.failureReason = failureReason;
    }

    public static FormTemplateRecognition success(List<FormRecognizedField> fields) {
        return new FormTemplateRecognition(true, fields, null, null);
    }

    public static FormTemplateRecognition failure(String failureReason) {
        return failure(FormCenterErrorCode.TEMPLATE_RECOGNITION_FAILED, failureReason);
    }

    public static FormTemplateRecognition failure(FormCenterErrorCode failureCode, String failureReason) {
        return new FormTemplateRecognition(false, List.of(), Objects.requireNonNull(failureCode), failureReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<FormRecognizedField> getFields() {
        return fields;
    }

    public FormCenterErrorCode getFailureCode() {
        return failureCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

}
