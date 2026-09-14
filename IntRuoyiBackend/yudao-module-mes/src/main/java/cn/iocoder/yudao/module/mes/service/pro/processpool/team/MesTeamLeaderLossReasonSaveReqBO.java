package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderLossReasonSaveReqBO {

    private Long leaderUserId;
    private Long routeProcessId;
    private String reasonName;

}
