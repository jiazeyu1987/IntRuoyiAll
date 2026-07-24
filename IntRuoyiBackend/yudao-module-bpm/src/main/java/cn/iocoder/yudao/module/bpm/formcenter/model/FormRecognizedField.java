package cn.iocoder.yudao.module.bpm.formcenter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FormRecognizedField {

    private final String fieldCode;
    private final String label;
    private final String fieldType;
    private final boolean required;

    @JsonCreator
    private FormRecognizedField(@JsonProperty("fieldCode") String fieldCode,
                                @JsonProperty("label") String label,
                                @JsonProperty("fieldType") String fieldType,
                                @JsonProperty("required") boolean required) {
        this.fieldCode = fieldCode;
        this.label = label;
        this.fieldType = fieldType;
        this.required = required;
    }

    public static FormRecognizedField required(String fieldCode, String label, String fieldType) {
        return new FormRecognizedField(fieldCode, label, fieldType, true);
    }

    public static FormRecognizedField of(String fieldCode, String label, String fieldType, boolean required) {
        return new FormRecognizedField(fieldCode, label, fieldType, required);
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getLabel() {
        return label;
    }

    public String getFieldType() {
        return fieldType;
    }

    public boolean isRequired() {
        return required;
    }

}
