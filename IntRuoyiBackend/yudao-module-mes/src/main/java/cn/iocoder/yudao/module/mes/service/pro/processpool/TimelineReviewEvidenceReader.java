package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;

/** Read-only projection linked by the displayed formal review primary key, not UI expectations. */
@Component
public class TimelineReviewEvidenceReader {
    @Resource private MesProcessPoolSubmissionReviewMapper reviews;
    @Resource private ElectronicSignatureVerifiedFactService signatures;

    public List<Evidence> read(ProcessPoolTimelineEventReadDO event) {
        if (!"REJECTED".equals(event.getDisplayedReviewStatus())) return List.of();
        if (event.getDisplayedReviewId() == null || event.getDisplayedReviewId() <= 0L)
            throw new IllegalStateException("Rejected review identity missing");
        var review = reviews.selectById(event.getDisplayedReviewId());
        if (review == null || !Objects.equals(review.getId(), event.getDisplayedReviewId()) || !Objects.equals(review.getEventId(), event.getId())
                || !Objects.equals(review.getReviewStatus(), event.getDisplayedReviewStatus())
                || !Objects.equals(review.getReviewRound(), event.getDisplayedReviewRound())
                || review.getReviewSignatureId() == null || review.getReviewSignatureUserId() == null
                || !Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId()))
            throw new IllegalStateException("Displayed formal review identity is inconsistent");
        // Formal review status retains the prior decision independently of derived reviewability.
        // A prior rejection cannot prove a review of the newly displayed signed correction.
        if (!Objects.equals(review.getSourceRevisionId() == null ? 0L : review.getSourceRevisionId(), event.getDisplayedRevisionId())) return List.of();
        var fact = signatures.requireVerifiedFact(review.getReviewSignatureId());
        var parsed = MesSignatureContentProtocol.parseVerifiedContent(fact, TenantContextHolder.getRequiredTenantId());
        var content = JsonUtils.parseTree(parsed.businessContentJson());
        if (!Objects.equals(fact.id(), review.getReviewSignatureId())
                || !Objects.equals(fact.actorId(), review.getReviewSignatureUserId())
                || !"TEAM_LEADER_REVIEW".equals(fact.actionCode())
                || !"PROCESS_POOL_EVENT".equals(content.path("reviewSourceType").asText())
                || !Objects.equals(content.path("reviewSourceId").asText(), String.valueOf(event.getId()))
                || !"VALID".equals(fact.verification().verificationStatus()))
            throw new IllegalStateException("Formal review signature is inconsistent");
        return List.of(new Evidence(String.valueOf(review.getEventId()), String.valueOf(review.getId()),
                String.valueOf(event.getDisplayedRevisionId()), review.getReviewRound(), review.getReviewStatus(),
                review.getLeaderType(), String.valueOf(review.getLeaderUserId()), review.getReviewRemark(),
                String.valueOf(fact.id()), String.valueOf(fact.actorId()), fact.actionCode(), fact.reason(),
                fact.verification().verificationStatus(), fact.contentHash(), fact.evidenceHash(),
                parsed.businessContentJson(), review.getReviewSignatureSnapshotJson(),
                MesProBatchRecordExecutionFieldAuditHasher.sha256(event.getOriginalPayloadJson()),
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getOriginalPayloadJson())));
    }
    public record Evidence(String eventId, String reviewId, String revisionId, Integer reviewRound,
            String status, String leaderType, String actorId, String reason, String signatureId,
            String signatureActorId, String signatureAction, String signatureReason, String verification,
            String contentHash, String evidenceHash, String signatureContent, String reviewSnapshot,
            String rawPayloadHash, String signaturePayloadHash) {}
}
