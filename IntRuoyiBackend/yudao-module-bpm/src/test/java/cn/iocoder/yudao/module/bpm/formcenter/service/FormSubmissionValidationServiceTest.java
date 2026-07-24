package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateFieldRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormSubmissionValidationServiceTest {

    @Test
    void requiredFieldMissingBlocksSubmitBeforeBpmStarts() {
        FormSubmissionValidationService service = new FormSubmissionValidationService();

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.validateRequiredFields(List.of(
                        FormTemplateFieldRule.required("changeReason", "Change Reason")), Map.of()));

        assertEquals(FormCenterErrorCode.FORM_REQUIRED_FIELD_MISSING, ex.getErrorCode());
    }

}
