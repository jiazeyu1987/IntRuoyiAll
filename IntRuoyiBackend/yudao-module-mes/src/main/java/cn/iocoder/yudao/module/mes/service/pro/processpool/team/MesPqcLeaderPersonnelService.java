package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.util.List;

public interface MesPqcLeaderPersonnelService {

    List<MesPqcLeaderPersonnelBO> listPersonnel(Long leaderUserId, Boolean enabled);

    List<MesTeamFormalUserCandidateBO> searchFormalInspectorCandidates(Long leaderUserId, String keyword);

    Long linkFormalInspector(MesPqcLeaderPersonnelLinkReqBO reqBO);

    void updatePersonnelStatus(MesPqcLeaderPersonnelStatusUpdateReqBO reqBO);
}
