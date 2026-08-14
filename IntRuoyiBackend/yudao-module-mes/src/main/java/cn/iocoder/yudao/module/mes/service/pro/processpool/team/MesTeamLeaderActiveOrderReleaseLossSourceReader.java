package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderReleaseLossSourceReader {

    MesTeamLeaderActiveOrderReleaseLossSourceReadResult read(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command);
}
