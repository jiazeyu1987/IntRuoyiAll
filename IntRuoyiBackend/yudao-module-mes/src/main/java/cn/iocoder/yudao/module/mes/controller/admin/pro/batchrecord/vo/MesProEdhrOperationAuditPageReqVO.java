package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrOperationAuditPageReqVO extends PageParam {

    private Long batchExecutionId;

    private Long executionId;

    private Long workTaskId;

    private Long routeId;

    private Long routeProcessId;

    private String reportId;

    private String recordCategory;

    private String objectType;

    private String objectId;

    private String operationType;

    private Long actorUserId;

    private String permissionDecision;

    private String resultStatus;

    private LocalDateTime[] occurredAt;
}
