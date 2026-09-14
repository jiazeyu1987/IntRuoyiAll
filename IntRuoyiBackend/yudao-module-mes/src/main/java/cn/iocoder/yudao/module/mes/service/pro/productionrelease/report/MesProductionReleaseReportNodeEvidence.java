package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportNodeEvidence {

    private Long batchExecutionId;
    private Long batchTaskId;
    private String nodeType;
    private String sterilizationBatchNo;
    private Integer activeAttachmentVersion;
    private List<Long> attachmentIds;
    private List<String> attachmentHashes;

    public String toPayloadJson() {
        JSONObject payload = new JSONObject(true);
        payload.put("releaseReportEvidence", true);
        payload.put("batchExecutionId", batchExecutionId);
        payload.put("batchTaskId", batchTaskId);
        payload.put("nodeType", nodeType);
        payload.put("sterilizationBatchNo", sterilizationBatchNo);
        payload.put("activeAttachmentVersion", activeAttachmentVersion);
        payload.put("attachmentIds", attachmentIds);
        payload.put("attachmentHashes", attachmentHashes);
        return payload.toJSONString();
    }

    public static MesProductionReleaseReportNodeEvidence fromPayloadJson(String payloadJson) {
        JSONObject payload = JSON.parseObject(payloadJson);
        if (payload == null || !Boolean.TRUE.equals(payload.getBoolean("releaseReportEvidence"))) {
            return null;
        }
        return new MesProductionReleaseReportNodeEvidence()
                .setBatchExecutionId(payload.getLong("batchExecutionId"))
                .setBatchTaskId(payload.getLong("batchTaskId"))
                .setNodeType(payload.getString("nodeType"))
                .setSterilizationBatchNo(payload.getString("sterilizationBatchNo"))
                .setActiveAttachmentVersion(payload.getInteger("activeAttachmentVersion"))
                .setAttachmentIds(payload.getJSONArray("attachmentIds") == null ? List.of()
                        : payload.getJSONArray("attachmentIds").toJavaList(Long.class))
                .setAttachmentHashes(payload.getJSONArray("attachmentHashes") == null ? List.of()
                        : payload.getJSONArray("attachmentHashes").toJavaList(String.class));
    }
}
