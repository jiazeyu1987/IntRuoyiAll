package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderAllocationTraceRespVO {

    private Long eventId;
    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal totalAllocatedQuantity;
    private List<Line> lines;

    @Data
    @Accessors(chain = true)
    public static class Line {

        private Long allocationId;
        private Long reviewId;
        private Long leaderUserId;
        private Long activeOrderId;
        private Long workOrderId;
        private Long routeProcessId;
        private Long processId;
        private BigDecimal allocatedQuantity;
        private String allocationMode;
        private LocalDateTime confirmedAt;
    }
}
