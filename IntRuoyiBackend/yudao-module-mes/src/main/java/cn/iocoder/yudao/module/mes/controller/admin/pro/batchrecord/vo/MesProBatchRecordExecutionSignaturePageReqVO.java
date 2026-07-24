package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProBatchRecordExecutionSignaturePageReqVO extends PageParam {

    private Long executionId;

    private String executionCode;

    private String actionType;

    private Long actorId;

    private String actorName;

    private String processInstanceId;

    private String bpmTaskId;

    private LocalDateTime beginSignedAt;

    private LocalDateTime endSignedAt;

    private LocalDateTime signedAtStart;

    private LocalDateTime signedAtEnd;
}
