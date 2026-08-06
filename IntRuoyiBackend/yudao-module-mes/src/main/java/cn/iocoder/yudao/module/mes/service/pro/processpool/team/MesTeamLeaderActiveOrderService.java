package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;

import java.util.List;

public interface MesTeamLeaderActiveOrderService {

    List<MesTeamLeaderActiveOrderCandidateBO> searchActiveOrderCandidates(String keyword);

    Long addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO);

    void removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO reqBO);

    List<MesProcessPoolActiveOrderDO> listActiveOrders(Long leaderUserId);
}
