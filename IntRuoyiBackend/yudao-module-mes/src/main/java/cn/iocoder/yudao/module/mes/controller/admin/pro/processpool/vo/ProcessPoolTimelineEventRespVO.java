package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 工序池提交事件时间轴 Response VO")
@Data
@Accessors(chain = true)
public class ProcessPoolTimelineEventRespVO {

    @Schema(description = "工序池提交事件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "工序池编号", example = "10")
    private Long processPoolId;

    @Schema(description = "服务端提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "登录账号用户编号", example = "100")
    private Long loginUserId;

    @Schema(description = "登录账号名称", example = "device-account-A")
    private String loginUserName;

    @Schema(description = "实际填写员工编号", example = "2001")
    private Long actualEmployeeUserId;

    @Schema(description = "实际填写员工姓名", example = "张三")
    private String actualEmployeeUserName;

    @Schema(description = "电子签名员工编号", example = "2001")
    private Long signatureEmployeeUserId;

    @Schema(description = "电子签名员工姓名", example = "张三")
    private String signatureEmployeeUserName;

    @Schema(description = "电子签名编号", example = "8001")
    private Long electronicSignatureId;

    @Schema(description = "设备编号", example = "9001")
    private Long deviceId;

    @Schema(description = "设备编码", example = "EQ-001")
    private String deviceCode;

    @Schema(description = "设备名称", example = "灌装设备")
    private String deviceName;

    @Schema(description = "工作站编号", example = "3001")
    private Long workstationId;

    @Schema(description = "工作站编码", example = "WS-01")
    private String workstationCode;

    @Schema(description = "工作站名称", example = "一线工作站")
    private String workstationName;

    @Schema(description = "工艺路线编号", example = "4001")
    private Long routeId;

    @Schema(description = "工艺路线编码", example = "ROUTE-A")
    private String routeCode;

    @Schema(description = "工艺路线工序编号", example = "5001")
    private Long routeProcessId;

    @Schema(description = "工序编号", example = "6001")
    private Long processId;

    @Schema(description = "工序编码", example = "P-001")
    private String processCode;

    @Schema(description = "工序名称", example = "粗洗")
    private String processName;

    @Schema(description = "模板类型", example = "PRODUCTION")
    private String templateType;

    @Schema(description = "模板类型名称", example = "生产模板")
    private String templateTypeName;

    @Schema(description = "生产工单编号", example = "30001")
    private Long workOrderId;

    @Schema(description = "生产工单编码", example = "WO-20260730001")
    private String workOrderCode;

    @Schema(description = "生产工单名称", example = "生产工单")
    private String workOrderName;

    @Schema(description = "来源报工编号", example = "7001")
    private Long sourceFeedbackId;

    @Schema(description = "来源记录本条目编号", example = "7101")
    private Long sourceRecordbookEntryId;

    @Schema(description = "来源记录本事件编号", example = "7201")
    private Long sourceRecordbookEventId;

    @Schema(description = "提交摘要")
    private String submittedSummary;

    @Schema(description = "原始 payload")
    private String originalPayloadJson;

    @Schema(description = "PQC 结果", example = "PASS")
    private String pqcResult;

    @Schema(description = "PQC 摘要")
    private String pqcSummary;

    @Schema(description = "FIFO 分配状态", example = "PARTIAL")
    private String fifoAllocationStatus;

    @Schema(description = "FIFO 分配摘要")
    private String fifoAllocationSummary;

    @Schema(description = "审核副本状态", example = "PENDING")
    private String auditCopyStatus;

    @Schema(description = "审核副本摘要")
    private String auditCopySummary;

    @Schema(description = "最新组长复核状态", example = "REJECTED")
    private String submissionReviewStatus;

    @Schema(description = "最新组长复核说明")
    private String submissionReviewRemark;

    @Schema(description = "最新组长复核人用户编号", example = "3001")
    private Long submissionReviewLeaderUserId;

    @Schema(description = "最新组长复核时间")
    private LocalDateTime submissionReviewedAt;

    @Schema(description = "修改历史摘要")
    private String modificationHistorySummary;

}
