package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccMessageDeliveryIdempotencyTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileMessageJobMapper messageJobMapper;
    @Mock private NotifyMessageSendApi notifyMessageSendApi;
    @InjectMocks private DccControlledFileMessageDeliveryService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() { DccMessageDeliveryTestSupport.wire(service, messageJobMapper).put(901L, job()); }

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void retryAfterMessageSentButAckLostCreatesOnlyOnePlatformMessage() {
        TenantContextHolder.setTenantId(1L);
        DccControlledFileMessageJobDO job = job();
        Map<String, Long> platformMessages = new LinkedHashMap<>();
        lenient().when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenAnswer(ignored -> platformMessages.computeIfAbsent("ordinary-" + platformMessages.size(), key -> (long) platformMessages.size() + 1));
        lenient().when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class)))
                .thenAnswer(invocation -> {
                    NotifySendSingleToUserIdempotentReqDTO req = invocation.getArgument(0);
                    assertNotNull(req.getBusinessKey());
                    return platformMessages.computeIfAbsent(req.getBusinessKey(), key -> (long) platformMessages.size() + 1);
                });
        when(messageJobMapper.updateById(any(DccControlledFileMessageJobDO.class)))
                .thenThrow(new IllegalStateException("DCC ACK write failed")).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> service.dispatchMessageJob(job, Map.of("title", "文件")));
        service.dispatchMessageJob(job, Map.of("title", "文件"));

        assertEquals(1, platformMessages.size(), "重试必须复用同一通知业务身份");
    }

    @Test
    void missingPlatformMessageIdMustNotBecomeSent() {
        TenantContextHolder.setTenantId(1L);
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(null);
        when(messageJobMapper.updateById(any(DccControlledFileMessageJobDO.class))).thenReturn(1);
        assertThrows(IllegalStateException.class, () -> service.dispatchMessageJob(job(), Map.of("title", "文件")));
        verify(messageJobMapper, never()).updateById(org.mockito.ArgumentMatchers.<DccControlledFileMessageJobDO>argThat(row -> "SENT".equals(row.getStatus())));
    }

    private DccControlledFileMessageJobDO job() {
        DccControlledFileMessageJobDO row = DccControlledFileMessageJobDO.builder().id(901L)
                .businessType("TRAINING").businessId(300L).recipientUserId(101L)
                .templateCode("dcc_training").status("PENDING").build();
        return row;
    }
}
