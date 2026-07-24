package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MesProBatchRecordExecutionTrackingRespVO {

    private Long executionId;
    private String executionCode;
    private Long workOrderId;
    private String workOrderCode;
    private Long batchId;
    private String batchCode;
    private String processName;
    private String workstationName;
    private Integer status;
    private String processInstanceId;
    private String currentNodeName;
    private List<String> currentAssigneeNames;
    private String lastEventType;
    private String lastEvidenceCategory;
    private String lastEvidenceCategoryName;
    private String lastEventReason;
    private LocalDateTime lastEventAt;
    private LocalDateTime closedAt;
    private String archiveStatus;
}
