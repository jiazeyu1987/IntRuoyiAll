package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionApprovalRespVO {

    private Long id;
    private Long executionId;
    private Long workTaskId;
    private String taskType;

    private String executionCode;

    private String workOrderCode;

    private String batchCode;

    private String processName;

    private String workstationName;

    private Integer status;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private String decision;

    private LocalDateTime handledAt;

    private Long actorId;

    private String actorName;

    private String processInstanceId;

    private String bpmTaskId;

    private String bpmTaskName;
    private String taskName;

    private String bpmTaskDefinitionKey;
    private String taskDefinitionKey;

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    private Long reviewSourceId;

    private String reviewSourceName;

    private Long approvalSnapshotId;
    private String approvalSnapshotHash;
    private String approvalSnapshotStatus;
    private Boolean canApprove;
    private Boolean canReject;
    private Boolean canViewTracking;
    private Boolean canViewSignatures;
    private Boolean canGenerateArchive;
    private Boolean canDownloadArchive;
    private LocalDateTime closedAt;

    private String executionSnapshotJson;

    private List<MesProBatchRecordExecutionCellValueVO> cellValues;

    private List<MesProBatchRecordExecutionSignatureRespVO> signatureSummaries;
}
