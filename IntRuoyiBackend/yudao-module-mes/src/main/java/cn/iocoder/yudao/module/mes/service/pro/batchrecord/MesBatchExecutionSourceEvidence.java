package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/** Immutable upstream evidence item carried from flows 1/2/3/5 into flow 6. */
@Data
@Accessors(chain = true)
public class MesBatchExecutionSourceEvidence {

    private String sourceType;
    private String sourceId;
    private String sourceVersion;
    private String sourceSnapshotHash;
    private String payloadHash;
    private String signature;
    private String sourceObjectType;
    private String sourceObjectId;
    private String snapshotJson;
    private String sourceIdentityKey;
    private String relationStatus;
    private String relationReason;
}
