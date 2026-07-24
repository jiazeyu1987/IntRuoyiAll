package cn.iocoder.yudao.module.dcc.service.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;

import java.util.List;

public interface DccApprovalRouteAdminService {
    PageResult<DccApprovalRouteRespVO> getRoutePage(DccApprovalRoutePageReqVO reqVO);
    List<DccCategoryApprovalRouteDO> getRoutes(Long categoryId);
    DccCategoryApprovalRouteDO saveRoute(Long categoryId, DccApprovalRouteSaveReqVO reqVO);
    void deleteRoute(Long id);
    List<DccApprovalRoutePreviewRespVO> previewRoute(DccApprovalRoutePreviewReqVO reqVO);
}
