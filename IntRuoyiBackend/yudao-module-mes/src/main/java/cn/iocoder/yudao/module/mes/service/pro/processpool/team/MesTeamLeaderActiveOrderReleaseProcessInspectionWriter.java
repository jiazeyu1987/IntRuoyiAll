package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderReleaseProcessInspectionWriter {

    MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command);

    MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult write(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan, Long batchExecutionId);
}
