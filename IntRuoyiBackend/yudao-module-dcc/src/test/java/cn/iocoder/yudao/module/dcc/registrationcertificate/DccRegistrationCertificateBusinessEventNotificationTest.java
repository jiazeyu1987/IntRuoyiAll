package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotificationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipient;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipientResolver;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_EVENT_NOTIFICATION_SCOPE_UNAPPROVED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateBusinessEventNotificationTest {

    private static final String PERMISSION = "dcc:registration-certificate:event:receive";
    private static final List<Long> ROLE_IDS = List.of(1001L, 1002L);
    private static final List<String> APPROVED_EVENTS = List.of(
            "NEW_CERTIFICATE_FORMALIZED",
            "RENEWAL_CANDIDATE_UPLOADED",
            "RENEWAL_CANDIDATE_ACTIVATED",
            "CHANGE_APPROVAL_RECORDED",
            "SUPPORTING_FILE_CONFIRMED");

    private final DccRegistrationCertificateRecipientResolver recipientResolver =
            mock(DccRegistrationCertificateRecipientResolver.class);
    private final NotifyMessageSendApi notifyMessageSendApi = mock(NotifyMessageSendApi.class);
    private final DccRegistrationCertificateBusinessEventNotificationService service =
            new DccRegistrationCertificateBusinessEventNotificationService(recipientResolver, notifyMessageSendApi);

    @BeforeEach
    void resetMocks() {
        reset(recipientResolver, notifyMessageSendApi);
    }

    @Test
    void approvedMatrixSendsToCompanyRecipientsAndActorWithStableBusinessKeys() {
        when(recipientResolver.resolve(501L, ROLE_IDS, PERMISSION)).thenReturn(List.of(
                new DccRegistrationCertificateRecipient(22L, 501L),
                new DccRegistrationCertificateRecipient(11L, 501L),
                new DccRegistrationCertificateRecipient(99L, 501L)));
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(invocation -> {
            NotifySendSingleToUserIdempotentReqDTO req = invocation.getArgument(0);
            return 900000L + req.getUserId();
        });

        Map<String, List<String>> keysByEvent = new LinkedHashMap<>();
        for (String eventType : APPROVED_EVENTS) {
            DccRegistrationCertificateBusinessEventNotificationResult result = service.send(command(eventType));

            assertEquals(Map.of(22L, 900022L, 11L, 900011L, 99L, 900099L),
                    result.recipientMessageIds(), eventType + " must send to exact approved recipients");
            keysByEvent.put(eventType, capturedBusinessKeysAndReset());
        }

        assertEquals(Map.of(
                "NEW_CERTIFICATE_FORMALIZED", List.of(
                        "REG_CERT:NEW_CERTIFICATE_FORMALIZED:EVENT-NEW_CERTIFICATE_FORMALIZED:USER:22",
                        "REG_CERT:NEW_CERTIFICATE_FORMALIZED:EVENT-NEW_CERTIFICATE_FORMALIZED:USER:11",
                        "REG_CERT:NEW_CERTIFICATE_FORMALIZED:EVENT-NEW_CERTIFICATE_FORMALIZED:USER:99"),
                "RENEWAL_CANDIDATE_UPLOADED", List.of(
                        "REG_CERT:RENEWAL_CANDIDATE_UPLOADED:EVENT-RENEWAL_CANDIDATE_UPLOADED:USER:22",
                        "REG_CERT:RENEWAL_CANDIDATE_UPLOADED:EVENT-RENEWAL_CANDIDATE_UPLOADED:USER:11",
                        "REG_CERT:RENEWAL_CANDIDATE_UPLOADED:EVENT-RENEWAL_CANDIDATE_UPLOADED:USER:99"),
                "RENEWAL_CANDIDATE_ACTIVATED", List.of(
                        "REG_CERT:RENEWAL_CANDIDATE_ACTIVATED:EVENT-RENEWAL_CANDIDATE_ACTIVATED:USER:22",
                        "REG_CERT:RENEWAL_CANDIDATE_ACTIVATED:EVENT-RENEWAL_CANDIDATE_ACTIVATED:USER:11",
                        "REG_CERT:RENEWAL_CANDIDATE_ACTIVATED:EVENT-RENEWAL_CANDIDATE_ACTIVATED:USER:99"),
                "CHANGE_APPROVAL_RECORDED", List.of(
                        "REG_CERT:CHANGE_APPROVAL_RECORDED:EVENT-CHANGE_APPROVAL_RECORDED:USER:22",
                        "REG_CERT:CHANGE_APPROVAL_RECORDED:EVENT-CHANGE_APPROVAL_RECORDED:USER:11",
                        "REG_CERT:CHANGE_APPROVAL_RECORDED:EVENT-CHANGE_APPROVAL_RECORDED:USER:99"),
                "SUPPORTING_FILE_CONFIRMED", List.of(
                        "REG_CERT:SUPPORTING_FILE_CONFIRMED:EVENT-SUPPORTING_FILE_CONFIRMED:USER:22",
                        "REG_CERT:SUPPORTING_FILE_CONFIRMED:EVENT-SUPPORTING_FILE_CONFIRMED:USER:11",
                        "REG_CERT:SUPPORTING_FILE_CONFIRMED:EVENT-SUPPORTING_FILE_CONFIRMED:USER:99")),
                keysByEvent);
    }

    @Test
    void sameEventReplayUsesTheSameRecipientBusinessKeys() {
        when(recipientResolver.resolve(501L, ROLE_IDS, PERMISSION)).thenReturn(List.of(
                new DccRegistrationCertificateRecipient(22L, 501L)));
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(900022L);

        service.send(command("NEW_CERTIFICATE_FORMALIZED"));
        List<String> firstKeys = capturedBusinessKeysAndReset();
        service.send(command("NEW_CERTIFICATE_FORMALIZED"));
        List<String> replayKeys = capturedBusinessKeysAndReset();

        assertEquals(firstKeys, replayKeys);
        assertEquals(List.of(
                        "REG_CERT:NEW_CERTIFICATE_FORMALIZED:EVENT-NEW_CERTIFICATE_FORMALIZED:USER:22",
                        "REG_CERT:NEW_CERTIFICATE_FORMALIZED:EVENT-NEW_CERTIFICATE_FORMALIZED:USER:99"),
                replayKeys);
    }

    @Test
    void unapprovedEventsNeverSendNotification() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.send(command("THRESHOLD_REMINDER")));

        assertEquals(REGISTRATION_CERTIFICATE_EVENT_NOTIFICATION_SCOPE_UNAPPROVED.getCode(), exception.getCode());
        verify(recipientResolver, never()).resolve(any(), any(), any());
        verify(notifyMessageSendApi, never()).sendSingleMessageIdempotentlyToAdmin(any());
    }

    @Test
    void missingRequiredEventParamsFailBeforeRecipientOrNotify() {
        DccRegistrationCertificateBusinessEventNotificationCommand command =
                new DccRegistrationCertificateBusinessEventNotificationCommand(
                        1L, 501L, 1001L, 8001L, 99L,
                        "NEW_CERTIFICATE_FORMALIZED", " ", ROLE_IDS, PERMISSION, Map.of());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.send(command));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING.getCode(), exception.getCode());
        verify(recipientResolver, never()).resolve(any(), any(), any());
        verify(notifyMessageSendApi, never()).sendSingleMessageIdempotentlyToAdmin(any());
    }

    @Test
    void recipientResolutionFailureDoesNotDefaultToAdmin() {
        ServiceException recipientFailure = new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
        when(recipientResolver.resolve(501L, ROLE_IDS, PERMISSION)).thenThrow(recipientFailure);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.send(command("RENEWAL_CANDIDATE_UPLOADED")));

        assertSame(recipientFailure, exception);
        verify(notifyMessageSendApi, never()).sendSingleMessageIdempotentlyToAdmin(any());
    }

    @Test
    void emptyMessageIdFailsClearly() {
        when(recipientResolver.resolve(501L, ROLE_IDS, PERMISSION)).thenReturn(List.of(
                new DccRegistrationCertificateRecipient(22L, 501L)));
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.send(command("SUPPORTING_FILE_CONFIRMED")));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void platformFailureRemainsVisibleAsNotifyFailureCause() {
        when(recipientResolver.resolve(501L, ROLE_IDS, PERMISSION)).thenReturn(List.of(
                new DccRegistrationCertificateRecipient(22L, 501L)));
        IllegalStateException platformFailure = new IllegalStateException("template disabled");
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenThrow(platformFailure);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.send(command("CHANGE_APPROVAL_RECORDED")));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED.getCode(), exception.getCode());
        assertSame(platformFailure, exception.getCause());
    }

    private DccRegistrationCertificateBusinessEventNotificationCommand command(String eventType) {
        return new DccRegistrationCertificateBusinessEventNotificationCommand(
                1L, 501L, 1001L, 8001L, 99L, eventType, "EVENT-" + eventType, ROLE_IDS, PERMISSION,
                Map.of("certificateNo", "NMPA-001", "eventTitle", eventType));
    }

    private List<String> capturedBusinessKeysAndReset() {
        ArgumentCaptor<NotifySendSingleToUserIdempotentReqDTO> captor =
                ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(notifyMessageSendApi, org.mockito.Mockito.atLeastOnce())
                .sendSingleMessageIdempotentlyToAdmin(captor.capture());
        List<String> keys = new ArrayList<>();
        for (NotifySendSingleToUserIdempotentReqDTO request : captor.getAllValues()) {
            assertEquals(DccRegistrationCertificateBusinessEventNotificationService.TEMPLATE_CODE,
                    request.getTemplateCode());
            assertEquals(1001L, request.getTemplateParams().get("certificateId"));
            assertEquals(8001L, request.getTemplateParams().get("versionId"));
            assertEquals(501L, request.getTemplateParams().get("ownerCompanyId"));
            assertEquals(99L, request.getTemplateParams().get("actorId"));
            keys.add(request.getBusinessKey());
        }
        reset(notifyMessageSendApi);
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(invocation -> {
            NotifySendSingleToUserIdempotentReqDTO req = invocation.getArgument(0);
            return 900000L + req.getUserId();
        });
        return keys;
    }
}
