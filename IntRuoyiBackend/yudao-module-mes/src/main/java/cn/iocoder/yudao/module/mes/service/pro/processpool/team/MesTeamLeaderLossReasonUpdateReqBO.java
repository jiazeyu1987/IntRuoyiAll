package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderLossReasonUpdateReqBO {

    private Long leaderUserId;
    private Long id;
    private String reasonName;
    private Boolean enabled;
    private String remark;

}
