package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Read-only original business identity. Never reads signature JSON or infers an employee from a name. */
@Component
@RequiredArgsConstructor
public class MesSubmissionBusinessIdentityReader {
    private final MesProProcessPoolEventMapper events;
    private final cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper reviews;

    public String read(Long eventId, Long reviewSignatureId) {
        var event = events.selectById(eventId);
        if (event == null || !Objects.equals(event.getId(), eventId)
                || !Objects.equals(event.getTenantId(), TenantContextHolder.getRequiredTenantId()))
            throw new IllegalStateException("ORIGINAL_BUSINESS_EVENT_IDENTITY_MISSING");
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO review = null;
        if (reviewSignatureId != null) {
            var matches = reviews.selectListByEventId(eventId).stream().filter(row ->
                    Objects.equals(row.getTenantId(), event.getTenantId()) && Objects.equals(row.getEventId(), eventId)
                            && Objects.equals(row.getReviewSignatureId(), reviewSignatureId)).toList();
            if (matches.size() != 1) throw new IllegalStateException("ORIGINAL_BUSINESS_REVIEW_IDENTITY_MISSING");
            review = matches.get(0);
        }
        return project(event, reviewSignatureId, review);
    }

    static String project(MesProProcessPoolEventDO event, Long reviewSignatureId) {
        return project(event, reviewSignatureId, null);
    }

    static String project(MesProProcessPoolEventDO event, Long reviewSignatureId,
                          cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO review) {
        var payload = JSON.parseObject(event.getRawPayload());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", text(event.getId()));
        result.put("eventType", event.getEventType());
        result.put("actorId", text(event.getActualEmployeeId()));
        result.put("operatorId", text(event.getDeviceAccountId()));
        result.put("tenantId", text(event.getTenantId()));
        result.put("signatureId", text(event.getSignatureId()));
        result.put("payloadHash", cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()));
        result.put("reviewSignatureId", text(reviewSignatureId));
        result.put("reviewId", review == null ? null : text(review.getId()));
        result.put("reviewActorId", review == null ? null : text(review.getLeaderUserId()));
        result.put("reviewStatus", review == null ? null : review.getReviewStatus());
        result.put("leaderType", review == null ? null : review.getLeaderType());
        result.put("identityDomain", "PQC_INSPECTION".equals(event.getEventType()) ? "SYSTEM_USER"
                : payload == null ? null : payload.getString("signatureIdentityDomain"));
        result.put("activeOrderId", payload == null ? null : text(payload.getLong("activeOrderId")));
        result.put("routeProcessId", text(event.getRouteProcessId()));
        result.put("processId", text(event.getProcessId()));
        result.put("submissionIdempotencyKey", event.getEventIdempotencyKey());
        result.put("pqcTaskId", "MES_PQC_INSPECTION_TASK".equals(event.getFeedbackSourceType())
                ? text(event.getFeedbackSourceId()) : null);
        return JsonUtils.toJsonString(result);
    }

    private static String text(Long value) { return value == null ? null : value.toString(); }
}
