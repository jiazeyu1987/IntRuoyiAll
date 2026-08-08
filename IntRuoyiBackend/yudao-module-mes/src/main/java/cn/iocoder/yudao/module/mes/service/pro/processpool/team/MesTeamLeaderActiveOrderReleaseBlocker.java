package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseBlocker {

    private String blockerType;
    private String objectType;
    private String objectId;
    private String objectCode;
    private String reason;
    private String suggestion;
}
