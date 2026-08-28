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
    void shouldRejectDuplicateProductionSubmitsForTheSameSnapshot() {
        MesProProcessPoolEventDO firstEvent = event(1001L,
                "{\"lossQuantity\":999,\"lossDetails\":[{\"reasonId\":8301,"
                        + "\"reasonCode\":\"LOSS-001\",\"reasonName\":\"正常损耗\","
                        + "\"quantity\":2.500}]}"
        );
        MesProProcessPoolEventDO secondEvent = event(1002L,
                "{\"lossQuantity\":999,\"lossDetails\":[{\"reasonId\":8301,"
                        + "\"reasonCode\":\"LOSS-001\",\"reasonName\":\"正常损耗\","
                        + "\"quantity\":2.500}]}"
        );
        when(eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of(firstEvent, secondEvent));
        when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L)))
                .thenReturn(List.of(feedback()));

        MesTeamLeaderActiveOrderReleaseLossSourceReadResult result = reader.read(command());

        assertTrue(result.getBlockers().stream().anyMatch(blocker ->
                "LOSS_SOURCE_REQUIRED".equals(blocker.getBlockerType())
                        && ROUTE_PROCESS_ID.equals(blocker.getRouteProcessId())
                        && "PRODUCTION_EVENT".equals(blocker.getObjectType())
                        && "当前活跃订单工序存在重复签名生产提交，无法形成唯一损耗来源闭环".equals(blocker.getReason())));
        assertTrue(result.getProcessSources().isEmpty());
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
        return event(1001L, payload);
    }

    private static MesProProcessPoolEventDO event(Long id, String payload) {
        return MesProProcessPoolEventDO.builder()
                .id(id)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2101L)
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(5101L)
                .rawPayload(payload)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .signatureId(1101L)
                .signatureUserId(2101L)
                .signatureSnapshot("{\"signedAt\":\"2026-08-01T08:30:00\"}")
                .build();
    }

    private static MesProFeedbackDO feedback() {
        return MesProFeedbackDO.builder()
                .id(5101L)
                .code("FB-5101")
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .processId(PROCESS_ID)
                .feedbackTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .feedbackQuantity(new BigDecimal("100.000"))
                .qualifiedQuantity(new BigDecimal("97.500"))
                .unqualifiedQuantity(new BigDecimal("2.500"))
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
        return MesProcessPoolReportAllocationDO.builder()
                .id(7101L)
                .eventId(1001L)
                .reviewId(7201L)
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
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(7201L)
                .eventId(1001L)
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
