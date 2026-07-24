package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;

/**
 * 工艺路线版本生命周期服务。
 */
public interface MesProRouteVersionLifecycleService {

    /**
     * 发布候选版本。
     *
     * @param candidateRouteVersionId 候选路线版本编号
     * @return 发布后的路线版本
     */
    MesProRouteVersionDO publishCandidate(Long candidateRouteVersionId);

    /**
     * 发布候选版本。
     *
     * @param candidateRouteVersionId 候选路线版本编号
     * @param publisherUserId 发布用户编号
     * @return 发布后的路线版本
     */
    MesProRouteVersionDO publishCandidate(Long candidateRouteVersionId, Long publisherUserId);
}
