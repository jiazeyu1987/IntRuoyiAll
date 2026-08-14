package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderReleaseBatchRecordWriter {

    MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command);

    MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult write(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan, Long batchExecutionId);
}
