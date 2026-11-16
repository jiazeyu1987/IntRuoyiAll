package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.mq.consumer.mail.MailSendConsumer;
import cn.iocoder.yudao.module.system.mq.consumer.sms.SmsSendConsumer;
import cn.iocoder.yudao.module.system.mq.message.mail.MailSendMessage;
import cn.iocoder.yudao.module.system.mq.message.sms.SmsSendMessage;
import cn.iocoder.yudao.module.system.mq.producer.mail.MailProducer;
import cn.iocoder.yudao.module.system.service.mail.MailSendService;
import cn.iocoder.yudao.module.system.service.sms.SmsSendService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.File;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantMessageDispatchTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test
    void mailProducerRequiresAndCapturesTenantBeforeDispatch() {
        var context = mock(ApplicationContext.class);
        var producer = new MailProducer();
        ReflectionTestUtils.setField(producer, "applicationContext", context);
        TenantContextHolder.setTenantId(11L);
        producer.sendMailSendMessage(1L, List.of("recipient@example.invalid"), List.of(), List.of(),
                2L, "Sender", "Title", "Body", new File[0]);
        verify(context).publishEvent((Object) argThat(event -> event instanceof MailSendMessage message
                && message.getTenantId().equals(11L)));
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> producer.sendMailSendMessage(1L, List.of(), List.of(), List.of(),
                2L, "Sender", "Title", "Body", new File[0]));
        verifyNoMoreInteractions(context);
    }

    @Test
    void mailConsumerUsesMessageTenantAndRestoresContextAfterFailure() {
        var consumer = new MailSendConsumer();
        var sender = mock(MailSendService.class);
        ReflectionTestUtils.setField(consumer, "mailSendService", sender);
        var message = new MailSendMessage().setTenantId(11L).setLogId(1L);
        doAnswer(call -> {
            assertEquals(11L, TenantContextHolder.getRequiredTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            throw new IllegalStateException("SEND_FAILED");
        }).when(sender).doSendMail(message);
        TenantContextHolder.setTenantId(22L);
        TenantContextHolder.setIgnore(true);
        assertThrows(IllegalStateException.class, () -> consumer.onMessage(message));
        assertEquals(22L, TenantContextHolder.getRequiredTenantId());
        assertTrue(TenantContextHolder.isIgnore());
        assertThrows(NullPointerException.class, () -> consumer.onMessage(new MailSendMessage()));
        verify(sender).doSendMail(message);
        verifyNoMoreInteractions(sender);
    }

    @Test
    void smsConsumerRequiresMessageTenantAndRestoresEmptyWorkerContext() {
        var consumer = new SmsSendConsumer();
        var sender = mock(SmsSendService.class);
        ReflectionTestUtils.setField(consumer, "smsSendService", sender);
        var message = new SmsSendMessage().setTenantId(33L).setLogId(3L);
        doAnswer(call -> {
            assertEquals(33L, TenantContextHolder.getRequiredTenantId());
            return null;
        }).when(sender).doSendSms(message);
        consumer.onMessage(message);
        assertNull(TenantContextHolder.getTenantId());
        assertThrows(NullPointerException.class, () -> consumer.onMessage(new SmsSendMessage()));
        verify(sender).doSendSms(message);
        verifyNoMoreInteractions(sender);
    }
}
