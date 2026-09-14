package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;

/** Reads production and PQC progress from locked formal sources. */
public interface MesTeamLeaderActiveOrderCompletionProgressPort {

    MesTeamLeaderActiveOrderCompletionProgress read(Long leaderUserId,
                                                     MesProcessPoolActiveOrderDO activeOrder);
}
