package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourceRespVO;
import jakarta.validation.Valid;

public interface MesProRouteResourceService {

    PageResult<MesProRouteResourceRespVO> getResourcePage(@Valid MesProRouteResourcePageReqVO pageReqVO);
}
