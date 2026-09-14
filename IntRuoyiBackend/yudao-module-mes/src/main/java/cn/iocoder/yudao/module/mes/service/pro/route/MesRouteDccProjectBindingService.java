package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingSaveReqVO;
import jakarta.validation.Valid;

public interface MesRouteDccProjectBindingService {

    MesRouteDccProjectBindingRespVO getBinding(Long routeId);

    MesRouteDccProjectBindingRespVO saveBinding(@Valid MesRouteDccProjectBindingSaveReqVO reqVO);

    MesRouteDccProjectBindingRespVO deleteBinding(Long routeId, Long expectedVersion);
}
