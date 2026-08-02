package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderAddReqBO {

    private Long leaderUserId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
}
