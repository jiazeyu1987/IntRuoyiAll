package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderReleaseDossierCompletenessChecker {

    MesTeamLeaderActiveOrderReleaseDossierCompletenessResult check(
            MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand command);
}
