package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipient;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipientResolver;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateRecipientResolverTest {

    private final MdmCompanyScopeApi companyScopeApi = mock(MdmCompanyScopeApi.class);
    private final DccRegistrationCertificateRecipientResolver resolver =
            new DccRegistrationCertificateRecipientResolver(companyScopeApi);

    @Test
    void resolvesEnabledExactScopeRecipientsAndDeduplicatesInReturnedOrder() {
        when(companyScopeApi.resolveRecipientUserIds(
                501L, List.of(1001L, 1002L), "dcc:registration-certificate:reminder:receive"))
                .thenReturn(new LinkedHashSet<>(List.of(22L, 11L)));

        List<DccRegistrationCertificateRecipient> recipients = resolver.resolve(
                501L, List.of(1001L, 1002L), "dcc:registration-certificate:reminder:receive");

        assertEquals(List.of(
                new DccRegistrationCertificateRecipient(22L, 501L),
                new DccRegistrationCertificateRecipient(11L, 501L)), recipients);
        verify(companyScopeApi).resolveRecipientUserIds(
                501L, List.of(1001L, 1002L), "dcc:registration-certificate:reminder:receive");
    }

    @Test
    void missingOrInvalidRecipientMappingFailsDeliveryRun() {
        when(companyScopeApi.resolveRecipientUserIds(
                501L, List.of(1001L), "dcc:registration-certificate:reminder:receive"))
                .thenReturn(Set.of());

        ServiceException exception = assertThrows(ServiceException.class, () -> resolver.resolve(
                501L, List.of(1001L), "dcc:registration-certificate:reminder:receive"));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void infrastructureFailureRemainsVisible() {
        IllegalStateException infrastructureFailure = new IllegalStateException("mdm unavailable");
        when(companyScopeApi.resolveRecipientUserIds(
                501L, List.of(1001L), "dcc:registration-certificate:reminder:receive"))
                .thenThrow(infrastructureFailure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> resolver.resolve(
                501L, List.of(1001L), "dcc:registration-certificate:reminder:receive"));

        assertSame(infrastructureFailure, thrown);
    }
}
