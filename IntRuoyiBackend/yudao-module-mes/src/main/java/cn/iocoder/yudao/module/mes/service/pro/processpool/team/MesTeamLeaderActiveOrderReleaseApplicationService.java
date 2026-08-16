package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderReleaseApplicationService {

    MesTeamLeaderActiveOrderReleaseApplicationResult apply(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command);

    MesTeamLeaderActiveOrderReleaseApplicationResult get(Long userId, Long activeOrderId);
}
