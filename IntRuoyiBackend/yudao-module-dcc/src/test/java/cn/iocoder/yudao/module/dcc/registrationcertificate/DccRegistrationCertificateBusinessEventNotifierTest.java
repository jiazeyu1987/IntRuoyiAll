package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationConfigService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateChangeConfirmationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateViewerRecipientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class DccRegistrationCertificateBusinessEventNotifierTest {

    private final DccRegistrationCertificateBusinessEventNotificationConfigService configService =
            mock(DccRegistrationCertificateBusinessEventNotificationConfigService.class);
    private final DccRegistrationCertificateBusinessEventNotificationService notificationService =
            mock(DccRegistrationCertificateBusinessEventNotificationService.class);
    private final DccRegistrationCertificateChangeConfirmationService confirmations =
            mock(DccRegistrationCertificateChangeConfirmationService.class);
    private final DccRegistrationCertificateViewerRecipientResolver viewers =
            mock(DccRegistrationCertificateViewerRecipientResolver.class);
    private final DccRegistrationCertificateBusinessEventNotifier notifier =
            new DccRegistrationCertificateBusinessEventNotifier(configService, notificationService, confirmations, viewers);

    @BeforeEach
    void resetMocks() {
        reset(configService, notificationService, confirmations, viewers);
    }

    @Test
    void missingChangeConfirmationRecipientsFailsApprovalExplicitly() {
        when(configService.resolveChangeConfirmationRecipientUserIds(1L))
                .thenThrow(new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED));

        assertThrows(ServiceException.class,this::notifyChangeApprovalRecorded);

        verify(notificationService, never()).send(any());
    }

    @Test
    void configuredReminderScopeSendsChangeApprovalRecordedNotification() {
        when(configService.resolveChangeConfirmationRecipientUserIds(1L)).thenReturn(List.of(1001L,1002L));

        notifyChangeApprovalRecorded();

        ArgumentCaptor<DccRegistrationCertificateBusinessEventNotificationCommand> captor =
                ArgumentCaptor.forClass(DccRegistrationCertificateBusinessEventNotificationCommand.class);
        verify(confirmations).send(captor.capture(),org.mockito.ArgumentMatchers.eq(List.of(1001L,1002L)));
        DccRegistrationCertificateBusinessEventNotificationCommand command = captor.getValue();
        assertEquals("CHANGE_APPROVAL_RECORDED", command.eventType());
        assertEquals("approval-key-001", command.eventKey());
        assertEquals(List.of(), command.documentControlRoleIds());
        assertEquals("变更已生效，请确认", command.detailParams().get("eventTitle"));
        assertEquals("变更后产品名称", command.detailParams().get("productName"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEW_CERTIFICATE_FORMALIZED", "RENEWAL_CANDIDATE_UPLOADED", "RENEWAL_CANDIDATE_ACTIVATED"})
    void certificateEventsUseCurrentTenantConfiguredRecipients(String eventType) {
        when(configService.resolveRecipientUserIds(186L)).thenReturn(List.of(1001L, 1002L));

        notifyCertificateEvent(eventType);

        ArgumentCaptor<DccRegistrationCertificateBusinessEventNotificationCommand> captor =
                ArgumentCaptor.forClass(DccRegistrationCertificateBusinessEventNotificationCommand.class);
        verify(notificationService).sendToUsers(captor.capture(), eq(List.of(1001L, 1002L)));
        var command = captor.getValue();
        assertEquals(186L, command.tenantId());
        assertEquals(eventType, command.eventType());
        assertEquals(List.of(), command.documentControlRoleIds());
        assertEquals(null, command.recipientPermission());
        verify(notificationService, never()).send(any());
    }

    @Test
    void missingTenantConfigFailsExplicitlyInsteadOfSkippingNotification() {
        var failure = new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        when(configService.resolveRecipientUserIds(186L)).thenThrow(failure);

        assertSame(failure, assertThrows(ServiceException.class,
                () -> notifyCertificateEvent("NEW_CERTIFICATE_FORMALIZED")));
        verifyNoInteractions(notificationService);
    }

    @Test
    void deliveryFailurePropagatesToApprovalTransaction() {
        when(configService.resolveRecipientUserIds(186L)).thenReturn(List.of(1001L));
        var failure = new IllegalStateException("notification delivery failed");
        when(notificationService.sendToUsers(any(), eq(List.of(1001L)))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> notifyCertificateEvent("NEW_CERTIFICATE_FORMALIZED")));
    }

    @Test
    void invalidEventParamsStillFailBeforeResolvingReminderJobConfig() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> notifier.notifyChangeApprovalRecorded(
                        1L, 501L, 1001L, 8001L, 99L, "approval-key-001",
                        " ", "NMPA-001", LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1), "DOMESTIC"));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING.getCode(), exception.getCode());
        verifyNoInteractions(configService);
        verify(notificationService, never()).send(any());
    }

    private void notifyChangeApprovalRecorded() {
        notifier.notifyChangeApprovalRecorded(
                1L, 501L, 1001L, 8001L, 99L, "approval-key-001",
                "变更后产品名称", "NMPA-001", LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1), "DOMESTIC");
    }

    private void notifyCertificateEvent(String eventType) {
        switch (eventType) {
            case "NEW_CERTIFICATE_FORMALIZED" -> notifier.notifyNewCertificateFormalized(
                    186L, 501L, 1001L, 8001L, 99L, "event-key", "产品", "REG-001", null, null, "DOMESTIC", true);
            case "RENEWAL_CANDIDATE_UPLOADED" -> notifier.notifyRenewalCandidateUploaded(
                    186L, 501L, 1001L, 8001L, 99L, "event-key", "产品", "REG-001", null, null, "DOMESTIC");
            case "RENEWAL_CANDIDATE_ACTIVATED" -> notifier.notifyRenewalCandidateActivated(
                    186L, 501L, 1001L, 8001L, 99L, "event-key", "产品", "REG-001", null, null, "DOMESTIC");
            default -> throw new IllegalArgumentException(eventType);
        }
    }
}
