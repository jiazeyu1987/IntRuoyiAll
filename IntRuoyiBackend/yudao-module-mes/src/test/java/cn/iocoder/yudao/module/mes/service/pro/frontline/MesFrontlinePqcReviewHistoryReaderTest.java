package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesFrontlinePqcReviewHistoryReaderTest {
    private final MesProcessPoolSubmissionReviewMapper reviews = mock(MesProcessPoolSubmissionReviewMapper.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final MesSignedReturnCorrectionResolver corrections = mock(MesSignedReturnCorrectionResolver.class);
    private final MesFrontlinePqcReviewHistoryReader reader = new MesFrontlinePqcReviewHistoryReader(reviews, users, corrections);
    private MesProcessPoolActiveOrderDO order;
    private MesPqcInspectionTaskDO task;
    private MesProProcessPoolEventDO event;

    @BeforeEach
    void context() {
        order = new MesProcessPoolActiveOrderDO().setId(1L).setWorkOrderId(2L).setRouteId(3L).setRouteVersionId(4L);
        order.setTenantId(1L);
        task = new MesPqcInspectionTaskDO().setId(5L).setActiveOrderId(1L).setWorkOrderId(2L)
                .setRouteId(3L).setRouteVersionId(4L).setQaProcessId(6L).setSubmittedEventId(7L);
        task.setTenantId(1L);
        event = new MesProProcessPoolEventDO().setId(7L).setWorkOrderId(2L).setRouteId(3L).setQaProcessId(6L)
                .setEventType("PQC_INSPECTION").setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(5L);
        event.setTenantId(1L);
    }

    @Test
    void retainsRejectionAndApprovalWithRealIdentityAndRevision() {
        var rejected = review(11L, "REJECTED", 0, null);
        var approved = review(12L, "APPROVED", 1, 21L);
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of(approved, rejected));
        when(users.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setTenantId(1L).setNickname("实际复核人"));
        TenantUtils.execute(1L, () -> {
            var projection = reader.read(order, task, event);
            assertEquals("APPROVED", projection.currentReviewStatus());
            assertEquals(List.of(11L, 12L), projection.reviewHistory().stream().map(row -> row.getReviewId()).toList());
            assertEquals("REJECTED", projection.reviewHistory().get(0).getReviewStatus());
            assertEquals("意见11", projection.reviewHistory().get(0).getReviewRemark());
            assertEquals(8L, projection.reviewHistory().get(1).getReviewerUserId());
            assertEquals("实际复核人", projection.reviewHistory().get(1).getReviewerName());
            assertEquals(21L, projection.reviewHistory().get(1).getRevisionId());
        });
        verifyNoInteractions(corrections);
    }

    @Test
    void rejectionRemainsRejectedUntilFormalSignedCorrectionExistsThenBecomesPending() {
        var rejected = review(11L, "REJECTED", 0, null);
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of(rejected));
        TenantUtils.execute(1L, () -> assertEquals("REJECTED", reader.read(order, task, event).currentReviewStatus()));
        when(corrections.find(event, rejected)).thenReturn(new MesProProcessPoolEventRevisionDO().setId(21L));
        TenantUtils.execute(1L, () -> {
            var projection = reader.read(order, task, event);
            assertEquals("PENDING", projection.currentReviewStatus());
            assertEquals(11L, projection.reviewHistory().get(0).getReviewId());
        });
        var rejectedAgain = review(12L, "REJECTED", 1, 21L);
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of(rejected, rejectedAgain));
        TenantUtils.execute(1L, () -> assertEquals("REJECTED", reader.read(order, task, event).currentReviewStatus()));
    }

    @Test
    void noReviewIsPendingWithEmptyHistory() {
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of());
        TenantUtils.execute(1L, () -> {
            var result = reader.read(order, task, event);
            assertEquals("PENDING", result.currentReviewStatus());
            assertTrue(result.reviewHistory().isEmpty());
        });
        verifyNoInteractions(users, corrections);
    }

    @Test
    void confirmedTaskRequiresAnActualApprovedReview() {
        task.setTaskStatus("CONFIRMED");
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of());
        TenantUtils.execute(1L, () -> assertThrows(ServiceException.class, () -> reader.read(order, task, event)));
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L)))
                .thenReturn(List.of(review(11L, "REJECTED", 0, null)));
        TenantUtils.execute(1L, () -> assertThrows(ServiceException.class, () -> reader.read(order, task, event)));
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L)))
                .thenReturn(List.of(review(12L, "APPROVED", 1, 21L)));
        TenantUtils.execute(1L, () -> assertEquals("APPROVED", reader.read(order, task, event).currentReviewStatus()));
    }

    @Test
    void generatedSimulationReviewsCannotBeDisplayedAsActualHumanReviews() {
        var simulated = review(11L, "APPROVED", 0, null).setSimulated(true);
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of(simulated));
        TenantUtils.execute(1L, () -> assertThrows(ServiceException.class, () -> reader.read(order, task, event)));
        simulated.setSimulated(false).setSimulationStage("STAGE1");
        TenantUtils.execute(1L, () -> assertThrows(ServiceException.class, () -> reader.read(order, task, event)));
        simulated.setSimulationStage(null).setSimulationRunId("simulation-run");
        TenantUtils.execute(1L, () -> assertThrows(ServiceException.class, () -> reader.read(order, task, event)));
        verifyNoInteractions(users, corrections);
    }

    @Test
    void invalidSignedCorrectionEvidenceIsReportedInsteadOfInventingPendingStatus() {
        var rejected = review(11L, "REJECTED", 0, null);
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of(rejected));
        when(corrections.find(event, rejected)).thenThrow(new IllegalStateException("签名补正证据不一致"));
        TenantUtils.execute(1L, () -> assertEquals("签名补正证据不一致",
                assertThrows(IllegalStateException.class, () -> reader.read(order, task, event)).getMessage()));
    }

    @Test
    void rejectsForeignTenantOrderTaskAndEventBeforeReadingReviews() {
        TenantUtils.execute(1L, () -> {
            task.setTenantId(2L);
            assertThrows(ServiceException.class, () -> reader.read(order, task, event));
            task.setTenantId(1L);
            task.setActiveOrderId(99L);
            assertThrows(ServiceException.class, () -> reader.read(order, task, event));
            task.setActiveOrderId(1L);
            event.setFeedbackSourceId(99L);
            assertThrows(ServiceException.class, () -> reader.read(order, task, event));
            event.setFeedbackSourceId(5L);
            event.setWorkOrderId(99L);
            assertThrows(ServiceException.class, () -> reader.read(order, task, event));
        });
        verifyNoInteractions(reviews, users, corrections);
    }

    @Test
    void rejectsOtherEventsReviewsIncludingSameSubmissionGroup() {
        var foreign = review(11L, "REJECTED", 0, null).setEventId(99L);
        when(reviews.selectFormalReviewsByEventIds(1L, List.of(7L))).thenReturn(List.of(foreign));
        TenantUtils.execute(1L, () -> assertThrows(ServiceException.class, () -> reader.read(order, task, event)));
        foreign.setEventId(7L).setTenantId(2L);
        TenantUtils.execute(1L, () -> assertThrows(ServiceException.class, () -> reader.read(order, task, event)));
        verifyNoInteractions(users, corrections);
    }

    private MesProcessPoolSubmissionReviewDO review(Long id, String status, int round, Long revisionId) {
        var result = new MesProcessPoolSubmissionReviewDO().setId(id).setEventId(7L).setLeaderUserId(8L)
                .setLeaderType("PQC").setReviewStatus(status).setReviewRemark("意见" + id)
                .setReviewedAt(LocalDateTime.of(2026, 11, 13, 10, 0).plusMinutes(round))
                .setReviewRound(round).setSourceRevisionId(revisionId);
        result.setTenantId(1L);
        return result;
    }
}
