package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessOverageLimitDO;

import java.math.BigDecimal;
import java.util.List;

public interface MesTeamLeaderOverageLimitService {
    /**
     * Returns configured limits for the leader. Missing process-specific values are represented as the
     * business default (10 percent) by the lookup methods below.
     */
    List<MesProcessPoolTeamProcessOverageLimitDO> list(Long leaderUserId);
    MesProcessPoolTeamProcessOverageLimitDO save(Long leaderUserId, Long routeProcessId, Long processId,
                                                  BigDecimal overagePercent);
    BigDecimal requirePercent(Long leaderUserId, Long routeProcessId, Long processId);
    BigDecimal findPercent(Long leaderUserId, Long routeProcessId, Long processId);
    void assertWithinLimit(Long leaderUserId, Long routeProcessId, Long processId,
                           BigDecimal plannedQuantity, BigDecimal submittedQuantity);
}
