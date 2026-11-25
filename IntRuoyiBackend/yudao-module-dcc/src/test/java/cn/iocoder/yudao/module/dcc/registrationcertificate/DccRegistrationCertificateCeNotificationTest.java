package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipientResolver;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.access.DccRegistrationCertificateBusinessFileAccessProvider.QUERY_CURRENT_PERMISSION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 使用真实人员筛选SQL、发送服务和确认记录，权限/站内信平台作为外部边界。 */
class DccRegistrationCertificateCeNotificationTest {
    private JdbcTemplate jdbc;
    private PermissionApi permissions;
    private NotifyMessageSendApi messages;
    private DccRegistrationCertificateBusinessEventNotificationConfigService config;
    private DccRegistrationCertificateViewerRecipientResolver viewers;
    private DccRegistrationCertificateChangeConfirmationService confirmations;
    private DccRegistrationCertificateBusinessEventNotifier notifier;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:ce-notify-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE system_users (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
                                           status INT NOT NULL, deleted INT NOT NULL)
                """);
        jdbc.execute("""
                INSERT INTO system_users VALUES
                  (11,1,0,0), (12,1,0,0), (13,1,1,0), (14,1,0,1), (15,2,0,0), (21,1,0,0), (99,1,0,0)
                """);
        jdbc.execute("""
                CREATE TABLE dcc_registration_certificate_change_confirmation (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, certificate_id BIGINT NOT NULL,
                  version_id BIGINT NOT NULL, event_key VARCHAR(160) NOT NULL, recipient_user_id BIGINT NOT NULL,
                  notify_message_id BIGINT NOT NULL, sent_at TIMESTAMP NOT NULL, confirmed_at TIMESTAMP NULL,
                  UNIQUE(tenant_id,event_key,recipient_user_id), UNIQUE(tenant_id,notify_message_id))
                """);
        permissions = mock(PermissionApi.class);
        // 13、14、15 即使有权限也不能越过账号状态、删除和租户边界。
        when(permissions.hasAnyPermissions(anyLong(), eq(QUERY_CURRENT_PERMISSION)))
                .thenAnswer(invocation -> List.of(11L, 13L, 14L, 15L, 21L).contains(invocation.<Long>getArgument(0)));
        messages = mock(NotifyMessageSendApi.class);
        when(messages.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(invocation ->
                1000L + invocation.<NotifySendSingleToUserIdempotentReqDTO>getArgument(0).getUserId());
        config = mock(DccRegistrationCertificateBusinessEventNotificationConfigService.class);
        var notifications = new DccRegistrationCertificateBusinessEventNotificationService(
                mock(DccRegistrationCertificateRecipientResolver.class), messages);
        var clock = mock(DccRegistrationCertificateBusinessClock.class);
        when(clock.now()).thenReturn(LocalDateTime.of(2026, 11, 24, 10, 0));
        confirmations = new DccRegistrationCertificateChangeConfirmationService(jdbc, notifications, clock);
        viewers = new DccRegistrationCertificateViewerRecipientResolver(jdbc, permissions);
        notifier = new DccRegistrationCertificateBusinessEventNotifier(config, notifications, confirmations, viewers);
    }

    @AfterEach
    void cleanup() {
        jdbc.execute("DROP ALL OBJECTS");
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEW", "RENEWAL_UPLOAD", "ACTIVATION", "CHANGE"})
    void effectiveCeEventsReachEveryEnabledViewerAndNoOneElse(String event) {
        // 旧名单中只有无查看权的12；真正有权的11、21都不在名单中。
        lenient().when(config.resolveRecipientUserIds(1L)).thenReturn(List.of(12L));
        lenient().when(config.resolveChangeConfirmationRecipientUserIds(1L)).thenReturn(List.of(12L));
        notify(event);

        var captor = ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(messages, times(2)).sendSingleMessageIdempotentlyToAdmin(captor.capture());
        assertEquals(List.of(11L, 21L), captor.getAllValues().stream().map(NotifySendSingleToUserIdempotentReqDTO::getUserId).toList());
        assertTrue(captor.getAllValues().stream().allMatch(req -> "CE产品".equals(req.getTemplateParams().get("productName"))));
        verifyNoInteractions(config);
        for (Long excluded : List.of(13L, 14L, 15L)) {
            verify(permissions, never()).hasAnyPermissions(excluded, QUERY_CURRENT_PERMISSION);
        }
    }

    @Test
    void ceChangeCreatesOnlyViewerConfirmationsAndReplayDoesNotDuplicateThem() {
        notifier.validateChangeConfirmationRecipients(1L, "CE");
        notify("CHANGE");
        notify("CHANGE");
        assertEquals(List.of(11L, 21L), confirmations.list(1L, 100L).stream()
                .map(DccRegistrationCertificateChangeConfirmationService.Confirmation::recipientUserId).sorted().toList());
        assertNotNull(confirmations.confirm(1L, 11L, 1011L).confirmedAt());
        assertThrows(ServiceException.class, () -> confirmations.confirm(1L, 12L, 1011L));
        assertThrows(ServiceException.class, () -> confirmations.confirm(1L, 99L, 1011L));
        verifyNoInteractions(config);
    }

    @Test
    void permissionChangesAreReflectedOnTheNextEvent() {
        assertEquals(List.of(11L, 21L), viewers.resolve(1L));
        when(permissions.hasAnyPermissions(11L, QUERY_CURRENT_PERMISSION)).thenReturn(false);
        when(permissions.hasAnyPermissions(12L, QUERY_CURRENT_PERMISSION)).thenReturn(true);
        notify("ACTIVATION");
        var captor = ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(messages, times(2)).sendSingleMessageIdempotentlyToAdmin(captor.capture());
        assertEquals(List.of(12L, 21L), captor.getAllValues().stream().map(NotifySendSingleToUserIdempotentReqDTO::getUserId).toList());
    }

    @Test
    void noAuthorizedViewerDoesNotSendToTheActorOrCreateConfirmationRows() {
        when(permissions.hasAnyPermissions(anyLong(), eq(QUERY_CURRENT_PERMISSION))).thenReturn(false);
        notify("NEW");
        notify("ACTIVATION");
        notify("CHANGE");
        verifyNoInteractions(messages, config);
        assertTrue(confirmations.list(1L, 100L).isEmpty());
    }

    @Test
    void foreignTenantAndPermissionFailureStopBeforeSending() {
        assertThrows(ServiceException.class, () -> viewers.resolve(2L));
        assertThrows(ServiceException.class, () -> viewers.resolve(null));
        var failure = new IllegalStateException("权限服务故障");
        when(permissions.hasAnyPermissions(11L, QUERY_CURRENT_PERMISSION)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> notify("CHANGE")));
        verifyNoInteractions(messages, config);
        assertTrue(confirmations.list(1L, 100L).isEmpty());
    }

    @Test
    void deliveryFailureIsNotReportedAsAConfirmedSend() {
        doThrow(new IllegalStateException("发送失败")).when(messages).sendSingleMessageIdempotentlyToAdmin(any());
        assertThrows(ServiceException.class, () -> notify("CHANGE"));
        assertTrue(confirmations.list(1L, 100L).isEmpty());
    }

    @Test
    void futureNewCeAndDomesticCandidateKeepTheirUploadNotificationScope() {
        when(config.resolveRecipientUserIds(1L)).thenReturn(List.of(12L));
        notifier.notifyNewCertificateFormalized(1L, 10L, 100L, 200L, 99L, "future-new", "CE产品", "CE-001",
                LocalDate.of(2026, 12, 1), LocalDate.of(2031, 12, 1), "CE", false);
        notifier.notifyRenewalCandidateUploaded(1L, 10L, 100L, 201L, 99L, "upload", "国内产品", "REG-001", null, null, "DOMESTIC");
        verify(config, times(2)).resolveRecipientUserIds(1L);
        verifyNoInteractions(permissions);
    }

    private void notify(String event) {
        switch (event) {
            case "NEW" -> notifier.notifyNewCertificateFormalized(1L, 10L, 100L, 200L, 99L, "ce-new",
                    "CE产品", "CE-001", null, LocalDate.of(2031, 12, 1), "CE", true);
            case "ACTIVATION" -> notifier.notifyRenewalCandidateActivated(1L, 10L, 100L, 200L, 99L, "ce-activation",
                    "CE产品", "CE-001", null, LocalDate.of(2031, 12, 1), "CE");
            case "RENEWAL_UPLOAD" -> notifier.notifyRenewalCandidateUploaded(1L, 10L, 100L, 200L, 99L, "ce-renewal-upload",
                    "CE产品", "CE-001", null, LocalDate.of(2031, 12, 1), "CE");
            case "CHANGE" -> notifier.notifyChangeApprovalRecorded(1L, 10L, 100L, 200L, 99L, "ce-change",
                    "CE产品", "CE-001", null, LocalDate.of(2031, 12, 1), "CE");
            default -> throw new IllegalArgumentException(event);
        }
    }
}
