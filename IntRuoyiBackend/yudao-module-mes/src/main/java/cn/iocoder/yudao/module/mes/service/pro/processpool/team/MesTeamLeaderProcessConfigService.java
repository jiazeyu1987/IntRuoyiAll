package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigListReqVO;

import java.util.List;

public interface MesTeamLeaderProcessConfigService {

    List<MesTeamLeaderProcessConfigRow> listProcessConfigs(Long leaderUserId,
                                                           MesTeamLeaderProcessConfigListReqVO reqVO);
}
