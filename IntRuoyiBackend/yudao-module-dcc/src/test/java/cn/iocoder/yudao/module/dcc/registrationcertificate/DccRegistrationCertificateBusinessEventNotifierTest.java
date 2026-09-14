package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationConfigService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateBusinessEventNotifierTest {

    private static final String PERMISSION = "dcc:registration-certificate:event:receive";

    private final DccRegistrationCertificateBusinessEventNotificationConfigService configService =
            mock(DccRegistrationCertificateBusinessEventNotificationConfigService.class);
    private final DccRegistrationCertificateBusinessEventNotificationService notificationService =
            mock(DccRegistrationCertificateBusinessEventNotificationService.class);
    private final DccRegistrationCertificateBusinessEventNotifier notifier =
            new DccRegistrationCertificateBusinessEventNotifier(configService, notificationService);

    @BeforeEach
    void resetMocks() {
        reset(configService, notificationService);
    }

    @Test
    void missingReminderJobConfigSkipsBusinessEventNotificationWithoutBlockingApproval() {
        when(configService.resolveRecipientScope())
                .thenThrow(new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED));

        assertDoesNotThrow(this::notifyChangeApprovalRecorded);

        verify(notificationService, never()).send(any());
    }

    @Test
    void configuredReminderScopeSendsChangeApprovalRecordedNotification() {
        when(configService.resolveRecipientScope())
                .thenReturn(new DccRegistrationCertificateBusinessEventNotificationConfigService.RecipientScope(
                        List.of(1001L, 1002L), PERMISSION));

        notifyChangeApprovalRecorded();

        ArgumentCaptor<DccRegistrationCertificateBusinessEventNotificationCommand> captor =
                ArgumentCaptor.forClass(DccRegistrationCertificateBusinessEventNotificationCommand.class);
        verify(notificationService).send(captor.capture());
        DccRegistrationCertificateBusinessEventNotificationCommand command = captor.getValue();
        assertEquals("CHANGE_APPROVAL_RECORDED", command.eventType());
        assertEquals("approval-key-001", command.eventKey());
        assertEquals(List.of(1001L, 1002L), command.documentControlRoleIds());
        assertEquals(PERMISSION, command.recipientPermission());
        assertEquals("变更批件已记录", command.detailParams().get("eventTitle"));
        assertEquals("变更后产品名称", command.detailParams().get("productName"));
    }

    @Test
    void invalidEventParamsStillFailBeforeResolvingReminderJobConfig() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> notifier.notifyChangeApprovalRecorded(
                        1L, 501L, 1001L, 8001L, 99L, "approval-key-001",
                        " ", "NMPA-001", LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1)));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING.getCode(), exception.getCode());
        verify(configService, never()).resolveRecipientScope();
        verify(notificationService, never()).send(any());
    }

    private void notifyChangeApprovalRecorded() {
        notifier.notifyChangeApprovalRecorded(
                1L, 501L, 1001L, 8001L, 99L, "approval-key-001",
                "变更后产品名称", "NMPA-001", LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1));
    }
}
