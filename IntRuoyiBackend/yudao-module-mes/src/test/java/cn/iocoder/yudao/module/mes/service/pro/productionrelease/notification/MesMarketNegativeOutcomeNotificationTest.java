package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MesMarketNegativeOutcomeNotificationTest {
    MesReleaseOutcomeNotificationService service;
    MesReleaseTaskNotifyDeliveryMapper deliveries;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        service = new MesReleaseOutcomeNotificationService();
        deliveries = mock(MesReleaseTaskNotifyDeliveryMapper.class);
        ReflectionTestUtils.setField(service, "deliveryMapper", deliveries);
        ReflectionTestUtils.setField(service, "audit", mock(MesReleaseTaskNotificationAudit.class));
        when(deliveries.selectTaskDeliveries(1L, 30L)).thenReturn(List.of());
        when(deliveries.insert(any(MesReleaseTaskNotifyDeliveryDO.class))).thenAnswer(invocation -> {
            invocation.<MesReleaseTaskNotifyDeliveryDO>getArgument(0).setId(100L); return 1;
        });
    }
    @AfterEach void clear() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TenantContextHolder.clear();
    }
    @Test void rejectionNotifiesApplicantAndLeavesQaAssignmentToFormalNcr() {
        service.scheduleMarketNegative(application(), transaction("REJECTED"), task(), 7L, true, "证据不足");
        var capture = org.mockito.ArgumentCaptor.forClass(MesReleaseTaskNotifyDeliveryDO.class);
        verify(deliveries).insert(capture.capture());
        assertEquals(5L, capture.getValue().getUserId());
        assertEquals("MARKET_REJECTED", capture.getValue().getEventType());
    }
    @Test void withdrawalNotifiesApplicantAndEveryOriginalFrozenCandidateOnce() {
        service.scheduleMarketNegative(application(), transaction("WITHDRAWN"), task(), 5L, false, "申请补正");
        var capture = org.mockito.ArgumentCaptor.forClass(MesReleaseTaskNotifyDeliveryDO.class);
        verify(deliveries, times(3)).insert(capture.capture());
        assertEquals(List.of(5L, 7L, 8L), capture.getAllValues().stream().map(MesReleaseTaskNotifyDeliveryDO::getUserId).toList());
    }
    @Test void withdrawalUsesAssigneeInsteadOfBroadCandidateGroup() {
        service.scheduleMarketNegative(application(), transaction("WITHDRAWN"), task().setAssigneeUserId(8L), 5L, false, "申请补正");
        var capture = org.mockito.ArgumentCaptor.forClass(MesReleaseTaskNotifyDeliveryDO.class);
        verify(deliveries, times(2)).insert(capture.capture());
        assertEquals(List.of(5L, 8L), capture.getAllValues().stream().map(MesReleaseTaskNotifyDeliveryDO::getUserId).toList());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"MARKET_REJECTED", "MARKET_WITHDRAWN"})
    void applicantOutsideOriginalCandidatesReceivesResultThroughRealDispatch(String event) {
        var row = new MesReleaseTaskNotifyDeliveryDO().setId(100L).setTenantId(1L).setWorkTaskId(30L)
                .setUserId(5L).setInitiatedBy(7L).setEventType(event).setEventRevision(0L)
                .setStatus("PENDING").setRowVersion(0).setBusinessKey("result-" + event)
                .setTemplateCode("MES_EDHR_RELEASE_" + event).setTemplateParamsJson("{\"reason\":\"正式结果\"}");
        when(deliveries.selectTaskDeliveries(1L, 30L)).thenReturn(List.of(row));
        var transaction = mock(MesReleaseTaskNotificationTransactionService.class);
        when(transaction.recordAttempt(eq(1L), eq(100L), eq(0), eq(7L), anyString(), eq(false))).thenReturn(row);
        var sender = mock(MesReleaseTaskNotificationPlatformSender.class);
        when(sender.send(any())).thenReturn(900L);
        var dispatcher = new MesReleaseTaskNotificationDispatchService();
        ReflectionTestUtils.setField(dispatcher, "deliveryMapper", deliveries);
        ReflectionTestUtils.setField(dispatcher, "transactionService", transaction);
        ReflectionTestUtils.setField(dispatcher, "platformSender", sender);
        var tasks = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper.class);
        ReflectionTestUtils.setField(dispatcher, "workTaskMapper", tasks);
        dispatcher.dispatchAssigned(1L, 30L);
        verify(sender).send(argThat(req -> Long.valueOf(5L).equals(req.getUserId())));
        verify(transaction).recordSent(eq(1L), eq(100L), eq(0), eq(900L), eq(7L), anyString(), eq(false));
        verifyNoInteractions(tasks);
    }
    private MesProcessPoolActiveOrderReleaseApplicationDO application() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO().setId(10L).setAppliedBy(5L)
                .setReleaseTransactionId(20L).setReleaseApprovalWorkTaskId(30L);
    }
    private MesProEdhrReleaseTransactionDO transaction(String status) {
        return new MesProEdhrReleaseTransactionDO().setId(20L).setReleaseStatus(status);
    }
    private MesProEdhrWorkTaskDO task() {
        return new MesProEdhrWorkTaskDO().setId(30L).setBusinessScopeId(20L).setBusinessScopeType("RELEASE_TRANSACTION")
                .setTaskType("RELEASE_APPROVE").setBatchExecutionId(40L).setWorkOrderId(50L)
                .setWorkOrderCode("WO-50").setBatchCode("B-40").setProcessName("上市放行")
                .setOwnershipLocked(true).setCandidateUserSnapshot("7,8")
                .setActionUrl("/mes/pro/feedback/edhr-batch-execution?batchExecutionId=40&releaseTransactionId=20&workTaskId=30&action=marketRelease");
    }
}
