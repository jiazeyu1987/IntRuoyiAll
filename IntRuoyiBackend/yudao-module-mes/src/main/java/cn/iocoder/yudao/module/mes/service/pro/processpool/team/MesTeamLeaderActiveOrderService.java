package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.util.List;

public interface MesTeamLeaderActiveOrderService {

    List<MesTeamLeaderActiveOrderCandidateBO> searchActiveOrderCandidates(String keyword);

    Long addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO);

    void removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO reqBO);

    void moveActiveOrder(MesTeamLeaderActiveOrderMoveReqBO reqBO);

    List<MesTeamLeaderActiveOrderRow> listActiveOrders(Long leaderUserId);
}
