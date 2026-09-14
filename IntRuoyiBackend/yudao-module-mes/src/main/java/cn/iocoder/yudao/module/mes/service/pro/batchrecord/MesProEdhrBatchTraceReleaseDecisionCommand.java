package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceReleaseDecisionCommand {

    private Long batchExecutionId;
    private Long originId;
    private Long releaseApplicationId;
    private Long releaseDecisionId;
    private String sourceSnapshotJson;
    private String sourceSnapshotHash;
    private String idempotencyKey;
    private Long capturedBy;
}
