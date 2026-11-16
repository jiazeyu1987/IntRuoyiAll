package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProductionQuantityDraftTest {
    private final MesProductionQuantityDraftMapper drafts = mock(MesProductionQuantityDraftMapper.class);
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesTeamLeaderScopeService scopes = mock(MesTeamLeaderScopeService.class);
    private final MesProductionQuantityCapacityService capacity = mock(MesProductionQuantityCapacityService.class);
    private final MesProBatchRecordExecutionSignatureService signatures = mock(MesProBatchRecordExecutionSignatureService.class);
    private final GxpAuditService audit = mock(GxpAuditService.class);
    private final MesProEdhrNonconformanceReviewService freeze = mock(MesProEdhrNonconformanceReviewService.class);
    private final MesProWorkOrderMapper workOrders = mock(MesProWorkOrderMapper.class);
    private final MesProductionQuantityDraftService service = new MesProductionQuantityDraftService(
            drafts,orders,scopes,capacity,signatures,audit,freeze,workOrders);

    private MesProductionQuantityDraftDO row() {
        var row = new MesProductionQuantityDraftDO();
        row.setId(1L); row.setOperatorUserId(7L);row.setEmployeeId(7L);row.setIdentityDomain("SYSTEM_USER");
        row.setLeaderUserId(8L);row.setActiveOrderId(2L);row.setWorkOrderId(3L);row.setProcessId(4L);row.setRouteProcessId(5L);
        row.setStatus("BLOCKED");row.setVersion(0);row.setRequestHash("hash");row.setRequestJson("{}");
        when(drafts.lock(1L)).thenReturn(row);
        when(orders.selectByIdForUpdate(2L)).thenReturn(new MesProcessPoolActiveOrderDO().setId(2L)
                .setWorkOrderId(3L).setLeaderUserId(8L).setActiveStatus("ACTIVE").setBusinessStatus("ACTIVE"));
        when(drafts.updateById(any(MesProductionQuantityDraftDO.class))).thenReturn(1);
        return row;
    }
    private MesProductionQuantityDraftService.Review command(String decision) {
        var c = new MesProductionQuantityDraftService.Review();c.setId(1L);c.setExpectedVersion(0);
        c.setDecision(decision);c.setReason("核对真实提交，纠正归属");c.setSignaturePassword("test-password");return c;
    }
    private void signature() {
        when(signatures.recordFieldChangeSignature(any())).thenReturn(new MesProBatchRecordExecutionFieldAuditSignatureResult()
                .setActorId(8L).setSignatureId(99L).setSignedAt(LocalDateTime.now()));
    }
    @Test void currentErrorNeedsResponsibleLeadersSignedDecisionBeforeWorkerCanRetry() {
        var row = row();signature();
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(8L);
            service.review(command("CURRENT_ERROR"));
        }
        assertEquals("READY",row.getStatus());assertEquals(99L,row.getReviewSignatureId());assertEquals(1,row.getVersion());
        verifyNoInteractions(capacity);verify(scopes).assertCanAccessEmployee(8L,"PRODUCTION",7L);
        service.submitted(row,100L,"new-request-hash");
        assertEquals("SUBMITTED",row.getStatus());assertEquals(100L,row.getSubmittedEventId());
    }
    @Test void historicalErrorCannotBeApprovedUntilActualReallocationFreesCapacity() {
        row();when(capacity.evaluate(any())).thenReturn(new MesProductionQuantityCapacityService.Capacity(null,
                new BigDecimal("600"),new BigDecimal("600"),new BigDecimal("1200"),new BigDecimal("1100")));
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(8L);
            var error = assertThrows(ServiceException.class,()->service.review(command("HISTORY_ERROR")));
            assertEquals(400, error.getCode());
            assertTrue(error.getMessage().contains("请先完成历史归属更正"));
        }
        verifyNoInteractions(signatures);verify(drafts,never()).updateById(any(MesProductionQuantityDraftDO.class));
    }
    @Test void historicalErrorCanBeSignedAfterConservedPartialReallocation() {
        var row=row();signature();when(capacity.evaluate(any())).thenReturn(new MesProductionQuantityCapacityService.Capacity(null,
                new BigDecimal("400"),new BigDecimal("600"),new BigDecimal("1000"),new BigDecimal("1100")));
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(8L);service.review(command("HISTORY_ERROR"));
        }
        assertEquals("READY",row.getStatus());assertEquals("HISTORY_ERROR",row.getDecision());
    }
    @Test void unrelatedAccountAndStaleVersionCannotApprove() {
        row();try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(77L);
            assertThrows(ServiceException.class,()->service.review(command("CURRENT_ERROR")));
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(8L);
            var c=command("CURRENT_ERROR");c.setExpectedVersion(9);
            assertThrows(ServiceException.class,()->service.review(c));
        }
        verifyNoInteractions(signatures);verify(drafts,never()).updateById(any(MesProductionQuantityDraftDO.class));
    }
    @Test void signatureFailurePreservesBlockedStatusAndProducesNoFormalLink() {
        var row=row();when(signatures.recordFieldChangeSignature(any())).thenThrow(new IllegalArgumentException("bad credential"));
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(8L);
            assertThrows(IllegalArgumentException.class,()->service.review(command("CURRENT_ERROR")));
        }
        assertEquals("BLOCKED",row.getStatus());assertNull(row.getSubmittedEventId());
        verify(drafts,never()).updateById(any(MesProductionQuantityDraftDO.class));
    }
    @Test void draftNeverStoresCredentialsAndReturnsExplicitNonFormalOutcome() {
        var req=new MesProFrontlineFeedbackSubmitReqVO().setActualEmployeeId(7L).setSignatureEmployeeId(7L)
                .setSignatureIdentityDomain("SYSTEM_USER").setSignaturePassword("never-store")
                .setProcessPoolSubmissionIdempotencyKey("first-attempt")
                .setProcessPoolContext(new MesProFrontlineProcessPoolContextReqVO().setRouteProcessId(5L).setProcessId(4L))
                .setRawPayload(Map.of("nested",Map.of("password","also-secret","note","real filling")));
        when(drafts.insert(any(MesProductionQuantityDraftDO.class))).thenAnswer(i->{i.<MesProductionQuantityDraftDO>getArgument(0).setId(1L);return 1;});
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
            var row=service.capture(req,"original-hash",new MesProductionQuantityCapacityService.Capacity(
                    new MesProcessPoolActiveOrderDO().setId(2L).setWorkOrderId(3L).setLeaderUserId(8L),
                    BigDecimal.TEN,BigDecimal.TEN,new BigDecimal("20"),new BigDecimal("11")),null);
            assertFalse(row.getRequestJson().contains("secret"));assertFalse(row.getRequestJson().contains("never-store"));
            assertTrue(row.getRequestJson().contains("real filling"));
            var result=service.blocked(row,"too large");assertEquals("QUANTITY_BLOCKED",result.getSubmissionStatus());
            assertNull(result.getFeedbackId());assertNull(result.getProcessPoolEventId());assertNull(row.getSubmittedEventId());
        }
    }
}
