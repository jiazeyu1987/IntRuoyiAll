package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo.DccRegistrationCertificateReminderConfigUpdateReqVO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateReminderConfigUpdateReqVOTest {

    @Test
    void acceptsFourThresholdsAndChangeConfirmationRecipients() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request()).isEmpty());
        }
    }

    @Test
    void rejectsMissingRecipientGroupAtControllerBoundary() {
        var request = request();
        var recipients = new LinkedHashMap<>(request.getThresholdRecipientUserIds());
        recipients.remove("CHANGE_CONFIRMATION");
        request.setThresholdRecipientUserIds(recipients);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);
            assertEquals(1, violations.size());
            assertEquals("thresholdRecipientUserIds", violations.iterator().next().getPropertyPath().toString());
        }
    }

    private static DccRegistrationCertificateReminderConfigUpdateReqVO request() {
        var request = new DccRegistrationCertificateReminderConfigUpdateReqVO();
        request.setEnabled(true);
        request.setDailyRunTime("09:00");
        request.setExpectedRowVersion(1);
        request.setThresholdRecipientUserIds(Map.of(
                "T_30", List.of(1L), "T_8", List.of(1L),
                "T_2", List.of(1L), "T_1", List.of(1L),
                "CHANGE_CONFIRMATION", List.of(910522L, 910524L)));
        return request;
    }
}
