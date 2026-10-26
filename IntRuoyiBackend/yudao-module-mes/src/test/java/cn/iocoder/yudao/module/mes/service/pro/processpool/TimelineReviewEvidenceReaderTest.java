package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Association tests; the independently tested verified-fact service owns digest/audit evaluation. */
class TimelineReviewEvidenceReaderTest {
    private final TimelineReviewEvidenceReader reader = new TimelineReviewEvidenceReader();
    private final MesProcessPoolSubmissionReviewMapper reviews = mock(MesProcessPoolSubmissionReviewMapper.class);
    private final ElectronicSignatureVerifiedFactService signatures = mock(ElectronicSignatureVerifiedFactService.class);
    private final ProcessPoolTimelineEventReadDO event = new ProcessPoolTimelineEventReadDO().setId(11L)
            .setOriginalPayloadJson("{\"value\":9, \"supersededReviewId\":123}")
            .setDisplayedReviewId(124L).setDisplayedReviewRound(1).setDisplayedRevisionId(101L).setDisplayedReviewStatus("REJECTED");
    private final MesProcessPoolSubmissionReviewDO review = new MesProcessPoolSubmissionReviewDO().setId(124L)
            .setEventId(11L).setReviewStatus("REJECTED").setReviewRound(1).setSourceRevisionId(101L)
            .setLeaderType("PQC").setLeaderUserId(51L).setReviewSignatureUserId(51L).setReviewSignatureId(61L)
            .setReviewRemark("exact return reason").setReviewSignatureSnapshotJson("{}");
    TimelineReviewEvidenceReaderTest() {
        ReflectionTestUtils.setField(reader,"reviews",reviews);ReflectionTestUtils.setField(reader,"signatures",signatures);
        when(reviews.selectById(124L)).thenReturn(review);TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void pendingDoesNotInventAReviewOrReadSignatures(){
        event.setDisplayedReviewStatus("PENDING");assertTrue(reader.read(event).isEmpty());verifyNoInteractions(reviews,signatures);
    }
    @Test void rejectsForeignEventAndChangedRevisionBeforeSignatureRead(){
        review.setEventId(12L);assertThrows(IllegalStateException.class,()->reader.read(event));verifyNoInteractions(signatures);
        review.setEventId(11L).setSourceRevisionId(100L);assertTrue(reader.read(event).isEmpty());verifyNoInteractions(signatures);
    }
    @Test void invalidSignatureFactIsNeverProjectedAsValid(){
        when(signatures.requireVerifiedFact(61L)).thenThrow(new IllegalStateException("digest mismatch"));
        assertThrows(IllegalStateException.class,()->reader.read(event));
    }
    @Test void projectsOnlyTheActualVerifiedSignatureBoundToPersistedReview(){
        var fact=mock(ElectronicSignatureVerifiedFactService.VerifiedSignatureFact.class);
        var parsed=mock(MesSignatureContentProtocol.VerifiedMesSignatureContent.class);
        var verification=mock(ElectronicSignatureVerificationDTO.class);
        when(signatures.requireVerifiedFact(61L)).thenReturn(fact);
        when(fact.id()).thenReturn(61L);when(fact.actorId()).thenReturn(51L);when(fact.actionCode()).thenReturn("TEAM_LEADER_REVIEW");
        when(fact.reason()).thenReturn("组长复核:PQC:11:REJECTED");when(fact.verification()).thenReturn(verification);
        when(verification.verificationStatus()).thenReturn("VALID");
        when(parsed.businessContentJson()).thenReturn("{\"reviewSourceType\":\"PROCESS_POOL_EVENT\",\"reviewSourceId\":\"11\"}");
        try(var protocol=mockStatic(MesSignatureContentProtocol.class)) {
            protocol.when(()->MesSignatureContentProtocol.parseVerifiedContent(fact,1L)).thenReturn(parsed);
            var proof=reader.read(event).get(0);assertEquals("124",proof.reviewId());assertEquals("61",proof.signatureId());
            assertEquals(MesProBatchRecordExecutionFieldAuditHasher.sha256(event.getOriginalPayloadJson()),proof.rawPayloadHash());
            assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getOriginalPayloadJson()),proof.signaturePayloadHash());
            assertNotEquals(proof.rawPayloadHash(),proof.signaturePayloadHash());
            assertEquals("exact return reason",proof.reason());assertEquals("VALID",proof.verification());
            when(fact.actorId()).thenReturn(52L);assertThrows(IllegalStateException.class,()->reader.read(event));
            when(fact.actorId()).thenReturn(51L);when(parsed.businessContentJson()).thenReturn("{\"reviewSourceType\":\"PROCESS_POOL_EVENT\",\"reviewSourceId\":\"12\"}");
            assertThrows(IllegalStateException.class,()->reader.read(event));
        }
    }
}
