package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import lombok.Data;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportNodeCompleteResult {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchExecutionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchTaskId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workTaskId;
    private String nodeType;
    private String nodeStatus;
    private Integer activeAttachmentVersion;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> attachmentIds;
    private List<String> attachmentHashes;
    private String reportUploadStatus;
    private String reportSnapshotHash;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long releaseTransactionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long managerReleaseWorkTaskId;
    private Integer version;
}
