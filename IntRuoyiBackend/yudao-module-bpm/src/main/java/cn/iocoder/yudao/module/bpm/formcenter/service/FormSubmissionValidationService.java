package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateFieldRule;

import java.util.List;
import java.util.Map;

public class FormSubmissionValidationService {

    public void validateRequiredFields(List<FormTemplateFieldRule> fieldRules, Map<String, Object> formData) {
        for (FormTemplateFieldRule fieldRule : fieldRules) {
            Object value = formData.get(fieldRule.getFieldCode());
            if (fieldRule.isRequired() && (value == null || (value instanceof String text && text.isBlank()))) {
                throw new FormCenterException(FormCenterErrorCode.FORM_REQUIRED_FIELD_MISSING,
                        "Required form field missing: " + fieldRule.getFieldCode());
            }
        }
    }

}
