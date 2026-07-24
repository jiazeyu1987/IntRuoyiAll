package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProBatchRecordExecutionTrackingPageReqVO extends PageParam {

    private String executionCode;

    private String workOrderCode;

    private String batchCode;

    private Long processId;

    private Long workstationId;

    private Integer status;

    private Long submittedBy;

    private Long approvedBy;

    private String processInstanceId;

    private String actorName;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private LocalDateTime occurredAtStart;

    private LocalDateTime occurredAtEnd;
}
