package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ProcessPoolTimelineEventReadDO {

    private Long id;
    private Long processPoolId;
    private LocalDateTime submittedAt;
    private Long loginUserId;
    private String loginUserName;
    private Long actualEmployeeUserId;
    private String actualEmployeeUserName;
    private Long signatureEmployeeUserId;
    private String signatureEmployeeUserName;
    private Long electronicSignatureId;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long workstationId;
    private String workstationCode;
    private String workstationName;
    private Long routeId;
    private String routeCode;
    private Long routeProcessId;
    private Long processId;
    private String processCode;
    private String processName;
    private String templateType;
    private String templateTypeName;
    private Long workOrderId;
    private String workOrderCode;
    private String workOrderName;
    private Long productId;
    private String productCode;
    private String productName;
    private Long pqcTaskId;
    private String inspectionType;
    private LocalDate pqcBusinessDate;
    private String pqcShiftCode;
    private Integer roundNo;
    private Long sourceFeedbackId;
    private Long sourceRecordbookEntryId;
    private Long sourceRecordbookEventId;
    private String submittedSummary;
    private String originalPayloadJson;
    private String pqcResult;
    private String pqcSummary;
    private String processInspectionAggregationStatus;
    private Long processInspectionReviewId;
    private LocalDateTime processInspectionAggregatedAt;
    private String fifoAllocationStatus;
    private String fifoAllocationSummary;
    private String auditCopyStatus;
    private String auditCopySummary;
    private String submissionReviewStatus;
    private String submissionReviewRemark;
    private Long submissionReviewLeaderUserId;
    private LocalDateTime submissionReviewedAt;
    private String modificationHistorySummary;

}
