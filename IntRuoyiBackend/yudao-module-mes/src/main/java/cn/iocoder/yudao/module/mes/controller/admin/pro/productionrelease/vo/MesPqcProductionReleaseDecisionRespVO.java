package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesPqcProductionReleaseDecisionRespVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long pqcReleaseWorkTaskId;

    private String decision;
    private String status;
    private String rejectReason;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchExecutionId;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> batchRecordEvidenceIds;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> processInspectionEvidenceIds;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> lossReportEvidenceIds;

    private List<MesProductionReleaseReportUploadTaskRespVO> reportUploadTasks;
    private String sourceSnapshotHash;
    private String reportSnapshotHash;
    private Integer version;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long decidedBy;

    private LocalDateTime decidedAt;
}
