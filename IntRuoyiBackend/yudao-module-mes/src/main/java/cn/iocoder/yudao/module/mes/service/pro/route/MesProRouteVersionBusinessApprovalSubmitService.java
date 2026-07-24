package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;

public interface MesProRouteVersionBusinessApprovalSubmitService {

    MesProRouteVersionDO submitAndPublishCandidate(Long routeVersionId);

}
