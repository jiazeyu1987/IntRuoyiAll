package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/** The QA writer and its read-only lineage projection use the same signed-content protocol. */
public final class MesEdhrQaDispositionAggregateHash {
    private MesEdhrQaDispositionAggregateHash() {}

    public static String build(MesProEdhrNonconformanceReviewDO review, String disposition,
                               String reviewMaterialUrl, Long reviewMaterialFileId,
                               String reviewMaterialsJson, String reviewOpinion, Long qaUserId) {
        JSONObject payload = new JSONObject(true);
        payload.put("reviewId", review.getId());
        payload.put("reviewCode", review.getReviewCode());
        payload.put("sourceType", review.getSourceType());
        payload.put("sourceId", review.getSourceId());
        payload.put("batchExecutionId", review.getBatchExecutionId());
        payload.put("workOrderId", review.getWorkOrderId());
        payload.put("disposition", disposition);
        payload.put("reviewMaterialUrl", reviewMaterialUrl);
        payload.put("reviewMaterialFileId", reviewMaterialFileId);
        payload.put("reviewMaterialsJson", reviewMaterialsJson);
        payload.put("reviewOpinion", reviewOpinion);
        payload.put("qaUserId", qaUserId);
        return DigestUtil.sha256Hex(JSON.toJSONString(payload));
    }
}
