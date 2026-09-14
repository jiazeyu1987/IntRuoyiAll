package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;

/**
 * Flow-2/3/5 adapter for formal completion sources and Tx-A material writes.
 * Implementations must participate in the caller transaction and must never create a batch execution.
 */
public interface MesTeamLeaderActiveOrderCompletionBackfillPort {

    /**
     * Reads the current formal source snapshot without materializing any Tx-A document.
     * Replay handling uses this read to prevent returning a stale immutable receipt.
     */
    String readSourceSnapshotHash(Long leaderUserId, MesProcessPoolActiveOrderDO activeOrder,
                                  MesTeamLeaderActiveOrderCompletionCommand command);

    MesTeamLeaderActiveOrderCompletionBackfillDraft prepare(
            Long leaderUserId, MesProcessPoolActiveOrderDO activeOrder,
            MesTeamLeaderActiveOrderCompletionCommand command);

    void write(MesTeamLeaderActiveOrderCompletionBackfillDraft draft, Long activeOrderId);
}
