package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMessageJobReplayReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileMessageReplayServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Mock
    private NotifyMessageSendApi notifyMessageSendApi;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileTrainingMapper trainingMapper;

    private DccControlledFileMessageDeliveryService messageDeliveryService;
    private java.util.Map<Long, DccControlledFileMessageJobDO> messageJobs;
    private DccControlledFileMessageReplayServiceImpl replayService;

    @org.junit.jupiter.api.AfterEach
    void clearMessageTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }

    @BeforeEach
    void setUp() {
        messageDeliveryService = new DccControlledFileMessageDeliveryService();
        messageJobs = DccMessageDeliveryTestSupport.wire(messageDeliveryService, messageJobMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "messageJobMapper", messageJobMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "notifyMessageSendApi", notifyMessageSendApi);
        ReflectionTestUtils.setField(messageDeliveryService, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "distributionMapper", distributionMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "trainingMapper", trainingMapper);
        replayService = new DccControlledFileMessageReplayServiceImpl();
        ReflectionTestUtils.setField(replayService, "messageJobMapper", messageJobMapper);
        ReflectionTestUtils.setField(replayService, "messageDeliveryService", messageDeliveryService);
    }

    @Test
    void replayMessageJobs_distributionPendingJob_sendsAndMarksSent() {
        DccControlledFileMessageJobDO job = DccControlledFileMessageJobDO.builder()
                .id(1001L)
                .businessType(DccControlledFileFinalizationServiceImpl.MESSAGE_BUSINESS_TYPE_DISTRIBUTION)
                .businessId(2001L)
                .templateCode(DccControlledFileFinalizationServiceImpl.MESSAGE_TEMPLATE_DISTRIBUTION)
                .recipientUserId(501L)
                .status(DccControlledFileMessageJobStatusEnum.PENDING.getCode())
                .build();
        messageJobs.put(job.getId(), job);
        when(messageJobMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(job));
        when(distributionMapper.selectById(2001L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(2001L)
                .controlledFileId(3001L)
                .build());
        when(controlledFileMapper.selectById(3001L)).thenReturn(DccControlledFileDO.builder()
                .id(3001L)
                .title("说明书")
                .fileName("说明书")
                .versionNo("A.2")
                .effectiveDate(LocalDate.of(2026, 5, 20))
                .build());
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class))).thenReturn(9001L);

        int replayedCount = replayService.replayMessageJobs(req(List.of(1001L)));

        assertEquals(1, replayedCount);
        ArgumentCaptor<NotifySendSingleToUserIdempotentReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageIdempotentlyToAdmin(notifyCaptor.capture());
        assertEquals(501L, notifyCaptor.getValue().getUserId());
        assertEquals(DccControlledFileFinalizationServiceImpl.MESSAGE_TEMPLATE_DISTRIBUTION,
                notifyCaptor.getValue().getTemplateCode());
        assertEquals("说明书", notifyCaptor.getValue().getTemplateParams().get("title"));
        verify(messageJobMapper).updateById(any(DccControlledFileMessageJobDO.class));
    }

    @Test
    void replayMessageJobs_trainingFailedJob_sendsAndMarksSent() {
        DccControlledFileMessageJobDO job = DccControlledFileMessageJobDO.builder()
                .id(1002L)
                .businessType(DccControlledFileFinalizationServiceImpl.MESSAGE_BUSINESS_TYPE_TRAINING)
                .businessId(2002L)
                .templateCode(DccControlledFileFinalizationServiceImpl.MESSAGE_TEMPLATE_TRAINING)
                .recipientUserId(601L)
                .status(DccControlledFileMessageJobStatusEnum.FAILED.getCode())
                .build();
        messageJobs.put(job.getId(), job);
        when(messageJobMapper.selectBatchIds(List.of(1002L))).thenReturn(List.of(job));
        when(trainingMapper.selectById(2002L)).thenReturn(DccControlledFileTrainingDO.builder()
                .id(2002L)
                .controlledFileId(3002L)
                .build());
        when(controlledFileMapper.selectById(3002L)).thenReturn(DccControlledFileDO.builder()
                .id(3002L)
                .title("工艺文件")
                .fileName("工艺文件")
                .versionNo("1.0")
                .effectiveDate(LocalDate.of(2026, 5, 21))
                .build());
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class))).thenReturn(9002L);

        int replayedCount = replayService.replayMessageJobs(req(List.of(1002L)));

        assertEquals(1, replayedCount);
        verify(notifyMessageSendApi).sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class));
        verify(messageJobMapper).updateById(any(DccControlledFileMessageJobDO.class));
    }

    @Test
    void replayMessageJobs_jobMissing_throwsExplicitFailure() {
        when(messageJobMapper.selectBatchIds(List.of(1003L))).thenReturn(List.of());

        assertServiceException(() -> replayService.replayMessageJobs(req(List.of(1003L))),
                CONTROLLED_FILE_MESSAGE_JOB_NOT_EXISTS);
    }

    @Test
    void replayMessageJobs_sentJob_throwsExplicitFailure() {
        when(messageJobMapper.selectBatchIds(List.of(1004L))).thenReturn(List.of(
                DccControlledFileMessageJobDO.builder()
                        .id(1004L)
                        .status(DccControlledFileMessageJobStatusEnum.SENT.getCode())
                        .build()));

        assertServiceException(() -> replayService.replayMessageJobs(req(List.of(1004L))),
                CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED);
    }

    @Test
    void replayMessageJobs_downstreamFailure_marksFailedAndRethrows() {
        DccControlledFileMessageJobDO job = DccControlledFileMessageJobDO.builder()
                .id(1005L)
                .businessType("OBSOLETE")
                .businessId(3005L)
                .templateCode("dcc_obsolete")
                .recipientUserId(701L)
                .status(DccControlledFileMessageJobStatusEnum.PENDING.getCode())
                .build();
        when(messageJobMapper.selectBatchIds(List.of(1005L))).thenReturn(List.of(job));
        when(controlledFileMapper.selectById(3005L)).thenReturn(DccControlledFileDO.builder()
                .id(3005L)
                .title("旧图纸")
                .fileName("旧图纸")
                .versionNo("0.9")
                .obsoleteReason("版本升级")
                .build());
        messageJobs.put(job.getId(), job);
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class)))
                .thenThrow(new ServiceException(1234, "notify failed"));

        ServiceException ex = org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
                () -> replayService.replayMessageJobs(req(List.of(1005L))));

        assertEquals(1234, ex.getCode());
        ArgumentCaptor<DccControlledFileMessageJobDO> updateCaptor =
                ArgumentCaptor.forClass(DccControlledFileMessageJobDO.class);
        verify(messageJobMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileMessageJobStatusEnum.FAILED.getCode(), updateCaptor.getValue().getStatus());
        assertTrue(updateCaptor.getValue().getErrorMessage().contains("notify failed"));
    }

    @Test
    void dispatchMessageJob_afterCommitFailureRecordsFailedWithoutPropagatingToPublisher() {
        DccControlledFileMessageJobDO job = DccControlledFileMessageJobDO.builder()
                .id(1006L)
                .businessType("DISTRIBUTION")
                .businessId(2006L)
                .templateCode("dcc_distribution")
                .recipientUserId(702L)
                .status(DccControlledFileMessageJobStatusEnum.PENDING.getCode())
                .build();
        messageJobs.put(job.getId(), job);
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class)))
                .thenThrow(new ServiceException(1234, "notify failed after commit"));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            messageDeliveryService.dispatchMessageJob(job, java.util.Map.of("title", "发布文件"));
            TransactionSynchronization callback = TransactionSynchronizationManager.getSynchronizations().get(0);

            assertDoesNotThrow(callback::afterCommit);

            ArgumentCaptor<DccControlledFileMessageJobDO> updateCaptor =
                    ArgumentCaptor.forClass(DccControlledFileMessageJobDO.class);
            verify(messageJobMapper).updateById(updateCaptor.capture());
            assertEquals(DccControlledFileMessageJobStatusEnum.FAILED.getCode(), updateCaptor.getValue().getStatus());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private static DccControlledFileMessageJobReplayReqVO req(List<Long> jobIds) {
        DccControlledFileMessageJobReplayReqVO reqVO = new DccControlledFileMessageJobReplayReqVO();
        reqVO.setJobIds(jobIds);
        return reqVO;
    }
}
