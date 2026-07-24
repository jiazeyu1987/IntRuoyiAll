package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrWorkTaskRespVO {

    private Long id;

    private String taskCode;

    private String taskType;

    private Long batchExecutionId;

    private Long batchTaskId;

    private String businessScopeType;

    private Long businessScopeId;

    private Long executionId;

    private Long sourceExecutionId;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Long routeProcessId;

    private String processName;

    private Long assigneeUserId;

    private String assigneeUserName;

    private String candidateSourceType;

    private Long candidateSourceId;

    private String candidatePoolName;

    private String candidateUserSnapshot;

    private String candidateSnapshotDisplay;

    private Long sourceUserId;

    private String sourceUserName;

    private String responsibilitySource;

    private String inactionReason;

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    private Long reviewSourceId;

    private String reviewSourceName;

    private String bpmTaskId;

    private String status;

    private LocalDateTime dueTime;

    private LocalDateTime overdueAt;

    private String overdueReason;

    private LocalDateTime completedAt;

    private String actionUrl;

    private String reason;

    private String remark;

    private LocalDateTime createTime;
}
