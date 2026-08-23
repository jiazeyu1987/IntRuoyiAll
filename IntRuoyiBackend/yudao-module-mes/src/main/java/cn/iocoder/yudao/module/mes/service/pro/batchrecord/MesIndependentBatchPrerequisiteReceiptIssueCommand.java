package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceiptIssueCommand {
    private String entryType;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeId;
    private Long routeVersionId;
    private String routeVersion;
    private String batchCode;
    private String sourceRelationId;
    private String sourceRelationVersion;
    private String sourceRelationSnapshotHash;
    private String sourceObjectType;
    private String sourceObjectId;
    private String materialSourceType;
    private String materialSourceId;
    private String sourceContextHash;
    private String sourceSnapshotHash;
    private String businessReason;
    private String idempotencyKey;
    private List<MesBatchExecutionSourceEvidence> sourceEvidence;
}
