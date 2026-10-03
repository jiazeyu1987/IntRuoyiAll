package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RELEASE-SIGNATURE01: a nonempty cached string is not authenticated signature evidence. */
class MesProductionReleaseSignatureContractTest {

    private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 10, 1, 10, 43, 46);
    private static final LocalDateTime REVIEWED_AT = SUBMITTED_AT.plusMinutes(1);

    @Test
    void batchRecordGuardRejectsUnverifiedNonemptySignatureSnapshot() {
        var writer = mock(MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.class, CALLS_REAL_METHODS);
        inject(writer);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(writer, "validEventSignature", event())),
                "The formal signature FK, actor, production subject and live evidence hash must be verified");
    }

    @Test
    void lossReportGuardRejectsUnverifiedNonemptySignatureSnapshot() {
        var writer = mock(MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.class, CALLS_REAL_METHODS);
        inject(writer);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(writer, "validSignatures",
                feedback(), event(), allocation(), review())),
                "A nonempty event snapshot cannot substitute for the formal production signature");
    }

    @Test
    void lossSourceGuardRejectsUnverifiedNonemptySignatureSnapshot() {
        var reader = mock(MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.class, CALLS_REAL_METHODS);
        inject(reader);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(reader, "validSignatures",
                feedback(), event(), allocation(), review())),
                "The source reader must enforce the same formal signature contract as both writers");
    }

    @Test
    void allThreeGuardsAcceptAnAuthenticatedFormalSignatureWithNoCachedEventSnapshot() {
        var event = event().setSignatureSnapshot(null);
        var batch = mock(MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.class, CALLS_REAL_METHODS);
        var loss = mock(MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.class, CALLS_REAL_METHODS);
        var source = mock(MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.class, CALLS_REAL_METHODS);
        for (Object consumer : java.util.List.of(batch, loss, source)) {
            var evidence = inject(consumer);
            when(evidence.isValidForEvent(event)).thenReturn(true);
            Boolean valid = consumer == batch ? ReflectionTestUtils.invokeMethod(consumer, "validEventSignature", event)
                    : ReflectionTestUtils.invokeMethod(consumer, "validSignatures", feedback(), event, allocation(), review());
            assertTrue(Boolean.TRUE.equals(valid));
            verify(evidence).isValidForEvent(event);
        }
    }

    private static MesProductionSignatureEvidenceService inject(Object consumer) {
        var evidence = mock(MesProductionSignatureEvidenceService.class);
        ReflectionTestUtils.setField(consumer, "productionSignatureEvidenceService", evidence);
        return evidence;
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder().id(1001L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .eventIdempotencyKey("production-submit-contract-1001")
                .workOrderId(30L).routeId(40L).routeProcessId(101L).processId(1L)
                .feedbackSourceType("MES_PRO_FEEDBACK").feedbackSourceId(501L)
                .actualEmployeeId(2101L).signatureUserId(2101L).signatureId(1101L)
                .serverSubmitTime(SUBMITTED_AT).rawPayload("{\"outputQuantity\":80}")
                .signatureSnapshot("{\"signedAt\":\"2026-10-01T10:43:46\"}").build();
    }

    private static MesProFeedbackDO feedback() {
        return new MesProFeedbackDO().setId(501L).setFeedbackUserId(2101L).setApproveUserId(3001L);
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder().id(7101L).activeOrderId(10L)
                .workOrderId(30L).routeProcessId(101L).processId(1L).eventId(1001L).reviewId(7201L)
                .leaderUserId(3001L).confirmedAt(REVIEWED_AT).build();
    }

    private static MesProcessPoolSubmissionReviewDO review() {
        return MesProcessPoolSubmissionReviewDO.builder().id(7201L).eventId(1001L)
                .leaderType("PRODUCTION").leaderUserId(3001L)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewSignatureId(1201L).reviewSignatureUserId(3001L).reviewedAt(REVIEWED_AT)
                .reviewSignatureSnapshotJson("{\"signedAt\":\"2026-10-01T10:44:46\"}").build();
    }
}
