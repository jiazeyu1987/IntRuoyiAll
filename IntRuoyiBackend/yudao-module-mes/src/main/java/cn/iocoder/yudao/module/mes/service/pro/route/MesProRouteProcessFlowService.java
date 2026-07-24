package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationRespVO;

import java.util.Map;

/**
 * MES 工艺路线工序流转关系图 Service 接口
 */
public interface MesProRouteProcessFlowService {

    MesProRouteProcessFlowGraphRespVO getGraph(Long routeId);

    MesProRouteProcessFlowGraphRespVO getGraph(Long routeId, Long routeVersionId);

    MesProRouteProcessFlowValidationRespVO validateGraph(MesProRouteProcessFlowSaveReqVO reqVO);

    MesProRouteProcessFlowValidationRespVO saveGraph(MesProRouteProcessFlowSaveReqVO reqVO);

    void validateRouteEnable(Long routeId);

    void copyGraph(Long sourceRouteId, Long targetRouteId, Map<Long, Long> routeProcessIdMap);

    void deleteByRouteId(Long routeId);

    void deleteByRouteProcessId(Long routeId, Long routeProcessId);

}
