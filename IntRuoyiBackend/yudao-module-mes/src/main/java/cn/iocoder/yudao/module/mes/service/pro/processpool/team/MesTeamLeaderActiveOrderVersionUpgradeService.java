package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderVersionUpgradeService {

    MesTeamLeaderActiveOrderVersionUpgradePreview preview(Long leaderUserId, Long activeOrderId);

    MesTeamLeaderActiveOrderVersionUpgradeSubmitResult submit(
            Long leaderUserId, MesTeamLeaderActiveOrderVersionUpgradeSubmitCommand command);

    void markApprovalPending(Long requestId, String processInstanceId, Long actorUserId);

    MesTeamLeaderActiveOrderVersionUpgradeApplyResult applyApprovedUpgrade(Long requestId, Long actorUserId);

    void rejectOrCancelApproval(Long requestId, Long actorUserId, String reason, boolean cancelled);
}
