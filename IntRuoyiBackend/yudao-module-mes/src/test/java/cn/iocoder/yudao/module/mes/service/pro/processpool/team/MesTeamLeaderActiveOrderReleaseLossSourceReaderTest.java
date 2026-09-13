package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderReleaseLossSourceReaderTest {

    private static final Long ACTIVE_ORDER_ID = 8101L;
    private static final Long WORK_ORDER_ID = 9001L;
    private static final Long ROUTE_ID = 7001L;
    private static final Long ROUTE_VERSION_ID = 7002L;
    private static final Long ROUTE_PROCESS_ID = 5001L;
    private static final Long PROCESS_ID = 6001L;

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;

    private MesTeamLeaderActiveOrderReleaseLossSourceReader reader;

    @BeforeEach
    void setUp() {
        reader = new MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl(
                eventMapper, feedbackMapper, allocationMapper, reviewMapper);
    }

    @Test
    void shouldReadOnlyCurrentActiveOrderAllocatedSignedFeedbackAndStructuredLossDetails() {
        MesProProcessPoolEventDO event = event(
                "{\"lossQuantity\":999,\"lossDetails\":[{\"reasonId\":8301,"
                        + "\"reasonCode\":\"LOSS-001\",\"reasonName\":\"正常损耗\","
                        + "\"quantity\":2.500}]}"
        );
        when(eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of(event));
        when(allocationMapper.selectListByEventIdForUpdate(event.getId())).thenReturn(List.of(allocation()));
        when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L))).thenReturn(List.of(feedback()));
        when(reviewMapper.selectListByEventIdForUpdate(event.getId())).thenReturn(List.of(review()));

        MesTeamLeaderActiveOrderReleaseLossSourceReadResult result = reader.read(command());

        assertAll(
                () -> assertTrue(result.getBlockers().isEmpty()),
                () -> assertEquals(1, result.getProcessSources().size()),
                () -> assertEquals(5101L, result.getProcessSources().get(0).getFeedback().getId()),
                () -> assertEquals(7101L, result.getProcessSources().get(0).getAllocation().getId()),
                () -> assertEquals(7201L, result.getProcessSources().get(0).getReview().getId()),
                () -> assertEquals(List.of(new BigDecimal("2.500")), result.getProcessSources().get(0)
                        .getLossDetails().stream().map(
                                MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail::getQuantity).toList()),
                () -> assertEquals("LOSS-001", result.getProcessSources().get(0).getLossDetails().get(0)
                        .getReasonCode()));
    }

    @Test
    void shouldRejectLegacyRawReasonAliasInsteadOfTreatingItAsFormalStructuredLossDetails() {
        MesProProcessPoolEventDO event = event(
                "{\"lossReasonDetails\":[{\"reasonId\":8301,\"reasonCode\":\"LOSS-001\","
                        + "\"reasonName\":\"正常损耗\",\"quantity\":2.500}]}"
        );
        when(eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of(event));
        when(allocationMapper.selectListByEventIdForUpdate(event.getId())).thenReturn(List.of(allocation()));
        when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L))).thenReturn(List.of(feedback()));
        when(reviewMapper.selectListByEventIdForUpdate(event.getId())).thenReturn(List.of(review()));

        MesTeamLeaderActiveOrderReleaseLossSourceReadResult result = reader.read(command());

        assertTrue(result.getBlockers().stream().anyMatch(blocker ->
                "LOSS_SOURCE_REQUIRED".equals(blocker.getBlockerType())
                        && ROUTE_PROCESS_ID.equals(blocker.getRouteProcessId())
                        && "lossDetails".equals(blocker.getFieldCode())));
        assertTrue(result.getProcessSources().isEmpty());
    }

    @Test
    void shouldAcceptMultipleFormalProductionSubmitsForTheSameSnapshot() {
        MesProProcessPoolEventDO firstEvent = event(1001L, 5101L,
                "{\"lossQuantity\":999,\"lossDetails\":[{\"reasonId\":8301,"
                        + "\"reasonCode\":\"LOSS-001\",\"reasonName\":\"正常损耗\","
                        + "\"quantity\":2.500}]}"
        );
        MesProProcessPoolEventDO secondEvent = event(1002L, 5102L,
                "{\"lossQuantity\":999,\"lossDetails\":[{\"reasonId\":8301,"
                        + "\"reasonCode\":\"LOSS-001\",\"reasonName\":\"正常损耗\","
                        + "\"quantity\":1.500}]}"
        );
        when(eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of(firstEvent, secondEvent));
        when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L, 5102L)))
                .thenReturn(List.of(feedback(5101L, new BigDecimal("2.500")),
                        feedback(5102L, new BigDecimal("1.500"))));
        when(allocationMapper.selectListByEventIdForUpdate(1001L))
                .thenReturn(List.of(allocation(1001L, 7101L, 7201L)));
        when(allocationMapper.selectListByEventIdForUpdate(1002L))
                .thenReturn(List.of(allocation(1002L, 7102L, 7202L)));
        when(reviewMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(review(1001L, 7201L)));
        when(reviewMapper.selectListByEventIdForUpdate(1002L)).thenReturn(List.of(review(1002L, 7202L)));

        MesTeamLeaderActiveOrderReleaseLossSourceReadResult result = reader.read(command());

        assertAll(
                () -> assertTrue(result.getBlockers().isEmpty()),
                () -> assertEquals(2, result.getProcessSources().size()),
                () -> assertEquals(List.of(1001L, 1002L), result.getProcessSources().stream()
                        .map(source -> source.getEvent().getId()).toList()),
                () -> assertEquals(List.of(5101L, 5102L), result.getProcessSources().stream()
                        .map(source -> source.getFeedback().getId()).toList()),
                () -> assertEquals(List.of(new BigDecimal("2.500"), new BigDecimal("1.500")),
                        result.getProcessSources().stream()
                                .map(source -> source.getLossDetails().get(0).getQuantity()).toList()));
    }

    private static MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command() {
        return new MesTeamLeaderActiveOrderReleaseLossReportPlanCommand()
                .setTenantId(1L)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setWorkOrderId(WORK_ORDER_ID)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setProductId(3101L)
                .setBatchCode("BATCH-9001")
                .setSourceSnapshotHash("AO_RELEASE_SOURCE_V1:loss-source")
                .setProcessSnapshots(List.of(MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .id(4101L)
                        .activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID)
                        .routeId(ROUTE_ID)
                        .routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .build()));
    }

    private static MesProProcessPoolEventDO event(String payload) {
        return event(1001L, 5101L, payload);
    }

    private static MesProProcessPoolEventDO event(Long id, String payload) {
        return event(id, 5101L, payload);
    }

    private static MesProProcessPoolEventDO event(Long id, Long feedbackSourceId, String payload) {
        return MesProProcessPoolEventDO.builder()
                .id(id)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2101L)
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(feedbackSourceId)
                .rawPayload(payload)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .signatureId(1101L)
                .signatureUserId(2101L)
                .signatureSnapshot("{\"signedAt\":\"2026-08-01T08:30:00\"}")
                .build();
    }

    private static MesProFeedbackDO feedback() {
        return feedback(5101L, new BigDecimal("2.500"));
    }

    private static MesProFeedbackDO feedback(Long id, BigDecimal unqualifiedQuantity) {
        return MesProFeedbackDO.builder()
                .id(id)
                .code("FB-" + id)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .processId(PROCESS_ID)
                .feedbackTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .feedbackQuantity(new BigDecimal("100.000"))
                .qualifiedQuantity(new BigDecimal("100.000").subtract(unqualifiedQuantity))
                .unqualifiedQuantity(unqualifiedQuantity)
                .laborScrapQuantity(new BigDecimal("1.000"))
                .materialScrapQuantity(new BigDecimal("1.500"))
                .otherScrapQuantity(BigDecimal.ZERO)
                .lossReasonId(8301L)
                .lossReasonCodeSnapshot("LOSS-001")
                .lossReasonNameSnapshot("正常损耗")
                .feedbackUserId(2101L)
                .approveUserId(3001L)
                .status(MesProFeedbackStatusEnum.APPROVING.getStatus())
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return allocation(1001L, 7101L, 7201L);
    }

    private static MesProcessPoolReportAllocationDO allocation(Long eventId, Long allocationId, Long reviewId) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(allocationId)
                .eventId(eventId)
                .reviewId(reviewId)
                .leaderUserId(3001L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("100.000"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO review() {
        return review(1001L, 7201L);
    }

    private static MesProcessPoolSubmissionReviewDO review(Long eventId, Long reviewId) {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(reviewId)
                .eventId(eventId)
                .leaderUserId(3001L)
                .leaderType("PRODUCTION")
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .reviewSignatureId(1201L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signedAt\":\"2026-08-01T09:00:00\"}")
                .build();
    }
}
