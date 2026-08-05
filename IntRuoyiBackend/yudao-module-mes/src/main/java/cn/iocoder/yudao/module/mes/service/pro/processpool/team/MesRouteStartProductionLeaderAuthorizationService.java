package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;

import java.util.List;

public interface MesRouteStartProductionLeaderAuthorizationService {

    List<MesProRouteProcessDO> listAuthorizedRouteProcesses(Long leaderUserId);

    void assertCanMaintainRouteProcess(Long leaderUserId, Long routeProcessId);

}
