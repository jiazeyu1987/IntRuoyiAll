package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.util.List;

public interface MesTeamLeaderProcessConfigService {

    List<MesTeamLeaderProcessConfigRow> listProcessConfigs(Long leaderUserId);
}
