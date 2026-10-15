package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.notify.*;
import cn.iocoder.yudao.module.system.api.notify.dto.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccDistributionMessageFactsTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMessageJobMapper messageJobMapper;
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccControlledFileDistributionMapper distributionMapper;
    @Mock DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock DccControlledFileTrainingMapper trainingMapper;
    @Mock NotifyMessageSendApi notifyMessageSendApi;
    @Mock cn.iocoder.yudao.module.system.service.notify.NotifyMessageService notifyMessageService;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks DccControlledFileMessageDeliveryService service;
    DccControlledFileMessageJobDO job;
    DccControlledFileDO file;
    final AtomicInteger commits=new AtomicInteger();
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        job=DccControlledFileMessageJobDO.builder().id(81L).businessType("DISTRIBUTION").businessId(51L)
                .recipientUserId(99L).templateCode("dcc_distribution").status("FAILED").build();
        var now=LocalDateTime.of(2026,10,11,9,0);
        file=DccControlledFileDO.builder().id(42L).versionNo("B/1").processInstanceId("round-42")
                .status("ACTIVE").controlledTime(now).activatedTime(now).distributedTime(now).build();file.setTenantId(1L);
        lenient().when(messageJobMapper.selectByIdAndTenantForUpdate(1L,81L)).thenAnswer(ignored->job);
        lenient().when(messageJobMapper.updateById(any(DccControlledFileMessageJobDO.class))).thenReturn(1);
        lenient().when(distributionMapper.selectById(51L)).thenReturn(DccControlledFileDistributionDO.builder().id(51L)
                .controlledFileId(42L).distributionMedium("PUBLIC_FOLDER").status("PENDING").build());
        lenient().when(controlledFileMapper.selectById(42L)).thenAnswer(ignored->file);
        lenient().when(distributionRecipientMapper.selectListByDistributionId(51L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder().id(71L).distributionId(51L).userId(99L).messageJobId(81L).build()));
        lenient().when(transactionManager.getTransaction(any())).thenAnswer(ignored->new SimpleTransactionStatus());
        lenient().doAnswer(ignored->{ commits.incrementAndGet();return null; }).when(transactionManager).commit(any());
        lenient().when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(901L);
        var platformMessage=cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO.builder().id(901L)
                .tenantId(1L).userId(99L).userType(2).templateCode("dcc_distribution").businessKey("DCC_MESSAGE_JOB:1:81").build();
        platformMessage.setCreateTime(now.minusHours(1));
        lenient().when(notifyMessageService.getNotifyMessage(901L)).thenReturn(platformMessage);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void canonicalNotificationCarriesExactVersionAndRecipientIds() {
        service.replayMessageJob(job);
        var capture=ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageIdempotentlyToAdmin(capture.capture());
        var params=capture.getValue().getTemplateParams();
        assertEquals("42",params.get("controlledFileId"));assertEquals("51",params.get("distributionId"));
        assertEquals("71",params.get("recipientId"));assertEquals("B/1",params.get("version"));
        assertEquals("round-42",params.get("approvalProcessInstanceId"));
    }
    @Test void pendingEffectiveFailsAndPersistsFailureBeforeAnyPlatformSend() {
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");file.setActivatedTime(null);
        assertThrows(RuntimeException.class,()->service.replayMessageJob(job));
        verifyNoInteractions(notifyMessageSendApi);
        verify(messageJobMapper).updateById(argThat((DccControlledFileMessageJobDO row)->"FAILED".equals(row.getStatus())));
    }
    @Test void attemptReadCommitsBeforePlatformSendAndResultCommitsSeparately() {
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(ignored->{
            assertTrue(commits.get()>=1,"attempt/current identity transaction must commit before platform call");return 901L;
        });
        service.replayMessageJob(job);
        assertTrue(commits.get()>=2);
    }
    @Test void staleCallerStatusCannotResendPersistedSentJob() {
        var stale=DccControlledFileMessageJobDO.builder().id(81L).status("FAILED").build();job.setStatus("SENT");
        service.replayMessageJob(stale);verifyNoInteractions(notifyMessageSendApi);
    }
    @Test void failureStoredWithoutPasswordOrTokenMaterial() {
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenThrow(new IllegalStateException("password=secret token=abc"));
        assertThrows(RuntimeException.class,()->service.replayMessageJob(job));
        var capture=ArgumentCaptor.forClass(DccControlledFileMessageJobDO.class);
        verify(messageJobMapper,times(2)).updateById(capture.capture());
        var failed=capture.getAllValues().stream().filter(row->"FAILED".equals(row.getStatus())).findFirst().orElseThrow();
        assertFalse(failed.getErrorMessage().contains("secret"));
        assertFalse(failed.getErrorMessage().contains("abc"));
    }
    @Test void historicalExactVersionIsNotResolvedToCurrentMasterVersion() {
        file.setStatus("OBSOLETE");service.replayMessageJob(job);
        verify(controlledFileMapper).selectById(42L);
        verify(controlledFileMapper,never()).updateById(any(DccControlledFileDO.class));
    }
    @Test void newDispatchWithoutBusinessTransactionCannotSend() {
        assertThrows(IllegalStateException.class,()->service.dispatchMessageJob(job,Map.of()));
        verifyNoInteractions(notifyMessageSendApi,messageJobMapper);
    }
    @Test void retryAfterSentButAcknowledgementLostUsesOnePlatformIdentity() {
        Map<String,Long> messages=new LinkedHashMap<>();
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(call->{
            var request=call.getArgument(0,NotifySendSingleToUserIdempotentReqDTO.class);
            return messages.computeIfAbsent(request.getBusinessKey(),key->901L);
        });
        when(messageJobMapper.updateById(any(DccControlledFileMessageJobDO.class))).thenAnswer(call->{
            var row=call.getArgument(0,DccControlledFileMessageJobDO.class);
            if("SENT".equals(row.getStatus()) && messages.size()==1 && commits.get()<3)
                throw new IllegalStateException("domain acknowledgement lost");
            return 1;
        });
        assertThrows(IllegalStateException.class,()->service.replayMessageJob(job));
        service.replayMessageJob(job);assertEquals(1,messages.size());
    }
    @Test void actualSentTimeComesFromOriginalPlatformMessageAndSurvivesReplay() {
        service.replayMessageJob(job);
        var capture=ArgumentCaptor.forClass(DccControlledFileMessageJobDO.class);
        verify(messageJobMapper,times(2)).updateById(capture.capture());
        var sent=capture.getAllValues().stream().filter(row->"SENT".equals(row.getStatus())).findFirst().orElseThrow();
        assertEquals(LocalDateTime.of(2026,10,11,8,0),sent.getSentAt());
    }
}
