package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.util.List;

public interface MesTeamLeaderActiveOrderService {

    List<MesTeamLeaderActiveOrderCandidateBO> searchActiveOrderCandidates(String keyword);

    List<MesTeamLeaderPickListOptionBO> listPickListOptions(Long workOrderId);

    MesTeamLeaderActiveOrderAddResult addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO);

    MesTeamLeaderActiveOrderRebuildPreview previewRebuildActiveOrder(Long leaderUserId, Long activeOrderId);

    MesTeamLeaderActiveOrderRebuildResult rebuildActiveOrder(MesTeamLeaderActiveOrderRebuildReqBO reqBO);

    void removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO reqBO);

    void moveActiveOrder(MesTeamLeaderActiveOrderMoveReqBO reqBO);

    List<MesTeamLeaderActiveOrderRow> listActiveOrders(Long leaderUserId);
}
