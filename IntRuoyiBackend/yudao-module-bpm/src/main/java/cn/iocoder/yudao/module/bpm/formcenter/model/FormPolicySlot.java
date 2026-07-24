package cn.iocoder.yudao.module.bpm.formcenter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FormPolicySlot {

    private final String slotCode;
    private final boolean required;
    private final FormTemplateVersionRef templateVersionRef;

    @JsonCreator
    public FormPolicySlot(@JsonProperty("slotCode") String slotCode,
            @JsonProperty("required") boolean required,
            @JsonProperty("templateVersionRef") FormTemplateVersionRef templateVersionRef) {
        this.slotCode = slotCode;
        this.required = required;
        this.templateVersionRef = templateVersionRef;
    }

    public static FormPolicySlot required(String slotCode, FormTemplateVersionRef templateVersionRef) {
        return new FormPolicySlot(slotCode, true, templateVersionRef);
    }

    public String getSlotCode() {
        return slotCode;
    }

    public boolean isRequired() {
        return required;
    }

    public FormTemplateVersionRef getTemplateVersionRef() {
        return templateVersionRef;
    }

}
