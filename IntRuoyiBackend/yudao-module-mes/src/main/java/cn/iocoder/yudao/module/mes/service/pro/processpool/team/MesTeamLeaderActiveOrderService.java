package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.util.List;

public interface MesTeamLeaderActiveOrderService {

    List<MesTeamLeaderActiveOrderCandidateBO> searchActiveOrderCandidates(String keyword);

    MesTeamLeaderActiveOrderAddResult addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO);

    MesTeamLeaderActiveOrderTestResetResult resetFixedSimulationActiveOrder(Long leaderUserId);

    MesTeamLeaderActiveOrderSimulationCopyResult copyLatestSimulationActiveOrder(
            Long leaderUserId, Long sourceActiveOrderId, String simulationRunId);

    void cleanupLatestSimulationActiveOrder(Long leaderUserId, Long activeOrderId);

    MesTeamLeaderActiveOrderRebuildPreview previewRebuildActiveOrder(Long leaderUserId, Long activeOrderId);

    MesTeamLeaderActiveOrderRebuildResult rebuildActiveOrder(MesTeamLeaderActiveOrderRebuildReqBO reqBO);

    void removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO reqBO);

    MesTeamLeaderDataCleanupPreview previewDataCleanup(Long leaderUserId);

    MesTeamLeaderDataCleanupResult executeDataCleanup(Long leaderUserId,
                                                       MesTeamLeaderDataCleanupPreview expectedScope);

    void moveActiveOrder(MesTeamLeaderActiveOrderMoveReqBO reqBO);

    List<MesTeamLeaderActiveOrderRow> listActiveOrders(Long leaderUserId);

    void closeForRelease(Long activeOrderId, Integer expectedVersion,
                         Long releaseDecisionId, Long actorUserId);
}
