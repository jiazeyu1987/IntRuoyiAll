package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateChangeConfirmationServiceTest {
    private JdbcTemplate jdbc;
    private DccRegistrationCertificateChangeConfirmationService service;
    private DccRegistrationCertificateBusinessEventNotificationService notifications;
    private DccRegistrationCertificateBusinessClock clock;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:change-confirm-"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("""
            CREATE TABLE dcc_registration_certificate_change_confirmation (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,certificate_id BIGINT NOT NULL,
              version_id BIGINT NOT NULL,event_key VARCHAR(160) NOT NULL,recipient_user_id BIGINT NOT NULL,
              notify_message_id BIGINT NOT NULL,sent_at TIMESTAMP NOT NULL,confirmed_at TIMESTAMP NULL,
              UNIQUE(tenant_id,event_key,recipient_user_id),UNIQUE(tenant_id,notify_message_id))
            """);
        notifications=mock(DccRegistrationCertificateBusinessEventNotificationService.class);
        clock=mock(DccRegistrationCertificateBusinessClock.class);
        when(clock.now()).thenReturn(LocalDateTime.of(2026,10,30,10,0));
        service=new DccRegistrationCertificateChangeConfirmationService(jdbc,notifications,clock);
        when(notifications.sendToUsers(any(),anyList())).thenReturn(
                new DccRegistrationCertificateBusinessEventNotificationResult("CHANGE_APPROVAL_RECORDED",Map.of(21L,1001L,22L,1002L)));
    }
    @AfterEach void cleanup() {jdbc.execute("DROP ALL OBJECTS");TenantContextHolder.clear();}

    @Test
    void sendsRecordOnceAndOnlyTheActualRecipientCanConfirm() {
        service.send(command(),List.of(21L,22L,21L));
        service.send(command(),List.of(21L,22L));
        assertEquals(2,service.list(1L,100L).size());
        var first=service.getForRecipient(1L,21L,1001L);
        assertNull(first.confirmedAt());
        assertThrows(ServiceException.class,()->service.confirm(1L,99L,1001L));
        assertThrows(ServiceException.class,()->service.confirm(2L,21L,1001L));
        assertThrows(ServiceException.class,()->service.confirm(1L,21L,1002L));
        var acknowledged=service.confirm(1L,21L,1001L);
        assertNotNull(acknowledged.confirmedAt());
        when(clock.now()).thenReturn(first.sentAt().plusDays(1));
        assertEquals(acknowledged.confirmedAt(),service.confirm(1L,21L,1001L).confirmedAt());
        assertNull(service.getForRecipient(1L,22L,1002L).confirmedAt());
        assertTrue(service.list(2L,100L).isEmpty());
    }
    @Test
    void failedDeliveryAndForeignTenantNeverCreateFalseSendRecords() {
        when(notifications.sendToUsers(any(),anyList())).thenThrow(new IllegalStateException("发送失败"));
        assertThrows(IllegalStateException.class,()->service.send(command(),List.of(21L)));
        assertTrue(service.list(1L,100L).isEmpty());
        TenantContextHolder.setTenantId(2L);
        assertThrows(ServiceException.class,()->service.send(command(),List.of(21L)));
        verify(notifications,times(1)).sendToUsers(any(),anyList());
    }
    private static DccRegistrationCertificateBusinessEventNotificationCommand command() {
        return new DccRegistrationCertificateBusinessEventNotificationCommand(1L,10L,100L,200L,99L,
                "CHANGE_APPROVAL_RECORDED","approved-change-1",List.of(),null,Map.of());
    }
}
