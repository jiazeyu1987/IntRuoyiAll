package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesPqcProductionReleaseDecisionResult {

    private Long applicationId;
    private Long pqcReleaseWorkTaskId;
    private String decision;
    private String status;
    private String rejectReason;
    private Long batchExecutionId;
    private List<Long> batchRecordEvidenceIds;
    private List<Long> processInspectionEvidenceIds;
    private List<Long> lossReportEvidenceIds;
    private List<MesProductionReleaseReportUploadTaskReceipt> reportUploadTasks;
    private String sourceSnapshotHash;
    private String reportSnapshotHash;
    private Integer version;
    private Long decidedBy;
    private LocalDateTime decidedAt;
    private String decisionIdempotencyKey;
    private String decisionPayloadHash;
}
