package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderFifoAllocationReqBO {

    private Long leaderUserId;
    private Long eventId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal confirmQuantity;
    private Long excludedEventId;
    private Set<Long> excludedActiveOrderIds;
}
