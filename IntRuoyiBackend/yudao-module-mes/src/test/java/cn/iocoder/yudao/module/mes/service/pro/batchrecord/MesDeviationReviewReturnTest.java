package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationReviewReturnReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesDeviationReviewReturnTest {
    private final MesProEdhrDeviationMapper deviations = mock(MesProEdhrDeviationMapper.class);
    private final MesProEdhrDeviationHandlingMapper handlings = mock(MesProEdhrDeviationHandlingMapper.class);
    private final MesProEdhrDeviationReviewTaskService reviews = mock(MesProEdhrDeviationReviewTaskService.class);
    private final MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class);
    private final SecurityFrameworkService permissions = mock(SecurityFrameworkService.class);
    private final MesProEdhrOperationAuditService audit = mock(MesProEdhrOperationAuditService.class);
    private final MesProEdhrDeviationHandlingServiceImpl service = new MesProEdhrDeviationHandlingServiceImpl(
            deviations, handlings, mock(ElectronicSignatureRecordMapper.class),
            mock(MesProBatchRecordExecutionSignatureService.class), audit);
    private MesProEdhrDeviationDO deviation;
    private MesProEdhrDeviationHandlingDO current;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(71L);
        ReflectionTestUtils.setField(service, "securityFrameworkService", permissions);
        ReflectionTestUtils.setField(service, "reviewTaskService", reviews);
        ReflectionTestUtils.setField(service, "workTaskService", tasks);
        ReflectionTestUtils.setField(service, "gxpAuditService", mock(GxpAuditService.class));
        ReflectionTestUtils.setField(service, "accessService", mock(MesProEdhrDeviationAccessService.class));
        when(permissions.hasPermission(anyString())).thenReturn(true);
        deviation = new MesProEdhrDeviationDO().setId(901L).setTenantId(71L).setStatus("OPEN")
                .setLevel("NORMAL").setReceiverId(101L).setDeviationCode("PC-901");
        current = new MesProEdhrDeviationHandlingDO().setId(701L).setTenantId(71L).setDeviationId(901L)
                .setContentVersion(3).setContentHash("hash3").setInvestigationSubmitted(true)
                .setRootCauseAnalysis("根因").setImpactScope("影响范围").setRiskAssessment("风险")
                .setProductDisposition("产品处置").setHandlingConclusion("CLOSED_LOOP")
                .setVerificationResult("PASS").setVerificationContent("独立验证证据")
                .setCorrectiveStatus("VERIFIED").setCorrectiveOwnerId(301L).setCorrectiveResult("完成整改")
                .setCompletedAt(LocalDateTime.of(2026, 11, 17, 10, 0))
                .setReviewerAssignmentsJson("{\"PREPARER\":101,\"VERIFIER\":201,\"DEPARTMENT_OWNER\":401,\"QA\":501,\"QUALITY_OWNER\":601}");
        when(deviations.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(deviation);
        when(handlings.selectByTenantAndDeviationIdForUpdate(71L, 901L)).thenReturn(current);
    }

    @AfterEach void tearDown() { TenantContextHolder.clear(); }

    @Test void reviewerReturnReopensInvestigationPreservesCompletedCorrectionAndInvalidatesOldReview() {
        when(handlings.updateContent(eq(71L), eq(901L), eq(3), any())).thenReturn(1);
        var response = service.returnToInvestigation(501L, request("QA"));
        var changed = ArgumentCaptor.forClass(MesProEdhrDeviationHandlingDO.class);
        verify(handlings).updateContent(eq(71L), eq(901L), eq(3), changed.capture());
        assertEquals(4, response.getContentVersion());
        assertFalse(response.getInvestigationSubmitted());
        assertFalse(response.getReviewReady());
        assertEquals("RETURNED_TO_INVESTIGATION", changed.getValue().getHandlingConclusion());
        assertEquals("完成整改", changed.getValue().getCorrectiveResult());
        assertEquals(current.getCompletedAt(), changed.getValue().getCompletedAt());
        assertNotEquals("hash3", response.getContentHash());
        verify(reviews).require(901L, current, "QA", 501L);
        verify(reviews).cancelAll(eq(901L), anyString());
        verify(reviews, never()).assign(any(), any(), anyString(), anyLong());
        verify(tasks).assignDeviationTask(eq(deviation), eq("DEVIATION_RECEIVE"), eq(101L), contains("证据不足"));
        var command = ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(audit).recordInCallerTransaction(command.capture());
        assertTrue(command.getValue().getMetadataJson().contains("审核退回调查[QA]"));
    }

    @Test void designatedQaCanReturnFromOutstandingCloseTask() {
        when(handlings.updateContent(eq(71L), eq(901L), eq(3), any())).thenReturn(1);
        service.returnToInvestigation(501L, request("CLOSE"));
        verify(reviews).require(901L, current, "CLOSE", 501L);
    }

    @Test void otherUserCannotReturnSomebodyElsesReview() {
        assertThrows(ServiceException.class, () -> service.returnToInvestigation(601L, request("QA")));
        verifyNoWrites();
    }

    @Test void completedOrCancelledTaskCannotReturnTheInvestigation() {
        when(reviews.require(901L, current, "QA", 501L)).thenThrow(new IllegalStateException("任务已失效"));
        assertThrows(IllegalStateException.class, () -> service.returnToInvestigation(501L, request("QA")));
        verifyNoWrites();
    }

    @Test void staleVersionOrDigestCannotReturnNewerContent() {
        var req = request("QA"); req.setExpectedContentVersion(2);
        assertThrows(ServiceException.class, () -> service.returnToInvestigation(501L, req));
        req.setExpectedContentVersion(3); req.setExpectedContentHash("stale");
        assertThrows(ServiceException.class, () -> service.returnToInvestigation(501L, req));
        verifyNoWrites();
    }

    @Test void emptyReasonClosedDeviationOrUnsubmittedContentCannotBeReturned() {
        var req = request("QA"); req.setReason("  ");
        assertThrows(ServiceException.class, () -> service.returnToInvestigation(501L, req));
        deviation.setStatus("CLOSED");
        assertThrows(ServiceException.class, () -> service.returnToInvestigation(501L, request("QA")));
        deviation.setStatus("OPEN"); current.setInvestigationSubmitted(false);
        assertThrows(ServiceException.class, () -> service.returnToInvestigation(501L, request("QA")));
        verifyNoWrites();
    }

    private void verifyNoWrites() {
        verify(handlings, never()).updateContent(anyLong(), anyLong(), anyInt(), any());
        verifyNoInteractions(tasks, audit);
    }

    private MesDeviationReviewReturnReqVO request(String node) {
        var req = new MesDeviationReviewReturnReqVO();
        req.setDeviationId(901L); req.setExpectedContentVersion(3); req.setExpectedContentHash("hash3");
        req.setNode(node); req.setReason("证据不足");
        return req;
    }
}
