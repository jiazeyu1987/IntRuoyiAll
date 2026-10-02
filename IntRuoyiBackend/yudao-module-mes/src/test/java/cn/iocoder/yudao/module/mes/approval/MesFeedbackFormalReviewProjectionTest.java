package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesFeedbackFormalReviewProjectionTest {
    @Mock MesProProcessPoolEventMapper eventMapper;
    @Mock MesProcessPoolSubmissionReviewMapper reviewMapper;
    @InjectMocks MesFeedbackFormalReviewProjection projection;

    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void batchMatchesFormalEvidenceAndLeavesOriginalFeedbackApproving() {
        MesProFeedbackDO feedback = feedback(5660L);
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L)));
        when(reviewMapper.selectFormalReviewsByEventIds(1L, List.of(282240L)))
                .thenReturn(List.of(review(282240L, "APPROVED")));
        var fact = projection.read(List.of(feedback)).get(5660L);
        assertEquals(12743L, fact.review().getReviewSignatureId());
        assertEquals(341L, fact.reviewerId());
        assertEquals(MesProFeedbackStatusEnum.APPROVING.getStatus(), feedback.getStatus());
        verify(eventMapper, times(1)).selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L));
        verify(reviewMapper, times(1)).selectFormalReviewsByEventIds(1L, List.of(282240L));
        verifyNoMoreInteractions(eventMapper, reviewMapper);
    }

    @Test void noFormalEventIsLegacyAndDoesNotRequireReview() {
        assertEquals(Map.of(), projection.read(List.of(feedback(5660L))));
        verify(reviewMapper).selectFormalReviewsByEventIds(1L, List.of());
    }

    @Test void manyFeedbacksUseOneEventBatchAndOneReviewBatchIncludingUnlinkedLegacy() {
        var pending = event(282241L, 5661L);
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L, 5661L, 5662L)))
                .thenReturn(List.of(event(282240L, 5660L), pending));
        when(reviewMapper.selectFormalReviewsByEventIds(1L, List.of(282240L, 282241L)))
                .thenReturn(List.of(review(282240L, "APPROVED")));
        var facts = projection.read(List.of(feedback(5660L), feedback(5661L), feedback(5662L)));
        assertTrue(facts.get(5660L).reviewed()); assertFalse(facts.get(5661L).reviewed());
        assertFalse(facts.containsKey(5662L));
        verify(eventMapper).selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L, 5661L, 5662L));
        verify(reviewMapper).selectFormalReviewsByEventIds(1L, List.of(282240L, 282241L));
        verifyNoMoreInteractions(eventMapper, reviewMapper);
    }

    @Test void pendingFormalEventRetainsProductionSignature() {
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L)));
        var fact = projection.read(List.of(feedback(5660L))).get(5660L);
        assertFalse(fact.reviewed());
        assertEquals(12742L, fact.event().getSignatureId());
    }

    @Test void actualProductionWriterSignatureForeignKeysDoNotRequireUnusedInlineSnapshot() {
        // The real production event writer persists signatureId/userId; it does not supply signatureSnapshot.
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L).setSignatureSnapshot(null)));
        when(reviewMapper.selectFormalReviewsByEventIds(1L, List.of(282240L)))
                .thenReturn(List.of(review(282240L, "APPROVED")));
        var fact = projection.read(List.of(feedback(5660L))).get(5660L);
        assertTrue(fact.reviewed());
        assertEquals(12742L, fact.event().getSignatureId());
        assertEquals(342L, fact.event().getSignatureUserId());
        assertNull(fact.event().getSignatureSnapshot());
        assertEquals(341L, fact.reviewerId());
    }

    @Test void duplicateEventsFailInsteadOfChoosingFirst() {
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L), event(282241L, 5660L)));
        assertInvalid(() -> projection.read(List.of(feedback(5660L))));
        verifyNoInteractions(reviewMapper);
    }

    @Test void duplicateReviewsFailInsteadOfChoosingLatest() {
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L)));
        when(reviewMapper.selectFormalReviewsByEventIds(1L, List.of(282240L)))
                .thenReturn(List.of(review(282240L, "APPROVED"), review(282240L, "APPROVED").setId(281241L)));
        assertInvalid(() -> projection.read(List.of(feedback(5660L))));
    }

    @Test void crossTenantEventAndReviewFailClosed() {
        MesProProcessPoolEventDO event = event(282240L, 5660L); event.setTenantId(2L);
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L))).thenReturn(List.of(event));
        assertInvalid(() -> projection.read(List.of(feedback(5660L))));
        event.setTenantId(1L);
        MesProcessPoolSubmissionReviewDO review = review(282240L, "APPROVED"); review.setTenantId(2L);
        when(reviewMapper.selectFormalReviewsByEventIds(1L, List.of(282240L))).thenReturn(List.of(review));
        assertInvalid(() -> projection.read(List.of(feedback(5660L))));
    }

    @Test void missingOrMismatchedProductionIdentityFails() {
        for (Consumer<MesProProcessPoolEventDO> change : List.<Consumer<MesProProcessPoolEventDO>>of(
                event -> event.setSignatureId(null), event -> event.setSignatureUserId(999L),
                event -> event.setWorkOrderId(999L), event -> event.setFeedbackSourceType("OTHER"))) {
            MesProProcessPoolEventDO event = event(282240L, 5660L); change.accept(event);
            when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L))).thenReturn(List.of(event));
            assertInvalid(() -> projection.read(List.of(feedback(5660L))));
        }
        verifyNoInteractions(reviewMapper);
    }

    @Test void missingSignatureAndEverySnapshotIdentityMismatchFailWithoutDone() {
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L)));
        for (Consumer<MesProcessPoolSubmissionReviewDO> change : List.<Consumer<MesProcessPoolSubmissionReviewDO>>of(
                review -> review.setReviewSignatureId(null), review -> review.setReviewSignatureUserId(999L),
                review -> review.setReviewedAt(null), review -> review.setLeaderType("PQC"),
                review -> review.setReviewStatus("PENDING"), review -> review.setReviewSignatureSnapshotJson("{"),
                review -> review.setReviewSignatureSnapshotJson(snapshot(282241L, "APPROVED")),
                review -> review.setReviewSignatureSnapshotJson(snapshot(282240L, "REJECTED")),
                review -> review.setReviewSignatureSnapshotJson(snapshot(282240L, "APPROVED").replace("12743", "12744")),
                review -> review.setReviewSignatureSnapshotJson(snapshot(282240L, "APPROVED").replace("341", "999")))) {
            MesProcessPoolSubmissionReviewDO review = review(282240L, "APPROVED"); change.accept(review);
            when(reviewMapper.selectFormalReviewsByEventIds(1L, List.of(282240L))).thenReturn(List.of(review));
            assertInvalid(() -> projection.read(List.of(feedback(5660L))));
        }
    }

    @Test void rejectedReviewUsesItsFormalSignedDecision() {
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, java.util.Set.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L)));
        when(reviewMapper.selectFormalReviewsByEventIds(1L, List.of(282240L)))
                .thenReturn(List.of(review(282240L, "REJECTED")));
        assertEquals("REJECTED", projection.read(List.of(feedback(5660L))).get(5660L).review().getReviewStatus());
    }

    @Test void actualReviewerDiscoveryIsTenantBoundAndDoesNotTrustOrphanReviews() {
        when(reviewMapper.selectProductionReviewsByReviewer(1L, 341L)).thenReturn(List.of(review(282240L, "APPROVED")));
        assertInvalid(() -> projection.reviewedFeedbackIds(341L));
        when(eventMapper.selectFormalProductionByEventIds(1L, List.of(282240L)))
                .thenReturn(List.of(event(282240L, 5660L)));
        assertEquals(List.of(5660L), projection.reviewedFeedbackIds(341L));
    }

    @Test void legacyGuardBlocksEvenBrokenFormalEvidenceBeforeAnyInventoryAction() {
        when(eventMapper.selectFormalProductionByFeedbackIds(1L, List.of(5660L)))
                .thenReturn(List.of(event(282240L, 5660L).setSignatureId(null)));
        ServiceException error = assertThrows(ServiceException.class, () -> projection.assertLegacyOperationAllowed(5660L));
        assertEquals(MesFeedbackApprovalErrorCodeConstants.FORMAL_FEEDBACK_LEGACY_REVIEW_FORBIDDEN.getCode(), error.getCode());
        verifyNoInteractions(reviewMapper);
    }

    @Test void legacyGuardAllowsUnlinkedFeedbackAndRequiresTenant() {
        projection.assertLegacyOperationAllowed(5660L);
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> projection.assertLegacyOperationAllowed(5660L));
    }

    static MesProFeedbackDO feedback(Long id) {
        MesProFeedbackDO value = MesProFeedbackDO.builder().id(id).code("FB-" + id).workOrderId(990274L)
                .routeId(980091L).processId(922985L).feedbackUserId(342L).approveUserId(340L)
                .status(MesProFeedbackStatusEnum.APPROVING.getStatus()).build();
        value.setCreateTime(LocalDateTime.of(2026, 10, 1, 10, 43));
        return value;
    }
    static MesProProcessPoolEventDO event(Long eventId, Long feedbackId) {
        MesProProcessPoolEventDO value = MesProProcessPoolEventDO.builder().id(eventId).eventType("PRODUCTION_SUBMIT")
                .feedbackSourceType("MES_PRO_FEEDBACK").feedbackSourceId(feedbackId).workOrderId(990274L)
                .routeId(980091L).processId(922985L).actualEmployeeId(342L).signatureId(12742L)
                .signatureUserId(342L).signatureSnapshot("{\"signatureId\":12742}")
                .serverSubmitTime(LocalDateTime.of(2026, 10, 1, 10, 43)).build();
        value.setTenantId(1L); return value;
    }
    static MesProcessPoolSubmissionReviewDO review(Long eventId, String status) {
        MesProcessPoolSubmissionReviewDO value = MesProcessPoolSubmissionReviewDO.builder().id(281240L).eventId(eventId)
                .leaderUserId(341L).leaderType("PRODUCTION").reviewStatus(status).reviewRemark("旧冻结事件正式复核")
                .reviewedAt(LocalDateTime.of(2026, 10, 1, 20, 35)).reviewSignatureId(12743L)
                .reviewSignatureUserId(341L).reviewSignatureSnapshotJson(snapshot(eventId, status)).build();
        value.setTenantId(1L); return value;
    }
    private static String snapshot(Long eventId, String status) {
        return "{\"signatureId\":12743,\"actorId\":341,\"processPoolEventId\":" + eventId
                + ",\"actionType\":\"TEAM_LEADER_REVIEW\",\"eventType\":\"PRODUCTION_SUBMIT\","
                + "\"leaderType\":\"PRODUCTION\",\"reviewStatus\":\"" + status + "\"}";
    }
    private static void assertInvalid(org.junit.jupiter.api.function.Executable action) {
        ServiceException error = assertThrows(ServiceException.class, action);
        assertEquals(MesFeedbackApprovalErrorCodeConstants.FORMAL_FEEDBACK_EVIDENCE_INVALID.getCode(), error.getCode());
    }
}
