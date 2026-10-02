package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.approval.MesFeedbackApprovalErrorCodeConstants.*;

/** Read-only projection. Never completes a feedback or runs its inventory approval. */
@Component
public class MesFeedbackFormalReviewProjection {
    public static final String FEEDBACK_SOURCE_TYPE = "MES_PRO_FEEDBACK";
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;

    public MesFeedbackFormalReviewProjection(MesProProcessPoolEventMapper eventMapper,
                                            MesProcessPoolSubmissionReviewMapper reviewMapper) {
        this.eventMapper = eventMapper;
        this.reviewMapper = reviewMapper;
    }

    public record Fact(MesProProcessPoolEventDO event, MesProcessPoolSubmissionReviewDO review) {
        public boolean reviewed() { return review != null; }
        public Long reviewerId() { return review == null ? null : review.getLeaderUserId(); }
    }

    public Map<Long, Fact> read(Collection<MesProFeedbackDO> feedbacks) {
        if (feedbacks.isEmpty()) return Map.of();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Map<Long, MesProFeedbackDO> sources = new LinkedHashMap<>();
        for (MesProFeedbackDO feedback : feedbacks) {
            require(feedback.getId() != null && sources.putIfAbsent(feedback.getId(), feedback) == null,
                    feedback.getId(), "来源报工不唯一");
        }
        List<MesProProcessPoolEventDO> events = eventMapper.selectFormalProductionByFeedbackIds(
                tenantId, sources.keySet());
        Map<Long, MesProProcessPoolEventDO> byFeedback = new LinkedHashMap<>();
        Map<Long, MesProProcessPoolEventDO> eventsById = new LinkedHashMap<>();
        for (MesProProcessPoolEventDO event : events) {
            Long id = event.getFeedbackSourceId();
            MesProFeedbackDO feedback = sources.get(id);
            require(feedback != null && Objects.equals(tenantId, event.getTenantId())
                    && event.getId() != null
                    && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                    && FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType()), id, "事件来源身份不一致");
            require(byFeedback.putIfAbsent(id, event) == null, id, "同一报工关联多个正式事件");
            require(eventsById.putIfAbsent(event.getId(), event) == null, id, "正式事件身份不唯一");
            require(Objects.equals(event.getWorkOrderId(), feedback.getWorkOrderId())
                    && event.getWorkOrderId() != null && event.getRouteId() != null && event.getProcessId() != null
                    && Objects.equals(event.getRouteId(), feedback.getRouteId())
                    && Objects.equals(event.getProcessId(), feedback.getProcessId()), id, "工单/路线/工序身份不一致");
            require(event.getSignatureId() != null && event.getSignatureUserId() != null
                    && event.getServerSubmitTime() != null
                    && Objects.equals(event.getSignatureUserId(), event.getActualEmployeeId())
                    && Objects.equals(event.getSignatureUserId(), feedback.getFeedbackUserId()), id, "生产提交签名身份缺失");
        }
        Map<Long, MesProcessPoolSubmissionReviewDO> byEvent = new LinkedHashMap<>();
        List<Long> eventIds = events.stream().map(MesProProcessPoolEventDO::getId).toList();
        for (MesProcessPoolSubmissionReviewDO review : reviewMapper.selectFormalReviewsByEventIds(tenantId, eventIds)) {
            MesProProcessPoolEventDO event = eventsById.get(review.getEventId());
            Long id = event == null ? null : event.getFeedbackSourceId();
            require(event != null && Objects.equals(tenantId, review.getTenantId()), id, "复核事件或租户不一致");
            require(byEvent.putIfAbsent(review.getEventId(), review) == null, id, "同一正式事件存在多个复核");
            validateReview(id, event, review);
        }
        Map<Long, Fact> result = new LinkedHashMap<>();
        byFeedback.forEach((id, event) -> result.put(id, new Fact(event, byEvent.get(event.getId()))));
        return result;
    }

    /** Include records reviewed by this user even when the original designated approver differs. */
    public List<Long> reviewedFeedbackIds(Long reviewerId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<MesProcessPoolSubmissionReviewDO> reviews = reviewMapper.selectProductionReviewsByReviewer(tenantId, reviewerId);
        List<Long> eventIds = reviews.stream().map(MesProcessPoolSubmissionReviewDO::getEventId).distinct().toList();
        List<MesProProcessPoolEventDO> events = eventMapper.selectFormalProductionByEventIds(tenantId, eventIds);
        Map<Long, MesProProcessPoolEventDO> byId = new LinkedHashMap<>();
        for (MesProProcessPoolEventDO event : events) {
            require(Objects.equals(tenantId, event.getTenantId()) && event.getId() != null
                    && event.getFeedbackSourceId() != null
                    && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                    && FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                    && byId.putIfAbsent(event.getId(), event) == null,
                    event.getFeedbackSourceId(), "实际复核人事件来源不一致");
        }
        for (MesProcessPoolSubmissionReviewDO review : reviews) {
            MesProProcessPoolEventDO event = byId.get(review.getEventId());
            require(event != null && Objects.equals(tenantId, review.getTenantId())
                    && Objects.equals(reviewerId, review.getLeaderUserId()),
                    event == null ? null : event.getFeedbackSourceId(), "实际复核人记录来源不一致");
            validateReview(event.getFeedbackSourceId(), event, review);
        }
        return events.stream().map(MesProProcessPoolEventDO::getFeedbackSourceId).distinct().toList();
    }

    /** Used by both the adapter and all direct legacy service entry points, before side effects. */
    public void assertLegacyOperationAllowed(Long feedbackId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!eventMapper.selectFormalProductionByFeedbackIds(tenantId, List.of(feedbackId)).isEmpty()) {
            throw exception(FORMAL_FEEDBACK_LEGACY_REVIEW_FORBIDDEN, feedbackId);
        }
    }

    private static void validateReview(Long feedbackId, MesProProcessPoolEventDO event,
                                       MesProcessPoolSubmissionReviewDO review) {
        require(review.getId() != null && "PRODUCTION".equals(review.getLeaderType())
                && (MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())
                || MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(review.getReviewStatus()))
                && review.getReviewedAt() != null && review.getLeaderUserId() != null
                && review.getReviewSignatureId() != null && review.getReviewSignatureUserId() != null
                && Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())
                && hasText(review.getReviewSignatureSnapshotJson()), feedbackId, "正式复核或签名身份缺失");
        JsonNode signature;
        try {
            signature = JsonUtils.parseObject(review.getReviewSignatureSnapshotJson(), JsonNode.class);
        } catch (RuntimeException error) {
            throw exception(FORMAL_FEEDBACK_EVIDENCE_INVALID, feedbackId, "正式复核签名快照无法解析");
        }
        require(signature != null && signature.isObject()
                && signature.path("signatureId").isIntegralNumber()
                && signature.path("signatureId").longValue() == review.getReviewSignatureId()
                && signature.path("actorId").isIntegralNumber()
                && signature.path("actorId").longValue() == review.getReviewSignatureUserId()
                && signature.path("processPoolEventId").isIntegralNumber()
                && signature.path("processPoolEventId").longValue() == event.getId()
                && "TEAM_LEADER_REVIEW".equals(signature.path("actionType").asText())
                && event.getEventType().equals(signature.path("eventType").asText())
                && "PRODUCTION".equals(signature.path("leaderType").asText())
                && review.getReviewStatus().equals(signature.path("reviewStatus").asText()),
                feedbackId, "正式复核签名快照身份不一致");
    }

    private static boolean hasText(String text) { return text != null && !text.isBlank(); }
    private static void require(boolean valid, Long id, String reason) {
        if (!valid) throw exception(FORMAL_FEEDBACK_EVIDENCE_INVALID, id, reason);
    }
}
