package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.util.List;

public interface MesTeamLeaderLossReasonService {

    List<MesTeamLeaderLossReasonRow> listLossReasonRows(Long leaderUserId);

    Long createLossReason(MesTeamLeaderLossReasonSaveReqBO reqBO);

    void updateLossReason(MesTeamLeaderLossReasonUpdateReqBO reqBO);

    void deleteLossReason(Long leaderUserId, Long reasonId);

}
