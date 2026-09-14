package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceSource {

    private String linkType;
    private String sourceObjectType;
    private Long sourceObjectId;
    private Long sourceLineId;
    private Long sourceEventId;
    private Integer sourceVersion;
    private String sourceIdentityKey;
    private String snapshotJson;
    private String snapshotHash;
    private String relationStatus;
    private String relationReason;
}
