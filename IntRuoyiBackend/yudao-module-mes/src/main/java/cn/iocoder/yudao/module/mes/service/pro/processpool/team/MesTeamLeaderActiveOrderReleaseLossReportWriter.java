package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderReleaseLossReportWriter {

    MesTeamLeaderActiveOrderReleaseLossReportPlan plan(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command);

    MesTeamLeaderActiveOrderReleaseLossReportWriteResult write(
            MesTeamLeaderActiveOrderReleaseLossReportPlan plan, Long batchExecutionId);
}
