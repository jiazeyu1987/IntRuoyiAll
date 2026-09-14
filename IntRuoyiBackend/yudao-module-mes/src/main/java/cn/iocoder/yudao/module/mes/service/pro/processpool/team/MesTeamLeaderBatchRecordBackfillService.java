package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderBatchRecordBackfillService {

    MesTeamLeaderBatchRecordBackfillResult backfillCompletedProcess(MesTeamLeaderBatchRecordBackfillCommand command);
}
