package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionBlockerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;

import java.util.List;

/**
 * 工艺路线版本候选工作流 Service。
 */
public interface MesProRouteVersionWorkflowService {

    List<MesProRouteVersionDO> listByRouteId(Long routeId);

    MesProRouteVersionDO getVersion(Long id);

    MesProRouteVersionDO createCandidate(MesProRouteVersionCreateReqVO reqVO);

    MesProRouteVersionDO submitCandidate(Long id);

    MesProRouteVersionDO withdrawCandidate(Long id);

    MesProRouteVersionDO reopenRejectedCandidate(Long id);

    MesProRouteVersionDO cancelCandidate(Long id);

    MesProRouteVersionBlockerRespVO getPublishBlockers(Long id);

}
