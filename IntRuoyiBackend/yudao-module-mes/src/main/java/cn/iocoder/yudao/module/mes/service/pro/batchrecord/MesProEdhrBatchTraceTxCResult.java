package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceTxCResult {

    private String eventId;
    private Long batchExecutionId;
    private Long originId;
    private Long originLinkId;
    private String traceLinkHash;
    private String sourceSnapshotHash;
    private Integer manifestVersion;
    private String status;
    private String eventType;
    private String errorCode;
    private String reason;
    private Boolean idempotent;
    private Boolean retryable;
}
