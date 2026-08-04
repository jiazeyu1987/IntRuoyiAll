package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderAddReqBO {

    private Long leaderUserId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
    private List<Long> transferIds;
}
