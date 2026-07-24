package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionApprovalActionRespVO {

    private Long executionId;

    private Long revisionExecutionId;

    private Long reworkTaskId;

    private Long approveTaskId;

    private Integer status;

    private String resultType;

    private String processInstanceId;

    private String bpmTaskId;

    private Long signatureId;

    private Long trackingEventId;

    private LocalDateTime closedAt;

    private LocalDateTime rejectedAt;
}
