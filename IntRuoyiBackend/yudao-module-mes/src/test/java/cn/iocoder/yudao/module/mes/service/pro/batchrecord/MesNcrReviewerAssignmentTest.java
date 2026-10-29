package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesNcrReviewerAssignmentTest {
    MesProEdhrNonconformanceReviewServiceImpl service;
    MesProEdhrNonconformanceReviewMapper reviews;
    MesActiveOrderHandoffService handoff;
    GxpAuditService audit;
    MesProEdhrNonconformanceReviewDO review;

    @BeforeEach void init() {
        TenantContextHolder.setTenantId(1L);
        service = new MesProEdhrNonconformanceReviewServiceImpl();
        reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
        handoff = mock(MesActiveOrderHandoffService.class);
        ReflectionTestUtils.setField(service, "reviewMapper", reviews);
        ReflectionTestUtils.setField(service, "handoffService", handoff);
        audit=mock(GxpAuditService.class);
        ReflectionTestUtils.setField(service, "unifiedAudit", audit);
        review = new MesProEdhrNonconformanceReviewDO().setId(10L).setTenantId(1L)
                .setActiveOrderId(20L).setWorkOrderId(30L).setReviewStatus("pending_review");
        when(reviews.selectByIdForUpdate(10L)).thenReturn(review);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void historicalUnassignedReviewNeedsExplicitResponsibilityAndPersistsIt() {
        when(reviews.updateById(any(MesProEdhrNonconformanceReviewDO.class))).thenReturn(1);
        var result = service.assignReviewer(10L, 40L);
        assertEquals(40L, result.getQaReviewerUserId());
        verify(handoff).qaReviewCreated(review, 40L, null);
        verify(reviews).updateById(argThat((MesProEdhrNonconformanceReviewDO row) ->
                row.getId().equals(10L) && row.getQaReviewerUserId().equals(40L)));
        service.assignReviewer(10L, 40L);
        verify(reviews, times(1)).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        var receipt=org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit,times(1)).append(receipt.capture());
        assertEquals("mes.active-order-handoff.qa-reviewer-assigned",receipt.getValue().getOperationId());
        assertTrue(receipt.getValue().getBeforeState().getCanonicalJson().contains("UNASSIGNED"));
        assertTrue(receipt.getValue().getAfterState().getCanonicalJson().contains("40"));
    }

    @Test void conflictingAssignmentAndClosedReviewCannotDispatch() {
        review.setQaReviewerUserId(40L);
        assertThrows(IllegalArgumentException.class, () -> service.assignReviewer(10L, 41L));
        review.setReviewStatus("closed");
        assertThrows(RuntimeException.class, () -> service.assignReviewer(10L, 40L));
        verifyNoInteractions(handoff);
        verify(reviews, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
    }

    @Test void invalidQaDoesNotPersistAssignment() {
        doThrow(new IllegalStateException("QA responsibility permission missing"))
                .when(handoff).qaReviewCreated(review, 40L, null);
        assertThrows(IllegalStateException.class, () -> service.assignReviewer(10L, 40L));
        verify(reviews, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
    }
}
