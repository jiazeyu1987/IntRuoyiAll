package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderCompletionService {

    MesTeamLeaderActiveOrderCompletionResult complete(
            Long leaderUserId, MesTeamLeaderActiveOrderCompletionCommand command);
}
