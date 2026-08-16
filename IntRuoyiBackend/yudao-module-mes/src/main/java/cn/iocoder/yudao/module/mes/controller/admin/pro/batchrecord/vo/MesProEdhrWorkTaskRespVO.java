package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrWorkTaskRespVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String taskCode;

    private String taskType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchExecutionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchTaskId;

    private String businessScopeType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long businessScopeId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long executionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceExecutionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeProcessId;

    private String processName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long assigneeUserId;

    private String assigneeUserName;

    private String candidateSourceType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateSourceId;

    private String candidatePoolName;

    private String candidateUserSnapshot;

    private String candidateSnapshotDisplay;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceUserId;

    private String sourceUserName;

    private String responsibilitySource;

    private String inactionReason;

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewSourceId;

    private String reviewSourceName;

    private String bpmTaskId;

    private String status;

    private String nodeType;

    private String nodeName;

    private Integer version;

    private LocalDateTime dueTime;

    private LocalDateTime overdueAt;

    private String overdueReason;

    private LocalDateTime completedAt;

    private String actionUrl;

    private String reason;

    private String remark;

    private LocalDateTime createTime;
}
