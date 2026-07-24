package cn.iocoder.yudao.module.bpm.formcenter.model;

public class FormTemplateFieldRule {

    private final String fieldCode;
    private final String label;
    private final boolean required;

    private FormTemplateFieldRule(String fieldCode, String label, boolean required) {
        this.fieldCode = fieldCode;
        this.label = label;
        this.required = required;
    }

    public static FormTemplateFieldRule required(String fieldCode, String label) {
        return new FormTemplateFieldRule(fieldCode, label, true);
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRequired() {
        return required;
    }

}
