package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

/** Server-validated event content and the exact formal correction reviewed in this round. */
public record MesTeamLeaderReviewSignatureContext(Long eventId, String reviewStatus, String payloadHash,
                                                   Long revisionId, Long revisionSignatureId,
                                                   Long supersededReviewId) {
    public static final String SOURCE_TYPE = "PROCESS_POOL_EVENT";

    public MesTeamLeaderReviewSignatureContext {
        if (eventId == null || eventId <= 0
                || !("APPROVED".equals(reviewStatus) || "REJECTED".equals(reviewStatus))
                || payloadHash == null || !payloadHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Exact event, decision and current content hash are required");
        }
        boolean initial = revisionId == null && revisionSignatureId == null && supersededReviewId == null;
        if (!initial && (revisionId == null || revisionId <= 0 || revisionSignatureId == null
                || revisionSignatureId <= 0 || supersededReviewId == null || supersededReviewId <= 0)) {
            throw new IllegalArgumentException("The complete signed correction identity is required");
        }
    }

    public String sourceName() {
        return JsonUtils.toJsonString(this);
    }
}
