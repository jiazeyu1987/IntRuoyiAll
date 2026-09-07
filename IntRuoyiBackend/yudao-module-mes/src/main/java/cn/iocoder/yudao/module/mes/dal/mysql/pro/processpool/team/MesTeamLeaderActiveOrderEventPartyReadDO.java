package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderEventPartyReadDO {

    private Long eventId;
    private String submitterName;
    private String reviewerName;
}
