package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateReasonDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateReasonMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class DccPublicationNotificationDispatchOrchestratorTest extends BaseMockitoUnitTest {

    @Mock private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Mock private DccPublicationNotificationCandidateReasonMapper reasonMapper;
    @Mock private DccPublicationFollowupBatchMapper batchMapper;
    @Mock private NotifyMessageSendApi notifyMessageSendApi;
    @Mock private DccPublicationNotificationTransactionService transactionService;
    @InjectMocks private DccPublicationNotificationDispatchOrchestrator orchestrator;

    @Test
    void dispatchBatch_oneRecipientFailureDoesNotPreventNextRecipient() {
        DccPublicationNotificationDeliveryDO first = delivery(1L, 11L, 0);
        DccPublicationNotificationDeliveryDO second = delivery(2L, 12L, 0);
        when(deliveryMapper.selectDispatchableByBatchId(1L, 7L)).thenReturn(List.of(first, second));
        when(transactionService.beginAttempt(1L, 1L, 0, null, "发布后自动发送", false))
                .thenReturn(delivery(1L, 11L, 1));
        when(transactionService.beginAttempt(1L, 2L, 0, null, "发布后自动发送", false))
                .thenReturn(delivery(2L, 12L, 1));
        stubMessageContext();
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any()))
                .thenThrow(new IllegalStateException("secret-token=do-not-store"))
                .thenReturn(9002L);

        orchestrator.dispatchBatch(1L, 7L);

        verify(transactionService).markFailed(1L, 1L, 1, null, "发布后自动发送",
                "IllegalStateException: publication notification delivery failed");
        verify(transactionService).markSent(1L, 2L, 1, 9002L, null, "发布后自动发送");
    }

    @Test
    void retryAfterAckFailureReusesExactPlatformBusinessKeyAndMessageId() {
        DccPublicationNotificationDeliveryDO firstAttempt = delivery(1L, 11L, 1);
        DccPublicationNotificationDeliveryDO failed = delivery(1L, 11L, 2);
        failed.setStatus("FAILED");
        DccPublicationNotificationDeliveryDO secondAttempt = delivery(1L, 11L, 3);
        when(transactionService.beginAttempt(1L, 1L, 0, 9L, "人工重试", true)).thenReturn(firstAttempt);
        when(transactionService.beginAttempt(1L, 1L, 2, 9L, "人工重试", true)).thenReturn(secondAttempt);
        when(deliveryMapper.selectById(1L)).thenReturn(delivery(1L, 11L, 0), failed);
        stubMessageContext();
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(9001L);
        doThrow(new IllegalStateException("ack transaction failed"))
                .when(transactionService).markSent(1L, 1L, 1, 9001L, 9L, "人工重试");

        assertThrows(IllegalStateException.class,
                () -> orchestrator.retryDelivery(1L, 9L, 1L, 0, "人工重试"));
        orchestrator.retryDelivery(1L, 9L, 1L, 2, "人工重试");

        ArgumentCaptor<NotifySendSingleToUserIdempotentReqDTO> captor =
                ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(notifyMessageSendApi, times(2)).sendSingleMessageIdempotentlyToAdmin(captor.capture());
        assertEquals("DCC_PUBLICATION:7:USER:11", captor.getAllValues().get(0).getBusinessKey());
        assertEquals(captor.getAllValues().get(0).getBusinessKey(),
                captor.getAllValues().get(1).getBusinessKey());
        verify(transactionService).markFailed(1L, 1L, 1, 9L, "人工重试",
                "IllegalStateException: publication notification delivery failed");
        verify(transactionService).markSent(1L, 1L, 3, 9001L, 9L, "人工重试");
    }

    @Test
    void dispatchBatch_nullRowsAndMissingReasonsFailExplicitlyWithoutSending() {
        when(deliveryMapper.selectDispatchableByBatchId(1L, 7L)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> orchestrator.dispatchBatch(1L, 7L));

        DccPublicationNotificationDeliveryDO attempt = delivery(1L, 11L, 1);
        when(deliveryMapper.selectDispatchableByBatchId(1L, 8L)).thenReturn(List.of(delivery(1L, 11L, 0)));
        when(transactionService.beginAttempt(1L, 1L, 0, null, "发布后自动发送", false)).thenReturn(attempt);
        DccPublicationFollowupBatchDO batch = DccPublicationFollowupBatchDO.builder()
                .id(7L).publishedControlledFileId(70L).fileNumberSnapshot("DOC-70")
                .fileNameSnapshot("作业指导书").versionNoSnapshot("B/1").build();
        batch.setTenantId(1L);
        when(batchMapper.selectById(7L)).thenReturn(batch);
        when(reasonMapper.selectListByCandidateId(1L, 11L)).thenReturn(null);

        orchestrator.dispatchBatch(1L, 8L);

        verify(notifyMessageSendApi, never()).sendSingleMessageIdempotentlyToAdmin(any());
        verify(transactionService).markFailed(1L, 1L, 1, null, "发布后自动发送",
                "IllegalStateException: publication notification context invalid");
    }

    private void stubMessageContext() {
        DccPublicationFollowupBatchDO batch = DccPublicationFollowupBatchDO.builder()
                .id(7L).publishedControlledFileId(70L).fileNumberSnapshot("DOC-70")
                .fileNameSnapshot("作业指导书").versionNoSnapshot("B/1").build();
        batch.setTenantId(1L);
        when(batchMapper.selectById(7L)).thenReturn(batch);
        when(reasonMapper.selectListByCandidateId(any(), any())).thenReturn(List.of(
                DccPublicationNotificationCandidateReasonDO.builder()
                        .reasonType("FILE_OWNER").sourceId(70L).reasonSummary("发布文件责任人").build(),
                DccPublicationNotificationCandidateReasonDO.builder()
                        .reasonType("FORMAL_DISTRIBUTION").sourceId(71L).reasonSummary("正式分发对象").build()));
    }

    private DccPublicationNotificationDeliveryDO delivery(Long id, Long userId, int version) {
        DccPublicationNotificationDeliveryDO row = DccPublicationNotificationDeliveryDO.builder()
                .id(id).batchId(7L).candidateId(userId).userId(userId)
                .businessKey("DCC_PUBLICATION:7:USER:" + userId)
                .templateCode("dcc_publication_released").status("PENDING")
                .attemptCount(version).rowVersion(version).build();
        row.setTenantId(1L);
        return row;
    }
}
