package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigRespVO;
import jakarta.validation.Valid;

import java.util.List;

public interface MesProRouteFlowConfigService {

    List<MesProRouteFlowProcessConfigRespVO> getRouteFlowProcessConfigList(Long routeId, String useType);

    List<MesProRouteFlowProcessConfigRespVO> getRouteFlowProcessConfigList(Long routeId, String useType,
                                                                           Long routeVersionId);

    void saveRouteFlowConfig(@Valid MesProRouteFlowConfigSaveReqVO saveReqVO);

    void saveRouteFlowConfigForConfigPackageImport(@Valid MesProRouteFlowConfigSaveReqVO saveReqVO);

}
