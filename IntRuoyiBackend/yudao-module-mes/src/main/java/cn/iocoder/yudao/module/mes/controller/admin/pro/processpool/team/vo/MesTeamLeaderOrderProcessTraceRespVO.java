package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesTeamLeaderOrderProcessTraceRespVO {

    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal targetQuantity;
    private BigDecimal confirmedQuantity;
    private String completionStatus;
    private LocalDateTime completedAt;
    private String backfillStatus;
    private Long backfillExecutionId;
    private String backfillError;
    private Long lastEventId;
    private Long lastReviewId;
}
