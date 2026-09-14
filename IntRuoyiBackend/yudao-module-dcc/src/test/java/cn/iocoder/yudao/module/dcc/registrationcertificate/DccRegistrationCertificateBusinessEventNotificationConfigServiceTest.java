package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateBusinessEventNotificationConfigServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DccRegistrationCertificateBusinessEventNotificationConfigService service =
            new DccRegistrationCertificateBusinessEventNotificationConfigService(jdbcTemplate);

    @Test
    void resolveRecipientUserIdsReadsThresholdRecipientConfigInsteadOfReminderJobParam() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(1L)))
                .thenReturn(List.of("""
                        {"T_30":[1001,1002],"T_8":[1002,1003],"T_2":[1003],"T_1":[]}
                        """));

        List<Long> recipientUserIds = service.resolveRecipientUserIds(1L);

        assertEquals(List.of(1001L, 1002L, 1003L), recipientUserIds);
    }

    @Test
    void missingActiveConfigFailsClearlyWithoutDefaultingToJobParam() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(1L))).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.resolveRecipientUserIds(1L));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT.getCode(), exception.getCode());
    }
}
