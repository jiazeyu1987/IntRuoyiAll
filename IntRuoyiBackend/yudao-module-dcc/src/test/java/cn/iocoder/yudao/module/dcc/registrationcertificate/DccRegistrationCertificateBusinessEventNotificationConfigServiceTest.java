package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

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

        assertEquals(Set.of(1001L, 1002L, 1003L), Set.copyOf(recipientUserIds));
        assertEquals(3, recipientUserIds.size());
    }

    @Test
    void missingActiveConfigFailsClearlyWithoutDefaultingToJobParam() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(1L))).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.resolveRecipientUserIds(1L));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void recipientsAreIsolatedByTenant() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(186L)))
                .thenReturn(List.of("{\"T_30\":[1001]}"));
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(187L)))
                .thenReturn(List.of("{\"T_30\":[2001]}"));

        assertEquals(List.of(1001L), service.resolveRecipientUserIds(186L));
        assertEquals(List.of(2001L), service.resolveRecipientUserIds(187L));
    }

    @Test
    void existingEmptyRecipientConfigDoesNotInventRecipients() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(186L)))
                .thenReturn(List.of("{\"T_30\":[],\"T_8\":[],\"T_2\":[],\"T_1\":[]}"));

        assertEquals(List.of(), service.resolveRecipientUserIds(186L));
    }
}
